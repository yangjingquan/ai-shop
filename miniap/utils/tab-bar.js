function syncTabBar(page, selected) {
  const tabBar = page.getTabBar && page.getTabBar()
  if (tabBar && typeof tabBar.setSelected === 'function') tabBar.setSelected(selected)
}

module.exports = { syncTabBar }
