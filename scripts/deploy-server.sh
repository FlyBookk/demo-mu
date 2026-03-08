#!/bin/bash
# ============================================================
# 慕声报税系统 — 服务器端部署脚本
# 前提：已执行 docs/install.sh 完成环境初始化
# 执行身份：musheng 用户
# 使用方式：bash /opt/musheng/deploy/deploy-server.sh
# ============================================================

set -e

# ── 颜色输出 ──────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ── 配置（与 install.sh 保持一致）────────────────────────
APP_DIR="/opt/musheng"
JAR_NAME="musheng-web-1.0.0-SNAPSHOT.jar"
DEPLOY_PKG="${APP_DIR}/deploy"
BACKUP_DIR="${APP_DIR}/backup"
KEEP_VERSIONS=3                          # 保留最近几个历史版本
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# ── 检查部署包目录 ────────────────────────────────────────
[[ -d "$DEPLOY_PKG" ]] || error "部署包目录不存在：${DEPLOY_PKG}，请先上传"
mkdir -p "${BACKUP_DIR}/backend" "${BACKUP_DIR}/frontend"

# ============================================================
# 第一步：停止当前服务
# ============================================================
info "第一步：停止当前服务..."
if sudo systemctl is-active musheng &>/dev/null; then
    sudo systemctl stop musheng
    info "服务已停止"
else
    warn "服务未运行，跳过停止"
fi

# ============================================================
# 第二步：备份当前版本
# ============================================================
info "第二步：备份当前版本（时间戳：${TIMESTAMP}）..."

# 备份后端 JAR
CURRENT_JAR="${APP_DIR}/backend/${JAR_NAME}"
if [[ -f "$CURRENT_JAR" ]]; then
    cp "$CURRENT_JAR" "${BACKUP_DIR}/backend/${JAR_NAME}.${TIMESTAMP}"
    info "  JAR 已备份：${BACKUP_DIR}/backend/${JAR_NAME}.${TIMESTAMP}"

    # 只保留最近 N 个备份，多余的删除
    ls -t "${BACKUP_DIR}/backend/${JAR_NAME}."* 2>/dev/null \
        | tail -n +$((KEEP_VERSIONS + 1)) \
        | xargs -r rm -f
    info "  JAR 备份保留最近 ${KEEP_VERSIONS} 个版本"
else
    warn "  当前无 JAR 文件，跳过后端备份"
fi

# 备份前端目录
if [[ -d "${APP_DIR}/frontend" && -n "$(ls -A "${APP_DIR}/frontend" 2>/dev/null)" ]]; then
    tar -czf "${BACKUP_DIR}/frontend/frontend.${TIMESTAMP}.tar.gz" \
        -C "${APP_DIR}/frontend" .
    info "  前端已备份：${BACKUP_DIR}/frontend/frontend.${TIMESTAMP}.tar.gz"

    # 只保留最近 N 个备份
    ls -t "${BACKUP_DIR}/frontend/frontend."*.tar.gz 2>/dev/null \
        | tail -n +$((KEEP_VERSIONS + 1)) \
        | xargs -r rm -f
    info "  前端备份保留最近 ${KEEP_VERSIONS} 个版本"
else
    warn "  前端目录为空，跳过前端备份"
fi

# ============================================================
# 第三步：部署后端 JAR
# ============================================================
info "第三步：部署后端 JAR..."
JAR_SRC=""
for p in "${DEPLOY_PKG}/${JAR_NAME}" "${DEPLOY_PKG}/backend/${JAR_NAME}"; do
    [[ -f "$p" ]] && { JAR_SRC="$p"; break; }
done
[[ -n "$JAR_SRC" ]] || error "未找到 ${JAR_NAME}，请确认已上传到 ${DEPLOY_PKG}"

cp "$JAR_SRC" "${APP_DIR}/backend/${JAR_NAME}"
info "JAR 已部署：${APP_DIR}/backend/${JAR_NAME}"

# ============================================================
# 第四步：部署前端静态文件
# ============================================================
info "第四步：部署前端静态文件..."
FRONTEND_SRC=""
for p in "${DEPLOY_PKG}/dist" "${DEPLOY_PKG}/frontend"; do
    [[ -d "$p" && -f "$p/index.html" ]] && { FRONTEND_SRC="$p"; break; }
done

if [[ -n "$FRONTEND_SRC" ]]; then
    rm -rf "${APP_DIR}/frontend/"*
    cp -r "${FRONTEND_SRC}/"* "${APP_DIR}/frontend/"
    info "前端文件已部署：${APP_DIR}/frontend/"
else
    warn "未找到前端 dist 目录，跳过前端部署"
fi

# ============================================================
# 第五步：启动服务
# ============================================================
info "第五步：启动服务..."
sudo systemctl start musheng
sleep 3
sudo systemctl status musheng --no-pager || true

# ============================================================
# 第六步：重载 Nginx
# ============================================================
info "第六步：重载 Nginx..."
sudo systemctl reload nginx

# ============================================================
# 验证
# ============================================================
echo ""
info "========== 部署验证 =========="
echo -n "  后端服务："; sudo systemctl is-active musheng
echo -n "  Nginx 状态："; sudo systemctl is-active nginx
echo -n "  端口监听："; ss -tlnp | grep -E ':80|:8888' | awk '{print $4}' | tr '\n' ' '; echo
echo -n "  HTTP 响应："; curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost/ || echo "（前端未部署）"
echo -n "  API 响应：";  curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost/api/v1/auth/login || echo "（服务未就绪）"

SERVER_IP=$(curl -s --connect-timeout 2 ifconfig.me 2>/dev/null || echo '<服务器IP>')
echo ""
info "========== 部署完成 =========="
echo "  前端地址：http://${SERVER_IP}/"
echo "  API 文档：http://${SERVER_IP}/api/doc.html"
echo "  默认账号：admin / Admin@123"
echo ""
echo "  查看日志：tail -f /opt/musheng/logs/app.log"
echo "  服务管理：sudo systemctl [start|stop|restart|status] musheng"
echo ""
echo "  数据库导入（首次部署手动执行）："
echo "  mysql -uroot -proot musheng_tax < /path/to/musheng_tax.sql"
echo ""
echo "  版本回滚（后端）："
echo "  cp ${BACKUP_DIR}/backend/${JAR_NAME}.<时间戳> ${APP_DIR}/backend/${JAR_NAME}"
echo "  sudo systemctl restart musheng"
echo ""
echo "  版本回滚（前端）："
echo "  rm -rf ${APP_DIR}/frontend/* && tar -xzf ${BACKUP_DIR}/frontend/frontend.<时间戳>.tar.gz -C ${APP_DIR}/frontend/"
echo "  sudo systemctl reload nginx"
