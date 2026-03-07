#!/usr/bin/env bash
# =============================================================================
# 慕声报税系统 - 服务器端一键部署脚本
# 在阿里云白机上执行，自动安装 Java/MySQL/Nginx 并部署应用
# 用法: MUSHENG_DB_PASSWORD=xxx ./deploy-server.sh [部署包路径]
# =============================================================================

set -e

# 配置
APP_DIR="/opt/musheng"
DEPLOY_DIR="${1:-/tmp/musheng-deploy}"
JAR_NAME="musheng-web-1.0.0-SNAPSHOT.jar"
DB_NAME="musheng_tax"
DB_USER="musheng"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# 检测操作系统
detect_os() {
  if [ -f /etc/os-release ]; then
    . /etc/os-release
    echo "$ID"
  elif [ -f /etc/redhat-release ]; then
    echo "centos"
  else
    log_error "无法识别操作系统"
    exit 1
  fi
}

# 安装 Java 17
install_java() {
  if java -version 2>&1 | grep -q "17"; then
    log_info "Java 17 已安装"
    return
  fi
  log_info "安装 Java 17..."
  case "$OS" in
    centos|rhel|almalinux|rocky)
      yum install -y java-17-openjdk java-17-openjdk-devel
      ;;
    ubuntu|debian)
      apt-get update -qq
      apt-get install -y openjdk-17-jdk
      ;;
    *)
      log_error "不支持的操作系统: $OS"
      exit 1
      ;;
  esac
  log_info "Java 17 安装完成"
}

# 安装 MySQL 8
install_mysql() {
  if systemctl is-active --quiet mysqld 2>/dev/null || systemctl is-active --quiet mysql 2>/dev/null; then
    log_info "MySQL 已安装且运行中"
    return
  fi
  log_info "安装 MySQL 8..."
  case "$OS" in
    centos|rhel|almalinux|rocky)
      yum install -y mysql-server
      systemctl start mysqld
      systemctl enable mysqld
      MYSQL_SVC="mysqld"
      ;;
    ubuntu|debian)
      apt-get install -y mysql-server
      systemctl start mysql
      systemctl enable mysql
      MYSQL_SVC="mysql"
      ;;
    *)
      log_error "不支持的操作系统: $OS"
      exit 1
      ;;
  esac
  log_info "MySQL 安装完成"
}

# 配置 MySQL：创建库、用户、导入 SQL
setup_mysql() {
  [ -n "$MUSHENG_DB_PASSWORD" ] || { log_error "请设置环境变量 MUSHENG_DB_PASSWORD"; exit 1; }

  # 等待 MySQL 就绪
  for i in $(seq 1 30); do
    if mysqladmin ping -h localhost -u root --silent 2>/dev/null || sudo mysqladmin ping -h localhost --silent 2>/dev/null; then
      break
    fi
    [ $i -eq 30 ] && { log_error "MySQL 启动超时"; exit 1; }
    sleep 2
  done

  # 获取 root 临时密码（CentOS 首次安装）
  ROOT_PASS=""
  if [ -f /var/log/mysqld.log ]; then
    ROOT_PASS=$(grep "temporary password" /var/log/mysqld.log 2>/dev/null | awk '{print $NF}' | tail -1)
  fi

  SQL_FILE="$DEPLOY_DIR/musheng_tax.sql"
  [ -f "$SQL_FILE" ] || SQL_FILE="$DEPLOY_DIR/sql/musheng_tax.sql"
  [ -f "$SQL_FILE" ] || { log_error "未找到 musheng_tax.sql，请放入 $DEPLOY_DIR"; exit 1; }

  INIT_SQL="
CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP USER IF EXISTS '$DB_USER'@'localhost';
CREATE USER '$DB_USER'@'localhost' IDENTIFIED BY '$MUSHENG_DB_PASSWORD';
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost';
FLUSH PRIVILEGES;
"

  # 创建库和用户
  log_info "创建数据库和用户..."
  MYSQL_OK=0
  if [ -n "$ROOT_PASS" ]; then
    if mysql -u root -p"$ROOT_PASS" --connect-expired-password -e "
      ALTER USER 'root'@'localhost' IDENTIFIED BY '$MUSHENG_DB_PASSWORD';
      CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
      DROP USER IF EXISTS '$DB_USER'@'localhost';
      CREATE USER '$DB_USER'@'localhost' IDENTIFIED BY '$MUSHENG_DB_PASSWORD';
      GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost';
      FLUSH PRIVILEGES;
    " 2>/dev/null; then
      MYSQL_OK=1
    fi
  fi
  if [ $MYSQL_OK -eq 0 ]; then
    echo "$INIT_SQL" | mysql -u root -p"$MUSHENG_DB_PASSWORD" 2>/dev/null && MYSQL_OK=1
  fi
  if [ $MYSQL_OK -eq 0 ]; then
    echo "$INIT_SQL" | mysql -u root 2>/dev/null && MYSQL_OK=1
  fi
  if [ $MYSQL_OK -eq 0 ]; then
    echo "$INIT_SQL" | sudo mysql 2>/dev/null && MYSQL_OK=1
  fi
  if [ $MYSQL_OK -eq 0 ]; then
    log_warn "无法用 root 连接 MySQL，请手动创建用户后重新运行："
    echo "$INIT_SQL"
    exit 1
  fi

  # 导入 SQL
  log_info "导入数据库结构..."
  mysql -u "$DB_USER" -p"$MUSHENG_DB_PASSWORD" "$DB_NAME" < "$SQL_FILE"
  log_info "数据库初始化完成"
}

# 安装 Nginx
install_nginx() {
  if command -v nginx &>/dev/null; then
    log_info "Nginx 已安装"
    return
  fi
  log_info "安装 Nginx..."
  case "$OS" in
    centos|rhel|almalinux|rocky)
      yum install -y nginx
      ;;
    ubuntu|debian)
      apt-get install -y nginx
      ;;
    *)
      log_error "不支持的操作系统: $OS"
      exit 1
      ;;
  esac
  systemctl start nginx
  systemctl enable nginx
  log_info "Nginx 安装完成"
}

# 部署应用文件
deploy_app() {
  log_info "部署应用文件..."

  # 查找 jar
  JAR_SRC=""
  for p in "$DEPLOY_DIR/$JAR_NAME" "$DEPLOY_DIR/backend/$JAR_NAME" "$DEPLOY_DIR/musheng-tax-system/musheng-web/target/$JAR_NAME"; do
    [ -f "$p" ] && { JAR_SRC="$p"; break; }
  done
  [ -n "$JAR_SRC" ] || { log_error "未找到 $JAR_NAME"; exit 1; }

  mkdir -p "$APP_DIR/backend" "$APP_DIR/frontend" "$APP_DIR/uploads"/{chunks,files,temp}
  cp "$JAR_SRC" "$APP_DIR/backend/$JAR_NAME"

  # 查找前端 dist
  FRONTEND_SRC=""
  for p in "$DEPLOY_DIR/dist" "$DEPLOY_DIR/frontend" "$DEPLOY_DIR/musheng-tax-web/dist"; do
    [ -d "$p" ] && [ -f "$p/index.html" ] && { FRONTEND_SRC="$p"; break; }
  done
  if [ -n "$FRONTEND_SRC" ]; then
    cp -r "$FRONTEND_SRC"/* "$APP_DIR/frontend/"
    log_info "前端文件已部署"
  else
    log_warn "未找到前端 dist，请手动上传到 $APP_DIR/frontend"
  fi
}

# 配置应用
configure_app() {
  log_info "配置应用..."
  cat > "$APP_DIR/backend/application-prod.yml" << EOF
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/$DB_NAME?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: $DB_USER
    password: $MUSHENG_DB_PASSWORD

app:
  upload:
    chunk-dir: $APP_DIR/uploads/chunks
    final-dir: $APP_DIR/uploads/files
    temp-dir: $APP_DIR/uploads/temp
EOF
}

# 配置 Nginx
configure_nginx() {
  log_info "配置 Nginx..."
  cat | sudo tee /etc/nginx/conf.d/musheng.conf << EOF
server {
    listen 80;
    server_name _;

    root $APP_DIR/frontend;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    client_max_body_size 100M;
}
EOF
  sudo nginx -t && sudo systemctl reload nginx
}

# 配置 systemd 服务
configure_systemd() {
  log_info "配置 systemd 服务..."
  sudo tee /etc/systemd/system/musheng.service << EOF
[Unit]
Description=Musheng Tax System
After=network.target mysql.service mysqld.service

[Service]
Type=simple
User=musheng
Group=musheng
WorkingDirectory=$APP_DIR/backend
ExecStart=/usr/bin/java -jar $APP_DIR/backend/$JAR_NAME --spring.profiles.active=prod
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
  sudo systemctl daemon-reload
  sudo systemctl enable musheng
  sudo systemctl restart musheng
  log_info "后端服务已启动"
}

# 开放防火墙
open_firewall() {
  if command -v firewall-cmd &>/dev/null && systemctl is-active --quiet firewalld 2>/dev/null; then
    sudo firewall-cmd --permanent --add-service=http 2>/dev/null || true
    sudo firewall-cmd --permanent --add-service=ssh 2>/dev/null || true
    sudo firewall-cmd --reload 2>/dev/null || true
  elif command -v ufw &>/dev/null; then
    echo "y" | sudo ufw allow 80 2>/dev/null || true
    echo "y" | sudo ufw allow 22 2>/dev/null || true
    echo "y" | sudo ufw --force enable 2>/dev/null || true
  fi
}

# 主流程
main() {
  log_info "========== 慕声报税系统 - 服务器端部署 =========="
  OS=$(detect_os)
  log_info "检测到操作系统: $OS"

  # 支持从脚本所在目录的上级查找部署包
  SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  if [ ! -d "$DEPLOY_DIR" ] && [ -d "$SCRIPT_DIR/../deploy-package" ]; then
    DEPLOY_DIR="$(cd "$SCRIPT_DIR/../deploy-package" && pwd)"
    log_info "使用部署包: $DEPLOY_DIR"
  fi

  [ -d "$DEPLOY_DIR" ] || { log_error "部署包目录不存在: $DEPLOY_DIR"; exit 1; }

  install_java
  install_mysql
  setup_mysql
  install_nginx
  deploy_app
  configure_app
  configure_nginx
  configure_systemd
  open_firewall

  PUBLIC_IP=$(curl -s --connect-timeout 3 ifconfig.me 2>/dev/null || echo "<公网IP>")
  log_info "========== 部署完成 =========="
  echo ""
  echo "  访问地址: http://$PUBLIC_IP/"
  echo "  API 文档: http://$PUBLIC_IP/api/doc.html"
  echo "  默认账号: admin / Admin@123"
  echo ""
}

main "$@"
