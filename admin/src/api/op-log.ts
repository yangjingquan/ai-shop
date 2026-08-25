import request from '@/utils/request'
import type { PageResult } from '@/api/merchant'

export interface OpLogRow {
  id: number
  operatorType: number
  operatorId: number
  action: string
  targetType?: string
  targetId?: string
  payload?: string
  ip?: string
  createdAt?: string
}

export const opLogApi = {
  page: (params: Record<string, unknown>) =>
    request.get<unknown, PageResult<OpLogRow>>('/api/admin/op-logs/page', { params }),
}
