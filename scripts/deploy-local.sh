#!/usr/bin/env bash
# =============================================================================
# 慕声报税系统 - 本地一键构建并部署到远程服务器
# 用法: ./deploy-local.sh <服务器IP> [SSH用户]
# 示例: MUSHENG_DB_PASSWORD=MyPass123 ./deploy-local.sh 47.96.123.45 root
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEPLOY_PKG="$REPO_ROOT/deploy-package"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

usage() {
  echo "用法: $0 <服务器IP> [SSH用户]"
  echo ""
  echo "环境变量:"
  echo "  MUSHENG_DB_PASSWORD  数据库密码（必填）"
  echo "  SSH_KEY              可选，SSH 私钥路径，如 -i /path/to/key.pem"
  echo ""
  echo "示例:"
  echo "  MUSHENG_DB_PASSWORD=MyPass123 $0 47.96.123.45"
  echo "  MUSHENG_DB_PASSWORD=MyPass123 SSH_KEY=~/.ssh/aliyun.pem $0 47.96.123.45 root"
  exit 1
}

[ $# -lt 1 ] && usage
SERVER_IP="$1"
SSH_USER="${2:-root}"
SSH_OPTS="-o StrictHostKeyChecking=no -o ConnectTimeout=10"
[ -n "${SSH_KEY:-}" ] && SSH_OPTS="$SSH_OPTS -i $SSH_KEY"

# 构建后端
build_backend() {
  log_info "构建后端..."
  cd "$REPO_ROOT/musheng-tax-system"
  mvn clean package -DskipTests -q
  log_info "后端构建完成"
}

# 构建前端
build_frontend() {
  log_info "构建前端..."
  cd "$REPO_ROOT/musheng-tax-web"
  [ -f .env.production ] || log_warn ".env.production 不存在，使用默认配置"
  npm run build 2>/dev/null || (npm ci && npm run build)
  log_info "前端构建完成"
}

# 打包部署文件
pack_deploy() {
  log_info "打包部署文件..."
  rm -rf "$DEPLOY_PKG"
  mkdir -p "$DEPLOY_PKG"

  cp "$REPO_ROOT/musheng-tax-system/musheng-web/target/musheng-web-1.0.0-SNAPSHOT.jar" "$DEPLOY_PKG/"
  cp -r "$REPO_ROOT/musheng-tax-web/dist" "$DEPLOY_PKG/"
  cp "$REPO_ROOT/musheng-tax-system/sql/musheng_tax.sql" "$DEPLOY_PKG/"

  log_info "部署包已生成: $DEPLOY_PKG"
}

# 上传并远程执行
deploy_remote() {
  log_info "上传到服务器 $SSH_USER@$SERVER_IP ..."
  ssh $SSH_OPTS "$SSH_USER@$SERVER_IP" "mkdir -p /tmp/musheng-deploy"
  scp $SSH_OPTS -r "$DEPLOY_PKG"/* "$SSH_USER@$SERVER_IP:/tmp/musheng-deploy/"

  log_info "上传部署脚本..."
  scp $SSH_OPTS "$SCRIPT_DIR/deploy-server.sh" "$SSH_USER@$SERVER_IP:/tmp/musheng-deploy/"

  log_info "在服务器上执行部署..."
  ssh $SSH_OPTS "$SSH_USER@$SERVER_IP" "chmod +x /tmp/musheng-deploy/deploy-server.sh && MUSHENG_DB_PASSWORD='$MUSHENG_DB_PASSWORD' /tmp/musheng-deploy/deploy-server.sh /tmp/musheng-deploy"
}

# 主流程
main() {
  [ -n "${MUSHENG_DB_PASSWORD:-}" ] || { log_error "请设置 MUSHENG_DB_PASSWORD"; usage; }

  log_info "========== 慕声报税系统 - 本地构建并部署 =========="
  log_info "目标: $SSH_USER@$SERVER_IP"

  build_backend
  build_frontend
  pack_deploy
  deploy_remote

  log_info "========== 部署完成 =========="
  echo ""
  echo "  访问地址: http://$SERVER_IP/"
  echo "  默认账号: admin / Admin@123"
  echo ""
}

main "$@"
