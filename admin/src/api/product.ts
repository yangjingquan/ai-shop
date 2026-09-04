import request from '@/utils/request'
import type { PageResult } from '@/api/merchant'

export interface ProductListVO {
  id: number
  merchantId: number
  name: string
  mainImage?: string
  minPrice: number
  maxPrice: number
  minOriginalPrice?: number | null
  maxOriginalPrice?: number | null
  totalStock: number
  totalSales: number
  status: number
  auditStatus?: number
  auditReason?: string
  auditedBy?: number | null
  auditOperatorType?: number | null
  auditedAt?: string | null
  isRecommend?: number
  isGroupBuy?: number
  groupBuyPrice?: number | null
  groupBuyRequiredCount?: number | null
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
  originalPrice?: number | null
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
  minOriginalPrice?: number | null
  maxOriginalPrice?: number | null
  totalStock: number
  totalSales: number
  status: number
  auditStatus?: number
  auditReason?: string
  auditedBy?: number | null
  auditOperatorType?: number | null
  auditedAt?: string | null
  isRecommend?: number
  isGroupBuy?: number
  groupBuyPrice?: number | null
  groupBuyRequiredCount?: number | null
  groupBuyDurationHours?: number | null
  groupBuyUserLimit?: number | null
  groupBuyShowActive?: number | null
  groupBuySkuIds?: number[]
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
  originalPrice?: number | null
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
  isRecommend: number
  isGroupBuy?: number
  groupBuyPrice?: number | null
  groupBuyRequiredCount?: number | null
  groupBuyDurationHours?: number | null
  groupBuyUserLimit?: number | null
  groupBuyShowActive?: number | null
  groupBuySkuIds?: number[]
  specs: ProductSpecInput[]
  skus: ProductSkuInput[]
}

export interface ProductPageQuery {
  page: number
  size: number
  categoryId?: number
  keyword?: string
  status?: number
  isRecommend?: number
  isGroupBuy?: number
  auditStatus?: number
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
  audit: (id: number) =>
    request.put<unknown, void>(`/api/merchant/products/${id}/audit`),
  setStatus: (id: number, status: number) =>
    request.put<unknown, void>(`/api/merchant/products/${id}/status`, null, {
      params: { status },
    }),
  remove: (id: number) =>
    request.delete<unknown, void>(`/api/merchant/products/${id}`),
}

export const adminProductApi = {
  auditPage: (params: { page: number; size: number; auditStatus?: number; keyword?: string; merchantId?: number }) =>
    request.get<unknown, PageResult<ProductListVO>>('/api/admin/products/audit/page', { params }),
  forceOffline: (id: number, reason: string) =>
    request.post<unknown, void>(`/api/admin/products/${id}/force-offline`, { reason }),
}

export interface InventorySkuVO {
  skuId: number
  productId: number
  productName: string
  mainImage?: string
  skuCode?: string
  specText?: string
  stock: number
}

export interface InventoryTransactionVO {
  id: number
  productId: number
  skuId: number
  productName?: string
  skuCode?: string
  specText?: string
  changeQty: number
  stockBefore: number
  stockAfter: number
  operationType: string
  referenceNo?: string
  reason?: string
  operatorId?: number
  createdAt?: string
}

export const inventoryApi = {
  skus: (params: {
    page: number
    size: number
    keyword?: string
    lowStockOnly?: boolean
    threshold?: number
  }) => request.get<unknown, PageResult<InventorySkuVO>>('/api/merchant/inventory/skus', { params }),
  transactions: (params: { page: number; size: number; skuId?: number }) =>
    request.get<unknown, PageResult<InventoryTransactionVO>>('/api/merchant/inventory/transactions', { params }),
  adjust: (data: { skuId: number; changeQty: number; reason?: string }) =>
    request.post<unknown, void>('/api/merchant/inventory/adjust', data),
}
