<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fileApi, type UploadScope } from '@/api/file'

const props = withDefaults(defineProps<{
  modelValue: string | string[]
  scope: UploadScope
  multiple?: boolean
  limit?: number
  label?: string
}>(), {
  multiple: false,
  limit: 9,
  label: '上传图片',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | string[]): void
}>()

const inputRef = ref<HTMLInputElement>()
const uploading = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')
const helpText = computed(() =>
  props.multiple ? `支持批量选择，最多 ${props.limit} 张；支持 jpg / png / webp / gif` : '支持 jpg / png / webp / gif',
)

const urls = computed<string[]>(() => {
  if (Array.isArray(props.modelValue)) return props.modelValue.filter(Boolean)
  return props.modelValue ? [props.modelValue] : []
})

function resolveImageUrl(url: string) {
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url
  return `${apiBaseUrl}${url.startsWith('/') ? url : `/${url}`}`
}

function openPicker() {
  inputRef.value?.click()
}

function openPreview(url: string) {
  previewUrl.value = resolveImageUrl(url)
  previewVisible.value = true
}

async function handleFiles(event: Event) {
  const input = event.target as HTMLInputElement
  const selected = Array.from(input.files ?? [])
  input.value = ''
  if (selected.length === 0) return

  const imageFiles = selected.filter((file) => file.type.startsWith('image/'))
  if (imageFiles.length !== selected.length) {
    ElMessage.warning('只能上传图片文件')
  }
  if (imageFiles.length === 0) return

  const remain = props.multiple ? props.limit - urls.value.length : 1
  if (remain <= 0) {
    ElMessage.warning(`最多上传 ${props.limit} 张图片`)
    return
  }
  const files = imageFiles.slice(0, remain)
  if (files.length < imageFiles.length) {
    ElMessage.warning(`最多上传 ${props.limit} 张图片，已自动截取`)
  }

  uploading.value = true
  try {
    const uploaded = files.length > 1
      ? await fileApi.uploadBatch(props.scope, files)
      : [(await fileApi.upload(props.scope, files[0])).url]
    if (props.multiple) {
      emit('update:modelValue', [...urls.value, ...uploaded])
    } else {
      emit('update:modelValue', uploaded[0] ?? '')
    }
    ElMessage.success('上传成功')
  } finally {
    uploading.value = false
  }
}

async function removeImage(url: string) {
  try {
    await fileApi.remove(props.scope, url)
  } finally {
    if (props.multiple) {
      emit('update:modelValue', urls.value.filter((item) => item !== url))
    } else {
      emit('update:modelValue', '')
    }
  }
}
</script>

<template>
  <div class="image-uploader">
    <div class="image-list" :class="{ empty: urls.length === 0 }">
      <div v-for="url in urls" :key="url" class="image-card">
        <button class="preview-trigger" type="button" title="点击放大查看" @click="openPreview(url)">
          <img :src="resolveImageUrl(url)" alt="已上传图片" class="preview" />
        </button>
        <button class="remove" type="button" title="删除图片" @click.stop="removeImage(url)">×</button>
      </div>
      <button
        v-if="multiple ? urls.length < limit : urls.length === 0"
        class="upload-card"
        type="button"
        :disabled="uploading"
        @click="openPicker"
      >
        <span>{{ uploading ? '上传中...' : label }}</span>
      </button>
    </div>
    <div class="upload-help">{{ helpText }}</div>
    <input
      ref="inputRef"
      class="file-input"
      type="file"
      accept="image/*"
      :multiple="multiple"
      @change="handleFiles"
    />
    <el-dialog v-model="previewVisible" class="image-preview-dialog" width="min(860px, 86vw)" append-to-body>
      <img :src="previewUrl" alt="图片预览" class="large-preview" />
    </el-dialog>
  </div>
</template>

<style scoped>
.image-uploader {
  width: 100%;
  max-width: 640px;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-list.empty {
  min-height: 96px;
}

.image-card,
.upload-card {
  position: relative;
  width: 112px;
  height: 112px;
  overflow: hidden;
  border: 1px solid var(--shop-border);
  border-radius: 16px;
  background: #fffaf2;
}

.preview-trigger {
  display: block;
  width: 100%;
  height: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: zoom-in;
}

.preview {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s ease;
}

.image-card:hover .preview {
  transform: scale(1.05);
}

.remove {
  position: absolute;
  top: 6px;
  right: 6px;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 999px;
  color: #fff;
  background: rgba(32, 20, 12, 0.72);
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
  opacity: 0;
  transform: translateY(-3px);
  transition: opacity 0.18s ease, transform 0.18s ease, background 0.18s ease;
}

.image-card:hover .remove {
  opacity: 1;
  transform: translateY(0);
}

.remove:hover {
  background: #c94a3a;
}

.upload-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--shop-primary-dark);
  cursor: pointer;
}

.upload-card::before {
  content: '+';
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  color: #fff;
  background: var(--shop-primary);
  font-size: 22px;
  line-height: 1;
}

.upload-card span {
  font-weight: 800;
}

.upload-help {
  width: 100%;
  margin-top: 8px;
  color: var(--shop-text-muted);
  font-size: 12px;
  line-height: 1.5;
  text-align: right;
}

.upload-card:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.file-input {
  display: none;
}

.large-preview {
  display: block;
  max-width: 100%;
  max-height: 72vh;
  margin: 0 auto;
  border-radius: 16px;
  object-fit: contain;
}

:global(.image-preview-dialog .el-dialog__body) {
  padding: 12px 16px 18px;
}
</style>
