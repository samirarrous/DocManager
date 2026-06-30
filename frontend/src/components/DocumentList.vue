<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { apiRequest, getErrorMessage } from '../utils/api'
import DocumentFilters from './DocumentFilters.vue'
import DocumentCard from './DocumentCard.vue'
import DocumentDetails from './DocumentDetails.vue'

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

const props = withDefaults(defineProps<{
  currentUser: User
  API_BASE: string
  usersList?: { id: number; email: string; role?: string }[]
}>(), {
  usersList: () => []
})

const emit = defineEmits<{
  (e: 'show-toast', message: string, type: 'success' | 'error' | 'info'): void
  (e: 'logout'): void
}>()

// State
const documents = ref<DocumentItem[]>([])
const types = ref<string[]>([])
const loading = ref(false)
const uploading = ref(false)
const selectedDoc = ref<DocumentItem | null>(null)

// Search & Filter State
const searchQuery = ref('')
const searchType = ref('')
const searchYear = ref('')
const searchUser = ref('')

const fetchTypes = async () => {
  try {
    const res = await apiRequest('/documents/types')
    if (res.ok) {
      types.value = await res.json()
    }
  } catch (err) {
    console.error('Error fetching types', err)
  }
}

const fetchDocuments = async () => {
  loading.value = true
  try {
    // Build query params
    const params = new URLSearchParams()
    if (searchYear.value) params.append('year', searchYear.value)
    if (searchType.value) params.append('type', searchType.value)
    if (searchQuery.value) params.append('query', searchQuery.value)
    if (searchUser.value) params.append('targetUser', searchUser.value)

    const endpoint = params.toString() 
      ? `/documents/search?${params.toString()}`
      : `/documents`

    const res = await apiRequest(endpoint)
    if (!res.ok) throw new Error('Failed to retrieve documents')
    documents.value = await res.json()
  } catch (err: any) {
    emit('show-toast', err.message || 'Error loading documents', 'error')
  } finally {
    loading.value = false
  }
}

// Watch filters to trigger searches automatically
watch([searchQuery, searchYear, searchType, searchUser], () => {
  fetchDocuments()
})

const handleFileUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (!file.name.endsWith('.pdf')) {
    emit('show-toast', 'Only PDF files are supported', 'error')
    return
  }

  uploading.value = true
  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await apiRequest('/documents/upload', {
      method: 'POST',
      body: formData
    })
    if (!res.ok) {
      const errMsg = await getErrorMessage(res)
      throw new Error(errMsg)
    }
    emit('show-toast', 'Document uploaded and analyzed successfully!', 'success')
    // Reset file input
    target.value = ''
    // Refresh documents and types lists
    await Promise.all([fetchDocuments(), fetchTypes()])
  } catch (err: any) {
    emit('show-toast', err.message || 'Error uploading document', 'error')
  } finally {
    uploading.value = false
  }
}

const handleDownload = async (doc: DocumentItem) => {
  try {
    const res = await apiRequest(`/documents/${doc.id}/download`)
    if (!res.ok) throw new Error('Download failed')
    const blob = await res.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    // Remove UUID prefix if present
    const cleanName = doc.fileName.includes('_') 
      ? doc.fileName.split('_').slice(1).join('_') 
      : doc.fileName
    a.download = String(cleanName)
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch (err: any) {
    emit('show-toast', err.message || 'Error downloading file', 'error')
  }
}

const handleDelete = async (doc: DocumentItem) => {
  if (!confirm(`Are you sure you want to delete "${doc.fileName}"?`)) return

  try {
    const res = await apiRequest(`/documents/${doc.id}`, {
      method: 'DELETE'
    })
    if (!res.ok) throw new Error('Failed to delete document')
    emit('show-toast', 'Document deleted successfully', 'success')
    if (selectedDoc.value?.id === doc.id) {
      selectedDoc.value = null
    }
    await Promise.all([fetchDocuments(), fetchTypes()])
  } catch (err: any) {
    emit('show-toast', err.message || 'Error deleting document', 'error')
  }
}

const selectDoc = (doc: DocumentItem) => {
  selectedDoc.value = doc
}

onMounted(() => {
  fetchDocuments()
  fetchTypes()
})
</script>

<template>
  <div class="documents-layout">
    <!-- Main Left Panel -->
    <div class="documents-main">
      <!-- Upload Card -->
      <div class="glass-card upload-section">
        <div class="upload-area">
          <svg class="upload-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
          <div class="upload-text">
            <h3>Analyze New Document</h3>
            <p>Upload a PDF financial statement or invoice</p>
          </div>
          <label class="btn btn-primary" :class="{ disabled: uploading }">
            <span v-if="uploading" class="btn-spinner"></span>
            {{ uploading ? 'Analyzing PDF...' : 'Select File' }}
            <input 
              type="file" 
              accept=".pdf" 
              @change="handleFileUpload" 
              :disabled="uploading" 
              style="display: none;" 
            />
          </label>
        </div>
      </div>

      <!-- Filters & Toolbar Component -->
      <DocumentFilters 
        v-model:searchQuery="searchQuery"
        v-model:searchType="searchType"
        v-model:searchYear="searchYear"
        v-model:searchUser="searchUser"
        :currentUser="currentUser"
        :types="types"
        :usersList="usersList"
      />

      <!-- Documents list -->
      <div v-if="loading" class="loader-container">
        <div class="glowing-spinner"></div>
        <p>Retrieving documents...</p>
      </div>

      <div v-else-if="documents.length === 0" class="empty-state">
        <div class="empty-icon-wrapper">
          <i class="fa-solid fa-folder-open"></i>
        </div>
        <h2>No documents found</h2>
        <p>Upload a new PDF to get started, or clear your filters.</p>
      </div>

      <div v-else class="documents-grid">
        <DocumentCard 
          v-for="doc in documents" 
          :key="doc.id"
          :doc="doc"
          :currentUser="currentUser"
          :isSelected="selectedDoc?.id === doc.id"
          @select="selectDoc"
          @download="handleDownload"
          @delete="handleDelete"
        />
      </div>
    </div>

    <!-- Details Panel Right Component -->
    <div class="documents-aside">
      <DocumentDetails 
        :doc="selectedDoc"
        :currentUser="currentUser"
        @show-toast="(msg, type) => emit('show-toast', msg, type)"
      />
    </div>
  </div>
</template>

<style scoped>
.documents-layout {
  display: flex;
  gap: 24px;
  height: calc(100vh - 160px);
}

.documents-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
  padding-right: 4px;
}

.documents-aside {
  width: 40%;
  min-width: 380px;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.upload-section {
  padding: 24px;
}

.upload-area {
  display: flex;
  align-items: center;
  gap: 24px;
  justify-content: space-between;
}

.upload-icon {
  width: 40px;
  height: 40px;
  color: var(--primary);
  flex-shrink: 0;
}

.upload-text {
  flex: 1;
}

.upload-text h3 {
  margin: 0 0 4px 0;
  font-size: 1.1rem;
  color: var(--text);
}

.upload-text p {
  margin: 0;
  font-size: 0.9rem;
  color: var(--text-muted);
}
</style>
