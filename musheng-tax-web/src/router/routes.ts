/**
 * 路由配置 - 路由定义
 */

import type { RouteRecordRaw } from 'vue-router'

// 主布局
const MainLayout = () => import('@/layouts/MainLayout.vue')

// 路由配置
export const routes: RouteRecordRaw[] = [
  // 登录页
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: {
      title: '登录',
      requiresAuth: false
    }
  },

  // 主布局路由
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      // 首页
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: {
          title: '首页',
          icon: 'DashboardOutlined',
          requiresAuth: true
        }
      },

      // ========== 基础配置 ==========
      {
        path: 'config',
        name: 'Config',
        redirect: '/config/currency',
        meta: {
          title: '基础配置',
          icon: 'SettingOutlined'
        },
        children: [
          {
            path: 'currency',
            name: 'Currency',
            component: () => import('@/views/config/currency/index.vue'),
            meta: {
              title: '货币管理',
              permission: 'config:currency'
            }
          },
          {
            path: 'marketplace',
            name: 'Marketplace',
            component: () => import('@/views/config/marketplace/index.vue'),
            meta: {
              title: '站点管理',
              permission: 'config:marketplace'
            }
          },
          {
            path: 'transaction-type',
            name: 'TransactionType',
            component: () => import('@/views/config/transactionType/index.vue'),
            meta: {
              title: '交易类型映射',
              permission: 'config:transactionType'
            }
          },
          {
            path: 'field-mapping',
            name: 'FieldMapping',
            component: () => import('@/views/config/fieldMapping/index.vue'),
            meta: {
              title: '字段映射模板',
              permission: 'config:fieldMapping'
            }
          },
          {
            path: 'field-mapping/add',
            name: 'FieldMappingAdd',
            component: () => import('@/views/config/fieldMapping/edit.vue'),
            meta: {
              title: '新增映射模板',
              permission: 'config:fieldMapping',
              hideInMenu: true
            }
          },
          {
            path: 'field-mapping/edit/:id',
            name: 'FieldMappingEdit',
            component: () => import('@/views/config/fieldMapping/edit.vue'),
            meta: {
              title: '编辑映射模板',
              permission: 'config:fieldMapping',
              hideInMenu: true
            }
          },
          {
            path: 'shop',
            name: 'Shop',
            component: () => import('@/views/config/shop/index.vue'),
            meta: {
              title: '店铺管理',
              permission: 'config:shop'
            }
          },
          {
            path: 'import-record',
            name: 'ImportRecord',
            component: () => import('@/views/config/importRecord/index.vue'),
            meta: {
              title: '导入记录',
              permission: 'config:importRecord'
            }
          },
          {
            path: 'data-clean',
            name: 'DataClean',
            component: () => import('@/views/config/dataClean/index.vue'),
            meta: {
              title: '数据清理',
              permission: 'config:dataClean'
            }
          },
          {
            path: 'export-setting',
            name: 'ExportSetting',
            component: () => import('@/views/config/exportSetting/index.vue'),
            meta: {
              title: '导出设置',
              permission: 'config:exportSetting'
            }
          }
        ]
      },

      // ========== 销售数据 ==========
      {
        path: 'sales',
        name: 'Sales',
        redirect: '/sales/list',
        meta: {
          title: '销售数据',
          icon: 'ShoppingCartOutlined'
        },
        children: [
          {
            path: 'import',
            name: 'SalesImport',
            component: () => import('@/views/sales/import/index.vue'),
            meta: {
              title: '数据导入',
              permission: 'sales:import'
            }
          },
          {
            path: 'list',
            name: 'SalesList',
            component: () => import('@/views/sales/list/index.vue'),
            meta: {
              title: '数据列表',
              permission: 'sales:list'
            }
          }
        ]
      },

      // ========== 配送数据 ==========
      {
        path: 'shipping',
        name: 'Shipping',
        redirect: '/shipping/list',
        meta: {
          title: '配送数据',
          icon: 'CarOutlined'
        },
        children: [
          {
            path: 'import',
            name: 'ShippingImport',
            component: () => import('@/views/shipping/import/index.vue'),
            meta: {
              title: '数据导入',
              permission: 'shipping:import'
            }
          },
          {
            path: 'list',
            name: 'ShippingList',
            component: () => import('@/views/shipping/list/index.vue'),
            meta: {
              title: '数据列表',
              permission: 'shipping:list'
            }
          }
        ]
      },

      // ========== FBA货件明细 ==========
      {
        path: 'fba-shipment',
        name: 'FbaShipment',
        redirect: '/fba-shipment/list',
        meta: {
          title: 'FBA货件明细',
          icon: 'InboxOutlined'
        },
        children: [
          {
            path: 'import',
            name: 'FbaShipmentImport',
            component: () => import('@/views/fba-shipment/import/index.vue'),
            meta: {
              title: '数据导入',
              permission: 'fba-shipment:import'
            }
          },
          {
            path: 'list',
            name: 'FbaShipmentList',
            component: () => import('@/views/fba-shipment/list/index.vue'),
            meta: {
              title: '数据列表',
              permission: 'fba-shipment:list'
            }
          },
          {
            path: 'detail',
            name: 'FbaShipmentDetail',
            component: () => import('@/views/fba-shipment/detail/index.vue'),
            meta: {
              title: 'SKU明细',
              permission: 'fba-shipment:list',
              hideInMenu: true
            }
          }
        ]
      },

      // ========== FBA单据管理 ==========
      {
        path: 'document',
        name: 'Document',
        redirect: '/document/list',
        meta: {
          title: 'FBA单据管理',
          icon: 'FileTextOutlined'
        },
        children: [
          {
            path: 'list',
            name: 'DocumentList',
            component: () => import('@/views/document/list/index.vue'),
            meta: {
              title: '单据列表',
              permission: 'document:list'
            }
          },
          {
            path: 'generate',
            name: 'DocumentGenerate',
            component: () => import('@/views/document/generate/index.vue'),
            meta: {
              title: 'PO/DN生成',
              permission: 'document:generate'
            }
          },
          {
            path: 'settlement-generate',
            name: 'SettlementGenerate',
            component: () => import('@/views/document/settlement-generate/index.vue'),
            meta: {
              title: '结算单/INV生成',
              permission: 'document:generate'
            }
          },
          // 结算推导：根据结算数据推导税务相关金额
          {
            path: 'settlement-derivation',
            name: 'SettlementDerivation',
            component: () => import('@/views/document/settlement-derivation/index.vue'),
            meta: {
              title: '结算推导',
              permission: 'document:derivation'
            }
          },
          {
            path: 'msku-list',
            name: 'MskuList',
            component: () => import('@/views/document/msku-list/index.vue'),
            meta: {
              title: 'MSKU列表',
              permission: 'document:msku'
            }
          },
          {
            path: 'party-config',
            name: 'DocumentPartyConfig',
            component: () => import('@/views/document/party-config/index.vue'),
            meta: {
              title: '交易方配置',
              permission: 'document:partyConfig'
            }
          }
        ]
      },

      // ========== 广告数据 ==========
      {
        path: 'advertising',
        name: 'Advertising',
        redirect: '/advertising/list',
        meta: {
          title: '广告数据',
          icon: 'NotificationOutlined'
        },
        children: [
          {
            path: 'import',
            name: 'AdvertisingImport',
            component: () => import('@/views/advertising/import/index.vue'),
            meta: {
              title: '广告费录入',
              permission: 'advertising:import'
            }
          },
          {
            path: 'list',
            name: 'AdvertisingList',
            component: () => import('@/views/advertising/list/index.vue'),
            meta: {
              title: '数据列表',
              permission: 'advertising:list'
            }
          },
          {
            path: 'detail',
            name: 'AdvertisingDetail',
            component: () => import('@/views/advertising/detail/index.vue'),
            meta: {
              title: '活动明细',
              permission: 'advertising:list',
              hideInMenu: true
            }
          }
        ]
      },

      // ========== 汇率管理 ==========
      {
        path: 'rate',
        name: 'Rate',
        redirect: '/rate/list',
        meta: {
          title: '汇率管理',
          icon: 'DollarOutlined'
        },
        children: [
          {
            path: 'import',
            name: 'RateImport',
            component: () => import('@/views/rate/import/index.vue'),
            meta: {
              title: '汇率导入',
              permission: 'rate:import'
            }
          },
          {
            path: 'list',
            name: 'RateList',
            component: () => import('@/views/rate/list/index.vue'),
            meta: {
              title: '汇率查询',
              permission: 'rate:list'
            }
          }
        ]
      },

      // ========== 报税汇总 ==========
      {
        path: 'report',
        name: 'Report',
        redirect: '/report/tax-summary',
        meta: {
          title: '报税汇总',
          icon: 'BarChartOutlined'
        },
        children: [
          {
            path: 'tax-summary',
            name: 'TaxSummary',
            component: () => import('@/views/report/tax-summary/index.vue'),
            meta: {
              title: '报税汇总',
              permission: 'report:taxSummary'
            }
          }
        ]
      },

      // ========== 系统管理 ==========
      {
        path: 'system',
        name: 'System',
        redirect: '/system/user',
        meta: {
          title: '系统管理',
          icon: 'ToolOutlined',
          roles: ['admin']
        },
        children: [
          {
            path: 'user',
            name: 'SystemUser',
            component: () => import('@/views/system/user/index.vue'),
            meta: {
              title: '用户管理',
              permission: 'system:user'
            }
          },
          {
            path: 'role',
            name: 'SystemRole',
            component: () => import('@/views/system/role/index.vue'),
            meta: {
              title: '权限管理',
              permission: 'system:role'
            }
          },
          {
            path: 'log',
            name: 'SystemLog',
            component: () => import('@/views/system/log/index.vue'),
            meta: {
              title: '操作日志',
              permission: 'system:log'
            }
          }
        ]
      },

      // ========== TikTok 商品管理 ==========
      {
        path: 'tiktok/product',
        name: 'TiktokProduct',
        redirect: '/tiktok/product/list',
        meta: {
          title: '商品管理',
          icon: 'TagOutlined',
          platform: 'TIKTOK'
        },
        children: [
          {
            path: 'list',
            name: 'TiktokProductList',
            component: () => import('@/views/tiktok/product/index.vue'),
            meta: { title: '商品列表', platform: 'TIKTOK' }
          },
          {
            path: 'import',
            name: 'TiktokProductImport',
            component: () => import('@/views/tiktok/product/import.vue'),
            meta: { title: '对照表导入', platform: 'TIKTOK' }
          }
        ]
      },

      // ========== TikTok FBT货件 ==========
      {
        path: 'tiktok/shipment',
        name: 'TiktokShipment',
        redirect: '/tiktok/shipment/list',
        meta: {
          title: 'FBT货件',
          icon: 'InboxOutlined',
          platform: 'TIKTOK'
        },
        children: [
          {
            path: 'list',
            name: 'TiktokShipmentList',
            component: () => import('@/views/tiktok/shipment/index.vue'),
            meta: { title: '货件列表', platform: 'TIKTOK' }
          },
          {
            path: 'import',
            name: 'TiktokShipmentImport',
            component: () => import('@/views/tiktok/shipment/import.vue'),
            meta: { title: '货件导入', platform: 'TIKTOK' }
          }
        ]
      },

      // ========== TikTok 结算数据 ==========
      {
        path: 'tiktok/settlement',
        name: 'TiktokSettlement',
        redirect: '/tiktok/settlement/orders',
        meta: {
          title: '结算数据',
          icon: 'DollarOutlined',
          platform: 'TIKTOK'
        },
        children: [
          {
            path: 'import',
            name: 'TiktokSettlementImport',
            component: () => import('@/views/tiktok/settlement/import.vue'),
            meta: { title: '结算单导入', platform: 'TIKTOK' }
          },
          {
            path: 'orders',
            name: 'TiktokSettlementOrders',
            component: () => import('@/views/tiktok/settlement/orders.vue'),
            meta: { title: '订单明细', platform: 'TIKTOK' }
          }
        ]
      },

      // ========== TikTok 单据管理 ==========
      {
        path: 'tiktok/document',
        name: 'TiktokDocument',
        redirect: '/tiktok/document/list',
        meta: {
          title: '单据管理',
          icon: 'FileTextOutlined',
          platform: 'TIKTOK'
        },
        children: [
          {
            path: 'derivation',
            name: 'TiktokDerivation',
            component: () => import('@/views/tiktok/document/derivation.vue'),
            meta: { title: '结算推导', platform: 'TIKTOK' }
          },
          {
            path: 'msku-list',
            name: 'TiktokMskuList',
            component: () => import('@/views/tiktok/document/msku-list.vue'),
            meta: { title: 'MSKU列表', platform: 'TIKTOK' }
          },
          {
            path: 'list',
            name: 'TiktokDocumentList',
            component: () => import('@/views/tiktok/document/index.vue'),
            meta: { title: '单据列表', platform: 'TIKTOK' }
          },
          {
            path: 'generate',
            name: 'TiktokDocumentGenerate',
            component: () => import('@/views/tiktok/document/generate.vue'),
            meta: { title: 'PO/DN生成', platform: 'TIKTOK' }
          },
          {
            path: 'settlement-generate',
            name: 'TiktokSettlementGenerate',
            component: () => import('@/views/tiktok/document/settlement-generate.vue'),
            meta: { title: '结算单/INV生成', platform: 'TIKTOK' }
          },
          {
            path: 'party-config',
            name: 'TiktokPartyConfig',
            component: () => import('@/views/tiktok/party-config/index.vue'),
            meta: { title: '交易方配置', platform: 'TIKTOK' }
          }
        ]
      },

      // ========== TikTok 报税汇总 ==========
      {
        path: 'tiktok/report',
        name: 'TiktokReport',
        component: () => import('@/views/tiktok/report/index.vue'),
        meta: {
          title: '报税汇总',
          icon: 'BarChartOutlined',
          platform: 'TIKTOK'
        }
      }
    ]
  },

  // 404页面
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: {
      title: '页面不存在',
      requiresAuth: false
    }
  }
]

export default routes
