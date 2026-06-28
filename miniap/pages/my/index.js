const userApi = require('../../api/user')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    phone: '',
    nickname: '',
    avatar: '',
    avatarUrl: '',
  },

  onShow() {
    const phone = wx.getStorageSync('user_phone') || ''
    const nickname = wx.getStorageSync('user_nickname') || ''
    const avatar = wx.getStorageSync('user_avatar') || ''
    this.setData({ phone, nickname, avatar, avatarUrl: resolveImageUrl(avatar) })
    this.loadProfile()
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
    }).catch(() => {})
  },

  onChooseAvatar(e) {
    const tempPath = e.detail.avatarUrl || ''
    if (!tempPath) return
    this.setData({ avatarUrl: tempPath })
    userApi.uploadAvatar(tempPath).then((avatar) => {
      this.setData({ avatar, avatarUrl: resolveImageUrl(avatar) })
      this.saveProfile({ fields: ['avatar'] })
    }).catch(() => {})
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
    }).catch(() => {})
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
})
