# Extractable components

## BasicLayout

- Source: `admin/src/layouts/BasicLayout.vue`
- Category: layout
- Description: Dark merchant navigation rail, top header, user dropdown, and routed content canvas.
- Extractable props: `activeIndex`, `menus`, `currentUserLabel`, `roleLabel`.
- Hardcoded: Shop Suite branding, warm brown palette, menu label typography.

## ImageUploader

- Source: `admin/src/components/upload/ImageUploader.vue`
- Category: basic
- Description: Merchant-scoped image upload input.
- Extractable props: `modelValue`, `scope`, `limit`, `label`.
- Hardcoded: Existing upload affordances and Element Plus treatment.
