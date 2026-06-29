<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import LoginView from './components/LoginView.vue'
import Dashboard from './components/Dashboard.vue'
import { apiRequest } from './utils/api'

interface User {
  id: number
  email: string
  token: string
  role?: string
}

const currentUser = ref<User | null>(null)

onMounted(async () => {
  window.addEventListener('unauthorized-logout', handleLogout)
  
  const sessionStr = localStorage.getItem('docmanager_session')
  if (sessionStr) {
    try {
      const session = JSON.parse(sessionStr) as User
      if (session && session.token) {
        // Validate session with backend
        const res = await apiRequest('/auth/me')
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

onUnmounted(() => {
  window.removeEventListener('unauthorized-logout', handleLogout)
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
