# 常用命令

## 前端 (musheng-tax-web)

```bash
# 进入前端目录
cd musheng-tax-web

# 安装依赖
npm install

# 开发模式运行
npm run dev

# 构建生产版本
npm run build

# 预览构建结果
npm run preview

# ESLint 检查并修复
npm run lint

# Prettier 格式化
npm run format
```

## 后端 (musheng-tax-system)

```bash
# 进入后端目录
cd musheng-tax-system

# Maven 构建
mvn clean package -DskipTests

# Maven 运行测试
mvn test

# 运行应用（在 musheng-web 模块）
cd musheng-web
mvn spring-boot:run
```

## Git 常用命令

```bash
# 查看状态
git status

# 查看差异
git diff

# 提交（遵循 Conventional Commits）
git commit -m "feat: 添加新功能"
git commit -m "fix: 修复问题"
git commit -m "docs: 更新文档"
git commit -m "refactor: 重构代码"
```

## 系统工具 (macOS/Darwin)

```bash
# 查找文件
find . -name "*.vue"

# 搜索内容
grep -r "关键词" --include="*.ts"

# 查看目录结构
ls -la
```
