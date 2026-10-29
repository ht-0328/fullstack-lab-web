import { defineConfig } from '@playwright/test'

// 起動済みの環境（ローカル: Docker Compose）に対して実行する。ブラウザは Playwright の公式イメージに同梱のものを使う
export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  // 同じバックエンド（DB、IdP のセッション）を共有するので直列に実行する
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8080',
    trace: 'retain-on-failure',
  },
})
