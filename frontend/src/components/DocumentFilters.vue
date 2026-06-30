<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'

interface User {
  id: number
  email: string
  token: string
  role?: string
}

interface UserListItem {
  id: number
  email: string
  role?: string
}

const props = defineProps<{
  currentUser: User
  types: string[]
  usersList: UserListItem[]
}>()

// Define two-way bound models using Vue 3.4+ defineModel
const searchQuery = defineModel<string>('searchQuery', { default: '' })
const searchType = defineModel<string>('searchType', { default: '' })
const searchYear = defineModel<string>('searchYear', { default: '' })
const searchUser = defineModel<string>('searchUser', { default: '' })

// Dropdown state for user selector
const showDropdown = ref(false)
const userSearchText = ref('')
const searchInputRef = ref<HTMLInputElement | null>(null)
const customSelectRef = ref<HTMLElement | null>(null)

const filteredUsersList = computed(() => {
  if (!userSearchText.value) return props.usersList
  const query = userSearchText.value.toLowerCase()
  return props.usersList.filter(u => u.email.toLowerCase().includes(query))
})

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
  if (showDropdown.value) {
    userSearchText.value = ''
    nextTick(() => {
      searchInputRef.value?.focus()
    })
  }
}

const selectUser = (email: string) => {
  searchUser.value = email
  showDropdown.value = false
}

const handleClickOutside = (event: MouseEvent) => {
  if (customSelectRef.value && !customSelectRef.value.contains(event.target as Node)) {
    showDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="glass-card toolbar-section">
    <div class="filters-row">
      <!-- Text Query Search -->
      <div class="search-box inline-search">
        <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input 
          type="text" 
          v-model="searchQuery" 
          placeholder="Search values inside files..." 
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

      <!-- User Select (Admin only) -->
      <div 
        v-if="currentUser.role === 'ADMIN'" 
        class="select-wrapper custom-select-wrapper" 
        ref="customSelectRef"
      >
        <!-- Simulated Select Box -->
        <div 
          class="simulated-select" 
          @click="toggleDropdown"
          :class="{ open: showDropdown }"
        >
          {{ searchUser ? searchUser : 'All Users' }}
        </div>
        
        <!-- Custom Dropdown Menu -->
        <div class="custom-dropdown" v-if="showDropdown">
          <div class="dropdown-search-box">
            <input 
              type="text" 
              v-model="userSearchText" 
              placeholder="Filter users..." 
              ref="searchInputRef"
              @click.stop
            />
          </div>
          
          <div class="dropdown-options">
            <div 
              class="dropdown-option" 
              :class="{ active: searchUser === '' }"
              @click="selectUser('')"
            >
              All Users
            </div>
            <div 
              v-for="u in filteredUsersList" 
              :key="u.id" 
              class="dropdown-option"
              :class="{ active: searchUser === u.email }"
              @click="selectUser(u.email)"
            >
              {{ u.email }}
            </div>
            <div v-if="filteredUsersList.length === 0" class="no-options">
              No users found
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar-section {
  padding: 16px;
  position: relative;
  z-index: 50;
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

.custom-select-wrapper {
  position: relative;
  width: 200px;
}

.simulated-select {
  width: 100%;
  padding: 12px 36px 12px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text);
  font-size: 0.9rem;
  outline: none;
  cursor: pointer;
  box-sizing: border-box;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
  transition: all 0.2s;
}

.simulated-select:hover {
  background: rgba(36, 40, 52, 0.4);
  border-color: rgba(255, 255, 255, 0.15);
}

.simulated-select.open {
  border-color: var(--primary);
}

.custom-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 6px;
  background: #1e2230;
  border: 1px solid var(--border);
  border-radius: 8px;
  z-index: 100;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dropdown-search-box {
  padding: 8px;
  border-bottom: 1px solid var(--border);
}

.dropdown-search-box input {
  width: 100%;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid var(--border);
  border-radius: 6px;
  color: var(--text);
  font-size: 0.85rem;
  outline: none;
  box-sizing: border-box;
}

.dropdown-search-box input:focus {
  border-color: var(--primary);
}

.dropdown-options {
  max-height: 200px;
  overflow-y: auto;
}

.dropdown-option {
  padding: 10px 16px;
  cursor: pointer;
  color: var(--text-muted);
  font-size: 0.85rem;
  text-align: left;
  transition: all 0.2s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-option:hover {
  background: var(--bg-hover);
  color: var(--text);
}

.dropdown-option.active {
  background: var(--bg-active);
  color: var(--primary);
  font-weight: 600;
}

.no-options {
  padding: 12px;
  font-size: 0.85rem;
  color: var(--text-muted);
  text-align: center;
}
</style>
