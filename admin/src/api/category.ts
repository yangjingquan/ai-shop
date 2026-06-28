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
