import type { App, Directive } from 'vue'
import { useUserStore } from '@/stores/user'

const permissionDirective: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const required = Array.isArray(binding.value) ? binding.value : [binding.value]
    const visible = required.some((permission) => useUserStore().hasPermission(permission))
    if (!visible) el.style.display = 'none'
  },
}

export function registerPermissionDirective(app: App) {
  app.directive('permission', permissionDirective)
}
