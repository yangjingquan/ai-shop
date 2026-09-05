const pointsApi = require('../../api/points')
const marketingCapabilities = require('../../utils/marketing-capabilities')
Page({
  data: { profile: {}, ledgers: [], loading: true, signedToday: false },
  onShow() { marketingCapabilities.ensure('POINTS_MEMBER_DAY').then(ok => ok ? this.load() : wx.navigateBack()) },
  load() { this.setData({ loading: true }); return Promise.all([pointsApi.profile(), pointsApi.ledger()]).then(([p, l]) => {
    const ledgers = (l.data || []).map(item => ({ ...item, sign: Number(item.changeValue) > 0 ? '+' : '', timeText: this.time(item.createdAt) }))
    const signedToday = ledgers.some(x => x.source === 'SIGN_IN' && x.timeText === this.time(Date.now()))
    this.setData({ profile: p.data || {}, ledgers, signedToday, loading: false })
  }).catch(() => this.setData({ loading: false })) },
  time(value) { const d = new Date(typeof value === 'number' ? value : String(value || '').replace(' ', 'T')); if (isNaN(d)) return ''; const p=n=>String(n).padStart(2,'0'); return `${d.getMonth()+1}-${p(d.getDate())}` },
  signIn() { if (this.data.signedToday) return; pointsApi.signIn().then(res => { this.setData({ profile: res.data || this.data.profile, signedToday: true }); wx.showToast({ title: '签到成功' }); this.load() }) },
  goMall() { wx.navigateTo({ url: '/pages/points/mall' }) }, goMemberDay() { wx.navigateTo({ url: '/pages/points/member-day' }) },
})
