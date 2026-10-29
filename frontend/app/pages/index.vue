<script setup lang="ts">
// 初回表示は SSR で取得する（Nuxt サーバー → BFF）。
// 一般利用者には自分のタスクだけ、社員には全件が返る（判定は BFF → 認可 API）
const bff = useBff()
const { data: me } = await useMe()
const { data: tasks, error } = await useAsyncData('tasks', async () => unwrap(await bff.GET('/bff/tasks')))
const isStaff = computed(() => me.value?.userType === 'STAFF')
</script>

<template>
  <section>
    <h1>{{ isStaff ? 'すべてのタスク' : 'あなたのタスク' }}</h1>
    <p v-if="me?.capabilities.canCreateTask"><NuxtLink to="/tasks/new">＋ タスクを登録</NuxtLink></p>
    <p v-if="error" class="error">一覧を取得できませんでした（{{ error.statusCode }}）</p>
    <p v-else-if="!tasks?.length" class="muted">タスクはまだありません。</p>
    <table v-else>
      <thead>
        <tr><th>ID</th><th>タイトル</th><th>期限</th><th v-if="isStaff">登録者</th></tr>
      </thead>
      <tbody>
        <tr v-for="task in tasks" :key="task.id">
          <td>{{ task.id }}</td>
          <td><NuxtLink :to="`/tasks/${task.id}`">{{ task.title }}</NuxtLink></td>
          <td>{{ task.dueDate ?? '-' }}</td>
          <td v-if="isStaff">{{ task.createdByName }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
