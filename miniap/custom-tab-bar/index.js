const tabs = [
  { pagePath: 'pages/home/index', text: '首页', iconPath: '/assets/tabbar/home.png', selectedIconPath: '/assets/tabbar/home-active.png' },
  { pagePath: 'pages/category/index', text: '分类', iconPath: '/assets/tabbar/category.png', selectedIconPath: '/assets/tabbar/category-active.png' },
  { pagePath: 'pages/cart/index', text: '购物车', iconPath: '/assets/tabbar/cart.png', selectedIconPath: '/assets/tabbar/cart-active.png' },
  { pagePath: 'pages/order/list', text: '订单', iconPath: '/assets/tabbar/order.png', selectedIconPath: '/assets/tabbar/order-active.png' },
  { pagePath: 'pages/my/index', text: '我的', iconPath: '/assets/tabbar/user.png', selectedIconPath: '/assets/tabbar/user-active.png' },
]

Component({
  data: {
    selected: 0,
    tabs,
  },
  lifetimes: {
    attached() {
      this.syncSelected()
    },
  },
  pageLifetimes: {
    show() {
      this.syncSelected()
    },
  },
  methods: {
    syncSelected() {
      const pages = getCurrentPages()
      const current = pages[pages.length - 1]
      const index = tabs.findIndex((item) => item.pagePath === current?.route)
      if (index >= 0 && index !== this.data.selected) this.setData({ selected: index })
    },
    switchTab(event) {
      const index = Number(event.currentTarget.dataset.index)
      const target = tabs[index]
      if (!target || index === this.data.selected) return
      this.setData({ selected: index })
      wx.switchTab({ url: `/${target.pagePath}` })
    },
  },
})
