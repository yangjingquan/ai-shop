#!/usr/bin/env node
/**
 * Minimal WXML → HTML preview for layout verification (375px viewport, rpx→px at /2).
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { chromium } from 'playwright'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.resolve(__dirname, '../..')
const MINIAP = path.join(ROOT, 'miniap')
const ARTIFACTS = process.env.ARTIFACTS_DIR || '/opt/cursor/artifacts/wxml-preview'

const PAGES = JSON.parse(fs.readFileSync(path.join(MINIAP, 'app.json'), 'utf8')).pages

function rpxToPx(css) {
  return css.replace(/(\d+(?:\.\d+)?)rpx/g, (_, n) => `${parseFloat(n) / 2}px`)
}

function loadWxss(...files) {
  return files
    .filter((f) => fs.existsSync(f))
    .map((f) => rpxToPx(fs.readFileSync(f, 'utf8')))
    .join('\n')
}

function stripWxml(wxml) {
  return wxml
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/\swx:[a-z-]+="[^"]*"/gi, '')
    .replace(/\swx:[a-z-]+='[^']*'/gi, '')
    .replace(/\sbind[a-z]*="[^"]*"/gi, '')
    .replace(/\scatch[a-z]*="[^"]*"/gi, '')
    .replace(/\sdata-[a-z-]+="[^"]*"/gi, '')
    .replace(/\s(open-type|mode|lazy-load|enhanced|show-scrollbar|disabled|loading|size|type|maxlength|placeholder-class|hover-class|scroll-x|scroll-y)="[^"]*"/gi, '')
    .replace(/<block[^>]*>/gi, '')
    .replace(/<\/block>/gi, '')
    .replace(/<view/g, '<div')
    .replace(/<\/view>/g, '</div>')
    .replace(/<text/g, '<span')
    .replace(/<\/text>/g, '</span>')
    .replace(/<image([^>]*)>/gi, '<div class="wx-image-stub"$1></div>')
    .replace(/<scroll-view/g, '<div class="wx-scroll-view"')
    .replace(/<\/scroll-view>/g, '</div>')
    .replace(/<navigator[^>]*>/gi, '<div class="wx-navigator">')
    .replace(/<\/navigator>/gi, '</div>')
    .replace(/<input[^>]*\/>/gi, '<div class="wx-input-stub"></div>')
    .replace(/<textarea[^>]*>[\s\S]*?<\/textarea>/gi, '<div class="wx-textarea-stub"></div>')
    .replace(/<switch[^>]*\/>/gi, '<div class="wx-switch-stub"></div>')
    .replace(/<picker[^>]*>/gi, '<div class="wx-picker">')
    .replace(/<\/picker>/gi, '</div>')
    .replace(/<button/g, '<button class="wx-native-button"')
}

function applyData(html, data) {
  let out = html
  const re = /\{\{([^}]+)\}\}/g
  out = out.replace(re, (_, expr) => {
    const key = expr.trim().split(/\s/)[0].replace(/[!?].*/, '')
    if (key.includes('.')) {
      const parts = key.split('.')
      let v = data
      for (const p of parts) v = v?.[p]
      return v == null ? '' : String(v)
    }
    return data[key] != null ? String(data[key]) : ''
  })
  out = out.replace(/\swx:if="\{\{[^}]+\}\}"/gi, '')
  out = out.replace(/\swx:elif="\{\{[^}]+\}\}"/gi, '')
  out = out.replace(/\swx:else\b/gi, '')
  out = out.replace(/\swx:for="\{\{[^}]+\}\}"/gi, '')
  out = out.replace(/\swx:key="[^"]*"/gi, '')
  out = out.replace(/\swx:for-item="[^"]*"/gi, '')
  return out
}

const FIXTURES = {
  'pages/activity/seckill/list': {
    activeSession: {
      status: 1,
      countdownText: '01:23:45',
      name: '场次2',
      startText: '14:00',
      endText: '18:00',
      activityDescription: '限时特价，售完即止',
      products: [
        {
          seckillSkuId: 1,
          mainImage: '',
          productName: '秒杀商品名称',
          specText: '精选规格',
          activityPriceText: '9.90',
          originalPriceText: '29.90',
          remainingText: '仅剩 62 件',
          stockPercent: 62,
          userLimit: 2,
          status: 1,
          statusText: '立即抢',
        },
      ],
    },
    sessions: [
      { id: 1, name: '场次1', startText: '10:00', status: 2 },
      { id: 2, name: '场次2', startText: '14:00', status: 1 },
      { id: 3, name: '场次3', startText: '20:00', status: 0 },
    ],
    activeIndex: 1,
    loading: false,
  },
  'pages/my/index': {
    avatarUrl: '',
    nickname: '杨小胖',
    phone: '13800000000',
    pointsEnabled: true,
    pointsProfile: { balance: 61, levelName: '普通会员', level: 1, memberDay: 8, memberDayActive: false },
    unreadCount: 0,
    couponEnabled: true,
    couponCount: 0,
    referralEnabled: true,
  },
  'pages/history/list/index': {
    items: [
      {
        productId: 1,
        name: '锋悦中性笔 0.5mm 黑色',
        mainImage: '',
        priceText: '15.00',
        available: true,
        viewedAtText: '2026-10-17 12:50',
        viewCountText: 8,
        initial: '锋',
      },
    ],
    total: 1,
    loading: false,
    loadingMore: false,
    hasMore: false,
    removingId: null,
    clearing: false,
    loadError: false,
  },
  'pages/address/list': {
    list: [
      {
        id: 1,
        receiver: '杨小胖',
        phone: '18201117988',
        region: '辽宁省/沈阳市/皇姑区',
        detail: '融创金地御璟壹号',
        isDefault: true,
      },
    ],
  },
}

function pageToFiles(pageRoute) {
  const base = path.join(MINIAP, pageRoute.replace(/\/index$/, ''))
  const isIndex = pageRoute.endsWith('/index')
  const wxmlPath = isIndex ? path.join(base, 'index.wxml') : `${base}.wxml`
  const wxssPath = isIndex ? path.join(base, 'index.wxss') : `${base}.wxss`
  if (!fs.existsSync(wxmlPath)) {
    const alt = path.join(MINIAP, pageRoute, 'index.wxml')
    if (fs.existsSync(alt)) return { wxmlPath: alt, wxssPath: path.join(MINIAP, pageRoute, 'index.wxss') }
  }
  return { wxmlPath, wxssPath }
}

function buildHtml(pageRoute, wxml, wxss) {
  const data = FIXTURES[pageRoute] || {}
  const body = applyData(stripWxml(wxml), data)
  const css = loadWxss(path.join(MINIAP, 'app.wxss'), wxss)
  return `<!DOCTYPE html><html><head><meta charset="utf-8"><style>
html,body{margin:0;padding:0;width:375px;background:#F6F6F8;}
.wx-native-button{margin:0;padding:0;border:none;background:transparent;width:auto;display:inline-flex;}
.wx-native-button::after{display:none;}
.wx-image-stub{background:linear-gradient(135deg,#EDEEF2,#D5D8E0);border-radius:12px;min-height:80px;min-width:80px;}
${css}
</style></head><body>${body}</body></html>`
}

async function screenshotPage(browser, pageRoute) {
  const { wxmlPath, wxssPath } = pageToFiles(pageRoute)
  if (!fs.existsSync(wxmlPath)) return { pageRoute, ok: false, error: 'missing wxml' }
  const wxml = fs.readFileSync(wxmlPath, 'utf8')
  const html = buildHtml(pageRoute, wxml, wxssPath)
  const slug = pageRoute.replace(/\//g, '_')
  const outDir = path.join(ARTIFACTS, 'rendered')
  fs.mkdirSync(outDir, { recursive: true })
  const htmlPath = path.join(outDir, `${slug}.html`)
  fs.writeFileSync(htmlPath, html)
  const page = await browser.newPage({ viewport: { width: 375, height: 812 } })
  await page.goto(`file://${htmlPath}`, { waitUntil: 'load' })
  await page.waitForTimeout(200)
  const pngPath = path.join(outDir, `${slug}.png`)
  await page.screenshot({ path: pngPath, fullPage: true })
  await page.close()
  return { pageRoute, ok: true, pngPath, htmlPath }
}

async function main() {
  const only = process.argv.slice(2)
  const targets = only.length ? only : PAGES
  fs.mkdirSync(ARTIFACTS, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const results = []
  for (const route of targets) {
    try {
      results.push(await screenshotPage(browser, route))
    } catch (e) {
      results.push({ pageRoute: route, ok: false, error: String(e) })
    }
  }
  await browser.close()
  fs.writeFileSync(path.join(ARTIFACTS, 'results.json'), JSON.stringify(results, null, 2))
  console.log(JSON.stringify(results.filter((r) => !r.ok), null, 2))
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
