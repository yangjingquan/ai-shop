const userApi = require('../../api/user')
const notificationApi = require('../../api/notification')
const marketingCapabilities = require('../../utils/marketing-capabilities')
const couponApi = require('../../api/coupon')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    phone: '',
    nickname: '',
    avatar: '',
    avatarUrl: '',
    unreadCount: 0,
    couponEnabled: false,
    couponCount: 0,
    referralEnabled: false,
    pointsEnabled: false,
    pointsProfile: null,
  },

  onShow() {
    const phone = wx.getStorageSync('user_phone') || ''
    const nickname = wx.getStorageSync('user_nickname') || ''
    const avatar = wx.getStorageSync('user_avatar') || ''
    this.setData({ phone, nickname, avatar, avatarUrl: resolveImageUrl(avatar) })
    this.loadProfile()
    this.loadUnreadCount()
    this.loadCouponEntry()
    this.loadPointsEntry()
  },

  async loadPointsEntry() {
    const featureMap = await marketingCapabilities.load(false).catch(() => ({}))
    const enabled = !!(featureMap.POINTS_MEMBER_DAY && (featureMap.POINTS_MEMBER_DAY.enabled === true || Number(featureMap.POINTS_MEMBER_DAY.enabled) === 1))
    if (!enabled) { this.setData({ pointsEnabled: false, pointsProfile: null }); return }
    this.setData({ pointsEnabled: true })
    require('../../api/points').profile().then(res => this.setData({ pointsProfile: res.data || {} })).catch(() => {})
  },

  goPoints() { wx.navigateTo({ url: '/pages/points/index' }) },

  async loadCouponEntry() {
    const featureMap = await marketingCapabilities.load(false).catch(() => ({}))
    const referralEnabled = !!(featureMap.REFERRAL
      && (featureMap.REFERRAL.enabled === true || Number(featureMap.REFERRAL.enabled) === 1))
    this.setData({ referralEnabled })
    const enabled = !!(featureMap.NEW_USER_COUPON
      && (featureMap.NEW_USER_COUPON.enabled === true || Number(featureMap.NEW_USER_COUPON.enabled) === 1))
    if (!enabled) { this.setData({ couponEnabled: false, couponCount: 0 }); return }
    this.setData({ couponEnabled: true })
    couponApi.list(0).then(res => this.setData({ couponCount: ((res && res.data) || []).length })).catch(() => {})
  },

  goCoupons() { wx.navigateTo({ url: '/pages/coupon/list' }) },

  goReferral() {
    wx.navigateTo({ url: '/pages/activity/referral/index' })
  },

  loadUnreadCount() {
    notificationApi.unreadCount().then((res) => {
      const count = Number(res && res.data && res.data.count || 0)
      this.setData({ unreadCount: count })
    }).catch(() => this.setData({ unreadCount: 0 }))
  },

  loadProfile() {
    userApi.getProfile().then((res) => {
      const profile = (res && res.data) || {}
      const data = {}
      if (profile.phone) {
        data.phone = profile.phone
        wx.setStorageSync('user_phone', profile.phone)
        wx.setStorageSync('has_phone', true)
      }
      if (profile.nickname) {
        data.nickname = profile.nickname
        wx.setStorageSync('user_nickname', profile.nickname)
      }
      if (profile.avatar) {
        data.avatar = profile.avatar
        data.avatarUrl = resolveImageUrl(profile.avatar)
        wx.setStorageSync('user_avatar', profile.avatar)
      }
      this.setData(data)
    }).catch(() => {
      wx.showToast({ title: '个人资料加载失败，请重试', icon: 'none' })
    })
  },

  onChooseAvatar(e) {
    const tempPath = e.detail.avatarUrl || ''
    if (!tempPath) return
    this.setData({ avatarUrl: tempPath })
    const previousAvatar = this.data.avatarUrl
    userApi.uploadAvatar(tempPath).then((avatar) => {
      this.setData({ avatar, avatarUrl: resolveImageUrl(avatar) })
      this.saveProfile({ fields: ['avatar'] })
    }).catch(() => {
      this.setData({ avatarUrl: previousAvatar })
      wx.showToast({ title: '头像上传失败，请重试', icon: 'none' })
    })
  },

  onNicknameInput(e) {
    this.setData({ nickname: e.detail.value })
  },

  onNicknameBlur(e) {
    const nickname = ((e && e.detail && e.detail.value) || this.data.nickname || '').trim()
    this.setData({ nickname })
    this.saveProfile({ fields: ['nickname'], silent: true })
  },

  saveProfile(options = {}) {
    const fields = options.fields || ['nickname', 'avatar']
    const nickname = (this.data.nickname || '').trim()
    const avatar = this.data.avatar || ''
    const payload = {}
    if (fields.includes('nickname') && nickname) payload.nickname = nickname
    if (fields.includes('avatar') && avatar) payload.avatar = avatar
    if (!Object.keys(payload).length) return Promise.resolve()

    return userApi.updateProfile(payload).then(() => {
      if (payload.nickname) wx.setStorageSync('user_nickname', payload.nickname)
      if (payload.avatar) wx.setStorageSync('user_avatar', payload.avatar)
      this.setData({ nickname, avatar, avatarUrl: resolveImageUrl(avatar) })
      if (!options.silent) {
        wx.showToast({ title: '已保存' })
      }
    }).catch(() => {
      wx.showToast({ title: '资料保存失败，请重试', icon: 'none' })
      this.loadProfile()
    })
  },

  onGetPhone(e) {
    if (!e.detail.code) {
      wx.showToast({ title: '已取消', icon: 'none' })
      return
    }
    userApi
      .bindPhone(e.detail.code)
      .then((res) => {
        const phone = (res && res.data) || '已绑定'
        wx.setStorageSync('user_phone', phone)
        wx.setStorageSync('has_phone', true)
        this.setData({ phone })
        wx.showToast({ title: '绑定成功' })
      })
      .catch(() => {
        // request.js 已弹 toast，无需重复
      })
  },

  goOrders() {
    wx.switchTab({ url: '/pages/order/list' })
  },

  goNotifications() {
    wx.navigateTo({ url: '/pages/notification/list' })
  },
})
