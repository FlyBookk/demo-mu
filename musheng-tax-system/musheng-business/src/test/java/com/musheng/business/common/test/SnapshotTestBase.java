package com.musheng.business.common.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 快照测试基类
 * 
 * 用于在重构过程中验证 API 响应不变性。
 * 首次运行时会创建快照文件，后续运行时会与快照对比。
 * 
 * ⚠️ 重要：本类用于确保重构不改变任何业务输出
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
public abstract class SnapshotTestBase {
    
    /**
     * 快照文件存储目录
     */
    private static final String SNAPSHOT_DIR = "src/test/resources/snapshots/";
    
    /**
     * 是否更新快照模式（设置为 true 时会覆盖现有快照）
     */
    private static final boolean UPDATE_SNAPSHOTS = Boolean.parseBoolean(
            System.getProperty("updateSnapshots", "false"));
    
    /**
     * Jackson ObjectMapper
     */
    private static final ObjectMapper objectMapper;
    
    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * 断言结果与快照匹配
     * 
     * @param snapshotName 快照名称（不含扩展名）
     * @param actual 实际结果对象
     */
    protected void assertMatchesSnapshot(String snapshotName, Object actual) {
        String snapshotPath = SNAPSHOT_DIR + snapshotName + ".json";
        String actualJson = toJson(actual);
        
        File snapshotFile = new File(snapshotPath);
        
        if (!snapshotFile.exists() || UPDATE_SNAPSHOTS) {
            // 首次运行或更新模式，创建/更新快照
            saveSnapshot(snapshotPath, actualJson);
            System.out.println("[Snapshot] Created/Updated: " + snapshotPath);
            return;
        }
        
        String expectedJson = readSnapshot(snapshotPath);
        
        // 对比快照
        Assertions.assertEquals(
                normalizeJson(expectedJson), 
                normalizeJson(actualJson),
                "Snapshot mismatch for: " + snapshotName + 
                "\n\nExpected:\n" + expectedJson + 
                "\n\nActual:\n" + actualJson
        );
    }
    
    /**
     * 断言结果与快照匹配（忽略指定字段）
     * 
     * @param snapshotName 快照名称
     * @param actual 实际结果对象
     * @param ignoreFields 要忽略的字段名数组
     */
    protected void assertMatchesSnapshotIgnoring(String snapshotName, Object actual, String... ignoreFields) {
        // 将对象转为 JSON，移除要忽略的字段后再对比
        String actualJson = toJson(actual);
        
        for (String field : ignoreFields) {
            // 简单的字段移除（适用于顶层字段）
            actualJson = actualJson.replaceAll("\"" + field + "\"\\s*:\\s*[^,}\\]]+[,]?", "");
        }
        
        try {
            Object parsed = objectMapper.readValue(actualJson, Object.class);
            assertMatchesSnapshot(snapshotName, parsed);
        } catch (JsonProcessingException e) {
            assertMatchesSnapshot(snapshotName, actualJson);
        }
    }
    
    /**
     * 将对象转换为格式化的 JSON 字符串
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }
    
    /**
     * 标准化 JSON（移除空白差异）
     */
    private String normalizeJson(String json) {
        if (json == null) {
            return "";
        }
        try {
            Object parsed = objectMapper.readValue(json, Object.class);
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception e) {
            return json.trim();
        }
    }
    
    /**
     * 保存快照到文件
     */
    private void saveSnapshot(String path, String content) {
        try {
            Path filePath = Paths.get(path);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save snapshot: " + path, e);
        }
    }
    
    /**
     * 读取快照文件内容
     */
    private String readSnapshot(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read snapshot: " + path, e);
        }
    }
    
    /**
     * 删除快照文件（用于测试清理）
     */
    protected void deleteSnapshot(String snapshotName) {
        String snapshotPath = SNAPSHOT_DIR + snapshotName + ".json";
        try {
            Files.deleteIfExists(Paths.get(snapshotPath));
        } catch (IOException e) {
            // 忽略删除失败
        }
    }
}
