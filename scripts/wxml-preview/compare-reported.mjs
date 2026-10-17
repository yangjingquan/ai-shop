#!/usr/bin/env node
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { chromium } from 'playwright'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.resolve(__dirname, '../..')
const ARTIFACTS = process.env.ARTIFACTS_DIR || '/opt/cursor/artifacts/wxml-preview'

const PAIRS = [
  { name: 'seckill', mock: path.join(ROOT, 'design/batch2/src/seckill-list.html'), rendered: path.join(ARTIFACTS, 'rendered/pages_activity_seckill_list.png'), fb: path.join(ROOT, 'miniap/fb/1-seckill.png') },
  { name: 'my', mock: path.join(ROOT, 'design/src/my.html'), rendered: path.join(ARTIFACTS, 'rendered/pages_my_index.png'), fb: path.join(ROOT, 'miniap/fb/2-my.png') },
  { name: 'history', mock: path.join(ROOT, 'design/batch2/src/history.html'), rendered: path.join(ARTIFACTS, 'rendered/pages_history_list_index.png'), fb: path.join(ROOT, 'miniap/fb/3-history.png') },
  { name: 'address', mock: path.join(ROOT, 'design/batch2/src/address-list.html'), rendered: path.join(ARTIFACTS, 'rendered/pages_address_list.png'), fb: path.join(ROOT, 'miniap/fb/4-address.png') },
]

async function shotHtml(browser, htmlPath, outPng) {
  const page = await browser.newPage({ viewport: { width: 375, height: 900 } })
  await page.goto(`file://${htmlPath}`, { waitUntil: 'load' })
  await page.waitForTimeout(300)
  await page.screenshot({ path: outPng, fullPage: true })
  await page.close()
}

function dataUrl(filePath) {
  const buf = fs.readFileSync(filePath)
  const ext = path.extname(filePath).toLowerCase()
  const mime = ext === '.png' ? 'image/png' : ext === '.jpg' || ext === '.jpeg' ? 'image/jpeg' : 'image/png'
  return `data:${mime};base64,${buf.toString('base64')}`
}

async function compose(browser, name, left, right, out) {
  const page = await browser.newPage({ viewport: { width: 820, height: 920 } })
  const l = dataUrl(left)
  const r = dataUrl(right)
  await page.setContent(`<!DOCTYPE html><html><body style="margin:0;display:flex;gap:8px;background:#111;padding:8px"><div><div style="color:#fff;font:12px sans-serif;padding:4px">${name} left</div><img src="${l}" style="width:375px;height:auto;display:block"/></div><div><div style="color:#fff;font:12px sans-serif;padding:4px">${name} right</div><img src="${r}" style="width:375px;height:auto;display:block"/></div></body></html>`)
  await page.screenshot({ path: out, fullPage: true })
  await page.close()
}

async function main() {
  fs.mkdirSync(path.join(ARTIFACTS, 'compare'), { recursive: true })
  const browser = await chromium.launch({ headless: true })
  for (const p of PAIRS) {
    const mockPng = path.join(ARTIFACTS, 'compare', `${p.name}-mock.png`)
    if (fs.existsSync(p.mock)) await shotHtml(browser, p.mock, mockPng)
    if (fs.existsSync(p.rendered) && fs.existsSync(mockPng)) {
      await compose(browser, p.name, mockPng, p.rendered, path.join(ARTIFACTS, 'compare', `${p.name}-mock-vs-rendered.png`))
    }
    if (fs.existsSync(p.fb) && fs.existsSync(p.rendered)) {
      await compose(
        browser,
        `${p.name}-devtools`,
        p.fb,
        p.rendered,
        path.join(ARTIFACTS, 'compare', `${p.name}-devtools-vs-rendered.png`)
      )
    }
  }
  await browser.close()
}

main()
