# 报税统计明细导出 - Excel 与 CSV 对比评估

## 一、实现方案

| 数据量 | 导出格式 | 说明 |
|--------|----------|------|
| ≤ 10 万行 | Excel 多 sheet | EasyExcel 增量写入，4 个 sheet：收入/退款/费用/其它 |
| > 10 万行 | CSV + ZIP | 4 个 CSV 文件打 zip 包，UTF-8 BOM |

## 二、性能对比

| 维度 | Excel (EasyExcel) | CSV + ZIP |
|------|-------------------|-----------|
| **内存** | 流式写入，每批 2000 行，内存稳定 | 流式写入，内存更低 |
| **写入速度** | 中等（xlsx 格式有压缩开销） | 快（纯文本） |
| **文件大小** | 较大（xml 结构 + 压缩） | 较小（纯文本） |
| **Excel 打开** | 直接打开 | 需解压后打开 CSV |
| **适用场景** | 10 万行以内，用户习惯 Excel | 大数据量，批量分析 |

## 三、阈值选择依据

- **10 万行**：Excel 单文件在此量级下仍可接受（约 5–15 秒），超过后打开/编辑明显变慢
- EasyExcel 流式写入可支撑更大数据，主要瓶颈在客户端打开和编辑体验
- CSV 无格式开销，百万行级别仍可快速生成和用脚本处理

## 四、接口说明

- **路径**：`GET /api/v1/business/reports/tax-summary/export-detail`
- **参数**：siteCode（可选）、startQuarter、endQuarter
- **响应**：`application/vnd...sheet`（Excel）或 `application/zip`（ZIP）
