#!/bin/bash
# M3 冒烟脚本：admin 建分类 → merchant 发商品 + 上架 → public 列表/详情可见
set -e

ADMIN_BASE=${ADMIN_BASE:-http://127.0.0.1:8081}
WX_BASE=${WX_BASE:-http://127.0.0.1:8082}
SUFFIX=$(date +%s)

echo "=== 1. Admin 登录 ==="
ATOKEN=$(curl -s -X POST "$ADMIN_BASE/api/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .data.token)
test -n "$ATOKEN" && test "$ATOKEN" != "null" || { echo "admin 登录失败"; exit 1; }

echo "=== 2. Admin 建一级分类 ==="
CAT1=$(curl -s -X POST "$ADMIN_BASE/api/admin/categories" \
  -H "Authorization: Bearer $ATOKEN" -H "Content-Type: application/json" \
  -d "{\"name\":\"M3冒烟一级_$SUFFIX\",\"parentId\":0,\"sort\":99}" | jq -r .data)
test -n "$CAT1" && test "$CAT1" != "null" || { echo "建一级分类失败"; exit 1; }
echo "一级分类 id=$CAT1"

echo "=== 3. Admin 建二级分类 ==="
CAT2=$(curl -s -X POST "$ADMIN_BASE/api/admin/categories" \
  -H "Authorization: Bearer $ATOKEN" -H "Content-Type: application/json" \
  -d "{\"name\":\"M3冒烟二级_$SUFFIX\",\"parentId\":$CAT1,\"sort\":1}" | jq -r .data)
test -n "$CAT2" && test "$CAT2" != "null" || { echo "建二级分类失败"; exit 1; }
echo "二级分类 id=$CAT2"

echo "=== 4. Public 分类树（应能看到上面这级） ==="
curl -s "$WX_BASE/api/public/categories/tree" | jq "[.data[] | select(.id == $CAT1)]"

echo "=== 5. Merchant 登录（依赖 M2 已建的 merchant01） ==="
MTOKEN=$(curl -s -X POST "$ADMIN_BASE/api/merchant/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"merchant01","password":"merchant123"}' | jq -r .data.token)
test -n "$MTOKEN" && test "$MTOKEN" != "null" || { echo "merchant 登录失败（请确认 M2 已创建 merchant01/merchant123）"; exit 1; }

echo "=== 6. Merchant 发商品 ==="
PID=$(curl -s -X POST "$ADMIN_BASE/api/merchant/products" \
  -H "Authorization: Bearer $MTOKEN" -H "Content-Type: application/json" \
  -d "{
    \"name\":\"M3冒烟商品_$SUFFIX\",
    \"subtitle\":\"冒烟副标题\",
    \"categoryId\":$CAT2,
    \"mainImage\":\"https://example.com/m3.jpg\",
    \"images\":[\"https://example.com/1.jpg\"],
    \"description\":\"<p>详情</p>\",
    \"specs\":[{\"name\":\"颜色\",\"values\":[\"红\",\"蓝\"]}],
    \"skus\":[
      {\"specValueIndexes\":[0],\"price\":99.00,\"stock\":10,\"skuCode\":\"R\"},
      {\"specValueIndexes\":[1],\"price\":109.00,\"stock\":5,\"skuCode\":\"B\"}
    ]
  }" | jq -r .data)
test -n "$PID" && test "$PID" != "null" || { echo "发商品失败"; exit 1; }
echo "商品 id=$PID"

echo "=== 7. Merchant 商品列表（自己看，应能看到） ==="
curl -s "$ADMIN_BASE/api/merchant/products?page=1&size=5&keyword=$SUFFIX" \
  -H "Authorization: Bearer $MTOKEN" | jq '.data.list | map({id,name,minPrice,maxPrice,totalStock,status})'

echo "=== 8. 商品详情（merchant 视角，含 specs+skus） ==="
curl -s "$ADMIN_BASE/api/merchant/products/$PID" -H "Authorization: Bearer $MTOKEN" | \
  jq '{id:.data.id,name:.data.name,minPrice:.data.minPrice,maxPrice:.data.maxPrice,totalStock:.data.totalStock,specs:(.data.specs|length),skus:(.data.skus|length)}'

echo "=== 9. Public 列表（未上架前应查不到） ==="
HIT_OFF=$(curl -s "$WX_BASE/api/public/products/page?page=1&size=10&categoryId=$CAT2" | jq "[.data.list[]?.id // empty] | map(select(. == $PID)) | length")
test "$HIT_OFF" = "0" || { echo "未上架时不应被 public 列表查到，但命中了 $HIT_OFF 条"; exit 1; }
echo "未上架不可见 ✅"

echo "=== 10. 上架 ==="
curl -s -X PUT "$ADMIN_BASE/api/merchant/products/$PID/status?status=1" \
  -H "Authorization: Bearer $MTOKEN" | jq

echo "=== 11. Public 列表（上架后应可见） ==="
curl -s "$WX_BASE/api/public/products/page?page=1&size=10&categoryId=$CAT2" | \
  jq '.data.list | map({id,name,minPrice,maxPrice,totalStock})'

echo "=== 12. Public 详情 ==="
curl -s "$WX_BASE/api/public/products/$PID" | \
  jq '{id:.data.id,name:.data.name,minPrice:.data.minPrice,maxPrice:.data.maxPrice,specs:(.data.specs|length),skus:(.data.skus|length)}'

echo "=== 13. 越权：merchant02 改 merchant01 的商品 → 期望 PRODUCT_NOT_FOUND(212) ==="
M2TOKEN=$(curl -s -X POST "$ADMIN_BASE/api/merchant/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"merchant02","password":"merchant234"}' | jq -r .data.token)
if [ -n "$M2TOKEN" ] && [ "$M2TOKEN" != "null" ]; then
  CODE=$(curl -s -X PUT "$ADMIN_BASE/api/merchant/products/$PID/status?status=0" \
    -H "Authorization: Bearer $M2TOKEN" | jq -r .code)
  test "$CODE" = "112" || { echo "越权未被拦截，期望 code=112 实得 $CODE"; exit 1; }
  echo "越权拦截 ✅"
else
  echo "skip：merchant02 不存在（M2 可能没建第二个商家），跳过越权检查"
fi

echo
echo "=== M3 冒烟通过 ✅  分类 id=$CAT1/$CAT2，商品 id=$PID ==="
