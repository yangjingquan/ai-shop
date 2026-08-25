const ENV = 'dev'

const CONFIGS = {
  dev: {
    BASE_URL: 'http://localhost:8082',
  },
  prod: {
    BASE_URL: 'https://miniapi.nexbyte.top',
  },
}

module.exports = {
  ...CONFIGS[ENV],
  ENV,
  MERCHANT_CODE: 'M0000000001',
  REQUEST_TIMEOUT: 10000,
}
