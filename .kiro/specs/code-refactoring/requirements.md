# 慕声报税系统 - 代码重构规划

**创建日期**: 2026-02-01  
**版本**: 1.0.0  
**状态**: 规划中

---

## 🚨 核心原则 (必须遵守)

> **⚠️ 重要声明**: 本次重构的最高原则是：
> 
> 1. **禁止修改业务流程** - 所有业务逻辑必须保持不变
> 2. **禁止改变输出结果** - API 响应、数据格式、计算结果必须与重构前完全一致
> 3. **只能调整代码逻辑和架构设计** - 仅允许代码结构、组织方式、设计模式的调整
>
> 每个阶段开始前必须重申此原则，结束后必须验证此原则。

---

## 一、重构目标

### 1.1 目标范围

| 模块 | 当前问题 | 重构目标 |
|-----|---------|---------|
| SalesDataServiceImpl | 1547 行,职责过多 | 拆分为 4-5 个专职 Service |
| RateServiceImpl | 806 行,导入逻辑重复 | 提取策略模式,消除重复 |
| FbaShipmentServiceImpl | 编译错误,实体不完整 | 修复错误,补全实体 |
| 公共逻辑 | 重复代码散落各处 | 提取工具类和公共组件 |

### 1.2 不变承诺

以下内容在重构过程中**绝对不能改变**：

- ✅ API 接口签名 (URL、参数、返回类型)
- ✅ 数据库表结构
- ✅ 业务计算逻辑 (汇率转换、税费计算等)
- ✅ 数据校验规则
- ✅ 错误码和错误消息
- ✅ 日志输出格式
- ✅ 导入/导出文件格式


---

## 二、重构阶段规划

### 阶段 0: 准备工作 (第 1 天)

#### 🚨 原则重申
> 本阶段目标是建立测试基线，确保后续重构不会改变任何业务行为和输出结果。

#### 0.1 建立测试基线

**目的**: 在重构前记录所有 API 的输入输出，作为重构后的验证基准。

**任务清单**:
- [ ] 0.1.1 创建 API 快照测试框架
- [ ] 0.1.2 记录 SalesData 模块所有 API 的请求/响应快照
- [ ] 0.1.3 记录 Rate 模块所有 API 的请求/响应快照
- [ ] 0.1.4 记录 FbaShipment 模块所有 API 的请求/响应快照
- [ ] 0.1.5 记录关键业务计算的输入/输出快照

**验证方式**:
```java
// 快照测试示例
@Test
void testListSalesData_Snapshot() {
    // Given: 固定的请求参数
    SalesQueryRequest request = createFixedRequest();
    
    // When: 调用 API
    Page<SalesData> result = salesDataService.list(request);
    
    // Then: 与快照对比
    assertMatchesSnapshot("sales_list_response", result);
}
```

#### 0.2 创建回归测试套件

**任务清单**:
- [ ] 0.2.1 为汇率计算逻辑创建单元测试
- [ ] 0.2.2 为日期解析逻辑创建单元测试
- [ ] 0.2.3 为数据转换逻辑创建单元测试
- [ ] 0.2.4 为去重检查逻辑创建单元测试

#### ✅ 阶段 0 完成检查

| 检查项 | 验证方法 | 通过标准 |
|-------|---------|---------|
| 快照测试覆盖所有 API | 检查测试数量 | 每个 API 至少 1 个快照 |
| 业务逻辑测试覆盖 | 检查测试覆盖率 | 核心逻辑覆盖率 > 80% |
| 所有测试通过 | 运行测试套件 | 0 失败 |

---

### 阶段 1: 修复编译错误 (第 2 天)

#### 🚨 原则重申
> 本阶段只修复编译错误，不修改任何业务逻辑。所有修复必须是"补全缺失"而非"修改现有"。

#### 1.1 修复 FbaShipment 实体类

**问题**: FbaShipment 和 FbaShipmentItem 实体类缺少字段定义

**任务清单**:
- [ ] 1.1.1 分析 FbaShipmentServiceImpl 中使用的所有字段
- [ ] 1.1.2 补全 FbaShipment 实体类缺失字段
- [ ] 1.1.3 补全 FbaShipmentItem 实体类缺失字段
- [ ] 1.1.4 确保 Lombok 注解正确配置

**修复原则**:
```java
// ❌ 禁止: 修改现有字段
// private String shipmentId;  // 不能改名或改类型

// ✅ 允许: 补全缺失字段
@Data
@TableName("fba_shipment")
public class FbaShipment {
    // 现有字段保持不变
    private Long id;
    
    // 补全缺失字段 (根据 Service 中的使用情况)
    private Long shopId;           // 补全
    private String shipmentId;     // 补全
    private String warehouseCode;  // 补全
    private String shopName;       // 补全
    private String country;        // 补全
    private LocalDateTime createdDate;  // 补全
    private Integer skuCount;      // 补全
    private Integer totalQuantity; // 补全
    
    @TableField(exist = false)
    private List<FbaShipmentItem> items;  // 补全
}
```

#### 1.2 修复 ImportRecord 实体类

**任务清单**:
- [ ] 1.2.1 分析 ImportRecord 使用的所有字段
- [ ] 1.2.2 补全 ImportRecord 缺失字段

#### ✅ 阶段 1 完成检查

| 检查项 | 验证方法 | 通过标准 |
|-------|---------|---------|
| 编译通过 | `mvn compile` | 0 错误 |
| 快照测试通过 | 运行快照测试 | 所有快照匹配 |
| API 响应不变 | 对比重构前后响应 | 完全一致 |


---

### 阶段 2: 提取公共工具类 (第 3-4 天)

#### 🚨 原则重申
> 本阶段只提取重复代码到工具类，不修改任何业务逻辑。提取后的方法必须与原方法行为完全一致。

#### 2.1 提取日期解析工具类

**当前问题**: 日期解析逻辑在多个 Service 中重复

**任务清单**:
- [ ] 2.1.1 创建 DateParseUtils 工具类
- [ ] 2.1.2 提取 parseStartDate 方法
- [ ] 2.1.3 提取 parseEndDate 方法
- [ ] 2.1.4 提取 parseRateDate 方法 (支持多种格式)
- [ ] 2.1.5 替换 SalesDataServiceImpl 中的日期解析调用
- [ ] 2.1.6 替换 RateServiceImpl 中的日期解析调用
- [ ] 2.1.7 替换 FbaShipmentServiceImpl 中的日期解析调用

**提取规范**:
```java
// 原代码 (SalesDataServiceImpl)
private LocalDateTime parseStartDate(String dateStr) {
    if (!StringUtils.hasText(dateStr)) {
        return null;
    }
    try {
        return java.time.LocalDate.parse(dateStr).atStartOfDay();
    } catch (Exception e) {
        log.warn("Invalid start date format: {}", dateStr);
        return null;
    }
}

// 提取后 (DateParseUtils)
@Slf4j
public final class DateParseUtils {
    
    private DateParseUtils() {}
    
    /**
     * 解析开始日期，返回当天 00:00:00
     * 
     * @param dateStr 日期字符串 (yyyy-MM-dd 格式)
     * @return 解析后的 LocalDateTime，解析失败返回 null
     */
    public static LocalDateTime parseStartDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr).atStartOfDay();
        } catch (Exception e) {
            log.warn("Invalid start date format: {}", dateStr);
            return null;  // ⚠️ 保持原有行为：返回 null 而非抛异常
        }
    }
}

// 替换后的调用
private void applyDateRangeFilter(LambdaQueryWrapper<SalesData> wrapper, 
                                   String startDate, String endDate) {
    LocalDateTime start = DateParseUtils.parseStartDate(startDate);  // 替换
    if (start != null) {
        wrapper.ge(SalesData::getTransactionDate, start);
    }
    // ...
}
```

**验证方式**:
```java
@Test
void testParseStartDate_ShouldMatchOriginalBehavior() {
    // 测试正常情况
    assertEquals(
        LocalDate.of(2026, 1, 15).atStartOfDay(),
        DateParseUtils.parseStartDate("2026-01-15")
    );
    
    // 测试空值
    assertNull(DateParseUtils.parseStartDate(null));
    assertNull(DateParseUtils.parseStartDate(""));
    
    // 测试无效格式 - 必须返回 null (保持原有行为)
    assertNull(DateParseUtils.parseStartDate("invalid"));
}
```

#### 2.2 提取查询构建工具类

**任务清单**:
- [ ] 2.2.1 创建 QueryWrapperUtils 工具类
- [ ] 2.2.2 提取 applyDateRangeFilter 方法
- [ ] 2.2.3 提取 applyShopIdFilter 方法
- [ ] 2.2.4 替换各 Service 中的查询构建代码

**提取规范**:
```java
public final class QueryWrapperUtils {
    
    /**
     * 添加店铺数据隔离条件
     * 
     * @param wrapper 查询包装器
     * @param shopIdGetter 获取 shopId 的方法引用
     */
    public static <T> void applyShopIdFilter(
            LambdaQueryWrapper<T> wrapper,
            SFunction<T, Long> shopIdGetter) {
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(shopIdGetter, shopId);
    }
    
    /**
     * 添加日期范围过滤条件
     * 
     * @param wrapper 查询包装器
     * @param dateGetter 获取日期的方法引用
     * @param startDate 开始日期字符串
     * @param endDate 结束日期字符串
     */
    public static <T> void applyDateRangeFilter(
            LambdaQueryWrapper<T> wrapper,
            SFunction<T, LocalDateTime> dateGetter,
            String startDate, 
            String endDate) {
        LocalDateTime start = DateParseUtils.parseStartDate(startDate);
        if (start != null) {
            wrapper.ge(dateGetter, start);
        }
        LocalDateTime end = DateParseUtils.parseEndDate(endDate);
        if (end != null) {
            wrapper.le(dateGetter, end);
        }
    }
}
```

#### 2.3 提取金额转换工具类

**任务清单**:
- [ ] 2.3.1 创建 MoneyConvertUtils 工具类
- [ ] 2.3.2 提取 convertToCny 方法
- [ ] 2.3.3 替换 SalesDataServiceImpl 中的调用

**提取规范**:
```java
@Slf4j
public final class MoneyConvertUtils {
    
    /**
     * 将金额按汇率转换为人民币
     * 
     * ⚠️ 重要: 保持原有行为
     * - amount 为 null 时返回 ZERO
     * - exchangeRate 为 null 或 0 时返回 ZERO (并记录警告日志)
     * 
     * @param amount 原始金额
     * @param exchangeRate 汇率
     * @return 人民币金额
     */
    public static BigDecimal convertToCny(BigDecimal amount, BigDecimal exchangeRate) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Missing exchange rate for conversion, amount={}", amount);
            return BigDecimal.ZERO;  // ⚠️ 保持原有行为
        }
        return amount.multiply(exchangeRate);
    }
}
```

#### ✅ 阶段 2 完成检查

| 检查项 | 验证方法 | 通过标准 |
|-------|---------|---------|
| 编译通过 | `mvn compile` | 0 错误 |
| 工具类单元测试 | 运行工具类测试 | 100% 通过 |
| 快照测试通过 | 运行快照测试 | 所有快照匹配 |
| API 响应不变 | 对比重构前后响应 | 完全一致 |
| 日志输出不变 | 对比日志格式 | 完全一致 |


---

### 阶段 3: 拆分 SalesDataServiceImpl (第 5-7 天)

#### 🚨 原则重申
> 本阶段将臃肿的 Service 拆分为多个专职 Service。拆分过程中：
> - 不修改任何业务逻辑
> - 不改变任何方法的输入输出
> - 原 Service 保留为门面，委托给新 Service

#### 3.1 拆分策略

**拆分方案**:
```
SalesDataServiceImpl (1547 行)
    ↓ 拆分为
├── SalesDataServiceImpl (门面，约 100 行)
│   └── 委托给以下 Service
├── SalesDataQueryService (查询，约 200 行)
├── SalesDataImportService (导入，约 400 行)
├── SalesDataExportService (导出，约 150 行)
└── SalesDataStatisticsService (统计，约 150 行)
```

#### 3.2 创建 SalesDataQueryService

**任务清单**:
- [ ] 3.2.1 创建 SalesDataQueryService 接口
- [ ] 3.2.2 创建 SalesDataQueryServiceImpl 实现类
- [ ] 3.2.3 迁移 list() 方法
- [ ] 3.2.4 迁移 getById() 方法
- [ ] 3.2.5 迁移 delete() 方法
- [ ] 3.2.6 迁移 batchDelete() 方法

**迁移规范**:
```java
// 新建 SalesDataQueryService
public interface SalesDataQueryService {
    Page<SalesData> list(SalesQueryRequest request);
    SalesData getById(Long id);
    void delete(Long id);
    void batchDelete(List<Long> ids);
}

// 实现类 - 代码从原 Service 复制，不做任何修改
@Service
@Slf4j
@RequiredArgsConstructor
public class SalesDataQueryServiceImpl implements SalesDataQueryService {
    
    private final SalesDataMapper salesDataMapper;
    
    @Override
    public Page<SalesData> list(SalesQueryRequest request) {
        // ⚠️ 代码与原 Service 完全一致，只是位置变了
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
        
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(SalesData::getShopId, shopId);
        
        // ... 完全相同的逻辑
    }
}

// 原 Service 改为门面模式
@Service
@Slf4j
@RequiredArgsConstructor
public class SalesDataServiceImpl implements SalesDataService {
    
    private final SalesDataQueryService queryService;  // 委托
    private final SalesDataImportService importService;
    private final SalesDataExportService exportService;
    private final SalesDataStatisticsService statisticsService;
    
    @Override
    public Page<SalesData> list(SalesQueryRequest request) {
        return queryService.list(request);  // 委托
    }
    
    @Override
    public SalesData getById(Long id) {
        return queryService.getById(id);  // 委托
    }
    
    // ... 其他方法同样委托
}
```

#### 3.3 创建 SalesDataImportService

**任务清单**:
- [ ] 3.3.1 创建 SalesDataImportService 接口
- [ ] 3.3.2 创建 SalesDataImportServiceImpl 实现类
- [ ] 3.3.3 迁移 importData() 方法
- [ ] 3.3.4 迁移 parseSalesRecord() 方法
- [ ] 3.3.5 迁移 parseDecimalField() 方法
- [ ] 3.3.6 迁移 getMappedValue() 方法
- [ ] 3.3.7 迁移 getCommonFieldNames() 方法
- [ ] 3.3.8 迁移 getFieldMapping() 方法
- [ ] 3.3.9 迁移 getTransactionTypeMapping() 方法
- [ ] 3.3.10 迁移 isDuplicate() 方法
- [ ] 3.3.11 迁移 fillExchangeRate() 方法
- [ ] 3.3.12 迁移 generateBatchNo() 方法

#### 3.4 创建 SalesDataExportService

**任务清单**:
- [ ] 3.4.1 创建 SalesDataExportService 接口
- [ ] 3.4.2 创建 SalesDataExportServiceImpl 实现类
- [ ] 3.4.3 迁移 exportData() 方法

#### 3.5 创建 SalesDataStatisticsService

**任务清单**:
- [ ] 3.5.1 创建 SalesDataStatisticsService 接口
- [ ] 3.5.2 创建 SalesDataStatisticsServiceImpl 实现类
- [ ] 3.5.3 迁移 getSummary() 方法
- [ ] 3.5.4 迁移 getStatByType() 方法
- [ ] 3.5.5 迁移 convertToCny() 方法 (改为调用 MoneyConvertUtils)

#### ✅ 阶段 3 完成检查

| 检查项 | 验证方法 | 通过标准 |
|-------|---------|---------|
| 编译通过 | `mvn compile` | 0 错误 |
| 快照测试通过 | 运行快照测试 | 所有快照匹配 |
| API 响应不变 | 对比重构前后响应 | 完全一致 |
| 导入结果不变 | 使用相同文件导入 | 结果完全一致 |
| 导出结果不变 | 导出相同数据 | 文件内容一致 |
| 统计结果不变 | 查询相同条件 | 数值完全一致 |


---

### 阶段 4: 引入策略模式重构导入逻辑 (第 8-10 天)

#### 🚨 原则重申
> 本阶段引入策略模式消除重复代码。策略模式只是代码组织方式的改变，不改变任何业务逻辑和输出结果。

#### 4.1 创建文件导入策略接口

**任务清单**:
- [ ] 4.1.1 创建 FileImportStrategy 接口
- [ ] 4.1.2 创建 ImportContext 上下文类
- [ ] 4.1.3 创建 ImportResult 结果类

**接口定义**:
```java
/**
 * 文件导入策略接口
 * 
 * ⚠️ 注意: 实现类必须保证与原有逻辑完全一致
 */
public interface FileImportStrategy<T> {
    
    /**
     * 判断是否支持该文件类型
     */
    boolean supports(String fileName);
    
    /**
     * 解析文件内容
     * 
     * @return 解析结果，必须与原有解析逻辑结果一致
     */
    List<T> parse(MultipartFile file, ImportContext context);
}

/**
 * 导入上下文
 */
@Data
@Builder
public class ImportContext {
    private String siteCode;
    private Long shopId;
    private Map<String, String> fieldMapping;
    private Map<String, String> transactionTypeMapping;
    private Marketplace marketplace;
}
```

#### 4.2 实现汇率导入策略

**任务清单**:
- [ ] 4.2.1 创建 RateCsvImportStrategy
- [ ] 4.2.2 创建 RateExcelImportStrategy
- [ ] 4.2.3 提取公共逻辑到 AbstractRateImportStrategy
- [ ] 4.2.4 重构 RateServiceImpl.importData() 使用策略

**实现规范**:
```java
// 抽象基类 - 提取公共逻辑
public abstract class AbstractRateImportStrategy implements FileImportStrategy<ExchangeRate> {
    
    protected final CurrencyMapper currencyMapper;
    protected final ExchangeRateMapper exchangeRateMapper;
    
    /**
     * 获取已配置的货币代码
     * ⚠️ 逻辑与原 RateServiceImpl.getConfiguredCurrencyCodes() 完全一致
     */
    protected Set<String> getConfiguredCurrencyCodes() {
        LambdaQueryWrapper<Currency> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Currency::getStatus, 1)
               .ne(Currency::getCurrencyCode, "CNY");
        List<Currency> currencies = currencyMapper.selectList(wrapper);
        return currencies.stream()
                .map(Currency::getCurrencyCode)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }
    
    /**
     * 批量检查重复并插入
     * ⚠️ 逻辑与原 RateServiceImpl.batchCheckAndInsert() 完全一致
     */
    protected int[] batchCheckAndInsert(List<ExchangeRate> ratesToImport, 
                                        AtomicInteger existsCount) {
        // 完全复制原有逻辑
    }
    
    /**
     * 解析日期
     * ⚠️ 逻辑与原 RateServiceImpl.parseRateDate() 完全一致
     */
    protected LocalDate parseRateDate(String dateStr) {
        // 完全复制原有逻辑
    }
}

// CSV 导入策略
@Component
public class RateCsvImportStrategy extends AbstractRateImportStrategy {
    
    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".csv");
    }
    
    @Override
    public List<ExchangeRate> parse(MultipartFile file, ImportContext context) {
        // ⚠️ 逻辑与原 RateServiceImpl.importCsvData() 完全一致
    }
}

// Excel 导入策略
@Component
public class RateExcelImportStrategy extends AbstractRateImportStrategy {
    
    @Override
    public boolean supports(String fileName) {
        return fileName != null && 
               (fileName.toLowerCase().endsWith(".xlsx") || 
                fileName.toLowerCase().endsWith(".xls"));
    }
    
    @Override
    public List<ExchangeRate> parse(MultipartFile file, ImportContext context) {
        // ⚠️ 逻辑与原 RateServiceImpl.importExcelData() 完全一致
    }
}
```

#### 4.3 重构 RateServiceImpl

**任务清单**:
- [ ] 4.3.1 注入策略列表
- [ ] 4.3.2 重构 importData() 方法使用策略
- [ ] 4.3.3 删除原有的 importCsvData() 和 importExcelData() 方法

**重构后代码**:
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class RateServiceImpl implements RateService {
    
    private final List<FileImportStrategy<ExchangeRate>> importStrategies;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importData(MultipartFile file) {
        log.info("Importing exchange rates: fileName={}", file.getOriginalFilename());
        
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new BusinessException(ErrorCode.IMPORT_FILE_FORMAT_ERROR, "File name is empty");
        }
        
        // 选择策略
        FileImportStrategy<ExchangeRate> strategy = importStrategies.stream()
                .filter(s -> s.supports(fileName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.IMPORT_FILE_FORMAT_ERROR,
                        "Unsupported file format. Please use .xlsx, .xls or .csv file"));
        
        // 执行导入
        ImportContext context = buildImportContext();
        return strategy.importAndSave(file, context);
    }
}
```

#### ✅ 阶段 4 完成检查

| 检查项 | 验证方法 | 通过标准 |
|-------|---------|---------|
| 编译通过 | `mvn compile` | 0 错误 |
| 快照测试通过 | 运行快照测试 | 所有快照匹配 |
| CSV 导入结果不变 | 使用相同 CSV 文件 | 结果完全一致 |
| Excel 导入结果不变 | 使用相同 Excel 文件 | 结果完全一致 |
| 错误处理不变 | 使用错误文件测试 | 错误消息一致 |
| 跳过逻辑不变 | 测试未配置货币 | 跳过行为一致 |


---

### 阶段 5: 引入 Repository 模式 (第 11-13 天)

#### 🚨 原则重申
> 本阶段引入 Repository 模式封装数据访问。Repository 只是 Mapper 的包装，不改变任何查询逻辑和结果。

#### 5.1 创建 SalesDataRepository

**任务清单**:
- [ ] 5.1.1 创建 SalesDataRepository 接口
- [ ] 5.1.2 创建 SalesDataRepositoryImpl 实现类
- [ ] 5.1.3 迁移查询逻辑到 Repository
- [ ] 5.1.4 修改 Service 使用 Repository

**接口定义**:
```java
/**
 * 销售数据仓储接口
 * 
 * ⚠️ 注意: 所有方法的返回结果必须与直接使用 Mapper 完全一致
 */
public interface SalesDataRepository {
    
    /**
     * 分页查询
     */
    Page<SalesData> findByQuery(SalesQueryRequest request);
    
    /**
     * 根据 ID 查询
     */
    Optional<SalesData> findById(Long id);
    
    /**
     * 检查是否重复
     */
    boolean existsByOrderIdAndCategory(String orderId, String category);
    
    /**
     * 保存
     */
    void save(SalesData salesData);
    
    /**
     * 删除
     */
    void deleteById(Long id);
    
    /**
     * 批量删除
     */
    void deleteByIds(List<Long> ids);
    
    /**
     * 查询列表 (用于统计)
     */
    List<SalesData> findListByQuery(SalesQueryRequest request);
}
```

**实现规范**:
```java
@Repository
@RequiredArgsConstructor
public class SalesDataRepositoryImpl implements SalesDataRepository {
    
    private final SalesDataMapper salesDataMapper;
    
    @Override
    public Page<SalesData> findByQuery(SalesQueryRequest request) {
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
        
        // ⚠️ 查询逻辑与原 Service 完全一致
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(SalesData::getShopId, shopId);
        
        if (StringUtils.hasText(request.getSiteCode())) {
            wrapper.eq(SalesData::getSiteCode, request.getSiteCode());
        }
        // ... 完全相同的条件构建
        
        wrapper.orderByDesc(SalesData::getTransactionDate);
        
        int page = request.getPage() != null ? request.getPage() : 1;
        int size = request.getSize() != null ? request.getSize() : 20;
        return salesDataMapper.selectPage(new Page<>(page, size), wrapper);
    }
    
    @Override
    public boolean existsByOrderIdAndCategory(String orderId, String category) {
        // ⚠️ 逻辑与原 isDuplicate() 方法完全一致
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesData::getOrderId, orderId)
                .eq(SalesData::getTransactionCategory, category);
        return salesDataMapper.selectCount(wrapper) > 0;
    }
}
```

#### 5.2 创建 ExchangeRateRepository

**任务清单**:
- [ ] 5.2.1 创建 ExchangeRateRepository 接口
- [ ] 5.2.2 创建 ExchangeRateRepositoryImpl 实现类
- [ ] 5.2.3 迁移汇率查询逻辑
- [ ] 5.2.4 修改 RateService 使用 Repository

#### 5.3 创建 FbaShipmentRepository

**任务清单**:
- [ ] 5.3.1 创建 FbaShipmentRepository 接口
- [ ] 5.3.2 创建 FbaShipmentRepositoryImpl 实现类
- [ ] 5.3.3 迁移货件查询逻辑
- [ ] 5.3.4 修改 FbaShipmentService 使用 Repository

#### ✅ 阶段 5 完成检查

| 检查项 | 验证方法 | 通过标准 |
|-------|---------|---------|
| 编译通过 | `mvn compile` | 0 错误 |
| 快照测试通过 | 运行快照测试 | 所有快照匹配 |
| 查询结果不变 | 对比查询结果 | 完全一致 |
| 分页结果不变 | 对比分页数据 | 完全一致 |
| 去重逻辑不变 | 测试重复数据 | 行为一致 |

---

### 阶段 6: 代码清理和优化 (第 14 天)

#### 🚨 原则重申
> 本阶段进行代码清理，删除无用代码，优化代码结构。所有清理必须确保不影响业务逻辑。

#### 6.1 清理任务

**任务清单**:
- [ ] 6.1.1 删除原 Service 中已迁移的私有方法
- [ ] 6.1.2 删除未使用的 import 语句
- [ ] 6.1.3 删除注释掉的代码
- [ ] 6.1.4 统一代码格式
- [ ] 6.1.5 添加必要的注释

#### 6.2 代码审查

**任务清单**:
- [ ] 6.2.1 检查所有新建类是否符合代码规范
- [ ] 6.2.2 检查所有方法是否有适当的注释
- [ ] 6.2.3 检查日志输出是否完整
- [ ] 6.2.4 检查异常处理是否正确

#### ✅ 阶段 6 完成检查

| 检查项 | 验证方法 | 通过标准 |
|-------|---------|---------|
| 编译通过 | `mvn compile` | 0 错误 |
| 快照测试通过 | 运行快照测试 | 所有快照匹配 |
| 代码规范检查 | 运行 checkstyle | 0 警告 |
| 所有 API 正常 | 运行集成测试 | 100% 通过 |



---

## 三、时间线总览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        重构时间线 (共 14 天)                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  第 1 天     │ 阶段 0: 准备工作                                              │
│             │ ├── 建立 API 快照测试                                          │
│             │ └── 创建回归测试套件                                           │
│             │                                                               │
│  第 2 天     │ 阶段 1: 修复编译错误                                          │
│             │ ├── 补全 FbaShipment 实体字段                                  │
│             │ └── 补全 ImportRecord 实体字段                                 │
│             │                                                               │
│  第 3-4 天   │ 阶段 2: 提取公共工具类                                        │
│             │ ├── DateParseUtils                                            │
│             │ ├── QueryWrapperUtils                                         │
│             │ └── MoneyConvertUtils                                         │
│             │                                                               │
│  第 5-7 天   │ 阶段 3: 拆分 SalesDataServiceImpl                             │
│             │ ├── SalesDataQueryService                                     │
│             │ ├── SalesDataImportService                                    │
│             │ ├── SalesDataExportService                                    │
│             │ └── SalesDataStatisticsService                                │
│             │                                                               │
│  第 8-10 天  │ 阶段 4: 引入策略模式                                          │
│             │ ├── FileImportStrategy 接口                                   │
│             │ ├── RateCsvImportStrategy                                     │
│             │ └── RateExcelImportStrategy                                   │
│             │                                                               │
│  第 11-13 天 │ 阶段 5: 引入 Repository 模式                                  │
│             │ ├── SalesDataRepository                                       │
│             │ ├── ExchangeRateRepository                                    │
│             │ └── FbaShipmentRepository                                     │
│             │                                                               │
│  第 14 天    │ 阶段 6: 代码清理和优化                                        │
│             │ ├── 删除冗余代码                                              │
│             │ └── 代码审查                                                  │
│             │                                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 四、风险评估与应对

### 4.1 风险矩阵

| 风险 | 可能性 | 影响 | 应对措施 |
|-----|-------|-----|---------|
| 快照测试不完整导致遗漏问题 | 中 | 高 | 阶段 0 必须覆盖所有 API 和边界情况 |
| 拆分 Service 时遗漏依赖 | 中 | 中 | 每次拆分后立即运行全量测试 |
| 策略模式引入新 Bug | 低 | 高 | 策略实现必须逐行复制原有逻辑 |
| Repository 查询条件遗漏 | 中 | 高 | 对比原有 Mapper 调用确保一致 |
| 并发问题 | 低 | 高 | 保持原有事务边界不变 |

### 4.2 关键风险详解

#### 风险 1: 业务逻辑意外变更

**场景**: 在提取工具类或拆分 Service 时，不小心修改了业务逻辑

**应对措施**:
1. 每个阶段开始前运行快照测试，记录基线
2. 每个阶段结束后运行快照测试，对比结果
3. 代码审查时重点关注业务逻辑是否变更

**检测方法**:
```java
// 快照测试会自动检测任何输出变化
@Test
void testImportData_Snapshot() {
    // 使用固定的测试文件
    MultipartFile file = loadTestFile("test_import.xlsx");
    
    // 执行导入
    Map<String, Object> result = salesDataService.importData(file);
    
    // 与快照对比 - 任何字段变化都会失败
    assertMatchesSnapshot("import_result", result);
}
```

#### 风险 2: 隐式依赖丢失

**场景**: 拆分 Service 时，某些方法依赖于类级别的状态或其他方法

**应对措施**:
1. 拆分前分析所有方法的依赖关系
2. 使用 IDE 的"查找引用"功能确认依赖
3. 拆分后检查所有 private 方法是否被正确迁移

**检测方法**:
```bash
# 编译检查会发现缺失的依赖
mvn compile

# 如果有方法调用缺失，编译会报错
```

#### 风险 3: 事务边界变更

**场景**: 拆分 Service 后，原本在同一事务中的操作被分到不同事务

**应对措施**:
1. 保持 @Transactional 注解在原有位置
2. 新 Service 的方法不添加 @Transactional
3. 事务由门面 Service 统一管理

**正确做法**:
```java
// 门面 Service - 保持事务
@Service
public class SalesDataServiceImpl implements SalesDataService {
    
    @Override
    @Transactional(rollbackFor = Exception.class)  // 事务在这里
    public Map<String, Object> importData(MultipartFile file) {
        return importService.importData(file);  // 委托
    }
}

// 内部 Service - 不加事务
@Service
public class SalesDataImportServiceImpl implements SalesDataImportService {
    
    @Override
    // 不加 @Transactional，由调用方管理
    public Map<String, Object> importData(MultipartFile file) {
        // 业务逻辑
    }
}
```

---

## 五、回滚计划

### 5.1 回滚策略

每个阶段完成后创建 Git Tag，便于回滚：

```bash
# 阶段 0 完成
git tag -a v0.1.0-refactor-phase0 -m "重构阶段0完成: 测试基线建立"

# 阶段 1 完成
git tag -a v0.1.0-refactor-phase1 -m "重构阶段1完成: 编译错误修复"

# 阶段 2 完成
git tag -a v0.1.0-refactor-phase2 -m "重构阶段2完成: 工具类提取"

# 以此类推...
```

### 5.2 回滚触发条件

以下情况必须回滚到上一个稳定版本：

1. **快照测试失败** - 任何 API 响应与基线不一致
2. **生产环境报错** - 重构代码导致生产问题
3. **性能严重下降** - 响应时间增加超过 50%
4. **数据不一致** - 导入/导出结果与预期不符

### 5.3 回滚步骤

```bash
# 1. 确认要回滚到的版本
git tag -l "v0.1.0-refactor-*"

# 2. 回滚到指定版本
git checkout v0.1.0-refactor-phase2

# 3. 创建回滚分支
git checkout -b hotfix/rollback-to-phase2

# 4. 合并到主分支
git checkout main
git merge hotfix/rollback-to-phase2

# 5. 重新部署
./deploy.sh
```

---

## 六、验收标准

### 6.1 功能验收

| 验收项 | 验收方法 | 通过标准 |
|-------|---------|---------|
| 所有 API 正常工作 | 运行集成测试 | 100% 通过 |
| 快照测试全部通过 | 运行快照测试 | 0 失败 |
| 导入功能正常 | 使用测试文件导入 | 结果与重构前一致 |
| 导出功能正常 | 导出相同数据 | 文件内容一致 |
| 统计功能正常 | 查询统计数据 | 数值完全一致 |

### 6.2 代码质量验收

| 验收项 | 验收方法 | 通过标准 |
|-------|---------|---------|
| 编译无错误 | `mvn compile` | 0 错误 |
| 单元测试通过 | `mvn test` | 100% 通过 |
| 代码规范检查 | checkstyle | 0 严重警告 |
| Service 类行数 | 代码统计 | 每个 < 300 行 |
| 方法行数 | 代码统计 | 每个 < 50 行 |

### 6.3 架构验收

| 验收项 | 验收方法 | 通过标准 |
|-------|---------|---------|
| 工具类提取完成 | 代码审查 | 无重复的日期/金额处理代码 |
| Service 拆分完成 | 代码审查 | SalesDataServiceImpl < 200 行 |
| 策略模式应用 | 代码审查 | 导入逻辑使用策略模式 |
| Repository 模式应用 | 代码审查 | 数据访问通过 Repository |

---

## 七、总结

### 7.1 重构收益

完成本次重构后，预期获得以下收益：

| 指标 | 重构前 | 重构后 | 改善 |
|-----|-------|-------|-----|
| SalesDataServiceImpl 行数 | 1547 | ~100 | -93% |
| RateServiceImpl 行数 | 806 | ~200 | -75% |
| 重复代码 | 大量 | 极少 | -90% |
| 测试覆盖率 | 0% | >80% | +80% |
| 代码可读性 | 差 | 良好 | 显著提升 |
| 可维护性 | 差 | 良好 | 显著提升 |

### 7.2 核心原则再次强调

> **⚠️ 最终提醒**: 
> 
> 本次重构的唯一目标是**改善代码结构**，而非改变业务行为。
> 
> 在整个重构过程中，必须始终牢记：
> 
> 1. **禁止修改业务流程**
> 2. **禁止改变输出结果**
> 3. **只能调整代码逻辑和架构设计**
> 
> 如果任何时候发现 API 响应、计算结果、导入导出结果与重构前不一致，
> 必须立即停止并回滚，找出问题原因后再继续。

---

**文档版本**: 1.0.0  
**创建日期**: 2026-02-01  
**最后更新**: 2026-02-01  
**作者**: Kiro AI Assistant
