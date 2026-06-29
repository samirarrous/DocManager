<script setup lang="ts">
import { ref } from 'vue'
import { apiRequest } from '../utils/api'

// Props & Emits
const emit = defineEmits<{
  (e: 'auth-success', session: { id: number; email: string; token: string; role?: string }): void
}>()

// State
const mode = ref<'login' | 'register'>('login')
const email = ref('')
const password = ref('')
const loading = ref(false)

// Custom Toast State
interface Toast {
  id: number
  message: string
  type: 'success' | 'error'
}
const toasts = ref<Toast[]>([])
let toastIdCounter = 0
const showToast = (message: string, type: 'success' | 'error' = 'success') => {
  const id = toastIdCounter++
  toasts.value.push({ id, message, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }, 4000)
}

const handleSubmit = async () => {
  if (!email.value || !password.value) {
    showToast('Please fill out all fields', 'error')
    return
  }

  loading.value = true
  const endpoint = mode.value === 'login' ? '/auth/login' : '/auth/register'

  try {
    const res = await apiRequest(endpoint, {
      method: 'POST',
      body: JSON.stringify({
        email: email.value,
        password: password.value
      })
    })

    if (!res.ok) {
      const errorMsg = await res.text()
      throw new Error(errorMsg || `Authentication failed: Status ${res.status}`)
    }

    const userData = await res.json()

    if (mode.value === 'login') {
      showToast('Login successful!', 'success')
      // Emit success to parent to set the session
      setTimeout(() => {
        emit('auth-success', {
          id: userData.user.id,
          email: userData.user.email,
          token: userData.token,
          role: userData.user.role
        })
      }, 500)
    } else {
      showToast('Registration successful! You can now log in.', 'success')
      mode.value = 'login'
      password.value = ''
    }
  } catch (err: any) {
    showToast(err.message || 'Connection to authentication service failed', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-overlay">
    <div class="auth-card glass-card">
      <div class="auth-header">
        <div class="logo-glow"></div>
        <svg class="logo-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" />
          <path d="M12 16V12" stroke-linecap="round" stroke-linejoin="round" />
          <path d="M12 8H12.01" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <h1>DocManager</h1>
        <p class="subtitle">Secure support ticket portal</p>
      </div>

      <div class="auth-tabs">
        <button 
          class="auth-tab" 
          :class="{ active: mode === 'login' }" 
          @click="mode = 'login'; password = ''"
        >
          Login
        </button>
        <button 
          class="auth-tab" 
          :class="{ active: mode === 'register' }" 
          @click="mode = 'register'; password = ''"
        >
          Register
        </button>
      </div>

      <form @submit.prevent="handleSubmit" class="auth-form">
        <div class="form-group">
          <label for="auth-email">Email Address</label>
          <input 
            id="auth-email" 
            type="email" 
            v-model="email" 
            placeholder="name@company.com" 
            required 
            :disabled="loading"
          />
        </div>

        <div class="form-group">
          <label for="auth-password">Password</label>
          <input 
            id="auth-password" 
            type="password" 
            v-model="password" 
            placeholder="••••••••" 
            required 
            :disabled="loading"
          />
        </div>

        <button type="submit" class="btn btn-primary btn-block" :disabled="loading">
          <span v-if="loading" class="btn-spinner"></span>
          {{ loading ? 'Processing...' : (mode === 'login' ? 'Log In' : 'Register Account') }}
        </button>
      </form>
    </div>

    <!-- Floating notifications inside login -->
    <TransitionGroup name="toast" tag="div" class="toasts-container">
      <div 
        v-for="toast in toasts" 
        :key="toast.id" 
        class="toast-card" 
        :class="toast.type"
      >
        <span class="toast-icon">
          {{ toast.type === 'success' ? '✓' : '⚠' }}
        </span>
        <span class="toast-message">{{ toast.message }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>
