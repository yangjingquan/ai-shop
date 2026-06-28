const app = getApp();
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    preview: null,
    cartItemIds: [],
    addressId: null,
    submitting: false
  },

  onLoad(options) {
    const cartItemIds = (options.cartItemIds || '').split(',').map(Number);
    this.setData({ cartItemIds });
    this.loadDefaultAddress();
  },

  loadDefaultAddress() {
    app.request({
      url: '/api/wx/addresses',
      method: 'GET'
    }).then(res => {
      if (res.code === 0 && res.data && res.data.length > 0) {
        const defaultAddr = res.data.find(a => a.isDefault) || res.data[0];
        this.setData({ addressId: defaultAddr.id });
        this.loadPreview();
      } else {
        wx.showToast({ title: '请先添加收货地址', icon: 'none' });
      }
    });
  },

  loadPreview() {
    if (!this.data.addressId) return;
    app.request({
      url: '/api/wx/order/preview',
      method: 'POST',
      data: {
        cartItemIds: this.data.cartItemIds,
        addressId: this.data.addressId
      }
    }).then(res => {
      if (res.code === 0) {
        const preview = {
          ...res.data,
          groups: (res.data.groups || []).map(group => ({
            ...group,
            items: (group.items || []).map(item => ({
              ...item,
              mainImage: resolveImageUrl(item.mainImage || '')
            }))
          }))
        };
        this.setData({ preview });
      }
    });
  },

  submitOrder() {
    if (this.data.submitting) return;
    this.setData({ submitting: true });

    app.request({
      url: '/api/wx/order/create',
      method: 'POST',
      data: {
        cartItemIds: this.data.cartItemIds,
        addressId: this.data.addressId
      }
    }).then(res => {
      if (res.code === 0) {
        const orders = res.data;
        // dev 环境自动 mock-pay
        this.mockPayAll(orders);
      } else {
        wx.showToast({ title: res.msg, icon: 'none' });
        this.setData({ submitting: false });
      }
    }).catch(() => {
      this.setData({ submitting: false });
    });
  },

  mockPayAll(orders) {
    const promises = orders.map(order => {
      return app.request({
        url: '/api/wx/order/' + order.orderNo + '/mock-pay',
        method: 'POST'
      });
    });
    Promise.all(promises).then(() => {
      wx.showToast({ title: '下单成功', icon: 'success' });
      setTimeout(() => {
        wx.switchTab({ url: '/pages/order/list' });
      }, 1000);
    }).catch(() => {
      wx.showToast({ title: '支付失败', icon: 'none' });
    });
  }
});
