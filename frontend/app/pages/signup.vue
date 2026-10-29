<script setup lang="ts">
// 会員登録は未認証で開ける公開ページ（入口の Nginx / ALB が認証なしで転送する）。
// 登録そのものは BFF → 認証 API → Keycloak で行い、登録後はいつものログイン（前段 → Keycloak のログイン画面）へ誘導する
const { data: me } = await useMe()
const form = reactive({ email: '', familyName: '', givenName: '', password: '' })
const errorMessage = ref('')
const submitting = ref(false)
const registered = ref<{ email: string } | null>(null)

async function submit() {
  submitting.value = true
  errorMessage.value = ''
  try {
    // body の型は契約（SignupInput）から決まる
    const { data, error } = await useBff().POST('/bff/signup', { body: { ...form } })
    if (error || !data) {
      // 400 は入力エラー、409 は登録済みのメールアドレス
      errorMessage.value = error?.detail ?? '登録に失敗しました'
      return
    }
    registered.value = data
  } catch (e: unknown) {
    errorMessage.value = e instanceof Error ? e.message : '登録に失敗しました'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section>
    <h1>会員登録</h1>
    <p v-if="me?.userType === 'STAFF'" class="muted">社員のアカウントは IdP 側で管理します。この画面は一般利用者向けです。</p>
    <template v-else-if="registered">
      <p data-testid="signup-done">登録しました（{{ registered.email }}）。登録したメールアドレスとパスワードでログインしてください。</p>
      <p><a href="/oauth2/start?rd=/">ログインする</a></p>
    </template>
    <template v-else>
      <form @submit.prevent="submit">
        <label for="email">メールアドレス（ログイン ID）</label>
        <input id="email" v-model="form.email" type="email" required maxlength="254" autocomplete="username">
        <label for="familyName">姓</label>
        <input id="familyName" v-model="form.familyName" required maxlength="50">
        <label for="givenName">名</label>
        <input id="givenName" v-model="form.givenName" required maxlength="50">
        <label for="password">パスワード（8 文字以上）</label>
        <input id="password" v-model="form.password" type="password" required minlength="8" maxlength="128" autocomplete="new-password">
        <button type="submit" :disabled="submitting">登録</button>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      </form>
      <p class="muted">すでに登録済みの方は <a href="/oauth2/start?rd=/">ログイン</a> へ。</p>
    </template>
  </section>
</template>
