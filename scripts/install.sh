#!/bin/bash
# ============================================================
# 慕声报税系统 — 服务器一键安装脚本
# 适用系统：腾讯云轻量服务器 Ubuntu 20.04 / 22.04
# 执行身份：root
# 使用方式：bash install.sh（可重复执行，幂等安全）
# ============================================================

# ── 颜色输出 ──────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; }
die()   { echo -e "${RED}[FATAL]${NC} $*"; exit 1; }

# ── 固定配置 ──────────────────────────────────────────────
DB_PASSWORD="root"

# ── 检查 root ─────────────────────────────────────────────
[[ $EUID -ne 0 ]] && die "请以 root 身份执行此脚本"

# ── 检测系统 ──────────────────────────────────────────────
info "检测操作系统..."
source /etc/os-release
echo "  ID=${ID}  VERSION_ID=${VERSION_ID}"
[[ "$ID" != "ubuntu" ]] && warn "当前系统 ${ID} 未经测试，脚本针对 Ubuntu 优化，继续执行..."

info "更新 apt 软件包索引..."
apt-get update -y

# ============================================================
# 第一步：创建系统用户 musheng
# ============================================================
info "第一步：创建系统用户 musheng..."
if id musheng &>/dev/null; then
    warn "用户 musheng 已存在，跳过创建"
else
    useradd -m -s /bin/bash musheng
    echo "musheng:musheng" | chpasswd
    info "用户 musheng 创建完成"
fi

# 每次覆盖写入，保证配置最新
cat > /etc/sudoers.d/musheng << 'EOF'
musheng ALL=(ALL) NOPASSWD: /usr/bin/systemctl start musheng
musheng ALL=(ALL) NOPASSWD: /usr/bin/systemctl stop musheng
musheng ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart musheng
musheng ALL=(ALL) NOPASSWD: /usr/bin/systemctl status musheng
musheng ALL=(ALL) NOPASSWD: /usr/bin/systemctl reload nginx
musheng ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart nginx
musheng ALL=(ALL) NOPASSWD: /usr/bin/journalctl -u musheng *
EOF
chmod 440 /etc/sudoers.d/musheng
# 验证 sudoers 语法
visudo -c -f /etc/sudoers.d/musheng && info "sudoers 配置完成" || die "sudoers 配置语法错误"

# ============================================================
# 第二步：安装 Java 17
# ============================================================
info "第二步：安装 Java 17..."
if java -version 2>&1 | grep -q '"17'; then
    warn "Java 17 已安装，跳过"
else
    apt-get install -y openjdk-17-jdk || die "Java 17 安装失败"
fi
java -version 2>&1 | head -1

# ============================================================
# 第三步：安装并初始化 MySQL 8
# ============================================================
info "第三步：安装 MySQL 8..."
if ! dpkg -l mysql-server 2>/dev/null | grep -q '^ii'; then
    DEBIAN_FRONTEND=noninteractive apt-get install -y mysql-server || die "MySQL 安装失败"
else
    warn "MySQL 已安装，跳过安装"
fi

# 确保 MySQL 运行
systemctl start mysql 2>/dev/null || true
sleep 2
systemctl enable mysql &>/dev/null || true
systemctl is-active mysql || die "MySQL 启动失败"

# ── MySQL 初始化 ─────────────────────────────────────────
# 策略：先尝试 auth_socket 无密码登录（首次安装），失败则用已有密码登录（重复执行）
info "初始化 MySQL root 密码及权限..."

mysql_init() {
    mysql -u root "$@" 2>/dev/null << EOF
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED WITH mysql_native_password BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
CREATE DATABASE IF NOT EXISTS musheng_tax CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
FLUSH PRIVILEGES;
EOF
}

# 首次：auth_socket 无密码登录
if mysql_init; then
    info "MySQL root 密码已设置为：${DB_PASSWORD}"
else
    # 重复执行：已切换为密码认证，用密码登录
    warn "auth_socket 登录失败，尝试用密码登录..."
    if mysql_init "-p${DB_PASSWORD}"; then
        info "MySQL 初始化完成（密码登录）"
    else
        die "MySQL 初始化失败，请手动检查"
    fi
fi

# ── 开放 MySQL 远程端口 ──────────────────────────────────
MYCNF="/etc/mysql/mysql.conf.d/mysqld.cnf"
if [[ -f "$MYCNF" ]]; then
    # 替换已有的 bind-address，不存在则追加
    if grep -q "^bind-address" "$MYCNF"; then
        sed -i 's/^bind-address[ ]*=.*/bind-address = 0.0.0.0/' "$MYCNF"
    else
        echo "bind-address = 0.0.0.0" >> "$MYCNF"
    fi
    info "MySQL bind-address 已设置为 0.0.0.0"
fi
systemctl restart mysql
sleep 2

# 验证
if mysql -u root -p"${DB_PASSWORD}" -e "SHOW DATABASES;" 2>/dev/null | grep -q musheng_tax; then
    info "数据库 musheng_tax 验证通过"
else
    error "数据库验证失败，请手动检查"
fi

# ============================================================
# 第四步：安装 Nginx
# ============================================================
info "第四步：安装 Nginx..."
if ! command -v nginx &>/dev/null; then
    apt-get install -y nginx || die "Nginx 安装失败"
else
    warn "Nginx 已安装，跳过安装"
fi
systemctl start nginx 2>/dev/null || true
systemctl enable nginx &>/dev/null || true
systemctl is-active nginx

# ============================================================
# 第五步：创建应用目录
# ============================================================
info "第五步：创建应用目录..."
mkdir -p /opt/musheng/{backend,frontend,uploads/{chunks,files,temp},backup,logs}
chown -R musheng:musheng /opt/musheng
chmod -R 755 /opt/musheng
ls -la /opt/musheng/

# ============================================================
# 第六步：配置 Nginx（每次覆盖写入，保证配置最新）
# ============================================================
info "第六步：配置 Nginx..."

# 禁用 Ubuntu 默认站点
rm -f /etc/nginx/sites-enabled/default

cat > /etc/nginx/sites-available/musheng << 'EOF'
server {
    listen 80;
    server_name _;

    root /opt/musheng/frontend;
    index index.html;

    # 开启 gzip，JS/CSS 压缩率约 70%，1.5MB → 400KB
    gzip on;
    gzip_static on;
    gzip_types text/plain text/css application/javascript application/json application/xml;
    gzip_min_length 1k;
    gzip_comp_level 6;
    gzip_vary on;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # index.html 不缓存，避免新旧版本错配
    location = /index.html {
        add_header Cache-Control "no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0";
    }

    # JS/CSS/图片等静态资源强缓存（文件名已带 hash，适合长期缓存）
    location ~* \.(js|css|png|jpg|jpeg|gif|svg|ico|woff2?|woff|ttf|eot)$ {
        expires 365d;
        add_header Cache-Control "public, immutable";
    }

    location /api/ {
        proxy_pass         http://127.0.0.1:8888/;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout    300s;
        proxy_read_timeout    300s;
    }

    client_max_body_size 100M;
}
EOF

ln -sf /etc/nginx/sites-available/musheng /etc/nginx/sites-enabled/musheng

if nginx -t 2>&1; then
    systemctl reload nginx
    info "Nginx 配置完成"
else
    die "Nginx 配置测试失败，请检查"
fi

# ============================================================
# 第七步：配置 systemd 服务（每次覆盖写入）
# ============================================================
info "第七步：配置 systemd 服务..."
cat > /etc/systemd/system/musheng.service << 'EOF'
[Unit]
Description=Musheng Tax System
After=network.target mysql.service

[Service]
Type=simple
User=musheng
Group=musheng
WorkingDirectory=/opt/musheng/backend
ExecStart=/usr/bin/java -jar /opt/musheng/backend/musheng-web-1.0.0-SNAPSHOT.jar \
          --spring.profiles.active=prod \
          --server.port=8888
Restart=on-failure
RestartSec=10
StandardOutput=null
StandardError=null

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable musheng 2>/dev/null || true
info "systemd 服务注册完成（等上传 JAR 后再启动）"

# 配置日志定期清理（幂等）
CRON_JOB="0 2 * * * find /opt/musheng/logs -name '*.log' -mtime +30 -delete"
if ! crontab -u musheng -l 2>/dev/null | grep -qF "musheng/logs"; then
    (crontab -u musheng -l 2>/dev/null; echo "$CRON_JOB") | crontab -u musheng -
    info "日志清理 cron 已配置"
else
    warn "日志清理 cron 已存在，跳过"
fi

# ============================================================
# 第八步：配置防火墙（Ubuntu 用 ufw）
# ============================================================
info "第八步：配置防火墙..."
if command -v ufw &>/dev/null; then
    ufw allow 22/tcp   2>/dev/null || true
    ufw allow 80/tcp   2>/dev/null || true
    ufw allow 3306/tcp 2>/dev/null || true
    ufw --force enable 2>/dev/null || true
    info "ufw 防火墙规则已更新"
    ufw status
else
    warn "ufw 未安装，跳过（请在腾讯云安全组开放 TCP 80/3306/22 端口）"
fi

# ============================================================
# 全量验证
# ============================================================
echo ""
info "========== 全量验证 =========="
echo -n "  Java 版本：";    java -version 2>&1 | head -1
echo -n "  MySQL 状态：";   systemctl is-active mysql
echo -n "  Nginx 状态：";   systemctl is-active nginx
echo -n "  目录权限：";     stat -c "%U" /opt/musheng
echo -n "  端口监听：";     ss -tlnp | grep -E ':80|:3306|:8888' | awk '{print $4}' | tr '\n' ' '; echo
echo -n "  HTTP 响应：";    curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost/ || echo "（前端未部署）"

echo ""
info "========== 安装完成 =========="
echo "  后续步骤："
echo "  1. 上传 JAR：    scp musheng-web-1.0.0-SNAPSHOT.jar root@<IP>:/opt/musheng/backend/"
echo "  2. 上传前端：    scp -r dist/* root@<IP>:/opt/musheng/frontend/"
echo "  3. 导入数据库：  mysql -u root -p${DB_PASSWORD} musheng_tax < musheng_tax.sql"
echo "  4. 启动服务：    systemctl start musheng"
echo "  5. 查看日志：    journalctl -u musheng -f"
echo ""
warn "  腾讯云安全组记得开放 TCP 80 / 3306 / 22 端口！"
echo ""

if [[ -t 0 ]]; then
    exec su - musheng
fi
