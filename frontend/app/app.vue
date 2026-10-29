<script setup lang="ts">
const { data: me } = await useMe()

// Keycloak が自動で付けるロールは表示しない
const IMPLICIT_ROLES = ['offline_access', 'uma_authorization']
const displayRoles = computed(() =>
  me.value?.roles.filter(r => !IMPLICIT_ROLES.includes(r) && !r.startsWith('default-roles-')) ?? [],
)
</script>

<template>
  <div class="container">
    <header>
      <NuxtLink to="/" class="brand">
        fullstack-lab <span class="badge">{{ me?.userType === 'STAFF' ? '管理画面' : 'マイページ' }}</span>
      </NuxtLink>
      <span class="user">
        <template v-if="me">
          <span data-testid="user-name">{{ me.name }}</span>
          <span v-if="displayRoles.length" class="muted">（{{ displayRoles.join(', ') }}）</span>
          <!-- 一般利用者だけ（社員のアカウントは IdP 側で管理する）。出し分けは BFF の capabilities に従う -->
          <NuxtLink v-if="me.capabilities.canChangePassword" to="/me/password" class="logout">パスワード変更</NuxtLink>
          <!-- oauth2-proxy のセッションを消し、IdP 側のセッションも終了させる（AWS では ALB の Cookie 削除 + IdP のログアウト） -->
          <a href="/oauth2/sign_out?rd=/" class="logout">ログアウト</a>
        </template>
        <template v-else>
          <!-- 公開ページ（会員登録）を未認証で開いているとき -->
          <span data-testid="user-name">未認証</span>
          <a href="/oauth2/start?rd=/" class="logout">ログイン</a>
        </template>
      </span>
    </header>
    <main>
      <NuxtPage />
    </main>
  </div>
</template>

<style>
body { font-family: system-ui, sans-serif; margin: 0; background: #f6f7f9; color: #1f2328; }
.container { max-width: 720px; margin: 0 auto; padding: 0 16px 48px; }
header { display: flex; justify-content: space-between; align-items: center; padding: 16px 0; border-bottom: 1px solid #d0d7de; margin-bottom: 24px; }
.brand { font-weight: bold; text-decoration: none; color: inherit; }
.user { font-size: 0.85rem; color: #57606a; }
.badge { font-size: 0.75rem; font-weight: normal; background: #ddf4ff; color: #0969da; border-radius: 4px; padding: 2px 6px; margin-left: 4px; }
.logout { margin-left: 12px; }
a { color: #0969da; }
table { width: 100%; border-collapse: collapse; background: #fff; }
th, td { text-align: left; padding: 8px; border-bottom: 1px solid #d0d7de; }
label { display: block; margin-top: 12px; font-weight: 600; }
input, textarea { width: 100%; box-sizing: border-box; padding: 6px; font: inherit; }
button { margin-top: 16px; padding: 8px 16px; font: inherit; cursor: pointer; }
.error { color: #cf222e; }
.muted { color: #57606a; }
</style>
