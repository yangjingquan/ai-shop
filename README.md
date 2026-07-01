# 商城 MVP

多商家电商微信小程序 MVP。

## 子工程
- `server/` — Spring Boot 3 后端（Maven 多模块：`shop-admin-app` 后台接口、`shop-wx-app` 小程序接口）
- `admin/` — Vue 3 管理后台（运营 + 商家共用）
- `miniap/` — 原生微信小程序（C 端）
- `docker/` — 本地基础设施（MySQL on 3306、Redis on 6380）

## 本地启动

```bash
# 1. 起基础设施（首次）
cd docker && cp .env.template .env && docker compose up -d

# 2. 起后台管理接口服务（admin/merchant，端口 8081）
cd ../server && ./mvnw -pl shop-admin-app -am spring-boot:run -Dspring-boot.run.profiles=dev

# 3. 另开终端，起小程序接口服务（wx/public/callback，端口 8082）
cd server && ./mvnw -pl shop-wx-app -am spring-boot:run -Dspring-boot.run.profiles=dev

# 4. 起管理后台
cd ../admin && pnpm install && pnpm dev

# 5. miniap 用微信开发者工具导入 miniap/ 目录
```

## 文档
- 设计：`docs/superpowers/specs/`
- 实施计划：`docs/superpowers/plans/`
