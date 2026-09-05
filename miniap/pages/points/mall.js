const pointsApi = require('../../api/points')
const addressApi = require('../../api/address')
const { resolveImageUrl } = require('../../utils/url')
const marketingCapabilities = require('../../utils/marketing-capabilities')
Page({
 data:{ items:[], balance:0, addressId:null },
 onShow(){ marketingCapabilities.ensure('POINTS_MEMBER_DAY').then(ok=>{if(ok)this.load()}); const id=wx.getStorageSync('points_selected_address_id'); if(id){this.setData({addressId:id});wx.removeStorageSync('points_selected_address_id')} },
 load(){Promise.all([pointsApi.profile(),pointsApi.mall()]).then(([p,m])=>this.setData({balance:(p.data||{}).balance||0,items:(m.data||[]).map(i=>({...i,image:resolveImageUrl(i.image||'')}))}))},
 chooseAddress(){wx.navigateTo({url:'/pages/address/list?select=points'})},
 redeem(e){const item=e.currentTarget.dataset.item;if(!item)return;if(Number(item.pointsPrice)>this.data.balance){wx.showToast({title:'积分不足',icon:'none'});return} const submit=()=>pointsApi.redeem({pointsProductId:item.id,quantity:1,addressId:item.physical?this.data.addressId:null}).then(res=>{wx.showToast({title:item.physical?'兑换成功，等待发货':'兑换成功'});this.load();if(res.data&&res.data.orderNo)wx.navigateTo({url:`/pages/order/detail?orderNo=${res.data.orderNo}`})}); if(item.physical&&!this.data.addressId){this.chooseAddress();return} wx.showModal({title:'确认兑换',content:`将使用 ${item.pointsPrice} 积分兑换${item.title}${item.physical?'，商家包邮':''}`,success:r=>r.confirm&&submit()})},
})
