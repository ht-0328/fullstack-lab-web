import { expect, test } from '@playwright/test'
import { CUSTOMER_URL, STAFF_URL, createTask, login, uniqueTitle } from './helpers'

test.describe('タスク（画面 → BFF → 認可 API / API → DB / WireMock）', () => {
  test('登録したタスクが詳細と一覧に表示され、外部サービスの補足情報が出る', async ({ page }) => {
    await login(page, CUSTOMER_URL, 'alice')
    const title = uniqueTitle('祝日')
    await createTask(page, CUSTOMER_URL, title, '2026-11-03')

    await expect(page.locator('h1')).toHaveText(title)
    await expect(page.getByTestId('due-date-info')).toHaveText('休日（文化の日）')

    await page.getByText('← 一覧へ戻る').click()
    await expect(page.getByRole('link', { name: title })).toBeVisible()
  })

  test('外部サービスが落ちていても詳細は表示され、補足情報だけ縮退する', async ({ page }) => {
    await login(page, CUSTOMER_URL, 'alice')
    const title = uniqueTitle('障害')
    await createTask(page, CUSTOMER_URL, title, '2099-12-31')
    await expect(page.locator('h1')).toHaveText(title)
    await expect(page.getByTestId('due-date-info')).toHaveText('現在取得できません')
  })

  test('他の一般利用者のタスクは一覧に出ず、URL を直接開いても見つからない', async ({ browser }) => {
    const alice = await (await browser.newContext()).newPage()
    await login(alice, CUSTOMER_URL, 'alice')
    const title = uniqueTitle('alice の秘密')
    const id = await createTask(alice, CUSTOMER_URL, title)

    const bob = await (await browser.newContext()).newPage()
    await login(bob, CUSTOMER_URL, 'bob')
    await expect(bob.getByRole('link', { name: title })).toHaveCount(0)
    await bob.goto(`${CUSTOMER_URL}/tasks/${id}`)
    await expect(bob.getByText('タスクが見つかりません')).toBeVisible()
  })

  test('task-viewer の社員は全件を閲覧できるが、登録はできない', async ({ browser }) => {
    const alice = await (await browser.newContext()).newPage()
    await login(alice, CUSTOMER_URL, 'alice')
    const title = uniqueTitle('社員が見る')
    const id = await createTask(alice, CUSTOMER_URL, title)

    const carol = await (await browser.newContext()).newPage()
    await login(carol, STAFF_URL, 'carol')
    await expect(carol.locator('h1')).toHaveText('すべてのタスク')
    await expect(carol.getByText('＋ タスクを登録')).toHaveCount(0)
    await carol.goto(`${STAFF_URL}/tasks/${id}`)
    await expect(carol.locator('h1')).toHaveText(title)

    // 画面にボタンが無くても、URL を直接開いて送信すれば BFF（認可 API）が 403 で止める
    await carol.goto(`${STAFF_URL}/tasks/new`)
    await carol.locator('#title').fill(uniqueTitle('carol'))
    await carol.getByRole('button', { name: '登録' }).click()
    await expect(carol.locator('.error')).toHaveText('この操作は許可されていません')
  })

  test('task-admin の社員は登録できる', async ({ page }) => {
    await login(page, STAFF_URL, 'dave')
    await expect(page.getByText('＋ タスクを登録')).toBeVisible()
    const title = uniqueTitle('dave')
    await createTask(page, STAFF_URL, title)
    await expect(page.locator('h1')).toHaveText(title)
  })
})
