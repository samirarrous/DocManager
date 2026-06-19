<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import Sidebar from './Sidebar.vue'
import TicketList from './TicketList.vue'
import UserList from './UserList.vue'
import DocumentList from './DocumentList.vue'

// API config
const API_BASE = 'http://localhost:8081'

// Types
interface User {
  id: number
  email: string
  token: string
  role?: string
}
// Rest of the interface definitions remain the same
interface Ticket {
  id: number
  zammadId: number
  number: string
  title: string
  description: string
  status: 'OPEN' | 'CLOSED' | string
  user: User
}

interface Toast {
  id: number
  message: string
  type: 'success' | 'error' | 'info'
}

// Props & Emits
const props = defineProps<{
  currentUser: User
}>()

const emit = defineEmits<{
  (e: 'logout'): void
}>()

// State
const users = ref<User[]>([])
const tickets = ref<Ticket[]>([])
const currentTab = ref<'tickets' | 'users' | 'documents'>('tickets')
const loadingUsers = ref(false)
const loadingTickets = ref(false)
const searchQuery = ref('')

// Toast Notification System
const toasts = ref<Toast[]>([])
let toastIdCounter = 0
const showToast = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
  const id = toastIdCounter++
  toasts.value.push({ id, message, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }, 4000)
}

// Modals State
const showUserModal = ref(false)
const submittingUser = ref(false)

// Data Fetching
const fetchUsers = async () => {
  loadingUsers.value = true
  try {
    const res = await fetch(`${API_BASE}/users`, {
      headers: {
        'Authorization': `Bearer ${props.currentUser.token}`
      }
    })
    if (res.status === 401) {
      emit('logout')
      return
    }
    if (!res.ok) throw new Error('Failed to load users')
    users.value = await res.json()
  } catch (err: any) {
    showToast(err.message || 'Error connecting to backend users service', 'error')
  } finally {
    loadingUsers.value = false
  }
}

const fetchTickets = async () => {
  loadingTickets.value = true
  try {
    const res = await fetch(`${API_BASE}/tickets?email=${props.currentUser.email}`, {
      headers: {
        'Authorization': `Bearer ${props.currentUser.token}`
      }
    })
    if (res.status === 401) {
      emit('logout')
      return
    }
    if (!res.ok) throw new Error('Failed to load tickets')
    tickets.value = await res.json()
  } catch (err: any) {
    showToast(err.message || 'Error connecting to backend tickets service', 'error')
  } finally {
    loadingTickets.value = false
  }
}

// Actions
const handleCreateUser = async (newUser: any) => {
  submittingUser.value = true
  try {
    const res = await fetch(`${API_BASE}/users`, {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${props.currentUser.token}`
      },
      body: JSON.stringify(newUser)
    })
    if (res.status === 401) {
      emit('logout')
      return
    }
    if (!res.ok) {
      const errorText = await res.text()
      throw new Error(errorText || 'Failed to create user')
    }
    showToast('User successfully registered!', 'success')
    showUserModal.value = false
    await fetchUsers()
  } catch (err: any) {
    showToast(err.message || 'Error registering user', 'error')
  } finally {
    submittingUser.value = false
  }
}

const initZammadForm = () => {
  nextTick(() => {
    const $ = (window as any).$
    if ($ && $.fn && $.fn.ZammadForm && $('#zammad-feedback-form').length) {
      $('#zammad-feedback-form').ZammadForm({
        messageTitle: 'Formulaire de feedback',
        messageSubmit: 'Soumettre',
        messageThankYou: 'Merci pour votre requête (#%s) ! Nous vous recontacterons dans les meilleurs délais.',
        showTitle: true,
        modal: true,
        attachmentSupport: true
      });
    }
  })
}

// Watch tab changes to initialize the form button
watch(currentTab, (newTab) => {
  if (newTab === 'tickets') {
    initZammadForm()
  }
})

const openUserModal = () => {
  showUserModal.value = true
}

const initZammadChat = () => {
  nextTick(() => {
    const ZammadChatClass = (window as any).ZammadChat
    if (ZammadChatClass) {
      new ZammadChatClass({
        fontSize: '12px',
        chatId: 1,
        show: true,
        debug: true,
        host: 'ws://localhost:8080/ws',
        waitingListTimeout: 60,
        buttonClass: 'open-zammad-chat',
        target: document.body
      })
    }
  })
}

onMounted(async () => {
  const promises: Promise<any>[] = [fetchTickets()]
  if (props.currentUser.role === 'ADMIN') {
    promises.push(fetchUsers())
  }
  await Promise.all(promises)
  initZammadForm()
  initZammadChat()
})
</script>

<template>
  <div class="dashboard-container">
    <!-- Sidebar Navigation -->
    <Sidebar 
      :current-user="currentUser" 
      v-model:currentTab="currentTab" 
      :tickets-count="tickets.length"
      :users-count="users.length"
      @logout="emit('logout')"
    />

    <!-- Main Content Area -->
    <main class="main-content">
      <!-- Search and Header Area -->
      <header class="content-header">
        <div class="header-title">
          <h1>{{ currentTab === 'tickets' ? 'Tickets Directory' : currentTab === 'documents' ? 'Documents Repository' : 'User Profiles' }}</h1>
          <p class="subtitle">
            {{ currentTab === 'tickets' 
              ? 'View all registered tickets linked to Zammad and database.' 
              : currentTab === 'documents'
                ? 'Manage and search through your extracted accounting documents.'
                : 'Manage and register users to grant them ticket access.' 
            }}
          </p>
        </div>

        <div class="header-actions" v-if="currentTab !== 'documents'">
          <div class="search-box">
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input 
              type="text" 
              v-model="searchQuery" 
              :placeholder="currentTab === 'tickets' ? 'Search ticket #, title, user...' : 'Search user email...'" 
            />
          </div>

          <button 
            v-if="currentTab === 'tickets'" 
            class="btn btn-secondary" 
            @click="fetchTickets"
            title="Refresh tickets"
            style="padding: 12px;"
          >
            <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M23 4v6h-6" />
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
            </svg>
          </button>

          <button 
            v-if="currentTab === 'tickets'" 
            id="zammad-feedback-form"
            class="btn btn-primary"
          >
            <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            Report a Problem
          </button>

          <button 
            v-else 
            class="btn btn-primary" 
            @click="openUserModal"
          >
            <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <line x1="19" y1="8" x2="19" y2="14" />
              <line x1="16" y1="11" x2="22" y2="11" />
            </svg>
            New User
          </button>
        </div>
      </header>

      <!-- Tickets Tab View -->
      <TicketList 
        v-if="currentTab === 'tickets'"
        :tickets="tickets"
        :search-query="searchQuery"
        :loading="loadingTickets"
      />

      <!-- Documents Tab View -->
      <DocumentList
        v-else-if="currentTab === 'documents'"
        :current-user="currentUser"
        :API_BASE="API_BASE"
        @show-toast="showToast"
        @logout="emit('logout')"
      />

      <!-- Users Tab View -->
      <UserList 
        v-else
        :users="users"
        :tickets="tickets"
        :search-query="searchQuery"
        :loading="loadingUsers"
        :submitting="submittingUser"
        v-model:showModal="showUserModal"
        @create-user="handleCreateUser"
      />
    </main>

    <!-- Custom Floating Toast Notifications -->
    <TransitionGroup name="toast" tag="div" class="toasts-container">
      <div 
        v-for="toast in toasts" 
        :key="toast.id" 
        class="toast-card" 
        :class="toast.type"
      >
        <span class="toast-icon">
          {{ toast.type === 'success' ? '✓' : toast.type === 'error' ? '⚠' : 'ℹ' }}
        </span>
        <span class="toast-message">{{ toast.message }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>
