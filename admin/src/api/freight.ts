import request from '@/utils/request'
export interface FreightRule { province?: string; city?: string; firstWeightGram: number; firstFee: number; additionalWeightGram: number; additionalFee: number; freeThresholdAmount?: number }
export interface FreightTemplate { id: number; name: string; enabled: number; version: number; rules: FreightRule[] }
export const freightApi = { list: () => request.get<unknown, FreightTemplate[]>('/api/merchant/freight-templates'), create: (data: Omit<FreightTemplate, 'id'|'version'>) => request.post('/api/merchant/freight-templates', data), update: (id:number,data: Omit<FreightTemplate,'id'|'version'>) => request.put(`/api/merchant/freight-templates/${id}`,data), remove:(id:number)=>request.delete(`/api/merchant/freight-templates/${id}`) }
