# 汇率自动同步功能说明

## 功能概述

汇率模块已升级为从**中国外汇交易中心**自动同步汇率数据，支持手动触发和定时自动同步两种方式。

### 核心特性

1. **自动同步**：每天早上8:00自动从外汇交易中心同步最近3天的汇率
2. **关联货币管理**：只同步货币管理中已启用的货币
3. **手动触发**：提供REST API接口支持手动触发同步
4. **智能更新**：自动判断新增/更新汇率数据
5. **错误处理**：完善的错误处理和日志记录

---

## 一、配置说明

### 1.1 application.yml配置

```yaml
# 汇率同步配置
rate:
  sync:
    # 是否启用自动同步 (true/false)
    enabled: true
    # 同步时间 Cron 表达式 (默认: 每天早上8:00)
    cron: "0 0 8 * * ?"
    check:
      # 是否启用汇率数据检查 (true/false)
      enabled: false
      # 检查时间 Cron 表达式 (默认: 每小时)
      cron: "0 0 * * * ?"
```

### 1.2 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `rate.sync.enabled` | 是否启用自动同步 | `true` |
| `rate.sync.cron` | 同步时间Cron表达式 | `0 0 8 * * ?` (每天8:00) |
| `rate.sync.check.enabled` | 是否启用定时检查 | `false` |
| `rate.sync.check.cron` | 检查时间Cron表达式 | `0 0 * * * ?` (每小时) |

---

## 二、货币管理配置

### 2.1 启用货币

汇率同步只会同步**货币管理中已启用的货币**。

**操作步骤**：
1. 访问系统管理 → 货币管理
2. 查看货币列表，确保需要同步的货币状态为"启用"
3. 如需添加新货币，点击"新增货币"并设置状态为"启用"

**支持的货币**：
- USD (美元)
- EUR (欧元)
- GBP (英镑)
- CAD (加元)
- JPY (日元)
- AUD (澳元)
- NZD (新西兰元)
- SGD (新加坡元)
- CHF (瑞士法郎)
- HKD (港币)
- 等其他外汇中心支持的货币

---

## 三、API接口说明

### 3.1 同步指定日期范围汇率

**接口**：`POST /api/v1/business/rates/sync`

**参数**：
- `startDate`: 开始日期 (YYYY-MM-DD)
- `endDate`: 结束日期 (YYYY-MM-DD)

**示例**：
```bash
curl -X POST "http://localhost:8080/api/v1/business/rates/sync?startDate=2025-12-21&endDate=2026-01-20" \
  -H "Authorization: Bearer {token}"
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "startDate": "2025-12-21",
    "endDate": "2026-01-20",
    "currencyCodes": ["USD", "EUR", "GBP", "CAD"],
    "totalCount": 120,
    "insertCount": 100,
    "updateCount": 20,
    "failCount": 0,
    "durationMs": 2345,
    "message": "Synced 120 rates (100 new, 20 updated, 0 failed)"
  }
}
```

### 3.2 同步指定货币汇率

**接口**：`POST /api/v1/business/rates/sync/currencies`

**参数**：
- `startDate`: 开始日期 (YYYY-MM-DD)
- `endDate`: 结束日期 (YYYY-MM-DD)
- `currencyCodes`: 货币编码列表 (例如: USD,EUR,GBP)

**示例**：
```bash
curl -X POST "http://localhost:8080/api/v1/business/rates/sync/currencies?startDate=2026-01-01&endDate=2026-01-20&currencyCodes=USD,EUR" \
  -H "Authorization: Bearer {token}"
```

### 3.3 同步最近N天汇率

**接口**：`POST /api/v1/business/rates/sync/recent`

**参数**：
- `days`: 天数 (1-365)

**示例**：
```bash
# 同步最近7天的汇率
curl -X POST "http://localhost:8080/api/v1/business/rates/sync/recent?days=7" \
  -H "Authorization: Bearer {token}"
```

---

## 四、定时任务说明

### 4.1 每日自动同步

- **触发时间**：每天早上 8:00
- **同步范围**：最近 3 天的汇率
- **同步货币**：所有启用的货币
- **日志位置**：`logs/musheng-tax-system.log`

### 4.2 日志示例

```
2026-01-20 08:00:00.000 [scheduling-1] INFO  RateSyncScheduler - Starting scheduled rate sync task
2026-01-20 08:00:01.234 [scheduling-1] INFO  RateSyncServiceImpl - Starting rate sync from China Money: startDate=2026-01-18, endDate=2026-01-20
2026-01-20 08:00:01.235 [scheduling-1] INFO  RateSyncServiceImpl - Syncing rates for enabled currencies: [USD, EUR, GBP, CAD]
2026-01-20 08:00:03.456 [scheduling-1] INFO  RateSyncServiceImpl - Rate sync completed: total=12, insert=10, update=2, fail=0, duration=2221ms
2026-01-20 08:00:03.457 [scheduling-1] INFO  RateSyncScheduler - Scheduled rate sync completed successfully: Synced 12 rates (10 new, 2 updated, 0 failed)
```

---

## 五、数据说明

### 5.1 数据来源

- **数据源**：中国外汇交易中心 (https://www.chinamoney.com.cn)
- **数据类型**：银行间外汇市场人民币汇率中间价
- **更新频率**：工作日每天更新

### 5.2 汇率存储

汇率数据存储在 `t_exchange_rate` 表中：

| 字段 | 说明 |
|------|------|
| `rate_date` | 汇率日期 |
| `currency_code` | 货币编码 |
| `rate` | 汇率中间价(对人民币) |
| `is_workday` | 是否工作日 |
| `source` | 数据来源(CHINA_MONEY) |

### 5.3 节假日处理

系统支持节假日汇率顺延功能：
- 如果查询日期为周末或节假日，自动顺延到下一个工作日
- 最大顺延天数：10天（可在配置中调整）

---

## 六、故障排查

### 6.1 同步失败

**可能原因**：
1. 网络连接问题
2. 外汇中心API暂时不可用
3. 没有启用的货币

**解决方法**：
1. 检查网络连接
2. 查看日志文件获取详细错误信息
3. 稍后重试或手动触发同步

### 6.2 数据缺失

如果发现某天的汇率数据缺失，可以：

1. **手动触发同步**：
```bash
curl -X POST "http://localhost:8080/api/v1/business/rates/sync/recent?days=7"
```

2. **查看同步日志**：
```bash
tail -f logs/musheng-tax-system.log | grep "rate sync"
```

### 6.3 关闭自动同步

如果需要临时关闭自动同步，修改配置：

```yaml
rate:
  sync:
    enabled: false  # 设置为false
```

---

## 七、注意事项

1. **首次使用**：建议手动触发一次同步，导入历史数据
2. **货币管理**：确保需要的货币在货币管理中已启用
3. **时区设置**：系统时区为 `Asia/Shanghai`
4. **数据准确性**：汇率数据来自官方外汇中心，仅供参考
5. **性能考虑**：建议同步时间范围不超过1年

---

## 八、开发说明

### 8.1 核心类

| 类名 | 说明 |
|------|------|
| `ChinaMoneyClient` | 外汇中心API客户端 |
| `RateSyncService` | 汇率同步服务 |
| `RateSyncScheduler` | 定时同步任务 |
| `RateController` | 汇率接口控制器 |

### 8.2 扩展开发

如需支持其他汇率数据源，可以：
1. 实现新的客户端类（参考 `ChinaMoneyClient`）
2. 在 `RateSyncService` 中添加新的同步方法
3. 更新配置文件添加新数据源配置

---

## 九、更新日志

### v1.1.0 (2026-01-20)

**新增功能**：
- ✅ 集成中国外汇交易中心API
- ✅ 自动同步定时任务
- ✅ 关联货币管理，只同步启用货币
- ✅ 手动触发同步接口
- ✅ 完善的错误处理和日志

**改进**：
- 从手动导入CSV改为自动同步
- 数据源更可靠，更新更及时

---

## 十、联系方式

如有问题，请联系：
- 项目负责人：后端研发团队
- 文档位置：`musheng-tax-system/docs/rate-sync-guide.md`
