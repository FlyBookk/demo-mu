# 慕声报税系统 - 代码重构设计文档

**创建日期**: 2026-02-01  
**版本**: 1.0.0  
**状态**: 设计中

---

## 一、设计概述

### 1.1 设计目标

将现有的单体式 Service 实现重构为符合 DDD 分层架构的模块化设计，同时**严格保证业务逻辑和输出结果不变**。

### 1.2 设计原则

1. **行为不变原则**: 所有 API 的输入输出必须与重构前完全一致
2. **渐进式重构**: 每个阶段独立可验证，可随时回滚
3. **测试先行**: 先建立测试基线，再进行重构
4. **最小改动**: 只做必要的结构调整，不做"顺便优化"

---

## 二、架构设计

### 2.1 目标架构

```
musheng-business/
├── common/                          # 公共模块
│   ├── utils/                       # 工具类
│   │   ├── DateParseUtils.java      # 日期解析
│   │   ├── QueryWrapperUtils.java   # 查询构建
│   │   └── MoneyConvertUtils.java   # 金额转换
│   └── strategy/                    # 策略接口
│       └── FileImportStrategy.java  # 文件导入策略
│
├── sales/                           # 销售模块
│   ├── controller/                  # 控制器层
│   │   └── SalesDataController.java
│   ├── service/                     # 服务层
│   │   ├── SalesDataService.java    # 门面接口
│   │   ├── SalesDataQueryService.java
│   │   ├── SalesDataImportService.java
│   │   ├── SalesDataExportService.java
│   │   └── SalesDataStatisticsService.java
│   ├── repository/                  # 仓储层
│   │   ├── SalesDataRepository.java
│   │   └── impl/
│   │       └── SalesDataRepositoryImpl.java
│   └── entity/                      # 实体层
│       └── SalesData.java
│
├── rate/                            # 汇率模块
│   ├── service/
│   │   └── RateService.java
│   ├── repository/
│   │   └── ExchangeRateRepository.java
│   └── strategy/                    # 导入策略
│       ├── AbstractRateImportStrategy.java
│       ├── RateCsvImportStrategy.java
│       └── RateExcelImportStrategy.java
│
└── fbashipment/                     # FBA货件模块
    ├── service/
    │   └── FbaShipmentService.java
    ├── repository/
    │   └── FbaShipmentRepository.java
    └── entity/
        ├── FbaShipment.java
        └── FbaShipmentItem.java
```

### 2.2 层次职责

| 层次 | 职责 | 示例 |
|-----|-----|-----|
| Controller | 接收请求，参数校验，调用 Service | SalesDataController |
| Service (门面) | 编排业务流程，事务管理 | SalesDataServiceImpl |
| Service (专职) | 单一职责的业务逻辑 | SalesDataQueryService |
| Repository | 数据访问封装 | SalesDataRepository |
| Entity | 数据模型 | SalesData |
| Utils | 无状态工具方法 | DateParseUtils |
| Strategy | 可替换的算法实现 | FileImportStrategy |

---

## 三、详细设计

### 3.1 工具类设计

#### 3.1.1 DateParseUtils

```java
/**
 * 日期解析工具类
 * 
 * ⚠️ 所有方法必须与原有逻辑行为一致
 */
@Slf4j
public final class DateParseUtils {
    
    private DateParseUtils() {}
    
    // 支持的日期格式
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyy年MM月dd日"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };
    
    /**
     * 解析开始日期，返回当天 00:00:00
     */
    public static LocalDateTime parseStartDate(String dateStr) {
        // 实现与原 SalesDataServiceImpl.parseStartDate() 一致
    }
    
    /**
     * 解析结束日期，返回当天 23:59:59
     */
    public static LocalDateTime parseEndDate(String dateStr) {
        // 实现与原 SalesDataServiceImpl.parseEndDate() 一致
    }
    
    /**
     * 解析汇率日期，支持多种格式
     */
    public static LocalDate parseRateDate(String dateStr) {
        // 实现与原 RateServiceImpl.parseRateDate() 一致
    }
}
```

#### 3.1.2 QueryWrapperUtils

```java
/**
 * 查询构建工具类
 */
public final class QueryWrapperUtils {
    
    private QueryWrapperUtils() {}
    
    /**
     * 添加店铺数据隔离条件
     */
    public static <T> void applyShopIdFilter(
            LambdaQueryWrapper<T> wrapper,
            SFunction<T, Long> shopIdGetter) {
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(shopIdGetter, shopId);
    }
    
    /**
     * 添加日期范围过滤
     */
    public static <T> void applyDateRangeFilter(
            LambdaQueryWrapper<T> wrapper,
            SFunction<T, LocalDateTime> dateGetter,
            String startDate, 
            String endDate) {
        // 实现
    }
    
    /**
     * 添加字符串模糊查询
     */
    public static <T> void applyLikeFilter(
            LambdaQueryWrapper<T> wrapper,
            SFunction<T, String> getter,
            String value) {
        if (StringUtils.hasText(value)) {
            wrapper.like(getter, value);
        }
    }
}
```

#### 3.1.3 MoneyConvertUtils

```java
/**
 * 金额转换工具类
 */
@Slf4j
public final class MoneyConvertUtils {
    
    private MoneyConvertUtils() {}
    
    /**
     * 将金额按汇率转换为人民币
     * 
     * @param amount 原始金额
     * @param exchangeRate 汇率
     * @return 人民币金额，异常情况返回 ZERO
     */
    public static BigDecimal convertToCny(BigDecimal amount, BigDecimal exchangeRate) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Missing exchange rate for conversion, amount={}", amount);
            return BigDecimal.ZERO;
        }
        return amount.multiply(exchangeRate);
    }
}
```

### 3.2 Service 拆分设计

#### 3.2.1 门面模式

```java
/**
 * 销售数据服务 - 门面
 * 
 * 职责：
 * 1. 作为对外接口，保持 API 不变
 * 2. 委托给专职 Service 处理
 * 3. 管理事务边界
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SalesDataServiceImpl implements SalesDataService {
    
    private final SalesDataQueryService queryService;
    private final SalesDataImportService importService;
    private final SalesDataExportService exportService;
    private final SalesDataStatisticsService statisticsService;
    
    @Override
    public Page<SalesData> list(SalesQueryRequest request) {
        return queryService.list(request);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importData(MultipartFile file, String siteCode) {
        return importService.importData(file, siteCode);
    }
    
    @Override
    public void exportData(SalesQueryRequest request, HttpServletResponse response) {
        exportService.exportData(request, response);
    }
    
    @Override
    public SalesSummaryVO getSummary(SalesQueryRequest request) {
        return statisticsService.getSummary(request);
    }
}
```

#### 3.2.2 专职 Service

```java
/**
 * 销售数据查询服务
 */
public interface SalesDataQueryService {
    Page<SalesData> list(SalesQueryRequest request);
    SalesData getById(Long id);
    void delete(Long id);
    void batchDelete(List<Long> ids);
}

/**
 * 销售数据导入服务
 */
public interface SalesDataImportService {
    Map<String, Object> importData(MultipartFile file, String siteCode);
}

/**
 * 销售数据导出服务
 */
public interface SalesDataExportService {
    void exportData(SalesQueryRequest request, HttpServletResponse response);
}

/**
 * 销售数据统计服务
 */
public interface SalesDataStatisticsService {
    SalesSummaryVO getSummary(SalesQueryRequest request);
    List<SalesStatByTypeVO> getStatByType(SalesQueryRequest request);
}
```

### 3.3 策略模式设计

#### 3.3.1 策略接口

```java
/**
 * 文件导入策略接口
 * 
 * @param <T> 导入的实体类型
 */
public interface FileImportStrategy<T> {
    
    /**
     * 判断是否支持该文件类型
     */
    boolean supports(String fileName);
    
    /**
     * 解析文件内容
     */
    List<T> parse(MultipartFile file, ImportContext context) throws IOException;
    
    /**
     * 执行导入并保存
     */
    Map<String, Object> importAndSave(MultipartFile file, ImportContext context);
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
}
```

#### 3.3.2 策略实现

```java
/**
 * 汇率导入策略基类
 */
public abstract class AbstractRateImportStrategy implements FileImportStrategy<ExchangeRate> {
    
    protected final CurrencyMapper currencyMapper;
    protected final ExchangeRateMapper exchangeRateMapper;
    
    // 公共方法：获取已配置货币
    protected Set<String> getConfiguredCurrencyCodes() { ... }
    
    // 公共方法：批量检查并插入
    protected int[] batchCheckAndInsert(List<ExchangeRate> rates, AtomicInteger existsCount) { ... }
    
    // 公共方法：解析日期
    protected LocalDate parseRateDate(String dateStr) { ... }
}

/**
 * CSV 汇率导入策略
 */
@Component
public class RateCsvImportStrategy extends AbstractRateImportStrategy {
    
    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".csv");
    }
    
    @Override
    public List<ExchangeRate> parse(MultipartFile file, ImportContext context) {
        // CSV 解析逻辑
    }
}

/**
 * Excel 汇率导入策略
 */
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
        // Excel 解析逻辑
    }
}
```

### 3.4 Repository 设计

```java
/**
 * 销售数据仓储接口
 */
public interface SalesDataRepository {
    
    Page<SalesData> findByQuery(SalesQueryRequest request);
    
    Optional<SalesData> findById(Long id);
    
    boolean existsByOrderIdAndCategory(String orderId, String category);
    
    void save(SalesData salesData);
    
    void saveBatch(List<SalesData> salesDataList);
    
    void deleteById(Long id);
    
    void deleteByIds(List<Long> ids);
    
    List<SalesData> findListByQuery(SalesQueryRequest request);
}

/**
 * 销售数据仓储实现
 */
@Repository
@RequiredArgsConstructor
public class SalesDataRepositoryImpl implements SalesDataRepository {
    
    private final SalesDataMapper salesDataMapper;
    
    @Override
    public Page<SalesData> findByQuery(SalesQueryRequest request) {
        LambdaQueryWrapper<SalesData> wrapper = buildQueryWrapper(request);
        int page = request.getPage() != null ? request.getPage() : 1;
        int size = request.getSize() != null ? request.getSize() : 20;
        return salesDataMapper.selectPage(new Page<>(page, size), wrapper);
    }
    
    private LambdaQueryWrapper<SalesData> buildQueryWrapper(SalesQueryRequest request) {
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
        
        // 店铺隔离
        QueryWrapperUtils.applyShopIdFilter(wrapper, SalesData::getShopId);
        
        // 站点过滤
        if (StringUtils.hasText(request.getSiteCode())) {
            wrapper.eq(SalesData::getSiteCode, request.getSiteCode());
        }
        
        // 日期范围
        QueryWrapperUtils.applyDateRangeFilter(
            wrapper, 
            SalesData::getTransactionDate,
            request.getStartDate(),
            request.getEndDate()
        );
        
        // 排序
        wrapper.orderByDesc(SalesData::getTransactionDate);
        
        return wrapper;
    }
}
```

---

## 四、实体补全设计

### 4.1 FbaShipment 实体

```java
@Data
@TableName("fba_shipment")
public class FbaShipment {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 店铺ID */
    private Long shopId;
    
    /** 货件编号 */
    private String shipmentId;
    
    /** 货件名称 */
    private String shipmentName;
    
    /** 仓库代码 */
    private String warehouseCode;
    
    /** 店铺名称 */
    private String shopName;
    
    /** 国家/地区 */
    private String country;
    
    /** 站点代码 */
    private String siteCode;
    
    /** 货件状态 */
    private String status;
    
    /** SKU数量 */
    private Integer skuCount;
    
    /** 总数量 */
    private Integer totalQuantity;
    
    /** 创建日期 */
    private LocalDateTime createdDate;
    
    /** 发货日期 */
    private LocalDate shipDate;
    
    /** 批次号 */
    private String batchNo;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
    
    /** 货件明细 (非数据库字段) */
    @TableField(exist = false)
    private List<FbaShipmentItem> items;
}
```

### 4.2 FbaShipmentItem 实体

```java
@Data
@TableName("fba_shipment_item")
public class FbaShipmentItem {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 货件ID */
    private Long shipmentId;
    
    /** 店铺ID */
    private Long shopId;
    
    /** SKU */
    private String sku;
    
    /** FNSKU */
    private String fnsku;
    
    /** ASIN */
    private String asin;
    
    /** 商品名称 */
    private String productName;
    
    /** 数量 */
    private Integer quantity;
    
    /** 单价 */
    private BigDecimal unitPrice;
    
    /** 总价 */
    private BigDecimal totalPrice;
    
    /** 币种 */
    private String currency;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}
```

---

## 五、测试设计

### 5.1 快照测试框架

```java
/**
 * 快照测试基类
 */
public abstract class SnapshotTestBase {
    
    private static final String SNAPSHOT_DIR = "src/test/resources/snapshots/";
    
    /**
     * 断言结果与快照匹配
     */
    protected void assertMatchesSnapshot(String snapshotName, Object actual) {
        String snapshotPath = SNAPSHOT_DIR + snapshotName + ".json";
        String actualJson = JSON.toJSONString(actual, SerializerFeature.PrettyFormat);
        
        File snapshotFile = new File(snapshotPath);
        if (!snapshotFile.exists()) {
            // 首次运行，创建快照
            saveSnapshot(snapshotPath, actualJson);
            return;
        }
        
        String expectedJson = readSnapshot(snapshotPath);
        assertEquals(expectedJson, actualJson, 
            "Snapshot mismatch for: " + snapshotName);
    }
}
```

### 5.2 回归测试示例

```java
@SpringBootTest
class SalesDataServiceRegressionTest extends SnapshotTestBase {
    
    @Autowired
    private SalesDataService salesDataService;
    
    @Test
    void testList_Snapshot() {
        SalesQueryRequest request = new SalesQueryRequest();
        request.setSiteCode("US");
        request.setPage(1);
        request.setSize(10);
        
        Page<SalesData> result = salesDataService.list(request);
        
        assertMatchesSnapshot("sales_list_us", result);
    }
    
    @Test
    void testGetSummary_Snapshot() {
        SalesQueryRequest request = new SalesQueryRequest();
        request.setSiteCode("US");
        
        SalesSummaryVO result = salesDataService.getSummary(request);
        
        assertMatchesSnapshot("sales_summary_us", result);
    }
}
```

---

## 六、正确性属性

### 6.1 核心属性

| 属性 | 描述 | 验证方法 |
|-----|-----|---------|
| API 响应不变性 | 重构前后 API 响应完全一致 | 快照测试 |
| 计算结果不变性 | 汇率转换、统计计算结果不变 | 单元测试 |
| 导入结果不变性 | 相同文件导入结果一致 | 集成测试 |
| 导出结果不变性 | 相同条件导出文件一致 | 文件对比 |
| 错误处理不变性 | 错误码和错误消息不变 | 异常测试 |

### 6.2 属性测试示例

```java
@Test
void property_DateParseShouldMatchOriginalBehavior() {
    // 对于任意有效日期字符串，新旧实现结果应一致
    String[] testDates = {"2026-01-15", "2026/01/15", "invalid", "", null};
    
    for (String date : testDates) {
        LocalDateTime newResult = DateParseUtils.parseStartDate(date);
        LocalDateTime oldResult = originalParseStartDate(date);
        assertEquals(oldResult, newResult, "Date: " + date);
    }
}

@Test
void property_MoneyConvertShouldMatchOriginalBehavior() {
    // 对于任意金额和汇率组合，新旧实现结果应一致
    BigDecimal[] amounts = {null, BigDecimal.ZERO, new BigDecimal("100.50")};
    BigDecimal[] rates = {null, BigDecimal.ZERO, new BigDecimal("7.2")};
    
    for (BigDecimal amount : amounts) {
        for (BigDecimal rate : rates) {
            BigDecimal newResult = MoneyConvertUtils.convertToCny(amount, rate);
            BigDecimal oldResult = originalConvertToCny(amount, rate);
            assertEquals(oldResult, newResult, 
                "Amount: " + amount + ", Rate: " + rate);
        }
    }
}
```

---

**文档版本**: 1.0.0  
**创建日期**: 2026-02-01  
**最后更新**: 2026-02-01
