// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2026-10-01',
  devtools: { enabled: false },
  runtimeConfig: {
    // サーバー（SSR）専用。コンテナ間通信で BFF を直接呼ぶ。NUXT_BFF_INTERNAL_BASE で上書き
    bffInternalBase: 'http://localhost:8081',
    public: {
      // ブラウザ用。空文字 = 同一オリジン（Nginx / ALB が /bff/* を BFF へ振り分ける）
      bffPublicBase: '',
    },
  },
})
