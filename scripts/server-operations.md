# 慕声报税系统 — 服务器运维操作手册

## 一、后端服务管理（musheng）

### root 用户

```bash
# 启动
systemctl start musheng

# 停止
systemctl stop musheng

# 重启
systemctl restart musheng

# 查看状态
systemctl status musheng
```

### musheng 用户（需加 sudo）

```bash
sudo systemctl start musheng
sudo systemctl stop musheng
sudo systemctl restart musheng
sudo systemctl status musheng
```

---

## 二、日志查看

```bash
# 实时跟踪日志
journalctl -u musheng -f

# 查看最近 100 行
journalctl -n 100 -u musheng

# 查看今天的日志
journalctl -u musheng --since today

# 直接查看日志文件
tail -f /opt/musheng/logs/app.log

# 查看最近 200 行
tail -n 200 /opt/musheng/logs/app.log
```

---

## 三、Nginx 管理

```bash
# 重载配置（不中断服务）
systemctl reload nginx

# 重启
systemctl restart nginx

# 查看状态
systemctl status nginx

# 测试配置是否正确
nginx -t
```

---

## 四、MySQL 管理

```bash
# 启动 / 停止 / 重启
systemctl start mysqld
systemctl stop mysqld
systemctl restart mysqld

# 查看状态
systemctl status mysqld

# 登录（密码：root）
mysql -u root -p

# 导入 SQL 文件
mysql -u root -proot musheng_tax < musheng_tax.sql

# 备份数据库
mysqldump -u root -proot musheng_tax > musheng_tax_backup.sql
```

---

## 五、文件上传

```bash
# 上传后端 JAR
scp musheng-web-1.0.0-SNAPSHOT.jar musheng@<服务器IP>:/opt/musheng/backend/

# 上传前端静态文件
scp -r dist/* musheng@<服务器IP>:/opt/musheng/frontend/
```

---

## 六、目录结构

```
/opt/musheng/
├── backend/        # JAR 包
├── frontend/       # 前端静态文件
├── uploads/        # 上传文件
│   ├── chunks/     # 分片临时文件
│   ├── files/      # 正式文件
│   └── temp/       # 临时文件
├── backup/         # 备份
└── logs/           # 应用日志
```

---

## 七、端口说明

| 端口 | 服务 | 说明 |
|------|------|------|
| 80   | Nginx | 前端 + API 反向代理 |
| 8888 | Spring Boot | 后端服务（内网） |
| 3306 | MySQL | 数据库 |

---

## 八、常用排查命令

```bash
# 查看端口监听情况
ss -tlnp | grep -E ':80|:8888|:3306'

# 测试后端是否正常响应
curl http://localhost:8888/actuator/health

# 测试 Nginx 是否正常
curl -s -o /dev/null -w "HTTP %{http_code}" http://localhost/

# 查看系统资源
top
df -h        # 磁盘
free -h      # 内存
```

---

## 九、部署流程（首次）

```bash
# 1. 执行安装脚本（root）
bash install.sh

# 2. 上传 JAR 包
scp musheng-web-1.0.0-SNAPSHOT.jar musheng@<IP>:/opt/musheng/backend/

# 3. 上传前端
scp -r dist/* musheng@<IP>:/opt/musheng/frontend/

# 4. 导入数据库
mysql -u root -proot musheng_tax < musheng_tax.sql

# 5. 启动后端
systemctl start musheng

# 6. 查看日志确认启动成功
journalctl -u musheng -f
```

---

> 阿里云安全组需开放：TCP 80（HTTP）、TCP 22（SSH）、TCP 3306（MySQL 外部访问）
