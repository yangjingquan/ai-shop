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
        path: 'merchant',
        name: 'MerchantDashboard',
        component: () => import('@/views/merchant/Dashboard.vue'),
        meta: { roles: ['merchant'] },
      },
      {
        path: 'merchant/profile',
        name: 'MerchantProfile',
        component: () => import('@/views/merchant/Profile.vue'),
        meta: { roles: ['merchant'] },
      },
      {
        path: 'merchant/products',
        name: 'MerchantProductList',
        component: () => import('@/views/merchant/ProductList.vue'),
        meta: { roles: ['merchant'] },
      },
      {
        path: 'merchant/products/edit/:id?',
        name: 'MerchantProductEdit',
        component: () => import('@/views/merchant/ProductEdit.vue'),
        meta: { roles: ['merchant'] },
      },
      {
        path: 'merchant/banners',
        name: 'MerchantBannerList',
        component: () => import('@/views/merchant/BannerList.vue'),
        meta: { roles: ['merchant'] },
      },
      {
        path: 'merchant/order-ship',
        name: 'OrderShip',
        component: () => import('@/views/merchant/OrderShip.vue'),
        meta: { roles: ['merchant'] },
      },
      {
        path: 'merchant/refund-review',
        name: 'RefundReview',
        component: () => import('@/views/merchant/RefundReview.vue'),
        meta: { roles: ['merchant'] },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.public) return true
  if (!userStore.token) return { path: '/login', query: { redirect: to.fullPath } }
  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && requiredRoles.length > 0 && !requiredRoles.includes(userStore.role)) {
    if (userStore.role === 'admin') return { path: '/admin' }
    if (userStore.role === 'merchant') return { path: '/merchant' }
    return { path: '/login' }
  }
  return true
})

export default router
