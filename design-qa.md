# Design QA

source visual truth paths:
- `/var/folders/gd/133fk3b92tl_glzpvs2kxf9r0000gn/T/codex-clipboard-106cf343-929c-45bf-a6bf-e6de3eb26380.png` — 退款处理中
- `/var/folders/gd/133fk3b92tl_glzpvs2kxf9r0000gn/T/codex-clipboard-b96be9fa-b121-40b8-8266-1b966d5f6fd0.png` — 已成团

implementation preview: 微信开发者工具，`pages/group-buy/group`，iPhone 12/13 Pro 131%
state: 已成团终态；退款处理中分支同步完成结构与样式检查

## Comparison evidence

- 顶部重复的终态 `hero` 区域已移除，页面从结果卡开始。
- 结果卡、商品卡、详情信息卡的纵向层级与参考图一致。
- “查看订单”使用白底红框，“查看物流”使用红色实心按钮。
- 页面级按钮样式覆盖全局橙色按钮，避免主题色被公共样式覆盖。
- 微信开发者工具实时预览已确认：终态页面不再出现“团购结果 / 已成团 / 退款处理中”的重复顶部信息。
- 开发者工具问题面板：0 个问题。

## Implementation Checklist

- [x] 已成团状态移除顶部重复信息区。
- [x] 退款处理中状态移除顶部重复信息区。
- [x] 进行中状态保留团购进度与倒计时。
- [x] 统一结果卡、商品卡、详情卡间距和圆角层级。
- [x] 修正本页按钮主题色并完成实时预览检查。
- [x] `git diff --check` 通过。

## Follow-up notes

当前预览账号直接打开的是已成团详情；退款处理中分支已按相同终态布局完成代码检查，待后续有可切换的失败团数据时可再补一张实机截图。

final result: passed
