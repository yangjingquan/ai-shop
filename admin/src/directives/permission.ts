import type { App, Directive } from 'vue'
import { useUserStore } from '@/stores/user'

function updateVisibility(el: HTMLElement, value: string | string[]) {
  const required = Array.isArray(value) ? value : [value]
  const visible = required.some((permission) => useUserStore().hasPermission(permission))
  el.style.display = visible ? '' : 'none'
}

const permissionDirective: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    updateVisibility(el, binding.value)
  },
  updated(el, binding) {
    updateVisibility(el, binding.value)
  },
}

export function registerPermissionDirective(app: App) {
  app.directive('permission', permissionDirective)
}
