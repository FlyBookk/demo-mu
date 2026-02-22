# 启动项目后的接口与页面测试指南

项目启动后，可以用下面几类方式**直接模拟请求或操作**做接口测试、PC 页面测试。

---

## 一、接口测试（API）

### 1. Postman / Insomnia（推荐，可保存用例）

- **Base URL**：后端已启动时为 `http://localhost:8080/api`（若改过端口以 `application.yml` 为准）。
- 在 Postman 里建 Environment，设 `base_url = http://localhost:8080/api`，请求用 `{{base_url}}/v1/...`。
- 项目里已提供 **Postman Collection**：`scripts/postman/musheng-tax-api.json`，导入后即可对主要接口做请求与回归。

**示例请求：**

- 健康/存活（若有）：`GET {{base_url}}/actuator/health` 或 `GET {{base_url}}/...`（以实际为准）
- 业务接口：如 `GET/POST {{base_url}}/v1/admin/data-deletion/...`（路径以 Knife4j 或代码为准）

### 2. cURL（命令行，无需安装 GUI）

在项目根目录或任意终端执行（需先启动后端）：

```bash
# 示例：GET 请求（请把 /v1/xxx 换成实际路径）
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/xxx

# 带 JSON body 的 POST 示例
curl -X POST http://localhost:8080/api/v1/xxx \
  -H "Content-Type: application/json" \
  -d '{"key":"value"}'
```

更多示例见 `scripts/api-test-examples.sh`（若有）。

### 3. Knife4j 在线调试（本项目已集成）

- 后端启动后访问：**http://localhost:8080/api/doc.html**
- 在文档页里可直接“调试”各接口，等同于在浏览器里模拟请求，无需额外工具。

### 4. 集成测试（JUnit + TestRestTemplate）

- 已有模块：`musheng-business`、`musheng-system` 等带单元/集成测试。
- 若要“启动完整应用再打接口”的集成测试，可在 `musheng-web` 下加 `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` + `TestRestTemplate` 调 `http://localhost:${port}/api/...`，适合 CI 或本地 `mvn test`。

---

## 二、PC 页面测试（模拟用户操作）

### 1. Playwright（推荐：无头/有头、稳定、可脚本化）

- **作用**：启动浏览器（或无头），访问前端页面，模拟点击、输入、导航等，做 E2E 回归。
- **位置**：前端工程 `musheng-tax-web` 下已预留 **Playwright** 配置与示例用例。

**使用步骤：**

1. 确保后端已启动（8080）、前端已启动（如 `npm run dev`，默认 3000）。
2. 进入前端目录并安装依赖（若未装过 Playwright）：
   ```bash
   cd musheng-tax-web
   npm install
   npx playwright install
   ```
3. 运行 E2E：
   ```bash
   npx playwright test
   ```
4. 带界面调试：
   ```bash
   npx playwright test --ui
   ```
   或对单文件：
   ```bash
   npx playwright test e2e/smoke.spec.ts
   ```

- 配置与示例用例：`musheng-tax-web/playwright.config.ts`、`musheng-tax-web/e2e/smoke.spec.ts`（见下文“已添加文件”）。

### 2. Cypress（可选）

- 若更习惯 Cypress，可在 `musheng-tax-web` 中安装 `cypress`，写 `cypress/e2e/*.cy.ts`，在 `npm run dev` 与后端都启动后运行 `npx cypress open` 做页面模拟。本项目当前以 Playwright 为主。

### 3. 浏览器手动测试

- 前端：`http://localhost:3000`（Vite 默认）。
- 登录、各菜单（销售、Shipping、FBA 等）按业务手动点选，配合 Knife4j 或 Network 面板核对接口。

---

## 三、推荐工作流（启动后直接测）

1. **只测接口**  
   启动后端 → 打开 **http://localhost:8080/api/doc.html** 用 Knife4j 调接口；或导入 `scripts/postman/musheng-tax-api.json` 用 Postman 调。

2. **只测页面**  
   启动后端 + 前端 → 浏览器打开 **http://localhost:3000** 手动操作；或运行 `cd musheng-tax-web && npx playwright test` 做自动化 E2E。

3. **接口 + 页面一起测**  
   先启后端再启前端，Knife4j 测接口 + Playwright 跑 E2E（或 Cypress），必要时在 Playwright 里加“等待某接口返回”的断言。

---

## 四、已添加/可用的文件一览

| 文件 | 用途 |
|------|------|
| `scripts/postman/musheng-tax-api.json` | Postman Collection，导入后直接请求项目 API |
| `scripts/api-test-examples.sh` | 常用接口的 curl 示例（可选） |
| `musheng-tax-web/playwright.config.ts` | Playwright 配置，baseURL 指向本地前端 |
| `musheng-tax-web/e2e/smoke.spec.ts` | 一条简单 E2E（如访问首页/登录页），可复制扩展 |

---

## 五、常见问题

| 现象 | 处理 |
|------|------|
| 接口 404 | 确认 base 为 `http://localhost:8080/api`，路径与 Knife4j 一致（含 `/api` 前缀）。 |
| 前端 E2E 超时 | 确认后端、前端都已启动；`playwright.config.ts` 里 `baseURL` 为 `http://localhost:3000`（或你改的端口）。 |
| 登录态 | E2E 若需登录，可在 `beforeEach` 里先访问登录页、填表、提交，或使用项目约定的 token 注入。 |

以上配置可以让你在**启动项目后**直接通过 Knife4j/Postman/curl 测接口，通过 Playwright 或浏览器测 PC 页面；按需选用即可。
