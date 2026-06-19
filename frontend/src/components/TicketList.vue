<script setup lang="ts">
import { computed } from 'vue'

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
  tickets: Ticket[]
  searchQuery: string
  loading: boolean
}>()

// Stats computation
const stats = computed(() => {
  const total = props.tickets.length
  const closed = props.tickets.filter(t => t.status === 'CLOSED').length
  const open = total - closed
  return { total, open, closed }
})

// Filtered lists
const filteredTickets = computed(() => {
  if (!props.searchQuery) return props.tickets
  const query = props.searchQuery.toLowerCase()
  return props.tickets.filter(t => 
    t.title.toLowerCase().includes(query) ||
    t.description.toLowerCase().includes(query) ||
    t.number.toLowerCase().includes(query) ||
    t.user.email.toLowerCase().includes(query)
  )
})

// Helper: Get user email initials
const getInitials = (email: string) => {
  if (!email) return 'U'
  const name = email.split('@')[0]
  if (name.length <= 2) return name.toUpperCase()
  return name.slice(0, 2).toUpperCase()
}
</script>

<template>
  <section class="tab-pane">
    <!-- Stats cards -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon-wrapper total">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2" /></svg>
        </div>
        <div class="stat-info">
          <span class="stat-label">Total Tickets</span>
          <span class="stat-value">{{ stats.total }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrapper open">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" /></svg>
        </div>
        <div class="stat-info">
          <span class="stat-label">Open Tickets</span>
          <span class="stat-value">{{ stats.open }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrapper closed">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" /></svg>
        </div>
        <div class="stat-info">
          <span class="stat-label">Closed / Solved</span>
          <span class="stat-value">{{ stats.closed }}</span>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loader-container">
      <div class="glowing-spinner"></div>
      <p>Retrieving tickets...</p>
    </div>

    <div v-else-if="filteredTickets.length === 0" class="empty-state">
      <div class="empty-icon-wrapper">📁</div>
      <h2>No tickets found</h2>
      <p>Try creating a ticket or modifying your search filters.</p>
    </div>

    <div v-else class="tickets-grid">
      <div v-for="ticket in filteredTickets" :key="ticket.id" class="glass-card ticket-card">
        <div class="ticket-header">
          <span class="ticket-number">#{{ ticket.number || ticket.id }}</span>
          <span class="status-badge" :class="ticket.status?.toLowerCase()">
            {{ ticket.status }}
          </span>
        </div>
        
        <h3 class="ticket-title">{{ ticket.title }}</h3>
        <p class="ticket-description">{{ ticket.description }}</p>

        <div class="ticket-footer">
          <div class="user-chip">
            <div class="avatar" :title="ticket.user?.email">
              {{ getInitials(ticket.user?.email) }}
            </div>
            <span class="user-email" :title="ticket.user?.email">{{ ticket.user?.email }}</span>
          </div>
          <div class="zammad-badge" v-if="ticket.zammadId">
            <span>Zammad ID: {{ ticket.zammadId }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
