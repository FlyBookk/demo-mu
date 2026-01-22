# ngrok 远程访问配置指南

本文档介绍如何使用 ngrok 让外部网络访问本地开发环境的慕声报税系统。

---

## 目录

1. [背景说明](#1-背景说明)
2. [ngrok 安装](#2-ngrok-安装)
3. [基本概念](#3-基本概念)
4. [配置步骤](#4-配置步骤)
5. [常见问题](#5-常见问题)
6. [其他内网穿透方案](#6-其他内网穿透方案)

---

## 1. 背景说明

### 1.1 为什么需要暴露两个端口？

慕声报税系统是前后端分离架构：

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户浏览器（外网）                         │
├─────────────────────────────────────────────────────────────────┤
│  1. 访问前端 ngrok 地址                                          │
│     → 下载 HTML/JS/CSS 到用户浏览器                              │
│                                                                 │
│  2. 前端 JS 在【用户浏览器】中执行                                │
│     → 发起 API 请求到后端                                        │
│     → 这个请求是从【用户的电脑】发出的                            │
│     → 所以必须使用后端的公网地址，不能用 localhost               │
└─────────────────────────────────────────────────────────────────┘
```

**关键理解**：前端代码是在用户浏览器里运行的，不是在你的电脑上。当前端调用 `localhost:8080` 时，访问的是用户自己电脑的 8080 端口，而不是你的。

### 1.2 项目端口配置

| 服务 | 默认端口 | 说明 |
|------|---------|------|
| 前端 (Vue) | 3000 | Vite 开发服务器 |
| 后端 (Spring Boot) | 8080 | Java API 服务 |
| MySQL | 3306 | 数据库（无需暴露） |

---

## 2. ngrok 安装

### 2.1 macOS

```bash
# 使用 Homebrew 安装
brew install ngrok

# 或下载安装
# 访问 https://ngrok.com/download 下载对应版本
```

### 2.2 Windows

```powershell
# 使用 Chocolatey 安装
choco install ngrok

# 或使用 Scoop 安装
scoop install ngrok

# 或下载安装
# 访问 https://ngrok.com/download 下载 Windows 版本
# 解压后将 ngrok.exe 放入系统 PATH 目录
```

### 2.3 Linux

```bash
# Ubuntu/Debian
curl -s https://ngrok-agent.s3.amazonaws.com/ngrok.asc | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null
echo "deb https://ngrok-agent.s3.amazonaws.com buster main" | sudo tee /etc/apt/sources.list.d/ngrok.list
sudo apt update && sudo apt install ngrok

# 或下载安装
wget https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz
tar xvzf ngrok-v3-stable-linux-amd64.tgz
sudo mv ngrok /usr/local/bin/
```

### 2.4 注册并配置 authtoken

1. 访问 [https://dashboard.ngrok.com/signup](https://dashboard.ngrok.com/signup) 注册账号
2. 登录后在 [https://dashboard.ngrok.com/get-started/your-authtoken](https://dashboard.ngrok.com/get-started/your-authtoken) 获取 authtoken
3. 配置 authtoken：

```bash
ngrok config add-authtoken <你的authtoken>
```

---

## 3. 基本概念

### 3.1 ngrok 工作原理

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   你的电脑    │ ←── │  ngrok 云端   │ ←── │  外部用户    │
│ localhost:XX │     │ xxx.ngrok.io │     │   浏览器     │
└──────────────┘     └──────────────┘     └──────────────┘
```

ngrok 在你的电脑和 ngrok 云端之间建立一个安全隧道，外部用户通过 ngrok 提供的公网地址访问你的本地服务。

### 3.2 免费版限制

| 限制项 | 说明 |
|-------|------|
| 隧道数量 | 免费版每次只能开 1 个隧道（需要付费才能同时开多个） |
| 域名 | 每次启动域名会变化 |
| 连接数 | 有并发限制 |
| 带宽 | 有流量限制 |

**解决方案**：使用 ngrok 配置文件同时启动多个隧道。

---

## 4. 配置步骤

### 4.1 创建 ngrok 配置文件

创建 `~/.ngrok2/ngrok.yml`（如果使用 v3 则是 `~/.config/ngrok/ngrok.yml`）：

```yaml
version: "2"
authtoken: <你的authtoken>
tunnels:
  musheng-frontend:
    addr: 3000
    proto: http
  musheng-backend:
    addr: 8080
    proto: http
```

### 4.2 启动 ngrok 隧道

```bash
# 同时启动所有配置的隧道
ngrok start --all
```

启动后会显示类似：

```
Forwarding    https://abc123.ngrok.io -> http://localhost:3000
Forwarding    https://xyz789.ngrok.io -> http://localhost:8080
```

记录下这两个地址：
- 前端地址：`https://abc123.ngrok.io`
- 后端地址：`https://xyz789.ngrok.io`

### 4.3 创建前端 ngrok 环境配置

在 `musheng-tax-web/` 目录下创建 `.env.ngrok` 文件：

```env
# ngrok 远程访问环境配置
# 使用方式：npm run dev -- --mode ngrok

# 后端 API 地址（替换为你的后端 ngrok 地址）
VITE_API_BASE_URL=https://xyz789.ngrok.io
```

### 4.4 修改 package.json（可选）

在 `musheng-tax-web/package.json` 中添加便捷脚本：

```json
{
  "scripts": {
    "dev": "vite",
    "dev:ngrok": "vite --mode ngrok",
    "build": "vue-tsc && vite build",
    "preview": "vite preview"
  }
}
```

### 4.5 完整启动流程

#### 步骤 1：启动后端服务

```bash
cd musheng-tax-system/musheng-web
mvn spring-boot:run
# 或者在 IDE 中运行 MushengApplication.java
```

确保后端在 `localhost:8080` 运行。

#### 步骤 2：启动 ngrok 隧道

```bash
# 如果配置了 ngrok.yml
ngrok start --all

# 或者分别启动（需要两个终端）
# 终端1：
ngrok http 8080
# 终端2：
ngrok http 5173
```

#### 步骤 3：更新前端环境配置

将 ngrok 显示的后端地址（如 `https://xyz789.ngrok.io`）填入 `.env.ngrok`：

```env
VITE_API_BASE_URL=https://xyz789.ngrok.io
```

#### 步骤 4：启动前端服务

```bash
cd musheng-tax-web

# 使用 ngrok 模式启动
npm run dev -- --mode ngrok

# 或者如果配置了 package.json 脚本
npm run dev:ngrok
```

#### 步骤 5：分享访问地址

将前端的 ngrok 地址（如 `https://abc123.ngrok.io`）分享给需要访问的人。

---

## 5. 常见问题

### 5.1 CORS 跨域问题

如果出现跨域错误，需要在后端配置 CORS 允许 ngrok 域名。

修改 `WebMvcConfig.java`：

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOriginPatterns("*")  // 允许所有来源
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
}
```

或者添加更精确的配置：

```java
.allowedOrigins(
    "http://localhost:5173",
    "https://*.ngrok.io",
    "https://*.ngrok-free.app"
)
```

### 5.2 ngrok 免费版域名变化

每次重启 ngrok，域名都会变化。解决方案：

1. **付费版**：使用固定子域名
2. **临时方案**：每次启动后更新 `.env.ngrok` 中的后端地址

### 5.3 连接超时

如果遇到连接超时，可能是：

1. 本地服务未启动
2. 端口号配置错误
3. 防火墙阻止

检查命令：

```bash
# 检查端口是否在监听
lsof -i :3000
lsof -i :8080

# 或使用 netstat
netstat -an | grep 3000
netstat -an | grep 8080
```

### 5.4 文件上传大小限制

ngrok 免费版有请求大小限制。如果需要上传大文件，考虑：

1. 使用付费版
2. 分片上传
3. 使用其他内网穿透方案

### 5.5 HTTPS 证书警告

ngrok 提供的是有效的 HTTPS 证书，通常不会有警告。如果遇到问题，检查：

1. 是否使用了 `https://` 而非 `http://`
2. 浏览器是否有安全设置阻止

---

## 6. 其他内网穿透方案

如果 ngrok 不满足需求，以下是替代方案：

### 6.1 Cloudflare Tunnel（推荐，免费）

```bash
# 安装
brew install cloudflared

# 快速隧道（无需账号）
cloudflared tunnel --url http://localhost:3000
cloudflared tunnel --url http://localhost:8080
```

优点：免费、无连接数限制、Cloudflare CDN 加速

### 6.2 localtunnel（免费）

```bash
# 安装
npm install -g localtunnel

# 使用
lt --port 3000
lt --port 8080
```

### 6.3 frp（需要公网服务器）

如果有自己的云服务器，可以搭建 frp：

1. 服务端配置 `frps.ini`
2. 客户端配置 `frpc.ini`
3. 无域名变化、完全可控

### 6.4 Tailscale/ZeroTier（组网方案）

适合长期协作的团队：

1. 创建虚拟局域网
2. 所有成员加入同一网络
3. 通过虚拟 IP 直接访问

---

## 附录

### A. 快速启动脚本

创建 `scripts/start-ngrok.sh`：

```bash
#!/bin/bash

echo "=== 慕声报税系统 - ngrok 远程访问 ==="
echo ""

# 检查 ngrok 是否安装
if ! command -v ngrok &> /dev/null; then
    echo "错误：ngrok 未安装，请先安装 ngrok"
    echo "安装命令：brew install ngrok"
    exit 1
fi

# 检查后端是否运行
if ! lsof -i :8080 &> /dev/null; then
    echo "警告：后端服务（端口 8080）未运行"
    echo "请先启动后端：cd musheng-tax-system/musheng-web && mvn spring-boot:run"
fi

# 检查前端是否运行
if ! lsof -i :3000 &> /dev/null; then
    echo "警告：前端服务（端口 3000）未运行"
    echo "请先启动前端：cd musheng-tax-web && npm run dev"
fi

echo ""
echo "正在启动 ngrok 隧道..."
echo "启动后请将后端地址更新到 musheng-tax-web/.env.ngrok"
echo ""

ngrok start --all
```

### B. 环境配置文件汇总

| 文件 | 用途 | API 地址 |
|------|------|---------|
| `.env.development` | 本地开发 | `http://localhost:8080` |
| `.env.production` | 生产环境 | `/api`（通过 nginx 代理） |
| `.env.ngrok` | ngrok 远程访问 | `https://xxx.ngrok.io` |

### C. 检查清单

启动远程访问前确认：

- [ ] ngrok 已安装并配置 authtoken
- [ ] 后端服务正在运行（端口 8080）
- [ ] 前端服务正在运行（端口 3000）
- [ ] `.env.ngrok` 已更新后端 ngrok 地址
- [ ] 后端 CORS 配置允许 ngrok 域名
- [ ] 使用 `--mode ngrok` 启动前端

---

## 更新记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-01-22 | v1.0 | 初始版本 |
