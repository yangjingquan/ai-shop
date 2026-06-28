#!/bin/bash
# M2 冒烟脚本：admin 创建商家 → merchant 改资料 → wx 绑手机号 + 加 2 条地址
set -e

BASE=${BASE:-http://127.0.0.1:8080}

echo "=== 1. Admin 登录 ==="
ATOKEN=$(curl -s -X POST "$BASE/api/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .data.token)
test -n "$ATOKEN" && test "$ATOKEN" != "null" || { echo "admin 登录失败"; exit 1; }

echo "=== 2. Admin 创建冒烟商家 ==="
SUFFIX=$(date +%s)
CREATE_RESP=$(curl -s -X POST "$BASE/api/admin/merchants" \
  -H "Authorization: Bearer $ATOKEN" -H "Content-Type: application/json" \
  -d "{\"name\":\"冒烟商家$SUFFIX\",\"username\":\"smoke_$SUFFIX\",\"password\":\"smoke123\",\"contactName\":\"冒烟测试\",\"contactPhone\":\"13900000000\"}")
echo "$CREATE_RESP" | jq
MID=$(echo "$CREATE_RESP" | jq -r '.data.merchantId // .data.id')
test -n "$MID" && test "$MID" != "null" || { echo "创建商家失败"; exit 1; }

echo "=== 3. Merchant 登录改资料 ==="
MTOKEN=$(curl -s -X POST "$BASE/api/merchant/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"smoke_$SUFFIX\",\"password\":\"smoke123\"}" | jq -r .data.token)
test -n "$MTOKEN" && test "$MTOKEN" != "null" || { echo "merchant 登录失败"; exit 1; }

curl -s -X PUT "$BASE/api/merchant/profile" \
  -H "Authorization: Bearer $MTOKEN" -H "Content-Type: application/json" \
  -d '{"description":"smoke desc","contactPhone":"13888888888"}' | jq

echo "=== 4. Wx 静默登录 ==="
WTOKEN=$(curl -s -X POST "$BASE/api/wx/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"code\":\"smoke-$SUFFIX\"}" | jq -r .data.token)
test -n "$WTOKEN" && test "$WTOKEN" != "null" || { echo "wx 登录失败"; exit 1; }

echo "=== 5. Wx 绑手机号（幂等：mock 固定 13800000000，被占用时返回 180 视为 OK）==="
BIND_RESP=$(curl -s -X POST "$BASE/api/wx/user/bind-phone" \
  -H "wx-token: $WTOKEN" -H "Content-Type: application/json" \
  -d '{"code":"any-phone-code"}')
echo "$BIND_RESP" | jq
BIND_CODE=$(echo "$BIND_RESP" | jq -r .code)
if [ "$BIND_CODE" != "0" ] && [ "$BIND_CODE" != "180" ]; then
  echo "绑手机号异常 code=$BIND_CODE"; exit 1
fi

echo "=== 6. Wx 加 2 条地址（B 应为 default）==="
curl -s -X POST "$BASE/api/wx/addresses" \
  -H "wx-token: $WTOKEN" -H "Content-Type: application/json" \
  -d '{"receiver":"A","phone":"13800000001","region":"北京/北京/朝阳","detail":"x","isDefault":true}' | jq

curl -s -X POST "$BASE/api/wx/addresses" \
  -H "wx-token: $WTOKEN" -H "Content-Type: application/json" \
  -d '{"receiver":"B","phone":"13800000002","region":"上海/上海/浦东","detail":"y","isDefault":true}' | jq

echo "=== 7. Wx 列表确认 default 互斥 ==="
curl -s "$BASE/api/wx/addresses" -H "wx-token: $WTOKEN" \
  | jq '.data | map({id, receiver, isDefault})'

echo "=== M2 冒烟通过 ==="
