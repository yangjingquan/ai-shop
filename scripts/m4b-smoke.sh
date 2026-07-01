#!/usr/bin/env bash
set -e
ADMIN_BASE=${ADMIN_BASE:-http://localhost:8081}
WX_BASE=${WX_BASE:-http://localhost:8082}
PASS=0; FAIL=0
hr() { echo; echo "===== $* ====="; }
pass() { echo "  [PASS] $*"; PASS=$((PASS+1)); }
fail() { echo "  [FAIL] $*"; FAIL=$((FAIL+1)); }

hr "Step 1: wx dev-login"
WX_TOKEN=$(curl -s -X POST "$WX_BASE/api/wx/auth/dev-login" -H 'Content-Type: application/json' --data-binary '{"userId":3}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["token"])')
pass "wx dev-login"

hr "Step 2: merchant login"
M_TOKEN=$(curl -s -X POST "$ADMIN_BASE/api/merchant/auth/login" -H 'Content-Type: application/json' --data-binary '{"username":"merchant01","password":"merchant123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["token"])')
pass "merchant login"

hr "Step 3: create order + pay → WAIT_SHIP"
AID=$(curl -s "$WX_BASE/api/wx/addresses" -H "wx-token: $WX_TOKEN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"][0]["id"])')
curl -s -X POST "$WX_BASE/api/wx/cart" -H 'Content-Type: application/json' -H "wx-token: $WX_TOKEN" --data-binary '{"skuId":9,"quantity":1}' > /dev/null
CID=$(curl -s "$WX_BASE/api/wx/cart" -H "wx-token: $WX_TOKEN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"][0]["id"])')
ON=$(curl -s -X POST "$WX_BASE/api/wx/order/create" -H 'Content-Type: application/json' -H "wx-token: $WX_TOKEN" --data-binary "{\"cartItemIds\":[$CID],\"addressId\":$AID}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"][0]["orderNo"])')
curl -s -X POST "$WX_BASE/api/wx/order/$ON/mock-pay" -H "wx-token: $WX_TOKEN" > /dev/null
pass "order created+paid → $ON"

hr "Step 4: ship → WAIT_RECEIVE"
curl -s -X POST "$ADMIN_BASE/api/merchant/order/ship?orderNo=$ON" -H 'Content-Type: application/json' -H "Authorization: Bearer $M_TOKEN" --data-binary '{"shipNo":"SF12345678"}' > /dev/null
ST=$(curl -s "$WX_BASE/api/wx/order/$ON" -H "wx-token: $WX_TOKEN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["status"])')
[ "$ST" = "2" ] && pass "ship → WAIT_RECEIVE(2)" || fail "status=$ST"

hr "Step 5: confirm-receive → FINISHED"
curl -s -X POST "$WX_BASE/api/wx/order/$ON/confirm-receive" -H "wx-token: $WX_TOKEN" > /dev/null
ST=$(curl -s "$WX_BASE/api/wx/order/$ON" -H "wx-token: $WX_TOKEN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["status"])')
[ "$ST" = "3" ] && pass "confirm → FINISHED(3)" || fail "status=$ST"

hr "Step 6: refund apply + approve"
curl -s -X POST "$WX_BASE/api/wx/cart" -H 'Content-Type: application/json' -H "wx-token: $WX_TOKEN" --data-binary '{"skuId":9,"quantity":1}' > /dev/null
CID3=$(curl -s "$WX_BASE/api/wx/cart" -H "wx-token: $WX_TOKEN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"][0]["id"])')
ON3=$(curl -s -X POST "$WX_BASE/api/wx/order/create" -H 'Content-Type: application/json' -H "wx-token: $WX_TOKEN" --data-binary "{\"cartItemIds\":[$CID3],\"addressId\":$AID}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"][0]["orderNo"])')
curl -s -X POST "$WX_BASE/api/wx/order/$ON3/mock-pay" -H "wx-token: $WX_TOKEN" > /dev/null
curl -s -X POST "$WX_BASE/api/wx/order/$ON3/refund" -H 'Content-Type: application/json' -H "wx-token: $WX_TOKEN" --data-binary '{"reason":"test refund"}' > /dev/null
RID=$(curl -s "$ADMIN_BASE/api/merchant/refund/list?page=1&size=5" -H "Authorization: Bearer $M_TOKEN" | python3 -c 'import sys,json;d=json.load(sys.stdin)["data"];print(d[0]["id"] if d else "NONE")')
curl -s -X POST "$ADMIN_BASE/api/merchant/refund/$RID/approve" -H 'Content-Type: application/json' -H "Authorization: Bearer $M_TOKEN" --data-binary '{"approved":true}' > /dev/null
ST=$(curl -s "$WX_BASE/api/wx/order/$ON3" -H "wx-token: $WX_TOKEN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["status"])')
[ "$ST" = "4" ] && pass "refund approved → CANCELLED(4)" || fail "status=$ST"

hr "Step 7: duplicate refund → 191"
CODE=$(curl -s -X POST "$WX_BASE/api/wx/order/$ON3/refund" -H 'Content-Type: application/json' -H "wx-token: $WX_TOKEN" --data-binary '{"reason":"again"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["code"])')
[ "$CODE" = "191" ] && pass "duplicate → REFUND_ALREADY_EXISTS(191)" || fail "code=$CODE"

hr "Step 8: merchant order page"
TOTAL=$(curl -s "$ADMIN_BASE/api/merchant/order/page?page=1&size=10" -H "Authorization: Bearer $M_TOKEN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["total"])')
[ "$TOTAL" -ge 1 ] && pass "merchant page total=$TOTAL" || fail "page empty"

echo; echo "===== RESULT ====="
echo "$PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] && echo "M4b SMOKE TEST PASSED"
