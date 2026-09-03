import request from '@/utils/request'
import { encryptMerchantPassword } from '@/api/auth'

export interface MerchantRole {
  id: number
  code: string
  name: string
  description?: string
  builtin: number
  status: number
  userCount?: number
  permissionCodes: string[]
}

export interface MerchantPermission {
  id: number
  code: string
  name: string
  module: string
  type: 'MENU' | 'BUTTON' | 'API'
  parentId?: number | null
  sort: number
}

export interface MerchantUser {
  id: number
  username: string
  status: number
  createdAt?: string
  roles: MerchantRole[]
}

export interface MerchantAuthMe {
  userId: number
  username: string
  merchantId: number
  merchantName: string
  roles: MerchantRole[]
  permissions: string[]
}

export const merchantRbacApi = {
  me: () => request.get<unknown, MerchantAuthMe>('/api/merchant/auth/me'),
  permissions: () => request.get<unknown, MerchantPermission[]>('/api/merchant/rbac/permissions'),
  roles: () => request.get<unknown, MerchantRole[]>('/api/merchant/rbac/roles'),
  createRole: (data: { code: string; name: string; description?: string; permissionCodes: string[] }) =>
    request.post<unknown, { id: number }>('/api/merchant/rbac/roles', data),
  updateRole: (id: number, data: { code: string; name: string; description?: string; permissionCodes: string[] }) =>
    request.put<unknown, void>(`/api/merchant/rbac/roles/${id}`, data),
  deleteRole: (id: number) => request.delete<unknown, void>(`/api/merchant/rbac/roles/${id}`),
  users: () => request.get<unknown, MerchantUser[]>('/api/merchant/rbac/users'),
  createUser: async (data: { username: string; password: string; roleIds: number[] }) =>
    request.post<unknown, { id: number }>('/api/merchant/rbac/users', {
      ...data,
      password: await encryptMerchantPassword(data.password),
    }),
  setUserStatus: (id: number, status: number) =>
    request.put<unknown, void>(`/api/merchant/rbac/users/${id}/status`, { status }),
  setUserRoles: (id: number, roleIds: number[]) =>
    request.put<unknown, void>(`/api/merchant/rbac/users/${id}/roles`, { roleIds }),
  resetUserPassword: async (id: number, password: string) =>
    request.put<unknown, void>(`/api/merchant/rbac/users/${id}/password`, {
      password: await encryptMerchantPassword(password),
    }),
}
