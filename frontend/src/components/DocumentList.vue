<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

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
  currentUser: User
  API_BASE: string
}>()

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
const searchYear = ref('')
const searchType = ref('')
const searchQuery = ref('')

const fetchTypes = async () => {
  try {
    const res = await fetch(`${props.API_BASE}/documents/types`, {
      headers: {
        'Authorization': `Bearer ${props.currentUser.token}`
      }
    })
    if (res.status === 401) {
      emit('logout')
      return
    }
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

    const url = params.toString() 
      ? `${props.API_BASE}/documents/search?${params.toString()}`
      : `${props.API_BASE}/documents`

    const res = await fetch(url, {
      headers: {
        'Authorization': `Bearer ${props.currentUser.token}`
      }
    })
    if (res.status === 401) {
      emit('logout')
      return
    }
    if (!res.ok) throw new Error('Failed to retrieve documents')
    documents.value = await res.json()
  } catch (err: any) {
    emit('show-toast', err.message || 'Error loading documents', 'error')
  } finally {
    loading.value = false
  }
}

// Watch filters to trigger searches automatically
watch([searchYear, searchType], () => {
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
    const res = await fetch(`${props.API_BASE}/documents/upload`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${props.currentUser.token}`
      },
      body: formData
    })
    if (res.status === 401) {
      emit('logout')
      return
    }
    if (!res.ok) {
      const errMsg = await res.text()
      throw new Error(errMsg || 'Failed to upload document')
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
    const res = await fetch(`${props.API_BASE}/documents/${doc.id}/download`, {
      headers: {
        'Authorization': `Bearer ${props.currentUser.token}`
      }
    })
    if (res.status === 401) {
      emit('logout')
      return
    }
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
    const res = await fetch(`${props.API_BASE}/documents/${doc.id}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${props.currentUser.token}`
      }
    })
    if (res.status === 401) {
      emit('logout')
      return
    }
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

const parseJson = (jsonStr: string | null) => {
  if (!jsonStr) return {}
  try {
    return JSON.parse(jsonStr)
  } catch (e) {
    return { error: 'Invalid JSON data', raw: jsonStr }
  }
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

      <!-- Filters & Toolbar -->
      <div class="glass-card toolbar-section">
        <div class="filters-row">
          <!-- Text Query Search -->
          <div class="search-box inline-search">
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input 
              type="text" 
              v-model="searchQuery" 
              placeholder="Search values inside files..." 
              @input="fetchDocuments"
            />
          </div>

          <!-- Type Select -->
          <div class="select-wrapper">
            <select v-model="searchType">
              <option value="">All Document Types</option>
              <option v-for="t in types" :key="t" :value="t">{{ t }}</option>
            </select>
          </div>

          <!-- Year Select -->
          <div class="select-wrapper">
            <select v-model="searchYear">
              <option value="">All Years</option>
              <option value="2026">2026</option>
              <option value="2025">2025</option>
              <option value="2024">2024</option>
              <option value="2023">2023</option>
              <option value="2022">2022</option>
            </select>
          </div>
        </div>
      </div>

      <!-- Documents list -->
      <div v-if="loading" class="loader-container">
        <div class="glowing-spinner"></div>
        <p>Retrieving documents...</p>
      </div>

      <div v-else-if="documents.length === 0" class="empty-state">
        <div class="empty-icon-wrapper">📁</div>
        <h2>No documents found</h2>
        <p>Upload a new PDF to get started, or clear your filters.</p>
      </div>

      <div v-else class="documents-grid">
        <div 
          v-for="doc in documents" 
          :key="doc.id" 
          class="glass-card doc-card" 
          :class="{ selected: selectedDoc?.id === doc.id }"
          @click="selectDoc(doc)"
        >
          <div class="doc-header">
            <div class="doc-title-wrapper">
              <span class="pdf-tag">PDF</span>
              <h3 class="doc-title" :title="doc.fileName">
                {{ doc.fileName.includes('_') ? doc.fileName.split('_').slice(1).join('_') : doc.fileName }}
              </h3>
            </div>
            <div class="doc-actions" @click.stop>
              <button class="action-icon-btn download" @click="handleDownload(doc)" title="Download original PDF">
                📥
              </button>
              <button class="action-icon-btn delete" @click="handleDelete(doc)" title="Delete metadata & file">
                🗑️
              </button>
            </div>
          </div>

          <div class="doc-info">
            <span class="status-badge doc-type-badge">{{ doc.type.toLowerCase() }}</span>
            <span class="doc-owner" v-if="props.currentUser.role === 'ADMIN'" :title="doc.user?.email">
              👤 {{ doc.user?.email.split('@')[0] }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Details Panel Right -->
    <div class="documents-aside">
      <div class="glass-card details-card">
        <div v-if="!selectedDoc" class="aside-empty">
          <div class="select-preview-icon">👁️‍🗨️</div>
          <h3>Document Preview</h3>
          <p>Select any document in the list to view its parsed financial fields and data.</p>
        </div>

        <div v-else class="aside-content">
          <div class="aside-header">
            <h2>Extraction Result</h2>
            <span class="status-badge">{{ selectedDoc.type }}</span>
          </div>
          
          <div class="aside-meta">
            <div class="meta-item">
              <span class="meta-label">File:</span>
              <span class="meta-val" :title="selectedDoc.fileName">
                {{ selectedDoc.fileName.includes('_') ? selectedDoc.fileName.split('_').slice(1).join('_') : selectedDoc.fileName }}
              </span>
            </div>
            <div class="meta-item" v-if="props.currentUser.role === 'ADMIN'">
              <span class="meta-label">User:</span>
              <span class="meta-val">{{ selectedDoc.user?.email }}</span>
            </div>
          </div>

          <div class="json-viewer">
            <pre><code>{{ JSON.stringify(parseJson(selectedDoc.extractedJson), null, 2) }}</code></pre>
          </div>
        </div>
      </div>
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
  width: 400px;
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

.toolbar-section {
  padding: 16px;
}

.filters-row {
  display: flex;
  gap: 16px;
}

.inline-search {
  flex: 1;
}

.select-wrapper {
  position: relative;
  width: 200px;
}

.select-wrapper select {
  width: 100%;
  padding: 12px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text);
  font-size: 0.9rem;
  outline: none;
  cursor: pointer;
  appearance: none;
}

.select-wrapper::after {
  content: '▼';
  font-size: 0.8rem;
  color: var(--text-muted);
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
}

.documents-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  padding-bottom: 24px;
}

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
  padding: 4px;
  border-radius: 4px;
  transition: background 0.2s;
  display: flex;
  align-items: center;
}

.action-icon-btn:hover {
  background: var(--bg-hover);
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

/* Details Panel styling */
.details-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 24px;
  overflow: hidden;
}

.aside-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  text-align: center;
  color: var(--text-muted);
}

.select-preview-icon {
  font-size: 3rem;
  margin-bottom: 16px;
  opacity: 0.5;
}

.aside-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.aside-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.aside-header h2 {
  margin: 0;
  font-size: 1.3rem;
  color: var(--text);
}

.aside-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  font-size: 0.9rem;
}

.meta-label {
  color: var(--text-muted);
  width: 60px;
  font-weight: 500;
}

.meta-val {
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.json-viewer {
  flex: 1;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 8px;
  padding: 16px;
  overflow-y: auto;
  font-family: monospace;
}

.json-viewer pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  color: #55ee55;
  font-size: 0.85rem;
}
</style>
