const cartApi = require('../../api/cart')
const productApi = require('../../api/product')
const { resolveImageUrl } = require('../../utils/url')
const promotionApi = require('../../api/promotion')

const DELETE_ACTION_WIDTH_RPX = 144
const OPEN_THRESHOLD_RATIO = 0.5
const DIRECTION_LOCK_DISTANCE = 8
const RECOMMEND_PAGE_SIZE = 10

const UNAVAILABLE_REASON_TEXT = {
  OFF_SHELF: '商品已下架',
  SKU_GONE: '规格已失效',
  MERCHANT_MISMATCH: '商品归属已变化',
  STOCK_NOT_ENOUGH: '库存不足',
}

Page({
  data: {
    items: [],
    selectedIds: [],
    merchantGroups: [],
    totalAmount: '0.00',
    allSelected: false,
    manageMode: false,
    recommendList: [],
    recommendPage: 1,
    recommendLoading: false,
    promotionRecommendList: [],
    promotion: null,
    touchStartX: 0,
    touchStartY: 0,
    touchStartSwipeX: 0,
    isHorizontalSwipe: false,
    isVerticalScroll: false,
    swipingId: null,
  },

  onShow() {
    this.loadCart()
    if (!this.data.recommendList.length) {
      this.loadRecommend(true)
    }
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

  formatPrice(value) {
    return Number(value || 0).toFixed(2)
  },

  getUnavailableText(reason) {
    if (!reason) return '暂不可购买'
    return UNAVAILABLE_REASON_TEXT[reason] || reason
  },

  normalizeRecommendData(data) {
    const list = (data && (data.records || data.list || data.items)) || data || []
    if (!Array.isArray(list)) return []
    return list.map(item => ({
      ...item,
      id: Number(item.id),
      name: item.name || item.productName || '',
      priceText: this.formatPrice(item.price || item.minPrice || item.salePrice || 0),
      mainImage: resolveImageUrl(item.mainImage || item.coverImage || ''),
    }))
  },

  loadCart() {
    cartApi.list().then(res => {
      if (res.code === 0) {
        const items = (res.data || []).map(item => {
          const unitPrice = Number(item.unitPrice || 0)
          return {
            ...item,
            id: Number(item.id),
            merchantId: Number(item.merchantId),
            productId: Number(item.productId),
            skuId: Number(item.skuId),
            quantity: Number(item.quantity || 1),
            unitPrice,
            unitPriceText: this.formatPrice(unitPrice),
            stock: Number(item.stock || 0),
            available: item.available !== false,
            unavailableText: this.getUnavailableText(item.unavailableReason),
            selected: this.data.selectedIds.includes(Number(item.id)),
            swipeX: 0,
            mainImage: resolveImageUrl(item.mainImage || '')
          }
        })
        this.updateCartState(items, this.data.selectedIds)
      }
    })
  },

  updateCartState(items, selectedIds) {
    const selectableIds = this.data.manageMode
      ? items.map(i => i.id)
      : items.filter(i => i.available !== false).map(i => i.id)
    const selectableSet = new Set(selectableIds)
    const nextSelectedIds = selectedIds
      .map(Number)
      .filter(id => selectableSet.has(id))
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
      if (selectedSet.has(item.id) && item.available !== false) {
        total += Number(item.unitPrice || 0) * Number(item.quantity || 0)
      }
    })

    this.setData({
      items: Object.values(groupMap).flatMap(group => group.items),
      merchantGroups: Object.values(groupMap),
      selectedIds: nextSelectedIds,
      allSelected: nextSelectedIds.length === selectableIds.length && selectableIds.length > 0,
      totalAmount: total.toFixed(2),
    })
    this.loadPromotion(nextSelectedIds)
  },

  loadPromotion(selectedIds) {
    const requestId = (this.promotionRequestId || 0) + 1
    this.promotionRequestId = requestId
    if (this.data.manageMode || !selectedIds.length) {
      this.setData({ promotion: null, promotionRecommendList: [] })
      return
    }
    promotionApi.cartProgress(selectedIds).then((res) => {
      if (requestId !== this.promotionRequestId || res.code !== 0) return
      const promotion = res.data || null
      if (!promotion || !promotion.activityId) return this.setData({ promotion: null, promotionRecommendList: [] })
      const achieved = Number(promotion.discountAmount || 0) > 0
      this.setData({ promotion: {
        ...promotion,
        discountText: this.formatPrice(promotion.discountAmount),
        qualifiedText: this.formatPrice(promotion.qualifiedAmount),
        remainingText: this.formatPrice(promotion.remainingAmount),
        thresholdText: promotion.thresholdAmount ? this.formatPrice(promotion.thresholdAmount) : '',
        achieved,
      }, promotionRecommendList: [] })
      if (!achieved) this.loadPromotionRecommendations(promotion.recommendProductIds || [], requestId)
    }).catch(() => {
      if (requestId === this.promotionRequestId) this.setData({ promotion: null, promotionRecommendList: [] })
    })
  },

  loadPromotionRecommendations(productIds, requestId) {
    if (!productIds.length) return this.setData({ promotionRecommendList: [] })
    Promise.all(productIds.slice(0, 4).map(id => productApi.get(id).catch(() => null))).then((responses) => {
      if (requestId !== this.promotionRequestId) return
      const products = responses.map(res => res && res.data).filter(Boolean)
      this.setData({ promotionRecommendList: this.normalizeRecommendData(products) })
    })
  },

  closeSwipeItems(exceptId) {
    const items = this.data.items.map(item => ({
      ...item,
      swipeX: exceptId !== undefined && item.id === exceptId ? item.swipeX : 0,
    }))
    this.updateCartState(items, this.data.selectedIds)
  },

  toggleManageMode() {
    const manageMode = !this.data.manageMode
    const items = this.data.items.map(item => ({ ...item, swipeX: 0 }))
    this.setData({ manageMode }, () => {
      this.updateCartState(items, this.data.selectedIds)
      this.resetTouchState()
    })
  },

  toggleSelect(e) {
    const cartItemId = Number(e.currentTarget.dataset.id)
    const item = this.data.items.find(i => i.id === cartItemId)
    if (!item || (!this.data.manageMode && item.available === false)) return

    const selectedIds = [...this.data.selectedIds]
    const idx = selectedIds.indexOf(cartItemId)
    if (idx > -1) {
      selectedIds.splice(idx, 1)
    } else {
      selectedIds.push(cartItemId)
    }
    this.closeSwipeItems()
    this.updateCartState(this.data.items, selectedIds)
  },

  selectAll() {
    const selectableIds = this.data.manageMode
      ? this.data.items.map(i => i.id)
      : this.data.items.filter(i => i.available !== false).map(i => i.id)
    const checked = this.data.allSelected
    const selectedIds = checked ? [] : selectableIds
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
      title: '确认删除商品？',
      content: '删除后可在商品详情页重新加入购物车。',
      cancelText: '取消',
      confirmText: '确认删除',
      confirmColor: '#ff4b43',
      success: (modalRes) => {
        if (modalRes.confirm) {
          cartApi.remove(id).then(res => {
            if (res.code === 0) {
              const items = this.data.items.filter(item => item.id !== id)
              const selectedIds = this.data.selectedIds.filter(sid => sid !== id)
              this.updateCartState(items, selectedIds)
              this.resetTouchState()
              wx.showToast({ title: '已删除', icon: 'success' })
            }
          })
        } else {
          this.closeSwipeItems()
        }
      }
    })
  },

  batchDeleteSelected() {
    const ids = this.data.selectedIds.map(Number)
    if (!ids.length) {
      wx.showToast({ title: '请选择商品', icon: 'none' })
      return
    }

    wx.showModal({
      title: '确认删除选中商品？',
      content: `将删除已选的 ${ids.length} 件商品，删除后可在商品详情页重新加入购物车。`,
      cancelText: '取消',
      confirmText: '确认删除',
      confirmColor: '#ff4b43',
      success: (modalRes) => {
        if (!modalRes.confirm) return
        cartApi.batchRemove(ids).then(res => {
          if (res.code === 0) {
            const idSet = new Set(ids)
            const items = this.data.items.filter(item => !idSet.has(item.id))
            this.setData({ manageMode: false }, () => {
              this.updateCartState(items, [])
              this.resetTouchState()
              wx.showToast({ title: '已删除', icon: 'success' })
            })
          }
        })
      }
    })
  },

  loadRecommend(reset) {
    if (this.data.recommendLoading) return
    const page = reset ? 1 : this.data.recommendPage

    this.setData({ recommendLoading: true })
    productApi.page({
      isRecommend: 1,
      page,
      size: RECOMMEND_PAGE_SIZE,
    }).then(res => {
      if (res.code !== 0) {
        this.setData({ recommendLoading: false })
        return
      }
      const list = this.normalizeRecommendData(res.data)
      if (!list.length && page > 1) {
        this.setData({ recommendPage: 1, recommendLoading: false }, () => {
          this.loadRecommend(true)
        })
        return
      }
      this.setData({
        recommendList: list,
        recommendPage: page + 1,
        recommendLoading: false,
      })
    }).catch(() => {
      this.setData({ recommendLoading: false })
    })
  },

  changeRecommend() {
    this.loadRecommend(false)
  },

  goProductDetail(e) {
    const id = Number(e.currentTarget.dataset.id || e.currentTarget.dataset.productId)
    if (!id) return
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  },

  goCategory() {
    wx.switchTab({ url: '/pages/category/index' })
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
