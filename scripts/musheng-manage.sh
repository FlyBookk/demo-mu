#!/usr/bin/env bash
# =============================================================================
# 慕声报税系统 — 服务器端管理脚本
# 用法：bash musheng-manage.sh <命令>
#
# 命令列表：
#   start       启动后端服务
#   stop        停止后端服务
#   restart     重启后端服务
#   status      查看服务状态
#   log         实时查看日志（Ctrl+C 退出）
#   log-tail    查看最近 200 行日志
#   deploy-backend  <jar路径>   替换 JAR 并重启
#   deploy-frontend <dist路径>  替换前端静态文件
#   nginx-reload    热重载 Nginx 配置
#   db-backup       备份数据库到 /opt/musheng/backup/
#   health          检查各组件运行状态
# =============================================================================

set -e

APP_DIR="/opt/musheng"
JAR_NAME="musheng-web-1.0.0-SNAPSHOT.jar"
SERVICE="musheng"
DB_NAME="musheng_tax"
DB_USER="musheng"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }
log_title() { echo -e "\n${BLUE}══════════════════════════════════════${NC}"; echo -e "${BLUE}  $*${NC}"; echo -e "${BLUE}══════════════════════════════════════${NC}"; }

usage() {
  echo ""
  echo "用法: $0 <命令> [参数]"
  echo ""
  echo "服务管理:"
  echo "  start              启动后端服务"
  echo "  stop               停止后端服务"
  echo "  restart            重启后端服务"
  echo "  status             查看服务状态"
  echo ""
  echo "日志:"
  echo "  log                实时日志（Ctrl+C 退出）"
  echo "  log-tail           最近 200 行日志"
  echo ""
  echo "部署更新:"
  echo "  deploy-backend  <jar文件路径>    替换 JAR 并重启"
  echo "  deploy-frontend <dist目录路径>   替换前端静态文件"
  echo "  nginx-reload                     热重载 Nginx 配置"
  echo ""
  echo "运维:"
  echo "  db-backup          备份数据库"
  echo "  health             检查各组件状态"
  echo ""
}

# ── 服务管理 ──────────────────────────────────────────────────────────────────

cmd_start() {
  log_info "启动 $SERVICE 服务..."
  systemctl start "$SERVICE"
  sleep 2
  systemctl is-active --quiet "$SERVICE" \
    && log_info "服务已启动" \
    || { log_error "启动失败，查看日志：journalctl -u $SERVICE -n 50"; exit 1; }
}

cmd_stop() {
  log_info "停止 $SERVICE 服务..."
  systemctl stop "$SERVICE"
  log_info "服务已停止"
}

cmd_restart() {
  log_info "重启 $SERVICE 服务..."
  systemctl restart "$SERVICE"
  sleep 3
  systemctl is-active --quiet "$SERVICE" \
    && log_info "服务已重启" \
    || { log_error "重启失败，查看日志：journalctl -u $SERVICE -n 50"; exit 1; }
}

cmd_status() {
  systemctl status "$SERVICE" --no-pager
}

# ── 日志 ──────────────────────────────────────────────────────────────────────

cmd_log() {
  log_info "实时日志（Ctrl+C 退出）..."
  journalctl -u "$SERVICE" -f
}

cmd_log_tail() {
  journalctl -u "$SERVICE" -n 200 --no-pager
}

# ── 部署更新 ──────────────────────────────────────────────────────────────────

cmd_deploy_backend() {
  local jar_src="$1"
  [ -n "$jar_src" ] || { log_error "请指定 JAR 文件路径"; exit 1; }
  [ -f "$jar_src" ] || { log_error "文件不存在: $jar_src"; exit 1; }

  log_title "更新后端"
  log_info "停止服务..."
  systemctl stop "$SERVICE" 2>/dev/null || true

  # 备份旧 JAR
  local old_jar="$APP_DIR/backend/$JAR_NAME"
  if [ -f "$old_jar" ]; then
    cp "$old_jar" "${old_jar}.bak.$(date +%Y%m%d%H%M%S)"
    log_info "旧 JAR 已备份"
  fi

  log_info "复制新 JAR..."
  cp "$jar_src" "$APP_DIR/backend/$JAR_NAME"

  log_info "启动服务..."
  systemctl start "$SERVICE"
  sleep 3
  systemctl is-active --quiet "$SERVICE" \
    && log_info "后端更新完成，服务运行中" \
    || { log_error "启动失败，查看日志：journalctl -u $SERVICE -n 50"; exit 1; }
}

cmd_deploy_frontend() {
  local dist_src="$1"
  [ -n "$dist_src" ] || { log_error "请指定 dist 目录路径"; exit 1; }
  [ -d "$dist_src" ] || { log_error "目录不存在: $dist_src"; exit 1; }
  [ -f "$dist_src/index.html" ] || { log_error "不是有效的 dist 目录（缺少 index.html）"; exit 1; }

  log_title "更新前端"
  log_info "清空旧文件..."
  rm -rf "$APP_DIR/frontend"/*

  log_info "复制新文件..."
  cp -r "$dist_src"/. "$APP_DIR/frontend/"

  log_info "热重载 Nginx..."
  nginx -t && systemctl reload nginx
  log_info "前端更新完成"
}

cmd_nginx_reload() {
  log_info "检查 Nginx 配置..."
  nginx -t
  log_info "热重载 Nginx..."
  systemctl reload nginx
  log_info "Nginx 已重载"
}

# ── 运维 ──────────────────────────────────────────────────────────────────────

cmd_db_backup() {
  local backup_dir="$APP_DIR/backup"
  mkdir -p "$backup_dir"
  local backup_file="$backup_dir/${DB_NAME}_$(date +%Y%m%d_%H%M%S).sql.gz"

  log_info "备份数据库到 $backup_file ..."

  # 尝试从 application-prod.yml 读取密码
  local db_pass=""
  local prod_yml="$APP_DIR/backend/application-prod.yml"
  if [ -f "$prod_yml" ]; then
    db_pass=$(grep -E "^\s*password:" "$prod_yml" | head -1 | awk '{print $2}' | tr -d '"'"'" 2>/dev/null || true)
  fi

  if [ -n "$db_pass" ]; then
    mysqldump -u "$DB_USER" -p"$db_pass" "$DB_NAME" | gzip > "$backup_file"
  else
    log_warn "未能自动读取密码，请输入数据库密码："
    mysqldump -u "$DB_USER" -p "$DB_NAME" | gzip > "$backup_file"
  fi

  log_info "备份完成：$backup_file（$(du -sh "$backup_file" | cut -f1)）"

  # 保留最近 10 个备份
  ls -t "$backup_dir"/${DB_NAME}_*.sql.gz 2>/dev/null | tail -n +11 | xargs rm -f 2>/dev/null || true
  log_info "已清理旧备份，保留最近 10 个"
}

cmd_health() {
  log_title "系统健康检查"

  # Java 服务
  echo -n "  后端服务 (musheng)  : "
  if systemctl is-active --quiet "$SERVICE"; then
    echo -e "${GREEN}运行中${NC}"
  else
    echo -e "${RED}已停止${NC}"
  fi

  # Nginx
  echo -n "  Nginx               : "
  if systemctl is-active --quiet nginx; then
    echo -e "${GREEN}运行中${NC}"
  else
    echo -e "${RED}已停止${NC}"
  fi

  # MySQL
  echo -n "  MySQL               : "
  if systemctl is-active --quiet mysqld 2>/dev/null || systemctl is-active --quiet mysql 2>/dev/null; then
    echo -e "${GREEN}运行中${NC}"
  else
    echo -e "${RED}已停止${NC}"
  fi

  # 端口
  echo -n "  端口 8080 (后端)    : "
  if ss -tlnp 2>/dev/null | grep -q ':8080' || netstat -tlnp 2>/dev/null | grep -q ':8080'; then
    echo -e "${GREEN}监听中${NC}"
  else
    echo -e "${YELLOW}未监听${NC}"
  fi

  echo -n "  端口 80 (Nginx)     : "
  if ss -tlnp 2>/dev/null | grep -q ':80 ' || netstat -tlnp 2>/dev/null | grep -q ':80 '; then
    echo -e "${GREEN}监听中${NC}"
  else
    echo -e "${YELLOW}未监听${NC}"
  fi

  # 磁盘
  echo ""
  echo "  磁盘使用："
  df -h "$APP_DIR" 2>/dev/null | tail -1 | awk '{printf "    %-20s 已用 %s / 总 %s (%s)\n", $6, $3, $2, $5}'

  # 上传目录大小
  echo -n "  上传文件目录大小    : "
  du -sh "$APP_DIR/uploads/files" 2>/dev/null | cut -f1 || echo "N/A"

  echo ""
}

# ── 入口 ──────────────────────────────────────────────────────────────────────

case "${1:-}" in
  start)           cmd_start ;;
  stop)            cmd_stop ;;
  restart)         cmd_restart ;;
  status)          cmd_status ;;
  log)             cmd_log ;;
  log-tail)        cmd_log_tail ;;
  deploy-backend)  cmd_deploy_backend "$2" ;;
  deploy-frontend) cmd_deploy_frontend "$2" ;;
  nginx-reload)    cmd_nginx_reload ;;
  db-backup)       cmd_db_backup ;;
  health)          cmd_health ;;
  *)               usage; exit 1 ;;
esac
