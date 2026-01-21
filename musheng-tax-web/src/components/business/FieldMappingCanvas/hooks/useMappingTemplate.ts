/**
 * 模板管理逻辑Hook
 */

import { ref } from 'vue'
import { message } from 'ant-design-vue'
import type { MappingTemplate, MappingConfig } from '../types'

// 模拟API（实际项目中替换为真实API调用）
async function fetchTemplates(siteCode?: string): Promise<MappingTemplate[]> {
  // TODO: 替换为真实API调用
  return []
}

async function saveTemplate(template: Partial<MappingTemplate>): Promise<MappingTemplate> {
  // TODO: 替换为真实API调用
  return template as MappingTemplate
}

export function useMappingTemplate(siteCode?: string) {
  // 模板列表
  const templates = ref<MappingTemplate[]>([])
  // 当前模板
  const currentTemplate = ref<MappingTemplate | null>(null)
  // 加载状态
  const loading = ref(false)

  /**
   * 加载模板列表
   */
  async function loadTemplates() {
    loading.value = true
    try {
      templates.value = await fetchTemplates(siteCode)
    } catch (error) {
      message.error('加载模板列表失败')
    } finally {
      loading.value = false
    }
  }

  /**
   * 加载指定模板
   */
  async function loadTemplate(templateId: number): Promise<MappingConfig[]> {
    loading.value = true
    try {
      const template = templates.value.find(t => t.id === templateId)
      if (template) {
        currentTemplate.value = template
        return template.mappings
      }
      throw new Error('模板不存在')
    } catch (error) {
      message.error('加载模板失败')
      return []
    } finally {
      loading.value = false
    }
  }

  /**
   * 保存为模板
   */
  async function saveAsTemplate(
    templateName: string,
    mappings: MappingConfig[],
    isDefault: boolean = false
  ): Promise<boolean> {
    loading.value = true
    try {
      const saved = await saveTemplate({
        templateName,
        siteCode,
        mappings,
        isDefault,
        unmappedDefaults: {}
      })
      templates.value.push(saved)
      currentTemplate.value = saved
      message.success('模板保存成功')
      return true
    } catch (error) {
      message.error('保存模板失败')
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取默认模板
   */
  function getDefaultTemplate(): MappingTemplate | null {
    return templates.value.find(t => t.isDefault && t.siteCode === siteCode) || null
  }

  /**
   * 设置当前模板
   */
  function setCurrentTemplate(template: MappingTemplate | null) {
    currentTemplate.value = template
  }

  return {
    templates,
    currentTemplate,
    loading,
    loadTemplates,
    loadTemplate,
    saveAsTemplate,
    getDefaultTemplate,
    setCurrentTemplate
  }
}
