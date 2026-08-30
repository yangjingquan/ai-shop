function decodeBase64(value: string) {
  const binary = window.atob(value)
  return Uint8Array.from(binary, (char) => char.charCodeAt(0))
}

function encodeBase64(value: ArrayBuffer) {
  const bytes = new Uint8Array(value)
  let binary = ''
  for (const byte of bytes) binary += String.fromCharCode(byte)
  return window.btoa(binary)
}

export async function encryptPassword(publicKey: string, password: string) {
  if (!window.crypto?.subtle) {
    throw new Error('当前浏览器不支持安全密码加密，请使用 HTTPS 或更新浏览器')
  }

  const cryptoKey = await window.crypto.subtle.importKey(
    'spki',
    decodeBase64(publicKey).buffer as ArrayBuffer,
    { name: 'RSA-OAEP', hash: 'SHA-256' },
    false,
    ['encrypt'],
  )
  const encrypted = await window.crypto.subtle.encrypt(
    { name: 'RSA-OAEP' },
    cryptoKey,
    new TextEncoder().encode(password),
  )
  return encodeBase64(encrypted)
}
