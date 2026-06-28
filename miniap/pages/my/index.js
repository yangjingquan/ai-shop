const userApi = require('../../api/user')

Page({
  data: {
    phone: '',
    nickname: '',
    avatarUrl: '',
  },

  onShow() {
    this.setData({
      phone: wx.getStorageSync('user_phone') || '',
      nickname: wx.getStorageSync('user_nickname') || '',
      avatarUrl: wx.getStorageSync('user_avatar') || '',
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
})
