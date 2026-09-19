<script setup lang="ts">
import { onBeforeUnmount, onMounted, shallowRef, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createEditor, createToolbar, type IDomEditor, type IEditorConfig, type IToolbarConfig, type Toolbar } from '@wangeditor/editor'
import '@wangeditor/editor/dist/css/style.css'
import { fileApi } from '@/api/file'
import { useUserStore } from '@/stores/user'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ (event: 'update:modelValue', value: string): void }>()

const toolbarElement = shallowRef<HTMLDivElement | null>(null)
const editorElement = shallowRef<HTMLDivElement | null>(null)
const editor = shallowRef<IDomEditor | null>(null)
const toolbar = shallowRef<Toolbar | null>(null)
const userStore = useUserStore()

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081').replace(/\/$/, '')
const emptyHtml = '<p><br></p>'
const canUpload = userStore.hasPermission('merchant:file:upload')

function resolveImageUrl(url: string) {
  if (/^(https?:)?\/\//i.test(url)) return url
  return `${apiBaseUrl}${url.startsWith('/') ? url : `/${url}`}`
}

function normalizeImagesForEditor(html: string) {
  const document = new DOMParser().parseFromString(html, 'text/html')
  document.querySelectorAll('img[src]').forEach((image) => {
    const source = image.getAttribute('src')?.trim()
    if (source && !/^(https?:)?\/\//i.test(source) && !/^(data|blob):/i.test(source)) {
      image.setAttribute('src', resolveImageUrl(source))
    }
  })
  return document.body.innerHTML
}

function isAllowedLink(url: string) {
  const value = url.trim()
  if (/^\/pages\/[\w/-]+(?:\?[^\s]*)?$/.test(value) && !value.includes('..')) return true
  try {
    const parsed = new URL(value)
    return parsed.protocol === 'https:' && Boolean(parsed.hostname)
  } catch {
    return false
  }
}

const toolbarConfig: Partial<IToolbarConfig> = {
  toolbarKeys: [
    'headerSelect', 'bold', 'italic', 'underline', 'through', 'clearStyle',
    '|', 'color', 'bgColor', 'fontSize',
    '|', 'bulletedList', 'numberedList', 'justifyLeft', 'justifyCenter', 'justifyRight',
    '|', 'insertLink', 'editLink', 'unLink',
    ...(canUpload ? ['uploadImage'] : []),
    '|', 'undo', 'redo',
  ],
}

const editorConfig: Partial<IEditorConfig> = {
  placeholder: '添加图文内容，介绍商品特点、规格和使用方式…',
  autoFocus: false,
  MENU_CONF: {
    insertLink: {
      checkLink: (_text: string, url: string) => isAllowedLink(url) || '请输入 https:// 网页链接或 /pages/ 开头的小程序页面路径',
    },
    editLink: {
      checkLink: (_text: string, url: string) => isAllowedLink(url) || '请输入 https:// 网页链接或 /pages/ 开头的小程序页面路径',
    },
    uploadImage: {
      maxFileSize: 10 * 1024 * 1024,
      maxNumberOfFiles: 20,
      allowedFileTypes: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
      async customUpload(file: File, insert: (url: string, alt: string, href: string) => void) {
        try {
          const result = await fileApi.upload('merchant', file)
          insert(resolveImageUrl(result.url), file.name, '')
        } catch {
          ElMessage.error('图片上传失败，请检查文件格式、大小和上传权限后重试')
        }
      },
    },
  },
  onChange(currentEditor) {
    emit('update:modelValue', currentEditor.getHtml())
  },
}

onMounted(() => {
  if (!toolbarElement.value || !editorElement.value) return
  editor.value = createEditor({
    selector: editorElement.value,
    html: normalizeImagesForEditor(props.modelValue || emptyHtml),
    config: editorConfig,
    mode: 'default',
  })
  toolbar.value = createToolbar({
    editor: editor.value,
    selector: toolbarElement.value,
    config: toolbarConfig,
    mode: 'default',
  })
})

watch(() => props.modelValue, (value) => {
  const nextHtml = normalizeImagesForEditor(value || emptyHtml)
  if (editor.value && editor.value.getHtml() !== nextHtml) editor.value.setHtml(nextHtml)
})

onBeforeUnmount(() => {
  toolbar.value?.destroy()
  editor.value?.destroy()
})
</script>

<template>
  <div class="rich-editor">
    <div ref="toolbarElement" class="rich-editor__toolbar" />
    <div ref="editorElement" class="rich-editor__body" />
    <div class="rich-editor__help">支持文字排版、图片和超链接；图片最大 10MB，网页链接使用 https://。</div>
  </div>
</template>

<style>
.rich-editor {
  width: min(100%, 860px);
  overflow: hidden;
  border: 1px solid var(--shop-border);
  border-radius: 12px;
  background: var(--shop-surface-strong);
}

.rich-editor__toolbar {
  border-bottom: 1px solid var(--shop-border);
  background: #fffaf2;
}

.rich-editor__body {
  min-height: 280px;
  padding: 14px 18px;
}

.rich-editor__body .w-e-text-container {
  min-height: 252px;
  color: var(--shop-text);
  font-size: 14px;
  line-height: 1.75;
}

.rich-editor__help {
  padding: 0 18px 12px;
  color: var(--shop-text-muted);
  font-size: 12px;
  line-height: 1.5;
}
</style>
