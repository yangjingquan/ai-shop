const app = getApp();
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    preview: null,
    cartItemIds: [],
    addressId: null,
    noAddress: false,
    submitting: false
  },

  onLoad(options) {
    const cartItemIds = (options.cartItemIds || '').split(',').map(Number);
    this.setData({ cartItemIds });
    this.loadDefaultAddress();
  },

  onShow() {
    const selectedAddressId = wx.getStorageSync('order_selected_address_id');
    if (!selectedAddressId) return;
    wx.removeStorageSync('order_selected_address_id');
    this.setData({ addressId: Number(selectedAddressId), noAddress: false });
    this.loadPreview();
  },

  loadDefaultAddress() {
    app.request({
      url: '/api/wx/addresses',
      method: 'GET'
    }).then(res => {
      if (res.code === 0 && res.data && res.data.length > 0) {
        const defaultAddr = res.data.find(a => a.isDefault) || res.data[0];
        this.setData({ addressId: defaultAddr.id, noAddress: false });
        this.loadPreview();
      } else {
        this.setData({ noAddress: true, preview: null, addressId: null });
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

  chooseAddress() {
    wx.navigateTo({ url: '/pages/address/list?select=1' });
  },

  goMy() {
    wx.switchTab({ url: '/pages/my/index' });
  },

  submitOrder() {
    if (this.data.noAddress || !this.data.addressId) {
      this.goMy();
      return;
    }
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
