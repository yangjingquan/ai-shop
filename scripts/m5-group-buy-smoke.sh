#!/usr/bin/env bash
set -euo pipefail

ADMIN_BASE=${ADMIN_BASE:-http://localhost:8081}
WX_BASE=${WX_BASE:-http://localhost:8082}
MERCHANT_USERNAME=${MERCHANT_USERNAME:-merchant01}
MERCHANT_PASSWORD=${MERCHANT_PASSWORD:-123456}
WX_USER_A=${WX_USER_A:-1}
WX_USER_B=${WX_USER_B:-2}
SUFFIX=$(date +%s)

hr() { echo; echo "=== $* ==="; }
post_json() { curl -s -X POST "$1" -H 'Content-Type: application/json' "${@:2}"; }
require_json() { jq -e "$1" >/dev/null; }

hr "Login merchant"
LOGIN_JSON=$(post_json "$ADMIN_BASE/api/merchant/auth/login" \
  -d "{\"username\":\"$MERCHANT_USERNAME\",\"password\":\"$MERCHANT_PASSWORD\"}")
echo "$LOGIN_JSON" | require_json '.code == 0 and (.data.token | length > 0)'
MTOKEN=$(echo "$LOGIN_JSON" | jq -r '.data.token')

hr "Read merchant code"
PROFILE_JSON=$(curl -s "$ADMIN_BASE/api/merchant/profile" -H "Authorization: Bearer $MTOKEN")
echo "$PROFILE_JSON" | require_json '.code == 0 and (.data.merchantCode | length > 0)'
MERCHANT_CODE=$(echo "$PROFILE_JSON" | jq -r '.data.merchantCode')
WX_HEADERS=(-H "merchant-code: $MERCHANT_CODE")

hr "Create group buy product"
CREATE_JSON=$(post_json "$ADMIN_BASE/api/merchant/products" \
  -H "Authorization: Bearer $MTOKEN" \
  -d "{\"name\":\"M5团购商品_$SUFFIX\",\"categoryId\":10001,\"isGroupBuy\":1,\"groupBuyPrice\":69.00,\"groupBuyRequiredCount\":2,\"specs\":[{\"name\":\"规格\",\"values\":[\"默认\"]}],\"skus\":[{\"specValueIndexes\":[0],\"price\":99.00,\"stock\":10}]}")
echo "$CREATE_JSON" | require_json '.code == 0 and .data != null'
PID=$(echo "$CREATE_JSON" | jq -r '.data')

hr "Put product on shelf"
STATUS_JSON=$(curl -s -X PUT "$ADMIN_BASE/api/merchant/products/$PID/status?status=1" -H "Authorization: Bearer $MTOKEN")
echo "$STATUS_JSON" | require_json '.code == 0'

hr "Verify mini program group-buy product list contains product"
GB_LIST_JSON=$(curl -s "$WX_BASE/api/wx/group-buy/products?page=1&size=20" "${WX_HEADERS[@]}")
echo "$GB_LIST_JSON" | jq -e --argjson pid "$PID" '.code == 0 and ([.data.list[]?.id] | index($pid) != null)' >/dev/null

hr "Read group-buy product detail"
DETAIL_JSON=$(curl -s "$WX_BASE/api/wx/group-buy/products/$PID" "${WX_HEADERS[@]}")
echo "$DETAIL_JSON" | require_json '.code == 0 and .data.product.isGroupBuy == 1 and (.data.product.skus | length) > 0'
SKUID=$(echo "$DETAIL_JSON" | jq -r '.data.product.skus[0].id')

hr "Login wx users"
TOKEN_A_JSON=$(post_json "$WX_BASE/api/wx/auth/dev-login" -d "{\"userId\":$WX_USER_A}")
TOKEN_B_JSON=$(post_json "$WX_BASE/api/wx/auth/dev-login" -d "{\"userId\":$WX_USER_B}")
echo "$TOKEN_A_JSON" | require_json '.code == 0 and (.data.token | length > 0)'
echo "$TOKEN_B_JSON" | require_json '.code == 0 and (.data.token | length > 0)'
WTOKEN_A=$(echo "$TOKEN_A_JSON" | jq -r '.data.token')
WTOKEN_B=$(echo "$TOKEN_B_JSON" | jq -r '.data.token')

hr "Create wx addresses"
ADDR_BODY_A="{\"receiver\":\"SmokeA\",\"phone\":\"13800138001\",\"region\":\"北京市/北京市/朝阳区\",\"detail\":\"M5 smoke $SUFFIX A\",\"isDefault\":true}"
ADDR_BODY_B="{\"receiver\":\"SmokeB\",\"phone\":\"13800138002\",\"region\":\"北京市/北京市/朝阳区\",\"detail\":\"M5 smoke $SUFFIX B\",\"isDefault\":true}"
ADDR_A_JSON=$(post_json "$WX_BASE/api/wx/addresses" -H "wx-token: $WTOKEN_A" "${WX_HEADERS[@]}" -d "$ADDR_BODY_A")
ADDR_B_JSON=$(post_json "$WX_BASE/api/wx/addresses" -H "wx-token: $WTOKEN_B" "${WX_HEADERS[@]}" -d "$ADDR_BODY_B")
echo "$ADDR_A_JSON" | require_json '.code == 0 and .data.id != null'
echo "$ADDR_B_JSON" | require_json '.code == 0 and .data.id != null'
ADDR_A=$(echo "$ADDR_A_JSON" | jq -r '.data.id')
ADDR_B=$(echo "$ADDR_B_JSON" | jq -r '.data.id')

hr "Open group"
OPEN_BODY="{\"productId\":$PID,\"skuId\":$SKUID,\"quantity\":1,\"addressId\":$ADDR_A,\"remark\":\"M5 smoke open $SUFFIX\"}"
OPEN_JSON=$(post_json "$WX_BASE/api/wx/group-buy/groups" -H "wx-token: $WTOKEN_A" "${WX_HEADERS[@]}" -d "$OPEN_BODY")
echo "$OPEN_JSON" | require_json '.code == 0 and .data.groupId != null and (.data.orderNo | length > 0)'
GROUP_ID=$(echo "$OPEN_JSON" | jq -r '.data.groupId')
ORDER_A=$(echo "$OPEN_JSON" | jq -r '.data.orderNo')

hr "Verify opened group is waiting for members"
GROUP_WAIT_JSON=$(curl -s "$WX_BASE/api/wx/group-buy/groups/$GROUP_ID" -H "wx-token: $WTOKEN_A" "${WX_HEADERS[@]}")
echo "$GROUP_WAIT_JSON" | require_json '.code == 0 and .data.status == 0 and .data.paidCount == 0'

hr "Mock pay opener and verify wait-group order"
PAY_A_JSON=$(post_json "$WX_BASE/api/wx/order/$ORDER_A/mock-pay" -H "wx-token: $WTOKEN_A" "${WX_HEADERS[@]}")
echo "$PAY_A_JSON" | require_json '.code == 0'
ORDER_A_JSON=$(curl -s "$WX_BASE/api/wx/order/$ORDER_A" -H "wx-token: $WTOKEN_A" "${WX_HEADERS[@]}")
echo "$ORDER_A_JSON" | require_json '.code == 0 and .data.status == 5 and .data.orderType == 1 and .data.groupBuyPaidCount == 1 and .data.groupBuyRequiredCount == 2'

hr "Join group"
JOIN_BODY="{\"productId\":$PID,\"skuId\":$SKUID,\"quantity\":1,\"addressId\":$ADDR_B,\"remark\":\"M5 smoke join $SUFFIX\"}"
JOIN_JSON=$(post_json "$WX_BASE/api/wx/group-buy/groups/$GROUP_ID/join" -H "wx-token: $WTOKEN_B" "${WX_HEADERS[@]}" -d "$JOIN_BODY")
echo "$JOIN_JSON" | require_json '.code == 0 and .data.groupId != null and (.data.orderNo | length > 0)'
ORDER_B=$(echo "$JOIN_JSON" | jq -r '.data.orderNo')

hr "Mock pay joiner and verify formation"
PAY_B_JSON=$(post_json "$WX_BASE/api/wx/order/$ORDER_B/mock-pay" -H "wx-token: $WTOKEN_B" "${WX_HEADERS[@]}")
echo "$PAY_B_JSON" | require_json '.code == 0'
GROUP_DONE_JSON=$(curl -s "$WX_BASE/api/wx/group-buy/groups/$GROUP_ID" -H "wx-token: $WTOKEN_A" "${WX_HEADERS[@]}")
echo "$GROUP_DONE_JSON" | require_json '.code == 0 and .data.status == 1 and .data.paidCount == 2'
ORDER_A_DONE_JSON=$(curl -s "$WX_BASE/api/wx/order/$ORDER_A" -H "wx-token: $WTOKEN_A" "${WX_HEADERS[@]}")
ORDER_B_DONE_JSON=$(curl -s "$WX_BASE/api/wx/order/$ORDER_B" -H "wx-token: $WTOKEN_B" "${WX_HEADERS[@]}")
echo "$ORDER_A_DONE_JSON" | require_json '.code == 0 and .data.status == 6 and .data.groupBuyPaidCount == 2'
echo "$ORDER_B_DONE_JSON" | require_json '.code == 0 and .data.status == 6 and .data.groupBuyPaidCount == 2'

hr "Verify merchant shipping readiness"
SHIP_PAGE_JSON=$(curl -s "$ADMIN_BASE/api/merchant/order/page?status=6&page=1&size=20" -H "Authorization: Bearer $MTOKEN")
echo "$SHIP_PAGE_JSON" | jq -e --arg orderA "$ORDER_A" --arg orderB "$ORDER_B" '.code == 0 and ([.data.list[]?.orderNo] | index($orderA) != null and index($orderB) != null)' >/dev/null
SHIP_JSON=$(post_json "$ADMIN_BASE/api/merchant/order/ship?orderNo=$ORDER_A" \
  -H "Authorization: Bearer $MTOKEN" \
  -d "{\"shipNo\":\"M5$SUFFIX\"}")
echo "$SHIP_JSON" | require_json '.code == 0'
SHIPPED_JSON=$(curl -s "$WX_BASE/api/wx/order/$ORDER_A" -H "wx-token: $WTOKEN_A" "${WX_HEADERS[@]}")
echo "$SHIPPED_JSON" | require_json '.code == 0 and .data.status == 2 and (.data.shipNo | length > 0)'

hr "Smoke complete"
echo "product=$PID group=$GROUP_ID openerOrder=$ORDER_A joinerOrder=$ORDER_B"
