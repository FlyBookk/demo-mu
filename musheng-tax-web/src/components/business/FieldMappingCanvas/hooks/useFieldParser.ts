/**
 * 文件/粘贴解析 Hook
 * 用于解析上传的文件或粘贴的表头文本
 */

import { ref } from 'vue'
import * as XLSX from 'xlsx'
import Papa from 'papaparse'
import type {
  SourceField,
  FilePreviewResponse,
  FilePreviewRow,
  ParseFieldsResponse,
  DelimiterType
} from '../types'

export function useFieldParser() {
  const parsing = ref(false)
  const error = ref<string | null>(null)

  /**
   * 预览文件（获取前N行）
   */
  const previewFile = async (
    file: File,
    previewRows: number = 10
  ): Promise<FilePreviewResponse> => {
    parsing.value = true
    error.value = null

    try {
      const extension = file.name.split('.').pop()?.toLowerCase()

      if (extension === 'csv') {
        return await previewCsvFile(file, previewRows)
      } else if (['xlsx', 'xls'].includes(extension || '')) {
        return await previewExcelFile(file, previewRows)
      } else {
        throw new Error('不支持的文件格式')
      }
    } catch (e: any) {
      error.value = e.message
      throw e
    } finally {
      parsing.value = false
    }
  }

  /**
   * 预览 CSV 文件
   */
  const previewCsvFile = (
    file: File,
    previewRows: number
  ): Promise<FilePreviewResponse> => {
    return new Promise((resolve, reject) => {
      let rowCount = 0
      const rows: FilePreviewRow[] = []
      let detectedDelimiter = ','

      Papa.parse(file, {
        preview: previewRows,
        step: (results) => {
          rowCount++
          const cells = results.data as string[]
          rows.push({
            rowNum: rowCount,
            content: cells.join('\t'),
            cells
          })

          // 检测分隔符
          if (rowCount === 1) {
            detectedDelimiter = results.meta.delimiter || ','
          }
        },
        complete: () => {
          resolve({
            rows,
            totalRows: rowCount,
            encoding: 'UTF-8',
            delimiter: detectedDelimiter
          })
        },
        error: (err) => {
          reject(new Error(err.message))
        }
      })
    })
  }

  /**
   * 预览 Excel 文件
   */
  const previewExcelFile = async (
    file: File,
    previewRows: number
  ): Promise<FilePreviewResponse> => {
    const arrayBuffer = await file.arrayBuffer()
    const workbook = XLSX.read(arrayBuffer, { type: 'array' })

    const sheets = workbook.SheetNames
    const sheetName = sheets[0]
    const worksheet = workbook.Sheets[sheetName]

    // 转换为 JSON，保留前 N 行
    const jsonData = XLSX.utils.sheet_to_json<string[]>(worksheet, {
      header: 1,
      defval: '',
      range: 0
    }).slice(0, previewRows)

    const rows: FilePreviewRow[] = jsonData.map((row, index) => ({
      rowNum: index + 1,
      content: row.join('\t'),
      cells: row.map(String)
    }))

    // 获取总行数
    const range = XLSX.utils.decode_range(worksheet['!ref'] || 'A1')
    const totalRows = range.e.r + 1

    return {
      rows,
      totalRows,
      encoding: 'UTF-8',
      delimiter: '\t',
      sheetName,
      sheets
    }
  }

  /**
   * 解析文件获取字段
   */
  const parseFile = async (
    file: File,
    headerRow: number = 1,
    sheetName?: string
  ): Promise<ParseFieldsResponse> => {
    parsing.value = true
    error.value = null

    try {
      const extension = file.name.split('.').pop()?.toLowerCase()

      if (extension === 'csv') {
        return await parseCsvFields(file, headerRow)
      } else if (['xlsx', 'xls'].includes(extension || '')) {
        return await parseExcelFields(file, headerRow, sheetName)
      } else {
        throw new Error('不支持的文件格式')
      }
    } catch (e: any) {
      error.value = e.message
      throw e
    } finally {
      parsing.value = false
    }
  }

  /**
   * 解析 CSV 字段
   */
  const parseCsvFields = (
    file: File,
    headerRow: number
  ): Promise<ParseFieldsResponse> => {
    return new Promise((resolve, reject) => {
      let currentRow = 0
      let headerFields: string[] = []
      let sampleValues: string[] = []
      let detectedDelimiter = ','

      Papa.parse(file, {
        step: (results, parser) => {
          currentRow++

          if (currentRow === headerRow) {
            headerFields = results.data as string[]
            detectedDelimiter = results.meta.delimiter || ','
          } else if (currentRow === headerRow + 1) {
            sampleValues = results.data as string[]
            parser.abort()
          }
        },
        complete: () => {
          const fields: SourceField[] = headerFields
            .filter((name) => name.trim() !== '')
            .map((name, index) => ({
              name: name.trim(),
              sample: sampleValues[index]?.substring(0, 30) || '',
              index
            }))

          resolve({
            fields,
            totalColumns: headerFields.length,
            headerRow,
            encoding: 'UTF-8',
            delimiter: detectedDelimiter
          })
        },
        error: reject
      })
    })
  }

  /**
   * 解析 Excel 字段
   */
  const parseExcelFields = async (
    file: File,
    headerRow: number,
    sheetName?: string
  ): Promise<ParseFieldsResponse> => {
    const arrayBuffer = await file.arrayBuffer()
    const workbook = XLSX.read(arrayBuffer, { type: 'array' })

    const sheet = sheetName || workbook.SheetNames[0]
    const worksheet = workbook.Sheets[sheet]

    const jsonData = XLSX.utils.sheet_to_json<string[]>(worksheet, {
      header: 1,
      defval: '',
      range: headerRow - 1
    })

    const headerFields = jsonData[0] || []
    const sampleValues = jsonData[1] || []

    const fields: SourceField[] = headerFields
      .filter((name: any) => name?.toString().trim() !== '')
      .map((name: any, index: number) => ({
        name: name.toString().trim(),
        sample: sampleValues[index]?.toString().substring(0, 30) || '',
        index
      }))

    return {
      fields,
      totalColumns: headerFields.length,
      headerRow,
      encoding: 'UTF-8',
      delimiter: '\t'
    }
  }

  /**
   * 解析粘贴的表头文本
   */
  const parseHeaderText = (
    text: string,
    delimiterType: DelimiterType = 'auto'
  ): SourceField[] => {
    let delimiter: string | RegExp

    if (delimiterType === 'auto') {
      delimiter = detectDelimiter(text)
    } else {
      const delimiterMap: Record<DelimiterType, string | RegExp> = {
        auto: '\t',
        tab: '\t',
        comma: ',',
        semicolon: ';',
        pipe: '|',
        space: /\s+/
      }
      delimiter = delimiterMap[delimiterType]
    }

    const fields = text
      .split(delimiter)
      .map((s) => s.trim())
      .filter((s) => s !== '')

    return fields.map((name, index) => ({
      name,
      index,
      sample: undefined
    }))
  }

  /**
   * 自动检测分隔符
   */
  const detectDelimiter = (text: string): string => {
    if (text.includes('\t')) return '\t'

    const commaCount = (text.match(/,/g) || []).length
    const semicolonCount = (text.match(/;/g) || []).length
    const pipeCount = (text.match(/\|/g) || []).length

    if (commaCount > 2) return ','
    if (semicolonCount > 2) return ';'
    if (pipeCount > 2) return '|'

    return ' '
  }

  return {
    parsing,
    error,
    previewFile,
    parseFile,
    parseHeaderText,
    detectDelimiter
  }
}
