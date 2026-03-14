# MySQL 服务器操作命令大全

## 一、连接与登录

```bash
# 本地登录
mysql -u root -p

# 指定主机和端口
mysql -h 127.0.0.1 -P 3306 -u root -p

# 直接指定数据库
mysql -u root -p mydb

# 免密登录（不推荐生产环境）
mysql -u root -p'yourpassword'
```

---

## 二、服务管理

```bash
# systemd 系统（CentOS 7+, Ubuntu 16+）
systemctl start mysql
systemctl stop mysql
systemctl restart mysql
systemctl status mysql
systemctl enable mysql   # 开机自启

# 旧版 service 命令
service mysql start
service mysql stop
service mysql restart
```

---

## 三、数据库操作

```sql
-- 查看所有数据库
SHOW DATABASES;

-- 创建数据库（推荐 utf8mb4）
CREATE DATABASE mydb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 选择数据库
USE mydb;

-- 删除数据库
DROP DATABASE mydb;

-- 查看当前数据库
SELECT DATABASE();
```

---

## 四、用户与权限

```sql
-- 查看所有用户
SELECT user, host FROM mysql.user;

-- 创建用户
CREATE USER 'username'@'%' IDENTIFIED BY 'password';
CREATE USER 'username'@'localhost' IDENTIFIED BY 'password';

-- 授权
GRANT ALL PRIVILEGES ON mydb.* TO 'username'@'%';
GRANT SELECT, INSERT, UPDATE ON mydb.* TO 'username'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 撤销权限
REVOKE ALL PRIVILEGES ON mydb.* FROM 'username'@'%';

-- 删除用户
DROP USER 'username'@'%';

-- 修改密码（MySQL 8+）
ALTER USER 'root'@'localhost' IDENTIFIED BY 'newpassword';
```

---

## 五、备份与恢复

```bash
# 备份单个数据库
mysqldump -u root -p mydb > mydb_backup.sql

# 备份所有数据库
mysqldump -u root -p --all-databases > all_backup.sql

# 备份（带事务，适合 InnoDB）
mysqldump -u root -p --databases mydb --single-transaction > mydb.sql

# 恢复数据库
mysql -u root -p mydb < mydb_backup.sql

# 压缩备份
mysqldump -u root -p mydb | gzip > mydb_backup.sql.gz

# 解压恢复
gunzip < mydb_backup.sql.gz | mysql -u root -p mydb
```

---

## 六、表操作

```sql
-- 查看所有表
SHOW TABLES;

-- 查看表结构
DESC tablename;
SHOW CREATE TABLE tablename;

-- 查看各表大小（MB）
SELECT table_name, ROUND(data_length/1024/1024, 2) AS 'MB'
FROM information_schema.tables
WHERE table_schema = 'mydb'
ORDER BY data_length DESC;
```

---

## 七、进程与性能

```sql
-- 查看当前连接
SHOW PROCESSLIST;

-- 杀掉某个连接
KILL 123;

-- 查看最大连接数
SHOW GLOBAL VARIABLES LIKE 'max_connections';

-- 查看当前连接数
SHOW GLOBAL STATUS LIKE 'Threads_connected';

-- 开启慢查询日志
SHOW VARIABLES LIKE 'slow_query_log';
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;
```

---

## 八、日志与配置

```bash
# 查看配置文件位置
mysql --help | grep my.cnf

# 常见配置文件路径
/etc/mysql/my.cnf
/etc/my.cnf
/etc/mysql/mysql.conf.d/mysqld.cnf

# 查看错误日志
tail -f /var/log/mysql/error.log
tail -f /var/log/mysqld.log
```

---

## 九、导入导出数据

```bash
# 导入 SQL 文件
mysql -u root -p mydb < data.sql

# 导出查询结果到文件
mysql -u root -p -e "SELECT * FROM mytable" mydb > output.txt
```

```sql
-- 在 MySQL 内导出 CSV
SELECT * INTO OUTFILE '/tmp/data.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
FROM mytable;
```

---

## 十、重置 root 密码（忘记密码）

```bash
# 1. 停止 MySQL
systemctl stop mysql

# 2. 跳过权限验证启动
mysqld_safe --skip-grant-tables &

# 3. 无密码登录
mysql -u root

# 4. 重置密码
FLUSH PRIVILEGES;
ALTER USER 'root'@'localhost' IDENTIFIED BY 'newpassword';

# 5. 重启服务
systemctl restart mysql
```

---

## 十一、常用查询技巧

```sql
-- 查看 MySQL 版本
SELECT VERSION();

-- 查看当前时间
SELECT NOW();

-- 查看数据库字符集
SHOW VARIABLES LIKE 'character_set%';

-- 查看存储引擎
SHOW ENGINES;

-- 分析查询性能
EXPLAIN SELECT * FROM mytable WHERE id = 1;

-- 查看索引
SHOW INDEX FROM tablename;

-- 创建索引
CREATE INDEX idx_name ON tablename(column_name);

-- 删除索引
DROP INDEX idx_name ON tablename;
```

---

> 文档生成时间：2026-03-14  
> 适用版本：MySQL 5.7 / 8.0+
