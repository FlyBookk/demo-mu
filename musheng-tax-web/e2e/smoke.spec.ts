import { test, expect } from '@playwright/test'

/**
 * 冒烟用例：启动后访问前端首页/登录页，确认页面可打开
 * 扩展：可在此目录增加更多 e2e 用例（列表、导入、筛选等）
 */
test.describe('慕声税务系统 - 前端冒烟', () => {
  test('访问根路径应跳转或展示登录/首页', async ({ page }) => {
    await page.goto('/')
    // 根路径可能是登录页或重定向到 dashboard，至少不应 5xx 或空白
    await expect(page).toHaveURL(/\//)
    // 页面应有主要内容（登录表单或主导航等）
    const body = page.locator('body')
    await expect(body).toBeVisible()
  })

  test('登录页可打开', async ({ page }) => {
    await page.goto('/login')
    await expect(page).toHaveURL(/\/login/)
    await expect(page.locator('body')).toBeVisible()
  })
})
