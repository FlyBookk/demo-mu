
#!/bin/bash

# FBA货件功能 - 一键部署脚本
# 执行此脚本完成所有必要的部署步骤

set -e  # 遇到错误立即退出

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║         FBA货件功能 - 一键部署脚本                            ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# 配置数据库连接信息
DB_USER="root"
DB_NAME="musheng_tax"

echo "📋 步骤 1/3: 执行数据库迁移"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 检查 SQL 文件是否存在
if [ ! -f "musheng-tax-system/sql/v2.2_fba_shipment_restructure.sql" ]; then
    echo "❌ 错误: 找不到 v2.2_fba_shipment_restructure.sql"
    exit 1
fi

if [ ! -f "musheng-tax-system/sql/v2.3_add_file_hash_to_import_record.sql" ]; then
    echo "❌ 错误: 找不到 v2.3_add_file_hash_to_import_record.sql"
    exit 1
fi

echo "请输入数据库密码:"
read -s DB_PASSWORD

echo ""
echo "正在执行数据库迁移..."

# 执行 v2.2 脚本
mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < musheng-tax-system/sql/v2.2_fba_shipment_restructure.sql
if [ $? -eq 0 ]; then
    echo "✅ v2.2 表结构重构完成"
else
    echo "❌ v2.2 执行失败"
    exit 1
fi

# 执行 v2.3 脚本
mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < musheng-tax-system/sql/v2.3_add_file_hash_to_import_record.sql
if [ $? -eq 0 ]; then
    echo "✅ v2.3 添加 file_hash 字段完成"
else
    echo "❌ v2.3 执行失败"
    exit 1
fi

echo ""
echo "📋 步骤 2/3: 验证数据库"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 验证表是否创建
TABLES=$(mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "SHOW TABLES LIKE 't_fba_shipment%';" -s)
if [[ $TABLES == *"t_fba_shipment"* ]] && [[ $TABLES == *"t_fba_shipment_item"* ]]; then
    echo "✅ 表结构验证通过"
    echo "   - t_fba_shipment (货件主表)"
    echo "   - t_fba_shipment_item (货件明细表)"
else
    echo "❌ 表结构验证失败"
    exit 1
fi

# 验证 file_hash 字段
FIELD=$(mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "DESCRIBE t_import_record;" -s | grep file_hash)
if [[ $FIELD == *"file_hash"* ]]; then
    echo "✅ file_hash 字段验证通过"
else
    echo "❌ file_hash 字段验证失败"
    exit 1
fi

echo ""
echo "📋 步骤 3/3: 后续操作提示"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✅ 数据库迁移完成！"
echo ""
echo "请执行以下操作："
echo ""
echo "1. 重启后端服务"
echo "   cd musheng-tax-system"
echo "   mvn spring-boot:run"
echo ""
echo "2. 刷新前端页面"
echo "   在浏览器中按 Ctrl+Shift+R (或 Cmd+Shift+R)"
echo ""
echo "3. 测试功能"
echo "   - 批量导入: http://localhost:3000/fba-shipment/import"
echo "   - 货件列表: http://localhost:3000/fba-shipment/list"
echo "   - SKU明细: 点击列表页的 'SKU明细视图' 按钮"
echo ""
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                    🎉 部署完成！                              ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
