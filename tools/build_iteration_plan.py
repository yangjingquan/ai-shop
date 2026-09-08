from pathlib import Path
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle

OUT = Path('/Users/yangjingquan/Documents/test/shop/docs/repost/test-20260906-iteration-plan.pdf')
FONT_PATH = '/System/Library/AssetsV2/com_apple_MobileAsset_Font8/53fe5be564086fefc7523ccd0a31200acf92e0e5.asset/AssetData/STHEITI.ttf'
pdfmetrics.registerFont(TTFont('ShopSans', FONT_PATH))
W,H=A4
s=getSampleStyleSheet()
TITLE=ParagraphStyle('title',parent=s['Title'],fontName='ShopSans',fontSize=22,leading=30,alignment=TA_CENTER,textColor=colors.HexColor('#9a3412'),spaceAfter=8)
SUB=ParagraphStyle('sub',parent=s['Normal'],fontName='ShopSans',fontSize=10,leading=15,alignment=TA_CENTER,textColor=colors.HexColor('#6b7280'))
H1=ParagraphStyle('h1',parent=s['Heading1'],fontName='ShopSans',fontSize=16,leading=23,textColor=colors.HexColor('#9a3412'),spaceBefore=10,spaceAfter=7)
H2=ParagraphStyle('h2',parent=s['Heading2'],fontName='ShopSans',fontSize=11.5,leading=17,textColor=colors.HexColor('#b45309'),spaceBefore=8,spaceAfter=5)
B=ParagraphStyle('b',parent=s['BodyText'],fontName='ShopSans',fontSize=8.6,leading=13.5,spaceAfter=4,textColor=colors.HexColor('#1f2937'))
S=ParagraphStyle('s',parent=B,fontSize=7.3,leading=10.6,textColor=colors.HexColor('#374151'))
BOX=ParagraphStyle('box',parent=B,borderWidth=.5,borderColor=colors.HexColor('#fed7aa'),borderPadding=7,backColor=colors.HexColor('#fff7ed'),spaceBefore=5,spaceAfter=7)
def P(x,st=B): return Paragraph(x,st)
def h1(x): return P(x,H1)
def h2(x): return P(x,H2)
def tbl(rows,widths,header=True):
    t=Table(rows,colWidths=widths,repeatRows=1 if header else 0)
    cmds=[('GRID',(0,0),(-1,-1),.35,colors.HexColor('#d1d5db')),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),5),('RIGHTPADDING',(0,0),(-1,-1),5),('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),5)]
    if header: cmds.append(('BACKGROUND',(0,0),(-1,0),colors.HexColor('#fef3c7')))
    t.setStyle(TableStyle(cmds)); return t
def footer(c,doc):
    c.saveState(); c.setStrokeColor(colors.HexColor('#fed7aa')); c.line(18*mm,15*mm,W-18*mm,15*mm)
    c.setFillColor(colors.HexColor('#6b7280')); c.setFont('ShopSans',7.5)
    c.drawString(18*mm,10*mm,'商城三端整改迭代计划 · 基于 test-20260906 测试报告')
    c.drawRightString(W-18*mm,10*mm,f'第 {doc.page} 页'); c.restoreState()

story=[Spacer(1,38*mm),P('商城三端问题归类与',TITLE),P('跨端整改迭代计划',TITLE),P('基于《商城三端深度测试分析报告》test-20260906.pdf',SUB),Spacer(1,12*mm),
P('<b>排期原则：</b>不按“小程序 / 运营后台 / 商户后台”分别建重复任务；按<b>一个业务事实源或状态机 = 一个整改包</b>排期。每个整改包必须同时完成后端契约、三个端的展示/权限/刷新及跨端验收，才允许关闭。',BOX),
h2('优先级定义'),P('<b>P0：</b>资金、订单事实、越权、库存等上线即可能造成不可逆损失的问题；必须在新增营销投放前关闭。<br/><b>P1：</b>业务闭环、对账、售后、治理或一致性问题；进入下一发布列车并与 P0 联调。<br/><b>P2：</b>体验、可读性、运营效率与可观测性增强；不阻塞 P0/P1，但不能单独改动造成契约漂移。',B),
h2('三端共同修改的完成定义'),P('同一整改包内必须有：① 后端领域模型/接口/幂等或事件变更；② 小程序消费者行为与错误提示；③ 商户后台配置/处理页；④ 运营后台治理、审计或异常处置；⑤ 数据看板口径与自动化契约/集成测试。若某端“不涉及”，需要在任务中写明不涉及原因和回归证据。',B),PageBreak()]

story += [h1('1. 整改包总览：按根因合并，而非按页面拆分'),
tbl([[P('<b>迭代包</b>',S),P('<b>优先级 / 建议顺序</b>',S),P('<b>统一修改点</b>',S),P('<b>共同影响端</b>',S)],
[P('I0-01<br/>价格与营销规则中心',S),P('P0 · 第 1 包',S),P('报价快照、活动互斥矩阵、优惠上限、资格校验、发布模拟',S),P('小程序 + 商户 + 运营 + 服务端',S)],
[P('I0-02<br/>订单领域模型统一',S),P('P0 · 第 2 包',S),P('canonical orderType / 状态字典 / 订单行快照 / 跨端枚举契约',S),P('小程序 + 商户 + 运营 + 服务端',S)],
[P('I0-03<br/>库存与权益幂等',S),P('P0 · 与 I0-01 联调',S),P('库存/秒杀库存/券/积分的预占、确认、释放、回调幂等',S),P('小程序 + 商户 + 运营 + 服务端',S)],
[P('I1-01<br/>平台治理与权限',S),P('P1 · P0 稳定后',S),P('平台营销/用户/角色、商户数据域隔离、高危操作审计',S),P('运营 + 商户 + 小程序 + 服务端',S)],
[P('I1-02<br/>履约、售后与资金对账',S),P('P1 · 与 I1-01 并行',S),P('发货/退款状态机、介入工单、对账任务、结算明细',S),P('小程序 + 商户 + 运营 + 服务端',S)],
[P('I1-03<br/>缓存与跨端同步',S),P('P1 · 依赖 I0-01/I0-02',S),P('数据版本、失效事件、最终报价 revalidate、一致性告警',S),P('小程序 + 商户 + 运营 + 服务端',S)],
[P('I2-01<br/>经营数据与结算分析',S),P('P1/P2 · 依赖 I1-02',S),P('统一口径、商户结算中心、经营/活动漏斗、导出任务',S),P('运营 + 商户 + 小程序埋点 + 服务端',S)],
[P('I2-02<br/>体验与运营效率',S),P('P2 · 最后一包',S),P('首页营销编排、价格解释、表格筛选/脱敏/空态/错误态',S),P('小程序 + 商户 + 运营',S)]],[30*mm,35*mm,65*mm,44*mm]),
h2('不能拆分的依赖关系'),P('I0-01 的 ruleVersion 是 I0-02 订单快照、I0-03 权益扣减、I1-03 缓存失效和 I2-01 活动 ROI 的共同字段；因此四项不能由各端各自计算。I0-02 的 orderType/state 是 I1-02 履约与退款、I2-01 对账结算的共同主键语义；先统一字典，再改页面。',BOX),PageBreak()]

story += [h1('2. P0 迭代：交易事实与资金防线（必须整包上线）'),
h2('I0-01 价格与营销规则中心'),
P('<b>合并来源：</b>X-02、M-01、C-MKT-01、C-MKT-02、C-MKT-03、C-B-03、C-B-04、A-MKT-01 至 A-MKT-04。<br/><b>为何必须合并：</b>零价、活动重叠、券使用、秒杀/拼团/预售/套餐价格差异都是同一个“最终报价”问题；分别改券页、秒杀页或小程序展示会继续产生不同价格。',B),
tbl([[P('<b>层级</b>',S),P('<b>本包必须修改</b>',S),P('<b>验收</b>',S)],
[P('服务端事实源',S),P('报价服务输出原价、活动价、券、积分、运费、实付、ruleVersion；活动矩阵规定秒杀/拼团/预售/套餐与券/满减/积分的互斥及白名单；非赠品应付 > 0；活动发布前模拟报价与预算预检。',S),P('同一请求在任意端得到同一金额；全组合回归无负价、非白名单零价；活动结束/停用后旧报价被拒绝或刷新。',S)],
[P('小程序',S),P('详情、购物车、结算只展示服务端报价；展示价格构成与不可用原因；提交前带 quoteId/ruleVersion 并处理失效刷新。',S),P('后台改价、券过期、活动提前结束、断网恢复时不能按旧价支付。',S)],
[P('商户后台',S),P('活动/券表单展示可否叠加、影响 SKU、预算与报价预览；禁止本店配置突破平台上限。',S),P('同 SKU 在冲突活动期不能发布；页面不再只靠“排除活动商品”字段。',S)],
[P('运营后台',S),P('新增平台营销治理、互斥矩阵、活动审批、阈值/预算与发布审计；可一键停用并查看影响。',S),P('管理员能阻止违规活动并追溯操作者、版本和受影响订单。',S)]],[27*mm,91*mm,56*mm]),
h2('I0-02 订单领域模型统一'),
P('<b>合并来源：</b>X-01、X-06、M-03、C-06、C-07。<br/><b>本包输出：</b>订单/订单行的 canonical orderType、canonical state、履约方式、活动/商品/地址/价格快照及状态转换表；三个端只能消费同一枚举与同一状态文案映射。',B),
P('<b>验收：</b>已发现的订单 26090601030300611294 在运营、商户、小程序显示相同类型、实付与状态；预售尾款、拼团待成团、套餐取消、零元积分兑换均有独立且可审计的转移路径。',BOX),PageBreak()]

story += [h1('2. P0 迭代（续）：库存/券/积分的并发闭环'),
h2('I0-03 库存与权益幂等'),
P('<b>合并来源：</b>X-04、C-B-01、C-B-04、C-B-05、C-B-06、C-CACHE-01/02。<br/><b>统一修改点：</b>库存、秒杀库存、拼团名额、套餐组件库存、券、积分与退款可退额都要具备“预占 - 支付确认 - 超时/取消释放”的同一事务/消息语义；请求与回调均有幂等键。',B),
tbl([[P('<b>场景</b>',S),P('<b>统一规则</b>',S),P('<b>三端表现</b>',S)],
[P('下单 / 支付',S),P('clientRequestId 创建订单幂等；库存和券锁定可回查；支付回调以支付单号幂等。',S),P('小程序超时提示“查询结果”；商户/运营显示预占、成功、释放原因。',S)],
[P('退款 / 售后',S),P('可退余额 = 实付 - 成功退款 - 处理中退款；同一订单行只允许一笔处理中的退款。',S),P('小程序展示剩余可退；商户审批不可超额；运营可看渠道失败和重试状态。',S)],
[P('活动结束 / 取消',S),P('未支付/未成团/退款失败按明确状态释放活动库存与权益，不得重复回补。',S),P('小程序失效且不可购买；商户看到恢复流水；运营看到异常队列。',S)]],[27*mm,79*mm,68*mm]),
h2('P0 上线闸门'),
P('I0-01、I0-02、I0-03 必须在同一个发布列车上线，且先灰度关闭“可叠加营销”与非白名单零价能力。发布前通过：报价组合矩阵、100 并发秒杀、同券双端支付、支付回调重复/乱序、部分退款并发、活动提前结束、订单跨端比对。任一失败，不开放新的商户营销活动。',BOX),PageBreak()]

story += [h1('3. P1 迭代：治理、履约、同步与对账'),
h2('I1-01 平台治理与权限'),
P('<b>合并来源：</b>O-01、A-01 至 A-06、M-AUTH-01 至 M-AUTH-04、A-UI-03。<br/><b>范围：</b>运营端补齐用户、平台券/营销、角色权限、消息、商户资质/冻结处置；商户角色拆分查看/编辑/发布/退款/成员授权/导出，所有 merchantId、orderId、couponId、fileId 由服务端数据域鉴权；高危操作采用原因、二次确认、双人复核（退款/成员授权）和审计。',B),
h2('I1-02 履约、售后与资金对账'),
P('<b>合并来源：</b>A-04、A-06、M-04、M-05、M-DATA-01、A-UI-04。<br/><b>范围：</b>发货字段和虚拟/到店/预售/拼团限制、售后完整状态机、平台介入工单、退款失败重试、对账任务幂等、结算明细。“立即对账”必须先预览范围、生成 taskId、记录执行日志和异常队列，不能作为页面按钮直接改变状态。',B),
h2('I1-03 缓存与跨端同步'),
P('<b>合并来源：</b>X-05、C-CACHE-01 至 C-CACHE-04、C-MKT-02、A-05。<br/><b>范围：</b>商品、库存、Banner、活动、商家状态、券、订单带 dataVersion/serverTime；后台发布后发出失效事件；小程序进入结算/恢复前台最终校验；运营/商户显示数据截至时间与异常刷新状态。',B),
h2('I2-01 经营数据与结算分析'),
P('<b>合并来源：</b>A-DATA-01/02、M-02、M-DATA-01/02。<br/><b>范围：</b>先定义 GMV、支付、退款、净额、可结算的口径和时区/延迟；再做商户结算中心、活动 ROI、漏斗、库存/售后指标、脱敏导出和下钻。注意“今日净额”不能命名或暗示为“可结算”。',B),
P('<b>P1 验收方式：</b>基于一笔普通、秒杀、拼团失败、预售尾款、套餐取消、部分退款订单做端到端勾稽：订单、支付、退款、佣金、补贴、手续费、结算状态在三端一致；权限账号无法读写其他商户数据。',BOX),PageBreak()]

story += [h1('4. P2 迭代：体验、可读性与运营效率'),
h2('I2-02 三端体验优化（依赖 P0/P1 的新事实源）'),
tbl([[P('<b>端</b>',S),P('<b>合并修改点</b>',S),P('<b>归入本包的原问题</b>',S)],
[P('小程序',S),P('首页营销优先级、频控和活动会场收敛；统一价格解释；商品卡截断/热区；空态、加载/错误/弱网与活动结束态。',S),P('C-UI-01 至 C-UI-05、C-05、C-MKT-03',S)],
[P('运营后台',S),P('列表组合筛选、列宽/手机号/账号可读性、敏感字段脱敏、历史数据缺失标注、导出任务。',S),P('A-UI-01 至 A-UI-05、A-DATA-02',S)],
[P('商户后台',S),P('账号页 owner 空态说明、退款/发货筛选与状态解释、经营首页数据新鲜度、活动配置影响提示。',S),P('M-AUTH-03、M-DATA-02、M-04/05 的 UI 部分',S)]],[22*mm,97*mm,55*mm]),
h2('任务拆分模板（研发管理系统可直接使用）'),
P('<b>标题：</b>[I0-01] 统一报价与营销互斥规则（小程序/商户/运营/服务端）<br/><b>不允许拆分：</b>接口版本、端侧展示、配置校验、审计和验收用例必须放在同一 Epic；子任务按端分配，但 Epic 不可因单端完成而关闭。<br/><b>每个子任务必填：</b>影响字段/事件、回滚方案、旧订单兼容、埋点/日志、权限要求、关联问题编号、测试用例。<br/><b>关闭条件：</b>后端契约测试 + 三端集成测试 + 数据勾稽 + 灰度监控均通过，并由测试在同一 ruleVersion/orderType 下复核。',BOX),
h2('推荐发布顺序'),
P('第 0 迭代：I0-01 → I0-02 → I0-03（同一发布列车）；第 1 迭代：I1-01 与 I1-02 并行，I1-03 跟随 P0 字段版本；第 2 迭代：I2-01；第 3 迭代：I2-02。任何 UI 单改不得绕开已定义的报价、订单类型或数据版本接口。',B),
P('<b>最终归类结论：</b>原报告中的大量三端问题已被收敛为 8 个迭代包，其中 I0-01/I0-02/I0-03 是 3 个不可拆分的 P0 跨端改造。后续排期请以本计划中的迭代包为 Epic，以端侧工作为子任务。',BOX)]

OUT.parent.mkdir(parents=True,exist_ok=True)
doc=SimpleDocTemplate(str(OUT),pagesize=A4,leftMargin=18*mm,rightMargin=18*mm,topMargin=17*mm,bottomMargin=20*mm,title='商城三端整改迭代计划')
doc.build(story,onFirstPage=footer,onLaterPages=footer)
print(OUT)
