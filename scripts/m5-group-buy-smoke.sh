#!/usr/bin/env bash
set -euo pipefail

ADMIN_BASE=${ADMIN_BASE:-http://localhost:8081}
WX_BASE=${WX_BASE:-http://localhost:8082}
SUFFIX=$(date +%s)

hr() { echo; echo "=== $* ==="; }

hr "Login merchant"
MTOKEN=$(curl -s -X POST "$ADMIN_BASE/api/merchant/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"merchant01","password":"123456"}' | jq -r '.data.token')
test -n "$MTOKEN" && test "$MTOKEN" != "null"

hr "Create group buy product"
PID=$(curl -s -X POST "$ADMIN_BASE/api/merchant/products" \
  -H "Authorization: Bearer $MTOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"M5团购商品_$SUFFIX\",\"categoryId\":10001,\"isGroupBuy\":1,\"groupBuyPrice\":69.00,\"groupBuyRequiredCount\":2,\"specs\":[{\"name\":\"规格\",\"values\":[\"默认\"]}],\"skus\":[{\"specValueIndexes\":[0],\"price\":99.00,\"stock\":10}]}" | jq -r '.data')
test -n "$PID" && test "$PID" != "null"

hr "Put product on shelf"
curl -s -X PUT "$ADMIN_BASE/api/merchant/products/$PID/status?status=1" -H "Authorization: Bearer $MTOKEN" | jq '.code'

hr "Verify public group buy list contains product"
curl -s "$WX_BASE/api/public/products/page?page=1&size=20&isGroupBuy=1" | jq --argjson pid "$PID" '[.data.list[]?.id] | index($pid) != null'

hr "Manual next steps"
echo "Use mini program dev login, open /pages/group-buy/list, open product $PID, start and join a group, then verify order statuses."
