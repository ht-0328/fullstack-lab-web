import { expect, test } from '@playwright/test'
import { CUSTOMER_URL, STAFF_URL, login } from './helpers'

test.describe('認証（Nginx → oauth2-proxy → Keycloak）', () => {
  test('未認証で画面を開くと IdP のログイン画面へ移る', async ({ page }) => {
    await page.goto('/')
    await expect(page.locator('#kc-form-login')).toBeVisible()
    expect(page.url()).toContain('/realms/customer/')
  })

  test('ログインすると利用者名が表示され、ログアウトで IdP のセッションも終了する', async ({ page }) => {
    await login(page, CUSTOMER_URL, 'alice')
    await expect(page.locator('.badge')).toHaveText('マイページ')

    await page.getByText('ログアウト').click()
    // IdP のセッションも終了しているので、再び自動ログインされずフォームが出る
    await expect(page.locator('#kc-form-login')).toBeVisible()
  })

  test('一般利用者のセッションでは管理画面に入れない', async ({ page }) => {
    await login(page, CUSTOMER_URL, 'alice')
    await page.goto(`${STAFF_URL}/`)
    await expect(page.locator('#kc-form-login')).toBeVisible()
    expect(page.url()).toContain('/realms/staff/')
  })
})
