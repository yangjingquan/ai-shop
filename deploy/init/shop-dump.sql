-- MySQL dump 10.13  Distrib 8.0.46, for Linux (aarch64)
--
-- Host: localhost    Database: shop
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `shop`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `shop` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `shop`;

--
-- Table structure for table `admin_user`
--

DROP TABLE IF EXISTS `admin_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'admin',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin_user`
--

LOCK TABLES `admin_user` WRITE;
/*!40000 ALTER TABLE `admin_user` DISABLE KEYS */;
INSERT INTO `admin_user` VALUES (1,'admin','$2a$10$rh5wna6Xhb3tKVmJK3EKpuZArt31w5oqNGiDzowuYqRYyGX8ay6FK','admin','2026-06-17 14:57:01','2026-06-17 15:32:37',0);
/*!40000 ALTER TABLE `admin_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `banner`
--

DROP TABLE IF EXISTS `banner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `banner` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `image_url` varchar(500) NOT NULL,
  `link_type` tinyint NOT NULL DEFAULT '0' COMMENT '0=NONE 1=PRODUCT 2=CATEGORY 3=URL',
  `link_value` varchar(500) DEFAULT '',
  `sort` int NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1=显示 0=隐藏',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`,`sort`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页轮播图';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `banner`
--

LOCK TABLES `banner` WRITE;
/*!40000 ALTER TABLE `banner` DISABLE KEYS */;
INSERT INTO `banner` VALUES (1,'/uploads/20260628/0f62892d-1b85-4f7b-8e3b-76486efcc2b4.jpg',1,'3',1,1,'2026-06-21 11:14:49','2026-06-21 11:14:49',0),(2,'/uploads/20260628/8150f0f0-e475-4247-899b-ffd44333967c.jpg',3,'https://www.baidu.com',1,1,'2026-06-22 09:00:10','2026-06-22 09:00:10',0),(5,'/uploads/test-banner.jpg',1,'/pages/home/index',99,1,'2026-06-28 10:51:06','2026-06-28 10:57:11',1);
/*!40000 ALTER TABLE `banner` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cart_item`
--

DROP TABLE IF EXISTS `cart_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `merchant_id` bigint unsigned NOT NULL COMMENT '冗余,便于按商家 group',
  `product_id` bigint unsigned NOT NULL COMMENT '冗余,便于联表',
  `sku_id` bigint unsigned NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`,`deleted`),
  KEY `idx_user_sku` (`user_id`,`sku_id`,`deleted`) COMMENT '加购 dedup'
) ENGINE=InnoDB AUTO_INCREMENT=81 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart_item`
--

LOCK TABLES `cart_item` WRITE;
/*!40000 ALTER TABLE `cart_item` DISABLE KEYS */;
INSERT INTO `cart_item` VALUES (1,3,1,3,9,1,'2026-06-19 06:16:50','2026-06-19 06:16:50',1),(2,3,1,3,9,2,'2026-06-20 11:02:16','2026-06-20 11:02:30',1),(3,3,1,3,9,2,'2026-06-20 11:05:04','2026-06-20 11:05:04',1),(4,3,1,3,9,1,'2026-06-20 11:07:51','2026-06-20 11:07:50',1),(15,3,1,3,9,1,'2026-06-20 11:15:38','2026-06-20 11:15:37',1),(16,3,1,3,9,1,'2026-06-20 11:15:38','2026-06-20 11:15:38',1),(17,3,1,3,9,1,'2026-06-20 14:54:24','2026-06-20 14:54:24',1),(18,3,1,3,9,1,'2026-06-20 14:54:25','2026-06-20 14:54:24',1),(19,3,1,3,9,1,'2026-06-20 14:54:37','2026-06-20 14:54:36',1),(20,3,1,3,9,1,'2026-06-20 14:54:37','2026-06-20 14:54:36',1),(21,3,1,3,9,1,'2026-06-20 14:59:28','2026-06-20 14:59:28',1),(22,3,1,3,9,1,'2026-06-20 14:59:28','2026-06-20 14:59:28',1),(23,3,1,3,9,1,'2026-06-20 15:00:52','2026-06-20 15:00:51',1),(24,3,1,3,9,1,'2026-06-20 15:00:52','2026-06-20 15:00:51',1),(25,3,1,3,9,1,'2026-06-20 15:01:46','2026-06-20 15:01:45',1),(26,3,1,3,9,3,'2026-06-20 15:01:46','2026-06-20 15:02:05',1),(27,3,1,3,9,1,'2026-06-20 15:02:06','2026-06-20 15:02:05',1),(48,3,1,3,9,1,'2026-06-21 11:14:49','2026-06-21 11:14:48',1),(49,3,1,3,9,1,'2026-06-22 08:59:16','2026-06-22 08:59:15',1),(50,3,1,3,9,1,'2026-06-22 09:00:10','2026-06-22 09:00:10',1),(61,48,1,3,83,10,'2026-06-27 17:57:34','2026-06-27 17:57:34',0),(62,50,1,3,84,1,'2026-06-28 11:07:27','2026-06-28 11:07:27',0),(63,51,1,3,84,1,'2026-06-28 11:08:47','2026-06-28 11:08:47',0),(64,52,1,3,84,1,'2026-06-28 11:14:43','2026-06-28 11:14:43',0),(65,53,1,3,84,3,'2026-06-28 11:15:39','2026-06-28 11:15:39',0),(66,54,1,3,84,1,'2026-06-28 11:21:33','2026-06-28 11:23:08',1),(67,59,1,2,88,1,'2026-06-28 12:45:21','2026-06-28 12:46:15',1),(68,61,1,68,303,2,'2026-06-28 22:53:14','2026-06-28 22:53:14',0),(69,61,1,3,92,1,'2026-06-28 23:03:32','2026-06-28 23:03:32',0),(70,61,1,68,304,2,'2026-06-28 23:05:11','2026-06-28 23:19:25',1);
/*!40000 ALTER TABLE `cart_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `parent_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '0 表示一级',
  `name` varchar(32) NOT NULL,
  `icon` varchar(255) DEFAULT '',
  `level` tinyint NOT NULL COMMENT '1 一级 / 2 二级',
  `sort` int DEFAULT '0',
  `status` tinyint(1) DEFAULT '1' COMMENT '1 启用 / 0 禁用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`,`deleted`),
  KEY `idx_level_sort` (`level`,`sort`)
) ENGINE=InnoDB AUTO_INCREMENT=186 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `category`
--

LOCK TABLES `category` WRITE;
/*!40000 ALTER TABLE `category` DISABLE KEYS */;
INSERT INTO `category` VALUES (1,0,'数码','',1,1,1,'2026-06-18 18:29:33','2026-06-18 18:29:33',0),(2,1,'手机','',2,1,1,'2026-06-18 18:29:33','2026-06-18 18:29:33',0),(3,0,'上衣','',1,99,1,'2026-06-18 20:08:31','2026-06-18 20:08:31',0),(4,3,'短袖','',2,1,1,'2026-06-18 20:08:31','2026-06-18 20:08:31',0),(5,0,'下装','',1,99,1,'2026-06-18 20:10:48','2026-06-18 20:10:48',0),(6,5,'短裤','',2,1,1,'2026-06-18 20:10:48','2026-06-18 20:10:48',0),(34,0,'xss_test_cat','',1,1,1,'2026-06-22 09:00:11','2026-06-28 22:06:24',1);
/*!40000 ALTER TABLE `category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','init login tables','SQL','V1__init_login_tables.sql',719648006,'shop','2026-06-17 06:57:01',36,1),(2,'2','seed bootstrap data','SQL','V2__seed_bootstrap_data.sql',-174852946,'shop','2026-06-17 07:54:35',6,1),(3,'3','user address and merchant extend','SQL','V3__user_address_and_merchant_extend.sql',-761840230,'shop','2026-06-17 13:27:58',40,1),(4,'4','product schema','SQL','V4__product_schema.sql',-260714167,'shop','2026-06-18 10:22:31',60,1),(5,'5','cart order schema','SQL','V5__cart_order_schema.sql',1036070658,'shop','2026-06-18 19:19:06',47,1),(6,'6','refund application','SQL','V6__refund_application.sql',820159213,'shop','2026-06-20 06:51:47',12,1),(7,'7','banner oplog','SQL','V7__banner_oplog.sql',-208927240,'shop','2026-06-21 03:14:17',23,1),(8,'8','merchant wechat credentials','SQL','V8__merchant_wechat_credentials.sql',-655927106,'shop','2026-06-28 05:19:04',13,1),(9,'9','merchant code','SQL','V9__merchant_code.sql',1098733011,'shop','2026-06-28 06:23:30',30,1),(10,'10','backfill empty merchant code','SQL','V10__backfill_empty_merchant_code.sql',-1075670021,'shop','2026-06-28 06:23:30',2,1),(11,'11','expand and recode merchant code','SQL','V11__expand_and_recode_merchant_code.sql',852207997,'shop','2026-06-28 12:47:46',51,1),(12,'12','merchant wx mch id','SQL','V12__merchant_wx_mch_id.sql',2132122091,'shop','2026-06-30 13:12:41',14,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `merchant`
--

DROP TABLE IF EXISTS `merchant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_code` varchar(32) NOT NULL DEFAULT '' COMMENT '商户代码，安全随机生成的对外商户编号',
  `name` varchar(64) NOT NULL,
  `logo` varchar(255) DEFAULT '',
  `description` varchar(500) DEFAULT '' COMMENT '店铺简介',
  `address` varchar(255) DEFAULT '' COMMENT '店铺地址',
  `contact_name` varchar(32) DEFAULT '',
  `contact_phone` varchar(20) DEFAULT '',
  `wx_app_id` varchar(64) DEFAULT '' COMMENT '微信小程序 AppID',
  `wx_secret` varchar(128) DEFAULT '' COMMENT '微信小程序 AppSecret',
  `wx_mch_id` varchar(32) DEFAULT '' COMMENT '微信支付商户号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1=正常 0=冻结',
  `created_by_admin_id` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_code` (`merchant_code`)
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `merchant`
--

LOCK TABLES `merchant` WRITE;
/*!40000 ALTER TABLE `merchant` DISABLE KEYS */;
INSERT INTO `merchant` VALUES (1,'M0000000001','测试商家','','主营数码','','张三','13900000099','wx8ebb7eaaac795c54','738b480c646a63dab2f90d1fbe57767b','1595736981',1,1,'2026-06-17 14:57:01','2026-06-28 20:47:46',0),(2,'M0000000002','测试商家2','','二号店','北京朝阳','李四','13900000002','','','',0,1,'2026-06-17 22:31:42','2026-06-28 20:47:46',0),(3,'M0000000003','冒烟商家1781775903','','','','冒烟测试','13900000000','','','',1,1,'2026-06-18 17:45:03','2026-06-28 20:47:46',0),(4,'M0000000004','冒烟商家1781775925','','smoke desc','','冒烟测试','13888888888','','','',1,1,'2026-06-18 17:45:26','2026-06-28 20:47:46',0),(5,'M0000000005','冒烟商家1781776090','','smoke desc','','冒烟测试','13888888888','','','',1,1,'2026-06-18 17:48:11','2026-06-28 20:47:46',0),(6,'M0000000006','冒烟商家17817764241','','smoke desc','','冒烟测试','13888888888','','','',1,1,'2026-06-18 17:53:44','2026-06-28 20:47:46',0),(7,'M0000000007','越权smoke1781781051','','','','smoke','13900000000','','','',1,1,'2026-06-18 19:10:52','2026-06-28 20:47:46',0),(16,'M0000000010','小小男装店','/uploads/20260628/61d329d8-1e27-4afb-9e46-4871942c0b8b.jpg','一个卖精致男装的小店','沈阳','孙小小','18988990098','','','',1,1,'2026-06-28 13:48:06','2026-06-28 20:47:46',0);
/*!40000 ALTER TABLE `merchant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `merchant_user`
--

DROP TABLE IF EXISTS `merchant_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchant_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL,
  `username` varchar(64) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'merchant',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=58 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `merchant_user`
--

LOCK TABLES `merchant_user` WRITE;
/*!40000 ALTER TABLE `merchant_user` DISABLE KEYS */;
INSERT INTO `merchant_user` VALUES (1,1,'merchant01','$2a$10$0VpzXiThga7aTISvjSOteuE9UKxIO6CQo/mpl5kk9jrpuiaaMiLIe','merchant','2026-06-17 14:57:01','2026-06-17 15:32:37',0),(3,2,'merchant02','$2a$10$oIR/mNGD81.K6SmERkTdJOe.u7MQlsy8KWPm3TfyawxCwMdyWtWNC','merchant','2026-06-17 22:31:42','2026-06-17 22:31:42',0),(4,3,'smoke_1781775903','$2a$10$C46s7uL5HGix7wY9P70v5u4luFJNd/ypYSeKorP5QF.5B4te2VVoC','merchant','2026-06-18 17:45:03','2026-06-18 17:45:03',0),(5,4,'smoke_1781775925','$2a$10$73Zepuqd2rC69xlDFKKLhu7bZEAt1p/fvwKffoKwrHbbjN4XYeoB.','merchant','2026-06-18 17:45:26','2026-06-18 17:45:26',0),(6,5,'smoke_1781776090','$2a$10$AQtgfemndvhuz7IQEpQFQe0o5TTF9sLizcYVc8nYLIUaxkzXkvzKS','merchant','2026-06-18 17:48:11','2026-06-18 17:48:11',0),(7,6,'smoke_1781776424','$2a$10$AJT98YvNMC4dnHMyU4j7Ge4jpPyDe5LQd91rI236EX4NKAh4naSda','merchant','2026-06-18 17:53:44','2026-06-18 17:53:44',0),(8,7,'smoke_1781781051','$2a$10$puY1tzWhYVx1GBdTFli3leo3UMzvq864ltBjPofv.YtIgojNcyITK','merchant','2026-06-18 19:10:52','2026-06-18 19:10:52',0),(17,16,'xxnz','$2a$10$aDeydRiPdcs95rqAi01S1O0JUe0MQ9l7YSjrs0Y3o8SM5J420dmsi','merchant','2026-06-28 13:48:06','2026-06-28 13:48:06',0);
/*!40000 ALTER TABLE `merchant_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `op_log`
--

DROP TABLE IF EXISTS `op_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `op_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `operator_type` tinyint NOT NULL COMMENT '1=ADMIN 2=MERCHANT 3=WX',
  `operator_id` bigint unsigned NOT NULL,
  `action` varchar(64) NOT NULL,
  `target_type` varchar(32) DEFAULT '',
  `target_id` varchar(64) DEFAULT '',
  `payload` json DEFAULT NULL COMMENT '入参快照',
  `ip` varchar(45) DEFAULT '',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operator` (`operator_type`,`operator_id`),
  KEY `idx_action_time` (`action`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `op_log`
--

LOCK TABLES `op_log` WRITE;
/*!40000 ALTER TABLE `op_log` DISABLE KEYS */;
INSERT INTO `op_log` VALUES (1,2,1,'ORDER_SHIP','ORDER','','{\"req\": {\"shipNo\": \"SF88887777\"}, \"orderNo\": \"26062208591500032220\"}','0:0:0:0:0:0:0:1','2026-06-22 08:59:15'),(2,2,1,'ORDER_SHIP','ORDER','','{\"req\": {\"shipNo\": \"SF88887777\"}, \"orderNo\": \"26062209001000038787\"}','0:0:0:0:0:0:0:1','2026-06-22 09:00:10'),(3,2,1,'ORDER_SHIP','ORDER','','{\"req\": {\"shipNo\": \"SF44871214784412121\"}, \"orderNo\": \"26062823192500610960\"}','0:0:0:0:0:0:0:1','2026-06-29 19:12:17'),(4,2,1,'REFUND_APPROVE','REFUND','','{\"req\": {\"approved\": false, \"rejectReason\": \"不好使\"}, \"refundId\": 1}','0:0:0:0:0:0:0:1','2026-06-29 19:13:22'),(5,2,1,'ORDER_SHIP','ORDER','','{\"req\": {\"shipNo\": \"JD565765374556\"}, \"orderNo\": \"26062812461500597444\"}','0:0:0:0:0:0:0:1','2026-06-30 20:35:44');
/*!40000 ALTER TABLE `op_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order`
--

DROP TABLE IF EXISTS `order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `merchant_id` bigint unsigned NOT NULL,
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0=WAIT_PAY 1=WAIT_SHIP 2=WAIT_RECEIVE 3=FINISHED 4=CANCELLED',
  `total_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '商品小计 sum(item.subtotal)',
  `freight_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT 'M4a 永远 0,预留',
  `discount_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT 'M4a 永远 0,预留',
  `pay_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '= total + freight - discount',
  `pay_method` tinyint DEFAULT '0' COMMENT '0=未支付 1=微信支付',
  `pay_time` datetime DEFAULT NULL,
  `pay_transaction_id` varchar(64) DEFAULT '' COMMENT '微信支付订单号 / mock 时为 MOCK_xxx',
  `address_snapshot` varchar(500) NOT NULL COMMENT 'JSON 快照',
  `ship_no` varchar(64) DEFAULT '' COMMENT 'M4b 用',
  `ship_time` datetime DEFAULT NULL COMMENT 'M4b 用',
  `finish_time` datetime DEFAULT NULL COMMENT 'M4b 用',
  `cancel_time` datetime DEFAULT NULL,
  `cancel_reason` varchar(32) DEFAULT '' COMMENT 'USER_CANCEL / TIMEOUT',
  `remark` varchar(255) DEFAULT '' COMMENT '用户备注,M4a 不收集',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status` (`user_id`,`status`,`deleted`),
  KEY `idx_merchant_status` (`merchant_id`,`status`,`deleted`),
  KEY `idx_status_created` (`status`,`created_at`) COMMENT '超时未支付定时任务扫'
) ENGINE=InnoDB AUTO_INCREMENT=97 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order`
--

LOCK TABLES `order` WRITE;
/*!40000 ALTER TABLE `order` DISABLE KEYS */;
INSERT INTO `order` VALUES (1,'26062011050400030612',3,1,3,198.00,0.00,0.00,198.00,1,'2026-06-20 11:06:12','MOCK_TXN_ed443abf1c9e431c9e33d8cf4e01565d','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','SF99999988','2026-06-21 11:17:26','2026-06-28 11:27:03',NULL,'','','2026-06-20 11:05:04','2026-06-28 11:27:03',0),(2,'26062011075000030329',3,1,4,99.00,0.00,0.00,99.00,0,NULL,'','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','',NULL,NULL,'2026-06-20 11:07:51','USER_CANCEL','','2026-06-20 11:07:51','2026-06-20 11:07:51',0),(9,'26062011153700037905',3,1,3,99.00,0.00,0.00,99.00,1,'2026-06-20 11:15:38','MOCK_TXN_b504c384cd5648c19a5ea534aefec42d','{\"receiver\":\"测试\",\"phone\":\"13800138000\",\"region\":\"北京 朝阳\",\"detail\":\"望京soho\"}','SF88887777','2026-06-21 11:16:26','2026-06-28 11:17:03',NULL,'','','2026-06-20 11:15:38','2026-06-28 11:17:03',0),(10,'26062011153800032819',3,1,4,99.00,0.00,0.00,99.00,0,NULL,'','{\"receiver\":\"测试\",\"phone\":\"13800138000\",\"region\":\"北京 朝阳\",\"detail\":\"望京soho\"}','',NULL,NULL,'2026-06-20 11:15:38','USER_CANCEL','','2026-06-20 11:15:38','2026-06-20 11:15:38',0),(11,'26062014542400035153',3,1,3,99.00,0.00,0.00,99.00,1,'2026-06-20 14:54:24','MOCK_TXN_0c0bb9d005004f5b83b1cd44ad1c1ba6','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','SF12345678','2026-06-20 14:54:25','2026-06-20 14:54:25',NULL,'','','2026-06-20 14:54:24','2026-06-20 14:54:25',0),(12,'26062014542400031832',3,1,4,99.00,0.00,0.00,99.00,0,NULL,'','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','',NULL,NULL,'2026-06-20 15:28:10','TIMEOUT','','2026-06-20 14:54:25','2026-06-20 14:54:25',0),(13,'26062014543600031014',3,1,3,99.00,0.00,0.00,99.00,1,'2026-06-20 14:54:37','MOCK_TXN_8171cbbcc7e94a84a989189ed37d4337','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','SF12345678','2026-06-20 14:54:37','2026-06-20 14:54:37',NULL,'','','2026-06-20 14:54:37','2026-06-20 14:54:37',0),(14,'26062014543600031031',3,1,3,99.00,0.00,0.00,99.00,1,'2026-06-20 14:54:37','MOCK_TXN_fdde6ed25a834654aed48d2d82e8ba4e','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','SF99999999','2026-06-21 11:15:35','2026-06-28 11:17:03',NULL,'','','2026-06-20 14:54:37','2026-06-28 11:17:03',0),(15,'26062014592800034459',3,1,3,99.00,0.00,0.00,99.00,1,'2026-06-20 14:59:28','MOCK_TXN_a880fc5f9b1e40688f5a8f8dceb942de','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','SF12345678','2026-06-20 14:59:28','2026-06-20 14:59:28',NULL,'','','2026-06-20 14:59:28','2026-06-20 14:59:28',0),(16,'26062014592800038236',3,1,4,99.00,0.00,0.00,99.00,1,'2026-06-20 14:59:28','MOCK_TXN_bf7979ea23b645d79a4b8ecb95e3f6cd','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','',NULL,NULL,'2026-06-20 14:59:28','REFUNDED','','2026-06-20 14:59:28','2026-06-20 14:59:28',0),(17,'26062015005100037219',3,1,3,99.00,0.00,0.00,99.00,1,'2026-06-20 15:00:52','MOCK_TXN_d2301d07da14419fac8c5d78d5ca7698','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','SF12345678','2026-06-20 15:00:52','2026-06-20 15:00:52',NULL,'','','2026-06-20 15:00:52','2026-06-20 15:00:52',0),(18,'26062015005100039983',3,1,4,99.00,0.00,0.00,99.00,1,'2026-06-20 15:00:52','MOCK_TXN_575f51ba616a4dd898f7e3c5bcb8378d','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','',NULL,NULL,'2026-06-20 15:00:52','REFUNDED','','2026-06-20 15:00:52','2026-06-20 15:00:52',0),(19,'26062015014500032213',3,1,3,99.00,0.00,0.00,99.00,1,'2026-06-20 15:01:46','MOCK_TXN_51d4d8c22dee48f490477e2a6879e24e','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','SF12345678','2026-06-20 15:01:46','2026-06-20 15:01:46',NULL,'','','2026-06-20 15:01:46','2026-06-20 15:01:46',0),(20,'26062015020500032430',3,1,3,297.00,0.00,0.00,297.00,1,'2026-06-20 15:02:06','MOCK_TXN_0de7b0889ca842dbb91f120dc95c32de','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','SF12345678','2026-06-20 15:02:06','2026-06-20 15:02:06',NULL,'','','2026-06-20 15:02:06','2026-06-20 15:02:06',0),(21,'26062015020500037129',3,1,4,99.00,0.00,0.00,99.00,1,'2026-06-20 15:02:06','MOCK_TXN_4c44190ce10944d1b892c7a7789da289','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','',NULL,NULL,'2026-06-20 15:02:06','REFUNDED','','2026-06-20 15:02:06','2026-06-20 15:02:06',0),(44,'26062111144800039375',3,1,3,99.00,0.00,0.00,99.00,1,'2026-06-21 11:14:49','MOCK_TXN_30e646c46585421d8e727b57b9968eef','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','SF12345678','2026-06-21 11:14:49','2026-06-28 11:17:03',NULL,'','','2026-06-21 11:14:49','2026-06-28 11:17:03',0),(45,'26062208591500032220',3,1,3,99.00,0.00,0.00,99.00,1,'2026-06-22 08:59:16','MOCK_TXN_d65948a6f3ef470f89739cf198839302','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','SF88887777','2026-06-22 08:59:16','2026-06-29 09:04:29',NULL,'','','2026-06-22 08:59:16','2026-06-29 09:04:29',0),(46,'26062209001000038787',3,1,3,99.00,0.00,0.00,99.00,1,'2026-06-22 09:00:11','MOCK_TXN_0a64feba85bd4d39a29319f4463dd5a7','{\"receiver\":\"张三\",\"phone\":\"13800138000\",\"region\":\"上海市 浦东新区\",\"detail\":\"世纪大道100号\"}','SF88887777','2026-06-22 09:00:11','2026-06-29 09:04:29',NULL,'','','2026-06-22 09:00:10','2026-06-29 09:04:29',0),(58,'26062811230800547832',54,1,1,109.00,0.00,0.00,109.00,1,'2026-06-28 11:23:09','MOCK_TXN_375b784f31f140679a8bc7c46ea276e5','{\"receiver\":\"小胖\",\"phone\":\"18200001111\",\"region\":\"北京市/北京市/东城区\",\"detail\":\"222\"}','',NULL,NULL,NULL,'','','2026-06-28 11:23:08','2026-06-28 11:23:08',0),(59,'26062812461500597444',59,1,2,100.00,0.00,0.00,100.00,1,'2026-06-28 12:46:15','MOCK_TXN_098532fa20ab4d47baa3f813f87111c6','{\"receiver\":\"小胖子\",\"phone\":\"19099998888\",\"region\":\"山西省/太原市/杏花岭区\",\"detail\":\"123\"}','JD565765374556','2026-06-30 20:35:44',NULL,NULL,'','','2026-06-28 12:46:15','2026-06-30 20:35:44',0),(70,'26062823192500610960',61,1,2,0.02,0.00,0.00,0.02,1,'2026-06-28 23:19:26','MOCK_TXN_0927b7046ce9495ca0030068003df1f8','{\"receiver\":\"杨小胖\",\"phone\":\"15566047217\",\"region\":\"辽宁省/沈阳市/沈河区\",\"detail\":\"北方传媒大厦808\"}','SF44871214784412121','2026-06-29 19:12:18',NULL,NULL,'','','2026-06-28 23:19:26','2026-06-29 19:12:18',0);
/*!40000 ALTER TABLE `order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_item`
--

DROP TABLE IF EXISTS `order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_id` bigint unsigned NOT NULL,
  `order_no` varchar(32) NOT NULL COMMENT '冗余,便于按 order_no 反查',
  `product_id` bigint unsigned NOT NULL,
  `sku_id` bigint unsigned NOT NULL,
  `product_name` varchar(128) NOT NULL COMMENT '快照',
  `main_image` varchar(255) DEFAULT '' COMMENT '快照',
  `spec_text` varchar(128) DEFAULT '' COMMENT '快照',
  `unit_price` decimal(10,2) NOT NULL COMMENT '快照',
  `quantity` int NOT NULL,
  `subtotal` decimal(10,2) NOT NULL COMMENT 'unit_price * quantity 冗余',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_product` (`product_id`) COMMENT '便于销量统计'
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_item`
--

LOCK TABLES `order_item` WRITE;
/*!40000 ALTER TABLE `order_item` DISABLE KEYS */;
INSERT INTO `order_item` VALUES (1,1,'26062011050400030612',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,2,198.00,'2026-06-20 11:05:04'),(2,2,'26062011075000030329',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-20 11:07:50'),(9,9,'26062011153700037905',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-20 11:15:37'),(10,10,'26062011153800032819',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-20 11:15:38'),(11,11,'26062014542400035153',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-20 14:54:24'),(12,12,'26062014542400031832',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-20 14:54:24'),(13,13,'26062014543600031014',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-20 14:54:36'),(14,14,'26062014543600031031',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-20 14:54:36'),(15,15,'26062014592800034459',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-20 14:59:28'),(16,16,'26062014592800038236',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-20 14:59:28'),(17,17,'26062015005100037219',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-20 15:00:51'),(18,18,'26062015005100039983',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-20 15:00:51'),(19,19,'26062015014500032213',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-20 15:01:45'),(20,20,'26062015020500032430',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,3,297.00,'2026-06-20 15:02:05'),(21,21,'26062015020500037129',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-20 15:02:05'),(34,44,'26062111144800039375',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-21 11:14:48'),(35,45,'26062208591500032220',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-22 08:59:15'),(36,46,'26062209001000038787',3,9,'M3冒烟商品_1781784647','https://example.com/m3.jpg','红',99.00,1,99.00,'2026-06-22 09:00:10'),(43,58,'26062811230800547832',3,84,'M3冒烟商品_1781784647','/uploads/20260626/3d182f70-9733-41a7-a35e-caad3ed806c4.jpg','蓝',109.00,1,109.00,'2026-06-28 11:23:08'),(44,59,'26062812461500597444',2,88,'短袖-男','/uploads/20260628/a5b4dbfe-99ad-4cce-9a43-cd5eabd188eb.jpg','M / 白色',100.00,1,100.00,'2026-06-28 12:46:15'),(45,70,'26062823192500610960',68,304,'Nike速干裤','/uploads/20260628/e6ab17f7-d931-4a4c-8d71-6eda0e90a6fe.jpg','红色 / XL',0.01,2,0.02,'2026-06-28 23:19:25');
/*!40000 ALTER TABLE `order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_log`
--

DROP TABLE IF EXISTS `payment_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL,
  `transaction_id` varchar(64) NOT NULL COMMENT '微信流水号 / MOCK_xxx',
  `amount` decimal(10,2) NOT NULL,
  `raw_payload` json DEFAULT NULL COMMENT '回调原始报文 / mock 时为 stub',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction` (`transaction_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付回调流水';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_log`
--

LOCK TABLES `payment_log` WRITE;
/*!40000 ALTER TABLE `payment_log` DISABLE KEYS */;
INSERT INTO `payment_log` VALUES (1,'26062011050400030612','MOCK_TXN_ed443abf1c9e431c9e33d8cf4e01565d',198.00,'{\"mock\": true, \"orderNo\": \"26062011050400030612\"}','2026-06-20 11:06:11'),(4,'26062011153700037905','MOCK_TXN_b504c384cd5648c19a5ea534aefec42d',99.00,'{\"mock\": true, \"orderNo\": \"26062011153700037905\"}','2026-06-20 11:15:37'),(5,'26062014542400035153','MOCK_TXN_0c0bb9d005004f5b83b1cd44ad1c1ba6',99.00,'{\"mock\": true, \"orderNo\": \"26062014542400035153\"}','2026-06-20 14:54:24'),(6,'26062014543600031014','MOCK_TXN_8171cbbcc7e94a84a989189ed37d4337',99.00,'{\"mock\": true, \"orderNo\": \"26062014543600031014\"}','2026-06-20 14:54:36'),(7,'26062014543600031031','MOCK_TXN_fdde6ed25a834654aed48d2d82e8ba4e',99.00,'{\"mock\": true, \"orderNo\": \"26062014543600031031\"}','2026-06-20 14:54:36'),(8,'26062014592800034459','MOCK_TXN_a880fc5f9b1e40688f5a8f8dceb942de',99.00,'{\"mock\": true, \"orderNo\": \"26062014592800034459\"}','2026-06-20 14:59:28'),(9,'26062014592800038236','MOCK_TXN_bf7979ea23b645d79a4b8ecb95e3f6cd',99.00,'{\"mock\": true, \"orderNo\": \"26062014592800038236\"}','2026-06-20 14:59:28'),(10,'26062015005100037219','MOCK_TXN_d2301d07da14419fac8c5d78d5ca7698',99.00,'{\"mock\": true, \"orderNo\": \"26062015005100037219\"}','2026-06-20 15:00:51'),(11,'26062015005100039983','MOCK_TXN_575f51ba616a4dd898f7e3c5bcb8378d',99.00,'{\"mock\": true, \"orderNo\": \"26062015005100039983\"}','2026-06-20 15:00:51'),(12,'26062015014500032213','MOCK_TXN_51d4d8c22dee48f490477e2a6879e24e',99.00,'{\"mock\": true, \"orderNo\": \"26062015014500032213\"}','2026-06-20 15:01:45'),(13,'26062015020500032430','MOCK_TXN_0de7b0889ca842dbb91f120dc95c32de',297.00,'{\"mock\": true, \"orderNo\": \"26062015020500032430\"}','2026-06-20 15:02:05'),(14,'26062015020500037129','MOCK_TXN_4c44190ce10944d1b892c7a7789da289',99.00,'{\"mock\": true, \"orderNo\": \"26062015020500037129\"}','2026-06-20 15:02:05'),(19,'26062111144800039375','MOCK_TXN_30e646c46585421d8e727b57b9968eef',99.00,'{\"mock\": true, \"orderNo\": \"26062111144800039375\"}','2026-06-21 11:14:49'),(20,'26062208591500032220','MOCK_TXN_d65948a6f3ef470f89739cf198839302',99.00,'{\"mock\": true, \"orderNo\": \"26062208591500032220\"}','2026-06-22 08:59:15'),(21,'26062209001000038787','MOCK_TXN_0a64feba85bd4d39a29319f4463dd5a7',99.00,'{\"mock\": true, \"orderNo\": \"26062209001000038787\"}','2026-06-22 09:00:10'),(24,'26062811230800547832','MOCK_TXN_375b784f31f140679a8bc7c46ea276e5',109.00,'{\"mock\": true, \"orderNo\": \"26062811230800547832\"}','2026-06-28 11:23:08'),(25,'26062812461500597444','MOCK_TXN_098532fa20ab4d47baa3f813f87111c6',100.00,'{\"mock\": true, \"orderNo\": \"26062812461500597444\"}','2026-06-28 12:46:15'),(26,'26062823192500610960','MOCK_TXN_0927b7046ce9495ca0030068003df1f8',0.02,'{\"mock\": true, \"orderNo\": \"26062823192500610960\"}','2026-06-28 23:19:25');
/*!40000 ALTER TABLE `payment_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product`
--

DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint unsigned NOT NULL,
  `category_id` bigint unsigned NOT NULL,
  `name` varchar(128) NOT NULL,
  `subtitle` varchar(255) DEFAULT '',
  `main_image` varchar(255) DEFAULT '',
  `images` json DEFAULT NULL COMMENT '详情图 URL 列表',
  `description` longtext,
  `min_price` decimal(10,2) DEFAULT '0.00',
  `max_price` decimal(10,2) DEFAULT '0.00',
  `total_stock` int DEFAULT '0',
  `total_sales` int DEFAULT '0',
  `status` tinyint(1) DEFAULT '0' COMMENT '1 上架 / 0 下架',
  `is_recommend` tinyint(1) DEFAULT '0' COMMENT '1 推荐 / 0 不推荐',
  `sort` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_status` (`merchant_id`,`status`,`deleted`),
  KEY `idx_category_status` (`category_id`,`status`,`deleted`),
  KEY `idx_status_sort` (`status`,`sort`,`deleted`),
  KEY `idx_status_recommend_sort` (`status`,`is_recommend`,`sort`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=113 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品 SPU';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product`
--

LOCK TABLES `product` WRITE;
/*!40000 ALTER TABLE `product` DISABLE KEYS */;
INSERT INTO `product` VALUES (1,1,2,'测试手机改','测试副标题','https://example.com/main.jpg','[\"https://example.com/1.jpg\"]','<p>详情</p>',6999.00,7999.00,35,0,0,0,0,'2026-06-18 18:36:36','2026-06-18 18:56:28',0),(2,1,4,'短袖-男','夏天男士短袖-多款式','/uploads/20260628/a5b4dbfe-99ad-4cce-9a43-cd5eabd188eb.jpg','[\"/uploads/20260628/d04209f5-5f6e-4b60-8275-5def6c6cdc92.jpg\", \"/uploads/20260628/d42d75ee-7bd3-4cca-9aa3-b83184c986e5.jpg\"]','<p>详情</p>',100.00,105.00,226,1,1,0,0,'2026-06-18 20:08:31','2026-06-28 12:46:15',0),(3,1,6,'短裤-男','夏季男生短裤','/uploads/20260626/3d182f70-9733-41a7-a35e-caad3ed806c4.jpg','[\"/uploads/20260626/5b8e056c-29c1-417b-b79f-2290a81da3c6.jpg\", \"/uploads/20260626/e93eb2b8-77e1-4dfb-bd3f-0b0ca920f92f.jpg\"]','<p>详情说法</p>',99.00,109.00,99,19,1,0,0,'2026-06-18 20:10:48','2026-06-28 11:23:08',0),(68,1,6,'Nike速干裤','Nike速干裤运动短裤','/uploads/20260628/e6ab17f7-d931-4a4c-8d71-6eda0e90a6fe.jpg','[\"/uploads/20260628/5f703d42-f53e-4700-924d-646b6f41c1bb.jpg\", \"/uploads/20260628/aded9b15-c28f-40a0-a5f4-14df375f841c.jpg\", \"/uploads/20260628/caf6538e-eab5-4e13-96c7-202be7c0bd66.jpg\", \"/uploads/20260628/bd90e061-b358-49a3-a958-fa448ef81a93.jpg\", \"/uploads/20260628/294b8992-ba90-4a13-add3-1b01a80fdd2e.jpg\"]','男士速干短裤',0.01,0.01,433,2,1,0,0,'2026-06-28 22:47:24','2026-06-28 23:19:25',0);
/*!40000 ALTER TABLE `product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_sku`
--

DROP TABLE IF EXISTS `product_sku`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_sku` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `product_id` bigint unsigned NOT NULL,
  `sku_code` varchar(64) DEFAULT '',
  `spec_value_ids` json DEFAULT NULL COMMENT 'spec_value.id 列表，按 spec 顺序',
  `spec_text` varchar(128) DEFAULT '' COMMENT '冗余可读，例: 黑色 / 256G',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00',
  `stock` int NOT NULL DEFAULT '0',
  `image` varchar(255) DEFAULT '',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=489 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品 SKU';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_sku`
--

LOCK TABLES `product_sku` WRITE;
/*!40000 ALTER TABLE `product_sku` DISABLE KEYS */;
INSERT INTO `product_sku` VALUES (1,1,'BLK-256','[1, 3]','黑 / 256G',7999.00,10,'','2026-06-18 18:36:36','2026-06-18 18:56:28',1),(2,1,'','[1, 4]','黑 / 512G',8999.00,8,'','2026-06-18 18:36:36','2026-06-18 18:56:28',1),(3,1,'','[2, 3]','白 / 256G',7999.00,6,'','2026-06-18 18:36:36','2026-06-18 18:56:28',1),(4,1,'','[2, 4]','白 / 512G',8999.00,5,'','2026-06-18 18:36:36','2026-06-18 18:56:28',1),(5,1,'','[5]','金',6999.00,20,'','2026-06-18 18:56:28','2026-06-18 18:56:28',0),(6,1,'DUP','[5]','金',7999.00,15,'','2026-06-18 18:56:28','2026-06-18 18:56:28',0),(7,2,'R','[6]','红',99.00,10,'','2026-06-18 20:08:31','2026-06-28 12:26:10',1),(8,2,'B','[7]','蓝',109.00,5,'','2026-06-18 20:08:31','2026-06-28 12:26:10',1),(9,3,'R','[8]','红',99.00,95,'','2026-06-18 20:10:48','2026-07-01 20:07:32',0),(10,3,'B','[9]','蓝',109.00,5,'','2026-06-18 20:10:48','2026-06-26 17:48:58',1),(83,3,'R','[78]','红',99.00,95,'/uploads/20260626/f416539f-2a13-4e0d-95be-8fae913ef88c.jpg','2026-06-26 17:48:59','2026-06-28 12:26:39',1),(84,3,'B','[79]','蓝',109.00,4,'/uploads/20260626/151a36e8-199a-4844-b4cf-300c4ee7c930.jpg','2026-06-26 17:48:59','2026-06-28 12:26:39',1),(85,2,'T0001','[80, 83]','S / 黑色',100.00,10,'/uploads/20260628/cda5ccaf-7dfd-4273-bd86-344381781342.jpg','2026-06-28 12:26:10','2026-06-28 12:26:10',0),(86,2,'T0002','[80, 84]','S / 白色',100.00,99,'/uploads/20260628/39931d93-bd77-4357-a373-51c554a15d32.jpg','2026-06-28 12:26:10','2026-06-28 12:26:10',0),(87,2,'T0003','[81, 83]','M / 黑色',100.00,58,'/uploads/20260628/eb113b3b-f29d-4373-aca1-61e76da1c5d7.jpg','2026-06-28 12:26:10','2026-06-28 12:26:10',0),(88,2,'T0004','[81, 84]','M / 白色',100.00,9,'/uploads/20260628/0028257f-b237-4df6-898d-6fb1a7b640c1.jpg','2026-06-28 12:26:10','2026-06-28 12:46:15',0),(89,2,'T0005','[82, 83]','L / 黑色',100.00,20,'/uploads/20260628/ed09e06b-6d04-45f8-9f88-d0af180f3ede.jpg','2026-06-28 12:26:10','2026-06-28 12:26:10',0),(90,2,'T0006','[82, 84]','L / 白色',105.00,30,'/uploads/20260628/9ebb63de-7aa1-45ff-8b1e-60edde135c15.jpg','2026-06-28 12:26:10','2026-06-28 12:26:10',0),(91,3,'R','[85]','红',99.00,95,'/uploads/20260626/f416539f-2a13-4e0d-95be-8fae913ef88c.jpg','2026-06-28 12:26:39','2026-06-28 12:26:39',0),(92,3,'B','[86]','蓝',109.00,4,'/uploads/20260626/151a36e8-199a-4844-b4cf-300c4ee7c930.jpg','2026-06-28 12:26:39','2026-06-28 12:26:39',0),(299,68,'','[286, 288]','绿色 / M',0.01,10,'/uploads/20260628/74218d64-a1fb-441f-87e3-904c5819ea92.jpg','2026-06-28 22:47:24','2026-06-28 22:47:24',0),(300,68,'','[286, 289]','绿色 / L',0.01,2,'/uploads/20260628/dc5bf88e-ca6b-4398-9bd1-058b9b6c744c.jpg','2026-06-28 22:47:24','2026-06-28 22:47:24',0),(301,68,'','[286, 290]','绿色 / XL',0.01,303,'/uploads/20260628/8f84182b-6d31-420a-b912-40fb98290329.jpg','2026-06-28 22:47:24','2026-06-28 22:47:24',0),(302,68,'','[287, 288]','红色 / M',0.01,30,'/uploads/20260628/22f66774-65e3-4a53-95bd-da967db6c011.jpg','2026-06-28 22:47:24','2026-06-28 22:47:24',0),(303,68,'','[287, 289]','红色 / L',0.01,40,'/uploads/20260628/df1a3a7a-62e1-4144-b969-b111ae5defd3.jpg','2026-06-28 22:47:24','2026-06-28 22:47:24',0),(304,68,'','[287, 290]','红色 / XL',0.01,48,'/uploads/20260628/a59ce492-b29b-4218-b363-a2bf9d88f25e.jpg','2026-06-28 22:47:24','2026-06-28 23:19:25',0);
/*!40000 ALTER TABLE `product_sku` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_spec`
--

DROP TABLE IF EXISTS `product_spec`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_spec` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `product_id` bigint unsigned NOT NULL,
  `name` varchar(32) NOT NULL,
  `sort` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=243 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品规格定义';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_spec`
--

LOCK TABLES `product_spec` WRITE;
/*!40000 ALTER TABLE `product_spec` DISABLE KEYS */;
INSERT INTO `product_spec` VALUES (1,1,'颜色',0,'2026-06-18 18:36:36','2026-06-18 18:56:28',1),(2,1,'存储',1,'2026-06-18 18:36:36','2026-06-18 18:56:28',1),(3,1,'颜色',0,'2026-06-18 18:56:28','2026-06-18 18:56:28',0),(4,2,'颜色',0,'2026-06-18 20:08:31','2026-06-28 12:26:10',1),(5,3,'颜色',0,'2026-06-18 20:10:48','2026-06-26 17:48:58',1),(42,3,'颜色',0,'2026-06-26 17:48:59','2026-06-28 12:26:39',1),(43,2,'尺寸',0,'2026-06-28 12:26:10','2026-06-28 12:26:10',0),(44,2,'颜色',1,'2026-06-28 12:26:10','2026-06-28 12:26:10',0),(45,3,'颜色',0,'2026-06-28 12:26:39','2026-06-28 12:26:39',0),(149,68,'颜色',0,'2026-06-28 22:47:24','2026-06-28 22:47:24',0),(150,68,'尺码',1,'2026-06-28 22:47:24','2026-06-28 22:47:24',0);
/*!40000 ALTER TABLE `product_spec` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_spec_value`
--

DROP TABLE IF EXISTS `product_spec_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_spec_value` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `spec_id` bigint unsigned NOT NULL,
  `value` varchar(32) NOT NULL,
  `sort` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_spec` (`spec_id`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=471 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品规格值';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_spec_value`
--

LOCK TABLES `product_spec_value` WRITE;
/*!40000 ALTER TABLE `product_spec_value` DISABLE KEYS */;
INSERT INTO `product_spec_value` VALUES (1,1,'黑',0,'2026-06-18 18:36:36','2026-06-18 18:56:28',1),(2,1,'白',1,'2026-06-18 18:36:36','2026-06-18 18:56:28',1),(3,2,'256G',0,'2026-06-18 18:36:36','2026-06-18 18:56:28',1),(4,2,'512G',1,'2026-06-18 18:36:36','2026-06-18 18:56:28',1),(5,3,'金',0,'2026-06-18 18:56:28','2026-06-18 18:56:28',0),(6,4,'红',0,'2026-06-18 20:08:31','2026-06-28 12:26:10',1),(7,4,'蓝',1,'2026-06-18 20:08:31','2026-06-28 12:26:10',1),(8,5,'红',0,'2026-06-18 20:10:48','2026-06-26 17:48:58',1),(9,5,'蓝',1,'2026-06-18 20:10:48','2026-06-26 17:48:58',1),(78,42,'红',0,'2026-06-26 17:48:59','2026-06-28 12:26:39',1),(79,42,'蓝',1,'2026-06-26 17:48:59','2026-06-28 12:26:39',1),(80,43,'S',0,'2026-06-28 12:26:10','2026-06-28 12:26:10',0),(81,43,'M',1,'2026-06-28 12:26:10','2026-06-28 12:26:10',0),(82,43,'L',2,'2026-06-28 12:26:10','2026-06-28 12:26:10',0),(83,44,'黑色',0,'2026-06-28 12:26:10','2026-06-28 12:26:10',0),(84,44,'白色',1,'2026-06-28 12:26:10','2026-06-28 12:26:10',0),(85,45,'红',0,'2026-06-28 12:26:39','2026-06-28 12:26:39',0),(86,45,'蓝',1,'2026-06-28 12:26:39','2026-06-28 12:26:39',0),(286,149,'绿色',0,'2026-06-28 22:47:24','2026-06-28 22:47:24',0),(287,149,'红色',1,'2026-06-28 22:47:24','2026-06-28 22:47:24',0),(288,150,'M',0,'2026-06-28 22:47:24','2026-06-28 22:47:24',0),(289,150,'L',1,'2026-06-28 22:47:24','2026-06-28 22:47:24',0),(290,150,'XL',2,'2026-06-28 22:47:24','2026-06-28 22:47:24',0);
/*!40000 ALTER TABLE `product_spec_value` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `refund_application`
--

DROP TABLE IF EXISTS `refund_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refund_application` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `merchant_id` bigint unsigned NOT NULL,
  `reason` varchar(500) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0=PENDING 1=APPROVED 2=REJECTED',
  `reject_reason` varchar(255) DEFAULT '',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_active` (`order_no`,`status`) COMMENT '同一订单同时只有一个未决申请(status=0)',
  KEY `idx_merchant_status` (`merchant_id`,`status`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款申请';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `refund_application`
--

LOCK TABLES `refund_application` WRITE;
/*!40000 ALTER TABLE `refund_application` DISABLE KEYS */;
INSERT INTO `refund_application` VALUES (1,'26062014543600031031',3,1,'不想要了',2,'不好使','2026-06-20 14:54:36','2026-06-29 19:13:22'),(2,'26062014592800038236',3,1,'不想要了',1,'','2026-06-20 14:59:28','2026-06-20 14:59:28'),(3,'26062015005100039983',3,1,'test refund',1,'','2026-06-20 15:00:51','2026-06-20 15:00:52'),(4,'26062015020500037129',3,1,'test refund',1,'','2026-06-20 15:02:05','2026-06-20 15:02:06');
/*!40000 ALTER TABLE `refund_application` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `openid` varchar(64) NOT NULL,
  `unionid` varchar(64) DEFAULT '',
  `nickname` varchar(64) DEFAULT '',
  `avatar` varchar(255) DEFAULT '',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号，未绑定为 NULL',
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `openid` (`openid`),
  UNIQUE KEY `uk_user_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=62 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'dev_openid_test_code_alpha','','','',NULL,'2026-06-17 15:54:52','2026-06-17 15:54:52','2026-06-17 21:27:58',0),(2,'dev_openid_test_code_123','','','',NULL,'2026-06-17 19:59:11','2026-06-17 19:59:11','2026-06-17 21:27:58',0),(3,'dev_openid_smoke_test_code','','','',NULL,'2026-06-17 20:25:06','2026-06-17 20:23:55','2026-06-17 21:27:58',0),(4,'dev_openid_0b1VAJll2uWyVh4AVell2b0pNz1VAJlL','','','',NULL,'2026-06-17 20:26:31','2026-06-17 20:26:31','2026-06-17 21:27:58',0),(5,'dev_openid_0b14oCFa1HvMUL00s9Ia1Gwzph34oCFd','','','',NULL,'2026-06-17 20:46:50','2026-06-17 20:46:50','2026-06-17 21:27:58',0),(6,'dev_openid_0a1R7j000jJ5AW1LXC100rzJ634R7j0X','','','',NULL,'2026-06-17 20:47:26','2026-06-17 20:47:26','2026-06-17 21:27:58',0),(7,'dev_openid_u-bind-test','','','','13800000000','2026-06-18 17:02:55','2026-06-18 17:02:55','2026-06-18 17:02:55',0),(8,'dev_openid_addr-test','','','',NULL,'2026-06-18 17:10:59','2026-06-18 17:10:32','2026-06-18 17:10:32',0),(9,'dev_openid_addr-test2','','','',NULL,'2026-06-18 17:11:25','2026-06-18 17:11:25','2026-06-18 17:11:25',0),(10,'dev_openid_0d1wRsFa1KVGUL0J6sHa140Rji0wRsFY','','','',NULL,'2026-06-18 17:28:42','2026-06-18 17:28:42','2026-06-18 17:28:42',0),(11,'dev_openid_0c1Puqml2K9aVh4ZNznl2TJ7p71PuqmU','','','',NULL,'2026-06-18 17:29:24','2026-06-18 17:29:24','2026-06-18 17:29:24',0),(12,'dev_openid_0f13Kq000EtIzW1Kte300VYQOT03Kq0u','','','',NULL,'2026-06-18 17:30:50','2026-06-18 17:30:50','2026-06-18 17:30:50',0),(13,'dev_openid_0f1Hokll2524Uh4cE4ol2v3l0U3HoklH','','','',NULL,'2026-06-18 17:30:59','2026-06-18 17:30:59','2026-06-18 17:30:59',0),(14,'dev_openid_0c11y9ml23grVh4oWlll2ziWLb31y9mh','','','',NULL,'2026-06-18 17:31:02','2026-06-18 17:31:02','2026-06-18 17:31:02',0),(15,'dev_openid_0d1P9iGa114RTL0cRDHa1Lyp3H2P9iGb','','','',NULL,'2026-06-18 17:31:09','2026-06-18 17:31:09','2026-06-18 17:31:09',0),(16,'dev_openid_0e1xOH000DprzW1Mr8400T88C84xOH0j','','','',NULL,'2026-06-18 17:31:13','2026-06-18 17:31:13','2026-06-18 17:31:13',0),(17,'dev_openid_0c164KFa1capUL0sZJFa1CNnqc264KFl','','','',NULL,'2026-06-18 17:31:14','2026-06-18 17:31:14','2026-06-18 17:31:14',0),(18,'dev_openid_0f1TdzGa1IgMVL0CtLGa1pBI1b4TdzG6','','','',NULL,'2026-06-18 17:31:25','2026-06-18 17:31:25','2026-06-18 17:31:25',0),(19,'dev_openid_0d17K90005mZzW1PxH300ps0Ko27K90H','','','',NULL,'2026-06-18 17:31:39','2026-06-18 17:31:39','2026-06-18 17:31:39',0),(20,'dev_openid_0f1zrkll2TT3Uh4f3Enl21lD3g0zrklh','','','',NULL,'2026-06-18 17:31:44','2026-06-18 17:31:44','2026-06-18 17:31:44',0),(21,'dev_openid_smoke-1781775925','','','',NULL,'2026-06-18 17:45:26','2026-06-18 17:45:26','2026-06-18 17:45:26',0),(22,'dev_openid_smoke-1781776090','','','',NULL,'2026-06-18 17:48:11','2026-06-18 17:48:11','2026-06-18 17:48:11',0),(23,'dev_openid_smoke-1781776424','','','',NULL,'2026-06-18 17:53:45','2026-06-18 17:53:45','2026-06-18 17:53:45',0),(24,'dev_openid_0f13Vbll2uzeUh4u86ll2w9D2K13Vbl4','','','',NULL,'2026-06-18 19:51:33','2026-06-18 19:51:33','2026-06-18 19:51:33',0),(25,'dev_openid_0f1cnQ000zVkzW1Mt5300VG72U3cnQ0J','','','',NULL,'2026-06-18 19:51:36','2026-06-18 19:51:36','2026-06-18 19:51:36',0),(26,'dev_openid_0c18Cz000I0CzW1sXu200ru2gX38Cz01','','','',NULL,'2026-06-18 19:56:21','2026-06-18 19:56:21','2026-06-18 19:56:21',0),(27,'dev_openid_0b1x7Ekl2eHMUh41a4nl2SRqBJ0x7Ek4','','','',NULL,'2026-06-18 19:56:28','2026-06-18 19:56:28','2026-06-18 19:56:28',0),(28,'dev_openid_0f1J5IGa1NXFVL0iUuGa1Sx4Ht0J5IG0','','','',NULL,'2026-06-18 19:56:52','2026-06-18 19:56:52','2026-06-18 19:56:52',0),(29,'dev_openid_0b1bRF100UdIAW1IXO100QXZGI3bRF12','','','',NULL,'2026-06-18 19:57:07','2026-06-18 19:57:07','2026-06-18 19:57:07',0),(30,'dev_openid_0e1qDi00077TzW1w9f000tVg2h0qDi0n','','','',NULL,'2026-06-18 19:57:29','2026-06-18 19:57:29','2026-06-18 19:57:29',0),(31,'dev_openid_0e1bD7nl2r5IWh4Yhcll2OYJE01bD7n1','','','',NULL,'2026-06-18 19:57:37','2026-06-18 19:57:37','2026-06-18 19:57:37',0),(32,'dev_openid_0f1B3V00010nCW1zNg1000jEZo0B3V0V','','','',NULL,'2026-06-22 09:01:35','2026-06-22 09:01:35','2026-06-22 09:01:35',0),(33,'dev_openid_0d1JWLFa1pr5WL0gIaHa1ZHF0P2JWLFs','','','',NULL,'2026-06-23 09:52:59','2026-06-23 09:52:59','2026-06-23 09:52:59',0),(34,'dev_openid_0c1mUcll2RNkWh4RQsnl2QpO5C1mUclE','','','',NULL,'2026-06-24 11:16:45','2026-06-24 11:16:45','2026-06-24 11:16:45',0),(35,'dev_openid_0f1HCWFa1083YL05f3Ia1QOHc44HCWFV','','','',NULL,'2026-06-25 20:43:32','2026-06-25 20:43:32','2026-06-25 20:43:32',0),(36,'dev_openid_0d1ocA100hhXBW1yGR10008hG23ocA1k','','','',NULL,'2026-06-27 16:46:51','2026-06-27 16:46:51','2026-06-27 16:46:51',0),(37,'dev_openid_0e121B100TmYBW1erv100Yy2sa321B1Z','','','',NULL,'2026-06-27 17:00:15','2026-06-27 17:00:15','2026-06-27 17:00:15',0),(38,'dev_openid_0f1DE9ll2tPHXh4Jozml29FSwW1DE9l4','','','',NULL,'2026-06-27 17:36:56','2026-06-27 17:36:56','2026-06-27 17:36:56',0),(39,'dev_openid_0a1xRfml2dBBWh4ls5ol2TaZnm4xRfmS','','','',NULL,'2026-06-27 17:37:09','2026-06-27 17:37:09','2026-06-27 17:37:09',0),(40,'dev_openid_0d1c2g000memDW171H000cUqeD1c2g00','','','',NULL,'2026-06-27 17:37:22','2026-06-27 17:37:22','2026-06-27 17:37:22',0),(41,'dev_openid_0f1H3g0009dmDW1ok9000f5XrL2H3g0J','','','',NULL,'2026-06-27 17:37:46','2026-06-27 17:37:46','2026-06-27 17:37:46',0),(42,'dev_openid_0c1jMqll2gTqXh4o4sol2vO9s93jMqlo','','','',NULL,'2026-06-27 17:38:10','2026-06-27 17:38:10','2026-06-27 17:38:10',0),(43,'dev_openid_0b1CszFa1nV2YL0seaIa1Kibdt4CszFU','','','',NULL,'2026-06-27 17:39:28','2026-06-27 17:39:28','2026-06-27 17:39:28',0),(44,'dev_openid_0f14HQFa1TeMXL0WaOFa1oz6Ut34HQFu','','','',NULL,'2026-06-27 17:42:29','2026-06-27 17:42:29','2026-06-27 17:42:29',0),(45,'dev_openid_0d12NU100E8IBW1KYg300uxI5j22NU12','','','',NULL,'2026-06-27 17:44:57','2026-06-27 17:44:57','2026-06-27 17:44:57',0),(46,'dev_openid_0a1CE5100hixCW1bWe1006s2H50CE51l','','','',NULL,'2026-06-27 17:45:07','2026-06-27 17:45:07','2026-06-27 17:45:07',0),(47,'dev_openid_0f1M7Tkl2EsYXh4uowol2yI8fZ2M7Tkj','','','',NULL,'2026-06-27 17:45:27','2026-06-27 17:45:27','2026-06-27 17:45:27',0),(48,'dev_openid_0a1HX7Ga13ruXL0Y9CHa1d8Ey81HX7Gq','','','',NULL,'2026-06-27 17:46:05','2026-06-27 17:46:05','2026-06-27 17:46:05',0),(49,'dev_openid_bind_fix_test_1','','','','13970363287','2026-06-28 10:58:41','2026-06-28 10:58:41','2026-06-28 10:58:41',0),(50,'dev_openid_0a1Id3Ga19ePYL0DIIGa1oH5nm4Id3G4','','','','13933476301','2026-06-28 11:06:47','2026-06-28 11:06:47','2026-06-28 11:06:47',0),(51,'dev_openid_0c11Cmll2miJYh4jhYkl29OweZ21CmlW','','','','13990255831','2026-06-28 11:08:19','2026-06-28 11:08:19','2026-06-28 11:08:19',0),(52,'dev_openid_0f1fTOkl2XBbYh4IvHol2pqn5n3fTOkL','','','',NULL,'2026-06-28 11:14:29','2026-06-28 11:14:29','2026-06-28 11:14:29',0),(53,'dev_openid_0b1mccml2hOyZh4rJenl2IVzhe1mccmP','','','',NULL,'2026-06-28 11:15:32','2026-06-28 11:15:32','2026-06-28 11:15:32',0),(54,'dev_openid_0f1NEKml2Pg70i4VIPnl27zQPp4NEKmI','','','',NULL,'2026-06-28 11:21:27','2026-06-28 11:21:27','2026-06-28 11:21:27',0),(55,'dev_openid_0d1rUm100AmIEW1ijm000xzeJm0rUm1W','','','',NULL,'2026-06-28 12:26:59','2026-06-28 12:26:59','2026-06-28 12:26:59',0),(56,'dev_openid_0c16VCkl2qEjYh4JjGol214jZb16VCk7','','','',NULL,'2026-06-28 12:37:47','2026-06-28 12:37:47','2026-06-28 12:37:47',0),(57,'dev_openid_0e1nUGGa1FTKZL07RVIa1snyLH1nUGGD','','','',NULL,'2026-06-28 12:38:28','2026-06-28 12:38:28','2026-06-28 12:38:28',0),(58,'dev_openid_0b1ekDkl27mkYh4m9Fnl2HatD74ekDk8','','','','13958467767','2026-06-28 12:44:26','2026-06-28 12:44:26','2026-06-28 12:44:26',0),(59,'dev_openid_0e1lX6100y2OEW1eNa400qs3yW1lX61C','','','',NULL,'2026-06-28 12:44:58','2026-06-28 12:44:58','2026-06-28 12:44:58',0),(60,'dev_openid_0c1RcBFa19tFYL0F1THa1Kn01U3RcBF-','','','',NULL,'2026-06-28 12:46:34','2026-06-28 12:46:34','2026-06-28 12:46:34',0),(61,'oPQlb5IiXdeGFe2tzNWAsFO36_NM','','杨小胖','/uploads/20260629/d8cc3255-db45-430b-8a4a-d2a7e1a0e648.jpeg','18201117988','2026-07-01 20:24:35','2026-06-28 14:50:07','2026-06-28 22:34:27',0);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_address`
--

DROP TABLE IF EXISTS `user_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_address` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `receiver` varchar(50) NOT NULL COMMENT '收货人',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `region` varchar(100) NOT NULL COMMENT '省市区，例: 北京市/北京市/朝阳区',
  `detail` varchar(255) NOT NULL COMMENT '详细地址',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否默认',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=68 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收货地址';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_address`
--

LOCK TABLES `user_address` WRITE;
/*!40000 ALTER TABLE `user_address` DISABLE KEYS */;
INSERT INTO `user_address` VALUES (1,8,'张三','13800000001','北京市/北京市/朝阳区','望京 SOHO',0,'2026-06-18 17:10:59','2026-06-18 17:10:58',0),(2,8,'李四','13800000002','上海市/上海市/浦东新区','陆家嘴',1,'2026-06-18 17:10:59','2026-06-18 17:10:59',0),(3,9,'A','13900000099','北京市/北京市/朝阳区','望京改',1,'2026-06-18 17:11:26','2026-06-18 17:11:26',0),(4,9,'B','13800000002','上海市/上海市/浦东新区','陆家嘴',0,'2026-06-18 17:11:26','2026-06-18 17:11:25',1),(5,21,'A','13800000001','北京/北京/朝阳','x',0,'2026-06-18 17:45:26','2026-06-18 17:45:26',0),(6,21,'B','13800000002','上海/上海/浦东','y',1,'2026-06-18 17:45:26','2026-06-18 17:45:26',0),(7,22,'A','13800000001','北京/北京/朝阳','x',0,'2026-06-18 17:48:11','2026-06-18 17:48:10',0),(8,22,'B','13800000002','上海/上海/浦东','y',1,'2026-06-18 17:48:11','2026-06-18 17:48:11',0),(9,23,'A','13800000001','北京/北京/朝阳','x',0,'2026-06-18 17:53:45','2026-06-18 17:53:44',0),(10,23,'B','13800000002','上海/上海/浦东','y',1,'2026-06-18 17:53:45','2026-06-18 17:53:45',0),(11,31,'小胖','18201119090','辽宁省/沈阳市/和平区','同方广场A座3812',1,'2026-06-18 21:34:18','2026-06-18 21:34:18',0),(12,3,'张三','13800138000','上海市 浦东新区','世纪大道100号',1,'2026-06-20 11:02:23','2026-06-20 11:02:23',0),(18,3,'测试','13800138000','北京 朝阳','望京soho',0,'2026-06-20 11:15:38','2026-06-20 11:15:38',0),(34,54,'小胖','18200001111','北京市/北京市/东城区','222',1,'2026-06-28 11:22:54','2026-06-28 11:22:54',0),(35,59,'小胖子','19099998888','山西省/太原市/杏花岭区','123',1,'2026-06-28 12:46:07','2026-06-28 12:46:07',0),(46,61,'小胖子','18201117988','辽宁省/沈阳市/皇姑区','鸭绿江北街融创金地御景壹号',1,'2026-06-28 23:04:26','2026-06-28 23:04:26',0),(47,61,'杨小胖','15566047217','辽宁省/沈阳市/沈河区','北方传媒大厦808',0,'2026-06-28 23:05:01','2026-06-28 23:05:01',0);
/*!40000 ALTER TABLE `user_address` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-01 20:37:42
