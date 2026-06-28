const cartApi = require('../../api/cart')
const { resolveImageUrl } = require('../../utils/url')

const DELETE_ACTION_WIDTH_RPX = 144
const OPEN_THRESHOLD_RATIO = 0.5
const DIRECTION_LOCK_DISTANCE = 8

Page({
  data: {
    items: [],
    selectedIds: [],
    merchantGroups: [],
    totalAmount: '0.00',
    allSelected: false,
    touchStartX: 0,
    touchStartY: 0,
    touchStartSwipeX: 0,
    isHorizontalSwipe: false,
    isVerticalScroll: false,
    swipingId: null,
  },

  onShow() {
    this.loadCart()
  },

  getDeleteActionWidthPx() {
    if (!this.deleteActionWidthPx) {
      const { windowWidth } = wx.getSystemInfoSync()
      this.deleteActionWidthPx = DELETE_ACTION_WIDTH_RPX / 750 * windowWidth
    }
    return this.deleteActionWidthPx
  },

  resetTouchState() {
    this.setData({
      touchStartX: 0,
      touchStartY: 0,
      touchStartSwipeX: 0,
      isHorizontalSwipe: false,
      isVerticalScroll: false,
      swipingId: null,
    })
  },

  loadCart() {
    cartApi.list().then(res => {
      if (res.code === 0) {
        const items = (res.data || []).map(item => ({
          ...item,
          id: Number(item.id),
          merchantId: Number(item.merchantId),
          quantity: Number(item.quantity || 1),
          unitPrice: Number(item.unitPrice || 0),
          stock: Number(item.stock || 0),
          available: item.available !== false,
          selected: this.data.selectedIds.includes(Number(item.id)),
          swipeX: 0,
          mainImage: resolveImageUrl(item.mainImage || '')
        }))
        this.updateCartState(items, this.data.selectedIds)
      }
    })
  },

  updateCartState(items, selectedIds) {
    const validIds = items.filter(i => i.available !== false).map(i => i.id)
    const nextSelectedIds = selectedIds.filter(id => validIds.includes(Number(id))).map(Number)
    const selectedSet = new Set(nextSelectedIds)
    const groupMap = {}
    let total = 0

    items.forEach((sourceItem) => {
      const item = {
        ...sourceItem,
        selected: selectedSet.has(sourceItem.id),
      }
      if (!groupMap[item.merchantId]) {
        groupMap[item.merchantId] = {
          merchantId: item.merchantId,
          merchantName: item.merchantName,
          items: []
        }
      }
      groupMap[item.merchantId].items.push(item)
      if (nextSelectedIds.includes(item.id) && item.available !== false) {
        total += Number(item.unitPrice || 0) * Number(item.quantity || 0)
      }
    })

    this.setData({
      items: Object.values(groupMap).flatMap(group => group.items),
      merchantGroups: Object.values(groupMap),
      selectedIds: nextSelectedIds,
      allSelected: nextSelectedIds.length === validIds.length && validIds.length > 0,
      totalAmount: total.toFixed(2),
    })
  },

  closeSwipeItems(exceptId) {
    const items = this.data.items.map(item => ({
      ...item,
      swipeX: exceptId && item.id === exceptId ? item.swipeX : 0,
    }))
    this.updateCartState(items, this.data.selectedIds)
  },

  toggleSelect(e) {
    const cartItemId = Number(e.currentTarget.dataset.id)
    const item = this.data.items.find(i => i.id === cartItemId)
    if (!item || item.available === false) return

    const checked = e.currentTarget.dataset.checked === 'true'
    const selectedIds = [...this.data.selectedIds]
    const idx = selectedIds.indexOf(cartItemId)
    if (checked && idx > -1) {
      selectedIds.splice(idx, 1)
    } else if (!checked && idx === -1) {
      selectedIds.push(cartItemId)
    }
    this.closeSwipeItems()
    this.updateCartState(this.data.items, selectedIds)
  },

  selectAll() {
    const validIds = this.data.items.filter(i => i.available !== false).map(i => i.id)
    const checked = this.data.allSelected
    const selectedIds = checked ? [] : validIds
    this.closeSwipeItems()
    this.updateCartState(this.data.items, selectedIds)
  },

  onQtyMinus(e) {
    const id = Number(e.currentTarget.dataset.id)
    const item = this.data.items.find(i => i.id === id)
    if (!item || item.quantity <= 1) return
    this.updateQuantity(id, item.quantity - 1)
  },

  onQtyPlus(e) {
    const id = Number(e.currentTarget.dataset.id)
    const item = this.data.items.find(i => i.id === id)
    if (!item) return
    if (item.stock && item.quantity >= item.stock) {
      wx.showToast({ title: '库存不足', icon: 'none' })
      return
    }
    this.updateQuantity(id, item.quantity + 1)
  },

  updateQuantity(id, quantity) {
    cartApi.update(id, { quantity }).then(res => {
      if (res.code === 0) {
        const items = this.data.items.map(item => item.id === id ? { ...item, quantity } : item)
        this.updateCartState(items, this.data.selectedIds)
      }
    })
  },

  onItemTouchStart(e) {
    const id = Number(e.currentTarget.dataset.id)
    const item = this.data.items.find(i => i.id === id)
    this.setData({
      touchStartX: e.touches[0].clientX,
      touchStartY: e.touches[0].clientY,
      touchStartSwipeX: item ? Number(item.swipeX || 0) : 0,
      isHorizontalSwipe: false,
      isVerticalScroll: false,
      swipingId: id,
    })
  },

  onItemTouchMove(e) {
    const id = Number(e.currentTarget.dataset.id)
    const touch = e.touches[0]
    const diffX = touch.clientX - this.data.touchStartX
    const diffY = touch.clientY - this.data.touchStartY

    if (!this.data.swipingId || this.data.isVerticalScroll) return

    if (!this.data.isHorizontalSwipe) {
      if (Math.abs(diffX) < DIRECTION_LOCK_DISTANCE && Math.abs(diffY) < DIRECTION_LOCK_DISTANCE) return
      if (Math.abs(diffY) > Math.abs(diffX)) {
        this.setData({ isVerticalScroll: true })
        return
      }
      this.setData({ isHorizontalSwipe: true })
    }

    const actionWidth = this.getDeleteActionWidthPx()
    const swipeX = Math.min(0, Math.max(this.data.touchStartSwipeX + diffX, -actionWidth))
    const items = this.data.items.map(item => ({
      ...item,
      swipeX: item.id === id ? swipeX : 0,
    }))
    this.updateCartState(items, this.data.selectedIds)
  },

  onItemTouchEnd(e) {
    const id = Number(e.currentTarget.dataset.id)
    if (!this.data.swipingId) return

    if (this.data.isVerticalScroll) {
      this.resetTouchState()
      return
    }

    const item = this.data.items.find(i => i.id === id)
    const actionWidth = this.getDeleteActionWidthPx()
    const shouldOpen = item && Math.abs(Number(item.swipeX || 0)) > actionWidth * OPEN_THRESHOLD_RATIO
    const items = this.data.items.map(cartItem => ({
      ...cartItem,
      swipeX: shouldOpen && cartItem.id === id ? -actionWidth : 0,
    }))
    this.updateCartState(items, this.data.selectedIds)
    this.resetTouchState()
  },

  onItemTouchCancel(e) {
    this.onItemTouchEnd(e)
  },

  deleteItem(e) {
    const id = Number(e.currentTarget.dataset.id)
    wx.showModal({
      title: '确认删除',
      content: '确定要删除该商品吗？',
      success: (modalRes) => {
        if (modalRes.confirm) {
          cartApi.remove(id).then(res => {
            if (res.code === 0) {
              const items = this.data.items.filter(item => item.id !== id)
              const selectedIds = this.data.selectedIds.filter(sid => sid !== id)
              this.updateCartState(items, selectedIds)
              this.resetTouchState()
            }
          })
        } else {
          this.closeSwipeItems()
        }
      }
    })
  },

  goCheckout() {
    if (this.data.selectedIds.length === 0) {
      wx.showToast({ title: '请选择商品', icon: 'none' })
      return
    }
    const selectedItems = this.data.items.filter(i => this.data.selectedIds.includes(i.id))
    const merchantIds = new Set(selectedItems.map(i => i.merchantId))
    if (merchantIds.size > 1) {
      wx.showToast({ title: '不支持跨商家下单', icon: 'none' })
      return
    }
    const idsParam = this.data.selectedIds.join(',')
    wx.navigateTo({ url: '/pages/order/confirm?cartItemIds=' + idsParam })
  }
})
