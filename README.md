# 商城 MVP

多商家电商微信小程序 MVP。

## 子工程
- `server/` — Spring Boot 3 后端（Maven 多模块：`shop-admin-app` 后台接口、`shop-wx-app` 小程序接口）
- `admin/` — Vue 3 管理后台（运营 + 商家共用）
- `miniap/` — 原生微信小程序（C 端）
- `docker/` — 本地基础设施（MySQL on 3306、Redis on 6380）

## 本地启动

```bash
# 1. 起基础设施（首次；复制后先把 .env 中的所有密码替换为随机值）
cd docker && cp .env.template .env
# 编辑 .env，替换所有 <...> 占位符后再执行：
docker compose up -d

# 2. 起后台管理接口服务（admin/merchant，端口 8081）
cd ../server && set -a && source ../docker/.env && set +a
./mvnw -pl shop-admin-app -am install -DskipTests
./mvnw -pl shop-admin-app spring-boot:run -Dspring-boot.run.profiles=dev

# 3. 另开终端，起小程序接口服务（wx/public/callback，端口 8082）
cd server && set -a && source ../docker/.env && set +a
./mvnw -pl shop-wx-app -am install -DskipTests
./mvnw -pl shop-wx-app spring-boot:run -Dspring-boot.run.profiles=dev

# 4. 起管理后台（本地前端固定端口 5180）
cd ../admin && pnpm install && pnpm dev

# 5. miniap 用微信开发者工具导入 miniap/ 目录
```

## 小程序多商户配置

- 小程序不再写死商户编码；启动时读取当前编译包的 AppID，后端通过商户微信配置自动解析商户。
- 每个商户仍需使用其后台配置的 AppID/AppSecret 分别构建小程序包，这是微信 `wx.login` 的应用边界。
- 小程序默认请求生产地址。开发联调可在开发者工具控制台执行 `wx.setStorageSync('shop_api_env', 'dev')` 后重新启动，或通过 `extConfig` 提供 `env`、`baseUrl`、`merchantCode` 覆盖值。
- 商户微信配置中的 AppID 必须唯一，否则无法确定当前小程序所属商户。

## 文档
- 设计：`docs/superpowers/specs/`
- 实施计划：`docs/superpowers/plans/`
