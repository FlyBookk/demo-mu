/**
 * 通用类型定义
 */

// 选项类型
export interface Option {
  label: string
  value: string | number
  disabled?: boolean
  children?: Option[]
}

// 状态类型
export interface StatusOption {
  label: string
  value: number
  color: string
}

// 表格列定义
export interface TableColumn {
  title: string
  dataIndex: string
  key?: string
  width?: number | string
  fixed?: 'left' | 'right'
  align?: 'left' | 'center' | 'right'
  sorter?: boolean
  filters?: { text: string; value: any }[]
  customRender?: (opt: { text: any; record: any; index: number }) => any
  ellipsis?: boolean
}

// 树节点
export interface TreeNode {
  key: string | number
  title: string
  children?: TreeNode[]
  disabled?: boolean
  selectable?: boolean
  isLeaf?: boolean
  [key: string]: any
}

// 菜单项
export interface MenuItem {
  key: string
  label: string
  icon?: string
  path?: string
  children?: MenuItem[]
  permission?: string
  roles?: string[]
  hidden?: boolean
}

// 面包屑项
export interface BreadcrumbItem {
  title: string
  path?: string
}
