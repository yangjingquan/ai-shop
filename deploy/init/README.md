# 初始化目录

此目录不再存放数据库结构或业务数据的 SQL dump。生产环境的数据库结构完全由应用启动时的 Flyway 迁移（`server/shop-domain/src/main/resources/db/migration`）创建和升级。

请不要在此目录提交包含账号、支付配置、客户数据或 `flyway_schema_history` 的导出文件。需要恢复生产数据时，应使用受访问控制的备份系统，而不是随镜像或部署仓库分发。
