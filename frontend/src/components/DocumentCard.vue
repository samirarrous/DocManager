<script setup lang="ts">
import { computed } from 'vue'

interface User {
  id: number
  email: string
  token: string
  role?: string
}

interface DocumentItem {
  id: number
  fileName: string
  type: string
  user: {
    email: string
  }
  extractedJson: string | null
}

const props = defineProps<{
  doc: DocumentItem
  currentUser: User
  isSelected: boolean
}>()

const emit = defineEmits<{
  (e: 'select', doc: DocumentItem): void
  (e: 'download', doc: DocumentItem): void
  (e: 'delete', doc: DocumentItem): void
}>()

const cleanName = computed(() => {
  return props.doc.fileName.includes('_') 
    ? props.doc.fileName.split('_').slice(1).join('_') 
    : props.doc.fileName
})

const ownerDisplay = computed(() => {
  return props.doc.user?.email ? props.doc.user.email.split('@')[0] : ''
})
</script>

<template>
  <div 
    class="glass-card doc-card" 
    :class="{ selected: isSelected }"
    @click="emit('select', doc)"
  >
    <div class="doc-header">
      <div class="doc-title-wrapper">
        <span class="pdf-tag">PDF</span>
        <h3 class="doc-title" :title="doc.fileName">
          {{ cleanName }}
        </h3>
      </div>
      <div class="doc-actions" @click.stop>
        <button 
          class="action-icon-btn download" 
          @click="emit('download', doc)" 
          title="Download original PDF"
        >
          <i class="fa-solid fa-download"></i>
        </button>
        <button 
          class="action-icon-btn delete" 
          @click="emit('delete', doc)" 
          title="Delete metadata & file"
        >
          <i class="fa-solid fa-trash-can"></i>
        </button>
      </div>
    </div>

    <div class="doc-info">
      <span class="status-badge doc-type-badge">{{ doc.type.toLowerCase() }}</span>
      <span 
        class="doc-owner" 
        v-if="currentUser.role === 'ADMIN'" 
        :title="doc.user?.email"
      >
        <i class="fa-solid fa-user"></i> {{ ownerDisplay }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.doc-card {
  padding: 16px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

.doc-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px var(--shadow);
  border-color: var(--primary-border);
}

.doc-card.selected {
  background: var(--bg-active);
  border-color: var(--primary);
}

.doc-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.doc-title-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.pdf-tag {
  background: #ff5b5b;
  color: white;
  font-size: 0.7rem;
  font-weight: bold;
  padding: 2px 4px;
  border-radius: 4px;
}

.doc-title {
  font-size: 1rem;
  font-weight: 600;
  margin: 0;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doc-actions {
  display: flex;
  gap: 8px;
}

.action-icon-btn {
  background: transparent;
  border: none;
  font-size: 1.1rem;
  cursor: pointer;
  padding: 6px;
  border-radius: 4px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-icon-btn.download {
  color: var(--text-muted, #94a3b8);
}

.action-icon-btn.download:hover {
  background: rgba(74, 222, 128, 0.1);
  color: #4ade80; /* Soft green */
}

.action-icon-btn.delete {
  color: var(--text-muted, #94a3b8);
}

.action-icon-btn.delete:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444; /* Soft red */
}

.doc-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.doc-type-badge {
  text-transform: capitalize;
}

.doc-owner {
  font-size: 0.8rem;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}
</style>
