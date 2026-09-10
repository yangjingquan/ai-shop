# Key page dependency trees

## `/merchant/profile` (Store profile)

Entry: `admin/src/views/merchant/Profile.vue`

- `admin/src/views/merchant/Profile.vue`
  - `admin/src/api/profile.ts`
  - `admin/src/stores/user.ts`
  - `admin/src/components/upload/ImageUploader.vue`
  - Element Plus `el-card`, `el-form`, `el-input`, `el-button`
  - Global styles: `admin/src/style.css`
  - Shell: `admin/src/layouts/BasicLayout.vue`

## `/merchant` (Merchant dashboard)

Entry: `admin/src/views/merchant/Dashboard.vue`

- `admin/src/views/merchant/Dashboard.vue`
  - `admin/src/api/dashboard.ts`
  - Vue Router
  - Element Plus cards, tags, table, alert, button
  - Global styles: `admin/src/style.css`
  - Shell: `admin/src/layouts/BasicLayout.vue`
