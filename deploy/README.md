# deploy

服务器上保留完整仓库，部署目录为 `/opt/shop/deploy`。请在该目录执行 Compose；Nginx 镜像构建会读取上一级的 `admin/` 源码并在镜像内生成前端产物。

## 结构

```
deploy/
├── admin-app/            # 后台管理接口服务 Dockerfile + jar
├── wx-app/               # 小程序接口服务 Dockerfile + jar
├── init/                 # 首次启动 MySQL 时导入的 sql（dump）
├── nginx/
│   ├── Dockerfile        # 构建并托管后台管理前端的 Nginx 镜像
│   ├── conf.d/*.conf     # console / conapi / miniapi 三个域名
│   └── ssl/              # miniapi.nexbyte.top.{pem,key}
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

两个 Java 服务必须使用同一个 `SHOP_PAYMENT_CREDENTIAL_ENCRYPTION_KEY`，用于加密数据库中每个商户的 API v3 密钥和 `apiclient_key.pem` 内容。首次部署前，在服务器的 `deploy/.env` 中设置一次：

```bash
openssl rand -base64 32
# 将输出完整地填入：SHOP_PAYMENT_CREDENTIAL_ENCRYPTION_KEY=...
```

不要把该值、API v3 密钥或商户私钥提交到仓库；备份主密钥并保持不变。丢失或更换主密钥后，既有商户支付凭据将无法解密，需重新在运营后台填写。

在运营后台为每个商户分别填写：小程序 AppID、支付商户号、API v3 密钥、商户 API 证书序列号、完整 `apiclient_key.pem` 内容，以及该商户专属的 HTTPS 回调地址：

```
https://你的支付回调域名/api/callback/wxpay/该商户代码
```

系统会在保存时校验该地址，并将 API v3 密钥和 PEM 以 AES-GCM 密文保存；输入框留空不会覆盖已保存的密钥。全部配置完整后再启用“微信支付”。

升级既有数据库时，先执行（或让 Flyway 执行）`V18__merchant_wxpay_api_v3_config.sql`，然后重新填写每个商户的支付密钥，不支持把旧明文直接继续使用。

## 后续更新

```bash
# 拉取完整仓库的最新代码后，在 deploy/ 目录执行：
docker compose up -d --build shop-admin-app shop-wx-app nginx
../scripts/verify-console-deploy.sh
```

前端产物由 `deploy/nginx/Dockerfile` 在镜像构建阶段从 `admin/` 自动生成。不要再手工复制或挂载 `admin-dist`；这样一次部署中的 Nginx 配置和静态文件始终来自同一份代码。

## 域名

- https://console.nexbyte.top → Nginx 镜像内静态托管后台管理前端
- http://conapi.nexbyte.top   → 反代 shop-admin-app:8081
- https://miniapi.nexbyte.top → 反代 shop-wx-app:8082（TLS 由 nginx 终止）
