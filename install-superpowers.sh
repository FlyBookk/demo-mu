#!/bin/bash
# install-superpowers.sh
# 用法: ./install-superpowers.sh /path/to/superpowers

SUPERPOWERS_PATH="${1:-$HOME/project/superpowers}"

# 检查 superpowers 路径是否存在
if [ ! -d "$SUPERPOWERS_PATH" ]; then
    echo "错误: Superpowers 项目路径不存在: $SUPERPOWERS_PATH"
    echo "用法: ./install-superpowers.sh /path/to/superpowers"
    exit 1
fi

# 复制 .cursorrules
echo "正在创建 .cursorrules..."
cp "$SUPERPOWERS_PATH/docs/cursorrules-template.md" .cursorrules

# 创建技能目录
echo "正在创建技能目录..."
mkdir -p .cursor/superpowers

# 复制技能文件
echo "正在复制技能文件..."
cp "$SUPERPOWERS_PATH/skills/brainstorming/SKILL.md" .cursor/superpowers/brainstorming.md
cp "$SUPERPOWERS_PATH/skills/test-driven-development/SKILL.md" .cursor/superpowers/test-driven-development.md
cp "$SUPERPOWERS_PATH/skills/writing-plans/SKILL.md" .cursor/superpowers/writing-plans.md
cp "$SUPERPOWERS_PATH/skills/systematic-debugging/SKILL.md" .cursor/superpowers/systematic-debugging.md

echo ""
echo "✅ 安装完成！"
echo ""
echo "已创建:"
echo "  - .cursorrules"
echo "  - .cursor/superpowers/brainstorming.md"
echo "  - .cursor/superpowers/test-driven-development.md"
echo "  - .cursor/superpowers/writing-plans.md"
echo "  - .cursor/superpowers/systematic-debugging.md"
echo ""
echo "请重启 Cursor 或重新打开项目以生效。"