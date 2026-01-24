/**
 * 店铺管理类型定义
 * 与后端 ShopController 接口对齐
 */

// 店铺信息（对应后端 Shop 实体）
export interface Shop {
  id: number
  shopCode: string        // 店铺编码
  shopName: string        // 店铺名称
  sellerId?: string       // 亚马逊卖家ID
  companyName?: string    // 公司名称
  taxId?: string          // 统一社会信用代码
  status: number          // 状态 1-启用 0-禁用
  remark?: string         // 备注
  createTime: string
  updateTime?: string
}

// 店铺查询参数（对应后端 ShopQueryRequest）
export interface ShopQuery {
  shopCode?: string       // 店铺编码（模糊匹配）
  shopName?: string       // 店铺名称（模糊匹配）
  status?: number         // 状态
  page?: number
  size?: number
}

// 店铺创建/更新参数（对应后端 ShopRequest）
export interface ShopForm {
  id?: number
  shopCode: string
  shopName: string
  sellerId?: string
  companyName?: string
  taxId?: string
  status?: number
  remark?: string
}
