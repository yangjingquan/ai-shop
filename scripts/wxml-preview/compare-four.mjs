#!/usr/bin/env node
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { chromium } from 'playwright'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const ARTIFACTS = '/opt/cursor/artifacts/wxml-preview/compare-v2'
const PAIRS = [
  { name: 'seckill', mock: path.join(ROOT, 'design/batch2/src/seckill-list.html'), clone: path.join(ROOT, 'scripts/wxml-preview/clones/seckill.html') },
  { name: 'my', mock: path.join(ROOT, 'design/src/my.html'), clone: path.join(ROOT, 'scripts/wxml-preview/clones/my.html') },
  { name: 'history', mock: path.join(ROOT, 'design/batch2/src/history.html'), clone: path.join(ROOT, 'scripts/wxml-preview/clones/history.html') },
  { name: 'address', mock: path.join(ROOT, 'design/batch2/src/address-list.html'), clone: path.join(ROOT, 'scripts/wxml-preview/clones/address.html') },
]

function dataUrl(filePath) {
  const buf = fs.readFileSync(filePath)
  return `data:image/png;base64,${buf.toString('base64')}`
}

async function shot(browser, htmlPath, outPng) {
  const page = await browser.newPage({ viewport: { width: 375, height: 900 } })
  await page.goto(`file://${htmlPath}`, { waitUntil: 'load' })
  await page.waitForTimeout(250)
  await page.screenshot({ path: outPng, fullPage: true })
  await page.close()
}

async function compose(browser, left, right, out, label) {
  const page = await browser.newPage({ viewport: { width: 820, height: 1000 } })
  const l = dataUrl(left)
  const r = dataUrl(right)
  await page.setContent(`<!DOCTYPE html><html><body style="margin:0;display:flex;gap:8px;background:#111;padding:8px"><div><div style="color:#fff;font:12px sans-serif">${label} mock</div><img src="${l}" style="width:375px"/></div><div><div style="color:#fff;font:12px sans-serif">${label} clone</div><img src="${r}" style="width:375px"/></div></body></html>`)
  await page.screenshot({ path: out, fullPage: true })
  await page.close()
}

async function main() {
  fs.mkdirSync(ARTIFACTS, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  for (const p of PAIRS) {
    if (!fs.existsSync(p.clone)) {
      console.warn('missing clone', p.name)
      continue
    }
    const mockPng = path.join(ARTIFACTS, `${p.name}-mock.png`)
    const clonePng = path.join(ARTIFACTS, `${p.name}-clone.png`)
    if (fs.existsSync(p.mock)) await shot(browser, p.mock, mockPng)
    await shot(browser, p.clone, clonePng)
    await compose(browser, mockPng, clonePng, path.join(ARTIFACTS, `${p.name}-mock-vs-clone.png`), p.name)
  }
  await browser.close()
  console.log('Wrote', ARTIFACTS)
}

main()
