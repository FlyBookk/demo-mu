#!/bin/bash
# ============================================================
# 慕声报税系统 — 本地一键构建 + 上传 + 部署脚本
# 执行身份：本地开发机
# 使用方式：bash scripts/deploy-local.sh <服务器IP> [SSH用户]
#
# 依赖：
#   - sshpass（密码登录时需要）：brew install sshpass
#   - 或配置 SSH 密钥免密登录（推荐）
# ============================================================

set -e

# ── 颜色输出 ──────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ── 参数 ──────────────────────────────────────────────────
SERVER_IP="${1:-47.90.151.125}"
SSH_USER="${2:-root}"
SSH_PASS="${3:-liuzequan1028}"    # 如已配置密钥，此参数忽略
REMOTE_DEPLOY="/opt/musheng/deploy"

# ── 本地路径（相对于项目根目录）──────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKEND_DIR="${PROJECT_ROOT}/musheng-tax-system"
FRONTEND_DIR="${PROJECT_ROOT}/musheng-tax-web"
SQL_FILE="${PROJECT_ROOT}/sql/musheng_tax.sql"
JAR_NAME="musheng-web-1.0.0-SNAPSHOT.jar"
JAR_PATH="${BACKEND_DIR}/musheng-web/target/${JAR_NAME}"

# ── SSH 命令封装（优先密钥，降级密码）────────────────────
ssh_cmd() {
    if command -v sshpass &>/dev/null && [[ -n "$SSH_PASS" ]]; then
        sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no "${SSH_USER}@${SERVER_IP}" "$@"
    else
        ssh -o StrictHostKeyChecking=no "${SSH_USER}@${SERVER_IP}" "$@"
    fi
}

scp_cmd() {
    if command -v sshpass &>/dev/null && [[ -n "$SSH_PASS" ]]; then
        sshpass -p "$SSH_PASS" scp -o StrictHostKeyChecking=no "$@"
    else
        scp -o StrictHostKeyChecking=no "$@"
    fi
}

info "目标服务器：${SSH_USER}@${SERVER_IP}"
info "项目根目录：${PROJECT_ROOT}"

# ============================================================
# 第一步：构建后端
# ============================================================
info "第一步：构建后端 JAR..."
if [[ -d "$BACKEND_DIR" ]]; then
    cd "$BACKEND_DIR"
    mvn clean package -DskipTests -q
    [[ -f "$JAR_PATH" ]] || error "构建失败，未找到 ${JAR_PATH}"
    info "后端构建完成：${JAR_PATH}"
else
    warn "后端目录不存在（${BACKEND_DIR}），跳过构建"
    [[ -f "$JAR_PATH" ]] || error "未找到 JAR 文件，请先手动构建"
fi

# ============================================================
# 第二步：构建前端
# ============================================================
info "第二步：构建前端..."
if [[ -d "$FRONTEND_DIR" ]]; then
    cd "$FRONTEND_DIR"
    npm run build --silent
    [[ -d "${FRONTEND_DIR}/dist" ]] || error "前端构建失败，未找到 dist 目录"
    info "前端构建完成：${FRONTEND_DIR}/dist"
else
    warn "前端目录不存在（${FRONTEND_DIR}），跳过构建"
fi

# ============================================================
# 第三步：上传文件到服务器
# ============================================================
info "第三步：上传文件到服务器..."

# 确保远端目录存在
ssh_cmd "mkdir -p ${REMOTE_DEPLOY}/dist"

# 上传后端 JAR
info "  上传 JAR..."
scp_cmd "$JAR_PATH" "${SSH_USER}@${SERVER_IP}:${REMOTE_DEPLOY}/${JAR_NAME}"

# 上传前端 dist
if [[ -d "${FRONTEND_DIR}/dist" ]]; then
    info "  上传前端 dist..."
    scp_cmd -r "${FRONTEND_DIR}/dist/." "${SSH_USER}@${SERVER_IP}:${REMOTE_DEPLOY}/dist/"
fi

# 上传数据库 SQL（如存在）
if [[ -f "$SQL_FILE" ]]; then
    info "  上传数据库 SQL..."
    scp_cmd "$SQL_FILE" "${SSH_USER}@${SERVER_IP}:${REMOTE_DEPLOY}/musheng_tax.sql"
else
    warn "  未找到 ${SQL_FILE}，跳过 SQL 上传"
fi

# 上传服务器端部署脚本
info "  上传 deploy-server.sh..."
scp_cmd "${SCRIPT_DIR}/deploy-server.sh" "${SSH_USER}@${SERVER_IP}:${REMOTE_DEPLOY}/deploy-server.sh"

# ============================================================
# 第四步：在服务器上执行部署
# ============================================================
info "第四步：在服务器上执行部署..."
# root 上传后修正目录归属，再以 musheng 用户执行部署脚本
ssh_cmd "chown -R musheng:musheng ${REMOTE_DEPLOY} && su - musheng -c 'bash ${REMOTE_DEPLOY}/deploy-server.sh'"

info "========== 本地部署流程完成 =========="
echo "  前端地址：http://${SERVER_IP}/"
echo "  API 文档：http://${SERVER_IP}/api/doc.html"
