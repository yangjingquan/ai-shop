import request from '@/utils/request'
import type { PageResult } from '@/api/merchant'

export interface ProductListVO {
  id: number
  merchantId: number
  name: string
  mainImage?: string
  minPrice: number
  maxPrice: number
  totalStock: number
  totalSales: number
  status: number
  categoryId: number
  categoryName?: string
}

export interface ProductSpecValueVO {
  id: number
  value: string
  sort?: number
}

export interface ProductSpecVO {
  id: number
  name: string
  sort?: number
  values: ProductSpecValueVO[]
}

export interface ProductSkuVO {
  id: number
  skuCode?: string
  specValueIds: number[]
  specText?: string
  price: number
  stock: number
  image?: string
}

export interface ProductDetailVO {
  id: number
  merchantId: number
  categoryId: number
  categoryName?: string
  name: string
  subtitle?: string
  mainImage?: string
  images?: string[]
  description?: string
  minPrice: number
  maxPrice: number
  totalStock: number
  totalSales: number
  status: number
  sort?: number
  specs: ProductSpecVO[]
  skus: ProductSkuVO[]
}

export interface ProductSpecInput {
  name: string
  values: string[]
}

export interface ProductSkuInput {
  specValueIndexes: number[]
  price: number
  stock: number
  skuCode?: string
  image?: string
}

export interface ProductSavePayload {
  name: string
  subtitle?: string
  categoryId: number
  mainImage?: string
  images?: string[]
  description?: string
  specs: ProductSpecInput[]
  skus: ProductSkuInput[]
}

export interface ProductPageQuery {
  page: number
  size: number
  categoryId?: number
  keyword?: string
  status?: number
}

export const productApi = {
  page: (params: ProductPageQuery) =>
    request.get<unknown, PageResult<ProductListVO>>('/api/merchant/products', { params }),
  get: (id: number) =>
    request.get<unknown, ProductDetailVO>(`/api/merchant/products/${id}`),
  create: (data: ProductSavePayload) =>
    request.post<unknown, number>('/api/merchant/products', data),
  update: (id: number, data: ProductSavePayload) =>
    request.put<unknown, void>(`/api/merchant/products/${id}`, data),
  setStatus: (id: number, status: number) =>
    request.put<unknown, void>(`/api/merchant/products/${id}/status`, null, {
      params: { status },
    }),
  remove: (id: number) =>
    request.delete<unknown, void>(`/api/merchant/products/${id}`),
}
