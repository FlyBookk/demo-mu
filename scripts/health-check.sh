#!/bin/bash
# ============================================================
# 慕声报税系统 — 服务健康检查与自动重启脚本
# 适用系统：Alibaba Cloud Linux 3（alinux3）
# 执行身份：root
# 使用方式：bash health-check.sh（后台运行：nohup bash health-check.sh &）
# ============================================================

# ── 配置项 ────────────────────────────────────────────────
BACKEND_SERVICE="musheng"       # systemd 服务名
MYSQL_SERVICE="mysqld"          # MySQL 服务名
BACKEND_PORT=8888               # 后端监听端口
MYSQL_USER="root"
MYSQL_PASS="root"
MAX_RESTART_ATTEMPTS=3          # 每小时最大重启次数
RESTART_WAIT_SEC=15             # 重启后等待秒数
CHECK_INTERVAL=600              # 检测间隔（秒），600 = 10分钟
STATE_DIR="/tmp/musheng-health" # 存储重启计数的临时目录

mkdir -p "$STATE_DIR"

# ── 重启计数（每小时自动重置）────────────────────────────
get_restart_count() {
    local service="$1"
    local hour_key count_file
    hour_key=$(date '+%Y%m%d%H')
    count_file="${STATE_DIR}/${service}_${hour_key}.count"
    find "$STATE_DIR" -name "${service}_*.count" ! -name "${service}_${hour_key}.count" -delete 2>/dev/null
    [[ -f "$count_file" ]] && cat "$count_file" || echo 0
}

increment_restart_count() {
    local service="$1"
    local hour_key count_file count
    hour_key=$(date '+%Y%m%d%H')
    count_file="${STATE_DIR}/${service}_${hour_key}.count"
    count=$(get_restart_count "$service")
    echo $((count + 1)) > "$count_file"
}

# ── MySQL 检查与重启 ──────────────────────────────────────
check_mysql() {
    systemctl is-active "$MYSQL_SERVICE" &>/dev/null || return 1
    mysql -u "$MYSQL_USER" -p"$MYSQL_PASS" -e "SELECT 1;" &>/dev/null 2>&1 || return 1
    return 0
}

restart_mysql() {
    local count
    count=$(get_restart_count "$MYSQL_SERVICE")
    [[ $count -ge $MAX_RESTART_ATTEMPTS ]] && return 1
    systemctl restart "$MYSQL_SERVICE"
    increment_restart_count "$MYSQL_SERVICE"
    sleep "$RESTART_WAIT_SEC"
    systemctl is-active "$MYSQL_SERVICE" &>/dev/null
}

# ── 后端检查与重启 ────────────────────────────────────────
check_backend() {
    systemctl is-active "$BACKEND_SERVICE" &>/dev/null || return 1
    ss -tlnp | grep -q ":${BACKEND_PORT}" || return 1
    return 0
}

restart_backend() {
    local count
    count=$(get_restart_count "$BACKEND_SERVICE")
    [[ $count -ge $MAX_RESTART_ATTEMPTS ]] && return 1
    systemctl restart "$BACKEND_SERVICE"
    increment_restart_count "$BACKEND_SERVICE"
    sleep "$RESTART_WAIT_SEC"
    systemctl is-active "$BACKEND_SERVICE" &>/dev/null && ss -tlnp | grep -q ":${BACKEND_PORT}"
}

# ── 主流程 ────────────────────────────────────────────────
while true; do
    # 1. 检查 MySQL
    if ! check_mysql; then
        restart_mysql || true
    fi

    # 2. 检查后端（MySQL 正常后再检查，避免因 DB 连接失败误判）
    if check_mysql; then
        if ! check_backend; then
            restart_backend || true
        fi
    fi

    sleep "$CHECK_INTERVAL"
done
