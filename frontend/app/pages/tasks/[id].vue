<script setup lang="ts">
const route = useRoute()
const id = Number(route.params.id)
const bff = useBff()
const { data: task, error } = await useAsyncData(
  `task-${id}`,
  async () => unwrap(await bff.GET('/bff/tasks/{id}', { params: { path: { id } } })),
)
const completeError = ref('')
const completing = ref(false)

// 完了はブラウザから同一オリジンの /bff/tasks/{id}/complete へ送る（ブラウザ → Nginx → BFF → 認可 API → API）。
// 応答は完了後の詳細なので、そのまま画面を置き換える（canComplete が false になりボタンが消える）
async function complete() {
  completing.value = true
  completeError.value = ''
  try {
    const { data, error: problem } = await useBff().POST('/bff/tasks/{id}/complete', { params: { path: { id } } })
    if (problem || !data) {
      // 403 は認可 API で拒否された場合（他人のタスク、task-viewer ロールの社員）
      completeError.value = problem?.detail ?? '完了にできませんでした'
      return
    }
    task.value = data
  } catch (e: unknown) {
    completeError.value = e instanceof Error ? e.message : '完了にできませんでした'
  } finally {
    completing.value = false
  }
}
</script>

<template>
  <section>
    <p v-if="error" class="error">
      {{ error.statusCode === 404 ? 'タスクが見つかりません' : `タスクを取得できませんでした（${error.statusCode}）` }}
    </p>
    <template v-else-if="task">
      <h1>{{ task.title }}</h1>
      <table>
        <tbody>
          <tr><th>ID</th><td>{{ task.id }}</td></tr>
          <tr><th>説明</th><td>{{ task.description ?? '-' }}</td></tr>
          <tr><th>期限日</th><td>{{ task.dueDate ?? '-' }}</td></tr>
          <tr>
            <th>期限日の補足（外部サービス）</th>
            <td data-testid="due-date-info">
              <template v-if="task.dueDateInfoStatus === 'AVAILABLE' && task.dueDateInfo">
                {{ task.dueDateInfo.businessDay ? '営業日' : `休日（${task.dueDateInfo.holidayName ?? '祝日'}）` }}
              </template>
              <span v-else-if="task.dueDateInfoStatus === 'UNAVAILABLE'" class="error">
                現在取得できません
              </span>
              <span v-else class="muted">期限日なし</span>
            </td>
          </tr>
          <tr><th>登録者</th><td>{{ task.createdByName }}</td></tr>
          <tr><th>登録日時</th><td>{{ task.createdAt }}</td></tr>
          <tr>
            <th>状態</th>
            <td data-testid="completion-status">{{ task.completedAt ? `完了（${task.completedAt}）` : '未完了' }}</td>
          </tr>
        </tbody>
      </table>
      <!-- ボタンの有無は BFF の canComplete に従う（画面の出し分け）。守りは BFF と認可 API -->
      <button v-if="task.canComplete" type="button" :disabled="completing" @click="complete">完了にする</button>
      <p v-if="completeError" class="error">{{ completeError }}</p>
    </template>
    <p><NuxtLink to="/">← 一覧へ戻る</NuxtLink></p>
  </section>
</template>
