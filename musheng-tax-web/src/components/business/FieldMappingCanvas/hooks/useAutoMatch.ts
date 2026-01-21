/**
 * 智能匹配 Hook
 * 用于自动匹配源字段和目标字段
 */

import { ref } from 'vue'
import { autoMatchFields } from '@/api/fieldMapping'
import type {
  SourceField,
  TargetField,
  MappingConfig,
  AutoMatchResponse,
  MatchSuggestion,
  MatchType
} from '../types'

export function useAutoMatch() {
  const matching = ref(false)
  const error = ref<string | null>(null)

  /**
   * 执行智能匹配
   * 优先使用前端匹配，可选调用后端API
   */
  const executeAutoMatch = async (
    sourceFields: SourceField[],
    targetFields: TargetField[],
    siteCode?: string,
    useBackend: boolean = false,
    dataType: string = 'SALES',
    sourceType?: string
  ): Promise<AutoMatchResponse> => {
    matching.value = true
    error.value = null

    try {
      if (useBackend) {
        // 调用后端API
        const response = await autoMatchFields({
          dataType,
          sourceType,
          sourceFields: sourceFields.map((f) => f.name),
          siteCode
        })
        return response.data // API 返回 { code, message, data }
      }

      // 前端匹配逻辑
      return localAutoMatch(sourceFields, targetFields, siteCode)
    } catch (e: any) {
      error.value = e.message
      throw e
    } finally {
      matching.value = false
    }
  }

  /**
   * 前端本地匹配算法
   */
  const localAutoMatch = (
    sourceFields: SourceField[],
    targetFields: TargetField[],
    siteCode?: string
  ): AutoMatchResponse => {
    const mappings: MatchSuggestion[] = []
    const matchedSources = new Set<string>()
    const matchedTargets = new Set<string>()

    // 按优先级依次尝试匹配
    const matchers: Array<{
      type: MatchType
      match: (source: string, target: TargetField) => boolean
      confidence: number
    }> = [
      // 1. 完全匹配
      {
        type: 'exact',
        match: (s, t) => s === t.field,
        confidence: 1.0
      },
      // 2. 忽略大小写
      {
        type: 'ignore_case',
        match: (s, t) => s.toLowerCase() === t.field.toLowerCase(),
        confidence: 0.95
      },
      // 3. 站点别名匹配
      {
        type: 'alias',
        match: (s, t) => {
          if (!t.siteAliases) return false
          const alias = siteCode ? t.siteAliases[siteCode] : t.siteAliases['default']
          return alias ? s.toLowerCase() === alias.toLowerCase() : false
        },
        confidence: 0.9
      },
      // 4. 中文标签匹配
      {
        type: 'label',
        match: (s, t) => s === t.label || normalize(s) === normalize(t.label),
        confidence: 0.85
      },
      // 5. 规范化匹配（忽略分隔符）
      {
        type: 'normalized',
        match: (s, t) => normalize(s) === normalize(t.field),
        confidence: 0.8
      },
      // 6. 包含匹配
      {
        type: 'contains',
        match: (s, t) => {
          const ns = normalize(s)
          const nt = normalize(t.field)
          return ns.length > 3 && nt.length > 3 && (ns.includes(nt) || nt.includes(ns))
        },
        confidence: 0.7
      }
    ]

    // 遍历匹配器
    for (const matcher of matchers) {
      for (const source of sourceFields) {
        if (matchedSources.has(source.name)) continue

        for (const target of targetFields) {
          if (matchedTargets.has(target.field)) continue

          if (matcher.match(source.name, target)) {
            mappings.push({
              source: source.name,
              target: target.field,
              confidence: matcher.confidence,
              matchType: matcher.type
            })
            matchedSources.add(source.name)
            matchedTargets.add(target.field)
            break
          }
        }
      }
    }

    // 相似度匹配（Levenshtein距离）
    for (const source of sourceFields) {
      if (matchedSources.has(source.name)) continue

      let bestMatch: { target: TargetField; similarity: number } | null = null

      for (const target of targetFields) {
        if (matchedTargets.has(target.field)) continue

        const similarity = calculateSimilarity(normalize(source.name), normalize(target.field))
        if (similarity > 0.7 && (!bestMatch || similarity > bestMatch.similarity)) {
          bestMatch = { target, similarity }
        }
      }

      if (bestMatch) {
        mappings.push({
          source: source.name,
          target: bestMatch.target.field,
          confidence: bestMatch.similarity * 0.8,
          matchType: 'similar'
        })
        matchedSources.add(source.name)
        matchedTargets.add(bestMatch.target.field)
      }
    }

    // 未匹配的字段
    const unmatchedSource = sourceFields
      .filter((f) => !matchedSources.has(f.name))
      .map((f) => f.name)

    const unmatchedTarget = targetFields
      .filter((f) => !matchedTargets.has(f.field))
      .map((f) => f.field)

    return {
      mappings,
      unmatchedSource,
      unmatchedTarget
    }
  }

  /**
   * 规范化字符串（移除分隔符）
   */
  const normalize = (str: string): string => {
    return str.toLowerCase().replace(/[-_\s.]/g, '')
  }

  /**
   * 计算字符串相似度（Levenshtein 距离）
   */
  const calculateSimilarity = (s1: string, s2: string): number => {
    const a = s1.toLowerCase()
    const b = s2.toLowerCase()

    if (a === b) return 1
    if (a.length === 0 || b.length === 0) return 0

    const matrix: number[][] = []

    for (let i = 0; i <= b.length; i++) {
      matrix[i] = [i]
    }
    for (let j = 0; j <= a.length; j++) {
      matrix[0][j] = j
    }

    for (let i = 1; i <= b.length; i++) {
      for (let j = 1; j <= a.length; j++) {
        if (b.charAt(i - 1) === a.charAt(j - 1)) {
          matrix[i][j] = matrix[i - 1][j - 1]
        } else {
          matrix[i][j] = Math.min(
            matrix[i - 1][j - 1] + 1,
            matrix[i][j - 1] + 1,
            matrix[i - 1][j] + 1
          )
        }
      }
    }

    const distance = matrix[b.length][a.length]
    const maxLength = Math.max(a.length, b.length)
    return 1 - distance / maxLength
  }

  /**
   * 将匹配建议转换为映射配置
   */
  const suggestionsToMappings = (suggestions: MatchSuggestion[]): MappingConfig[] => {
    return suggestions.map((s) => ({
      source: s.source,
      target: s.target
    }))
  }

  return {
    matching,
    error,
    executeAutoMatch,
    localAutoMatch,
    suggestionsToMappings
  }
}
