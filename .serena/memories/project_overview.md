# 慕声报税管理系统 (Musheng Tax System)

## 项目简介
慕声亚马逊转口贸易报税管理系统，用于管理亚马逊 FBA 货件明细数据的导入、查询、导出等功能。

## 项目结构
- `musheng-tax-web/` - 前端 Vue 3 项目
- `musheng-tax-system/` - 后端 Spring Boot 项目（多模块）
  - `musheng-common` - 公共模块
  - `musheng-system` - 系统模块
  - `musheng-config` - 配置模块
  - `musheng-business` - 业务模块（FBA货件等）
  - `musheng-web` - Web 入口模块

## 技术栈

### 前端 (musheng-tax-web)
- Vue 3.4 + TypeScript
- Vite 5.2 构建工具
- Ant Design Vue 4.1 UI 组件库
- Vue Router 4 路由
- Pinia 状态管理
- Axios HTTP 客户端
- VXE-Table 表格组件
- ECharts 图表库
- SCSS 样式

### 后端 (musheng-tax-system)
- Java 17
- Spring Boot 3.2.2
- MyBatis-Plus 3.5.5
- Sa-Token 1.37.0 认证
- MySQL 8.0
- Knife4j 4.3.0 API 文档
- EasyExcel 3.3.3 Excel 处理
- Hutool 工具库

## 开发规范
参考 `.cursorrules` 文件，遵循 Superpowers 工作流程：
- 测试驱动开发（TDD）
- 系统化调试
- Conventional Commits 提交规范
