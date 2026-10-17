# Mini program page layout audit (39 routes)

**Global:** app.wxss button reset (margin/padding/width:auto/transparent), .btn-block / .btn-inline utilities.

| Route | Mock HTML | Buttons (unscoped) | Fix notes |
|-------|-----------|----------------------|----------|
| `pages/home/index` | `design/src/home.html` | 1 (1) | Global button reset; tab bar padding (prior pass). Popup claim-btn keeps btn-block width. |
| `pages/topic/detail` | — | 2 (0) | Prior mockup restyle; buttons use .btn utilities. |
| `pages/recommend/index` | — | 0 (0) | Prior hero restyle; no action buttons. |
| `pages/category/index` | `design/src/category.html` | 0 (0) | Tab bar padding; removed warm override block. |
| `pages/cart/index` | `design/src/cart.html` | 5 (3) | Hero + checkout above tab bar; mini buttons width:auto in page wxss. |
| `pages/order/list` | — | 1 (1) | Tab padding; empty-btn width:auto. |
| `pages/my/index` | `design/src/my.html` | 2 (1) | Hero layout; gradient service tiles (88rpx); points card overlap. |
| `pages/address/list` | `design/batch2/src/address-list.html` | 1 (0) | btn-block on 新增地址; flex bottom bar. |
| `pages/address/edit` | `design/batch2/src/address-edit.html` | 1 (0) | btn-block submit; full-width flex. |
| `pages/product/detail` | — | 6 (6) | Bottom bar buttons explicitly width:100% in grid (unchanged). |
| `pages/order/confirm` | — | 2 (0) | btn utilities on submit. |
| `pages/coupon/list` | — | 0 (0) | Prior restyle. |
| `pages/activity/referral/index` | — | 4 (0) | btn-lg block buttons. |
| `pages/activity/referral/rewards` | — | 1 (0) | btn-sm inline. |
| `pages/activity/lottery/index` | — | 2 (0) | btn-lg draw button. |
| `pages/activity/lottery/result` | — | 2 (0) | btn-lg actions. |
| `pages/activity/lottery/rewards` | — | 5 (0) | btn-sm per row. |
| `pages/activity/lottery/records` | — | 2 (0) | btn footer. |
| `pages/order/success` | — | 6 (6) | Custom order-btn widths in page wxss (full/intent). |
| `pages/order/detail` | — | 1 (1) | Share button uses .btn.dark. |
| `pages/points/index` | — | 1 (0) | Sign-in btn-sm. |
| `pages/points/mall` | `design/batch2/src/points-mall.html` | 1 (0) | Grid redeem btn-sm. |
| `pages/points/records/index` | — | 0 (0) | Prior restyle. |
| `pages/points/member-day` | — | 2 (0) | btn-lg CTA. |
| `pages/group-buy/list` | — | 0 (0) | Prior restyle. |
| `pages/group-buy/group` | — | 4 (4) | primary-btn/ghost-btn width rules in page wxss. |
| `pages/activity/seckill/list` | `design/batch2/src/seckill-list.html` | 1 (0) | Added missing page-hero; white hero text; session scroll height; chips row. |
| `pages/activity/presale/list` | — | 0 (0) | Prior purple restyle. |
| `pages/activity/presale/detail` | — | 0 (0) | Prior restyle. |
| `pages/activity/presale/confirm` | — | 1 (0) | submit-btn width in page wxss. |
| `pages/notification/list` | — | 1 (1) | read-all-btn width:auto. |
| `pages/webview/index` | — | 0 (0) | Placeholder shell. |
| `pages/promotion/full-reduction` | — | 1 (0) | btn-lg CTA. |
| `pages/promotion/bundle` | — | 2 (2) | cart-btn/buy-btn flex in bottom bar wxss. |
| `pages/review/edit/index` | — | 1 (0) | btn-lg submit. |
| `pages/review/list/index` | — | 0 (0) | Prior restyle. |
| `pages/question/list/index` | — | 1 (0) | btn-sm ask. |
| `pages/favorite/list/index` | `design/batch2/src/favorite.html` | 2 (0) | Heart pill as view; btn-sm empty actions. |
| `pages/history/list/index` | `design/batch2/src/history.html` | 2 (0) | 删除/清空 as view pills; goods-name min-width:0. |
