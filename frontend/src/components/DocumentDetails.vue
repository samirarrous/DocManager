<script setup lang="ts">
import { ref, computed, watch } from 'vue'

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
  doc: DocumentItem | null
  currentUser: User
}>()

const emit = defineEmits<{
  (e: 'show-toast', message: string, type: 'success' | 'error' | 'info'): void
}>()

const showRawJson = ref(false)

// Reset raw JSON collapse state on doc change
watch(() => props.doc, () => {
  showRawJson.value = false
})

const parsedMetadata = computed(() => {
  if (!props.doc || !props.doc.extractedJson) return null
  try {
    return JSON.parse(props.doc.extractedJson)
  } catch (e) {
    return null
  }
})

const extractedFields = computed(() => {
  const meta = parsedMetadata.value
  if (!meta) return {}
  return meta.extracted_data || meta
})

const flattenObject = (obj: any, prefix = ''): Record<string, any> => {
  let results: Record<string, any> = {}
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      const value = obj[key]
      const newKey = prefix ? `${prefix}_${key}` : key
      if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
        Object.assign(results, flattenObject(value, newKey))
      } else {
        results[newKey] = value
      }
    }
  }
  return results
}

const flattenedFields = computed(() => {
  return flattenObject(extractedFields.value)
})

const cleanFileName = computed(() => {
  if (!props.doc) return ''
  return props.doc.fileName.includes('_') 
    ? props.doc.fileName.split('_').slice(1).join('_') 
    : props.doc.fileName
})

const formatKey = (key: string | number): string => {
  return String(key)
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

const formatValue = (val: any): string => {
  if (val === null || val === undefined) return '-'
  if (Array.isArray(val)) {
    return val.map(item => typeof item === 'object' ? formatValue(item) : String(item)).join(', ')
  }
  if (typeof val === 'object') {
    const keys = Object.keys(val)
    if (keys.length === 0) return '{}'
    
    // Check if it's a common pair like percentage & amount
    if ('percentage' in val && 'amount' in val) {
      return `${val.percentage} (${val.amount})`
    }
    
    // Otherwise, join as "Key: Value" pairs
    return keys
      .map(k => {
        const formattedKey = k
          .split('_')
          .map(w => w.charAt(0).toUpperCase() + w.slice(1))
          .join(' ')
        return `${formattedKey}: ${typeof val[k] === 'object' ? formatValue(val[k]) : val[k]}`
      })
      .join(', ')
  }
  return val.toString()
}

const parseJson = (jsonStr: string | null) => {
  if (!jsonStr) return {}
  try {
    return JSON.parse(jsonStr)
  } catch (e) {
    return { error: 'Invalid JSON data', raw: jsonStr }
  }
}

const downloadJson = (doc: DocumentItem) => {
  if (!doc.extractedJson) return
  try {
    const data = parseJson(doc.extractedJson)
    const jsonStr = JSON.stringify(data, null, 2)
    const blob = new Blob([jsonStr], { type: 'application/json' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    
    const baseName = doc.fileName.includes('_') 
      ? doc.fileName.split('_').slice(1).join('_') 
      : doc.fileName
    const jsonName = baseName.replace(/\.[^/.]+$/, "") + "_analysis.json"
    
    a.download = jsonName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    emit('show-toast', 'JSON analysis downloaded successfully!', 'success')
  } catch (err: any) {
    emit('show-toast', 'Error downloading JSON: ' + err.message, 'error')
  }
}
</script>

<template>
  <div class="glass-card details-card">
    <div v-if="!doc" class="aside-empty">
      <div class="select-preview-icon">
        <i class="fa-solid fa-eye"></i>
      </div>
      <h3>Document Preview</h3>
      <p>Select any document in the list to view its parsed financial fields and data.</p>
    </div>

    <div v-else class="aside-content">
      <div class="aside-header">
        <h2>Extraction Result</h2>
        <span class="status-badge" :class="doc.type.toLowerCase()">
          {{ doc.type }}
        </span>
      </div>
      
      <div class="aside-meta">
        <div class="meta-item">
          <span class="meta-label">File:</span>
          <span class="meta-val" :title="doc.fileName">
            {{ cleanFileName }}
          </span>
        </div>
        <div class="meta-item" v-if="currentUser.role === 'ADMIN'">
          <span class="meta-label">User:</span>
          <span class="meta-val">{{ doc.user?.email }}</span>
        </div>
      </div>

      <!-- Structured Key-Value Fields Display -->
      <div class="structured-data" v-if="flattenedFields && Object.keys(flattenedFields).length > 0">
        <!-- Facture Specific Layout -->
        <div v-if="doc.type.toLowerCase() === 'facture'" class="fields-section">
          <div class="field-card main-amount" v-if="extractedFields.total_ttc">
            <span class="field-label">Total TTC</span>
            <span class="field-value highlight">{{ formatValue(extractedFields.total_ttc) }}</span>
          </div>
          <div class="fields-grid">
            <div class="field-item" v-if="extractedFields.company_name">
              <span class="field-label">Company / Vendor</span>
              <span class="field-value" :title="formatValue(extractedFields.company_name)">{{ formatValue(extractedFields.company_name) }}</span>
            </div>
            <div class="field-item" v-if="extractedFields.facture_number">
              <span class="field-label">Invoice Number</span>
              <span class="field-value">{{ formatValue(extractedFields.facture_number) }}</span>
            </div>
            <div class="field-item" v-if="extractedFields.limit_date">
              <span class="field-label">Due Date</span>
              <span class="field-value">{{ formatValue(extractedFields.limit_date) }}</span>
            </div>
            <div class="field-item" v-if="extractedFields.total_ht">
              <span class="field-label">Total HT</span>
              <span class="field-value">{{ formatValue(extractedFields.total_ht) }}</span>
            </div>
            <div class="field-item" v-if="extractedFields.tva">
              <span class="field-label">TVA (VAT)</span>
              <span class="field-value">{{ formatValue(extractedFields.tva) }}</span>
            </div>
            <div class="field-item" v-if="extractedFields.SIRET">
              <span class="field-label">SIRET</span>
              <span class="field-value">{{ formatValue(extractedFields.SIRET) }}</span>
            </div>
          </div>
        </div>

        <!-- Generic Document Layout (Bilan, Compte Resultat, etc.) -->
        <div v-else class="fields-section">
          <div class="generic-fields-list">
            <div 
              v-for="(val, key) in flattenedFields" 
              :key="key" 
              class="generic-field-row"
              v-show="key !== 'document_type'"
            >
              <span class="generic-key">{{ formatKey(key) }}</span>
              <span class="generic-val">{{ formatValue(val) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="no-fields-extracted">
        <i class="fa-solid fa-triangle-exclamation"></i>
        <p>No fields could be extracted.</p>
      </div>

      <!-- Expandable Raw JSON Viewer -->
      <div class="raw-json-section">
        <div class="raw-json-header">
          <button class="toggle-raw-btn" @click="showRawJson = !showRawJson">
            <i class="fa-solid" :class="showRawJson ? 'fa-chevron-down' : 'fa-chevron-right'"></i>
            {{ showRawJson ? 'Hide' : 'Show' }} Raw JSON Data
          </button>
          <button 
            v-if="doc.extractedJson"
            class="download-json-btn" 
            @click="downloadJson(doc)"
            title="Download JSON analysis"
          >
            <i class="fa-solid fa-download"></i> Download JSON
          </button>
        </div>
        <div class="json-viewer-wrapper" v-if="showRawJson">
          <div class="json-viewer">
            <pre><code>{{ JSON.stringify(parseJson(doc.extractedJson), null, 2) }}</code></pre>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
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
  overflow-y: auto;
  padding-right: 6px;
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

.structured-data {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.main-amount {
  background: rgba(74, 222, 128, 0.08);
  border: 1px solid rgba(74, 222, 128, 0.2);
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.main-amount .field-label {
  font-size: 0.85rem;
  color: var(--text-muted, #94a3b8);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.main-amount .field-value.highlight {
  font-size: 1.8rem;
  font-weight: 700;
  color: #4ade80;
}

.fields-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.field-item {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  font-size: 0.75rem;
  color: var(--text-muted, #94a3b8);
}

.field-value {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--text);
  word-break: break-all;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.generic-fields-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.generic-field-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 4px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.generic-key {
  font-size: 0.8rem;
  color: var(--text-muted, #94a3b8);
  font-weight: 500;
}

.generic-val {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text);
  max-width: 60%;
  text-align: right;
  word-break: break-all;
}

.no-fields-extracted {
  text-align: center;
  padding: 24px;
  color: var(--text-muted, #94a3b8);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.no-fields-extracted i {
  font-size: 1.5rem;
  color: #fbbf24;
}

.raw-json-section {
  margin-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: 16px;
}

.toggle-raw-btn {
  background: transparent;
  border: none;
  color: var(--text-muted, #94a3b8);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
  transition: color 0.2s;
}

.toggle-raw-btn:hover {
  color: var(--text);
}

.json-viewer-wrapper {
  margin-top: 12px;
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

.raw-json-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.download-json-btn {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: var(--text-muted, #94a3b8);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.2s;
}

.download-json-btn:hover {
  background: rgba(255, 255, 255, 0.05);
  color: var(--primary);
  border-color: var(--primary);
}
</style>
