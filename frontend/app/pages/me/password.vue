<script setup lang="ts">
// パスワード変更（一般利用者のみ）。ブラウザから同一オリジンの /bff/me/password へ送る。
// 対象の利用者は BFF が JWT の sub から決めるので、画面からは ID を送らない
const form = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const errorMessage = ref('')
const submitting = ref(false)
const done = ref(false)

async function submit() {
  errorMessage.value = ''
  if (form.newPassword !== form.confirmPassword) {
    errorMessage.value = '新しいパスワードが一致しません'
    return
  }
  submitting.value = true
  try {
    const { error, response } = await useBff().PUT('/bff/me/password', {
      body: { currentPassword: form.currentPassword, newPassword: form.newPassword },
    })
    if (error || !response.ok) {
      // 403 は認可 API の拒否（社員）または現在のパスワードの不一致。detail は BFF / 認証 API の文言をそのまま出す
      errorMessage.value = error?.detail ?? '変更に失敗しました'
      return
    }
    done.value = true
  } catch (e: unknown) {
    errorMessage.value = e instanceof Error ? e.message : '変更に失敗しました'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section>
    <h1>パスワード変更</h1>
    <p v-if="done" data-testid="password-done">変更しました。次回から新しいパスワードでログインしてください。</p>
    <form v-else @submit.prevent="submit">
      <label for="currentPassword">現在のパスワード</label>
      <input id="currentPassword" v-model="form.currentPassword" type="password" required autocomplete="current-password">
      <label for="newPassword">新しいパスワード（8 文字以上）</label>
      <input id="newPassword" v-model="form.newPassword" type="password" required minlength="8" maxlength="128" autocomplete="new-password">
      <label for="confirmPassword">新しいパスワード（確認）</label>
      <input id="confirmPassword" v-model="form.confirmPassword" type="password" required autocomplete="new-password">
      <button type="submit" :disabled="submitting">変更</button>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </form>
    <p><NuxtLink to="/">← 一覧へ戻る</NuxtLink></p>
  </section>
</template>
