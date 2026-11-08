<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router';
</script>

<template>
  <header class="app-nav">
    <span class="app-nav__brand">workbench-perf-a9</span>
    <nav class="app-nav__links">
      <RouterLink to="/">首页</RouterLink>
      <RouterLink to="/employees">员工管理</RouterLink>
    </nav>
  </header>
  <RouterView />
</template>

<style scoped>
.app-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 24px;
  padding: 14px 24px;
  background: var(--app-surface);
  border-bottom: 1px solid var(--app-border);
}

.app-nav__brand {
  font-size: 15px;
  font-weight: 600;
}

.app-nav__links {
  display: flex;
  gap: 16px;
}

.app-nav__links a {
  color: var(--app-muted);
  font-size: 14px;
  text-decoration: none;
}

.app-nav__links a:hover {
  color: var(--app-primary);
}

.app-nav__links a.router-link-active {
  color: var(--app-primary);
  font-weight: 600;
}
</style>
