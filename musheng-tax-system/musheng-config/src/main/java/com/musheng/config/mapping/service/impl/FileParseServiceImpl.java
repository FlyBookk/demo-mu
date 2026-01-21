package com.musheng.config.mapping.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.musheng.common.exception.BusinessException;
import com.musheng.config.mapping.dto.FilePreviewResponse;
import com.musheng.config.mapping.dto.FilePreviewResponse.FilePreviewRow;
import com.musheng.config.mapping.dto.ParseFieldsResponse;
import com.musheng.config.mapping.dto.ParseFieldsResponse.SourceFieldVO;
import com.musheng.config.mapping.service.FileParseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 文件解析服务实现
 */
@Slf4j
@Service
public class FileParseServiceImpl implements FileParseService {

    private static final int MAX_PREVIEW_ROWS = 20;
    private static final int MAX_SAMPLE_LENGTH = 30;

    @Override
    public FilePreviewResponse previewFile(MultipartFile file, Integer previewRows) {
        String fileName = file.getOriginalFilename();
        previewRows = Math.min(previewRows == null ? 10 : previewRows, MAX_PREVIEW_ROWS);

        try {
            if (fileName != null && fileName.toLowerCase().endsWith(".csv")) {
                return previewCsvFile(file, previewRows);
            } else {
                return previewExcelFile(file, previewRows);
            }
        } catch (Exception e) {
            log.error("文件预览失败: {}", e.getMessage(), e);
            throw new BusinessException("文件预览失败：" + e.getMessage());
        }
    }

    @Override
    public ParseFieldsResponse parseFields(MultipartFile file, Integer headerRow, String sheetName) {
        String fileName = file.getOriginalFilename();
        headerRow = headerRow == null ? 1 : headerRow;

        try {
            if (fileName != null && fileName.toLowerCase().endsWith(".csv")) {
                return parseCsvFields(file, headerRow);
            } else {
                return parseExcelFields(file, headerRow, sheetName);
            }
        } catch (Exception e) {
            log.error("文件解析失败: {}", e.getMessage(), e);
            throw new BusinessException("文件解析失败：" + e.getMessage());
        }
    }

    /**
     * 预览CSV文件
     */
    private FilePreviewResponse previewCsvFile(MultipartFile file, int previewRows) throws IOException {
        Charset charset = detectEncoding(file);
        char delimiter = detectCsvDelimiter(file, charset);

        List<FilePreviewRow> rows = new ArrayList<>();
        int totalRows = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), charset))) {

            CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(delimiter).build();
            CSVParser parser = CSVParser.parse(reader, format);

            for (CSVRecord record : parser) {
                totalRows++;
                if (totalRows <= previewRows) {
                    List<String> cells = new ArrayList<>();
                    record.forEach(cells::add);

                    FilePreviewRow row = new FilePreviewRow();
                    row.setRowNum(totalRows);
                    row.setContent(String.join("\t", cells));
                    row.setCells(cells);
                    rows.add(row);
                }
            }
        }

        FilePreviewResponse response = new FilePreviewResponse();
        response.setRows(rows);
        response.setTotalRows(totalRows);
        response.setEncoding(charset.name());
        response.setDelimiter(String.valueOf(delimiter));

        return response;
    }

    /**
     * 预览Excel文件
     */
    private FilePreviewResponse previewExcelFile(MultipartFile file, int previewRows) throws IOException {
        List<FilePreviewRow> rows = new ArrayList<>();

        try (InputStream is = file.getInputStream()) {
            EasyExcel.read(is)
                    .headRowNumber(0)
                    .registerReadListener(new ReadListener<Map<Integer, String>>() {
                        private int rowNum = 0;

                        @Override
                        public void invoke(Map<Integer, String> data, AnalysisContext context) {
                            rowNum++;
                            if (rowNum <= previewRows) {
                                List<String> cells = new ArrayList<>();
                                int maxCol = data.keySet().stream().max(Integer::compare).orElse(-1);
                                for (int i = 0; i <= maxCol; i++) {
                                    cells.add(data.getOrDefault(i, ""));
                                }

                                FilePreviewRow row = new FilePreviewRow();
                                row.setRowNum(rowNum);
                                row.setContent(String.join("\t", cells));
                                row.setCells(cells);
                                rows.add(row);
                            }
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                        }
                    })
                    .sheet(0)
                    .doRead();
        }

        FilePreviewResponse response = new FilePreviewResponse();
        response.setRows(rows);
        response.setTotalRows(rows.size());
        response.setEncoding("UTF-8");
        response.setDelimiter("\t");

        return response;
    }

    /**
     * 解析CSV字段
     */
    private ParseFieldsResponse parseCsvFields(MultipartFile file, int headerRow) throws IOException {
        Charset charset = detectEncoding(file);
        char delimiter = detectCsvDelimiter(file, charset);

        List<String> headerFields = null;
        List<String> sampleValues = null;
        int currentRow = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), charset))) {

            CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(delimiter).build();
            CSVParser parser = CSVParser.parse(reader, format);

            for (CSVRecord record : parser) {
                currentRow++;

                if (currentRow == headerRow) {
                    headerFields = new ArrayList<>();
                    record.forEach(headerFields::add);
                } else if (currentRow == headerRow + 1) {
                    sampleValues = new ArrayList<>();
                    record.forEach(sampleValues::add);
                    break;
                }
            }
        }

        if (headerFields == null) {
            throw new BusinessException("未找到表头行");
        }

        List<SourceFieldVO> fields = new ArrayList<>();
        for (int i = 0; i < headerFields.size(); i++) {
            String name = headerFields.get(i).trim();
            if (name.isEmpty()) continue;

            SourceFieldVO field = new SourceFieldVO();
            field.setName(name);
            field.setIndex(i);

            if (sampleValues != null && i < sampleValues.size()) {
                String sample = sampleValues.get(i);
                field.setSample(sample.length() > MAX_SAMPLE_LENGTH
                        ? sample.substring(0, MAX_SAMPLE_LENGTH) + "..."
                        : sample);
            }

            fields.add(field);
        }

        ParseFieldsResponse response = new ParseFieldsResponse();
        response.setFields(fields);
        response.setTotalColumns(headerFields.size());
        response.setHeaderRow(headerRow);
        response.setEncoding(charset.name());
        response.setDelimiter(String.valueOf(delimiter));

        return response;
    }

    /**
     * 解析Excel字段
     */
    private ParseFieldsResponse parseExcelFields(MultipartFile file, int headerRow, String sheetName)
            throws IOException {
        List<SourceFieldVO> fields = new ArrayList<>();
        int[] totalColumns = {0};

        try (InputStream is = file.getInputStream()) {
            List<List<String>> rowsData = new ArrayList<>();

            EasyExcel.read(is)
                    .headRowNumber(0)
                    .registerReadListener(new ReadListener<Map<Integer, String>>() {
                        private int rowNum = 0;

                        @Override
                        public void invoke(Map<Integer, String> data, AnalysisContext context) {
                            rowNum++;
                            if (rowNum == headerRow || rowNum == headerRow + 1) {
                                List<String> row = new ArrayList<>();
                                int maxCol = data.keySet().stream().max(Integer::compare).orElse(-1);
                                for (int i = 0; i <= maxCol; i++) {
                                    row.add(data.getOrDefault(i, ""));
                                }
                                rowsData.add(row);
                                totalColumns[0] = Math.max(totalColumns[0], row.size());
                            }
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                        }
                    })
                    .sheet(sheetName)
                    .doRead();

            if (rowsData.isEmpty()) {
                throw new BusinessException("未找到表头行");
            }

            List<String> headerFields = rowsData.get(0);
            List<String> sampleValues = rowsData.size() > 1 ? rowsData.get(1) : null;

            for (int i = 0; i < headerFields.size(); i++) {
                String name = headerFields.get(i).trim();
                if (name.isEmpty()) continue;

                SourceFieldVO field = new SourceFieldVO();
                field.setName(name);
                field.setIndex(i);

                if (sampleValues != null && i < sampleValues.size()) {
                    String sample = sampleValues.get(i);
                    field.setSample(sample.length() > MAX_SAMPLE_LENGTH
                            ? sample.substring(0, MAX_SAMPLE_LENGTH) + "..."
                            : sample);
                }

                fields.add(field);
            }
        }

        ParseFieldsResponse response = new ParseFieldsResponse();
        response.setFields(fields);
        response.setTotalColumns(totalColumns[0]);
        response.setHeaderRow(headerRow);
        response.setEncoding("UTF-8");
        response.setDelimiter("\t");

        return response;
    }

    /**
     * 检测文件编码
     */
    private Charset detectEncoding(MultipartFile file) throws IOException {
        byte[] bytes = new byte[1024];
        try (InputStream is = file.getInputStream()) {
            int len = is.read(bytes);

            // 检测 BOM
            if (len >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                return StandardCharsets.UTF_8;
            }

            // 简单检测高字节
            boolean hasHighByte = false;
            for (int i = 0; i < len; i++) {
                if ((bytes[i] & 0x80) != 0) {
                    hasHighByte = true;
                    break;
                }
            }

            if (hasHighByte) {
                // 尝试 UTF-8 解码
                try {
                    String decoded = new String(bytes, 0, len, StandardCharsets.UTF_8);
                    // 如果没有乱码字符，认为是UTF-8
                    if (!decoded.contains("\uFFFD")) {
                        return StandardCharsets.UTF_8;
                    }
                } catch (Exception e) {
                    // 忽略
                }
                return Charset.forName("GBK");
            }

            return StandardCharsets.UTF_8;
        }
    }

    /**
     * 检测CSV分隔符
     */
    private char detectCsvDelimiter(MultipartFile file, Charset charset) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), charset))) {

            String firstLine = reader.readLine();
            if (firstLine == null) return ',';

            // 统计分隔符出现次数
            int tabCount = countChar(firstLine, '\t');
            int commaCount = countChar(firstLine, ',');
            int semicolonCount = countChar(firstLine, ';');

            if (tabCount > commaCount && tabCount > semicolonCount) return '\t';
            if (semicolonCount > commaCount) return ';';
            return ',';
        }
    }

    private int countChar(String str, char c) {
        int count = 0;
        for (char ch : str.toCharArray()) {
            if (ch == c) count++;
        }
        return count;
    }
}
