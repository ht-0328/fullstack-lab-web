// コンテナのヘルスチェック用。BFF などの依存先には触れず、Nuxt サーバー自身の生存だけを返す
export default defineEventHandler(() => 'ok')
