<script setup lang="ts">
import { ref, computed } from 'vue'

interface User {
  id: number
  email: string
  token: string
}

interface Ticket {
  id: number
  zammadId: number
  number: string
  title: string
  description: string
  status: 'OPEN' | 'CLOSED' | string
  user: User
}

// Props
const props = defineProps<{
  users: User[]
  tickets: Ticket[]
  searchQuery: string
  loading: boolean
  submitting: boolean
  showModal: boolean
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:showModal', value: boolean): void
  (e: 'create-user', newUser: { email: ''; password: '' }): void
}>()

// Form Fields
const newUser = ref({
  email: '',
  password: ''
})

const handleCreateUser = () => {
  if (!newUser.value.email || !newUser.value.password) {
    return
  }
  emit('create-user', { ...newUser.value } as any)
  // Reset form
  newUser.value = { email: '', password: '' }
}

// Filtered list
const filteredUsers = computed(() => {
  if (!props.searchQuery) return props.users
  const query = props.searchQuery.toLowerCase()
  return props.users.filter(u => u.email.toLowerCase().includes(query))
})

// Helper: Get user email initials
const getInitials = (email: string) => {
  if (!email) return 'U'
  const name = email.split('@')[0]
  if (name.length <= 2) return name.toUpperCase()
  return name.slice(0, 2).toUpperCase()
}

// Helper: Count tickets for a user client-side
const getUserTicketCount = (userId: number) => {
  return props.tickets.filter(t => t.user && t.user.id === userId).length
}
</script>

<template>
  <section class="tab-pane">
    <div v-if="loading" class="loader-container">
      <div class="glowing-spinner"></div>
      <p>Fetching user directory...</p>
    </div>

    <div v-else-if="filteredUsers.length === 0" class="empty-state">
      <div class="empty-icon-wrapper">
        <i class="fa-solid fa-users"></i>
      </div>
      <h2>No users registered</h2>
      <p>Register a user to link them with tickets.</p>
    </div>

    <div v-else class="users-grid">
      <div v-for="user in filteredUsers" :key="user.id" class="glass-card user-card">
        <div class="user-card-header">
          <div class="user-large-avatar">
            {{ getInitials(user.email) }}
          </div>
          <div class="user-info">
            <h3>{{ user.email.split('@')[0] }}</h3>
            <span class="user-db-id">User ID: {{ user.id }}</span>
          </div>
        </div>

        <div class="user-card-body">
          <div class="user-detail-row">
            <span class="detail-label">Email:</span>
            <span class="detail-value">{{ user.email }}</span>
          </div>
          <div class="user-detail-row">
            <span class="detail-label">Linked Tickets:</span>
            <span class="detail-value ticket-count-badge">
              {{ getUserTicketCount(user.id) }} active tickets
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- User Creation Modal -->
    <div class="modal-overlay" v-if="showModal" @click.self="emit('update:showModal', false)">
      <div class="modal-content glass-card">
        <div class="modal-header">
          <h2>Register New User</h2>
          <button class="close-btn" @click="emit('update:showModal', false)">&times;</button>
        </div>

        <form @submit.prevent="handleCreateUser" class="modal-form">
          <div class="form-group">
            <label for="user-email">Email Address</label>
            <input 
              id="user-email" 
              type="email" 
              v-model="newUser.email" 
              placeholder="e.g. employee@company.com" 
              required 
            />
          </div>

          <div class="form-group">
            <label for="user-password">Account Password</label>
            <input 
              id="user-password" 
              type="password" 
              v-model="newUser.password" 
              placeholder="••••••••" 
              required 
            />
          </div>

          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" @click="emit('update:showModal', false)">Cancel</button>
            <button type="submit" class="btn btn-primary" :disabled="submitting">
              <span v-if="submitting" class="btn-spinner"></span>
              {{ submitting ? 'Registering...' : 'Register User' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>
