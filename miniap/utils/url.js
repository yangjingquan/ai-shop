const config = require('./config')

function isAbsoluteUrl(url) {
  return /^(https?:)?\/\//.test(url)
    || url.startsWith('data:')
    || url.startsWith('blob:')
    || url.startsWith('wxfile://')
    || url.startsWith('cloud://')
}

function resolveImageUrl(url) {
  if (!url || typeof url !== 'string') return ''
  if (url.startsWith('/images/')) return url
  if (isAbsoluteUrl(url)) return url

  const path = url.startsWith('/') ? url : `/${url}`
  return `${config.BASE_URL}${path}`
}

module.exports = {
  resolveImageUrl,
}
