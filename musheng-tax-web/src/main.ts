import { createApp } from 'vue'
import { createPinia } from 'pinia'
import VXETable from 'vxe-table'

import App from './App.vue'
import router from './router'

// 样式导入（ant-design-vue 组件样式由 unplugin-vue-components 按需自动引入）
import 'ant-design-vue/dist/reset.css'
import 'vxe-table/lib/style.css'
import '@/assets/styles/global.scss'

// 自定义指令
import { setupDirectives } from '@/directives'

const app = createApp(App)

// 注册Pinia
const pinia = createPinia()
app.use(pinia)

// 注册路由
app.use(router)

// 注册VXE Table（vxe-table 暂保留全量注册，后续可按需优化）
app.use(VXETable)

// 注册自定义指令
setupDirectives(app)

app.mount('#app')
