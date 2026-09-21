import request from '@/utils/request'

export interface CustomerTag {
  id: number
  code: string
  name: string
  tagType: 'SYSTEM' | 'MANUAL'
  color: string
  status: number
  userCount?: number
}

export interface CustomerMetric {
  paidOrderCount?: number
  totalPaidAmount?: number
  firstPaidAt?: string
  lastPaidAt?: string
  memberLevel?: number
  pointsBalance?: number
  unusedCouponCount?: number
  expiringCouponCount?: number
  favoriteCount?: number
  historyCount?: number
  lastViewedAt?: string
  calculatedAt?: string
}

export interface CustomerUser {
  userId: number
  nickname?: string
  avatar?: string
  phone?: string
  joinedAt?: string
  lastLoginAt?: string
  metric: CustomerMetric
  tags: CustomerTag[]
}

export interface SegmentCondition { field: string; operator: string; value: string }
export interface SegmentConditionGroup { logic: 'AND' | 'OR'; conditions: SegmentCondition[] }
export interface CustomerSegment {
  id: number
  name: string
  description?: string
  segmentType: 'DYNAMIC' | 'STATIC'
  condition: SegmentConditionGroup
  memberCount: number
  calculatedAt?: string
  status: number
  updatedAt?: string
}

export const customerOperationsApi = {
  tags: () => request.get<unknown, CustomerTag[]>('/api/merchant/customer-operations/tags'),
  createTag: (data: { name: string; color?: string; status: number }) => request.post<unknown, number>('/api/merchant/customer-operations/tags', data),
  updateTag: (id: number, data: { name: string; color?: string; status: number }) => request.put<unknown, number>(`/api/merchant/customer-operations/tags/${id}`, data),
  bindTag: (id: number, data: { userIds: number[]; status: number }) => request.post(`/api/merchant/customer-operations/tags/${id}/bindings`, data),
  user: (id: number) => request.get<unknown, CustomerUser & { systemTagNames: string[]; recentOrderAt?: string; recentFavoriteAt?: string }>(`/api/merchant/customer-operations/users/${id}`),
  segments: () => request.get<unknown, CustomerSegment[]>('/api/merchant/customer-operations/segments'),
  createSegment: (data: Omit<CustomerSegment, 'id' | 'memberCount' | 'calculatedAt' | 'updatedAt'>) => request.post<unknown, number>('/api/merchant/customer-operations/segments', data),
  updateSegment: (id: number, data: Omit<CustomerSegment, 'id' | 'memberCount' | 'calculatedAt' | 'updatedAt'>) => request.put<unknown, number>(`/api/merchant/customer-operations/segments/${id}`, data),
  rebuildSegment: (id: number) => request.post(`/api/merchant/customer-operations/segments/${id}/rebuild`),
  segmentUsers: (id: number, params: { page: number; size: number }) => request.get<unknown, { list: CustomerUser[]; total: number; pageNum: number; pageSize: number }>(`/api/merchant/customer-operations/segments/${id}/users`, { params }),
}
