import createClient, { type Middleware } from 'openapi-fetch'
import type { components, paths } from '~/types/bff'

// 画面で使う型は BFF との契約（openapi/bff.yaml）から生成したものを使う（npm run generate:bff）
export type Me = components['schemas']['MeView']
export type TaskSummary = components['schemas']['TaskSummaryView']
export type TaskDetail = components['schemas']['TaskDetailView']
export type CreateTaskInput = components['schemas']['CreateTaskInput']
export type SignupInput = components['schemas']['SignupInput']
export type ChangePasswordInput = components['schemas']['ChangePasswordInput']
export type ProblemDetail = components['schemas']['ProblemDetail']

// ブラウザでセッション切れ（401）になったら、ログインからやり直す
const redirectToLoginOn401: Middleware = {
  onResponse({ response }) {
    if (import.meta.client && response.status === 401) {
      window.location.href = `/oauth2/start?rd=${encodeURIComponent(window.location.pathname)}`
    }
    return response
  },
}

/**
 * BFF 呼び出し用の型付きクライアント（openapi-fetch）。パス・パラメーター・レスポンスの型は生成した paths から決まる。
 * HTTP の層は Nuxt / ブラウザ標準の fetch に任せ、生成するのは型だけにしている（生成コードに独自の HTTP 層を持たせない）。
 * - SSR 時（Nuxt サーバー内）: 内部 URL で BFF を直接呼び、元リクエストの認証ヘッダー（JWT）と
 *   リクエスト ID を引き継ぐ（Nginx を経由しないため、自分で転送する必要がある）
 * - ブラウザ時: 同一オリジンの /bff/* を呼ぶ（認証ヘッダーは Nginx / ALB が付与する）
 */
export function useBff() {
  const config = useRuntimeConfig()
  const forwarded = import.meta.server
    ? useRequestHeaders(['x-amzn-oidc-accesstoken', 'x-amzn-oidc-identity', 'x-request-id'])
    : {}
  const client = createClient<paths>({
    baseUrl: import.meta.server ? config.bffInternalBase : config.public.bffPublicBase,
    headers: forwarded,
  })
  client.use(redirectToLoginOn401)
  return client
}

/** openapi-fetch の { data, error } を「成功なら data、失敗なら throw」に変換し、useAsyncData の error に statusCode を載せる */
export function unwrap<T>(result: { data?: T, error?: unknown, response: Response }): T {
  if (result.error !== undefined || !result.response.ok) {
    const problem = result.error as ProblemDetail | undefined
    throw createError({
      statusCode: result.response.status,
      statusMessage: problem?.title ?? result.response.statusText,
      data: problem,
    })
  }
  return result.data as T
}

/**
 * ログイン中の利用者。app.vue と各ページで同じキーを使い、取得は 1 回にまとめる。
 * 公開ページ（会員登録）は未認証のまま描画するので、401 は「ログインしていない」（null）として扱う
 */
export function useMe() {
  const bff = useBff()
  return useAsyncData<Me | null>('me', async () => {
    const result = await bff.GET('/bff/me')
    if (result.response.status === 401) return null
    return unwrap(result)
  })
}
