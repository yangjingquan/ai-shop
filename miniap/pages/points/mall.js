const pointsApi = require('../../api/points')
const addressApi = require('../../api/address')
const { resolveImageUrl } = require('../../utils/url')
const marketingCapabilities = require('../../utils/marketing-capabilities')
Page({
 data:{ items:[], balance:0, addressId:null, redeemingId:null },
 onShow(){ marketingCapabilities.ensure('POINTS_MEMBER_DAY').then(ok=>{if(ok)this.load()}); const id=wx.getStorageSync('points_selected_address_id'); if(id){this.setData({addressId:id});wx.removeStorageSync('points_selected_address_id')} },
 load(){Promise.all([pointsApi.profile(),pointsApi.mall()]).then(([p,m])=>{const balance=Number((p.data||{}).balance||0);this.setData({balance,redeemingId:null,items:(m.data||[]).map(i=>this.normalizeItem(i,balance))})})},
 normalizeItem(item,balance){const stock=Math.max(0,Number(item.stock||0));const limit=Math.max(0,Number(item.perUserLimit||0));const redeemed=Math.max(0,Number(item.redeemedCount||0));const reachedLimit=limit>0&&redeemed>=limit;const insufficient=Number(item.pointsPrice||0)>balance;const redeemText=stock<=0?'已售罄':reachedLimit?'已兑完':insufficient?'积分不足':'兑换';return {...item,image:resolveImageUrl(item.image||''),stock,perUserLimit:limit,redeemedCount:redeemed,stockText:stock>0?`剩余 ${stock} 件`:'已售罄',quotaText:limit>0?`每人限兑 ${limit} 件，已兑 ${Math.min(redeemed,limit)} 件`:'每人不限兑',redeemText,canRedeem:stock>0&&!reachedLimit&&!insufficient}},
 chooseAddress(){wx.navigateTo({url:'/pages/address/list?select=points'})},
 redeem(e){const item=e.currentTarget.dataset.item;if(!item||this.data.redeemingId)return;if(!item.canRedeem){wx.showToast({title:item.redeemText,icon:'none'});return}const submit=()=>{this.setData({redeemingId:item.id});pointsApi.redeem({pointsProductId:item.id,quantity:1,addressId:item.physical?this.data.addressId:null}).then(res=>{wx.showToast({title:item.physical?'兑换成功，等待发货':'兑换成功'});this.load();if(res.data&&res.data.orderNo)wx.navigateTo({url:`/pages/order/detail?orderNo=${res.data.orderNo}`})}).catch(()=>this.setData({redeemingId:null}))};if(item.physical&&!this.data.addressId){this.chooseAddress();return}wx.showModal({title:'确认兑换',content:`将使用 ${item.pointsPrice} 积分兑换${item.title}${item.physical?'，商家包邮':''}`,success:r=>r.confirm&&submit()})},
})
