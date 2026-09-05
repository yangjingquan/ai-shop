import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/BasicLayout.vue'),
    children: [
      {
        path: '',
        redirect: () => {
          const userStore = useUserStore()
          if (userStore.role === 'admin') return '/admin'
          if (userStore.role === 'merchant') return '/merchant'
          return '/login'
        },
      },
      {
        path: 'admin',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { roles: ['admin'] },
      },
      {
        path: 'admin/merchants',
        name: 'AdminMerchantList',
        component: () => import('@/views/admin/MerchantList.vue'),
        meta: { roles: ['admin'] },
      },
      {
        path: 'admin/categories',
        name: 'AdminCategoryList',
        component: () => import('@/views/admin/CategoryList.vue'),
        meta: { roles: ['admin'] },
      },
      {
        path: 'admin/orders',
        name: 'AdminOrderList',
        component: () => import('@/views/admin/OrderList.vue'),
        meta: { roles: ['admin'] },
      },
      {
        path: 'admin/refunds',
        name: 'AdminRefundList',
        component: () => import('@/views/admin/RefundList.vue'),
        meta: { roles: ['admin'] },
      },
      {
        path: 'admin/payments',
        name: 'AdminPaymentList',
        component: () => import('@/views/admin/PaymentList.vue'),
        meta: { roles: ['admin'] },
      },
      {
        path: 'admin/banners',
        name: 'AdminBannerList',
        component: () => import('@/views/admin/BannerList.vue'),
        meta: { roles: ['admin'] },
      },
      {
        path: 'admin/product-audit',
        name: 'AdminProductAudit',
        component: () => import('@/views/admin/ProductAudit.vue'),
        meta: { roles: ['admin'] },
      },
      {
        path: 'admin/op-logs',
        name: 'AdminOpLogList',
        component: () => import('@/views/admin/OpLogList.vue'),
        meta: { roles: ['admin'] },
      },
      {
        path: 'admin/wechat-settings',
        name: 'AdminWechatSettings',
        component: () => import('@/views/admin/WechatSettings.vue'),
        meta: { roles: ['admin'] },
      },
      {
        path: 'admin/profile',
        name: 'AdminProfile',
        component: () => import('@/views/ChangePassword.vue'),
        meta: { roles: ['admin'] },
      },
      {
        path: 'merchant',
        name: 'MerchantDashboard',
        component: () => import('@/views/merchant/Dashboard.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:dashboard:view' },
      },
      {
        path: 'merchant/profile',
        name: 'MerchantProfile',
        component: () => import('@/views/merchant/Profile.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:profile:view' },
      },
      {
        path: 'merchant/password',
        name: 'MerchantPassword',
        component: () => import('@/views/ChangePassword.vue'),
        meta: { roles: ['merchant'] },
      },
      {
        path: 'merchant/categories',
        name: 'MerchantCategoryList',
        component: () => import('@/views/merchant/CategoryList.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:category:view' },
      },
      {
        path: 'merchant/products',
        name: 'MerchantProductList',
        component: () => import('@/views/merchant/ProductList.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:product:view' },
      },
      {
        path: 'merchant/inventory',
        name: 'MerchantInventoryList',
        component: () => import('@/views/merchant/InventoryList.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:inventory:view' },
      },
      {
        path: 'merchant/products/edit/:id?',
        name: 'MerchantProductEdit',
        component: () => import('@/views/merchant/ProductEdit.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:product:view' },
      },
      {
        path: 'merchant/banners',
        name: 'MerchantBannerList',
        component: () => import('@/views/merchant/BannerList.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:banner:view' },
      },
      {
        path: 'merchant/marketing',
        name: 'MerchantMarketingActivity',
        component: () => import('@/views/merchant/MerchantMarketingActivity.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:marketing:view' },
      },
      {
        path: 'merchant/seckill',
        name: 'MerchantSeckill',
        component: () => import('@/views/merchant/SeckillActivity.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:seckill:view' },
      },
      {
        path: 'merchant/referral',
        name: 'MerchantReferral',
        component: () => import('@/views/merchant/MerchantReferralActivity.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:referral:view' },
      },
      {
        path: 'merchant/points',
        name: 'MerchantPoints',
        component: () => import('@/views/merchant/MerchantPoints.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:points:view' },
      },
      {
        path: 'merchant/coupon-templates',
        name: 'MerchantCouponTemplates',
        component: () => import('@/views/merchant/MerchantCouponTemplate.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:coupon:view', couponIssueScene: 'NEW_USER' },
      },
      {
        path: 'merchant/repurchase-coupon-templates',
        name: 'MerchantRepurchaseCouponTemplates',
        component: () => import('@/views/merchant/MerchantCouponTemplate.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:coupon:view', couponIssueScene: 'REPURCHASE_AFTER_PAID' },
      },
      {
        path: 'merchant/order-ship',
        name: 'OrderShip',
        component: () => import('@/views/merchant/OrderShip.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:order:view' },
      },
      {
        path: 'merchant/refund-review',
        name: 'RefundReview',
        component: () => import('@/views/merchant/RefundReview.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:refund:view' },
      },
      {
        path: 'merchant/access-control',
        name: 'MerchantAccessControl',
        component: () => import('@/views/merchant/AccessControl.vue'),
        meta: { roles: ['merchant'], permission: 'merchant:rbac:manage' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

function merchantFallbackPath(userStore: ReturnType<typeof useUserStore>) {
  const layout = routes.find((route) => route.path === '/')
  const fallback = layout?.children?.find((route) => {
    const permission = route.meta?.permission as string | undefined
    return route.path.startsWith('merchant/') && permission && userStore.hasPermission(permission)
  })
  return fallback ? `/${fallback.path}` : '/merchant/password'
}

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  if (to.meta.public) return true
  if (!userStore.token) return { path: '/login', query: { redirect: to.fullPath } }
  await userStore.loadCurrentUser()
  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && requiredRoles.length > 0 && !requiredRoles.includes(userStore.role)) {
    if (userStore.role === 'admin') return { path: '/admin' }
    if (userStore.role === 'merchant') return { path: '/merchant' }
    return { path: '/login' }
  }
  const requiredPermission = to.meta.permission as string | undefined
  if (requiredPermission && !userStore.hasPermission(requiredPermission)) {
    return userStore.role === 'merchant' ? merchantFallbackPath(userStore) : '/admin'
  }
  return true
})

export default router
