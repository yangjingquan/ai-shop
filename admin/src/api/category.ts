import request from '@/utils/request'

export interface CategoryVO {
  id: number
  parentId: number
  name: string
  icon?: string
  level: number
  sort: number
  status: number
  children?: CategoryVO[]
}

export interface CategoryPayload {
  name: string
  parentId?: number
  icon?: string
  sort?: number
}

export const categoryApi = {
  tree: () => request.get<CategoryVO[], CategoryVO[]>('/api/admin/categories/tree'),
  publicTree: () => request.get<CategoryVO[], CategoryVO[]>('/api/public/categories/tree'),
  create: (data: CategoryPayload) =>
    request.post<number, number>('/api/admin/categories', data),
  update: (id: number, data: CategoryPayload) =>
    request.put<void, void>(`/api/admin/categories/${id}`, data),
  setStatus: (id: number, status: number) =>
    request.put<void, void>(`/api/admin/categories/${id}/status`, null, {
      params: { status },
    }),
  remove: (id: number) =>
    request.delete<void, void>(`/api/admin/categories/${id}`),
}

export interface MerchantCategoryVO extends Omit<CategoryVO, 'children'> {
  merchantId: number
  sourceCategoryId?: number | null
  children?: MerchantCategoryVO[]
}

export interface MerchantCategoryImportPayload {
  sourceCategoryIds: number[]
  includeChildren?: boolean
}

export const merchantCategoryApi = {
  tree: () => request.get<MerchantCategoryVO[], MerchantCategoryVO[]>('/api/merchant/categories/tree'),
  enabledTree: () => request.get<MerchantCategoryVO[], MerchantCategoryVO[]>('/api/merchant/categories/enabled-tree'),
  platformTree: () => request.get<CategoryVO[], CategoryVO[]>('/api/merchant/categories/platform-tree'),
  create: (data: CategoryPayload) =>
    request.post<number, number>('/api/merchant/categories', data),
  importFromPlatform: (data: MerchantCategoryImportPayload) =>
    request.post<void, void>('/api/merchant/categories/import', data),
  update: (id: number, data: CategoryPayload) =>
    request.put<void, void>(`/api/merchant/categories/${id}`, data),
  setStatus: (id: number, status: number) =>
    request.put<void, void>(`/api/merchant/categories/${id}/status`, null, {
      params: { status },
    }),
  remove: (id: number) =>
    request.delete<void, void>(`/api/merchant/categories/${id}`),
}
