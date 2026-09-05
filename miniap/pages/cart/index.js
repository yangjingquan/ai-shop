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
    itemCount: 0,
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
    this.promotionActivitiesPromise = null
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
            bundleGroupId: item.bundleGroupId || '',
            bundleActivityId: Number(item.bundleActivityId || 0),
            bundleItemIds: (item.bundleItemIds || []).map(Number),
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
    const selectableIds = this.getSelectableIds(items)
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
      itemCount: items.reduce((count, item) => count + Number(item.quantity || 0), 0),
    })
    this.loadPromotion(nextSelectedIds)
  },

  getSelectableIds(items) {
    if (this.data.manageMode) return items.map(item => item.id)
    const unavailableBundleGroups = new Set(items
      .filter(item => item.bundleGroupId && item.available === false)
      .map(item => item.bundleGroupId))
    return items
      .filter(item => item.available !== false
        && (!item.bundleGroupId || !unavailableBundleGroups.has(item.bundleGroupId)))
      .map(item => item.id)
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
      const threshold = Number(promotion.thresholdAmount || 0)
      this.setData({ promotion: {
        ...promotion,
        discountText: this.formatPrice(promotion.discountAmount),
        qualifiedText: this.formatPrice(promotion.qualifiedAmount),
        remainingText: this.formatPrice(promotion.remainingAmount),
        thresholdText: promotion.thresholdAmount ? this.formatPrice(promotion.thresholdAmount) : '',
        progressPercent: threshold > 0 ? Math.min(100, Math.max(0, Number(promotion.qualifiedAmount || 0) / threshold * 100)) : 0,
        ruleText: '',
        achieved,
      }, promotionRecommendList: [] })
      this.loadPromotionRule(promotion, requestId)
      if (!achieved) this.loadPromotionRecommendations(promotion.recommendProductIds || [], requestId)
    }).catch(() => {
      if (requestId === this.promotionRequestId) this.setData({ promotion: null, promotionRecommendList: [] })
    })
  },

  loadPromotionRule(promotion, requestId) {
    // 同一次进入购物车共享配置请求；切换选中商品时只更新当前活动的阶梯文案。
    if (!this.promotionActivitiesPromise) {
      this.promotionActivitiesPromise = promotionApi.active().then(res => Array.isArray(res.data) ? res.data : []).catch(() => [])
    }
    this.promotionActivitiesPromise.then((activities) => {
      if (requestId !== this.promotionRequestId || !this.data.promotion) return
      const activity = activities.find(item => Number(item.id) === Number(promotion.activityId))
      const tier = activity && (activity.thresholds || []).find(item => Number(item.thresholdAmount) === Number(promotion.thresholdAmount))
      if (!tier) return
      const threshold = Number(tier.thresholdAmount)
      let ruleText = ''
      if (activity.activityType === 'FULL_DISCOUNT' && Number(tier.discountRate) > 0) {
        ruleText = `满 ${threshold} 元享 ${Number(tier.discountRate)} 折`
        if (Number(tier.discountCap) > 0) ruleText += `，最高优惠 ${Number(tier.discountCap)} 元`
      } else if (activity.activityType === 'FULL_REDUCTION' && Number(tier.reductionAmount) > 0) {
        ruleText = `满 ${threshold} 元减 ${Number(tier.reductionAmount)} 元`
      }
      this.setData({ 'promotion.ruleText': ruleText })
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
    const bundleIds = item.bundleGroupId
      ? this.data.items.filter(i => i.bundleGroupId === item.bundleGroupId).map(i => i.id)
      : [cartItemId]
    if (!this.data.manageMode && item.bundleGroupId
      && bundleIds.some(id => this.data.items.find(i => i.id === id)?.available === false)) {
      wx.showToast({ title: '套餐内有商品暂不可购买', icon: 'none' })
      return
    }
    const isSelected = bundleIds.every(id => selectedIds.includes(id))
    const next = new Set(selectedIds)
    bundleIds.forEach(id => isSelected ? next.delete(id) : next.add(id))
    this.closeSwipeItems()
    this.updateCartState(this.data.items, [...next])
  },

  selectAll() {
    const selectableIds = this.getSelectableIds(this.data.items)
    const checked = this.data.allSelected
    const selectedIds = checked ? [] : selectableIds
    this.closeSwipeItems()
    this.updateCartState(this.data.items, selectedIds)
  },

  onQtyMinus(e) {
    const id = Number(e.currentTarget.dataset.id)
    const item = this.data.items.find(i => i.id === id)
    if (!item || item.bundleGroupId || item.quantity <= 1) return
    this.updateQuantity(id, item.quantity - 1)
  },

  onQtyPlus(e) {
    const id = Number(e.currentTarget.dataset.id)
    const item = this.data.items.find(i => i.id === id)
    if (!item || item.bundleGroupId) return
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
    const current = this.data.items.find(item => item.id === id)
    const ids = current && current.bundleGroupId
      ? this.data.items.filter(item => item.bundleGroupId === current.bundleGroupId).map(item => item.id)
      : [id]
    wx.showModal({
      title: '确认删除商品？',
      content: '删除后可在商品详情页重新加入购物车。',
      cancelText: '取消',
      confirmText: '确认删除',
      confirmColor: '#ff4b43',
      success: (modalRes) => {
        if (modalRes.confirm) {
          const request = ids.length > 1 ? cartApi.batchRemove(ids) : cartApi.remove(id)
          request.then(res => {
            if (res.code === 0) {
              const idSet = new Set(ids)
              const items = this.data.items.filter(item => !idSet.has(item.id))
              const selectedIds = this.data.selectedIds.filter(sid => !idSet.has(sid))
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
    const bundleGroups = [...new Set(selectedItems.map(item => item.bundleGroupId).filter(Boolean))]
    if (bundleGroups.length > 0) {
      if (bundleGroups.length !== 1 || selectedItems.some(item => !item.bundleGroupId)) {
        wx.showToast({ title: '套餐需单独结算', icon: 'none' })
        return
      }
      const group = selectedItems.find(item => item.bundleGroupId === bundleGroups[0])
      wx.navigateTo({ url: `/pages/order/confirm?mode=bundle&bundleGroupId=${bundleGroups[0]}&cartItemIds=${(group.bundleItemIds || this.data.selectedIds).join(',')}` })
      return
    }
    const idsParam = this.data.selectedIds.join(',')
    wx.navigateTo({ url: '/pages/order/confirm?cartItemIds=' + idsParam })
  }
})
