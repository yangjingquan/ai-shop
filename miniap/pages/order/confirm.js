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
        const orders = res.data || [];
        this.payCreatedOrders(orders);
      } else {
        wx.showToast({ title: res.msg, icon: 'none' });
        this.setData({ submitting: false });
      }
    }).catch(() => {
      this.setData({ submitting: false });
    });
  },

  payCreatedOrders(orders) {
    if (!orders.length) {
      wx.showToast({ title: '订单创建失败', icon: 'none' });
      this.setData({ submitting: false });
      return;
    }
    const firstOrder = orders[0];
    const pendingCount = orders.length - 1;
    this.payOrder(firstOrder).then(() => {
      const title = pendingCount > 0 ? '支付成功，仍有订单待支付' : '支付成功，状态同步中';
      wx.showToast({ title, icon: 'success' });
      setTimeout(() => {
        wx.switchTab({ url: '/pages/order/list' });
      }, 1000);
    }).catch(() => {
      wx.showToast({ title: '支付未完成', icon: 'none' });
      this.setData({ submitting: false });
    });
  },

  payOrder(order) {
    return orderApi.repay(order.orderNo).then((res) => {
      const payParams = res && res.data && res.data.payParams;
      if (!payParams) {
        throw new Error('payParams missing');
      }
      return new Promise((resolve, reject) => {
        wx.requestPayment({
          timeStamp: payParams.timeStamp,
          nonceStr: payParams.nonceStr,
          package: payParams.packageStr,
          signType: payParams.signType || 'RSA',
          paySign: payParams.paySign,
          success: resolve,
          fail: reject
        });
      });
    });
  }
});
