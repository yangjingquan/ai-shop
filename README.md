# 商城 MVP

多商家电商微信小程序 MVP。

## 子工程
- `server/` — Spring Boot 3 后端
- `admin/` — Vue 3 管理后台（运营 + 商家共用）
- `miniap/` — 原生微信小程序（C 端）
- `docker/` — 本地基础设施（MySQL on 3306、Redis on 6380）

## 本地启动

```bash
# 1. 起基础设施（首次）
cd docker && cp .env.template .env && docker compose up -d

# 2. 起后端
cd ../server && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. 起管理后台
cd ../admin && pnpm install && pnpm dev

# 4. miniap 用微信开发者工具导入 miniap/ 目录
```

## 文档
- 设计：`docs/superpowers/specs/`
- 实施计划：`docs/superpowers/plans/`
