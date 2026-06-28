const addressApi = require('../../api/address')

Page({
  data: {
    id: null,
    form: {
      receiver: '',
      phone: '',
      region: '',
      detail: '',
      isDefault: false,
    },
  },

  onLoad(opts) {
    if (opts && opts.id) {
      this.setData({ id: Number(opts.id) })
      addressApi.get(opts.id).then((res) => {
        const d = res.data || {}
        this.setData({
          form: {
            receiver: d.receiver || '',
            phone: d.phone || '',
            region: d.region || '',
            detail: d.detail || '',
            isDefault: !!d.isDefault,
          },
        })
      }).catch(() => {})
    }
  },

  onReceiver(e) {
    this.setData({ 'form.receiver': e.detail.value })
  },

  onPhone(e) {
    this.setData({ 'form.phone': e.detail.value })
  },

  onDetail(e) {
    this.setData({ 'form.detail': e.detail.value })
  },

  onDefaultChange(e) {
    this.setData({ 'form.isDefault': e.detail.value })
  },

  onRegionChange(e) {
    const arr = e.detail.value || []
    this.setData({ 'form.region': arr.join('/') })
  },

  onSubmit() {
    const f = this.data.form
    if (!f.receiver) {
      wx.showToast({ title: '请输入收货人', icon: 'none' })
      return
    }
    if (!/^1[3-9]\d{9}$/.test(f.phone)) {
      wx.showToast({ title: '手机号格式错误', icon: 'none' })
      return
    }
    if (!f.region) {
      wx.showToast({ title: '请选择地区', icon: 'none' })
      return
    }
    if (!f.detail) {
      wx.showToast({ title: '请输入详细地址', icon: 'none' })
      return
    }

    const payload = {
      receiver: f.receiver,
      phone: f.phone,
      region: f.region,
      detail: f.detail,
      isDefault: !!f.isDefault,
    }

    const promise = this.data.id
      ? addressApi.update(this.data.id, payload)
      : addressApi.create(payload)

    promise.then(() => {
      wx.showToast({ title: '保存成功' })
      setTimeout(() => wx.navigateBack(), 600)
    }).catch(() => {})
  },
})
