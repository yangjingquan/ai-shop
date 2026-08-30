#!/usr/bin/env bash
set -euo pipefail

ADMIN_BASE=${ADMIN_BASE:-http://localhost:8081}
WX_BASE=${WX_BASE:-http://localhost:8082}
ADMIN_USERNAME=${ADMIN_USERNAME:-admin}
ADMIN_PASSWORD=${ADMIN_PASSWORD:?Set ADMIN_PASSWORD to a non-default password}
MERCHANT_USERNAME=${MERCHANT_USERNAME:-m5_smoke_$(date +%s)}
MERCHANT_PASSWORD=${MERCHANT_PASSWORD:?Set MERCHANT_PASSWORD to a non-default password}
SUFFIX=$(date +%s)

hr() { echo; echo "=== $* ==="; }
fail() { echo "ERROR: $*" >&2; exit 1; }
post_json() { curl -s -X POST "$1" -H 'Content-Type: application/json' "${@:2}"; }
require_json() { jq -e "$1" >/dev/null; }

hr "Login admin"
ADMIN_LOGIN_JSON=$(post_json "$ADMIN_BASE/api/admin/auth/login" \
  -d "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}")
echo "$ADMIN_LOGIN_JSON" | require_json '.code == 0 and (.data.token | length > 0)' \
  || fail "admin login failed; set ADMIN_USERNAME/ADMIN_PASSWORD"
ATOKEN=$(echo "$ADMIN_LOGIN_JSON" | jq -r '.data.token')

hr "Create smoke merchant"
MERCHANT_CREATE_JSON=$(post_json "$ADMIN_BASE/api/admin/merchants" \
  -H "Authorization: Bearer $ATOKEN" \
  -d "{\"name\":\"M5团购烟测商家_$SUFFIX\",\"username\":\"$MERCHANT_USERNAME\",\"password\":\"$MERCHANT_PASSWORD\",\"contactName\":\"烟测\",\"contactPhone\":\"13900000000\",\"wxAppId\":\"m5-smoke-app-$SUFFIX\",\"wxSecret\":\"m5-smoke-secret-$SUFFIX\"}")
echo "$MERCHANT_CREATE_JSON" | require_json '.code == 0 and .data.merchantId != null' \
  || fail "creating smoke merchant failed; response: $MERCHANT_CREATE_JSON"
MERCHANT_ID=$(echo "$MERCHANT_CREATE_JSON" | jq -r '.data.merchantId')

hr "Read smoke merchant code"
MERCHANT_JSON=$(curl -s "$ADMIN_BASE/api/admin/merchants/$MERCHANT_ID" -H "Authorization: Bearer $ATOKEN")
echo "$MERCHANT_JSON" | require_json '.code == 0 and (.data.merchantCode | length > 0)' \
  || fail "reading smoke merchant code failed; response: $MERCHANT_JSON"
MERCHANT_CODE=$(echo "$MERCHANT_JSON" | jq -r '.data.merchantCode')
WX_HEADERS=(-H "merchant-code: $MERCHANT_CODE")

hr "Login smoke merchant"
LOGIN_JSON=$(post_json "$ADMIN_BASE/api/merchant/auth/login" \
  -d "{\"username\":\"$MERCHANT_USERNAME\",\"password\":\"$MERCHANT_PASSWORD\"}")
echo "$LOGIN_JSON" | require_json '.code == 0 and (.data.token | length > 0)' \
  || fail "merchant login failed for generated merchant $MERCHANT_USERNAME/$MERCHANT_PASSWORD; response: $LOGIN_JSON"
MTOKEN=$(echo "$LOGIN_JSON" | jq -r '.data.token')

hr "Create merchant-owned enabled level-2 category"
CAT1_JSON=$(post_json "$ADMIN_BASE/api/merchant/categories" \
  -H "Authorization: Bearer $MTOKEN" \
  -d "{\"name\":\"M5团购一级_$SUFFIX\",\"parentId\":0,\"sort\":90}")
echo "$CAT1_JSON" | require_json '.code == 0 and .data != null' \
  || fail "creating merchant top category failed; response: $CAT1_JSON"
CAT1=$(echo "$CAT1_JSON" | jq -r '.data')
CAT2_JSON=$(post_json "$ADMIN_BASE/api/merchant/categories" \
  -H "Authorization: Bearer $MTOKEN" \
  -d "{\"name\":\"M5团购二级_$SUFFIX\",\"parentId\":$CAT1,\"sort\":1}")
echo "$CAT2_JSON" | require_json '.code == 0 and .data != null' \
  || fail "creating merchant level-2 category failed; response: $CAT2_JSON"
CAT2=$(echo "$CAT2_JSON" | jq -r '.data')

hr "Create group buy product"
CREATE_JSON=$(post_json "$ADMIN_BASE/api/merchant/products" \
  -H "Authorization: Bearer $MTOKEN" \
  -d "{\"name\":\"M5团购商品_$SUFFIX\",\"categoryId\":$CAT2,\"isGroupBuy\":1,\"groupBuyPrice\":69.00,\"groupBuyRequiredCount\":2,\"specs\":[{\"name\":\"规格\",\"values\":[\"默认\"]}],\"skus\":[{\"specValueIndexes\":[0],\"price\":99.00,\"stock\":10}]}")
echo "$CREATE_JSON" | require_json '.code == 0 and .data != null' \
  || fail "creating group-buy product failed; response: $CREATE_JSON"
PID=$(echo "$CREATE_JSON" | jq -r '.data')

hr "Put product on shelf"
STATUS_JSON=$(curl -s -X PUT "$ADMIN_BASE/api/merchant/products/$PID/status?status=1" -H "Authorization: Bearer $MTOKEN")
echo "$STATUS_JSON" | require_json '.code == 0' \
  || fail "put product on shelf failed; response: $STATUS_JSON"

hr "Verify mini program group-buy product list contains product"
GB_LIST_JSON=$(curl -s "$WX_BASE/api/wx/group-buy/products?page=1&size=20" "${WX_HEADERS[@]}")
echo "$GB_LIST_JSON" | jq -e --argjson pid "$PID" '.code == 0 and ([.data.list[]?.id] | index($pid) != null)' >/dev/null \
  || fail "group-buy UI endpoint did not return product $PID; response: $GB_LIST_JSON"

hr "Read group-buy product detail"
DETAIL_JSON=$(curl -s "$WX_BASE/api/wx/group-buy/products/$PID" "${WX_HEADERS[@]}")
echo "$DETAIL_JSON" | require_json '.code == 0 and .data.product.isGroupBuy == 1 and (.data.product.skus | length) > 0' \
  || fail "group-buy product detail failed; response: $DETAIL_JSON"
SKUID=$(echo "$DETAIL_JSON" | jq -r '.data.product.skus[0].id')

hr "Login two wx smoke users"
WX_LOGIN_A_JSON=$(post_json "$WX_BASE/api/wx/auth/login" \
  -d "{\"code\":\"m5-smoke-a-$SUFFIX\",\"merchantCode\":\"$MERCHANT_CODE\"}")
WX_LOGIN_B_JSON=$(post_json "$WX_BASE/api/wx/auth/login" \
  -d "{\"code\":\"m5-smoke-b-$SUFFIX\",\"merchantCode\":\"$MERCHANT_CODE\"}")
echo "$WX_LOGIN_A_JSON" | require_json '.code == 0 and (.data.token | length > 0)' || fail "wx user A login failed. Start shop-wx-app with local mock-wx profile, or provide a working local WeChat auth setup. Response: $WX_LOGIN_A_JSON"
echo "$WX_LOGIN_B_JSON" | require_json '.code == 0 and (.data.token | length > 0)' || fail "wx user B login failed. Start shop-wx-app with local mock-wx profile, or provide a working local WeChat auth setup. Response: $WX_LOGIN_B_JSON"
WTOKEN_A=$(echo "$WX_LOGIN_A_JSON" | jq -r '.data.token')
WTOKEN_B=$(echo "$WX_LOGIN_B_JSON" | jq -r '.data.token')

hr "Create wx addresses"
ADDR_BODY_A="{\"receiver\":\"SmokeA\",\"phone\":\"13800138001\",\"region\":\"北京市/北京市/朝阳区\",\"detail\":\"M5 smoke $SUFFIX A\",\"isDefault\":true}"
ADDR_BODY_B="{\"receiver\":\"SmokeB\",\"phone\":\"13800138002\",\"region\":\"北京市/北京市/朝阳区\",\"detail\":\"M5 smoke $SUFFIX B\",\"isDefault\":true}"
ADDR_A_JSON=$(post_json "$WX_BASE/api/wx/addresses" -H "wx-token: $WTOKEN_A" "${WX_HEADERS[@]}" -d "$ADDR_BODY_A")
ADDR_B_JSON=$(post_json "$WX_BASE/api/wx/addresses" -H "wx-token: $WTOKEN_B" "${WX_HEADERS[@]}" -d "$ADDR_BODY_B")
echo "$ADDR_A_JSON" | require_json '.code == 0 and .data.id != null' || fail "creating address A failed; response: $ADDR_A_JSON"
echo "$ADDR_B_JSON" | require_json '.code == 0 and .data.id != null' || fail "creating address B failed; response: $ADDR_B_JSON"
ADDR_A=$(echo "$ADDR_A_JSON" | jq -r '.data.id')
ADDR_B=$(echo "$ADDR_B_JSON" | jq -r '.data.id')

hr "Open group"
OPEN_BODY="{\"productId\":$PID,\"skuId\":$SKUID,\"quantity\":1,\"addressId\":$ADDR_A,\"remark\":\"M5 smoke open $SUFFIX\"}"
OPEN_JSON=$(post_json "$WX_BASE/api/wx/group-buy/groups" -H "wx-token: $WTOKEN_A" "${WX_HEADERS[@]}" -d "$OPEN_BODY")
echo "$OPEN_JSON" | require_json '.code == 0 and .data.groupId != null and (.data.orderNo | length > 0)' \
  || fail "opening group failed; response: $OPEN_JSON"
GROUP_ID=$(echo "$OPEN_JSON" | jq -r '.data.groupId')
ORDER_A=$(echo "$OPEN_JSON" | jq -r '.data.orderNo')

hr "Verify opened group is waiting for payment"
GROUP_WAIT_JSON=$(curl -s "$WX_BASE/api/wx/group-buy/groups/$GROUP_ID" -H "wx-token: $WTOKEN_A" "${WX_HEADERS[@]}")
echo "$GROUP_WAIT_JSON" | require_json '.code == 0 and .data.status == 0 and .data.paidCount == 0' \
  || fail "opened group did not start at waiting state; response: $GROUP_WAIT_JSON"

hr "Verify real payment params for opener"
echo "$OPEN_JSON" | require_json '.code == 0 and .data.payParams != null' \
  || fail "opener did not return real payment params; response: $OPEN_JSON"

hr "Join group"
JOIN_BODY="{\"productId\":$PID,\"skuId\":$SKUID,\"quantity\":1,\"addressId\":$ADDR_B,\"remark\":\"M5 smoke join $SUFFIX\"}"
JOIN_JSON=$(post_json "$WX_BASE/api/wx/group-buy/groups/$GROUP_ID/join" -H "wx-token: $WTOKEN_B" "${WX_HEADERS[@]}" -d "$JOIN_BODY")
echo "$JOIN_JSON" | require_json '.code == 0 and .data.groupId != null and (.data.orderNo | length > 0)' \
  || fail "joining group failed; response: $JOIN_JSON"
ORDER_B=$(echo "$JOIN_JSON" | jq -r '.data.orderNo')

hr "Verify real payment params for joiner"
echo "$JOIN_JSON" | require_json '.code == 0 and .data.payParams != null' \
  || fail "joiner did not return real payment params; response: $JOIN_JSON"

hr "Payment callback required for group formation"
echo "Created two unpaid group-buy orders with real WeChat payment params; complete payment in WeChat and verify callback-driven formation separately."

hr "Smoke complete"
echo "merchant=$MERCHANT_ID product=$PID category=$CAT2 group=$GROUP_ID openerOrder=$ORDER_A joinerOrder=$ORDER_B"
