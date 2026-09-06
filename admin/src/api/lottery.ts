import request from '@/utils/request'

export interface LotteryCondition {
  minMemberLevel: number
  minPoints: number
  firstOrderOnly: boolean
  repurchaseOnly: boolean
}

export interface LotteryPrize {
  id?: number
  name: string
  prizeType: 'POINTS' | 'COUPON' | 'PHYSICAL' | 'CONSOLATION'
  image?: string
  pointsAmount?: number | null
  couponTemplateId?: number | null
  productId?: number | null
  skuId?: number | null
  totalStock: number
  remainingStock?: number
  probability: number
  validityDays: number
  sort: number
  status: number
  skuOptions?: Array<{ id: number; specText?: string; stock?: number }>
}

export interface LotteryActivity {
  id?: number
  name: string
  themeImage?: string
  entryImage?: string
  ruleText: string
  condition: LotteryCondition
  dailyChances: number
  usedToday?: number
  remainingChances?: number
  startAt: string
  endAt: string
  status: number
  statusText?: string
  active?: boolean
  probabilityVersion?: number
  prizes: LotteryPrize[]
}

export interface LotteryStats {
  participants: number
  drawCount: number
  rewardsIssued: number
  rewardsPending: number
  couponRewards: number
  physicalRewards: number
  pointsRewards: number
  pointsCost: number
}

export interface LotteryReward {
  id: number
  prizeName: string
  prizeType: string
  userId: number
  orderNo?: string
  status: number
  statusText?: string
  createdAt?: string
}

export const lotteryApi = {
  list: () => request.get<unknown, LotteryActivity[]>('/api/merchant/lottery-activities'),
  get: (id: number) => request.get<unknown, LotteryActivity>(`/api/merchant/lottery-activities/${id}`),
  create: (data: LotteryActivity) => request.post<unknown, number>('/api/merchant/lottery-activities', data),
  update: (id: number, data: LotteryActivity) => request.put<unknown, void>(`/api/merchant/lottery-activities/${id}`, data),
  status: (id: number, status: number) => request.put<unknown, void>(`/api/merchant/lottery-activities/${id}/status`, null, { params: { status } }),
  stats: (id: number) => request.get<unknown, LotteryStats>(`/api/merchant/lottery-activities/${id}/stats`),
  rewards: (id: number) => request.get<unknown, LotteryReward[]>(`/api/merchant/lottery-activities/${id}/rewards`),
  rewardStatus: (id: number, rewardId: number, status: number) => request.put<unknown, void>(`/api/merchant/lottery-activities/${id}/rewards/${rewardId}/status`, null, { params: { status } }),
}
