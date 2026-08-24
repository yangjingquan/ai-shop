import request from '@/utils/request'

export interface MerchantWechatSettingsVO {
  merchantId: number
  merchantCode: string
  merchantName: string
  wxAppId?: string
  wxMchId?: string
  wxSecretConfigured?: boolean
  wxPayMchSerialNo?: string
  wxPayNotifyUrl?: string
  wxPayEnabled?: number
  wxPayApiV3KeyConfigured?: boolean
  wxPayPrivateKeyConfigured?: boolean
  wxPayConfigured?: boolean
  updatedAt?: string
}

export interface UpdateWechatSettingsPayload {
  wxAppId?: string
  wxSecret?: string
  wxMchId?: string
  wxPayApiV3Key?: string
  wxPayMchSerialNo?: string
  wxPayPrivateKey?: string
  wxPayNotifyUrl?: string
  wxPayEnabled?: number
}

export const wechatSettingsApi = {
  page: (params: { page: number; size: number; keyword?: string }) =>
    request.get<unknown, { list: MerchantWechatSettingsVO[]; total: number; pageNum: number; pageSize: number }>(
      '/api/admin/wechat-settings',
      { params },
    ),
  get: (merchantId: number) =>
    request.get<unknown, MerchantWechatSettingsVO>(`/api/admin/wechat-settings/${merchantId}`),
  update: (merchantId: number, data: UpdateWechatSettingsPayload) =>
    request.put<unknown, void>(`/api/admin/wechat-settings/${merchantId}`, data),
}
