package com.musheng.config.mapping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.config.mapping.dto.AutoMatchRequest;
import com.musheng.config.mapping.dto.AutoMatchResponse;
import com.musheng.config.mapping.dto.AutoMatchResponse.MatchSuggestion;
import com.musheng.config.mapping.dto.TargetFieldsResponse;
import com.musheng.config.mapping.dto.TargetFieldsResponse.AggregateConfig;
import com.musheng.config.mapping.dto.TargetFieldsResponse.TargetFieldVO;
import com.musheng.config.mapping.entity.ErpAggregateRule;
import com.musheng.config.mapping.entity.TargetFieldMetadata;
import com.musheng.config.mapping.mapper.ErpAggregateRuleMapper;
import com.musheng.config.mapping.mapper.TargetFieldMetadataMapper;
import com.musheng.config.mapping.service.TargetFieldMetadataService;
import com.musheng.common.dto.TargetFieldInfo;
import com.musheng.common.service.EntityFieldScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 目标字段元数据服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TargetFieldMetadataServiceImpl implements TargetFieldMetadataService {

    private final TargetFieldMetadataMapper metadataMapper;
    private final ErpAggregateRuleMapper aggregateRuleMapper;
    private final EntityFieldScanner entityFieldScanner;

    private static final Map<String, String> SOURCE_TYPE_NAMES = Map.of(
            "ORIGINAL", "亚马逊原始数据",
            "ERP", "ERP结算明细"
    );

    @Override
    @Cacheable(value = "targetFields", key = "#dataType + ':' + #sourceType", unless = "#result == null")
    public TargetFieldsResponse getTargetFields(String dataType, String sourceType) {
        log.info("获取目标字段: dataType={}, sourceType={}", dataType, sourceType);

        // 从实体类扫描获取字段（动态、准确）
        List<TargetFieldInfo> fieldInfos = entityFieldScanner.getTargetFields(dataType, sourceType);
        
        // 转换为 VO
        List<TargetFieldVO> fields = fieldInfos.stream()
                .map(this::convertInfoToVO)
                .collect(Collectors.toList());

        // 构建响应
        TargetFieldsResponse response = new TargetFieldsResponse();
        response.setDataType(dataType);
        response.setSourceType(sourceType);
        response.setSourceTypeName(SOURCE_TYPE_NAMES.get(sourceType));
        response.setFields(fields);

        // ERP数据需要返回聚合配置
        if ("ERP".equals(sourceType)) {
            response.setAggregateConfig(buildAggregateConfig());
        }

        return response;
    }

    @Override
    public AutoMatchResponse autoMatch(AutoMatchRequest request) {
        log.info("智能匹配: dataType={}, sourceFields={}", request.getDataType(), request.getSourceFields().size());

        // 从实体类扫描获取目标字段
        List<TargetFieldInfo> targetFieldInfos = entityFieldScanner.getTargetFields(
                request.getDataType(),
                request.getSourceType()
        );

        List<MatchSuggestion> mappings = new ArrayList<>();
        Set<String> matchedSources = new HashSet<>();
        Set<String> matchedTargets = new HashSet<>();

        // 按优先级匹配
        for (String source : request.getSourceFields()) {
            if (matchedSources.contains(source)) continue;

            MatchSuggestion suggestion = findBestMatchFromInfo(
                    source,
                    targetFieldInfos,
                    matchedTargets,
                    request.getSiteCode()
            );

            if (suggestion != null) {
                mappings.add(suggestion);
                matchedSources.add(source);
                matchedTargets.add(suggestion.getTarget());
            }
        }

        // 未匹配的字段
        List<String> unmatchedSource = request.getSourceFields().stream()
                .filter(s -> !matchedSources.contains(s))
                .collect(Collectors.toList());

        List<String> unmatchedTarget = targetFieldInfos.stream()
                .map(TargetFieldInfo::getField)
                .filter(t -> !matchedTargets.contains(t))
                .collect(Collectors.toList());

        AutoMatchResponse response = new AutoMatchResponse();
        response.setMappings(mappings);
        response.setUnmatchedSource(unmatchedSource);
        response.setUnmatchedTarget(unmatchedTarget);

        log.info("智能匹配结果: matched={}, unmatchedSource={}, unmatchedTarget={}",
                mappings.size(), unmatchedSource.size(), unmatchedTarget.size());

        return response;
    }

    /**
     * 构建ERP聚合配置
     */
    private AggregateConfig buildAggregateConfig() {
        LambdaQueryWrapper<ErpAggregateRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ErpAggregateRule::getSortOrder);
        List<ErpAggregateRule> rules = aggregateRuleMapper.selectList(wrapper);

        Map<String, String> pivotMapping = rules.stream()
                .collect(Collectors.toMap(
                        ErpAggregateRule::getTransactionType,
                        ErpAggregateRule::getTargetField,
                        (v1, v2) -> v1 // 如果有重复，保留第一个
                ));

        AggregateConfig config = new AggregateConfig();
        config.setGroupBy(Arrays.asList("order_id", "sku", "site_code"));
        config.setPivotField("transaction_type");
        config.setValueField("amount");
        config.setPivotMapping(pivotMapping);

        return config;
    }

    /**
     * 查找最佳匹配
     */
    private MatchSuggestion findBestMatch(
            String source,
            List<TargetFieldMetadata> targets,
            Set<String> matchedTargets,
            String siteCode
    ) {
        String normalizedSource = normalize(source);

        for (TargetFieldMetadata target : targets) {
            if (matchedTargets.contains(target.getFieldName())) continue;

            // 1. 完全匹配
            if (source.equals(target.getFieldName())) {
                return createSuggestion(source, target.getFieldName(), 1.0, "exact");
            }

            // 2. 忽略大小写
            if (source.equalsIgnoreCase(target.getFieldName())) {
                return createSuggestion(source, target.getFieldName(), 0.95, "ignore_case");
            }

            // 3. 站点别名匹配
            if (StringUtils.hasText(siteCode) && target.getSiteAliases() != null) {
                String alias = target.getSiteAliases().get(siteCode);
                if (alias != null && source.equalsIgnoreCase(alias)) {
                    return createSuggestion(source, target.getFieldName(), 0.92, "alias");
                }
                // 尝试 default 别名
                String defaultAlias = target.getSiteAliases().get("default");
                if (defaultAlias != null && source.equalsIgnoreCase(defaultAlias)) {
                    return createSuggestion(source, target.getFieldName(), 0.9, "alias");
                }
            }

            // 4. 中文标签匹配
            if (source.equals(target.getFieldLabel())) {
                return createSuggestion(source, target.getFieldName(), 0.88, "label");
            }

            // 5. 规范化匹配
            if (normalizedSource.equals(normalize(target.getFieldName()))) {
                return createSuggestion(source, target.getFieldName(), 0.85, "normalized");
            }

            // 6. 包含匹配
            String normalizedTarget = normalize(target.getFieldName());
            if (normalizedSource.length() > 3 && normalizedTarget.length() > 3) {
                if (normalizedSource.contains(normalizedTarget) || normalizedTarget.contains(normalizedSource)) {
                    return createSuggestion(source, target.getFieldName(), 0.75, "contains");
                }
            }
        }

        // 7. 相似度匹配（Levenshtein距离）
        for (TargetFieldMetadata target : targets) {
            if (matchedTargets.contains(target.getFieldName())) continue;

            double similarity = calculateSimilarity(normalizedSource, normalize(target.getFieldName()));
            if (similarity > 0.7) {
                return createSuggestion(source, target.getFieldName(), similarity * 0.8, "similar");
            }
        }

        return null;
    }

    /**
     * 查找最佳匹配（基于 TargetFieldInfo）
     */
    private MatchSuggestion findBestMatchFromInfo(
            String source,
            List<TargetFieldInfo> targets,
            Set<String> matchedTargets,
            String siteCode
    ) {
        String normalizedSource = normalize(source);

        for (TargetFieldInfo target : targets) {
            if (matchedTargets.contains(target.getField())) continue;

            // 1. 完全匹配
            if (source.equals(target.getField())) {
                return createSuggestion(source, target.getField(), 1.0, "exact");
            }

            // 2. 忽略大小写
            if (source.equalsIgnoreCase(target.getField())) {
                return createSuggestion(source, target.getField(), 0.95, "ignore_case");
            }

            // 3. 站点别名匹配
            if (StringUtils.hasText(siteCode) && target.getSiteAliases() != null) {
                String alias = target.getSiteAliases().get(siteCode);
                if (alias != null && source.equalsIgnoreCase(alias)) {
                    return createSuggestion(source, target.getField(), 0.92, "alias");
                }
                // 尝试 default 别名
                String defaultAlias = target.getSiteAliases().get("default");
                if (defaultAlias != null && source.equalsIgnoreCase(defaultAlias)) {
                    return createSuggestion(source, target.getField(), 0.9, "alias");
                }
            }

            // 4. 中文标签匹配
            if (source.equals(target.getLabel())) {
                return createSuggestion(source, target.getField(), 0.88, "label");
            }

            // 5. 规范化匹配
            if (normalizedSource.equals(normalize(target.getField()))) {
                return createSuggestion(source, target.getField(), 0.85, "normalized");
            }

            // 6. 包含匹配
            String normalizedTarget = normalize(target.getField());
            if (normalizedSource.length() > 3 && normalizedTarget.length() > 3) {
                if (normalizedSource.contains(normalizedTarget) || normalizedTarget.contains(normalizedSource)) {
                    return createSuggestion(source, target.getField(), 0.75, "contains");
                }
            }
        }

        // 7. 相似度匹配（Levenshtein距离）
        for (TargetFieldInfo target : targets) {
            if (matchedTargets.contains(target.getField())) continue;

            double similarity = calculateSimilarity(normalizedSource, normalize(target.getField()));
            if (similarity > 0.7) {
                return createSuggestion(source, target.getField(), similarity * 0.8, "similar");
            }
        }

        return null;
    }

    /**
     * 规范化字符串
     */
    private String normalize(String str) {
        return str.toLowerCase().replaceAll("[\\-_\\s.]", "");
    }

    /**
     * 计算相似度（Levenshtein距离）
     */
    private double calculateSimilarity(String s1, String s2) {
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        return 1.0 - (double) distance / maxLength;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private MatchSuggestion createSuggestion(String source, String target, double confidence, String matchType) {
        MatchSuggestion suggestion = new MatchSuggestion();
        suggestion.setSource(source);
        suggestion.setTarget(target);
        suggestion.setConfidence(confidence);
        suggestion.setMatchType(matchType);
        return suggestion;
    }

    private TargetFieldVO convertToVO(TargetFieldMetadata metadata) {
        TargetFieldVO vo = new TargetFieldVO();
        vo.setField(metadata.getFieldName());
        vo.setLabel(metadata.getFieldLabel());
        vo.setDescription(metadata.getFieldDescription());
        vo.setType(metadata.getFieldType());
        vo.setRequired(metadata.getRequired());
        vo.setMaxLength(metadata.getMaxLength());
        vo.setPrecision(metadata.getPrecision());
        vo.setSiteAliases(metadata.getSiteAliases());
        return vo;
    }

    /**
     * 将 TargetFieldInfo 转换为 TargetFieldVO
     */
    private TargetFieldVO convertInfoToVO(TargetFieldInfo info) {
        TargetFieldVO vo = new TargetFieldVO();
        vo.setField(info.getField());
        vo.setLabel(info.getLabel());
        vo.setDescription(info.getDescription());
        vo.setType(info.getType());
        vo.setRequired(info.getRequired());
        vo.setMaxLength(info.getMaxLength());
        vo.setPrecision(info.getPrecision());
        vo.setSiteAliases(info.getSiteAliases());
        vo.setSortOrder(info.getSortOrder());
        return vo;
    }
}
