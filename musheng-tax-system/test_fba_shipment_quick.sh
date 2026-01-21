#!/bin/bash

# FBA货件明细模块后端接口快速测试脚本
# 使用方法: ./test_fba_shipment_quick.sh

set -e  # 遇到错误立即退出

# ==================== 配置 ====================
BASE_URL="http://localhost:8080/api/v1/business/fba-shipment"
CSV_FILE="/Users/wanhua/Documents/慕声/musheng/慕声加拿大2025年3-12月FBA货件明细表.csv"

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ==================== 函数定义 ====================

# 获取Token（需要根据实际登录接口调整）
get_token() {
    echo -e "${YELLOW}[步骤0] 获取认证Token...${NC}"

    # 方式1: 如果有登录接口
    # TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
    #   -H "Content-Type: application/json" \
    #   -d '{"username":"admin","password":"admin123"}' | jq -r '.data.token')

    # 方式2: 手动设置Token（临时测试用）
    echo -e "${YELLOW}请输入Token (或按Enter跳过认证):${NC}"
    read TOKEN

    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}未设置Token，将不使用Authorization header${NC}"
        AUTH_HEADER=""
    else
        AUTH_HEADER="Authorization: Bearer $TOKEN"
        echo -e "${GREEN}Token已设置${NC}"
    fi
}

# 打印分隔线
print_separator() {
    echo -e "\n${YELLOW}================================================${NC}"
    echo -e "${YELLOW}$1${NC}"
    echo -e "${YELLOW}================================================${NC}\n"
}

# 检查响应状态
check_response() {
    local response=$1
    local test_name=$2

    if echo "$response" | grep -q '"code":200'; then
        echo -e "${GREEN}✓ $test_name - 成功${NC}"
        return 0
    else
        echo -e "${RED}✗ $test_name - 失败${NC}"
        echo "$response"
        return 1
    fi
}

# ==================== 前置检查 ====================

echo -e "${YELLOW}FBA货件明细模块后端接口快速测试${NC}"
echo -e "${YELLOW}======================================${NC}\n"

# 检查后端服务
print_separator "检查后端服务状态"
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 后端服务运行正常${NC}"
else
    echo -e "${RED}✗ 后端服务未启动或不可访问${NC}"
    echo -e "${YELLOW}请先启动后端服务: http://localhost:8080${NC}"
    exit 1
fi

# 检查CSV文件
if [ ! -f "$CSV_FILE" ]; then
    echo -e "${RED}✗ CSV文件不存在: $CSV_FILE${NC}"
    echo -e "${YELLOW}请确认CSV文件路径${NC}"
    exit 1
else
    echo -e "${GREEN}✓ CSV文件存在${NC}"
fi

# 获取Token
get_token

# ==================== 测试用例 ====================

# 测试1: CSV导入
print_separator "测试1: CSV文件导入"
echo "正在上传CSV文件..."
IMPORT_RESPONSE=$(curl -s -X POST "$BASE_URL/import" \
  ${AUTH_HEADER:+-H "$AUTH_HEADER"} \
  -F "file=@$CSV_FILE")

echo "$IMPORT_RESPONSE" | jq '.'
check_response "$IMPORT_RESPONSE" "CSV导入"

# 提取批次号和导入统计
BATCH_NO=$(echo "$IMPORT_RESPONSE" | jq -r '.data.batchNo')
TOTAL_COUNT=$(echo "$IMPORT_RESPONSE" | jq -r '.data.totalCount')
SUCCESS_COUNT=$(echo "$IMPORT_RESPONSE" | jq -r '.data.successCount')
FAIL_COUNT=$(echo "$IMPORT_RESPONSE" | jq -r '.data.failCount')
DUPLICATE_COUNT=$(echo "$IMPORT_RESPONSE" | jq -r '.data.duplicateCount')

echo -e "\n导入结果统计:"
echo -e "  批次号: ${GREEN}$BATCH_NO${NC}"
echo -e "  总行数: $TOTAL_COUNT"
echo -e "  成功: ${GREEN}$SUCCESS_COUNT${NC}"
echo -e "  失败: ${RED}$FAIL_COUNT${NC}"
echo -e "  重复: ${YELLOW}$DUPLICATE_COUNT${NC}"

# 等待1秒
sleep 1

# 测试2: 重复导入检测
print_separator "测试2: 重复导入检测"
echo "再次导入同一文件（应全部检测为重复）..."
DUPLICATE_RESPONSE=$(curl -s -X POST "$BASE_URL/import" \
  ${AUTH_HEADER:+-H "$AUTH_HEADER"} \
  -F "file=@$CSV_FILE")

echo "$DUPLICATE_RESPONSE" | jq '.'
DUP_COUNT=$(echo "$DUPLICATE_RESPONSE" | jq -r '.data.duplicateCount')

if [ "$DUP_COUNT" == "$TOTAL_COUNT" ]; then
    echo -e "${GREEN}✓ 重复检测正确: $DUP_COUNT 条重复${NC}"
else
    echo -e "${RED}✗ 重复检测异常: 预期$TOTAL_COUNT条重复，实际$DUP_COUNT条${NC}"
fi

# 测试3: 列表查询
print_separator "测试3: 列表查询（分页）"
LIST_RESPONSE=$(curl -s -X GET "$BASE_URL?page=1&size=5" \
  ${AUTH_HEADER:+-H "$AUTH_HEADER"})

echo "$LIST_RESPONSE" | jq '.'
check_response "$LIST_RESPONSE" "列表查询"

TOTAL=$(echo "$LIST_RESPONSE" | jq -r '.data.total')
echo -e "\n总记录数: ${GREEN}$TOTAL${NC}"

# 测试4: 按状态筛选
print_separator "测试4: 按状态筛选"
FILTER_RESPONSE=$(curl -s -X GET "$BASE_URL?status=已完成&page=1&size=3" \
  ${AUTH_HEADER:+-H "$AUTH_HEADER"})

echo "$FILTER_RESPONSE" | jq '.data.records[] | {shipmentId, status}'
check_response "$FILTER_RESPONSE" "状态筛选"

# 测试5: 详情查询
print_separator "测试5: 详情查询"
# 获取第一条记录的ID
FIRST_ID=$(echo "$LIST_RESPONSE" | jq -r '.data.records[0].id')

if [ "$FIRST_ID" != "null" ] && [ -n "$FIRST_ID" ]; then
    echo "查询ID=$FIRST_ID的详情..."
    DETAIL_RESPONSE=$(curl -s -X GET "$BASE_URL/$FIRST_ID" \
      ${AUTH_HEADER:+-H "$AUTH_HEADER"})

    echo "$DETAIL_RESPONSE" | jq '.'
    check_response "$DETAIL_RESPONSE" "详情查询"
else
    echo -e "${YELLOW}跳过详情查询（无数据）${NC}"
fi

# 测试6: 新增记录
print_separator "测试6: 新增记录"
ADD_RESPONSE=$(curl -s -X POST "$BASE_URL" \
  ${AUTH_HEADER:+-H "$AUTH_HEADER"} \
  -H "Content-Type: application/json" \
  -d '{
    "shipmentName": "TEST-AUTO-001",
    "shipmentId": "TEST-FBA-AUTO-001",
    "createdDate": "2025-01-21T10:00:00",
    "receivingAddress": "TEST1",
    "skuCount": 10,
    "expectedQuantity": 100,
    "foundQuantity": 98,
    "status": "测试中"
  }')

echo "$ADD_RESPONSE" | jq '.'
NEW_ID=$(echo "$ADD_RESPONSE" | jq -r '.data')

if check_response "$ADD_RESPONSE" "新增记录"; then
    echo -e "新记录ID: ${GREEN}$NEW_ID${NC}"

    # 测试7: 更新记录
    print_separator "测试7: 更新记录"
    UPDATE_RESPONSE=$(curl -s -X PUT "$BASE_URL/$NEW_ID" \
      ${AUTH_HEADER:+-H "$AUTH_HEADER"} \
      -H "Content-Type: application/json" \
      -d '{
        "shipmentName": "TEST-AUTO-001-UPDATED",
        "shipmentId": "TEST-FBA-AUTO-001",
        "status": "已完成",
        "foundQuantity": 100
      }')

    echo "$UPDATE_RESPONSE" | jq '.'
    check_response "$UPDATE_RESPONSE" "更新记录"

    # 测试8: 删除记录
    print_separator "测试8: 删除记录"
    DELETE_RESPONSE=$(curl -s -X DELETE "$BASE_URL/$NEW_ID" \
      ${AUTH_HEADER:+-H "$AUTH_HEADER"})

    echo "$DELETE_RESPONSE" | jq '.'
    check_response "$DELETE_RESPONSE" "删除记录"
fi

# 测试9: 统计汇总
print_separator "测试9: 统计汇总"
SUMMARY_RESPONSE=$(curl -s -X GET "$BASE_URL/summary" \
  ${AUTH_HEADER:+-H "$AUTH_HEADER"})

echo "$SUMMARY_RESPONSE" | jq '.'
check_response "$SUMMARY_RESPONSE" "统计汇总"

# 提取统计数据
TOTAL_SHIPMENTS=$(echo "$SUMMARY_RESPONSE" | jq -r '.data.totalShipments')
TOTAL_SKU=$(echo "$SUMMARY_RESPONSE" | jq -r '.data.totalSkuCount')
TOTAL_EXPECTED=$(echo "$SUMMARY_RESPONSE" | jq -r '.data.totalExpectedQuantity')
TOTAL_FOUND=$(echo "$SUMMARY_RESPONSE" | jq -r '.data.totalFoundQuantity')

echo -e "\n统计汇总:"
echo -e "  总货件数: ${GREEN}$TOTAL_SHIPMENTS${NC}"
echo -e "  总SKU数: ${GREEN}$TOTAL_SKU${NC}"
echo -e "  预计总数量: ${GREEN}$TOTAL_EXPECTED${NC}"
echo -e "  实际总数量: ${GREEN}$TOTAL_FOUND${NC}"

# 测试10: 导出Excel
print_separator "测试10: 导出Excel"
EXPORT_FILE="fba_shipment_export_$(date +%Y%m%d_%H%M%S).xlsx"
echo "正在导出到: $EXPORT_FILE"

curl -s -X GET "$BASE_URL/export" \
  ${AUTH_HEADER:+-H "$AUTH_HEADER"} \
  -o "$EXPORT_FILE"

if [ -f "$EXPORT_FILE" ] && [ -s "$EXPORT_FILE" ]; then
    FILE_SIZE=$(ls -lh "$EXPORT_FILE" | awk '{print $5}')
    echo -e "${GREEN}✓ 导出成功${NC}"
    echo -e "  文件: $EXPORT_FILE"
    echo -e "  大小: $FILE_SIZE"
else
    echo -e "${RED}✗ 导出失败${NC}"
fi

# ==================== 测试总结 ====================

print_separator "测试总结"
echo -e "${GREEN}所有测试执行完成！${NC}"
echo -e "\n请检查以上测试结果，确认："
echo -e "  1. CSV导入功能正常（编码识别、日期解析）"
echo -e "  2. 重复检测准确（全部检测为重复）"
echo -e "  3. CRUD操作正常"
echo -e "  4. 统计和导出功能正常"
echo -e "\n如需查看详细日志，请检查后端服务日志。"
echo -e "\n${YELLOW}下一步: 如果所有测试通过，可以开始前端开发${NC}"
echo -e "${YELLOW}命令: /agent 前端${NC}\n"
