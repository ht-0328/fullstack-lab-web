<script setup lang="ts">
const form = reactive({ title: '', description: '', dueDate: '' })
const errorMessage = ref('')
const submitting = ref(false)

// 登録はブラウザから同一オリジンの /bff/tasks へ送る（ブラウザ → Nginx → BFF）
async function submit() {
  submitting.value = true
  errorMessage.value = ''
  try {
    // body の型は契約（CreateTaskInput）から決まるので、項目名の誤りはビルド時に分かる
    const { data, error } = await useBff().POST('/bff/tasks', {
      body: {
        title: form.title,
        description: form.description || null,
        dueDate: form.dueDate || null,
      },
    })
    if (error || !data) {
      // 400 は入力エラー、403 は認可 API で拒否された場合（例: task-viewer ロールの社員）
      errorMessage.value = error?.detail ?? '登録に失敗しました'
      return
    }
    await navigateTo(`/tasks/${data.id}`)
  } catch (e: unknown) {
    errorMessage.value = e instanceof Error ? e.message : '登録に失敗しました'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section>
    <h1>タスク登録</h1>
    <form @submit.prevent="submit">
      <label for="title">タイトル（必須・100文字以内）</label>
      <input id="title" v-model="form.title" required maxlength="100">
      <label for="description">説明</label>
      <textarea id="description" v-model="form.description" rows="4" maxlength="1000" />
      <label for="dueDate">期限日</label>
      <input id="dueDate" v-model="form.dueDate" type="date">
      <p class="muted">2026-11-03 は祝日、2099-12-31 は外部サービス障害のスタブが返ります。</p>
      <button type="submit" :disabled="submitting">登録</button>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </form>
    <p><NuxtLink to="/">← 一覧へ戻る</NuxtLink></p>
  </section>
</template>
