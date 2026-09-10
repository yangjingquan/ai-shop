import request from '@/utils/request'

export type ProductSource = 'RECENT' | 'TOP_SALES' | 'RECOMMEND'

export interface HomeModule {
  code: string
  name: string
  description: string
  title: string
  subtitle?: string | null
  enabled: number
  sortOrder: number
  productSource?: ProductSource | null
  productLimit?: number | null
}

export const homeModuleApi = {
  list: () => request.get<unknown, HomeModule[]>('/api/merchant/home/modules'),
  update: (modules: HomeModule[]) => request.put<unknown, void>('/api/merchant/home/modules', { modules }),
}
