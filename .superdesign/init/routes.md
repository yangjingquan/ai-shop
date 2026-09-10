# Routes

Framework: Vue Router (history mode), with `BasicLayout.vue` as the authenticated shell.

## Merchant routes

- `/merchant` → `admin/src/views/merchant/Dashboard.vue`
- `/merchant/profile` → `admin/src/views/merchant/Profile.vue`
- `/merchant/password` → `admin/src/views/ChangePassword.vue`
- `/merchant/access-control` → `admin/src/views/merchant/AccessControl.vue`
- Product, inventory, marketing, order, refund, settlement routes also render within the same shell.

Router source: `admin/src/router/index.ts`
