import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import legacy from '@vitejs/plugin-legacy'
import compression from 'vite-plugin-compression'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    legacy({
      targets: ['Chrome >= 90', 'Edge >= 90', 'Firefox >= 90', 'Safari >= 14'],
      additionalLegacyPolyfills: ['regenerator-runtime/runtime']
    }),
    // 构建时预生成 .gz 文件，配合 Nginx gzip_static 直接返回，减少服务器 CPU 消耗
    compression({
      algorithm: 'gzip',
      ext: '.gz',
      threshold: 10240, // 10KB 以上才压缩
      deleteOriginFile: false // 保留原文件，兼容不支持 gzip 的客户端
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8888',
        changeOrigin: true
      }
    }
  },
  build: {
    target: 'es2015',
    outDir: 'dist',
    assetsDir: 'assets',
    rollupOptions: {
      output: {
        // 统一输出到 assets/ 目录，与 assetsDir 保持一致
        chunkFileNames: 'assets/[name]-[hash].js',
        entryFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash].[ext]',
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'antd-vendor': ['ant-design-vue', '@ant-design/icons-vue'],
          'chart-vendor': ['echarts', 'vue-echarts'],
          'table-vendor': ['vxe-table', 'xe-utils']
        }
      }
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@import "@/assets/styles/variables.scss";`
      }
    }
  }
})
