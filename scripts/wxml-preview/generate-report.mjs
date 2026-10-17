#!/usr/bin/env node
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const audit = JSON.parse(fs.readFileSync(path.join(ROOT, 'scripts/wxml-preview/audit-report.json'), 'utf8'))

const FIXES = {
  'pages/home/index': 'Global button reset; tab bar padding (prior pass). Popup claim-btn keeps btn-block width.',
  'pages/topic/detail': 'Prior mockup restyle; buttons use .btn utilities.',
  'pages/recommend/index': 'Prior hero restyle; no action buttons.',
  'pages/category/index': 'Tab bar padding; removed warm override block.',
  'pages/cart/index': 'Hero + checkout above tab bar; mini buttons width:auto in page wxss.',
  'pages/order/list': 'Tab padding; empty-btn width:auto.',
  'pages/my/index': 'Hero layout; gradient service tiles (88rpx); points card overlap.',
  'pages/address/list': 'btn-block on 新增地址; flex bottom bar.',
  'pages/address/edit': 'btn-block submit; full-width flex.',
  'pages/product/detail': 'Bottom bar buttons explicitly width:100% in grid (unchanged).',
  'pages/order/confirm': 'btn utilities on submit.',
  'pages/coupon/list': 'Prior restyle.',
  'pages/activity/referral/index': 'btn-lg block buttons.',
  'pages/activity/referral/rewards': 'btn-sm inline.',
  'pages/activity/lottery/index': 'btn-lg draw button.',
  'pages/activity/lottery/result': 'btn-lg actions.',
  'pages/activity/lottery/rewards': 'btn-sm per row.',
  'pages/activity/lottery/records': 'btn footer.',
  'pages/order/success': 'Custom order-btn widths in page wxss (full/intent).',
  'pages/order/detail': 'Share button uses .btn.dark.',
  'pages/points/index': 'Sign-in btn-sm.',
  'pages/points/mall': 'Grid redeem btn-sm.',
  'pages/points/records/index': 'Prior restyle.',
  'pages/points/member-day': 'btn-lg CTA.',
  'pages/group-buy/list': 'Prior restyle.',
  'pages/group-buy/group': 'primary-btn/ghost-btn width rules in page wxss.',
  'pages/activity/seckill/list': 'Added missing page-hero; white hero text; session scroll height; chips row.',
  'pages/activity/presale/list': 'Prior purple restyle.',
  'pages/activity/presale/detail': 'Prior restyle.',
  'pages/activity/presale/confirm': 'submit-btn width in page wxss.',
  'pages/notification/list': 'read-all-btn width:auto.',
  'pages/webview/index': 'Placeholder shell.',
  'pages/promotion/full-reduction': 'btn-lg CTA.',
  'pages/promotion/bundle': 'cart-btn/buy-btn flex in bottom bar wxss.',
  'pages/review/edit/index': 'btn-lg submit.',
  'pages/review/list/index': 'Prior restyle.',
  'pages/question/list/index': 'btn-sm ask.',
  'pages/favorite/list/index': 'Heart pill as view; btn-sm empty actions.',
  'pages/history/list/index': '删除/清空 as view pills; goods-name min-width:0.',
}

const global = `**Global:** app.wxss button reset (margin/padding/width:auto/transparent), .btn-block / .btn-inline utilities.`

let md = `# Mini program page layout audit (39 routes)\n\n${global}\n\n| Route | Mock HTML | Buttons (unscoped) | Fix notes |\n|-------|-----------|----------------------|----------|\n`
for (const row of audit) {
  md += `| \`${row.route}\` | ${row.mock ? `\`${row.mock}\`` : '—'} | ${row.buttons} (${row.unscopedButtons}) | ${FIXES[row.route] || 'Prior batch-2 restyle; inherits global button reset'} |\n`
}

const out = path.join(ROOT, 'scripts/wxml-preview/PAGE_FIX_REPORT.md')
fs.writeFileSync(out, md)
console.log('Wrote', out)
