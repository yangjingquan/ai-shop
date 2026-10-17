# Full page audit (mock vs clone)

| Page | Mismatches found | Fixes made | Compare PNG |
|------|------------------|------------|-------------|
| `pages/home/index` | None (WXSS scan + compare PNG) | Tab pad; presale entry price+CTA row align; marketing zone cards | `/opt/cursor/artifacts/compare/pages_home_index.png` |
| `pages/topic/detail` | None (WXSS scan + compare PNG) | Inset 16:9 cover card + gradient title overlay; section-title product grid (aspectFill square cards, 2-line ellipsis, + CTA); 24rpx bottom safe spacer | `/opt/cursor/artifacts/compare/pages_topic_detail.png` |
| `pages/recommend/index` | None (WXSS scan + compare PNG) | Hero gradient + product grid spacing per mock | `/opt/cursor/artifacts/compare/pages_recommend_index.png` |
| `pages/category/index` | None (WXSS scan + compare PNG) | Tab pad (no padding-bottom:0 shorthand); category layout | `/opt/cursor/artifacts/compare/pages_category_index.png` |
| `pages/cart/index` | Tab page missing pad-custom-tab-bar; Content may sit under fixed bottom UI (add pad-fixed-bottom-bar or padding-bottom) | Extra cart pad for guess-you-like + checkout + tab bar | `/opt/cursor/artifacts/compare/pages_cart_index.png` |
| `pages/order/list` | None (WXSS scan + compare PNG) | Tab pad; swipe card width 100%; end-text clearance; no orange delete bleed | `/opt/cursor/artifacts/compare/pages_order_list.png` |
| `pages/my/index` | None (WXSS scan + compare PNG) | Tab pad (no padding:0 wipe); hero overlap points card; 88rpx service tiles | `/opt/cursor/artifacts/compare/pages_my_index.png` |
| `pages/address/list` | None (WXSS scan + compare PNG) | btn-block add; pad-fixed-bottom-bar + fixed bottom bar | `/opt/cursor/artifacts/compare/pages_address_list.png` |
| `pages/address/edit` | None (WXSS scan + compare PNG) | btn-block save; form card radius | `/opt/cursor/artifacts/compare/pages_address_edit.png` |
| `pages/product/detail` | None (WXSS scan + compare PNG) | pad-fixed-bottom-bar; bottom-bar grid buttons; title ellipsis | `/opt/cursor/artifacts/compare/pages_product_detail.png` |
| `pages/order/confirm` | None (WXSS scan + compare PNG) | pad-fixed-bottom-bar; btn-inline submit | `/opt/cursor/artifacts/compare/pages_order_confirm.png` |
| `pages/coupon/list` | None (WXSS scan + compare PNG) | Inset hero; spaced tab chips; centered empty state | `/opt/cursor/artifacts/compare/pages_coupon_list.png` |
| `pages/activity/referral/index` | None (WXSS scan + compare PNG) | Inset hero; SHARE & REWARD text; body lift; btn-block CTAs | `/opt/cursor/artifacts/compare/pages_activity_referral_index.png` |
| `pages/activity/referral/rewards` | None (WXSS scan + compare PNG) | btn-sm row actions; list padding | `/opt/cursor/artifacts/compare/pages_activity_referral_rewards.png` |
| `pages/activity/lottery/index` | None (WXSS scan + compare PNG) | btn-block draw; hide overlay in preview; prize grid | `/opt/cursor/artifacts/compare/pages_activity_lottery_index.png` |
| `pages/activity/lottery/result` | None (WXSS scan + compare PNG) | btn-block primary/ghost actions | `/opt/cursor/artifacts/compare/pages_activity_lottery_result.png` |
| `pages/activity/lottery/rewards` | None (WXSS scan + compare PNG) | btn-sm per row; hero purple | `/opt/cursor/artifacts/compare/pages_activity_lottery_rewards.png` |
| `pages/activity/lottery/records` | None (WXSS scan + compare PNG) | btn-block footer; empty centered btn | `/opt/cursor/artifacts/compare/pages_activity_lottery_records.png` |
| `pages/order/success` | None (WXSS scan + compare PNG) | Full-width order-btn/repurchase-btn; footer text actions centered | `/opt/cursor/artifacts/compare/pages_order_success.png` |
| `pages/order/detail` | Content may sit under fixed bottom UI (add pad-fixed-bottom-bar or padding-bottom) | goods-name ellipsis; inline action chips | `/opt/cursor/artifacts/compare/pages_order_detail.png` |
| `pages/points/index` | None (WXSS scan + compare PNG) | Task/ledger flex min-width:0; title ellipsis | `/opt/cursor/artifacts/compare/pages_points_index.png` |
| `pages/points/mall` | None (WXSS scan + compare PNG) | Shared inset hero; page-body-lift; grid card layout | `/opt/cursor/artifacts/compare/pages_points_mall.png` |
| `pages/points/records/index` | None (WXSS scan + compare PNG) | Ledger list tokens | `/opt/cursor/artifacts/compare/pages_points_records_index.png` |
| `pages/points/member-day` | None (WXSS scan + compare PNG) | btn-block member CTA; benefit cards | `/opt/cursor/artifacts/compare/pages_points_member-day.png` |
| `pages/group-buy/list` | None (WXSS scan + compare PNG) | Shared inset hero + body lift; search row | `/opt/cursor/artifacts/compare/pages_group-buy_list.png` |
| `pages/group-buy/group` | None (WXSS scan + compare PNG) | primary-btn/ghost-btn width 100% | `/opt/cursor/artifacts/compare/pages_group-buy_group.png` |
| `pages/activity/seckill/list` | None (WXSS scan + compare PNG) | Inset hero; chip ellipsis; countdown days+time; lifted session card | `/opt/cursor/artifacts/compare/pages_activity_seckill_list.png` |
| `pages/activity/presale/list` | None (WXSS scan + compare PNG) | Shared inset hero + lifted content section | `/opt/cursor/artifacts/compare/pages_activity_presale_list.png` |
| `pages/activity/presale/detail` | None (WXSS scan + compare PNG) | Fixed bottom bar + page padding-bottom 160rpx | `/opt/cursor/artifacts/compare/pages_activity_presale_detail.png` |
| `pages/activity/presale/confirm` | None (WXSS scan + compare PNG) | pad-fixed-bottom-bar; goods-name ellipsis; btn-inline submit | `/opt/cursor/artifacts/compare/pages_activity_presale_confirm.png` |
| `pages/notification/list` | None (WXSS scan + compare PNG) | Hero top-row + read-all pill; body lift | `/opt/cursor/artifacts/compare/pages_notification_list.png` |
| `pages/webview/index` | None (WXSS scan + compare PNG) | Placeholder shell | `/opt/cursor/artifacts/compare/pages_webview_index.png` |
| `pages/promotion/full-reduction` | None (WXSS scan + compare PNG) | Shared inset hero + page-body-lift; btn-block CTA | `/opt/cursor/artifacts/compare/pages_promotion_full-reduction.png` |
| `pages/promotion/bundle` | None (WXSS scan + compare PNG) | pad-fixed-bottom-bar; product-name ellipsis; dual bottom buttons | `/opt/cursor/artifacts/compare/pages_promotion_bundle.png` |
| `pages/review/edit/index` | None (WXSS scan + compare PNG) | btn-block submit | `/opt/cursor/artifacts/compare/pages_review_edit_index.png` |
| `pages/review/list/index` | None (WXSS scan + compare PNG) | Review cards + ellipsis | `/opt/cursor/artifacts/compare/pages_review_list_index.png` |
| `pages/question/list/index` | None (WXSS scan + compare PNG) | btn-sm ask submit | `/opt/cursor/artifacts/compare/pages_question_list_index.png` |
| `pages/favorite/list/index` | None (WXSS scan + compare PNG) | Hero top-row count pill; body lift; goods ellipsis | `/opt/cursor/artifacts/compare/pages_favorite_list_index.png` |
| `pages/history/list/index` | None (WXSS scan + compare PNG) | Hero top-row 清空 pill; body lift; timeline list | `/opt/cursor/artifacts/compare/pages_history_list_index.png` |
| `custom-tab-bar` |  | Hand clone custom-tab-bar.html; capsule tab bar tokens in app.wxss | `/opt/cursor/artifacts/compare/custom-tab-bar.png` |
| `popups/product` | Popup sheet: compare uses design HTML as clone baseline | Popup styles live in page wxss + app; confirmColor #FF4A1C on modals | `/opt/cursor/artifacts/compare/popups_product.png` |
| `popups/order` | Popup sheet: compare uses design HTML as clone baseline | Popup styles live in page wxss + app; confirmColor #FF4A1C on modals | `/opt/cursor/artifacts/compare/popups_order.png` |
| `popups/points` | Popup sheet: compare uses design HTML as clone baseline | Popup styles live in page wxss + app; confirmColor #FF4A1C on modals | `/opt/cursor/artifacts/compare/popups_points.png` |
| `popups/activity` | Popup sheet: compare uses design HTML as clone baseline | Popup styles live in page wxss + app; confirmColor #FF4A1C on modals | `/opt/cursor/artifacts/compare/popups_activity.png` |
| `popups/misc` | Popup sheet: compare uses design HTML as clone baseline | Popup styles live in page wxss + app; confirmColor #FF4A1C on modals | `/opt/cursor/artifacts/compare/popups_misc.png` |