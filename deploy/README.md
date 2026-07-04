# deploy

服务器上的部署目录（同步到 `/opt/shop`）。

## 结构

```
deploy/
├── admin-app/            # 后台管理接口服务 Dockerfile + jar
├── wx-app/               # 小程序接口服务 Dockerfile + jar
├── admin-dist/           # 后台管理前端产物（nginx 静态托管）
├── init/                 # 首次启动 MySQL 时导入的 sql（dump）
├── nginx/
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

## 后续更新

```bash
# 覆盖对应 jar / dist / 配置文件后：
docker compose up -d --build shop-admin-app shop-wx-app
docker compose restart nginx
```

## 域名

- http://console.nexbyte.top  → nginx 静态托管 admin-dist
- http://conapi.nexbyte.top   → 反代 shop-admin-app:8081
- https://miniapi.nexbyte.top → 反代 shop-wx-app:8082（TLS 由 nginx 终止）
