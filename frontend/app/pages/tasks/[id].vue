<script setup lang="ts">
const route = useRoute()
const id = Number(route.params.id)
const bff = useBff()
const { data: task, error } = await useAsyncData(
  `task-${id}`,
  async () => unwrap(await bff.GET('/bff/tasks/{id}', { params: { path: { id } } })),
)
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
        </tbody>
      </table>
    </template>
    <p><NuxtLink to="/">← 一覧へ戻る</NuxtLink></p>
  </section>
</template>
