<script setup lang="ts">
interface User {
  id: number
  email: string
  token: string
  role?: string
}

// Props
const props = defineProps<{
  currentUser: User
  currentTab: 'tickets' | 'users' | 'documents'
  ticketsCount: number
  usersCount: number
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:currentTab', tab: 'tickets' | 'users' | 'documents'): void
  (e: 'logout'): void
}>()

// Helper: Get user email initials
const getInitials = (email: string) => {
  if (!email) return 'U'
  const name = email.split('@')[0]
  if (name.length <= 2) return name.toUpperCase()
  return name.slice(0, 2).toUpperCase()
}
</script>

<template>
  <aside class="sidebar">
    <div class="logo-area">
      <div class="logo-glow"></div>
      <svg class="nav-icon logo-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" />
        <path d="M12 16V12" stroke-linecap="round" stroke-linejoin="round" />
        <path d="M12 8H12.01" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
      <span>DocManager</span>
    </div>

    <nav class="nav-links">
      <button 
        class="nav-link" 
        :class="{ active: currentTab === 'tickets' }" 
        @click="emit('update:currentTab', 'tickets')"
      >
        <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
          <line x1="8" y1="21" x2="16" y2="21" />
          <line x1="12" y1="17" x2="12" y2="21" />
        </svg>
        <span>Tickets</span>
        <span class="badge" v-if="ticketsCount">{{ ticketsCount }}</span>
      </button>

      <!-- Documents Tab -->
      <button 
        class="nav-link" 
        :class="{ active: currentTab === 'documents' }" 
        @click="emit('update:currentTab', 'documents')"
      >
        <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <polyline points="14 2 14 8 20 8" />
          <line x1="16" y1="13" x2="8" y2="13" />
          <line x1="16" y1="17" x2="8" y2="17" />
          <polyline points="10 9 9 9 8 9" />
        </svg>
        <span>Documents</span>
      </button>

      <button 
        v-if="props.currentUser.role === 'ADMIN'"
        class="nav-link" 
        :class="{ active: currentTab === 'users' }" 
        @click="emit('update:currentTab', 'users')"
      >
        <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
          <path d="M16 3.13a4 4 0 0 1 0 7.75" />
        </svg>
        <span>Users</span>
        <span class="badge" v-if="usersCount">{{ usersCount }}</span>
      </button>

      <button 
        class="nav-link open-zammad-chat" 
      >
        <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
        </svg>
        <span>Live Chat</span>
      </button>
    </nav>

    <div class="sidebar-footer auth-footer">
      <div class="user-profile-section">
        <div class="avatar small-avatar" :title="props.currentUser.email">
          {{ getInitials(props.currentUser.email) }}
        </div>
        <div class="user-details">
          <span class="user-email-label" :title="props.currentUser.email">{{ props.currentUser.email }}</span>
          <span class="pulse-status"><span class="pulse-indicator-small"></span>Online</span>
        </div>
      </div>
      <button class="logout-btn" @click="emit('logout')" title="Log Out">
        <svg class="logout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
          <polyline points="16 17 21 12 16 7" />
          <line x1="21" y1="12" x2="9" y2="12" />
        </svg>
      </button>
    </div>
  </aside>
</template>
