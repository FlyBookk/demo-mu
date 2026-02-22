# 在 Kiro 中配置与运行 Java 项目（慕声税务系统）

本文说明**仅在 Kiro** 中打开、配置并运行慕声税务系统（Java/Spring Boot）的步骤。

---

## 一、环境要求

| 项目 | 要求 |
|------|------|
| JDK | **17**（与 `pom.xml` 中 `java.version` 一致） |
| Maven | 3.6+（用于编译、运行） |
| MySQL | 8.x，库名 `musheng_tax`（见 `application.yml`） |
| Node（可选） | 仅当需要跑前端 `musheng-tax-web` 时 |

---

## 二、安装与打开项目

### 2.1 安装 Kiro

- 从 [Kiro 官网 Downloads](https://kiro.dev/downloads/) 下载并安装对应系统版本。
- 详细步骤：[Installation - Kiro Docs](https://kiro.dev/docs/getting-started/installation)。

### 2.2 在 Kiro 中打开项目

1. 启动 **Kiro**。
2. **文件 → 打开文件夹**（或 Open Folder），选择本仓库根目录 **`musheng`**；若你只关心后端，也可只打开 **`musheng-tax-system`**。
3. 等待 Kiro 识别 Maven 多模块项目（根 `pom.xml` 在 `musheng-tax-system` 下），索引完成后即可编辑和运行。

### 2.3 配置 JDK 17

- Kiro 基于 VS Code，一般会使用系统或项目配置的 Java 运行时。
- 在 Kiro 中：**命令面板**（`Cmd+Shift+P` / `Ctrl+Shift+P`）→ 输入 **`java:`** → 选择 **“Java: Add Java Runtime”**，按提示添加 **JDK 17** 的安装路径（如 `/Library/Java/JavaVirtualMachines/xxx.jdk/Contents/Home`），并可在运行时列表里设为默认。
- 或在项目/用户 `settings.json` 中配置：
  ```json
  {
    "java.configuration.runtimes": [
      {
        "name": "JavaSE-17",
        "path": "/path/to/jdk-17",
        "default": true
      }
    }
  ```
  将 `/path/to/jdk-17` 换成你本机 JDK 17 路径（如 macOS：`/Library/Java/JavaVirtualMachines/xxx.jdk/Contents/Home`）。

---

## 三、准备数据库

- 启动 **MySQL**，创建库（若尚未创建）：
  ```sql
  CREATE DATABASE IF NOT EXISTS musheng_tax
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```
- 默认连接：`localhost:3306`，用户 `root`，密码由环境变量 **`DB_PASSWORD`** 指定，未设置时为 `root`（见 `musheng-tax-system/musheng-web/src/main/resources/application.yml`）。

---

## 四、在 Kiro 中运行后端

### 4.1 用“运行和调试”面板（推荐）

Kiro 里 Java 的 Run/Debug **不一定在右键菜单**，请用下面任一方式：

**方式 A：侧栏“运行和调试”**
1. 左侧点 **运行和调试** 图标（三角+虫子），或 `Ctrl+Shift+D` / `Cmd+Shift+D`。
2. 在顶部下拉里选 **“启动慕声税务系统 (MushengApplication)”**（项目已配好 `.vscode/launch.json`）。
3. 点绿色 **运行** 或 **调试** 按钮启动。
4. 控制台出现 “慕声税务系统启动成功!” 即表示启动完成。

**方式 B：主类上方的 Code Lens**
- 打开 `MushengApplication.java`，若 Java 扩展正常，在 `main` 方法**上方**会有一行小字 **“Run | Debug”**，直接点击即可（若看不到，多半是项目有编译错误或扩展未就绪，先看下方“常见问题”）。

**方式 C：命令面板**
- `Cmd+Shift+P` / `Ctrl+Shift+P` → 输入 **“Debug: Select and Start Debugging”** 或 **“Run: Run Without Debugging”**，再选对应配置。

### 4.2 用 Kiro 内置终端 + Maven

1. 在 Kiro 中打开终端（`` Ctrl+` `` 或 视图 → 终端）。
2. 在项目根目录 `musheng` 下执行：

```bash
cd musheng-tax-system
mvn -pl musheng-web spring-boot:run
```

或从仓库根目录指定模块：

```bash
mvn -f musheng-tax-system/pom.xml -pl musheng-web spring-boot:run
```

首次会下载依赖，完成后应用在 **8080** 端口、上下文路径 **`/api`** 启动，例如：

- 接口根：`http://localhost:8080/api`
- Knife4j 文档（若已启用）：一般为 `http://localhost:8080/api/doc.html`（以实际配置为准）。

### 4.3 仅编译不运行

```bash
cd musheng-tax-system
mvn clean compile
# 或打包
mvn clean package -DskipTests
```

---

## 五、可选：运行前端（musheng-tax-web）

若需在 Kiro 里同时跑前端，在 Kiro 终端中：

```bash
cd musheng-tax-web
npm install
npm run dev
```

前端需与后端 `application.yml` 中的 `server.port`、`context-path` 及前端的 API 基地址一致。

---

## 六、常见问题

| 现象 | 处理建议 |
|------|----------|
| Kiro 里 Java 不识别 / 报错 | 用 “Java: Add Java Runtime” 添加 JDK 17；仍异常时执行 “Java: Clean Java Language Server Workspace” 后重载窗口。 |
| 没有 Run/Debug 或 main 上方无 “Run \| Debug” | 用**运行和调试**侧栏（`Cmd+Shift+D`），选 “启动慕声税务系统”；若项目有大量编译错误（如 PROBLEMS 里很多红字），先 `mvn clean compile -DskipTests` 修到能编译通过再试。 |
| Maven 依赖下载失败 | 检查网络与 Maven 镜像（如阿里云），或配置公司/本地仓库。 |
| 启动报数据库连接错误 | 确认 MySQL 已启动、库 `musheng_tax` 已建、用户名密码正确，或设置 `DB_PASSWORD`。 |
| 端口 8080 被占用 | 在 `musheng-web/src/main/resources/application.yml` 中修改 `server.port`。 |

---

## 七、参考链接

- [Kiro 安装](https://kiro.dev/docs/getting-started/installation)
- [Kiro 第一个项目](https://kiro.dev/docs/getting-started/first-project)
- [Kiro Java 指南](https://kiro.dev/docs/guides/languages-and-frameworks/java-guide/)
- 项目内 Maven 规范：`.kiro/steering/maven-config.md`（若本地存在 `.kiro`）
