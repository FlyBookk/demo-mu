package com.musheng.business.rate.service;

import com.musheng.business.common.strategy.FileImportStrategy;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.rate.mapper.ExchangeRateMapper;
import com.musheng.business.rate.mapper.HolidayMapper;
import com.musheng.business.rate.repository.ExchangeRateRepository;
import com.musheng.business.rate.service.impl.RateServiceImpl;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 汇率导入错误处理测试
 * 
 * 验证重构后的 RateServiceImpl.importData() 方法对错误文件的处理行为
 * 与原有逻辑完全一致。
 * 
 * 测试场景：
 * 1. 不支持的文件格式（如 .txt, .pdf）
 * 2. 空文件名
 * 3. null 文件名
 * 
 * ⚠️ 重构说明：
 * - 更新构造函数以适应新增的 ExchangeRateRepository 依赖
 * - 测试逻辑保持不变
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("汇率导入错误处理测试")
class RateServiceImportErrorTest {
    
    @Mock
    private ExchangeRateRepository exchangeRateRepository;
    
    @Mock
    private ExchangeRateMapper exchangeRateMapper;
    
    @Mock
    private HolidayMapper holidayMapper;
    
    private RateServiceImpl rateService;
    
    // 模拟的导入策略列表
    private List<FileImportStrategy<ExchangeRate>> importStrategies;
    
    @BeforeEach
    void setUp() {
        // 创建模拟的策略列表（只支持 .csv, .xlsx, .xls）
        importStrategies = new ArrayList<>();
        
        // 添加 CSV 策略模拟
        @SuppressWarnings("unchecked")
        FileImportStrategy<ExchangeRate> csvStrategy = mock(FileImportStrategy.class);
        when(csvStrategy.supports(anyString())).thenAnswer(invocation -> {
            String fileName = invocation.getArgument(0);
            return fileName != null && fileName.toLowerCase().endsWith(".csv");
        });
        importStrategies.add(csvStrategy);
        
        // 添加 Excel 策略模拟
        @SuppressWarnings("unchecked")
        FileImportStrategy<ExchangeRate> excelStrategy = mock(FileImportStrategy.class);
        when(excelStrategy.supports(anyString())).thenAnswer(invocation -> {
            String fileName = invocation.getArgument(0);
            return fileName != null && 
                   (fileName.toLowerCase().endsWith(".xlsx") || 
                    fileName.toLowerCase().endsWith(".xls"));
        });
        importStrategies.add(excelStrategy);
        
        // ⚠️ 更新：使用新的构造函数签名（添加了 ExchangeRateRepository）
        rateService = new RateServiceImpl(
                exchangeRateRepository,
                exchangeRateMapper, 
                holidayMapper, 
                importStrategies
        );
    }
    
    @Nested
    @DisplayName("不支持的文件格式测试")
    class UnsupportedFileFormatTest {
        
        @Test
        @DisplayName("应该拒绝 .txt 文件并抛出正确异常")
        void testImportData_TxtFile_ShouldThrowException() {
            // Given: .txt 文件
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "rates.txt",
                    "text/plain",
                    "some content".getBytes(StandardCharsets.UTF_8)
            );
            
            // When & Then: 应该抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                rateService.importData(file);
            });
            
            // 验证错误码
            assertEquals(ErrorCode.IMPORT_FILE_FORMAT_ERROR.getCode(), exception.getCode());
            // 验证错误消息包含支持的格式提示
            assertTrue(exception.getMessage().contains(".xlsx") || 
                       exception.getMessage().contains(".xls") || 
                       exception.getMessage().contains(".csv"));
        }
        
        @Test
        @DisplayName("应该拒绝 .pdf 文件并抛出正确异常")
        void testImportData_PdfFile_ShouldThrowException() {
            // Given: .pdf 文件
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "rates.pdf",
                    "application/pdf",
                    "some content".getBytes(StandardCharsets.UTF_8)
            );
            
            // When & Then: 应该抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                rateService.importData(file);
            });
            
            assertEquals(ErrorCode.IMPORT_FILE_FORMAT_ERROR.getCode(), exception.getCode());
        }
        
        @Test
        @DisplayName("应该拒绝 .doc 文件并抛出正确异常")
        void testImportData_DocFile_ShouldThrowException() {
            // Given: .doc 文件
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "rates.doc",
                    "application/msword",
                    "some content".getBytes(StandardCharsets.UTF_8)
            );
            
            // When & Then: 应该抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                rateService.importData(file);
            });
            
            assertEquals(ErrorCode.IMPORT_FILE_FORMAT_ERROR.getCode(), exception.getCode());
        }
        
        @Test
        @DisplayName("应该拒绝无扩展名的文件并抛出正确异常")
        void testImportData_NoExtension_ShouldThrowException() {
            // Given: 无扩展名的文件
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "rates",
                    "application/octet-stream",
                    "some content".getBytes(StandardCharsets.UTF_8)
            );
            
            // When & Then: 应该抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                rateService.importData(file);
            });
            
            assertEquals(ErrorCode.IMPORT_FILE_FORMAT_ERROR.getCode(), exception.getCode());
        }
    }
    
    @Nested
    @DisplayName("空文件名测试")
    class EmptyFileNameTest {
        
        @Test
        @DisplayName("应该拒绝 null 文件名并抛出正确异常")
        void testImportData_NullFileName_ShouldThrowException() {
            // Given: null 文件名 - MockMultipartFile 会将 null 转换为空字符串
            // 所以我们需要测试空字符串的情况
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "",  // 空文件名
                    "text/csv",
                    "some content".getBytes(StandardCharsets.UTF_8)
            );
            
            // When & Then: 应该抛出 BusinessException
            // 注意：空字符串文件名会导致策略匹配失败，抛出不支持的文件格式异常
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                rateService.importData(file);
            });
            
            assertEquals(ErrorCode.IMPORT_FILE_FORMAT_ERROR.getCode(), exception.getCode());
        }
    }
    
    @Nested
    @DisplayName("支持的文件格式测试")
    class SupportedFileFormatTest {
        
        @Test
        @DisplayName("应该接受 .csv 文件")
        void testImportData_CsvFile_ShouldAccept() {
            // Given: .csv 文件
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "rates.csv",
                    "text/csv",
                    "date,currency,rate\n2026-01-15,USD,7.25".getBytes(StandardCharsets.UTF_8)
            );
            
            // 模拟策略返回结果
            FileImportStrategy<ExchangeRate> csvStrategy = importStrategies.get(0);
            when(csvStrategy.importAndSave(any(), any())).thenReturn(
                    java.util.Map.of("totalCount", 1, "successCount", 1)
            );
            
            // When & Then: 不应该抛出异常
            assertDoesNotThrow(() -> rateService.importData(file));
            
            // 验证策略被调用
            verify(csvStrategy).importAndSave(any(), any());
        }
        
        @Test
        @DisplayName("应该接受 .xlsx 文件")
        void testImportData_XlsxFile_ShouldAccept() {
            // Given: .xlsx 文件
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "rates.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "dummy content".getBytes(StandardCharsets.UTF_8)
            );
            
            // 模拟策略返回结果
            FileImportStrategy<ExchangeRate> excelStrategy = importStrategies.get(1);
            when(excelStrategy.importAndSave(any(), any())).thenReturn(
                    java.util.Map.of("totalCount", 1, "successCount", 1)
            );
            
            // When & Then: 不应该抛出异常
            assertDoesNotThrow(() -> rateService.importData(file));
            
            // 验证策略被调用
            verify(excelStrategy).importAndSave(any(), any());
        }
        
        @Test
        @DisplayName("应该接受 .xls 文件")
        void testImportData_XlsFile_ShouldAccept() {
            // Given: .xls 文件
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "rates.xls",
                    "application/vnd.ms-excel",
                    "dummy content".getBytes(StandardCharsets.UTF_8)
            );
            
            // 模拟策略返回结果
            FileImportStrategy<ExchangeRate> excelStrategy = importStrategies.get(1);
            when(excelStrategy.importAndSave(any(), any())).thenReturn(
                    java.util.Map.of("totalCount", 1, "successCount", 1)
            );
            
            // When & Then: 不应该抛出异常
            assertDoesNotThrow(() -> rateService.importData(file));
            
            // 验证策略被调用
            verify(excelStrategy).importAndSave(any(), any());
        }
        
        @Test
        @DisplayName("文件扩展名应该不区分大小写")
        void testImportData_UpperCaseExtension_ShouldAccept() {
            // Given: 大写扩展名的 .CSV 文件
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "RATES.CSV",
                    "text/csv",
                    "date,currency,rate\n2026-01-15,USD,7.25".getBytes(StandardCharsets.UTF_8)
            );
            
            // 模拟策略返回结果
            FileImportStrategy<ExchangeRate> csvStrategy = importStrategies.get(0);
            when(csvStrategy.importAndSave(any(), any())).thenReturn(
                    java.util.Map.of("totalCount", 1, "successCount", 1)
            );
            
            // When & Then: 不应该抛出异常
            assertDoesNotThrow(() -> rateService.importData(file));
        }
    }
}
