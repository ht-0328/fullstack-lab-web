import { expect, type Page } from '@playwright/test'

export const CUSTOMER_URL = process.env.E2E_BASE_URL ?? 'http://localhost:8080'
export const STAFF_URL = process.env.E2E_STAFF_BASE_URL ?? 'http://admin.localhost:8080'
const PASSWORD = process.env.E2E_USER_PASSWORD ?? 'password'

/** トップを開き、Keycloak のログインフォームを通って戻ってくる（oauth2-proxy → IdP → コールバック） */
export async function login(page: Page, baseUrl: string, user: string, password = PASSWORD) {
  await page.goto(`${baseUrl}/`)
  await page.locator('#username').fill(user)
  await page.locator('#password').fill(password)
  await page.locator('#kc-login').click()
  await page.waitForURL(`${baseUrl}/`)
  await expect(page.getByTestId('user-name')).toHaveText(user)
}

/** 登録画面からタスクを登録し、遷移先の詳細画面の ID を返す */
export async function createTask(page: Page, baseUrl: string, title: string, dueDate?: string) {
  await page.goto(`${baseUrl}/tasks/new`)
  await page.locator('#title').fill(title)
  if (dueDate) await page.locator('#dueDate').fill(dueDate)
  await page.getByRole('button', { name: '登録' }).click()
  await page.waitForURL(/\/tasks\/\d+$/)
  return Number(page.url().split('/').pop())
}

export const uniqueTitle = (label: string) => `e2e ${label} ${Date.now()}`
