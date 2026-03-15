#!/bin/bash
# ============================================================
# 慕声报税系统 — 服务器一键安装脚本
# 适用系统：Alibaba Cloud Linux 3（alinux3）
# 执行身份：root
# 使用方式：bash install.sh [db_password]
# ============================================================

set -e

# ── 颜色输出 ──────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ── 参数 ──────────────────────────────────────────────────
DB_PASSWORD="${1:-YourDbPassword}"

# ── 检查 root ─────────────────────────────────────────────
[[ $EUID -ne 0 ]] && error "请以 root 身份执行此脚本"

# ── 检测系统 ──────────────────────────────────────────────
info "检测操作系统..."
source /etc/os-release
echo "  ID=${ID}  VERSION_ID=${VERSION_ID}"
[[ "$ID" != "alinux" ]] && warn "当前系统非 alinux，脚本针对 Alibaba Cloud Linux 3 优化，继续执行..."

PKG_MGR="dnf"
command -v dnf &>/dev/null || PKG_MGR="yum"
info "包管理器：${PKG_MGR}"

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

if [[ ! -f /etc/sudoers.d/musheng ]]; then
    cat > /etc/sudoers.d/musheng << 'EOF'
musheng ALL=(ALL) NOPASSWD: \
    /usr/bin/systemctl start musheng, \
    /usr/bin/systemctl stop musheng, \
    /usr/bin/systemctl restart musheng, \
    /usr/bin/systemctl status musheng, \
    /usr/bin/systemctl reload nginx, \
    /usr/bin/systemctl restart nginx, \
    /usr/bin/journalctl -u musheng *
EOF
    chmod 440 /etc/sudoers.d/musheng
    info "sudoers 配置完成"
else
    warn "sudoers 配置已存在，跳过"
fi
id musheng

# ============================================================
# 第二步：安装 Java 17
# ============================================================
info "第二步：安装 Java 17..."
if java -version 2>&1 | grep -q '"17'; then
    warn "Java 17 已安装，跳过"
else
    $PKG_MGR install -y java-17-openjdk java-17-openjdk-devel
fi
java -version 2>&1 | head -1

# ============================================================
# 第三步：安装 MySQL 8
# ============================================================
info "第三步：安装 MySQL 8..."
if command -v mysqld &>/dev/null || rpm -q mysql-server &>/dev/null 2>&1; then
    warn "MySQL 已安装，跳过安装"
else
    # alinux3 官方源包含 mysql-server（MySQL 8）
    $PKG_MGR install -y mysql-server
fi
# 确保服务启动并开机自启（无论是否刚安装）
if ! systemctl is-active mysqld &>/dev/null; then
    systemctl start mysqld
fi
systemctl enable mysqld &>/dev/null
systemctl status mysqld --no-pager || true

# 初始化数据库
info "初始化 MySQL 数据库..."

# alinux3 首次安装后 root@localhost 默认无密码，直接登录
# 只做两件事：设置密码 + 允许远程连接（Navicat 等工具需要）
mysql -u root << 'EOSQL'
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'root';
UPDATE mysql.user SET host='%' WHERE user='root' AND host='localhost';
FLUSH PRIVILEGES;
EOSQL

# 创建业务数据库
mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS musheng_tax CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

info "MySQL 初始化完成（root/root，支持外部访问）"
mysql -u root -proot -e "SHOW DATABASES;" 2>/dev/null | grep -q musheng_tax \
    && info "数据库验证通过" || warn "数据库验证失败，请检查"

# ============================================================
# 第四步：安装 Nginx
# ============================================================
info "第四步：安装 Nginx..."
if command -v nginx &>/dev/null || rpm -q nginx &>/dev/null 2>&1; then
    warn "Nginx 已安装，跳过安装"
else
    $PKG_MGR install -y nginx
fi
# 确保服务启动并开机自启
if ! systemctl is-active nginx &>/dev/null; then
    systemctl start nginx
fi
systemctl enable nginx &>/dev/null
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
# 第六步：配置 Nginx
# ============================================================
info "第六步：配置 Nginx..."
if [[ -f /etc/nginx/conf.d/musheng.conf ]]; then
    warn "Nginx 配置已存在，跳过写入"
else
cat > /etc/nginx/conf.d/musheng.conf << 'EOF'
server {
    listen 80;
    server_name _;

    root /opt/musheng/frontend;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
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
fi

# 禁用默认配置：移除 default.d 下的默认站点，不改动 nginx.conf 主配置
if [[ -f /etc/nginx/default.d/default.conf ]]; then
    mv /etc/nginx/default.d/default.conf /etc/nginx/default.d/default.conf.bak
    info "已备份默认站点配置"
fi

nginx -t && systemctl reload nginx
info "Nginx 配置完成"

# ============================================================
# 第七步：配置 systemd 服务
# ============================================================
info "第七步：配置 systemd 服务..."
if [[ -f /etc/systemd/system/musheng.service ]]; then
    warn "systemd 服务文件已存在，跳过写入"
else
cat > /etc/systemd/system/musheng.service << 'EOF'
[Unit]
Description=Musheng Tax System
After=network.target mysqld.service

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
fi

systemctl daemon-reload
systemctl enable musheng || true
info "systemd 服务注册完成（等上传 JAR 后再启动）"

# 配置日志定期清理（每天凌晨2点，保留30天）
CRON_JOB="0 2 * * * find /opt/musheng/logs -name '*.log' -mtime +30 -delete"
if ! crontab -u musheng -l 2>/dev/null | grep -qF "musheng/logs"; then
    (crontab -u musheng -l 2>/dev/null; echo "$CRON_JOB") | crontab -u musheng -
    info "日志清理 cron 已配置（保留30天，每天凌晨2点执行）"
else
    warn "日志清理 cron 已存在，跳过"
fi

# ============================================================
# 第八步：开放防火墙
# ============================================================
info "第八步：配置防火墙..."
if systemctl is-active firewalld &>/dev/null; then
    # 已添加过则跳过，避免重复 reload
    if ! firewall-cmd --query-service=http &>/dev/null; then
        firewall-cmd --permanent --add-service=http
        firewall-cmd --permanent --add-service=ssh
        firewall-cmd --permanent --add-port=3306/tcp
        firewall-cmd --reload
        info "防火墙规则已添加"
    else
        # 补充 3306 端口（可能之前没加）
        firewall-cmd --permanent --add-port=3306/tcp &>/dev/null || true
        warn "防火墙规则已存在，跳过（已确保 3306 开放）"
    fi
    firewall-cmd --list-all
else
    warn "firewalld 未运行，跳过防火墙配置（请在阿里云安全组开放 80/22 端口）"
fi

# ============================================================
# 全量验证
# ============================================================
echo ""
info "========== 全量验证 =========="
echo -n "  Java 版本：";    java -version 2>&1 | head -1
echo -n "  MySQL 状态：";   systemctl is-active mysqld
echo -n "  Nginx 状态：";   systemctl is-active nginx
echo -n "  目录权限：";     stat -c "%U" /opt/musheng
echo -n "  端口监听：";     ss -tlnp | grep -E ':80|:8888' | awk '{print $4}' | tr '\n' ' '; echo
echo -n "  HTTP 响应：";    curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost/ || echo "（前端未部署）"

echo ""
info "========== 安装完成 =========="
echo "  后续步骤："
echo "  1. 上传 JAR：scp musheng-web-1.0.0-SNAPSHOT.jar musheng@<IP>:/opt/musheng/backend/"
echo "  2. 上传前端：scp -r dist/* musheng@<IP>:/opt/musheng/frontend/"
echo "  3. 导入数据库：mysql -u root -proot musheng_tax < musheng_tax.sql"
echo "  4. 启动服务：systemctl start musheng"
echo "  5. 查看日志：journalctl -u musheng -f"
echo ""
warn "  阿里云安全组记得开放 TCP 80 和 22 端口！"
echo ""
info "========== 切换到 musheng 用户 =========="
echo "  后续操作请在 musheng 用户下进行："
echo "  上传文件、启动服务、查看日志等均使用此用户"
echo "  切换命令：su - musheng"
echo ""
# 仅在交互式终端下切换用户，远程 ssh 执行时跳过
if [[ -t 0 ]]; then
    exec su - musheng
fi
