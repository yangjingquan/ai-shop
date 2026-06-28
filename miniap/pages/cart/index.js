const app = getApp();
const cartApi = require('../../api/cart')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    items: [],
    selectedIds: [],
    merchantGroups: [],
    totalAmount: '0.00',
    allSelected: false
  },

  onShow() {
    this.loadCart();
  },

  loadCart() {
    cartApi.list().then(res => {
      if (res.code === 0) {
        const items = (res.data || []).map(item => ({
          ...item,
          mainImage: resolveImageUrl(item.mainImage || '')
        }));
        // 按 merchant 分组
        const groupMap = {};
        items.forEach(item => {
          const key = item.merchantId;
          if (!groupMap[key]) {
            groupMap[key] = {
              merchantId: item.merchantId,
              merchantName: item.merchantName,
              items: []
            };
          }
          groupMap[key].items.push(item);
        });
        const groups = Object.values(groupMap);

        // 清理无效的 selectedIds
        const validIds = items.filter(i => i.available).map(i => i.id);
        const selectedIds = this.data.selectedIds.filter(id => validIds.includes(id));

        this.setData({
          items,
          merchantGroups: groups,
          selectedIds,
          allSelected: selectedIds.length === validIds.length && validIds.length > 0
        });
        this.calcTotal();
      }
    });
  },

  toggleSelect(e) {
    const cartItemId = Number(e.currentTarget.dataset.id);
    let selectedIds = [...this.data.selectedIds];
    const idx = selectedIds.indexOf(cartItemId);
    if (idx > -1) {
      selectedIds.splice(idx, 1);
    } else {
      selectedIds.push(cartItemId);
    }
    const validIds = this.data.items.filter(i => i.available).map(i => i.id);
    this.setData({
      selectedIds,
      allSelected: selectedIds.length === validIds.length && validIds.length > 0
    });
    this.calcTotal();
  },

  selectAll() {
    if (this.data.allSelected) {
      this.setData({ selectedIds: [], allSelected: false });
    } else {
      const validIds = this.data.items.filter(i => i.available).map(i => i.id);
      this.setData({ selectedIds: validIds, allSelected: true });
    }
    this.calcTotal();
  },

  calcTotal() {
    let total = 0;
    this.data.items.forEach(item => {
      if (this.data.selectedIds.includes(item.id) && item.available) {
        total += item.unitPrice * item.quantity;
      }
    });
    this.setData({ totalAmount: total.toFixed(2) });
  },

  changeQuantity(e) {
    const cartItemId = parseInt(e.currentTarget.dataset.id);
    const quantity = parseInt(e.detail.value);
    if (quantity <= 0) {
      this.deleteItem(null, cartItemId);
      return;
    }
    cartApi.update(cartItemId, { quantity }).then(res => {
      if (res.code === 0) {
        this.loadCart();
      }
    });
  },

  deleteItem(e, presetId) {
    const id = Number(presetId || e.currentTarget.dataset.id);
    wx.showModal({
      title: '确认删除',
      content: '确定要删除该商品吗？',
      success: (modalRes) => {
        if (modalRes.confirm) {
          cartApi.remove(id).then(res => {
            if (res.code === 0) {
              let selectedIds = this.data.selectedIds.filter(sid => sid !== id);
              this.setData({ selectedIds });
              this.loadCart();
            }
          });
        }
      }
    });
  },

  goCheckout() {
    if (this.data.selectedIds.length === 0) {
      wx.showToast({ title: '请选择商品', icon: 'none' });
      return;
    }
    // 跨商家互斥：只允许同一商家
    const selectedItems = this.data.items.filter(i => this.data.selectedIds.includes(i.id));
    const merchantIds = new Set(selectedItems.map(i => i.merchantId));
    if (merchantIds.size > 1) {
      wx.showToast({ title: '不支持跨商家下单', icon: 'none' });
      return;
    }
    const idsParam = this.data.selectedIds.join(',');
    wx.navigateTo({ url: '/pages/order/confirm?cartItemIds=' + idsParam });
  }
});
