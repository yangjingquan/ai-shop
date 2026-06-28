const addressApi = require('../../api/address')

Page({
  data: {
    list: [],
    selectMode: false,
  },

  onLoad(opts) {
    this.setData({ selectMode: opts && opts.select === '1' })
  },

  onShow() {
    this.loadList()
  },

  loadList() {
    addressApi.list().then((res) => {
      this.setData({ list: res.data || [] })
    }).catch(() => {})
  },

  onSelect(e) {
    if (!this.data.selectMode) return
    const id = e.currentTarget.dataset.id
    wx.setStorageSync('order_selected_address_id', id)
    wx.navigateBack()
  },

  onAdd() {
    wx.navigateTo({ url: '/pages/address/edit' })
  },

  onEdit(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/address/edit?id=${id}` })
  },

  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示',
      content: '确认删除此地址？',
      success: (res) => {
        if (!res.confirm) return
        addressApi.remove(id).then(() => {
          wx.showToast({ title: '已删除' })
          this.loadList()
        }).catch(() => {})
      },
    })
  },

  onSetDefault(e) {
    const id = e.currentTarget.dataset.id
    addressApi.setDefault(id).then(() => {
      wx.showToast({ title: '已设为默认' })
      this.loadList()
    }).catch(() => {})
  },
})
