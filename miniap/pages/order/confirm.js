const app = getApp();
const { resolveImageUrl } = require('../../utils/url')
const groupBuyApi = require('../../api/group-buy')
const orderApi = require('../../api/order')

Page({
  data: {
    preview: null,
    cartItemIds: [],
    addressId: null,
    noAddress: false,
    submitting: false,
    addresses: [],
    selectedAddress: null,
    mode: 'cart',
    productId: 0,
    skuId: 0,
    quantity: 1,
    groupId: 0,
  },

  onLoad(options) {
    const mode = options.mode || 'cart';
    const cartItemIds = (options.cartItemIds || '').split(',').filter(Boolean).map(Number);
    this.setData({
      mode,
      cartItemIds,
      productId: Number(options.productId || 0),
      skuId: Number(options.skuId || 0),
      quantity: Number(options.quantity || 1),
      groupId: Number(options.groupId || 0),
    });
    this.loadDefaultAddress();
  },

  onShow() {
    const selectedAddressId = wx.getStorageSync('order_selected_address_id');
    if (!selectedAddressId) return;
    wx.removeStorageSync('order_selected_address_id');
    const selectedAddress = (this.data.addresses || []).find(a => Number(a.id) === Number(selectedAddressId)) || this.data.selectedAddress;
    this.setData({ addressId: Number(selectedAddressId), selectedAddress, noAddress: false });
    this.loadPreview();
  },

  loadDefaultAddress() {
    app.request({
      url: '/api/wx/addresses',
      method: 'GET'
    }).then(res => {
      if (res.code === 0 && res.data && res.data.length > 0) {
        const defaultAddr = res.data.find(a => a.isDefault) || res.data[0];
        this.setData({ addresses: res.data, selectedAddress: defaultAddr, addressId: defaultAddr.id, noAddress: false });
        this.loadPreview();
      } else {
        this.setData({ addresses: [], selectedAddress: null, noAddress: true, preview: null, addressId: null });
      }
    });
  },

  loadPreview() {
    if (!this.data.addressId) return;
    if (this.data.mode === 'groupBuy') {
      this.loadGroupBuyPreview();
      return;
    }
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

  loadGroupBuyPreview() {
    groupBuyApi.productDetail(this.data.productId).then(res => {
      const product = res.data && res.data.product;
      const sku = product && (product.skus || []).find(item => Number(item.id) === this.data.skuId);
      if (!product || !sku) {
        wx.showToast({ title: '商品规格不存在', icon: 'none' });
        this.setData({ preview: null });
        return;
      }
      const unitPrice = Number(product.groupBuyPrice || sku.price || 0).toFixed(2);
      const quantity = Number(this.data.quantity || 1);
      const subtotal = (Number(unitPrice) * quantity).toFixed(2);
      const preview = {
        address: this.data.selectedAddress || {},
        totalAmount: subtotal,
        groups: [{
          merchantId: product.merchantId || 0,
          merchantName: product.merchantName || '团购商品',
          payAmount: subtotal,
          items: [{
            cartItemId: this.data.skuId,
            productName: product.name,
            specText: sku.specText || '',
            mainImage: resolveImageUrl(sku.image || product.mainImage || ''),
            available: true,
            unitPrice,
            quantity,
            subtotal,
          }],
        }],
      };
      this.setData({ preview });
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

    if (this.data.mode === 'groupBuy') {
      const payload = {
        productId: this.data.productId,
        skuId: this.data.skuId,
        quantity: this.data.quantity,
        addressId: this.data.addressId,
      };
      const request = this.data.groupId
        ? groupBuyApi.join(this.data.groupId, payload)
        : groupBuyApi.open(payload);
      request.then((res) => {
        if (res.code === 0) {
          return orderApi.mockPay(res.data.orderNo).then(() => {
            wx.showToast({ title: '下单成功', icon: 'success' });
            setTimeout(() => wx.redirectTo({
              url: `/pages/group-buy/group?groupId=${res.data.groupId}`,
            }), 1000);
          }).catch(() => {
            wx.showToast({ title: '支付失败', icon: 'none' });
            this.setData({ submitting: false });
          });
        }
        wx.showToast({ title: res.msg, icon: 'none' });
        this.setData({ submitting: false });
      }).catch(() => this.setData({ submitting: false }));
      return;
    }

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
    const payParams = order && order.payParams;
    if (!payParams) {
      return Promise.reject(new Error('payParams missing'));
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
  }
});
