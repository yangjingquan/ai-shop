const app = getApp();
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    orders: [],
    currentStatus: null, // null=全部, 0=待支付, 1=待发货, ...
    page: 1,
    hasMore: true
  },

  onShow() {
    this.setData({ page: 1, orders: [], hasMore: true });
    this.loadOrders();
  },

  loadOrders() {
    const params = {
      page: this.data.page,
      size: 10
    };
    if (this.data.currentStatus !== null && this.data.currentStatus !== undefined) {
      params.status = this.data.currentStatus;
    }

    app.request({
      url: '/api/wx/order/page',
      method: 'GET',
      data: params
    }).then(res => {
      if (res.code === 0) {
        const list = (res.data.list || []).map(item => ({
          ...item,
          firstItemImage: resolveImageUrl(item.firstItemImage || '')
        }));
        const newOrders = this.data.page === 1 ? list : [...this.data.orders, ...list];
        this.setData({
          orders: newOrders,
          hasMore: list.length === 10
        });
      }
    });
  },

  switchTab(e) {
    const rawStatus = e.currentTarget.dataset.status;
    const status = rawStatus === 'all' || rawStatus === undefined || rawStatus === '' ? null : Number(rawStatus);
    this.setData({ currentStatus: status, page: 1, orders: [], hasMore: true });
    this.loadOrders();
  },

  goDetail(e) {
    const orderNo = e.currentTarget.dataset.orderno;
    wx.navigateTo({ url: '/pages/order/detail?orderNo=' + orderNo });
  },

  onReachBottom() {
    if (this.data.hasMore) {
      this.setData({ page: this.data.page + 1 });
      this.loadOrders();
    }
  }
});
