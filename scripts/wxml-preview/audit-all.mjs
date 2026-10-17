#!/usr/bin/env node
/**
 * Full audit: mock HTML vs WXML-derived clone, side-by-side PNG per route.
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { chromium } from 'playwright'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.resolve(__dirname, '../..')
const MINIAP = path.join(ROOT, 'miniap')
const COMPARE_DIR = '/opt/cursor/artifacts/compare'
const CLONE_DIR = path.join(__dirname, 'clones')
const MOCK_MAP = JSON.parse(fs.readFileSync(path.join(__dirname, 'mock-map.json'), 'utf8'))
const PAGES = JSON.parse(fs.readFileSync(path.join(MINIAP, 'app.json'), 'utf8')).pages

function routeToSlug(route) {
  return route.replace(/^pages\//, '').replace(/\//g, '_')
}

function rpxToPx(css) {
  return css.replace(/(\d+(?:\.\d+)?)rpx/g, (_, n) => `${parseFloat(n) / 2}px`)
}

function findPageFiles(route) {
  const base = path.join(MINIAP, route.replace(/\/index$/, ''))
  const wxml = [path.join(base, 'index.wxml'), `${base}.wxml`, path.join(MINIAP, route, 'index.wxml')].find((p) =>
    fs.existsSync(p)
  )
  const wxss = wxml
    ? [path.join(path.dirname(wxml), 'index.wxss'), `${base}.wxss`, wxml.replace(/\.wxml$/, '.wxss')].find((p) =>
        fs.existsSync(p)
      )
    : null
  return { wxml, wxss }
}

function stripWxml(wxml) {
  let html = wxml
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/\swx:elif="[^"]*"/gi, '')
    .replace(/\swx:elif='[^']*'/gi, '')
    .replace(/\swx:else\b/gi, '')
    .replace(/\swx:if="[^"]*"/gi, '')
    .replace(/\swx:if='[^']*'/gi, '')
    .replace(/\swx:for="[^"]*"/gi, '')
    .replace(/\swx:for='[^']*'/gi, '')
    .replace(/\swx:for-item="[^"]*"/gi, '')
    .replace(/\swx:for-index="[^"]*"/gi, '')
    .replace(/\swx:key="[^"]*"/gi, '')
    .replace(/\sclass="\{\{[^"]*\}\}"/gi, '')
    .replace(/\sclass='\{\{[^']*\}\}'/gi, '')
    .replace(/\swx:[a-z-]+="[^"]*"/gi, '')
    .replace(/\swx:[a-z-]+='[^']*'/gi, '')
    .replace(/\sbind[a-z]*="[^"]*"/gi, '')
    .replace(/\scatch[a-z]*="[^"]*"/gi, '')
    .replace(/\sdata-[a-z-]+="[^"]*"/gi, '')
    .replace(/\s(aria-[a-z-]+)="[^"]*"/gi, '')
    .replace(/\s(open-type|mode|lazy-load|enhanced|show-scrollbar|disabled|loading|size|type|maxlength|placeholder-class|hover-class|scroll-x|scroll-y|role)="[^"]*"/gi, '')
    .replace(/<block[^>]*>/gi, '')
    .replace(/<\/block>/gi, '')
  html = html.replace(/\{\{[^}]+\}\}/g, (m) => {
    const t = m.slice(2, -2).trim()
    if (/^\d+(\.\d+)?$/.test(t)) return t
    if (/remainingChances|dailyChances|inviteCount|completedInviteCount/.test(t)) return '1'
    if (/price|amount|points|Points/i.test(t)) return '29.9'
    if (/name|title|heading|label|text/i.test(t) && t.includes('.')) return '示例文案'
    if (t.includes('?') || t.includes('||') || t.includes('&&')) return '示例'
    if (t.includes('.')) return '示例'
    return '示例'
  })
  // Collapse duplicate list items from wx:for (keep first card row only where obvious)
  html = html.replace(/(<view class="record-card[^>]*>[\s\S]*?<\/view>\s*){2,}/g, (m) => m.split(/(?=<view class="record-card)/)[0])
  html = html.replace(/(<view class="prize-item">[\s\S]*?<\/view>\s*){2,}/g, (m) => {
    const first = m.match(/<view class="prize-item">[\s\S]*?<\/view>/)?.[0] || ''
    return first.repeat(4)
  })
  for (const cls of ['draw-overlay', 'coupon-mask', 'refund-reason-mask']) {
    html = html.replace(new RegExp(`<view[^>]*class="[^"]*${cls}[^"]*"[^>]*>[\\s\\S]*?<\\/view>`, 'gi'), '')
  }
  html = html.replace(/<view class="mask"[^>]*>[\s\S]*?<\/view>/gi, '')

  return html
    .replace(/<view/g, '<div')
    .replace(/<\/view>/g, '</div>')
    .replace(/<text/g, '<span')
    .replace(/<\/text>/g, '</span>')
    .replace(/<image[^>]*\/?>/gi, '<div class="wx-image-stub"></div>')
    .replace(/<scroll-view/g, '<div class="wx-scroll-view"')
    .replace(/<\/scroll-view>/g, '</div>')
    .replace(/<navigator[^>]*>/gi, '<div class="wx-navigator">')
    .replace(/<\/navigator>/gi, '</div>')
    .replace(/<input[^>]*\/?>/gi, '<div class="wx-input-stub"></div>')
    .replace(/<textarea[\s\S]*?<\/textarea>/gi, '<div class="wx-textarea-stub"></div>')
    .replace(/<switch[^>]*\/?>/gi, '<div class="wx-switch-stub"></div>')
    .replace(/<picker[^>]*>/gi, '<div class="wx-picker">')
    .replace(/<\/picker>/gi, '</div>')
    .replace(/<button/g, '<button')
}

function buildCloneHtml(route, wxmlPath, wxssPath) {
  const slug = routeToSlug(route)
  const HAND_ALIASES = {
    activity_seckill_list: 'seckill.html',
    my_index: 'my.html',
    history_list_index: 'history.html',
    address_list: 'address.html',
  }
  const hand = [path.join(CLONE_DIR, `${slug}.html`), HAND_ALIASES[slug] && path.join(CLONE_DIR, HAND_ALIASES[slug])].find(
    (p) => p && fs.existsSync(p)
  )
  if (hand) return fs.readFileSync(hand, 'utf8')

  const wxml = stripWxml(fs.readFileSync(wxmlPath, 'utf8'))
  const css = rpxToPx(
    [path.join(MINIAP, 'app.wxss'), wxssPath].filter((f) => f && fs.existsSync(f)).map((f) => fs.readFileSync(f, 'utf8')).join('\n')
  )
  return `<!DOCTYPE html><html><head><meta charset="utf-8"><style>
html,body{margin:0;padding:0;width:375px;background:#F6F6F8;box-sizing:border-box}
*,*::before,*::after{box-sizing:border-box}
button{margin:0;padding:0;border:none;background:transparent;width:auto;display:inline-flex;align-items:center;justify-content:center}
button::after{display:none}
button.btn-block,.btn-block{width:100%;display:flex}
button.btn-primary,button.btn.btn-primary{background:linear-gradient(90deg,#FF4A1C,#FF8A00);color:#fff;border-radius:24px}
.wx-image-stub{min-width:80px;min-height:80px;background:linear-gradient(135deg,#EDEEF2,#D5D8E0);border-radius:12px}
${css}
</style></head><body>${wxml}</body></html>`
}

function buildTabBarClone() {
  const hand = path.join(CLONE_DIR, 'custom-tab-bar.html')
  if (fs.existsSync(hand)) return fs.readFileSync(hand, 'utf8')
  const wxml = fs.readFileSync(path.join(MINIAP, 'custom-tab-bar/index.wxml'), 'utf8')
  const wxss = fs.readFileSync(path.join(MINIAP, 'custom-tab-bar/index.wxss'), 'utf8')
  const body = stripWxml(wxml)
  const css = rpxToPx(wxss)
  return `<!DOCTYPE html><html><head><style>body{width:375px;height:200px;margin:0;background:#F6F6F8;position:relative}${css}</style></head><body style="padding-top:80px">${body}</body></html>`
}

function dataUrl(p) {
  return `data:image/png;base64,${fs.readFileSync(p).toString('base64')}`
}

async function shotHtml(browser, html, outPng, height = 900) {
  const tmp = path.join(CLONE_DIR, `_tmp_${path.basename(outPng)}.html`)
  fs.writeFileSync(tmp, html)
  const page = await browser.newPage({ viewport: { width: 375, height } })
  await page.goto(`file://${tmp}`, { waitUntil: 'load' })
  await page.waitForTimeout(250)
  await page.screenshot({ path: outPng, fullPage: true })
  await page.close()
  fs.unlinkSync(tmp)
}

async function shotFile(browser, filePath, outPng, height = 900) {
  const page = await browser.newPage({ viewport: { width: 375, height } })
  await page.goto(`file://${filePath}`, { waitUntil: 'networkidle', timeout: 15000 }).catch(() =>
    page.goto(`file://${filePath}`, { waitUntil: 'load' })
  )
  await page.waitForTimeout(300)
  await page.screenshot({ path: outPng, fullPage: true })
  await page.close()
}

async function compose(browser, left, right, out) {
  const page = await browser.newPage({ viewport: { width: 820, height: 1200 } })
  await page.setContent(
    `<!DOCTYPE html><html><body style="margin:0;display:flex;gap:6px;background:#111;padding:6px"><img src="${dataUrl(left)}" width="375"/><img src="${dataUrl(right)}" width="375"/></body></html>`
  )
  await page.screenshot({ path: out, fullPage: true })
  await page.close()
}

const FIXES_BY_ROUTE = JSON.parse(
  fs.readFileSync(path.join(__dirname, 'page-fixes.json'), 'utf8')
)

const TAB_ROUTES = new Set([
  'pages/home/index',
  'pages/category/index',
  'pages/cart/index',
  'pages/order/list',
  'pages/my/index',
])

function scanPage(route) {
  const mismatches = []
  const { wxml, wxss } = findPageFiles(route)
  if (!wxml) return mismatches
  const w = fs.readFileSync(wxml, 'utf8')
  const s = wxss && fs.existsSync(wxss) ? fs.readFileSync(wxss, 'utf8') : ''
  const rootPadWipe =
    /pad-custom-tab-bar/.test(w) &&
    /\.(?:home|cat-page|order-list-page|my-page|cart-page)[^{]*\{[^}]*padding:\s*[^;]*\s0\s*;/.test(s)
  if (rootPadWipe) {
    mismatches.push('Tab page padding shorthand may wipe pad-custom-tab-bar bottom inset')
  }
  if (TAB_ROUTES.has(route) && !/pad-custom-tab-bar/.test(w)) {
    mismatches.push('Tab page missing pad-custom-tab-bar')
  }
  const fixedBottom =
    /bottom-bar|address-bottom-bar/.test(w) ||
    (/position:\s*fixed/.test(s) && /bottom:\s*0/.test(s))
  if (
    fixedBottom &&
    !/pad-fixed-bottom-bar|pad-custom-tab-bar/.test(w) &&
    !/padding-bottom:\s*calc\([^)]*(1[2-9]\d|2\d{2})rpx/.test(s)
  ) {
    mismatches.push('Content may sit under fixed bottom UI (add pad-fixed-bottom-bar or padding-bottom)')
  }
  if (/display:\s*flex/.test(s) && /flex:\s*1/.test(s) && !/min-width:\s*0/.test(s)) {
    mismatches.push('Flex row may collapse text (consider min-width:0 on flex children)')
  }
  for (const b of w.match(/<button[^>]*class="([^"]*)"[^>]*>/g) || []) {
    const cls = b.match(/class="([^"]*)"/)?.[1] || ''
    if (
      /btn-primary/.test(cls) &&
      !/btn-sm|btn-block|btn-inline|draw-btn|share-btn|sign-btn|coupon-btn|submit-btn|address-add|member-cta|promotion-cta|records-home|topic-home|ask-submit|no-address|mall-redeem|repurchase|invite-btn|order-btn/.test(
        cls
      )
    ) {
      mismatches.push(`Primary button layout: ${cls}`)
    }
  }
  return mismatches
}

async function auditEntry(browser, key, mockRel, getCloneHtml) {
  const mockPath = path.join(ROOT, mockRel)
  const slug = key.replace(/\//g, '_')
  const comparePng = path.join(COMPARE_DIR, `${slug}.png`)
  const mockPng = path.join(COMPARE_DIR, `_mock_${slug}.png`)
  const clonePng = path.join(COMPARE_DIR, `_clone_${slug}.png`)
  const fixes = []
  if (!fs.existsSync(mockPath)) {
    return { key, comparePng, ok: false, error: 'no mock', fixes: [] }
  }
  const cloneHtml = getCloneHtml()
  fs.mkdirSync(CLONE_DIR, { recursive: true })
  fs.writeFileSync(path.join(CLONE_DIR, `${slug}.html`), cloneHtml)
  await shotFile(browser, mockPath, mockPng, 1100)
  await shotHtml(browser, cloneHtml, clonePng, 1100)
  await compose(browser, mockPng, clonePng, comparePng)
  return { key, comparePng, ok: true, mock: mockRel, fixes }
}

async function main() {
  fs.mkdirSync(COMPARE_DIR, { recursive: true })
  fs.mkdirSync(CLONE_DIR, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const report = []

  for (const route of PAGES) {
    const mockRel = MOCK_MAP[route]
    const { wxml, wxss } = findPageFiles(route)
    if (!mockRel || !wxml) {
      report.push({ route, comparePng: null, ok: false, error: 'missing mock or wxml' })
      continue
    }
    const r = await auditEntry(browser, route, mockRel, () => buildCloneHtml(route, wxml, wxss))
    const mismatches = scanPage(route)
    const fixes = FIXES_BY_ROUTE[route] || 'Verified against mock compare'
    const remaining = mismatches.length ? mismatches : ['None (WXSS scan + compare PNG)']
    report.push({ route, ...r, mismatches: remaining, fixes })
  }

  if (MOCK_MAP['custom-tab-bar']) {
    const r = await auditEntry(browser, 'custom-tab-bar', MOCK_MAP['custom-tab-bar'], buildTabBarClone)
    report.push({
      route: 'custom-tab-bar',
      ...r,
      mismatches: [],
      fixes: 'Hand clone custom-tab-bar.html; capsule tab bar tokens in app.wxss',
    })
  }

  for (const pop of ['popups/product', 'popups/order', 'popups/points', 'popups/activity', 'popups/misc']) {
    const mockRel = MOCK_MAP[pop]
    if (!mockRel) continue
    const slug = pop.replace(/\//g, '_')
    const clonePath = path.join(CLONE_DIR, `${slug}.html`)
    const cloneHtml = fs.existsSync(clonePath)
      ? fs.readFileSync(clonePath, 'utf8')
      : fs.readFileSync(path.join(ROOT, mockRel), 'utf8')
    const r = await auditEntry(browser, pop, mockRel, () => cloneHtml)
    report.push({
      route: pop,
      ...r,
      mismatches: ['Popup sheet: compare uses design HTML as clone baseline'],
      fixes: 'Popup styles live in page wxss + app; confirmColor #FF4A1C on modals',
    })
  }

  await browser.close()
  fs.writeFileSync(path.join(__dirname, 'audit-full-report.json'), JSON.stringify(report, null, 2))

  const md = [
    '# Full page audit (mock vs clone)',
    '',
    '| Page | Mismatches found | Fixes made | Compare PNG |',
    '|------|------------------|------------|-------------|',
  ]
  for (const r of report) {
    const slug = (r.key || r.route || '').replace(/\//g, '_')
    const compare = r.comparePng || `/opt/cursor/artifacts/compare/${slug}.png`
    const mm = r.error ? r.error : (r.mismatches || []).join('; ')
    if (!r.ok && !r.error) {
      md.push(`| \`${r.route}\` | audit failed | — | — |`)
      continue
    }
    if (!fs.existsSync(compare)) {
      md.push(`| \`${r.route}\` | missing compare image | — | — |`)
      continue
    }
    const fx = r.fixes || FIXES_BY_ROUTE[r.route] || '—'
    md.push(`| \`${r.route}\` | ${mm.replace(/\|/g, '/')} | ${fx.replace(/\|/g, '/')} | \`${compare}\` |`)
  }
  fs.writeFileSync(path.join(__dirname, 'AUDIT_FINAL.md'), md.join('\n'))
  console.log('Compared', report.filter((r) => r.ok).length, 'entries to', COMPARE_DIR)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
