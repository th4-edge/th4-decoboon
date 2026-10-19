-- MySQL dump 10.13  Distrib 8.4.0, for macos13.2 (arm64)
--
-- Host: 127.0.0.1    Database: decoboon
-- ------------------------------------------------------
-- Server version	8.0.24

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
-- Table structure for table `db_business_target`
--

DROP TABLE IF EXISTS `db_business_target`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_business_target` (
  `target_id` bigint NOT NULL,
  `create_date` int DEFAULT NULL,
  `create_date_time` bigint DEFAULT NULL,
  `create_day` int DEFAULT NULL,
  `create_hour` int DEFAULT NULL,
  `create_minute` int DEFAULT NULL,
  `create_month` int DEFAULT NULL,
  `create_second` int DEFAULT NULL,
  `create_time` int DEFAULT NULL,
  `create_week` tinyint DEFAULT NULL,
  `create_year` int DEFAULT NULL,
  `update_date` int DEFAULT NULL,
  `update_date_time` bigint DEFAULT NULL,
  `update_day` int DEFAULT NULL,
  `update_hour` int DEFAULT NULL,
  `update_minute` int DEFAULT NULL,
  `update_month` int DEFAULT NULL,
  `update_second` int DEFAULT NULL,
  `update_time` int DEFAULT NULL,
  `update_week` tinyint DEFAULT NULL,
  `update_year` int DEFAULT NULL,
  `active_project_count` int NOT NULL,
  `gross_profit` decimal(14,2) NOT NULL,
  `received_amount` decimal(14,2) NOT NULL,
  `signed_amount` decimal(14,2) NOT NULL,
  `target_month` date NOT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`target_id`),
  UNIQUE KEY `uk_business_target_company_month` (`com_id`,`target_month`),
  KEY `idx_business_target_company` (`com_id`,`target_id`),
  KEY `idx_business_target_company_month` (`com_id`,`target_month`,`target_id`),
  CONSTRAINT `FKp64xod7bvm8falnuqj6d97c82` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_business_target`
--

LOCK TABLES `db_business_target` WRITE;
/*!40000 ALTER TABLE `db_business_target` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_business_target` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_schema_migration`
--

DROP TABLE IF EXISTS `db_schema_migration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_schema_migration` (
  `version` varchar(64) NOT NULL,
  `description` varchar(255) NOT NULL,
  `checksum` char(64) NOT NULL,
  `status` varchar(16) NOT NULL,
  `started_at` datetime(6) NOT NULL,
  `finished_at` datetime(6) DEFAULT NULL,
  `applied_by` varchar(128) NOT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`version`),
  KEY `idx_schema_migration_status` (`status`,`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_schema_migration`
--

LOCK TABLES `db_schema_migration` WRITE;
/*!40000 ALTER TABLE `db_schema_migration` DISABLE KEYS */;
INSERT INTO `db_schema_migration` VALUES ('101','内容素材与授权','dd894f0f1f38902c1b7ed8f1bb5d6fc93661a7d4590deab59a5e688d98549c7e','SUCCESS','2026-10-10 05:33:36.787164','2026-10-10 05:33:36.787164','codex-p0-1',NULL),('102','内容任务中心','4c1b32f41d8bf810578303823105fff393cf2abf349177d7beb49a09b65eda60','SUCCESS','2026-10-10 06:00:10.548649','2026-10-10 06:02:24.001557','codex-p0-2',NULL),('103','员工成片与发布回填','398875ae0e8245cdcea7ed122b1d8001ab127612719dd38ea472dfe962016899','SUCCESS','2026-10-10 06:14:47.417152','2026-10-10 06:14:47.417152','codex-p0-3',NULL),('104','内容线索归因','0e2fd44e27e2885e92bab60e936c8ee95eb7aea3dcbb81aacb0877fa27e019fc','SUCCESS','2026-10-10 06:35:52.057527','2026-10-10 06:35:52.057527','codex-p0-4',NULL),('106','区域实测面积周长','bc1594fdc622f0959f045881bcc21f50bff2439b9897a51acbe82c070b5713bc','SUCCESS','2026-10-10 17:39:59.311948','2026-10-10 17:39:59.311948','codex-dev-local',NULL),('107','方案面积对齐实测口径','7b73c33eb59ba2c2056723084c3d316a6f9ebccae1c5b6d7309e875e5a5c0716','SUCCESS','2026-10-10 19:21:55.018060','2026-10-10 19:21:55.018060','codex-dev-local',NULL),('79','凭证补录修订轨迹','33ef9e56366e04fe49205d3a82044327257de4f7ec3fd1bdd970ea1ebc39248a','SUCCESS','2026-10-06 14:18:10.363395','2026-10-06 14:18:10.363395','codex-dev',NULL),('92','客户休眠状态与风险跟进排除','8168a7fdc4610fb37f8fdb74f32c7354e93404201a61fa0a9f34aac2f3cda83d','SUCCESS','2026-10-08 18:50:37.777957','2026-10-08 18:50:37.777957','codex-dev-customer-dormant','local dev migration; ledger 80-91 pre-existing gap');
/*!40000 ALTER TABLE `db_schema_migration` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-17 16:33:10
