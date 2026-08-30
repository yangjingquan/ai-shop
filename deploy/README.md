# deploy

Jenkins 会把 `deploy/` 中准备好的发布产物同步到服务器 `/opt/shop`；请在该目录执行 Compose。后台前端由 Jenkins 构建为 `admin-dist/`，随后与 Nginx 配置一起打包进镜像。

## 结构

```
deploy/
├── admin-app/            # 后台管理接口服务 Dockerfile + jar
├── wx-app/               # 小程序接口服务 Dockerfile + jar
├── init/                 # MySQL 初始化钩子的说明目录；结构由 Flyway 迁移管理
├── nginx/
│   ├── Dockerfile        # 构建并托管后台管理前端的 Nginx 镜像
│   ├── conf.d/*.conf     # console / conapi / miniapi 三个域名
│   └── ssl/              # 仅含说明；生产 TLS 文件放在 /etc/shop/tls
├── uploads/              # 后端上传文件持久化
├── logs/                 # 三个组件的日志目录
├── docker-compose.yml
├── .env.template         # 拷贝为 .env 并按需修改
└── README.md
```

## 首次启动

```bash
cp .env.template .env         # 按需修改密码
docker compose up -d
docker compose logs -f
```

## 微信支付凭据主密钥

两个 Java 服务必须使用同一个 `SHOP_PAYMENT_CREDENTIAL_ENCRYPTION_KEY`，用于加密数据库中每个商户的 API v3 密钥、`apiclient_key.pem` 内容和微信支付公钥。首次部署前，在服务器的 `deploy/.env` 中设置一次：

```bash
openssl rand -base64 32
# 将输出完整地填入：SHOP_PAYMENT_CREDENTIAL_ENCRYPTION_KEY=...
```

不要把该值、API v3 密钥或商户私钥提交到仓库；备份主密钥并保持不变。丢失或更换主密钥后，既有商户支付凭据将无法解密，需重新在运营后台填写。

在运营后台为每个商户分别填写：小程序 AppID、支付商户号、API v3 密钥、商户 API 证书序列号、完整 `apiclient_key.pem` 内容，以及该商户专属的 HTTPS 回调地址。若已切换微信支付公钥验签，还需填写微信支付公钥及公钥 ID：

```
https://你的支付回调域名/api/callback/wxpay/该商户代码
```

系统会在保存时校验该地址，并将 API v3 密钥和 PEM 以 AES-GCM 密文保存；输入框留空不会覆盖已保存的密钥。全部配置完整后再启用“微信支付”。

升级既有数据库时，先执行（或让 Flyway 执行）`V18__merchant_wxpay_api_v3_config.sql`，然后重新填写每个商户的支付密钥，不支持把旧明文直接继续使用。

## 后续更新

```bash
# Jenkins 已构建并同步 jar 与 admin-dist 后，在 /opt/shop 执行：
docker compose up -d --build shop-admin-app shop-wx-app nginx
```

`deploy/nginx/Dockerfile` 会把 Jenkins 生成的 `admin-dist/` 与 Nginx 配置打包进同一个镜像。不要将 `admin-dist` 作为运行时挂载；若首页或其引用的静态资源缺失，镜像构建会失败，旧容器会继续运行而不是发布 403 页面。

部署前必须通过 secret store 或受限文件挂载提供三个 Nginx 私钥：`console.nexbyte.top.key`、`conapi.nexbyte.top.key`、`miniapi.nexbyte.top.key`。生产环境统一放在发布目录之外的 `/etc/shop/tls`，并在 `.env` 中配置 `SHOP_TLS_DIR=/etc/shop/tls`。Compose 会将该目录只读挂载到容器 `/etc/nginx/ssl`；代码发布同步不会触碰它。私钥不得提交 Git；首次上线前请轮换仓库历史中曾出现过的旧私钥。

每次发布前先执行 TLS 预检，确认文件存在、可读且证书与私钥匹配：

```bash
bash verify-tls.sh
docker compose up -d --build shop-admin-app shop-wx-app nginx
```

`verify-tls.sh` 失败时不要启动或重建 Nginx；先补齐 `/etc/shop/tls` 中的六个文件。

生产环境不要使用迁移文件中的测试账号密码。可临时通过 secret store 设置 `SHOP_BOOTSTRAP_ADMIN_PASSWORD` 和 `SHOP_BOOTSTRAP_MERCHANT_PASSWORD` 完成首次登录并立即重置密码，重置后删除这两个环境变量。

## 域名

- https://console.nexbyte.top → Nginx 镜像内静态托管后台管理前端
- https://conapi.nexbyte.top  → 反代 shop-admin-app:8081
- https://miniapi.nexbyte.top → 反代 shop-wx-app:8082（TLS 由 nginx 终止）
