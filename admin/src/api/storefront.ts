import request from '@/utils/request'

export type StorefrontModuleCode = 'BANNER' | 'CATEGORY' | 'NEW_ARRIVALS' | 'POPULAR_PRODUCTS' | 'PRODUCT_FEED' | 'MARKETING_ZONE' | 'TOPIC_ENTRY'
export type ProductSource = 'RECENT' | 'TOP_SALES' | 'RECOMMEND'

export interface StorefrontModule {
  id: string
  code: StorefrontModuleCode
  title: string
  subtitle?: string
  enabled: number
  sortOrder: number
  productSource?: ProductSource
  productLimit?: number
  topicPageIds?: number[]
}

export interface StorefrontBlock {
  id: string
  type: 'TEXT' | 'IMAGE_TEXT' | 'PRODUCTS' | 'BUTTON'
  title?: string
  body?: string
  imageUrl: string
  buttonText?: string
  linkType?: 'PRODUCTS' | 'CATEGORY' | 'COUPON' | 'GROUP_BUY'
  productSource?: ProductSource
  productLimit?: number
}

export interface StorefrontDocument { modules: StorefrontModule[]; blocks: StorefrontBlock[] }
export interface StorefrontPage { id?: number; pageType: 'HOME' | 'TOPIC'; title: string; slug?: string; summary?: string; coverImage: string; status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE'; publishedAt?: string; draft: StorefrontDocument; published?: StorefrontDocument; draftChanged?: boolean }
export interface StorefrontPageSummary { id: number; title: string; slug: string; summary?: string; coverImage?: string; status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE'; updatedAt?: string; publishedAt?: string }
export interface StorefrontTemplate { code: string; name: string; description: string; pageType: 'HOME'; document: StorefrontDocument }

const pagePayload = (page: StorefrontPage) => ({ title: page.title, slug: page.slug, summary: page.summary, coverImage: page.coverImage, document: page.draft })

export const storefrontApi = {
  home: () => request.get<unknown, StorefrontPage>('/api/merchant/storefront/home'),
  saveHome: (page: StorefrontPage) => request.put<unknown, void>('/api/merchant/storefront/home', pagePayload(page)),
  publishHome: () => request.post<unknown, void>('/api/merchant/storefront/home/publish'),
  templates: () => request.get<unknown, StorefrontTemplate[]>('/api/merchant/storefront/templates'),
  topics: () => request.get<unknown, StorefrontPageSummary[]>('/api/merchant/storefront/topics'),
  topic: (id: number) => request.get<unknown, StorefrontPage>(`/api/merchant/storefront/topics/${id}`),
  createTopic: (page: StorefrontPage) => request.post<unknown, StorefrontPage>('/api/merchant/storefront/topics', pagePayload(page)),
  updateTopic: (id: number, page: StorefrontPage) => request.put<unknown, StorefrontPage>(`/api/merchant/storefront/topics/${id}`, pagePayload(page)),
  publishTopic: (id: number) => request.post<unknown, void>(`/api/merchant/storefront/topics/${id}/publish`),
  offlineTopic: (id: number) => request.post<unknown, void>(`/api/merchant/storefront/topics/${id}/offline`),
  publicTopic: (slug: string) => request.get<unknown, StorefrontPage>(`/api/public/store-pages/${encodeURIComponent(slug)}`),
}
