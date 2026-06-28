const app = getApp();
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    order: null
  },

  onLoad(options) {
    this.loadDetail(options.orderNo);
  },

  loadDetail(orderNo) {
    app.request({
      url: '/api/wx/order/' + orderNo,
      method: 'GET'
    }).then(res => {
      if (res.code === 0) {
        const order = {
          ...res.data,
          items: (res.data.items || []).map(item => ({
            ...item,
            mainImage: resolveImageUrl(item.mainImage || '')
          }))
        };
        this.setData({ order });
      }
    });
  },

  mockPay() {
    const orderNo = this.data.order.orderNo;
    app.request({
      url: '/api/wx/order/' + orderNo + '/mock-pay',
      method: 'POST'
    }).then(res => {
      if (res.code === 0) {
        wx.showToast({ title: '支付成功', icon: 'success' });
        this.loadDetail(orderNo);
      } else {
        wx.showToast({ title: res.msg, icon: 'none' });
      }
    });
  },

  cancelOrder() {
    wx.showModal({
      title: '取消订单',
      content: '确定要取消该订单吗？',
      success: (modalRes) => {
        if (modalRes.confirm) {
          app.request({
            url: '/api/wx/order/' + this.data.order.orderNo + '/cancel',
            method: 'POST'
          }).then(res => {
            if (res.code === 0) {
              wx.showToast({ title: '已取消', icon: 'success' });
              this.loadDetail(this.data.order.orderNo);
            } else {
              wx.showToast({ title: res.msg, icon: 'none' });
            }
          });
        }
      }
    });
  }
});
