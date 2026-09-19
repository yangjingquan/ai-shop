Page({
  data: {
    url: '',
  },

  onLoad(options) {
    let url = ''
    try {
      url = decodeURIComponent(options.url || '')
    } catch (error) {
      url = ''
    }
    if (!/^https:\/\/[^\s]+$/i.test(url)) {
      wx.showToast({ title: '链接无效', icon: 'none' })
      return
    }
    this.setData({ url })
  },
})
