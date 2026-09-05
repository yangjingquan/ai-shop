const app = getApp();
const { resolveImageUrl } = require('../../utils/url')
const groupBuyApi = require('../../api/group-buy')
const seckillApi = require('../../api/seckill')

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
    sessionId: 0,
    seckillSkuId: 0,
    selectedCouponId: null,
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
      sessionId: Number(options.sessionId || 0),
      seckillSkuId: Number(options.seckillSkuId || 0),
    });
    this.loadDefaultAddress();
  },

  onShow() {
    const selectedCouponId = wx.getStorageSync('order_selected_coupon_id');
    if (selectedCouponId) {
      wx.removeStorageSync('order_selected_coupon_id');
      this.setData({ selectedCouponId: Number(selectedCouponId) });
    }
    const selectedAddressId = wx.getStorageSync('order_selected_address_id');
    if (selectedAddressId) {
      wx.removeStorageSync('order_selected_address_id');
      const selectedAddress = (this.data.addresses || []).find(a => Number(a.id) === Number(selectedAddressId)) || this.data.selectedAddress;
      this.setData({ addressId: Number(selectedAddressId), selectedAddress, noAddress: false });
      this.loadPreview();
    } else if (selectedCouponId && this.data.addressId) {
      this.loadPreview();
    }
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
    if (this.data.mode === 'seckill') {
      this.loadSeckillPreview();
      return;
    }
    app.request({
      url: '/api/wx/order/preview',
      method: 'POST',
      data: {
        cartItemIds: this.data.cartItemIds,
        addressId: this.data.addressId,
        couponId: this.data.selectedCouponId || null
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
        this.setData({ preview, selectedCouponId: preview.couponId || null });
      } else {
        this.setData({ preview: null });
        wx.showToast({ title: res.msg || '不支持跨商家结算', icon: 'none' });
      }
    }).catch(() => {
      this.setData({ preview: null });
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

  loadSeckillPreview() {
    seckillApi.preview({
      sessionId: this.data.sessionId,
      seckillSkuId: this.data.seckillSkuId,
      addressId: this.data.addressId,
      quantity: this.data.quantity,
    }).then((res) => {
      if (res.code !== 0) {
        this.setData({ preview: null })
        wx.showToast({ title: res.msg || '秒杀活动不可用', icon: 'none' })
        return
      }
      const data = res.data || {}
      const unitPrice = Number(data.activityPrice || 0).toFixed(2)
      const quantity = Number(data.quantity || this.data.quantity || 1)
      const subtotal = (Number(unitPrice) * quantity).toFixed(2)
      const preview = {
        ...data,
        mainImage: resolveImageUrl(data.mainImage || ''),
        totalAmount: data.totalAmount || subtotal,
        payAmount: data.payAmount || subtotal,
        groups: [{
          merchantId: 0,
          merchantName: '秒杀商品',
          payAmount: data.payAmount || subtotal,
          items: [{
            cartItemId: data.seckillSkuId,
            productName: data.productName,
            specText: data.specText || '',
            mainImage: resolveImageUrl(data.mainImage || ''),
            available: true,
            unitPrice,
            quantity,
            subtotal,
          }],
        }],
      }
      this.setData({ preview })
    }).catch(() => {
      this.setData({ preview: null })
      wx.showToast({ title: '秒杀信息加载失败，请重试', icon: 'none' })
    })
  },

  chooseAddress() {
    wx.navigateTo({ url: '/pages/address/list?select=1' });
  },

  chooseCoupon() {
    wx.navigateTo({ url: '/pages/coupon/list?mode=select' });
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
          return this.payOrder(res.data).then(() => {
            wx.showToast({ title: '下单成功', icon: 'success' });
            setTimeout(() => wx.redirectTo({
              url: `/pages/order/success?orderNo=${res.data.orderNo || ''}&groupId=${res.data.groupId || 0}`,
            }), 1000);
          }).catch(() => {
            this.setData({ submitting: false });
            wx.showModal({
              title: '支付未完成',
              content: '订单已创建，可在订单列表中重新支付。',
              confirmText: '去订单',
              cancelText: '留在这里',
              success: (modalRes) => {
                if (modalRes.confirm) wx.switchTab({ url: '/pages/order/list' });
              },
            });
          });
        }
        wx.showToast({ title: res.msg, icon: 'none' });
        this.setData({ submitting: false });
      }).catch(() => this.setData({ submitting: false }));
      return;
    }

    if (this.data.mode === 'seckill') {
      seckillApi.createOrder({
        sessionId: this.data.sessionId,
        seckillSkuId: this.data.seckillSkuId,
        addressId: this.data.addressId,
        quantity: this.data.quantity,
      }).then((res) => {
        if (res.code !== 0) {
          wx.showToast({ title: res.msg || '秒杀下单失败', icon: 'none' })
          this.setData({ submitting: false })
          return
        }
        return this.payOrder(res.data).then(() => {
          wx.showToast({ title: '抢购成功，状态同步中', icon: 'success' })
          setTimeout(() => wx.redirectTo({ url: `/pages/order/success?orderNo=${res.data.orderNo || ''}` }), 1000)
        }).catch(() => {
          this.setData({ submitting: false })
          wx.showModal({
            title: '支付未完成',
            content: '秒杀订单已创建，可在订单列表中继续支付。',
            confirmText: '去订单',
            cancelText: '留在这里',
            success: (modalRes) => {
              if (modalRes.confirm) wx.switchTab({ url: '/pages/order/list' })
            },
          })
        })
      }).catch(() => {
        this.setData({ submitting: false })
        wx.showToast({ title: '秒杀下单失败，请重试', icon: 'none' })
      })
      return
    }

    app.request({
      url: '/api/wx/order/create',
      method: 'POST',
      data: {
        cartItemIds: this.data.cartItemIds,
        addressId: this.data.addressId,
        couponId: this.data.selectedCouponId || null
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
      wx.showToast({ title: '订单创建失败，请重试', icon: 'none' });
    });
  },

  payCreatedOrders(orders) {
    if (!orders.length) {
      wx.showToast({ title: '订单创建失败', icon: 'none' });
      this.setData({ submitting: false });
      return;
    }
    let paidCount = 0;
    const payAll = orders.reduce((chain, order) => chain.then(() => this.payOrder(order)
      .then(() => { paidCount += 1; })), Promise.resolve());
    payAll.then(() => {
      const title = orders.length > 1 ? `已支付 ${paidCount} 笔订单` : '支付成功，状态同步中';
      wx.showToast({ title, icon: 'success' });
      setTimeout(() => {
        wx.redirectTo({ url: `/pages/order/success?orderNo=${orders[0] && orders[0].orderNo || ''}` });
      }, 1000);
    }).catch(() => {
      this.setData({ submitting: false });
      const pendingCount = orders.length - paidCount;
      wx.showModal({
        title: '支付未完成',
        content: `已支付 ${paidCount} 笔，仍有 ${pendingCount} 笔订单待支付，可在订单列表中继续完成。`,
        confirmText: '去订单',
        cancelText: '留在这里',
        success: (modalRes) => {
          if (modalRes.confirm) wx.switchTab({ url: '/pages/order/list' });
        },
      });
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
