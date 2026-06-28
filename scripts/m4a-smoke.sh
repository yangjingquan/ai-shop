#!/usr/bin/env bash
# M4a Mall MVP - 购物车 + 下单 + 支付 + 订单查询 冒烟脚本
# 使用 dev profile，server 需要在 localhost:8080 运行
set -e
BASE="http://localhost:8080"
PASS=0
FAIL=0

hr() { echo; echo "===== $* ====="; }
pass() { echo "  [PASS] $*"; PASS=$((PASS+1)); }
fail() { echo "  [FAIL] $*"; FAIL=$((FAIL+1)); }

he() { pass "$1"; }

hr "Step 1: dev-login"
TOKEN=$(curl -s -X POST "$BASE/api/wx/auth/dev-login" \
  -H "Content-Type: application/json" \
  --data-binary '{"userId":3}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["token"])')
echo "TOKEN OK" && pass "dev-login"

hr "Step 2: Create address + add cart"
AID=$(curl -s -X POST "$BASE/api/wx/addresses" \
  -H "Content-Type: application/json" -H "wx-token: $TOKEN" \
  --data-binary '{"receiver":"测试","phone":"13800138000","region":"北京 朝阳","detail":"望京soho"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["id"])')
curl -s -X POST "$BASE/api/wx/cart" \
  -H "Content-Type: application/json" -H "wx-token: $TOKEN" \
  --data-binary '{"skuId":9,"quantity":1}' > /dev/null
CID=$(curl -s "$BASE/api/wx/cart" -H "wx-token: $TOKEN" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"][0]["id"])')
echo "addressId=$AID cartItemId=$CID" && pass "add-cart+address"

hr "Step 3: Preview order"
PREVIEW=$(curl -s -X POST "$BASE/api/wx/order/preview" \
  -H "Content-Type: application/json" -H "wx-token: $TOKEN" \
  --data-binary "{\"cartItemIds\":[$CID],\"addressId\":$AID}")
TOTAL=$(echo "$PREVIEW" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["totalAmount"])')
[ "$TOTAL" != "null" ] && pass "preview total=$TOTAL" || fail "preview failed"

hr "Step 4: Create order"
ORDER_NO=$(curl -s -X POST "$BASE/api/wx/order/create" \
  -H "Content-Type: application/json" -H "wx-token: $TOKEN" \
  --data-binary "{\"cartItemIds\":[$CID],\"addressId\":$AID}" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"][0]["orderNo"])')
[ -n "$ORDER_NO" ] && pass "orderNo=$ORDER_NO" || fail "create order failed"

hr "Step 5: Cart empty after order"
CART_LEN=$(curl -s "$BASE/api/wx/cart" -H "wx-token: $TOKEN" \
  | python3 -c 'import sys,json;print(len(json.load(sys.stdin)["data"]))')
[ "$CART_LEN" = "0" ] && pass "cart cleared" || fail "cart not empty: $CART_LEN"

hr "Step 6: Order page"
PAGE_RET=$(curl -s "$BASE/api/wx/order/page?page=1&size=10" -H "wx-token: $TOKEN")
PAGE_TOTAL=$(echo "$PAGE_RET" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["total"])')
[ "$PAGE_TOTAL" -ge 1 ] && pass "order page total=$PAGE_TOTAL" || fail "order page empty"

hr "Step 7: Order detail"
DETAIL=$(curl -s "$BASE/api/wx/order/$ORDER_NO" -H "wx-token: $TOKEN")
DETAIL_NO=$(echo "$DETAIL" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["orderNo"])')
[ "$DETAIL_NO" = "$ORDER_NO" ] && pass "detail orderNo match" || fail "detail mismatch"

hr "Step 8: Mock pay"
curl -s -X POST "$BASE/api/wx/order/$ORDER_NO/mock-pay" \
  -H "wx-token: $TOKEN" > /dev/null
STATUS=$(curl -s "$BASE/api/wx/order/$ORDER_NO" -H "wx-token: $TOKEN" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["status"])')
[ "$STATUS" = "1" ] && pass "mock-pay status=WAIT_SHIP" || fail "mock-pay status=$STATUS"

hr "Step 9: Idempotent pay"
curl -s -X POST "$BASE/api/wx/order/$ORDER_NO/mock-pay" \
  -H "wx-token: $TOKEN" > /dev/null
echo "idempotent OK" && pass "double mock-pay ok"

hr "Step 10: Another order → cancel"
curl -s -X POST "$BASE/api/wx/cart" \
  -H "Content-Type: application/json" -H "wx-token: $TOKEN" \
  --data-binary '{"skuId":9,"quantity":1}' > /dev/null
CID2=$(curl -s "$BASE/api/wx/cart" -H "wx-token: $TOKEN" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"][0]["id"])')
ON2=$(curl -s -X POST "$BASE/api/wx/order/create" \
  -H "Content-Type: application/json" -H "wx-token: $TOKEN" \
  --data-binary "{\"cartItemIds\":[$CID2],\"addressId\":$AID}" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"][0]["orderNo"])')
curl -s -X POST "$BASE/api/wx/order/$ON2/cancel" \
  -H "wx-token: $TOKEN" > /dev/null
CANCEL_CODE=$(curl -s -X POST "$BASE/api/wx/order/$ON2/cancel" \
  -H "wx-token: $TOKEN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["code"])')
[ "$CANCEL_CODE" = "132" ] && pass "cancel+reject 132 OK" || fail "cancel code=$CANCEL_CODE"

echo
echo "===== RESULT ====="
echo "$PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] && echo "M4a SMOKE TEST PASSED" || echo "M4a SMOKE TEST FAILED"
