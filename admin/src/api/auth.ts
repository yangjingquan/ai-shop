import request from '@/utils/request'
import { encryptPassword } from '@/utils/password-crypto'

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
}

async function encryptPasswordFields(
  getPublicKey: () => Promise<string>,
  data: Record<string, string>,
) {
  const publicKey = await getPublicKey()
  const encrypted = await Promise.all(
    Object.entries(data).map(async ([key, value]) => [key, await encryptPassword(publicKey, value)] as const),
  )
  return Object.fromEntries(encrypted)
}

const getAdminPublicKey = () =>
  request.get<unknown, string>('/api/admin/auth/public-key')

const getMerchantPublicKey = () =>
  request.get<unknown, string>('/api/merchant/auth/public-key')

export const adminAuthApi = {
  getPublicKey: getAdminPublicKey,
  login: async (username: string, password: string) =>
    request.post<unknown, unknown>('/api/admin/auth/login', {
      username,
      password: await encryptPasswordFields(getAdminPublicKey, { password }).then((data) => data.password),
    }),
  encryptPassword: (password: string) => encryptPasswordFields(getAdminPublicKey, { password }).then((data) => data.password),
  changePassword: async (data: ChangePasswordPayload) =>
    request.put<unknown, void>('/api/admin/auth/password',
      await encryptPasswordFields(getAdminPublicKey, { ...data })),
}

export const merchantAuthApi = {
  getPublicKey: getMerchantPublicKey,
  encryptPassword: (password: string) => encryptPasswordFields(getMerchantPublicKey, { password }).then((data) => data.password),
  login: async (username: string, password: string) =>
    request.post<unknown, unknown>('/api/merchant/auth/login', {
      username,
      password: await encryptPasswordFields(getMerchantPublicKey, { password }).then((data) => data.password),
    }),
  changePassword: async (data: ChangePasswordPayload) =>
    request.put<unknown, void>('/api/merchant/auth/password',
      await encryptPasswordFields(getMerchantPublicKey, { ...data })),
}

export async function encryptMerchantPassword(password: string) {
  return merchantAuthApi.encryptPassword(password)
}

export async function encryptAdminPassword(password: string) {
  return adminAuthApi.encryptPassword(password)
}
