#!/bin/bash
# 将 iTerm2 窗口调整为 1/4 屏幕
# 用法: ./iterm2-quarter-screen.sh 或 source 后调用

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
osascript "$SCRIPT_DIR/iterm2-resize-quarter-screen.scpt"
