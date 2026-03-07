-- 将 iTerm2 当前窗口调整为 1/4 屏幕大小（左上角）
-- 使用方式：osascript "本脚本路径" 或 在 iTerm2 中执行
-- 首次运行需在 系统设置 > 隐私与安全性 > 辅助功能 中授权 Terminal/Cursor

tell application "iTerm" to activate

delay 0.3

-- 获取主屏幕尺寸（通过 Finder 桌面 bounds）
tell application "Finder"
	set desktopBounds to bounds of window of desktop
	set screenWidth to (item 3 of desktopBounds) - (item 1 of desktopBounds)
	set screenHeight to (item 4 of desktopBounds) - (item 2 of desktopBounds)
end tell

-- 1/4 屏 = 半宽 x 半高
set quarterWidth to screenWidth / 2
set quarterHeight to screenHeight / 2

-- 左上角位置
set posX to 20
set posY to 50

tell application "System Events"
	tell process "iTerm2"
		set frontWindow to front window
		set position of frontWindow to {posX, posY}
		set size of frontWindow to {quarterWidth, quarterHeight}
	end tell
end tell
