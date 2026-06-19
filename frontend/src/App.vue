<script setup lang="ts">
import { ref, onMounted } from 'vue'
import LoginView from './components/LoginView.vue'
import Dashboard from './components/Dashboard.vue'

const API_BASE = 'http://localhost:8081'

interface User {
  id: number
  email: string
  token: string
  role?: string
}

const currentUser = ref<User | null>(null)

onMounted(async () => {
  const sessionStr = localStorage.getItem('docmanager_session')
  if (sessionStr) {
    try {
      const session = JSON.parse(sessionStr) as User
      if (session && session.token) {
        // Validate session with backend
        const res = await fetch(`${API_BASE}/auth/me`, {
          headers: {
            'Authorization': `Bearer ${session.token}`
          }
        })
        if (res.ok) {
          const userDetails = await res.json()
          currentUser.value = {
            ...session,
            role: userDetails.role
          }
          localStorage.setItem('docmanager_session', JSON.stringify(currentUser.value))
        } else {
          handleLogout()
        }
      } else {
        handleLogout()
      }
    } catch (e) {
      handleLogout()
    }
  }
})

const handleAuthSuccess = (session: User) => {
  currentUser.value = session
  localStorage.setItem('docmanager_session', JSON.stringify(session))
}

const handleLogout = () => {
  currentUser.value = null
  localStorage.removeItem('docmanager_session')
}
</script>

<template>
  <Transition name="fade" mode="out-in">
    <LoginView v-if="!currentUser" @auth-success="handleAuthSuccess" />
    <Dashboard v-else :current-user="currentUser" @logout="handleLogout" />
  </Transition>
</template>

<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
