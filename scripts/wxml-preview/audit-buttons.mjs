#!/usr/bin/env node
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const MINIAP = path.join(ROOT, 'miniap')
const pages = JSON.parse(fs.readFileSync(path.join(MINIAP, 'app.json'), 'utf8')).pages

const MOCK_MAP = {
  'pages/home/index': 'design/src/home.html',
  'pages/category/index': 'design/src/category.html',
  'pages/cart/index': 'design/src/cart.html',
  'pages/order/list': 'design/src/order-list.html',
  'pages/my/index': 'design/src/my.html',
  'pages/favorite/list/index': 'design/batch2/src/favorite.html',
  'pages/history/list/index': 'design/batch2/src/history.html',
  'pages/address/list': 'design/batch2/src/address-list.html',
  'pages/address/edit': 'design/batch2/src/address-edit.html',
  'pages/activity/seckill/list': 'design/batch2/src/seckill-list.html',
  'pages/points/mall': 'design/batch2/src/points-mall.html',
}

function findWxml(route) {
  const base = path.join(MINIAP, route.replace(/\/index$/, ''))
  const candidates = [
    path.join(base, 'index.wxml'),
    `${base}.wxml`,
    path.join(MINIAP, route, 'index.wxml'),
  ]
  return candidates.find((p) => fs.existsSync(p))
}

const report = pages.map((route) => {
  const wxml = findWxml(route)
  let buttons = 0
  let unscoped = 0
  if (wxml) {
    const src = fs.readFileSync(wxml, 'utf8')
    const matches = src.match(/<button\b[^>]*>/g) || []
    buttons = matches.length
    unscoped = matches.filter((b) => !/\b(btn-block|btn-inline|btn-sm|btn-lg|btn-primary|btn-ghost|manage-btn|checkout-btn|remove-btn|submit-btn|avatar-btn|buy-button)\b/.test(b)).length
  }
  const mock = MOCK_MAP[route]
  const mockExists = mock && fs.existsSync(path.join(ROOT, mock))
  return { route, wxml: wxml ? path.relative(ROOT, wxml) : null, buttons, unscopedButtons: unscoped, mock: mockExists ? mock : null }
})

const out = path.join(ROOT, 'scripts/wxml-preview/audit-report.json')
fs.writeFileSync(out, JSON.stringify(report, null, 2))
console.log(`Wrote ${out}`)
