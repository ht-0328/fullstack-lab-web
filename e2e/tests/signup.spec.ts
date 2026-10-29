import { expect, test } from '@playwright/test'
import { CUSTOMER_URL, STAFF_URL, createTask, login, uniqueTitle } from './helpers'

test.describe('会員登録とパスワード変更（画面 → BFF → 認証 API → Keycloak）', () => {
  test('登録した利用者でログインしてタスクを登録し、パスワードを変えて入り直せる', async ({ page }) => {
    const email = `e2e-${Date.now()}@example.com`

    // 公開ページなので、未認証でもログイン画面へは飛ばされない
    await page.goto(`${CUSTOMER_URL}/signup`)
    await expect(page.getByTestId('user-name')).toHaveText('未認証')
    await page.locator('#email').fill(email)
    await page.locator('#familyName').fill('山田')
    await page.locator('#givenName').fill('太郎')
    await page.locator('#password').fill('password-1')
    await page.getByRole('button', { name: '登録' }).click()
    await expect(page.getByTestId('signup-done')).toBeVisible()

    // 登録直後に、いつもの経路（oauth2-proxy → Keycloak）でログインできる
    await page.getByRole('link', { name: 'ログインする' }).click()
    await page.locator('#username').fill(email)
    await page.locator('#password').fill('password-1')
    await page.locator('#kc-login').click()
    await page.waitForURL(`${CUSTOMER_URL}/`)
    await expect(page.getByTestId('user-name')).toHaveText(email)

    const title = uniqueTitle('新規会員')
    await createTask(page, CUSTOMER_URL, title)
    await expect(page.locator('h1')).toHaveText(title)

    // 現在のパスワードが違えば認証 API（Keycloak）が拒否し、合っていれば変わる
    await page.getByRole('link', { name: 'パスワード変更' }).click()
    await page.locator('#currentPassword').fill('not-the-password')
    await page.locator('#newPassword').fill('password-2')
    await page.locator('#confirmPassword').fill('password-2')
    await page.getByRole('button', { name: '変更' }).click()
    await expect(page.locator('.error')).toHaveText('現在のパスワードが違います')

    await page.locator('#currentPassword').fill('password-1')
    await page.getByRole('button', { name: '変更' }).click()
    await expect(page.getByTestId('password-done')).toBeVisible()

    await page.getByText('ログアウト').click()
    await expect(page.locator('#kc-form-login')).toBeVisible()
    await login(page, CUSTOMER_URL, email, 'password-2')
  })

  test('登録済みのメールアドレスでは登録できない', async ({ page }) => {
    await page.goto(`${CUSTOMER_URL}/signup`)
    await page.locator('#email').fill('alice@example.com')
    await page.locator('#familyName').fill('山田')
    await page.locator('#givenName').fill('太郎')
    await page.locator('#password').fill('password-1')
    await page.getByRole('button', { name: '登録' }).click()
    await expect(page.locator('.error')).toHaveText('このメールアドレスは登録済みです')
  })

  test('管理画面には会員登録の公開パスが無く、社員にはパスワード変更の導線が出ない', async ({ page }) => {
    await page.goto(`${STAFF_URL}/signup`)
    await expect(page.locator('#kc-form-login')).toBeVisible()
    expect(page.url()).toContain('/realms/staff/')

    await login(page, STAFF_URL, 'carol')
    await expect(page.getByRole('link', { name: 'パスワード変更' })).toHaveCount(0)
  })
})
