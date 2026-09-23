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
-- Table structure for table `catalog_appliance_detail`
--

DROP TABLE IF EXISTS `catalog_appliance_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_appliance_detail` (
  `detail_id` bigint NOT NULL,
  `after_sales` varchar(512) DEFAULT NULL,
  `dimensions` varchar(128) DEFAULT NULL,
  `model` varchar(128) DEFAULT NULL,
  `power` varchar(64) DEFAULT NULL,
  `utility_requirement` varchar(512) DEFAULT NULL,
  `warranty_months` int NOT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `UKewj4fdoqatc239p2t0aah30di` (`item_id`),
  KEY `FKn22phhchyofta5olg3fx26fdq` (`com_id`),
  CONSTRAINT `FKn22phhchyofta5olg3fx26fdq` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKqbw14gsneddje3evnqf2c6yx3` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_appliance_detail`
--

LOCK TABLES `catalog_appliance_detail` WRITE;
/*!40000 ALTER TABLE `catalog_appliance_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_appliance_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_construction_detail`
--

DROP TABLE IF EXISTS `catalog_construction_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_construction_detail` (
  `detail_id` bigint NOT NULL,
  `concealed` bit(1) NOT NULL,
  `craft` longtext,
  `criterion` longtext,
  `duration_days` int NOT NULL,
  `measurement_formula` varchar(256) DEFAULT NULL,
  `stage` varchar(64) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `UK7o9o601fow4ln42w68hfi7ot0` (`item_id`),
  KEY `FKsqgli60rqokvs6j7vovfogu2q` (`com_id`),
  CONSTRAINT `FKq4d0ytj3gwutwl66w97vpoolc` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FKsqgli60rqokvs6j7vovfogu2q` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_construction_detail`
--

LOCK TABLES `catalog_construction_detail` WRITE;
/*!40000 ALTER TABLE `catalog_construction_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_construction_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_custom_detail`
--

DROP TABLE IF EXISTS `catalog_custom_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_custom_detail` (
  `detail_id` bigint NOT NULL,
  `board` varchar(128) DEFAULT NULL,
  `cabinet_type` varchar(64) DEFAULT NULL,
  `countertop` varchar(128) DEFAULT NULL,
  `door` varchar(128) DEFAULT NULL,
  `hardware` varchar(256) DEFAULT NULL,
  `parametric_formula` varchar(512) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `UK6kj9bsggt8xncmjf0p8ht9ltq` (`item_id`),
  KEY `FKsch3gm33d2sjstf0iypsg6bli` (`com_id`),
  CONSTRAINT `FK1lx5t6kuut7r0ytqb6npcajyy` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FKsch3gm33d2sjstf0iypsg6bli` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_custom_detail`
--

LOCK TABLES `catalog_custom_detail` WRITE;
/*!40000 ALTER TABLE `catalog_custom_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_custom_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_general_detail`
--

DROP TABLE IF EXISTS `catalog_general_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_general_detail` (
  `detail_id` bigint NOT NULL,
  `allocation_mode` varchar(64) DEFAULT NULL,
  `approval_required` bit(1) NOT NULL,
  `discountable` bit(1) NOT NULL,
  `fee_type` varchar(64) DEFAULT NULL,
  `invoice_type` varchar(64) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `UK5sb2uui6f4epfvb3s4g77lc0g` (`item_id`),
  KEY `FKddt86meyxhes3lj12qg11s8xf` (`com_id`),
  CONSTRAINT `FKddt86meyxhes3lj12qg11s8xf` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKqllseilpr2d8kft2ey5vcogtr` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_general_detail`
--

LOCK TABLES `catalog_general_detail` WRITE;
/*!40000 ALTER TABLE `catalog_general_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_general_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_item`
--

DROP TABLE IF EXISTS `catalog_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_item` (
  `item_id` bigint NOT NULL,
  `active` bit(1) NOT NULL,
  `brand` varchar(128) DEFAULT NULL,
  `category` varchar(64) NOT NULL,
  `code` varchar(64) DEFAULT NULL,
  `cost_price` decimal(12,2) NOT NULL,
  `craft` longtext,
  `criterion` longtext,
  `description` longtext,
  `formula` varchar(256) NOT NULL,
  `manual_price` decimal(12,2) NOT NULL,
  `material_price` decimal(12,2) NOT NULL,
  `name` varchar(256) NOT NULL,
  `name_initials` varchar(64) DEFAULT NULL,
  `pricing_mode` enum('FixedTotal','ManualOnly','MaterialAndManual','MaterialOnly') NOT NULL,
  `series` varchar(128) DEFAULT NULL,
  `sort` int NOT NULL,
  `spec` varchar(256) DEFAULT NULL,
  `type` enum('Appliance','Construction','Custom','General','Material','Soft') NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') NOT NULL,
  `com_id` bigint NOT NULL,
  `maintainer_id` bigint DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  KEY `idx_catalog_company` (`com_id`,`item_id`),
  KEY `idx_catalog_company_type_category` (`com_id`,`type`,`category`,`item_id`),
  KEY `idx_catalog_company_type_brand` (`com_id`,`type`,`category`,`brand`,`item_id`),
  KEY `idx_catalog_company_maintainer` (`com_id`,`maintainer_id`,`item_id`),
  KEY `FKol4l5c0yrnx8g2355jbl1w9w6` (`maintainer_id`),
  CONSTRAINT `FKjymracy97jde5o1j04hlopwtg` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKol4l5c0yrnx8g2355jbl1w9w6` FOREIGN KEY (`maintainer_id`) REFERENCES `db_tenant_employee` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_item`
--

LOCK TABLES `catalog_item` WRITE;
/*!40000 ALTER TABLE `catalog_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_item_images`
--

DROP TABLE IF EXISTS `catalog_item_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_item_images` (
  `item_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKsfne1wa1nmwlaew2m4nsyh1bu` (`item_id`),
  CONSTRAINT `FKsfne1wa1nmwlaew2m4nsyh1bu` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_item_images`
--

LOCK TABLES `catalog_item_images` WRITE;
/*!40000 ALTER TABLE `catalog_item_images` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_item_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_material_detail`
--

DROP TABLE IF EXISTS `catalog_material_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_material_detail` (
  `detail_id` bigint NOT NULL,
  `color` varchar(64) DEFAULT NULL,
  `inventory_mode` varchar(32) DEFAULT NULL,
  `loss_rate` decimal(10,4) NOT NULL,
  `material` varchar(128) DEFAULT NULL,
  `minimum_order` decimal(12,3) NOT NULL,
  `model` varchar(128) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `UKol21wivlioy3jhdq6xn3jhrsh` (`item_id`),
  KEY `FK20ywa3pr760u17hik2mmvcxdn` (`com_id`),
  CONSTRAINT `FK20ywa3pr760u17hik2mmvcxdn` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKdt1f6vweepmvklsitj4p05tef` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_material_detail`
--

LOCK TABLES `catalog_material_detail` WRITE;
/*!40000 ALTER TABLE `catalog_material_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_material_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_soft_detail`
--

DROP TABLE IF EXISTS `catalog_soft_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_soft_detail` (
  `detail_id` bigint NOT NULL,
  `composition` varchar(512) DEFAULT NULL,
  `dimensions` varchar(128) DEFAULT NULL,
  `installation_mode` varchar(64) DEFAULT NULL,
  `lead_time_days` int NOT NULL,
  `material` varchar(128) DEFAULT NULL,
  `style` varchar(64) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `UKlnc25358i78ivsd0ev51f9o3s` (`item_id`),
  KEY `FK5yyrpuy4iiif0pbqpvrwi9ffi` (`com_id`),
  CONSTRAINT `FK15a78rdx82fb55mmtc470mlf` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FK5yyrpuy4iiif0pbqpvrwi9ffi` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_soft_detail`
--

LOCK TABLES `catalog_soft_detail` WRITE;
/*!40000 ALTER TABLE `catalog_soft_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_soft_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cust_customer`
--

DROP TABLE IF EXISTS `cust_customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_customer` (
  `cust_id` bigint NOT NULL,
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
  `addr` varchar(256) DEFAULT NULL,
  `area` decimal(10,2) DEFAULT NULL,
  `budget` decimal(12,2) DEFAULT NULL,
  `community` varchar(128) DEFAULT NULL,
  `gender` enum('F','M') DEFAULT NULL,
  `handed_off_at` datetime(6) DEFAULT NULL,
  `last_follow_at` datetime(6) DEFAULT NULL,
  `last_visit_at` datetime(6) DEFAULT NULL,
  `measure_at` datetime(6) DEFAULT NULL,
  `measure_requested_at` datetime(6) DEFAULT NULL,
  `name` varchar(32) NOT NULL,
  `next_follow_at` datetime(6) DEFAULT NULL,
  `pending_measure_at` datetime(6) DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `source` enum('Lead','OldCustomer','Other','Referral','Video','WalkIn') DEFAULT NULL,
  `status` enum('Finished','Following','Lost','New','PendingMeasure','Quoted','Serving','Signed') NOT NULL,
  `tel_no` varchar(20) NOT NULL,
  `visit_count` int NOT NULL DEFAULT '0',
  `wechat_added` bit(1) NOT NULL,
  `wechat_added_at` datetime(6) DEFAULT NULL,
  `wechat_no` varchar(64) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `designer_id` bigint DEFAULT NULL,
  `emp_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`cust_id`),
  KEY `idx_cust_customer_company` (`com_id`,`cust_id`),
  KEY `idx_cust_customer_company_store` (`com_id`,`store_id`,`cust_id`),
  KEY `idx_cust_customer_company_team` (`com_id`,`team_id`,`cust_id`),
  KEY `idx_cust_customer_company_owner` (`com_id`,`emp_id`,`cust_id`),
  KEY `FKmtqer08cgq4uaqffagylo7tga` (`designer_id`),
  KEY `FKeekkqipy5oxjk35het9tnf6jq` (`emp_id`),
  KEY `FKjih66fbtm8oypu0k21t4ol2dl` (`store_id`),
  KEY `FKcuioiekid2onbjlxksrg9cudp` (`team_id`),
  CONSTRAINT `FKcuioiekid2onbjlxksrg9cudp` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKeekkqipy5oxjk35het9tnf6jq` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKjih66fbtm8oypu0k21t4ol2dl` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKmtqer08cgq4uaqffagylo7tga` FOREIGN KEY (`designer_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKpajh0po9gteo33b0obuqjhh84` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `cust_customer_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `cust_customer_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cust_customer`
--

LOCK TABLES `cust_customer` WRITE;
/*!40000 ALTER TABLE `cust_customer` DISABLE KEYS */;
/*!40000 ALTER TABLE `cust_customer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cust_customer_access`
--

DROP TABLE IF EXISTS `cust_customer_access`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_customer_access` (
  `access_id` bigint NOT NULL,
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
  `display_name` varchar(32) NOT NULL,
  `granted_at` datetime(6) DEFAULT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `status` enum('Active','Revoked') NOT NULL,
  `com_id` bigint NOT NULL,
  `cre_id` bigint NOT NULL,
  `cust_id` bigint NOT NULL,
  `invite_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `revoked_by` bigint DEFAULT NULL,
  PRIMARY KEY (`access_id`),
  UNIQUE KEY `uk_customer_access_credential_project` (`cre_id`,`project_id`),
  KEY `idx_customer_access_company` (`com_id`,`access_id`),
  KEY `idx_customer_access_company_project` (`com_id`,`project_id`,`access_id`),
  KEY `idx_customer_access_company_customer` (`com_id`,`cust_id`,`access_id`),
  KEY `idx_customer_access_company_credential` (`com_id`,`cre_id`,`access_id`),
  KEY `FK98qs32fhqvdb2lncgevogxy8n` (`cust_id`),
  KEY `FKkv3fnbi5xqedxa2uu3a811dj8` (`invite_id`),
  KEY `FKls2fg0swayo3lfmj4n9eiwtlv` (`project_id`),
  KEY `FKh5ipx91l78gcpf7icukeobn6g` (`revoked_by`),
  CONSTRAINT `FK79x39jfll0ls23k30inm697f3` FOREIGN KEY (`cre_id`) REFERENCES `db_tenant_credentials` (`cre_id`),
  CONSTRAINT `FK98qs32fhqvdb2lncgevogxy8n` FOREIGN KEY (`cust_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKh5ipx91l78gcpf7icukeobn6g` FOREIGN KEY (`revoked_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKi1rgn5kt8l1feff1b58pcspgu` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKkv3fnbi5xqedxa2uu3a811dj8` FOREIGN KEY (`invite_id`) REFERENCES `cust_customer_invite` (`invite_id`),
  CONSTRAINT `FKls2fg0swayo3lfmj4n9eiwtlv` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `cust_customer_access_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `cust_customer_access_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cust_customer_access`
--

LOCK TABLES `cust_customer_access` WRITE;
/*!40000 ALTER TABLE `cust_customer_access` DISABLE KEYS */;
/*!40000 ALTER TABLE `cust_customer_access` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cust_customer_follow`
--

DROP TABLE IF EXISTS `cust_customer_follow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_customer_follow` (
  `follow_id` bigint NOT NULL,
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
  `content` varchar(512) NOT NULL,
  `next_follow_at` datetime(6) DEFAULT NULL,
  `way` enum('Other','Phone','Video','Visit','Wechat') NOT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`follow_id`),
  KEY `idx_customer_follow_company` (`com_id`,`follow_id`),
  KEY `idx_customer_follow_company_customer` (`com_id`,`customer_id`,`follow_id`),
  KEY `idx_customer_follow_company_owner` (`com_id`,`emp_id`,`follow_id`),
  KEY `FKgsk3jqol3qeobtxb5a6gonw31` (`customer_id`),
  KEY `FKc3rhpmte8r0tpnekuvp69ji8p` (`emp_id`),
  KEY `FK5659xk57qfca8dmdffxr6k2a3` (`store_id`),
  KEY `FKowk7f9fk32mjgbave1ihk8y8l` (`team_id`),
  CONSTRAINT `FK1k6i4mtlshdvouxixgwbdkrb6` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK5659xk57qfca8dmdffxr6k2a3` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKc3rhpmte8r0tpnekuvp69ji8p` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKgsk3jqol3qeobtxb5a6gonw31` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKowk7f9fk32mjgbave1ihk8y8l` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `cust_customer_follow_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `cust_customer_follow_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cust_customer_follow`
--

LOCK TABLES `cust_customer_follow` WRITE;
/*!40000 ALTER TABLE `cust_customer_follow` DISABLE KEYS */;
/*!40000 ALTER TABLE `cust_customer_follow` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cust_customer_invite`
--

DROP TABLE IF EXISTS `cust_customer_invite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_customer_invite` (
  `invite_id` bigint NOT NULL,
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
  `accepted_at` datetime(6) DEFAULT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `customer_name` varchar(32) NOT NULL,
  `expire_time` datetime(6) DEFAULT NULL,
  `status` enum('Accepted','Cancelled','Expired','Pending','Rejected') NOT NULL,
  `tel_no` varchar(20) DEFAULT NULL,
  `token` varchar(64) NOT NULL,
  `accepted_by` bigint DEFAULT NULL,
  `cancelled_by` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `inviter_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`invite_id`),
  UNIQUE KEY `UK7lrxt8xc5odu5xawjhnefytea` (`token`),
  KEY `idx_customer_invite_company` (`com_id`,`invite_id`),
  KEY `idx_customer_invite_company_project` (`com_id`,`project_id`,`invite_id`),
  KEY `FK2vhcljd4n111josb0h42nv626` (`accepted_by`),
  KEY `FK87preb8wcnr7iss158i8d8d7p` (`cancelled_by`),
  KEY `FK63iwh8ri10beglqvph17snxc6` (`customer_id`),
  KEY `FK7ixokqqc1wwvy7is9xq7p6lnn` (`inviter_id`),
  KEY `FKg7jbfh4s2i9009vn7y2lkq09y` (`project_id`),
  CONSTRAINT `FK2vhcljd4n111josb0h42nv626` FOREIGN KEY (`accepted_by`) REFERENCES `db_tenant_credentials` (`cre_id`),
  CONSTRAINT `FK63iwh8ri10beglqvph17snxc6` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FK7ixokqqc1wwvy7is9xq7p6lnn` FOREIGN KEY (`inviter_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK87preb8wcnr7iss158i8d8d7p` FOREIGN KEY (`cancelled_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKg7jbfh4s2i9009vn7y2lkq09y` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKkf03qcg2rfdhrnd6vp5kxh2gw` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `cust_customer_invite_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `cust_customer_invite_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cust_customer_invite`
--

LOCK TABLES `cust_customer_invite` WRITE;
/*!40000 ALTER TABLE `cust_customer_invite` DISABLE KEYS */;
/*!40000 ALTER TABLE `cust_customer_invite` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cust_portrait`
--

DROP TABLE IF EXISTS `cust_portrait`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_portrait` (
  `portrait_id` bigint NOT NULL,
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
  `competitor` enum('ChosenOther','ComparingOneTwo','ComparingThreePlus','None') DEFAULT NULL,
  `decision_role` enum('Children','Couple','Parents','Self','Unknown') DEFAULT NULL,
  `eco` enum('Elder','Infant','None','Pregnant') DEFAULT NULL,
  `house_condition` enum('Old','Pending','Refined','Rough') DEFAULT NULL,
  `household` enum('Couple','Single','ThreeGenerations','ThreePlus','Unknown') DEFAULT NULL,
  `payment` enum('Full','Installment','Loan','Unknown') DEFAULT NULL,
  `purpose` enum('FirstHome','Improve','Investment','Partial','SchoolDistrict','Wedding') DEFAULT NULL,
  `start_window` enum('HalfYearPlus','OneToThreeMonths','ThreeToSixMonths','Undecided','WithinOneMonth') DEFAULT NULL,
  `style` enum('Cream','Industrial','Luxury','Modern','NewChinese','Undecided','Wood') DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `cust_id` bigint NOT NULL,
  `emp_id` bigint DEFAULT NULL,
  PRIMARY KEY (`portrait_id`),
  UNIQUE KEY `idx_cust_portrait_company_customer` (`com_id`,`cust_id`),
  KEY `idx_cust_portrait_company` (`com_id`,`portrait_id`),
  KEY `FK3ly3l0uswbjfcduwbjx1jhmyv` (`cust_id`),
  KEY `FKc0ljy42fuc8sc0f4byqeoi8gc` (`emp_id`),
  CONSTRAINT `FK3ly3l0uswbjfcduwbjx1jhmyv` FOREIGN KEY (`cust_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKc0ljy42fuc8sc0f4byqeoi8gc` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKdwerfb6cdgvowywuynq46c77x` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `cust_portrait_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `cust_portrait_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cust_portrait`
--

LOCK TABLES `cust_portrait` WRITE;
/*!40000 ALTER TABLE `cust_portrait` DISABLE KEYS */;
/*!40000 ALTER TABLE `cust_portrait` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cust_portrait_concern`
--

DROP TABLE IF EXISTS `cust_portrait_concern`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_portrait_concern` (
  `portrait_id` bigint NOT NULL,
  `concern` enum('AfterSales','Duration','Eco','Price','Quality') DEFAULT NULL,
  KEY `FK6vru5u1n2scr9jvvfmhyajjhs` (`portrait_id`),
  CONSTRAINT `FK6vru5u1n2scr9jvvfmhyajjhs` FOREIGN KEY (`portrait_id`) REFERENCES `cust_portrait` (`portrait_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cust_portrait_concern`
--

LOCK TABLES `cust_portrait_concern` WRITE;
/*!40000 ALTER TABLE `cust_portrait_concern` DISABLE KEYS */;
/*!40000 ALTER TABLE `cust_portrait_concern` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cust_tag`
--

DROP TABLE IF EXISTS `cust_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_tag` (
  `tag_id` bigint NOT NULL,
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
  `category` enum('Behavior','Demand','Intent','Preference','Risk','Source','Value') NOT NULL,
  `color` varchar(16) DEFAULT NULL,
  `name` varchar(20) NOT NULL,
  `scope` enum('Manual','System') NOT NULL,
  `sort` int NOT NULL,
  `status` enum('Disabled','Enabled') NOT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `idx_cust_tag_company_name` (`com_id`,`name`),
  KEY `idx_cust_tag_company` (`com_id`,`tag_id`),
  KEY `idx_cust_tag_company_category` (`com_id`,`category`,`status`),
  CONSTRAINT `FKdsp6s2hg276pv3e5q58pyqtj8` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `cust_tag_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `cust_tag_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cust_tag`
--

LOCK TABLES `cust_tag` WRITE;
/*!40000 ALTER TABLE `cust_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `cust_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cust_tag_link`
--

DROP TABLE IF EXISTS `cust_tag_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_tag_link` (
  `link_id` bigint NOT NULL,
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
  `com_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `cust_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`link_id`),
  UNIQUE KEY `idx_cust_tag_link_unique` (`com_id`,`cust_id`,`tag_id`),
  KEY `idx_cust_tag_link_company` (`com_id`,`link_id`),
  KEY `idx_cust_tag_link_company_customer` (`com_id`,`cust_id`,`link_id`),
  KEY `idx_cust_tag_link_company_tag` (`com_id`,`tag_id`,`link_id`),
  KEY `FKidx55sulrtj0hf3a4dcaduxp6` (`emp_id`),
  KEY `FKobs8yganj36uyl8y3pu7juwtv` (`cust_id`),
  KEY `FK178nw3nbh1ggjxkfju2dwqoi0` (`tag_id`),
  CONSTRAINT `FK178nw3nbh1ggjxkfju2dwqoi0` FOREIGN KEY (`tag_id`) REFERENCES `cust_tag` (`tag_id`),
  CONSTRAINT `FK3xbwarrejlqx8899ehpu3aa31` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKidx55sulrtj0hf3a4dcaduxp6` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKobs8yganj36uyl8y3pu7juwtv` FOREIGN KEY (`cust_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `cust_tag_link_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `cust_tag_link_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cust_tag_link`
--

LOCK TABLES `cust_tag_link` WRITE;
/*!40000 ALTER TABLE `cust_tag_link` DISABLE KEYS */;
/*!40000 ALTER TABLE `cust_tag_link` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_acceptance`
--

DROP TABLE IF EXISTS `db_acceptance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_acceptance` (
  `acceptance_id` bigint NOT NULL,
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
  `conclusion` varchar(1024) DEFAULT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `rectification_due_date` date DEFAULT NULL,
  `status` enum('Confirmed','Pending','Rejected') NOT NULL,
  `com_id` bigint NOT NULL,
  `confirmed_by` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `rectification_owner_id` bigint DEFAULT NULL,
  `stage_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `emp_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`acceptance_id`),
  KEY `idx_acceptance_company` (`com_id`,`acceptance_id`),
  KEY `idx_acceptance_company_store` (`com_id`,`store_id`,`acceptance_id`),
  KEY `idx_acceptance_company_team` (`com_id`,`team_id`,`acceptance_id`),
  KEY `idx_acceptance_company_owner` (`com_id`,`owner_id`,`acceptance_id`),
  KEY `FK7rmd6t7iaq6wccngq531879rw` (`confirmed_by`),
  KEY `FKgf3e695tinvcqrgywi15tycdq` (`owner_id`),
  KEY `FK9tc48g5fr4y8ocg410o38mpc1` (`project_id`),
  KEY `FK4j5ghgqupvpvcwc1gejrqyn48` (`property_id`),
  KEY `FK9jul63hjlrqrbwqs92x3x7t3y` (`rectification_owner_id`),
  KEY `FKhy1nn530k2eq3mdv7fcyw7fys` (`stage_id`),
  KEY `FKq0ht99hecx8bhp5a1lr79gmd8` (`store_id`),
  KEY `FKg8ayu1i0bx75yup1o0x8052d6` (`emp_id`),
  KEY `FK25xdnbc8acyrdb03lrttbod0m` (`team_id`),
  CONSTRAINT `FK25xdnbc8acyrdb03lrttbod0m` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FK4j5ghgqupvpvcwc1gejrqyn48` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FK5b4e7idsp987f9kd3f2g34b1k` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK7rmd6t7iaq6wccngq531879rw` FOREIGN KEY (`confirmed_by`) REFERENCES `db_tenant_credentials` (`cre_id`),
  CONSTRAINT `FK9jul63hjlrqrbwqs92x3x7t3y` FOREIGN KEY (`rectification_owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK9tc48g5fr4y8ocg410o38mpc1` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKg8ayu1i0bx75yup1o0x8052d6` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKgf3e695tinvcqrgywi15tycdq` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKhy1nn530k2eq3mdv7fcyw7fys` FOREIGN KEY (`stage_id`) REFERENCES `db_project_stage` (`stage_id`),
  CONSTRAINT `FKq0ht99hecx8bhp5a1lr79gmd8` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_acceptance_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_acceptance_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_acceptance`
--

LOCK TABLES `db_acceptance` WRITE;
/*!40000 ALTER TABLE `db_acceptance` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_acceptance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_acceptance_evidence`
--

DROP TABLE IF EXISTS `db_acceptance_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_acceptance_evidence` (
  `acceptance_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKnxrpkicmhev7q49equytx5vwb` (`acceptance_id`),
  CONSTRAINT `FKnxrpkicmhev7q49equytx5vwb` FOREIGN KEY (`acceptance_id`) REFERENCES `db_acceptance` (`acceptance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_acceptance_evidence`
--

LOCK TABLES `db_acceptance_evidence` WRITE;
/*!40000 ALTER TABLE `db_acceptance_evidence` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_acceptance_evidence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_acceptance_issue`
--

DROP TABLE IF EXISTS `db_acceptance_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_acceptance_issue` (
  `acceptance_id` bigint NOT NULL,
  `issue` varchar(512) DEFAULT NULL,
  KEY `FK2dqml84qfmjfb8g1dqs5gfyg9` (`acceptance_id`),
  CONSTRAINT `FK2dqml84qfmjfb8g1dqs5gfyg9` FOREIGN KEY (`acceptance_id`) REFERENCES `db_acceptance` (`acceptance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_acceptance_issue`
--

LOCK TABLES `db_acceptance_issue` WRITE;
/*!40000 ALTER TABLE `db_acceptance_issue` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_acceptance_issue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_acceptance_round`
--

DROP TABLE IF EXISTS `db_acceptance_round`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_acceptance_round` (
  `round_id` bigint NOT NULL,
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
  `conclusion` varchar(1024) DEFAULT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `rectification_due_date` date DEFAULT NULL,
  `rectification_note` tinytext,
  `result` enum('Confirmed','Pending','Rejected') NOT NULL,
  `round_no` int NOT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `acceptance_id` bigint NOT NULL,
  `com_id` bigint NOT NULL,
  `confirmed_by_id` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `rectification_owner_id` bigint DEFAULT NULL,
  `stage_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `submitted_by_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`round_id`),
  UNIQUE KEY `uk_acceptance_round_no` (`acceptance_id`,`round_no`),
  KEY `idx_acceptance_round_company` (`com_id`,`round_id`),
  KEY `idx_acceptance_round_company_acceptance` (`com_id`,`acceptance_id`,`round_id`),
  KEY `idx_acceptance_round_company_project` (`com_id`,`project_id`,`round_id`),
  KEY `idx_acceptance_round_company_stage` (`com_id`,`stage_id`,`round_id`),
  KEY `idx_acceptance_round_company_store` (`com_id`,`store_id`,`round_id`),
  KEY `idx_acceptance_round_company_team` (`com_id`,`team_id`,`round_id`),
  KEY `idx_acceptance_round_company_owner` (`com_id`,`owner_id`,`round_id`),
  KEY `FK6kp3l9i8mq71rj46qg8pjp00x` (`confirmed_by_id`),
  KEY `FKojm59xylyrdrd3in6a6cp41hm` (`owner_id`),
  KEY `FKrx1h7nsteq3ignxyfhknl7it8` (`project_id`),
  KEY `FKaa7kwo9qmcrhwoup6goqmr8uk` (`property_id`),
  KEY `FKtowi6jo54j7sroj417kn8hrgm` (`rectification_owner_id`),
  KEY `FKa5ohmedgsi6uty8hvv7t01nxo` (`stage_id`),
  KEY `FK8k1n1gs5m9elnt1j5qoe4d8an` (`store_id`),
  KEY `FK3m4wd2jthp13ltwirku12s0lm` (`submitted_by_id`),
  KEY `FKocb35s0pexuv8w2a7fcpiugjc` (`team_id`),
  CONSTRAINT `FK3m4wd2jthp13ltwirku12s0lm` FOREIGN KEY (`submitted_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK6kp3l9i8mq71rj46qg8pjp00x` FOREIGN KEY (`confirmed_by_id`) REFERENCES `db_tenant_credentials` (`cre_id`),
  CONSTRAINT `FK8k1n1gs5m9elnt1j5qoe4d8an` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKa5ohmedgsi6uty8hvv7t01nxo` FOREIGN KEY (`stage_id`) REFERENCES `db_project_stage` (`stage_id`),
  CONSTRAINT `FKaa7kwo9qmcrhwoup6goqmr8uk` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKhda5014gke3p7yt6qumq83x6p` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKocb35s0pexuv8w2a7fcpiugjc` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKojm59xylyrdrd3in6a6cp41hm` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKrx1h7nsteq3ignxyfhknl7it8` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKs9mnr1bi6nsuhopfln6a107so` FOREIGN KEY (`acceptance_id`) REFERENCES `db_acceptance` (`acceptance_id`),
  CONSTRAINT `FKtowi6jo54j7sroj417kn8hrgm` FOREIGN KEY (`rectification_owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_acceptance_round_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_acceptance_round_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_acceptance_round`
--

LOCK TABLES `db_acceptance_round` WRITE;
/*!40000 ALTER TABLE `db_acceptance_round` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_acceptance_round` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_acceptance_round_evidence`
--

DROP TABLE IF EXISTS `db_acceptance_round_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_acceptance_round_evidence` (
  `round_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKkfdqfgypf0yjlpkpjtl6i5ic1` (`round_id`),
  CONSTRAINT `FKkfdqfgypf0yjlpkpjtl6i5ic1` FOREIGN KEY (`round_id`) REFERENCES `db_acceptance_round` (`round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_acceptance_round_evidence`
--

LOCK TABLES `db_acceptance_round_evidence` WRITE;
/*!40000 ALTER TABLE `db_acceptance_round_evidence` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_acceptance_round_evidence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_acceptance_round_issue`
--

DROP TABLE IF EXISTS `db_acceptance_round_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_acceptance_round_issue` (
  `round_id` bigint NOT NULL,
  `issue` varchar(512) DEFAULT NULL,
  KEY `FK1emoisjx525tr1e3hmpo2rc5e` (`round_id`),
  CONSTRAINT `FK1emoisjx525tr1e3hmpo2rc5e` FOREIGN KEY (`round_id`) REFERENCES `db_acceptance_round` (`round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_acceptance_round_issue`
--

LOCK TABLES `db_acceptance_round_issue` WRITE;
/*!40000 ALTER TABLE `db_acceptance_round_issue` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_acceptance_round_issue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_acceptance_round_rectification_evidence`
--

DROP TABLE IF EXISTS `db_acceptance_round_rectification_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_acceptance_round_rectification_evidence` (
  `round_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FK9agiikhqhuk769xogcvb35l6v` (`round_id`),
  CONSTRAINT `FK9agiikhqhuk769xogcvb35l6v` FOREIGN KEY (`round_id`) REFERENCES `db_acceptance_round` (`round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_acceptance_round_rectification_evidence`
--

LOCK TABLES `db_acceptance_round_rectification_evidence` WRITE;
/*!40000 ALTER TABLE `db_acceptance_round_rectification_evidence` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_acceptance_round_rectification_evidence` ENABLE KEYS */;
UNLOCK TABLES;

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
  CONSTRAINT `FKp64xod7bvm8falnuqj6d97c82` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_business_target_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_business_target_chk_2` CHECK ((`update_week` between 0 and 6))
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
-- Table structure for table `db_company_construction_plan`
--

DROP TABLE IF EXISTS `db_company_construction_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_company_construction_plan` (
  `template_id` bigint NOT NULL,
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
  `code` varchar(64) NOT NULL,
  `is_default` bit(1) NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `sort` int NOT NULL,
  `source_baseline_code` varchar(64) DEFAULT NULL,
  `source_baseline_version` int DEFAULT NULL,
  `status` enum('Disabled','Draft','Enabled') NOT NULL,
  `total_days` int NOT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_company_construction_plan_code` (`com_id`,`code`),
  KEY `idx_company_construction_plan_company` (`com_id`,`template_id`),
  KEY `idx_company_construction_plan_status` (`com_id`,`status`,`is_default`),
  CONSTRAINT `FK4unlixdqv7beo7quf9qfxn8ko` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_company_construction_plan_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_company_construction_plan_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_company_construction_plan`
--

LOCK TABLES `db_company_construction_plan` WRITE;
/*!40000 ALTER TABLE `db_company_construction_plan` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_company_construction_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_company_construction_stage`
--

DROP TABLE IF EXISTS `db_company_construction_stage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_company_construction_stage` (
  `stage_id` bigint NOT NULL,
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
  `acceptance_mode` enum('AcceptanceDone','ConstructionDone','None') NOT NULL,
  `code` varchar(64) NOT NULL,
  `default_days` int NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `sort` int NOT NULL,
  `com_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  PRIMARY KEY (`stage_id`),
  UNIQUE KEY `uk_company_construction_stage_code` (`plan_id`,`code`),
  KEY `idx_company_construction_stage_company` (`com_id`,`stage_id`),
  KEY `idx_company_construction_stage_plan` (`plan_id`,`sort`),
  CONSTRAINT `FK1vwpaf7nlx2alijntwi55ouh9` FOREIGN KEY (`plan_id`) REFERENCES `db_company_construction_plan` (`template_id`),
  CONSTRAINT `FKox6w5cbrkx5ktgv1jw6ca10mp` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_company_construction_stage_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_company_construction_stage_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_company_construction_stage`
--

LOCK TABLES `db_company_construction_stage` WRITE;
/*!40000 ALTER TABLE `db_company_construction_stage` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_company_construction_stage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_company_join_limit`
--

DROP TABLE IF EXISTS `db_company_join_limit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_company_join_limit` (
  `limit_id` bigint NOT NULL,
  `blacklist_until` datetime(6) DEFAULT NULL,
  `join_count_180d` int NOT NULL,
  `last_invite_accept_at` datetime(6) DEFAULT NULL,
  `last_join_at` datetime(6) DEFAULT NULL,
  `last_resign_at` datetime(6) DEFAULT NULL,
  `phone` varchar(20) NOT NULL,
  `version` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`limit_id`),
  UNIQUE KEY `idx_join_limit_phone` (`phone`),
  UNIQUE KEY `idx_join_limit_user` (`user_id`),
  CONSTRAINT `FKcy5wqoxx5ox9kvxefy12chbtk` FOREIGN KEY (`user_id`) REFERENCES `db_tenant_credentials` (`cre_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_company_join_limit`
--

LOCK TABLES `db_company_join_limit` WRITE;
/*!40000 ALTER TABLE `db_company_join_limit` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_company_join_limit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_company_member_log`
--

DROP TABLE IF EXISTS `db_company_member_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_company_member_log` (
  `log_id` bigint NOT NULL,
  `action` enum('Handover','Join','Resign','Resume','RoleChange','StoreChange','Suspend','Switch','TeamChange') NOT NULL,
  `after_snapshot` longtext,
  `before_snapshot` longtext,
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
  `reason` varchar(500) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_member_log_company_user` (`com_id`,`user_id`,`create_date_time`),
  KEY `idx_member_log_company_emp` (`com_id`,`emp_id`,`create_date_time`),
  KEY `FKfgd1epma7uucffhdw9f4pk7o4` (`emp_id`),
  KEY `FKa7wsrg7s3ojvqf8sxm8sy6epi` (`operator_id`),
  KEY `FKbdlct2wtwqc4k4u292f2tpg78` (`user_id`),
  CONSTRAINT `FKa7wsrg7s3ojvqf8sxm8sy6epi` FOREIGN KEY (`operator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKbdlct2wtwqc4k4u292f2tpg78` FOREIGN KEY (`user_id`) REFERENCES `db_tenant_credentials` (`cre_id`),
  CONSTRAINT `FKfgd1epma7uucffhdw9f4pk7o4` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKhlplmbrq91js28jk7wtq6nhu3` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_company_member_log_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_company_member_log`
--

LOCK TABLES `db_company_member_log` WRITE;
/*!40000 ALTER TABLE `db_company_member_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_company_member_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_company_payment_node`
--

DROP TABLE IF EXISTS `db_company_payment_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_company_payment_node` (
  `node_id` bigint NOT NULL,
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
  `code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `offset_days` int NOT NULL,
  `ratio` decimal(10,4) NOT NULL,
  `sort` int NOT NULL,
  `trigger_stage_code` varchar(64) DEFAULT NULL,
  `trigger_type` enum('ContractSigned','ProjectFinished','StageAccepted','StageDone','StageStarted') NOT NULL,
  `type` enum('Final','First','Other','Progress','Retention') NOT NULL,
  `com_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  PRIMARY KEY (`node_id`),
  UNIQUE KEY `uk_company_payment_node_code` (`plan_id`,`code`),
  KEY `idx_company_payment_node_company` (`com_id`,`node_id`),
  KEY `idx_company_payment_node_plan` (`plan_id`,`sort`),
  CONSTRAINT `FKnys0r0ch89ic4wwl0k5wrvwr3` FOREIGN KEY (`plan_id`) REFERENCES `db_company_payment_plan` (`template_id`),
  CONSTRAINT `FKr5042hb0qw7htcdrlp8cmwiaj` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_company_payment_node_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_company_payment_node_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_company_payment_node`
--

LOCK TABLES `db_company_payment_node` WRITE;
/*!40000 ALTER TABLE `db_company_payment_node` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_company_payment_node` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_company_payment_plan`
--

DROP TABLE IF EXISTS `db_company_payment_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_company_payment_plan` (
  `template_id` bigint NOT NULL,
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
  `code` varchar(64) NOT NULL,
  `is_default` bit(1) NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `sort` int NOT NULL,
  `source_baseline_code` varchar(64) DEFAULT NULL,
  `source_baseline_version` int DEFAULT NULL,
  `status` enum('Disabled','Draft','Enabled') NOT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_company_payment_plan_code` (`com_id`,`code`),
  KEY `idx_company_payment_plan_company` (`com_id`,`template_id`),
  KEY `idx_company_payment_plan_status` (`com_id`,`status`,`is_default`),
  CONSTRAINT `FKn3rnf9hglyfqu5aap16ykonx5` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_company_payment_plan_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_company_payment_plan_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_company_payment_plan`
--

LOCK TABLES `db_company_payment_plan` WRITE;
/*!40000 ALTER TABLE `db_company_payment_plan` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_company_payment_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_contract_change`
--

DROP TABLE IF EXISTS `db_contract_change`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_contract_change` (
  `change_id` bigint NOT NULL,
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
  `amount` decimal(14,2) NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `conclusion` varchar(500) DEFAULT NULL,
  `cost_delta` decimal(14,2) NOT NULL,
  `customer_confirmed` bit(1) NOT NULL,
  `customer_confirmed_at` datetime(6) DEFAULT NULL,
  `no` varchar(64) NOT NULL,
  `reason` varchar(500) NOT NULL,
  `signature_url` varchar(512) DEFAULT NULL,
  `status` enum('Approved','Cancelled','CustomerConfirmed','Pending','Rejected') NOT NULL,
  `type` enum('Add','Price','Quantity','Remove','Replace') NOT NULL,
  `approver_id` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `contract_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `requester_id` bigint NOT NULL,
  `source_line_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`change_id`),
  UNIQUE KEY `UKao6bf082v7baoo2ed6konj3oe` (`no`),
  KEY `idx_change_company` (`com_id`,`change_id`),
  KEY `idx_change_company_contract` (`com_id`,`contract_id`,`change_id`),
  KEY `idx_change_company_project` (`com_id`,`project_id`,`change_id`),
  KEY `idx_contract_change_company` (`com_id`,`change_id`),
  KEY `idx_contract_change_company_store` (`com_id`,`store_id`,`change_id`),
  KEY `idx_contract_change_company_team` (`com_id`,`team_id`,`change_id`),
  KEY `idx_contract_change_company_owner` (`com_id`,`owner_id`,`change_id`),
  KEY `FK30t8ccbuef32ejf2g6dnx5tve` (`approver_id`),
  KEY `FK3w9odcbmbj99ltl4ovelfmrki` (`contract_id`),
  KEY `FK4e3qkwm6gpn9sj8rnsdps5jn5` (`owner_id`),
  KEY `FKo4cgo5hn8inlp6adhilcholxq` (`project_id`),
  KEY `FKic55e4d0d3a5r12eggivtkxec` (`property_id`),
  KEY `FK6ilyclmr5pugvyxbpv40j1q25` (`requester_id`),
  KEY `FKk8p5pbg6ld10m0ua92lebsob4` (`source_line_id`),
  KEY `FKj7ya817c6xpdaj62lj6aplaok` (`store_id`),
  KEY `FK3yxsc69qih7ipsk55d33fgklk` (`team_id`),
  CONSTRAINT `FK30t8ccbuef32ejf2g6dnx5tve` FOREIGN KEY (`approver_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK3w9odcbmbj99ltl4ovelfmrki` FOREIGN KEY (`contract_id`) REFERENCES `db_project_contract` (`contract_id`),
  CONSTRAINT `FK3yxsc69qih7ipsk55d33fgklk` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FK4e3qkwm6gpn9sj8rnsdps5jn5` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK6ilyclmr5pugvyxbpv40j1q25` FOREIGN KEY (`requester_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKic55e4d0d3a5r12eggivtkxec` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKj7ya817c6xpdaj62lj6aplaok` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKk8p5pbg6ld10m0ua92lebsob4` FOREIGN KEY (`source_line_id`) REFERENCES `db_contract_line` (`line_id`),
  CONSTRAINT `FKo4cgo5hn8inlp6adhilcholxq` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKpi2q5tix0woatguqhbq4cwi9n` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_contract_change_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_contract_change_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_contract_change`
--

LOCK TABLES `db_contract_change` WRITE;
/*!40000 ALTER TABLE `db_contract_change` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_contract_change` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_contract_change_image`
--

DROP TABLE IF EXISTS `db_contract_change_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_contract_change_image` (
  `change_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKenpabt0pxd1f18cv8rnbxs3bs` (`change_id`),
  CONSTRAINT `FKenpabt0pxd1f18cv8rnbxs3bs` FOREIGN KEY (`change_id`) REFERENCES `db_contract_change` (`change_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_contract_change_image`
--

LOCK TABLES `db_contract_change_image` WRITE;
/*!40000 ALTER TABLE `db_contract_change_image` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_contract_change_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_contract_change_item`
--

DROP TABLE IF EXISTS `db_contract_change_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_contract_change_item` (
  `change_item_id` bigint NOT NULL,
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
  `amount_delta` decimal(14,2) NOT NULL,
  `change_type` enum('Add','Price','Quantity','Remove','Replace') NOT NULL,
  `cost_delta` decimal(14,2) NOT NULL,
  `item_type` enum('Appliance','Construction','Custom','General','Material','Soft') DEFAULT NULL,
  `name` varchar(256) NOT NULL,
  `new_price` decimal(12,2) NOT NULL,
  `new_quantity` decimal(12,3) NOT NULL,
  `old_price` decimal(12,2) NOT NULL,
  `old_quantity` decimal(12,3) NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `change_id` bigint NOT NULL,
  `com_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `contract_line_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`change_item_id`),
  KEY `idx_change_item_company` (`com_id`,`change_item_id`),
  KEY `idx_change_item_change` (`com_id`,`change_id`,`change_item_id`),
  KEY `idx_contract_change_item_company` (`com_id`,`change_item_id`),
  KEY `idx_contract_change_item_company_store` (`com_id`,`store_id`,`change_item_id`),
  KEY `idx_contract_change_item_company_team` (`com_id`,`team_id`,`change_item_id`),
  KEY `idx_contract_change_item_company_owner` (`com_id`,`owner_id`,`change_item_id`),
  KEY `FK47r679g8b25hipt0fayg38xte` (`change_id`),
  KEY `FK4ca3u5kscfom4fh6pn2ay2nke` (`owner_id`),
  KEY `FK4m0y6xle0vgn7y2q1pchs8umb` (`contract_line_id`),
  KEY `FK9pu5npglfmb9xgb8dyrbrtmv3` (`store_id`),
  KEY `FKdvwcl8dytsgl8w1mjprvygdaq` (`team_id`),
  CONSTRAINT `FK47r679g8b25hipt0fayg38xte` FOREIGN KEY (`change_id`) REFERENCES `db_contract_change` (`change_id`),
  CONSTRAINT `FK4ca3u5kscfom4fh6pn2ay2nke` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK4m0y6xle0vgn7y2q1pchs8umb` FOREIGN KEY (`contract_line_id`) REFERENCES `db_contract_line` (`line_id`),
  CONSTRAINT `FK9pu5npglfmb9xgb8dyrbrtmv3` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKdvwcl8dytsgl8w1mjprvygdaq` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKfktuftj4g81uaaqvtw6xq21nn` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_contract_change_item_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_contract_change_item_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_contract_change_item`
--

LOCK TABLES `db_contract_change_item` WRITE;
/*!40000 ALTER TABLE `db_contract_change_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_contract_change_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_contract_construction_plan`
--

DROP TABLE IF EXISTS `db_contract_construction_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_contract_construction_plan` (
  `plan_id` bigint NOT NULL,
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
  `end_date` date NOT NULL,
  `name` varchar(128) NOT NULL,
  `source_code` varchar(64) NOT NULL,
  `source_version` int NOT NULL,
  `start_date` date NOT NULL,
  `status` enum('Confirmed','Draft','Superseded') NOT NULL,
  `com_id` bigint NOT NULL,
  `contract_id` bigint NOT NULL,
  PRIMARY KEY (`plan_id`),
  UNIQUE KEY `uk_contract_construction_plan_contract` (`contract_id`),
  KEY `FKksoyfnxfoi17v17i8f09i6rkf` (`com_id`),
  CONSTRAINT `FKeuyl8oc3tuc8pelyk5i029ynq` FOREIGN KEY (`contract_id`) REFERENCES `db_project_contract` (`contract_id`),
  CONSTRAINT `FKksoyfnxfoi17v17i8f09i6rkf` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_contract_construction_plan_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_contract_construction_plan_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_contract_construction_plan`
--

LOCK TABLES `db_contract_construction_plan` WRITE;
/*!40000 ALTER TABLE `db_contract_construction_plan` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_contract_construction_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_contract_construction_stage`
--

DROP TABLE IF EXISTS `db_contract_construction_stage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_contract_construction_stage` (
  `stage_id` bigint NOT NULL,
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
  `acceptance_mode` enum('AcceptanceDone','ConstructionDone','None') NOT NULL,
  `code` varchar(64) NOT NULL,
  `default_days` int NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `planned_end` date NOT NULL,
  `planned_start` date NOT NULL,
  `sort` int NOT NULL,
  `com_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `project_stage_id` bigint DEFAULT NULL,
  PRIMARY KEY (`stage_id`),
  UNIQUE KEY `UKeuxkgu7t8qfma5d0xdtmg0xeu` (`project_stage_id`),
  KEY `idx_contract_construction_stage_company` (`com_id`,`stage_id`),
  KEY `idx_contract_construction_stage_plan` (`plan_id`,`sort`),
  CONSTRAINT `FKdy4bk82bw3s2fqpynxdwdrxgr` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKl9ivw6vd1id79jcw345pv29df` FOREIGN KEY (`plan_id`) REFERENCES `db_contract_construction_plan` (`plan_id`),
  CONSTRAINT `FKshg1usywsji2w4bdiapdlgfra` FOREIGN KEY (`project_stage_id`) REFERENCES `db_project_stage` (`stage_id`),
  CONSTRAINT `db_contract_construction_stage_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_contract_construction_stage_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_contract_construction_stage`
--

LOCK TABLES `db_contract_construction_stage` WRITE;
/*!40000 ALTER TABLE `db_contract_construction_stage` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_contract_construction_stage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_contract_line`
--

DROP TABLE IF EXISTS `db_contract_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_contract_line` (
  `line_id` bigint NOT NULL,
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
  `amount` decimal(12,2) NOT NULL,
  `brand` varchar(128) DEFAULT NULL,
  `category` varchar(64) NOT NULL,
  `cost` decimal(12,2) NOT NULL,
  `formula` varchar(256) DEFAULT NULL,
  `manual_price` decimal(12,2) NOT NULL,
  `material_price` decimal(12,2) NOT NULL,
  `name` varchar(256) NOT NULL,
  `pricing_mode` enum('FixedTotal','ManualOnly','MaterialAndManual','MaterialOnly') NOT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `role` enum('Add','Base','Deduct','Gift','Option','Replace','Upgrade') DEFAULT NULL,
  `sort` int NOT NULL,
  `space_name` varchar(128) DEFAULT NULL,
  `spec` varchar(256) DEFAULT NULL,
  `type` enum('Appliance','Construction','Custom','General','Material','Soft') NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') NOT NULL,
  `com_id` bigint NOT NULL,
  `contract_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `property_id` bigint NOT NULL,
  `source_quote_line_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`line_id`),
  KEY `idx_contract_line_company` (`com_id`,`line_id`),
  KEY `idx_contract_line_contract` (`com_id`,`contract_id`,`line_id`),
  KEY `idx_contract_line_property` (`com_id`,`property_id`,`line_id`),
  KEY `idx_contract_line_company_store` (`com_id`,`store_id`,`line_id`),
  KEY `idx_contract_line_company_team` (`com_id`,`team_id`,`line_id`),
  KEY `idx_contract_line_company_owner` (`com_id`,`owner_id`,`line_id`),
  KEY `FK3lp0ma8lc4vsbymv4onoqof4y` (`contract_id`),
  KEY `FKo2nanc4spl2he96uhc515fe8s` (`owner_id`),
  KEY `FKbbe24joa874dlsn7uy4wyv2ce` (`property_id`),
  KEY `FKmqw2e5hjajqtcv32dqgv0ptkg` (`source_quote_line_id`),
  KEY `FKrkc6gfbrq6mclose3ctehljnd` (`store_id`),
  KEY `FKgamtlii4vhcvb2c9791h8y6ws` (`team_id`),
  CONSTRAINT `FK3lp0ma8lc4vsbymv4onoqof4y` FOREIGN KEY (`contract_id`) REFERENCES `db_project_contract` (`contract_id`),
  CONSTRAINT `FKbbe24joa874dlsn7uy4wyv2ce` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKgamtlii4vhcvb2c9791h8y6ws` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKm3qrvenuqjyn4coadnttjk52g` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKmqw2e5hjajqtcv32dqgv0ptkg` FOREIGN KEY (`source_quote_line_id`) REFERENCES `quote_line` (`item_id`),
  CONSTRAINT `FKo2nanc4spl2he96uhc515fe8s` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKrkc6gfbrq6mclose3ctehljnd` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_contract_line_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_contract_line_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_contract_line`
--

LOCK TABLES `db_contract_line` WRITE;
/*!40000 ALTER TABLE `db_contract_line` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_contract_line` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_contract_payment_node`
--

DROP TABLE IF EXISTS `db_contract_payment_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_contract_payment_node` (
  `node_id` bigint NOT NULL,
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
  `amount` decimal(14,2) NOT NULL,
  `code` varchar(64) NOT NULL,
  `due_date` date NOT NULL,
  `name` varchar(128) NOT NULL,
  `offset_days` int NOT NULL,
  `ratio` decimal(8,4) NOT NULL,
  `sort` int NOT NULL,
  `trigger_stage_code` varchar(64) DEFAULT NULL,
  `trigger_stage_name` varchar(128) DEFAULT NULL,
  `trigger_type` enum('ContractSigned','ProjectFinished','StageAccepted','StageDone','StageStarted') NOT NULL,
  `type` enum('Final','First','Other','Progress','Retention') NOT NULL,
  `com_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `receivable_id` bigint DEFAULT NULL,
  PRIMARY KEY (`node_id`),
  UNIQUE KEY `UKhpu7msb6lwcio5jdp5rvetanw` (`receivable_id`),
  KEY `idx_contract_payment_node_company` (`com_id`,`node_id`),
  KEY `idx_contract_payment_node_plan` (`plan_id`,`sort`),
  CONSTRAINT `FK8wmtj0imc2gy1f2indy6q8bp8` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKh072w5824pdck3pna6d0vvjyp` FOREIGN KEY (`receivable_id`) REFERENCES `db_finance_receivable` (`receivable_id`),
  CONSTRAINT `FKuh1a8erajq1xhnoe95q29lsw` FOREIGN KEY (`plan_id`) REFERENCES `db_contract_payment_plan` (`plan_id`),
  CONSTRAINT `db_contract_payment_node_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_contract_payment_node_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_contract_payment_node`
--

LOCK TABLES `db_contract_payment_node` WRITE;
/*!40000 ALTER TABLE `db_contract_payment_node` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_contract_payment_node` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_contract_payment_plan`
--

DROP TABLE IF EXISTS `db_contract_payment_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_contract_payment_plan` (
  `plan_id` bigint NOT NULL,
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
  `name` varchar(128) NOT NULL,
  `source_code` varchar(64) NOT NULL,
  `source_version` int NOT NULL,
  `status` enum('Confirmed','Draft','Superseded') NOT NULL,
  `total_amount` decimal(14,2) NOT NULL,
  `com_id` bigint NOT NULL,
  `contract_id` bigint NOT NULL,
  PRIMARY KEY (`plan_id`),
  UNIQUE KEY `uk_contract_payment_plan_contract` (`contract_id`),
  KEY `FKpgi8cn3mmiwuiuhwfv16u6j7w` (`com_id`),
  CONSTRAINT `FK6sp2ws7snafr0j4w88c47yrp6` FOREIGN KEY (`contract_id`) REFERENCES `db_project_contract` (`contract_id`),
  CONSTRAINT `FKpgi8cn3mmiwuiuhwfv16u6j7w` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_contract_payment_plan_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_contract_payment_plan_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_contract_payment_plan`
--

LOCK TABLES `db_contract_payment_plan` WRITE;
/*!40000 ALTER TABLE `db_contract_payment_plan` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_contract_payment_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_design`
--

DROP TABLE IF EXISTS `db_design`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design` (
  `design_id` bigint NOT NULL,
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
  `area` decimal(12,2) NOT NULL,
  `cad_convert_status` varchar(16) DEFAULT NULL,
  `cad_preview_url` varchar(512) DEFAULT NULL,
  `cad_url` varchar(512) DEFAULT NULL,
  `cover_url` varchar(512) DEFAULT NULL,
  `demolition_url` varchar(512) DEFAULT NULL,
  `description` longtext,
  `layout` varchar(256) DEFAULT NULL,
  `masonry_url` varchar(512) DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `no` varchar(64) NOT NULL,
  `optimization_url` varchar(512) DEFAULT NULL,
  `original_plan_url` varchar(512) DEFAULT NULL,
  `status` enum('Archived','Draft','Presented','Ready','Selected') NOT NULL,
  `style` varchar(64) DEFAULT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`design_id`),
  UNIQUE KEY `UK3utricqnce3967ji75qwqm8gk` (`no`),
  KEY `idx_design_company` (`com_id`,`design_id`),
  KEY `idx_design_company_store` (`com_id`,`store_id`,`design_id`),
  KEY `idx_design_company_team` (`com_id`,`team_id`,`design_id`),
  KEY `idx_design_company_owner` (`com_id`,`emp_id`,`design_id`),
  KEY `idx_design_company_customer` (`com_id`,`customer_id`,`design_id`),
  KEY `FKcm8i0si17xg8a0u18npt1lf92` (`customer_id`),
  KEY `FKbirugqyi76vwvuwtdyftp6e45` (`emp_id`),
  KEY `FKbnn0lrh6mejhy352i783ilyji` (`property_id`),
  KEY `FK5pj0ugjtgdnlvhba5v27ebb8e` (`store_id`),
  KEY `FK6p5m6foywvo7j5pngf2fnwkes` (`team_id`),
  CONSTRAINT `FK5pj0ugjtgdnlvhba5v27ebb8e` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FK6p5m6foywvo7j5pngf2fnwkes` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FK6sfsgoof2kgy27c4cd2gp136b` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKbirugqyi76vwvuwtdyftp6e45` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKbnn0lrh6mejhy352i783ilyji` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKcm8i0si17xg8a0u18npt1lf92` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `db_design_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_design_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_design`
--

LOCK TABLES `db_design` WRITE;
/*!40000 ALTER TABLE `db_design` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_design` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_design_item`
--

DROP TABLE IF EXISTS `db_design_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_item` (
  `item_id` bigint NOT NULL,
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
  `amount` decimal(12,2) NOT NULL,
  `brand` varchar(128) DEFAULT NULL,
  `category` varchar(64) NOT NULL,
  `cost` decimal(12,2) NOT NULL,
  `cost_price` decimal(12,2) NOT NULL,
  `criterion` varchar(512) DEFAULT NULL,
  `formula` varchar(256) DEFAULT NULL,
  `manual_price` decimal(12,2) NOT NULL,
  `material_price` decimal(12,2) NOT NULL,
  `name` varchar(128) NOT NULL,
  `pricing_mode` enum('FixedTotal','ManualOnly','MaterialAndManual','MaterialOnly') NOT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `sort` int NOT NULL,
  `spec` varchar(128) DEFAULT NULL,
  `type` enum('Appliance','Construction','Custom','General','Material','Soft') NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') NOT NULL,
  `catalog_item_id` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `design_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `space_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  KEY `idx_design_item_company` (`com_id`,`item_id`),
  KEY `idx_design_item_company_store` (`com_id`,`store_id`,`item_id`),
  KEY `idx_design_item_company_team` (`com_id`,`team_id`,`item_id`),
  KEY `idx_design_item_company_owner` (`com_id`,`emp_id`,`item_id`),
  KEY `idx_design_item_company_design` (`com_id`,`design_id`,`item_id`),
  KEY `idx_design_item_company_space` (`com_id`,`space_id`,`item_id`),
  KEY `FKtcgigop3rrvy9ws0ypr3g8l33` (`catalog_item_id`),
  KEY `FKksfisof1qf0teorae6mgv6y2u` (`design_id`),
  KEY `FKh4kfipp494w9snpvlxo4ogdpl` (`emp_id`),
  KEY `FKg10ivn0642p8o6bsn6q3fufbk` (`property_id`),
  KEY `FK8tssdj4mn2sjwrtndg6u3w4f` (`space_id`),
  KEY `FK7xqdn82135lmpmwd9csaovjvr` (`store_id`),
  KEY `FKs4hnght71ni4hvegqs6kuuuwn` (`team_id`),
  CONSTRAINT `FK7xqdn82135lmpmwd9csaovjvr` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FK8jswww5ctud8eit2f2oxh3o5c` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK8tssdj4mn2sjwrtndg6u3w4f` FOREIGN KEY (`space_id`) REFERENCES `db_design_space` (`space_id`),
  CONSTRAINT `FKg10ivn0642p8o6bsn6q3fufbk` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKh4kfipp494w9snpvlxo4ogdpl` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKksfisof1qf0teorae6mgv6y2u` FOREIGN KEY (`design_id`) REFERENCES `db_design` (`design_id`),
  CONSTRAINT `FKs4hnght71ni4hvegqs6kuuuwn` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKtcgigop3rrvy9ws0ypr3g8l33` FOREIGN KEY (`catalog_item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `db_design_item_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_design_item_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_design_item`
--

LOCK TABLES `db_design_item` WRITE;
/*!40000 ALTER TABLE `db_design_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_design_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_design_item_images`
--

DROP TABLE IF EXISTS `db_design_item_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_item_images` (
  `item_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKltvl336sfiu8451469fcbi4qa` (`item_id`),
  CONSTRAINT `FKltvl336sfiu8451469fcbi4qa` FOREIGN KEY (`item_id`) REFERENCES `db_design_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_design_item_images`
--

LOCK TABLES `db_design_item_images` WRITE;
/*!40000 ALTER TABLE `db_design_item_images` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_design_item_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_design_original_plan_images`
--

DROP TABLE IF EXISTS `db_design_original_plan_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_original_plan_images` (
  `design_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKinhgvgyj3s4a2ejv97ke8dev2` (`design_id`),
  CONSTRAINT `FKinhgvgyj3s4a2ejv97ke8dev2` FOREIGN KEY (`design_id`) REFERENCES `db_design` (`design_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_design_original_plan_images`
--

LOCK TABLES `db_design_original_plan_images` WRITE;
/*!40000 ALTER TABLE `db_design_original_plan_images` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_design_original_plan_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_design_requirement`
--

DROP TABLE IF EXISTS `db_design_requirement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_requirement` (
  `requirement_id` bigint NOT NULL,
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
  `category` enum('Budget','Function','Material','Other','Quality','Schedule','Style') NOT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `detail` longtext,
  `kind` enum('Demand','Reject') NOT NULL,
  `priority` enum('High','Low','Medium') NOT NULL,
  `response` longtext,
  `source` varchar(32) DEFAULT NULL,
  `space_name` varchar(64) DEFAULT NULL,
  `status` enum('Confirmed','Pending','Resolved') NOT NULL,
  `title` varchar(128) NOT NULL,
  `type` enum('Balcony','Bathroom','Bedroom','DiningRoom','Kitchen','LivingRoom') DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `emp_id` bigint DEFAULT NULL,
  `property_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`requirement_id`),
  KEY `idx_requirement_company` (`com_id`,`requirement_id`),
  KEY `idx_requirement_company_store` (`com_id`,`store_id`,`requirement_id`),
  KEY `idx_requirement_company_team` (`com_id`,`team_id`,`requirement_id`),
  KEY `idx_requirement_company_owner` (`com_id`,`emp_id`,`requirement_id`),
  KEY `idx_requirement_company_customer` (`com_id`,`customer_id`,`requirement_id`),
  KEY `FKco2r2dyp5jpa35htrsocuiuvv` (`customer_id`),
  KEY `FK9h7o7u8c7gjq7qq1u5cp1t73j` (`emp_id`),
  KEY `FK4q5y8uiw93uxh769cgm7doy7w` (`property_id`),
  KEY `FKdn7idcjk62fq4qqgjrhlhnokl` (`store_id`),
  KEY `FK5w3tp4sjq4fwnvamxs9s2sky2` (`team_id`),
  CONSTRAINT `FK4q5y8uiw93uxh769cgm7doy7w` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FK5w3tp4sjq4fwnvamxs9s2sky2` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FK9h7o7u8c7gjq7qq1u5cp1t73j` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKco2r2dyp5jpa35htrsocuiuvv` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKdn7idcjk62fq4qqgjrhlhnokl` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKrphtx5aioqkm2390meufx993` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_design_requirement_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_design_requirement_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_design_requirement`
--

LOCK TABLES `db_design_requirement` WRITE;
/*!40000 ALTER TABLE `db_design_requirement` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_design_requirement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_design_site_photos`
--

DROP TABLE IF EXISTS `db_design_site_photos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_site_photos` (
  `design_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKc6vdlwgwuu2o9ygxhe1whcv4t` (`design_id`),
  CONSTRAINT `FKc6vdlwgwuu2o9ygxhe1whcv4t` FOREIGN KEY (`design_id`) REFERENCES `db_design` (`design_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_design_site_photos`
--

LOCK TABLES `db_design_site_photos` WRITE;
/*!40000 ALTER TABLE `db_design_site_photos` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_design_site_photos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_design_space`
--

DROP TABLE IF EXISTS `db_design_space`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_space` (
  `space_id` bigint NOT NULL,
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
  `area` decimal(12,2) NOT NULL,
  `height` decimal(10,3) DEFAULT NULL,
  `length_m` decimal(10,3) DEFAULT NULL,
  `name` varchar(64) NOT NULL,
  `perimeter` decimal(12,2) NOT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `sort` int NOT NULL,
  `type` enum('Balcony','Bathroom','Bedroom','DiningRoom','Kitchen','LivingRoom') NOT NULL,
  `width` decimal(10,3) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `design_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`space_id`),
  KEY `idx_design_space_company` (`com_id`,`space_id`),
  KEY `idx_design_space_company_store` (`com_id`,`store_id`,`space_id`),
  KEY `idx_design_space_company_team` (`com_id`,`team_id`,`space_id`),
  KEY `idx_design_space_company_owner` (`com_id`,`emp_id`,`space_id`),
  KEY `idx_design_space_company_design` (`com_id`,`design_id`,`space_id`),
  KEY `FKdgumoqsqxppko4ut7yg9o4e6v` (`design_id`),
  KEY `FK77l034krhdd6olhcu5r9meyaw` (`emp_id`),
  KEY `FK2ta533qf2tc0oi3809twybbwa` (`property_id`),
  KEY `FKfwn3vvs338y19ggn6svdg09hy` (`store_id`),
  KEY `FKjky6x5q8hwn3ppp2h3057cglh` (`team_id`),
  CONSTRAINT `FK2ta533qf2tc0oi3809twybbwa` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FK77l034krhdd6olhcu5r9meyaw` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKdgumoqsqxppko4ut7yg9o4e6v` FOREIGN KEY (`design_id`) REFERENCES `db_design` (`design_id`),
  CONSTRAINT `FKfwn3vvs338y19ggn6svdg09hy` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKjky6x5q8hwn3ppp2h3057cglh` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKp1b0q8vq31jvhla5dwm3ik9w0` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_design_space_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_design_space_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_design_space`
--

LOCK TABLES `db_design_space` WRITE;
/*!40000 ALTER TABLE `db_design_space` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_design_space` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_design_space_images`
--

DROP TABLE IF EXISTS `db_design_space_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_space_images` (
  `space_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKhq8whiwlx4643al574ra9l5si` (`space_id`),
  CONSTRAINT `FKhq8whiwlx4643al574ra9l5si` FOREIGN KEY (`space_id`) REFERENCES `db_design_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_design_space_images`
--

LOCK TABLES `db_design_space_images` WRITE;
/*!40000 ALTER TABLE `db_design_space_images` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_design_space_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_design_survey_photos`
--

DROP TABLE IF EXISTS `db_design_survey_photos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_survey_photos` (
  `design_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FK10s8n0t0x0wcw1ftj6jy14ixp` (`design_id`),
  CONSTRAINT `FK10s8n0t0x0wcw1ftj6jy14ixp` FOREIGN KEY (`design_id`) REFERENCES `db_design` (`design_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_design_survey_photos`
--

LOCK TABLES `db_design_survey_photos` WRITE;
/*!40000 ALTER TABLE `db_design_survey_photos` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_design_survey_photos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_design_talk`
--

DROP TABLE IF EXISTS `db_design_talk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_talk` (
  `talk_id` bigint NOT NULL,
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
  `built_at` datetime(6) DEFAULT NULL,
  `cover_url` varchar(512) DEFAULT NULL,
  `description` longtext,
  `no` varchar(64) NOT NULL,
  `output_url` varchar(512) DEFAULT NULL,
  `status` enum('Archived','Draft','Generated','Published') NOT NULL,
  `title` varchar(128) NOT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `design_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `quote_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`talk_id`),
  UNIQUE KEY `UKoc7hjnh4dfbyb2li78l3ddvis` (`no`),
  KEY `idx_talk_company` (`com_id`,`talk_id`),
  KEY `idx_talk_company_store` (`com_id`,`store_id`,`talk_id`),
  KEY `idx_talk_company_team` (`com_id`,`team_id`,`talk_id`),
  KEY `idx_talk_company_owner` (`com_id`,`emp_id`,`talk_id`),
  KEY `idx_talk_company_customer` (`com_id`,`customer_id`,`talk_id`),
  KEY `FK2jmarmxsreqttcmxg0acl3kgg` (`customer_id`),
  KEY `FKdk95wiilqo2b3fck1bnwl86hn` (`design_id`),
  KEY `FK8eyrwk97jbtypeukg9mgxtvqs` (`emp_id`),
  KEY `FKrexa9wlkvn7jiocjwa57xapka` (`property_id`),
  KEY `FKpjq57oi4t3sg8goxssblsdgbo` (`quote_id`),
  KEY `FKrviptpssbqbipjjjuenuirpoo` (`store_id`),
  KEY `FKr39uinqhj8vfnwwe8x5dab2bm` (`team_id`),
  CONSTRAINT `FK2jmarmxsreqttcmxg0acl3kgg` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FK8eyrwk97jbtypeukg9mgxtvqs` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKbjdlvxqy9740qifek17vjq3el` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKdk95wiilqo2b3fck1bnwl86hn` FOREIGN KEY (`design_id`) REFERENCES `db_design` (`design_id`),
  CONSTRAINT `FKpjq57oi4t3sg8goxssblsdgbo` FOREIGN KEY (`quote_id`) REFERENCES `quote` (`quote_id`),
  CONSTRAINT `FKr39uinqhj8vfnwwe8x5dab2bm` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKrexa9wlkvn7jiocjwa57xapka` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKrviptpssbqbipjjjuenuirpoo` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_design_talk_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_design_talk_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_design_talk`
--

LOCK TABLES `db_design_talk` WRITE;
/*!40000 ALTER TABLE `db_design_talk` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_design_talk` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_design_talk_section`
--

DROP TABLE IF EXISTS `db_design_talk_section`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_talk_section` (
  `section_id` bigint NOT NULL,
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
  `content` longtext,
  `sort` int NOT NULL,
  `subtitle` varchar(256) DEFAULT NULL,
  `title` varchar(128) NOT NULL,
  `type` enum('Appliance','Catalog','Community','Cover','Demolition','End','Investment','Masonry','Material','Optimization','OriginalPlan','Problem','Requirement','RoomDesign','Saving','SitePhoto','SoftDecor','Survey','SurveyReport') NOT NULL,
  `visible` bit(1) NOT NULL,
  `com_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `talk_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`section_id`),
  KEY `idx_talk_section_company` (`com_id`,`section_id`),
  KEY `idx_talk_section_company_store` (`com_id`,`store_id`,`section_id`),
  KEY `idx_talk_section_company_team` (`com_id`,`team_id`,`section_id`),
  KEY `idx_talk_section_company_owner` (`com_id`,`emp_id`,`section_id`),
  KEY `idx_talk_section_company_talk` (`com_id`,`talk_id`,`section_id`),
  KEY `FKllwfh6uj72s07vloepq944igj` (`emp_id`),
  KEY `FK3429o0gjv4w0mnf8sbajqrovk` (`store_id`),
  KEY `FKey9a0rled8iuwqfpwyxnow0sb` (`talk_id`),
  KEY `FKesxmluutqjh5tbdybtqab946i` (`team_id`),
  CONSTRAINT `FK3429o0gjv4w0mnf8sbajqrovk` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKbvwg570meul71e9t4u8rw1si3` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKesxmluutqjh5tbdybtqab946i` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKey9a0rled8iuwqfpwyxnow0sb` FOREIGN KEY (`talk_id`) REFERENCES `db_design_talk` (`talk_id`),
  CONSTRAINT `FKllwfh6uj72s07vloepq944igj` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_design_talk_section_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_design_talk_section_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_design_talk_section`
--

LOCK TABLES `db_design_talk_section` WRITE;
/*!40000 ALTER TABLE `db_design_talk_section` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_design_talk_section` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_design_talk_section_images`
--

DROP TABLE IF EXISTS `db_design_talk_section_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_talk_section_images` (
  `section_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKhr7cwgjhspoc22asfoyddfwrp` (`section_id`),
  CONSTRAINT `FKhr7cwgjhspoc22asfoyddfwrp` FOREIGN KEY (`section_id`) REFERENCES `db_design_talk_section` (`section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_design_talk_section_images`
--

LOCK TABLES `db_design_talk_section_images` WRITE;
/*!40000 ALTER TABLE `db_design_talk_section_images` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_design_talk_section_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_document_print`
--

DROP TABLE IF EXISTS `db_document_print`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_document_print` (
  `print_id` bigint NOT NULL,
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
  `printed_at` datetime(6) NOT NULL,
  `reason` varchar(128) DEFAULT NULL,
  `sequence_no` int NOT NULL,
  `com_id` bigint NOT NULL,
  `document_id` bigint NOT NULL,
  `printed_by_id` bigint NOT NULL,
  PRIMARY KEY (`print_id`),
  KEY `idx_document_print_company` (`com_id`,`print_id`),
  KEY `idx_document_print_document` (`com_id`,`document_id`,`print_id`),
  KEY `FKt7a90ny9vl47heg582x1ddpv4` (`document_id`),
  KEY `FK3c6pda8wqkr343r3r2n2ax8se` (`printed_by_id`),
  CONSTRAINT `FK3c6pda8wqkr343r3r2n2ax8se` FOREIGN KEY (`printed_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKr623fstol1ryl8456sdrw1ija` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKt7a90ny9vl47heg582x1ddpv4` FOREIGN KEY (`document_id`) REFERENCES `db_document_record` (`document_id`),
  CONSTRAINT `db_document_print_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_document_print_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_document_print`
--

LOCK TABLES `db_document_print` WRITE;
/*!40000 ALTER TABLE `db_document_print` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_document_print` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_document_profile`
--

DROP TABLE IF EXISTS `db_document_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_document_profile` (
  `profile_id` bigint NOT NULL,
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
  `address` varchar(256) DEFAULT NULL,
  `bank_account` varchar(64) DEFAULT NULL,
  `bank_name` varchar(64) DEFAULT NULL,
  `credit_code` varchar(32) DEFAULT NULL,
  `legal_name` varchar(128) DEFAULT NULL,
  `legal_representative` varchar(32) DEFAULT NULL,
  `logo` varchar(512) DEFAULT NULL,
  `service_hotline` varchar(32) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `uk_document_profile_company` (`com_id`),
  CONSTRAINT `FKnbmjs114ay38t8adsrslyyv2x` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_document_profile_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_document_profile_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_document_profile`
--

LOCK TABLES `db_document_profile` WRITE;
/*!40000 ALTER TABLE `db_document_profile` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_document_profile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_document_record`
--

DROP TABLE IF EXISTS `db_document_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_document_record` (
  `document_id` bigint NOT NULL,
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
  `audience` enum('Customer','Internal') NOT NULL,
  `business_no` varchar(64) NOT NULL,
  `content_type` varchar(64) NOT NULL,
  `file_name` varchar(256) NOT NULL,
  `generated_at` datetime(6) NOT NULL,
  `path` varchar(512) NOT NULL,
  `sha256` varchar(64) NOT NULL,
  `size` bigint NOT NULL,
  `status` enum('Active','Voided') NOT NULL,
  `template_version` int NOT NULL,
  `type` enum('Contract','Material','Payment','Quote','Receipt') NOT NULL,
  `com_id` bigint NOT NULL,
  `generated_by_id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  `quote_id` bigint DEFAULT NULL,
  `reprint_of_id` bigint DEFAULT NULL,
  PRIMARY KEY (`document_id`),
  KEY `idx_document_record_company` (`com_id`,`document_id`),
  KEY `idx_document_record_quote` (`com_id`,`quote_id`,`document_id`),
  KEY `idx_document_record_project` (`com_id`,`project_id`,`document_id`),
  KEY `FKd23olxk4cefvkollkmkug4vim` (`generated_by_id`),
  KEY `FK8841bgw16sh7wqe8fkrsdvof7` (`project_id`),
  KEY `FK9b5h14slcayhw14trf0xhij9a` (`quote_id`),
  KEY `FKjb232jctxw982fs2pnset5iqm` (`reprint_of_id`),
  CONSTRAINT `FK8841bgw16sh7wqe8fkrsdvof7` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FK9b5h14slcayhw14trf0xhij9a` FOREIGN KEY (`quote_id`) REFERENCES `quote` (`quote_id`),
  CONSTRAINT `FKd23olxk4cefvkollkmkug4vim` FOREIGN KEY (`generated_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKjb232jctxw982fs2pnset5iqm` FOREIGN KEY (`reprint_of_id`) REFERENCES `db_document_record` (`document_id`),
  CONSTRAINT `FKje127knxdx1yjvx3a63wfxd8y` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_document_record_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_document_record_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_document_record`
--

LOCK TABLES `db_document_record` WRITE;
/*!40000 ALTER TABLE `db_document_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_document_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_document_template`
--

DROP TABLE IF EXISTS `db_document_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_document_template` (
  `template_id` bigint NOT NULL,
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
  `baseline_version` int NOT NULL,
  `field_config` tinytext NOT NULL,
  `name` varchar(128) NOT NULL,
  `scope` enum('Company','Platform') NOT NULL,
  `status` enum('Active','Voided') NOT NULL,
  `style_config` tinytext NOT NULL,
  `type` enum('Contract','Material','Payment','Quote','Receipt') NOT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`template_id`),
  KEY `idx_document_template_company` (`com_id`,`template_id`),
  KEY `idx_document_template_company_type` (`com_id`,`type`,`version`),
  CONSTRAINT `FKjif5qdvxsk6fgm6l6c8qc70p7` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_document_template_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_document_template_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_document_template`
--

LOCK TABLES `db_document_template` WRITE;
/*!40000 ALTER TABLE `db_document_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_document_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_finance_ledger`
--

DROP TABLE IF EXISTS `db_finance_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_ledger` (
  `ledger_id` bigint NOT NULL,
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
  `account` enum('Alipay','Cash','CompanyBank','Other','Wechat') NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `category` varchar(32) NOT NULL,
  `counterparty` varchar(128) DEFAULT NULL,
  `direction` enum('Expense','Income') NOT NULL,
  `occurred_at` date NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `source` enum('Adjustment','Manual','Payment','Receipt') NOT NULL,
  `source_id` bigint DEFAULT NULL,
  `summary` varchar(256) NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`ledger_id`),
  KEY `idx_fin_ledger_company` (`com_id`,`ledger_id`),
  KEY `idx_fin_ledger_project` (`com_id`,`project_id`,`ledger_id`),
  KEY `idx_fin_ledger_store` (`com_id`,`store_id`,`ledger_id`),
  KEY `idx_fin_ledger_team` (`com_id`,`team_id`,`ledger_id`),
  KEY `idx_fin_ledger_owner` (`com_id`,`owner_id`,`ledger_id`),
  KEY `FK3hitoqenx4csywx24ehrqp2rs` (`creator_id`),
  KEY `FK7qc5ko3h4qxwa6ahlfoxa4lal` (`owner_id`),
  KEY `FKabveqxsl98rb0c652q8jwx3my` (`project_id`),
  KEY `FKba9167ripw83a5b5j7c22qy34` (`store_id`),
  KEY `FKnao31r3t8ct5xfjt1pr9g7wr9` (`team_id`),
  CONSTRAINT `FK3hitoqenx4csywx24ehrqp2rs` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK7qc5ko3h4qxwa6ahlfoxa4lal` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKa1bi95tuvd3pcuxq0s91uh13c` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKabveqxsl98rb0c652q8jwx3my` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKba9167ripw83a5b5j7c22qy34` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKnao31r3t8ct5xfjt1pr9g7wr9` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `db_finance_ledger_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_ledger_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_finance_ledger`
--

LOCK TABLES `db_finance_ledger` WRITE;
/*!40000 ALTER TABLE `db_finance_ledger` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_finance_ledger` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_finance_partner_credit`
--

DROP TABLE IF EXISTS `db_finance_partner_credit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_partner_credit` (
  `partner_credit_id` bigint NOT NULL,
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
  `amount` decimal(14,2) NOT NULL,
  `effective_date` date DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `source_no` varchar(64) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `partner_id` bigint NOT NULL,
  PRIMARY KEY (`partner_credit_id`),
  KEY `idx_partner_credit_company` (`com_id`,`partner_credit_id`),
  KEY `idx_partner_credit_company_partner` (`com_id`,`partner_id`,`partner_credit_id`),
  KEY `FKsq3w3f4ydno3nvl5m1seo28iu` (`creator_id`),
  KEY `FK9nukdc7g19e94ouqr2fv1dk2r` (`partner_id`),
  CONSTRAINT `FK9nukdc7g19e94ouqr2fv1dk2r` FOREIGN KEY (`partner_id`) REFERENCES `db_partner` (`partner_id`),
  CONSTRAINT `FKsixb07jq5nkp8dmbeelv99r5l` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKsq3w3f4ydno3nvl5m1seo28iu` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_finance_partner_credit_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_partner_credit_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_finance_partner_credit`
--

LOCK TABLES `db_finance_partner_credit` WRITE;
/*!40000 ALTER TABLE `db_finance_partner_credit` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_finance_partner_credit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_finance_payable`
--

DROP TABLE IF EXISTS `db_finance_payable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_payable` (
  `payable_id` bigint NOT NULL,
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
  `amount` decimal(14,2) NOT NULL,
  `due_date` date DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `paid_amount` decimal(14,2) NOT NULL,
  `payee_name` varchar(128) DEFAULT NULL,
  `period` varchar(64) DEFAULT NULL,
  `receipt_date` date DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `settlement_mode` enum('MonthlyCutoff','ReceiptDays') DEFAULT NULL,
  `source_no` varchar(64) DEFAULT NULL,
  `status` enum('Closed','Overdue','Paid','Partial','Pending') NOT NULL,
  `type` enum('Expense','Material','Other','Project','Salary','Supplier') NOT NULL,
  `urgent` bit(1) NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `material_plan_id` bigint DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  `payee_emp_id` bigint DEFAULT NULL,
  `payee_partner_id` bigint DEFAULT NULL,
  `project_id` bigint DEFAULT NULL,
  `property_id` bigint NOT NULL,
  `purchase_order_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`payable_id`),
  KEY `idx_fin_payable_company` (`com_id`,`payable_id`),
  KEY `idx_fin_payable_project` (`com_id`,`project_id`,`payable_id`),
  KEY `idx_fin_payable_store` (`com_id`,`store_id`,`payable_id`),
  KEY `idx_fin_payable_team` (`com_id`,`team_id`,`payable_id`),
  KEY `idx_fin_payable_owner` (`com_id`,`owner_id`,`payable_id`),
  KEY `FKbnmab6sqvjs91dy6tfkt7bpem` (`creator_id`),
  KEY `FKt3eat6h9tqwg6woci3m5g8634` (`material_plan_id`),
  KEY `FK9dlqw5a5p2y2xpao9pb8db13h` (`owner_id`),
  KEY `FKije8724jsc0vurfvu5gpvby7o` (`payee_emp_id`),
  KEY `FKib0f8q9vfnl4ra0tk3a1p40i4` (`payee_partner_id`),
  KEY `FKnebqmsl5yon65yghybvyy35tx` (`project_id`),
  KEY `FK8ppc29o0u1p3q6dse3hbo1ii7` (`property_id`),
  KEY `FKiyn2vjwpq5ataftvei38bmxn1` (`purchase_order_id`),
  KEY `FKl2o0ndus7hjtij2rg96eq2pl3` (`store_id`),
  KEY `FK9rc3k3w4fmrt3vetu5sap9ltf` (`team_id`),
  CONSTRAINT `FK8ppc29o0u1p3q6dse3hbo1ii7` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FK9dlqw5a5p2y2xpao9pb8db13h` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK9rc3k3w4fmrt3vetu5sap9ltf` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKbnmab6sqvjs91dy6tfkt7bpem` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKib0f8q9vfnl4ra0tk3a1p40i4` FOREIGN KEY (`payee_partner_id`) REFERENCES `db_partner` (`partner_id`),
  CONSTRAINT `FKije8724jsc0vurfvu5gpvby7o` FOREIGN KEY (`payee_emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKiyn2vjwpq5ataftvei38bmxn1` FOREIGN KEY (`purchase_order_id`) REFERENCES `db_purchase_order` (`purchase_id`),
  CONSTRAINT `FKl2o0ndus7hjtij2rg96eq2pl3` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKn0x40mgcbid665tw3sw3q78gn` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKnebqmsl5yon65yghybvyy35tx` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKt3eat6h9tqwg6woci3m5g8634` FOREIGN KEY (`material_plan_id`) REFERENCES `db_material_plan` (`plan_id`),
  CONSTRAINT `db_finance_payable_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_payable_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_finance_payable`
--

LOCK TABLES `db_finance_payable` WRITE;
/*!40000 ALTER TABLE `db_finance_payable` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_finance_payable` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_finance_payable_settlement`
--

DROP TABLE IF EXISTS `db_finance_payable_settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_payable_settlement` (
  `settlement_id` bigint NOT NULL,
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
  `amount` decimal(14,2) NOT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `payable_id` bigint NOT NULL,
  `payment_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`settlement_id`),
  KEY `idx_settle_company` (`com_id`,`settlement_id`),
  KEY `idx_settle_company_payment` (`com_id`,`payment_id`,`settlement_id`),
  KEY `idx_settle_company_payable` (`com_id`,`payable_id`,`settlement_id`),
  KEY `idx_settle_company_store` (`com_id`,`store_id`,`settlement_id`),
  KEY `idx_settle_company_team` (`com_id`,`team_id`,`settlement_id`),
  KEY `idx_settle_company_owner` (`com_id`,`owner_id`,`settlement_id`),
  KEY `FKp7d1jav8tdsvdqsd6qbfs5nse` (`owner_id`),
  KEY `FK2crp6g3lhuqsttpiu9r04t1w5` (`payable_id`),
  KEY `FKjqyi0fxe8athgljtljag0u363` (`payment_id`),
  KEY `FKigg2b25t399u06666i6imy89w` (`store_id`),
  KEY `FKmkb5dictvh4oeey3pohmq1ygm` (`team_id`),
  CONSTRAINT `FK2crp6g3lhuqsttpiu9r04t1w5` FOREIGN KEY (`payable_id`) REFERENCES `db_finance_payable` (`payable_id`),
  CONSTRAINT `FKigg2b25t399u06666i6imy89w` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKjqyi0fxe8athgljtljag0u363` FOREIGN KEY (`payment_id`) REFERENCES `db_finance_payment` (`payment_id`),
  CONSTRAINT `FKlni61lx1mfvbwbvcwfduuh8fg` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKmkb5dictvh4oeey3pohmq1ygm` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKp7d1jav8tdsvdqsd6qbfs5nse` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_finance_payable_settlement_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_payable_settlement_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_finance_payable_settlement`
--

LOCK TABLES `db_finance_payable_settlement` WRITE;
/*!40000 ALTER TABLE `db_finance_payable_settlement` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_finance_payable_settlement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_finance_payment`
--

DROP TABLE IF EXISTS `db_finance_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_payment` (
  `payment_id` bigint NOT NULL,
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
  `account` enum('Alipay','Cash','CompanyBank','Other','Wechat') NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `paid_at` date NOT NULL,
  `payee_name` varchar(128) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `transaction_no` varchar(128) DEFAULT NULL,
  `voucher_url` varchar(512) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `partner_id` bigint DEFAULT NULL,
  `payable_id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  `worker_id` bigint DEFAULT NULL,
  PRIMARY KEY (`payment_id`),
  KEY `idx_fin_payment_company` (`com_id`,`payment_id`),
  KEY `idx_fin_payment_project` (`com_id`,`project_id`,`payment_id`),
  KEY `idx_fin_payment_payable` (`com_id`,`payable_id`,`payment_id`),
  KEY `idx_fin_payment_store` (`com_id`,`store_id`,`payment_id`),
  KEY `idx_fin_payment_team` (`com_id`,`team_id`,`payment_id`),
  KEY `idx_fin_payment_owner` (`com_id`,`owner_id`,`payment_id`),
  KEY `FKmcdjgewoyuuq79m43v95b7hiu` (`creator_id`),
  KEY `FK5x3u42xid7yfx6myax3sb9jdq` (`owner_id`),
  KEY `FK483ytvwlmn5pskj27rmaqvll5` (`partner_id`),
  KEY `FK8a3inpj4t37laa8uu85vmu6b2` (`payable_id`),
  KEY `FKnvwrjmvdh6umjaweqq81falmj` (`project_id`),
  KEY `FKrgjw750a7nklkhsbywubb3f3x` (`store_id`),
  KEY `FKo5w7jpnmsiudplqy1dvn3m5xd` (`team_id`),
  KEY `FK406o2oruc5a6nfh381glkxdeh` (`worker_id`),
  CONSTRAINT `FK406o2oruc5a6nfh381glkxdeh` FOREIGN KEY (`worker_id`) REFERENCES `db_worker` (`worker_id`),
  CONSTRAINT `FK483ytvwlmn5pskj27rmaqvll5` FOREIGN KEY (`partner_id`) REFERENCES `db_partner` (`partner_id`),
  CONSTRAINT `FK5x3u42xid7yfx6myax3sb9jdq` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK8a3inpj4t37laa8uu85vmu6b2` FOREIGN KEY (`payable_id`) REFERENCES `db_finance_payable` (`payable_id`),
  CONSTRAINT `FKamkoey6ludb6egey1rmomke13` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKmcdjgewoyuuq79m43v95b7hiu` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKnvwrjmvdh6umjaweqq81falmj` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKo5w7jpnmsiudplqy1dvn3m5xd` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKrgjw750a7nklkhsbywubb3f3x` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_finance_payment_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_payment_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_finance_payment`
--

LOCK TABLES `db_finance_payment` WRITE;
/*!40000 ALTER TABLE `db_finance_payment` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_finance_payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_finance_receipt`
--

DROP TABLE IF EXISTS `db_finance_receipt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_receipt` (
  `receipt_id` bigint NOT NULL,
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
  `account` enum('Alipay','Cash','CompanyBank','Other','Wechat') NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `idempotency_key` varchar(128) DEFAULT NULL,
  `payer_name` varchar(64) DEFAULT NULL,
  `received_at` date NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `voucher_url` varchar(512) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `receivable_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`receipt_id`),
  UNIQUE KEY `uk_fin_receipt_company_idempotency` (`com_id`,`idempotency_key`),
  KEY `idx_fin_receipt_company` (`com_id`,`receipt_id`),
  KEY `idx_fin_receipt_project` (`com_id`,`project_id`,`receipt_id`),
  KEY `idx_fin_receipt_receivable` (`com_id`,`receivable_id`,`receipt_id`),
  KEY `idx_fin_receipt_store` (`com_id`,`store_id`,`receipt_id`),
  KEY `idx_fin_receipt_team` (`com_id`,`team_id`,`receipt_id`),
  KEY `idx_fin_receipt_owner` (`com_id`,`owner_id`,`receipt_id`),
  KEY `idx_fin_receipt_idempotency` (`com_id`,`idempotency_key`),
  KEY `FKkfvyyur9t6bpi8xcfnpnf8yvk` (`creator_id`),
  KEY `FKo0i9r0abe0wkfmhwhujqll73q` (`customer_id`),
  KEY `FK6iw6ijm55bw9yx7w86mgy2gk5` (`owner_id`),
  KEY `FKnnpog2mmr79hkhavo44taba8i` (`project_id`),
  KEY `FK36ame5945ny8ye9w657sa1ins` (`receivable_id`),
  KEY `FKt2huygd04sq41ffbvg2ugou86` (`store_id`),
  KEY `FKs994t8dgmwycmi4dbxqadcwr4` (`team_id`),
  CONSTRAINT `FK36ame5945ny8ye9w657sa1ins` FOREIGN KEY (`receivable_id`) REFERENCES `db_finance_receivable` (`receivable_id`),
  CONSTRAINT `FK6iw6ijm55bw9yx7w86mgy2gk5` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKht1wubd6c3i2egae6qb1v2rfs` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKkfvyyur9t6bpi8xcfnpnf8yvk` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKnnpog2mmr79hkhavo44taba8i` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKo0i9r0abe0wkfmhwhujqll73q` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKs994t8dgmwycmi4dbxqadcwr4` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKt2huygd04sq41ffbvg2ugou86` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_finance_receipt_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_receipt_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_finance_receipt`
--

LOCK TABLES `db_finance_receipt` WRITE;
/*!40000 ALTER TABLE `db_finance_receipt` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_finance_receipt` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_finance_receivable`
--

DROP TABLE IF EXISTS `db_finance_receivable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_receivable` (
  `receivable_id` bigint NOT NULL,
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
  `amount` decimal(14,2) NOT NULL,
  `due_date` date NOT NULL,
  `invoice_no` varchar(64) DEFAULT NULL,
  `invoice_required` bit(1) NOT NULL,
  `name` varchar(128) NOT NULL,
  `received_amount` decimal(14,2) NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `stage_code` varchar(64) DEFAULT NULL,
  `stage_name` varchar(128) DEFAULT NULL,
  `status` enum('Closed','Overdue','Partial','Pending','Received') NOT NULL,
  `trigger_type` enum('ContractSigned','ProjectFinished','StageAccepted','StageDone','StageStarted') DEFAULT NULL,
  `type` enum('Final','First','Other','Progress','Retention') NOT NULL,
  `com_id` bigint NOT NULL,
  `contract_id` bigint DEFAULT NULL,
  `creator_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `stage_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`receivable_id`),
  KEY `idx_fin_receivable_company` (`com_id`,`receivable_id`),
  KEY `idx_fin_receivable_project` (`com_id`,`project_id`,`receivable_id`),
  KEY `idx_fin_receivable_store` (`com_id`,`store_id`,`receivable_id`),
  KEY `idx_fin_receivable_team` (`com_id`,`team_id`,`receivable_id`),
  KEY `idx_fin_receivable_owner` (`com_id`,`owner_id`,`receivable_id`),
  KEY `FKhl4212gjofbiyi9v0hktarlti` (`contract_id`),
  KEY `FKkhcuese11rudf4d5tb81l5um9` (`creator_id`),
  KEY `FKs57vo0rpj1m9cobk12afe0xqq` (`customer_id`),
  KEY `FKom8obdfuevywx37wg7fnx8rlq` (`owner_id`),
  KEY `FK4wci0xku3s56c3o2qtd8b0ls` (`project_id`),
  KEY `FK5ae8gjn8thps7wdl03wf68524` (`property_id`),
  KEY `FKe012mh8k87suxm6gi50fq7ruq` (`stage_id`),
  KEY `FKepg1osjygbqm3i34qx6py8vjb` (`store_id`),
  KEY `FK8gpj1042lrpr5jac17pacia2` (`team_id`),
  CONSTRAINT `FK4wci0xku3s56c3o2qtd8b0ls` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FK5ae8gjn8thps7wdl03wf68524` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FK8gpj1042lrpr5jac17pacia2` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKbg89xwg737gny0eekuajfxxas` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKe012mh8k87suxm6gi50fq7ruq` FOREIGN KEY (`stage_id`) REFERENCES `db_project_stage` (`stage_id`),
  CONSTRAINT `FKepg1osjygbqm3i34qx6py8vjb` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKhl4212gjofbiyi9v0hktarlti` FOREIGN KEY (`contract_id`) REFERENCES `db_project_contract` (`contract_id`),
  CONSTRAINT `FKkhcuese11rudf4d5tb81l5um9` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKom8obdfuevywx37wg7fnx8rlq` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKs57vo0rpj1m9cobk12afe0xqq` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `db_finance_receivable_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_receivable_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_finance_receivable`
--

LOCK TABLES `db_finance_receivable` WRITE;
/*!40000 ALTER TABLE `db_finance_receivable` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_finance_receivable` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_finance_settlement`
--

DROP TABLE IF EXISTS `db_finance_settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_settlement` (
  `settlement_id` bigint NOT NULL,
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
  `change_amount` decimal(14,2) NOT NULL,
  `closed_at` date DEFAULT NULL,
  `confirmed_at` date DEFAULT NULL,
  `contract_amount` decimal(14,2) NOT NULL,
  `deduction_amount` decimal(14,2) NOT NULL,
  `expense_amount` decimal(14,2) NOT NULL,
  `final_amount` decimal(14,2) NOT NULL,
  `gross_margin` decimal(8,2) DEFAULT NULL,
  `material_amount` decimal(14,2) NOT NULL,
  `paid_amount` decimal(14,2) NOT NULL,
  `payable_amount` decimal(14,2) NOT NULL,
  `profit_amount` decimal(14,2) DEFAULT NULL,
  `project_amount` decimal(14,2) NOT NULL,
  `receivable_amount` decimal(14,2) NOT NULL,
  `received_amount` decimal(14,2) NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `retention_amount` decimal(14,2) NOT NULL,
  `retention_rate` decimal(6,2) NOT NULL,
  `retention_release_at` date DEFAULT NULL,
  `retention_released_at` date DEFAULT NULL,
  `reward_amount` decimal(14,2) NOT NULL,
  `salary_amount` decimal(14,2) NOT NULL,
  `status` enum('Closed','Confirmed','Draft','RetentionPending','RetentionReleased') NOT NULL,
  `total_cost` decimal(14,2) NOT NULL,
  `com_id` bigint NOT NULL,
  `confirm_emp_id` bigint DEFAULT NULL,
  `creator_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`settlement_id`),
  UNIQUE KEY `UKkjh9scdib4t82uitxguhplalf` (`project_id`),
  KEY `idx_fin_settlement_company` (`com_id`,`settlement_id`),
  KEY `idx_fin_settlement_project` (`com_id`,`project_id`,`settlement_id`),
  KEY `idx_fin_settlement_store` (`com_id`,`store_id`,`settlement_id`),
  KEY `idx_fin_settlement_team` (`com_id`,`team_id`,`settlement_id`),
  KEY `idx_fin_settlement_owner` (`com_id`,`owner_id`,`settlement_id`),
  KEY `FKjh70ubua8qnqy6skq0shk57j7` (`confirm_emp_id`),
  KEY `FKo8bfpufots5am5gigitsmqdmc` (`creator_id`),
  KEY `FK2iqvxqtu4dark9rv6uperg6va` (`owner_id`),
  KEY `FKk3uwnxnn4a02d7q09cvngla4d` (`store_id`),
  KEY `FK7iduf5v4apb1gnovfs28rj3j` (`team_id`),
  CONSTRAINT `FK2iqvxqtu4dark9rv6uperg6va` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK7iduf5v4apb1gnovfs28rj3j` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKjh70ubua8qnqy6skq0shk57j7` FOREIGN KEY (`confirm_emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKk3uwnxnn4a02d7q09cvngla4d` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKlk2g5std3up3fm90pind89xmm` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKo8bfpufots5am5gigitsmqdmc` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKt5mf9533cgb3kgi0qsmjrvwk8` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_finance_settlement_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_settlement_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_finance_settlement`
--

LOCK TABLES `db_finance_settlement` WRITE;
/*!40000 ALTER TABLE `db_finance_settlement` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_finance_settlement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_goods_receipt`
--

DROP TABLE IF EXISTS `db_goods_receipt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_goods_receipt` (
  `receipt_id` bigint NOT NULL,
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
  `arrived_at` datetime(6) DEFAULT NULL,
  `batch_no` varchar(64) DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') NOT NULL,
  `com_id` bigint NOT NULL,
  `inspector_id` bigint NOT NULL,
  `material_plan_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `purchase_id` bigint NOT NULL,
  `purchase_item_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  `warehouse_id` bigint DEFAULT NULL,
  PRIMARY KEY (`receipt_id`),
  KEY `idx_goods_receipt_company` (`com_id`,`receipt_id`),
  KEY `idx_goods_receipt_purchase` (`com_id`,`purchase_id`,`receipt_id`),
  KEY `idx_goods_receipt_company_store` (`com_id`,`store_id`,`receipt_id`),
  KEY `idx_goods_receipt_company_team` (`com_id`,`team_id`,`receipt_id`),
  KEY `idx_goods_receipt_company_owner` (`com_id`,`owner_id`,`receipt_id`),
  KEY `FKoil1hjp5vp7gv5i6f26gytpj2` (`inspector_id`),
  KEY `FK2c1g9tw9xsw78efbbigk7gfbk` (`material_plan_id`),
  KEY `FK4ulckq5t27lrlv8vbcuuecksn` (`owner_id`),
  KEY `FKc4c6xr5x3l57t85bg6njqln64` (`project_id`),
  KEY `FK79o0k3djeoq7vk86dmrm7l2d1` (`property_id`),
  KEY `FK8i5lwy9hbacx6cym1b2lq4m4f` (`purchase_id`),
  KEY `FKnamtwluc909n0o1y70w64nea4` (`purchase_item_id`),
  KEY `FKlan6s7ik54fg8w7oue4fwmcuw` (`store_id`),
  KEY `FKsgts2qay9xuijpcb5dp7ji0vf` (`team_id`),
  KEY `FKef4flo12rfm2b3rocwfgkvpo9` (`warehouse_id`),
  CONSTRAINT `FK2c1g9tw9xsw78efbbigk7gfbk` FOREIGN KEY (`material_plan_id`) REFERENCES `db_material_plan` (`plan_id`),
  CONSTRAINT `FK4ulckq5t27lrlv8vbcuuecksn` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK79o0k3djeoq7vk86dmrm7l2d1` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FK8i5lwy9hbacx6cym1b2lq4m4f` FOREIGN KEY (`purchase_id`) REFERENCES `db_purchase_order` (`purchase_id`),
  CONSTRAINT `FKc4c6xr5x3l57t85bg6njqln64` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKef4flo12rfm2b3rocwfgkvpo9` FOREIGN KEY (`warehouse_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKlan6s7ik54fg8w7oue4fwmcuw` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKnamtwluc909n0o1y70w64nea4` FOREIGN KEY (`purchase_item_id`) REFERENCES `db_purchase_order_item` (`purchase_item_id`),
  CONSTRAINT `FKoil1hjp5vp7gv5i6f26gytpj2` FOREIGN KEY (`inspector_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKpl16f48x3pyhwylxcakc8nd8o` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKsgts2qay9xuijpcb5dp7ji0vf` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `db_goods_receipt_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_goods_receipt_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_goods_receipt`
--

LOCK TABLES `db_goods_receipt` WRITE;
/*!40000 ALTER TABLE `db_goods_receipt` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_goods_receipt` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_goods_receipt_images`
--

DROP TABLE IF EXISTS `db_goods_receipt_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_goods_receipt_images` (
  `receipt_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FK95i330n58tjnfb5mygbg5b5vx` (`receipt_id`),
  CONSTRAINT `FK95i330n58tjnfb5mygbg5b5vx` FOREIGN KEY (`receipt_id`) REFERENCES `db_goods_receipt` (`receipt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_goods_receipt_images`
--

LOCK TABLES `db_goods_receipt_images` WRITE;
/*!40000 ALTER TABLE `db_goods_receipt_images` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_goods_receipt_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_inventory_stock`
--

DROP TABLE IF EXISTS `db_inventory_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_inventory_stock` (
  `stock_id` bigint NOT NULL,
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
  `batch_no` varchar(64) DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `last_in_at` datetime(6) DEFAULT NULL,
  `last_out_at` datetime(6) DEFAULT NULL,
  `locked_quantity` decimal(14,3) NOT NULL,
  `quantity` decimal(14,3) NOT NULL,
  `safety_stock` decimal(14,3) NOT NULL,
  `unit_cost` decimal(12,2) NOT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  PRIMARY KEY (`stock_id`),
  KEY `idx_stock_company` (`com_id`,`stock_id`),
  KEY `idx_stock_company_store` (`com_id`,`store_id`,`stock_id`),
  KEY `idx_stock_company_item` (`com_id`,`item_id`,`stock_id`),
  KEY `FKntkw9u4knjekt1rpyktklwptt` (`item_id`),
  KEY `FKeaf55ika82twmus61h5x3qcu0` (`store_id`),
  CONSTRAINT `FKeaf55ika82twmus61h5x3qcu0` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKhwp3jrnr86rry8ys7i6oysgb5` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKntkw9u4knjekt1rpyktklwptt` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `db_inventory_stock_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_inventory_stock_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_inventory_stock`
--

LOCK TABLES `db_inventory_stock` WRITE;
/*!40000 ALTER TABLE `db_inventory_stock` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_inventory_stock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_material_category`
--

DROP TABLE IF EXISTS `db_material_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_material_category` (
  `material_category_id` bigint NOT NULL,
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
  `active` bit(1) NOT NULL,
  `default_loss_rate` decimal(8,4) NOT NULL,
  `name` varchar(64) NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `sort` int NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  PRIMARY KEY (`material_category_id`),
  UNIQUE KEY `uk_material_category_company_name` (`com_id`,`name`),
  KEY `idx_material_category_company` (`com_id`,`material_category_id`),
  KEY `idx_material_category_company_name` (`com_id`,`name`),
  KEY `FKpo483j0rvddb0rcyutwuh1pps` (`creator_id`),
  CONSTRAINT `FKkgxeqim18vgawplojdxyc1uty` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKpo483j0rvddb0rcyutwuh1pps` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_material_category_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_material_category_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_material_category`
--

LOCK TABLES `db_material_category` WRITE;
/*!40000 ALTER TABLE `db_material_category` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_material_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_material_plan`
--

DROP TABLE IF EXISTS `db_material_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_material_plan` (
  `plan_id` bigint NOT NULL,
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
  `arrived_at` date DEFAULT NULL,
  `cost` decimal(12,2) NOT NULL,
  `loss_quantity` decimal(12,2) DEFAULT NULL,
  `loss_rate` decimal(8,4) DEFAULT NULL,
  `material_key` varchar(512) DEFAULT NULL,
  `net_quantity` decimal(12,2) DEFAULT NULL,
  `quantity` decimal(12,2) NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `required_at` date DEFAULT NULL,
  `selected_brand` varchar(128) DEFAULT NULL,
  `selected_model` varchar(128) DEFAULT NULL,
  `selected_spec` varchar(256) DEFAULT NULL,
  `source_type` enum('ChangeAdd','ChangeReduce','Selection') DEFAULT NULL,
  `status` enum('Approved','Arrived','Cancelled','Closed','Ordered','PartiallyArrived','PartiallyOrdered','Planned','ReadyToOrder','Requested') NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `catalog_item_id` bigint DEFAULT NULL,
  `change_item_id` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `contract_id` bigint DEFAULT NULL,
  `contract_line_id` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `package_line_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `quote_item_id` bigint DEFAULT NULL,
  `reduce_of_plan_id` bigint DEFAULT NULL,
  `selection_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `task_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`plan_id`),
  KEY `idx_material_plan_company` (`com_id`,`plan_id`),
  KEY `idx_material_plan_company_store` (`com_id`,`store_id`,`plan_id`),
  KEY `idx_material_plan_company_team` (`com_id`,`team_id`,`plan_id`),
  KEY `idx_material_plan_company_owner` (`com_id`,`owner_id`,`plan_id`),
  KEY `idx_material_plan_company_project` (`com_id`,`project_id`,`plan_id`),
  KEY `idx_material_plan_company_material_key` (`com_id`,`material_key`,`plan_id`),
  KEY `FKc5ml9xjsyow4tecmrcn724bkb` (`catalog_item_id`),
  KEY `FK64kq6djtfx9my76mloceymp51` (`change_item_id`),
  KEY `FK4p6mnfqfe7mmygavq4josdofi` (`contract_id`),
  KEY `FK2ueii2mgox92dqb01uq95xjcg` (`contract_line_id`),
  KEY `FKsck33wmpnqwv9es2xbgofi25l` (`owner_id`),
  KEY `FKirfm9vo4w5bo138xtojkcbvc4` (`package_line_id`),
  KEY `FKotowsi06jeblg3pin1m3m88b8` (`project_id`),
  KEY `FKia89ttsckjwucdv570cshvm3q` (`property_id`),
  KEY `FK3tuqxqlcsh0gl697vvhelcfja` (`quote_item_id`),
  KEY `FKj3p3kxvd3w9vit7bopyg1u7ln` (`reduce_of_plan_id`),
  KEY `FKgd162ntnh9oax7t3wx7s8i5tn` (`selection_id`),
  KEY `FK455bd5r7m2e681ldp1yw7apqf` (`store_id`),
  KEY `FKotn86lcty9imcpq4tmt54hmts` (`task_id`),
  KEY `FK8g7ocohfxukh38a9fh78iwqmx` (`team_id`),
  CONSTRAINT `FK2ueii2mgox92dqb01uq95xjcg` FOREIGN KEY (`contract_line_id`) REFERENCES `db_contract_line` (`line_id`),
  CONSTRAINT `FK3tuqxqlcsh0gl697vvhelcfja` FOREIGN KEY (`quote_item_id`) REFERENCES `quote_line` (`item_id`),
  CONSTRAINT `FK455bd5r7m2e681ldp1yw7apqf` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FK4p6mnfqfe7mmygavq4josdofi` FOREIGN KEY (`contract_id`) REFERENCES `db_project_contract` (`contract_id`),
  CONSTRAINT `FK64kq6djtfx9my76mloceymp51` FOREIGN KEY (`change_item_id`) REFERENCES `db_contract_change_item` (`change_item_id`),
  CONSTRAINT `FK8g7ocohfxukh38a9fh78iwqmx` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKc5ml9xjsyow4tecmrcn724bkb` FOREIGN KEY (`catalog_item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FKgd162ntnh9oax7t3wx7s8i5tn` FOREIGN KEY (`selection_id`) REFERENCES `db_material_selection` (`selection_id`),
  CONSTRAINT `FKia89ttsckjwucdv570cshvm3q` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKirfm9vo4w5bo138xtojkcbvc4` FOREIGN KEY (`package_line_id`) REFERENCES `pack_line` (`item_id`),
  CONSTRAINT `FKj3p3kxvd3w9vit7bopyg1u7ln` FOREIGN KEY (`reduce_of_plan_id`) REFERENCES `db_material_plan` (`plan_id`),
  CONSTRAINT `FKotn86lcty9imcpq4tmt54hmts` FOREIGN KEY (`task_id`) REFERENCES `db_project_task` (`task_id`),
  CONSTRAINT `FKotowsi06jeblg3pin1m3m88b8` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKsck33wmpnqwv9es2xbgofi25l` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKsneqyhc2xelwipuc26jjec69c` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_material_plan_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_material_plan_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_material_plan`
--

LOCK TABLES `db_material_plan` WRITE;
/*!40000 ALTER TABLE `db_material_plan` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_material_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_material_return`
--

DROP TABLE IF EXISTS `db_material_return`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_material_return` (
  `material_return_id` bigint NOT NULL,
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
  `amount` decimal(14,2) NOT NULL,
  `no` varchar(64) DEFAULT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `returned_at` date DEFAULT NULL,
  `status` enum('Cancelled','Confirmed','Pending') NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `material_plan_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `purchase_order_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `supplier_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`material_return_id`),
  KEY `idx_material_return_company` (`com_id`,`material_return_id`),
  KEY `idx_material_return_company_project` (`com_id`,`project_id`,`material_return_id`),
  KEY `idx_material_return_company_plan` (`com_id`,`material_plan_id`,`material_return_id`),
  KEY `idx_material_return_company_purchase` (`com_id`,`purchase_order_id`,`material_return_id`),
  KEY `idx_material_return_company_store` (`com_id`,`store_id`,`material_return_id`),
  KEY `idx_material_return_company_team` (`com_id`,`team_id`,`material_return_id`),
  KEY `idx_material_return_company_owner` (`com_id`,`owner_id`,`material_return_id`),
  KEY `FK9sewhiw3jg6e2sg1scjmuyx7u` (`creator_id`),
  KEY `FKpjkontea0dsq1a6w2pb1kjlve` (`material_plan_id`),
  KEY `FK8p0wxrjcsx1avo2h196rxrgoe` (`owner_id`),
  KEY `FKlktutxr08meureo9irsdhoohd` (`project_id`),
  KEY `FKswifx8ne3mcjwcox621tq5bn9` (`property_id`),
  KEY `FKpxq58t6t1vx65m8483jo6xysy` (`purchase_order_id`),
  KEY `FKeorge31me32xc9fneusfj8a7e` (`store_id`),
  KEY `FKl2mc77el46dyt1r8kh9ec28px` (`supplier_id`),
  KEY `FKka5he6ctfu0cb68oofatf0wg9` (`team_id`),
  CONSTRAINT `FK8p0wxrjcsx1avo2h196rxrgoe` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK9sewhiw3jg6e2sg1scjmuyx7u` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKdy87qqo5uii79y8lud8he0m76` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKeorge31me32xc9fneusfj8a7e` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKka5he6ctfu0cb68oofatf0wg9` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKl2mc77el46dyt1r8kh9ec28px` FOREIGN KEY (`supplier_id`) REFERENCES `db_partner` (`partner_id`),
  CONSTRAINT `FKlktutxr08meureo9irsdhoohd` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKpjkontea0dsq1a6w2pb1kjlve` FOREIGN KEY (`material_plan_id`) REFERENCES `db_material_plan` (`plan_id`),
  CONSTRAINT `FKpxq58t6t1vx65m8483jo6xysy` FOREIGN KEY (`purchase_order_id`) REFERENCES `db_purchase_order` (`purchase_id`),
  CONSTRAINT `FKswifx8ne3mcjwcox621tq5bn9` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `db_material_return_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_material_return_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_material_return`
--

LOCK TABLES `db_material_return` WRITE;
/*!40000 ALTER TABLE `db_material_return` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_material_return` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_material_selection`
--

DROP TABLE IF EXISTS `db_material_selection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_material_selection` (
  `selection_id` bigint NOT NULL,
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
  `confirmed_at` datetime(6) DEFAULT NULL,
  `confirmer_name` varchar(64) DEFAULT NULL,
  `content` tinytext NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `selection_json` longtext,
  `status` enum('Confirmed','Draft','PendingConfirm') NOT NULL,
  `title` varchar(128) DEFAULT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  `confirmed_by_id` bigint DEFAULT NULL,
  `contract_id` bigint DEFAULT NULL,
  `customer_id` bigint NOT NULL,
  `designer_id` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint DEFAULT NULL,
  `property_id` bigint NOT NULL,
  `source_contract_line_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`selection_id`),
  UNIQUE KEY `uk_material_selection_item_version` (`com_id`,`source_contract_line_id`,`version`),
  KEY `idx_material_selection_company` (`com_id`,`selection_id`),
  KEY `idx_material_selection_company_property` (`com_id`,`property_id`,`selection_id`),
  KEY `idx_material_selection_company_contract` (`com_id`,`contract_id`,`selection_id`),
  KEY `idx_material_selection_company_project` (`com_id`,`project_id`,`selection_id`),
  KEY `idx_material_selection_company_item` (`com_id`,`source_contract_line_id`,`selection_id`),
  KEY `idx_material_selection_company_store` (`com_id`,`store_id`,`selection_id`),
  KEY `idx_material_selection_company_team` (`com_id`,`team_id`,`selection_id`),
  KEY `idx_material_selection_company_owner` (`com_id`,`owner_id`,`selection_id`),
  KEY `FK2iuiqcdgc38gi245kk0hfa0nl` (`confirmed_by_id`),
  KEY `FK4hksukslnvay59c7ucwtfah3w` (`contract_id`),
  KEY `FK6ehuq8p32wpo57ddbh7uw4cf8` (`customer_id`),
  KEY `FK1le3wqmbwbmurlbfptht7ekd8` (`designer_id`),
  KEY `FKruacqndiw2559wx1aam4ctsxq` (`owner_id`),
  KEY `FK9vncvjkf7eiso3lp4ysvc0dlg` (`project_id`),
  KEY `FKknw8mcd8fxt8agdvwbk00swr6` (`property_id`),
  KEY `FKsaqwwln54stykc6h5tvgc1uii` (`source_contract_line_id`),
  KEY `FK3du77nqh0ko8y15sblwh5yxan` (`store_id`),
  KEY `FKk2d6os6knpvvaa5r0evn30e3n` (`team_id`),
  CONSTRAINT `FK1le3wqmbwbmurlbfptht7ekd8` FOREIGN KEY (`designer_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK2iuiqcdgc38gi245kk0hfa0nl` FOREIGN KEY (`confirmed_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK3du77nqh0ko8y15sblwh5yxan` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FK4hksukslnvay59c7ucwtfah3w` FOREIGN KEY (`contract_id`) REFERENCES `db_project_contract` (`contract_id`),
  CONSTRAINT `FK6ehuq8p32wpo57ddbh7uw4cf8` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FK9vncvjkf7eiso3lp4ysvc0dlg` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKdx6bun40374k3pvpk4ncvkp0p` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKk2d6os6knpvvaa5r0evn30e3n` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKknw8mcd8fxt8agdvwbk00swr6` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKruacqndiw2559wx1aam4ctsxq` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKsaqwwln54stykc6h5tvgc1uii` FOREIGN KEY (`source_contract_line_id`) REFERENCES `db_contract_line` (`line_id`),
  CONSTRAINT `db_material_selection_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_material_selection_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_material_selection`
--

LOCK TABLES `db_material_selection` WRITE;
/*!40000 ALTER TABLE `db_material_selection` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_material_selection` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_material_selection_attachments`
--

DROP TABLE IF EXISTS `db_material_selection_attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_material_selection_attachments` (
  `selection_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKjokjkuvb9q6w4ke36yn2eneee` (`selection_id`),
  CONSTRAINT `FKjokjkuvb9q6w4ke36yn2eneee` FOREIGN KEY (`selection_id`) REFERENCES `db_material_selection` (`selection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_material_selection_attachments`
--

LOCK TABLES `db_material_selection_attachments` WRITE;
/*!40000 ALTER TABLE `db_material_selection_attachments` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_material_selection_attachments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_material_supplier`
--

DROP TABLE IF EXISTS `db_material_supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_material_supplier` (
  `material_supplier_id` bigint NOT NULL,
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
  `active` bit(1) NOT NULL,
  `brand` varchar(128) DEFAULT NULL,
  `effective_from` date DEFAULT NULL,
  `effective_to` date DEFAULT NULL,
  `lead_time_days` int NOT NULL,
  `material_key` varchar(512) NOT NULL,
  `material_name` varchar(128) DEFAULT NULL,
  `minimum_order` decimal(12,3) NOT NULL,
  `model` varchar(128) DEFAULT NULL,
  `purchase_price` decimal(12,2) NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `settlement_days` int DEFAULT NULL,
  `spec` varchar(256) DEFAULT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') DEFAULT NULL,
  `catalog_item_id` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `partner_id` bigint NOT NULL,
  PRIMARY KEY (`material_supplier_id`),
  KEY `idx_material_supplier_company` (`com_id`,`material_supplier_id`),
  KEY `idx_material_supplier_company_key` (`com_id`,`material_key`,`material_supplier_id`),
  KEY `idx_material_supplier_company_partner` (`com_id`,`partner_id`,`material_supplier_id`),
  KEY `FKeoajth4mgats7gujrlq4gjdjo` (`catalog_item_id`),
  KEY `FKq245mtm3e89u2pgan4kk1ke1x` (`creator_id`),
  KEY `FK8gfpiivpc2xehsn7dp3uemcqc` (`partner_id`),
  CONSTRAINT `FK8gfpiivpc2xehsn7dp3uemcqc` FOREIGN KEY (`partner_id`) REFERENCES `db_partner` (`partner_id`),
  CONSTRAINT `FKclcrfbjxj4euk90q7qqm3xxms` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKeoajth4mgats7gujrlq4gjdjo` FOREIGN KEY (`catalog_item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FKq245mtm3e89u2pgan4kk1ke1x` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_material_supplier_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_material_supplier_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_material_supplier`
--

LOCK TABLES `db_material_supplier` WRITE;
/*!40000 ALTER TABLE `db_material_supplier` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_material_supplier` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_measure`
--

DROP TABLE IF EXISTS `db_measure`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_measure` (
  `measure_id` bigint NOT NULL,
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
  `completed_at` datetime(6) DEFAULT NULL,
  `customer_requirements` tinytext,
  `designer_conclusion` tinytext,
  `dimensions` tinytext,
  `measure_time` datetime(6) DEFAULT NULL,
  `original_structure` tinytext,
  `scheduled_at` datetime(6) DEFAULT NULL,
  `status` enum('Cancelled','Completed','Scheduled') NOT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `designer_id` bigint DEFAULT NULL,
  `executor_id` bigint DEFAULT NULL,
  `emp_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`measure_id`),
  KEY `idx_measure_company` (`com_id`,`measure_id`),
  KEY `idx_measure_company_property` (`com_id`,`property_id`,`measure_id`),
  KEY `idx_measure_company_customer` (`com_id`,`customer_id`,`measure_id`),
  KEY `idx_measure_company_designer` (`com_id`,`designer_id`,`measure_id`),
  KEY `idx_measure_company_executor` (`com_id`,`executor_id`,`measure_id`),
  KEY `FKivuym875ecjm4x3rptm92o7qc` (`customer_id`),
  KEY `FK97x7jhitsl5ibnjyopr8ccw05` (`designer_id`),
  KEY `FKn6kht6xrlvin4y9t2bfc7jqyj` (`executor_id`),
  KEY `FKcls4w7o2gb8p1wu644qn7p1pd` (`emp_id`),
  KEY `FKb2evwhp02x1cs87hc1tsw6qe4` (`property_id`),
  KEY `FK1qpy63fit8niuvjf85uvgr383` (`store_id`),
  KEY `FKfixet81wm0kvxjv8geeuhfep8` (`team_id`),
  CONSTRAINT `FK1qpy63fit8niuvjf85uvgr383` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FK97x7jhitsl5ibnjyopr8ccw05` FOREIGN KEY (`designer_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKb2evwhp02x1cs87hc1tsw6qe4` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKcls4w7o2gb8p1wu644qn7p1pd` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKfixet81wm0kvxjv8geeuhfep8` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKivuym875ecjm4x3rptm92o7qc` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKn6kht6xrlvin4y9t2bfc7jqyj` FOREIGN KEY (`executor_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKnhevfjus6evwu2t318afvuetm` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_measure_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_measure_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_measure`
--

LOCK TABLES `db_measure` WRITE;
/*!40000 ALTER TABLE `db_measure` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_measure` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_measure_attachments`
--

DROP TABLE IF EXISTS `db_measure_attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_measure_attachments` (
  `measure_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKa6ivugx7c16r8pmfou17kp6ky` (`measure_id`),
  CONSTRAINT `FKa6ivugx7c16r8pmfou17kp6ky` FOREIGN KEY (`measure_id`) REFERENCES `db_measure` (`measure_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_measure_attachments`
--

LOCK TABLES `db_measure_attachments` WRITE;
/*!40000 ALTER TABLE `db_measure_attachments` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_measure_attachments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_measure_site_photos`
--

DROP TABLE IF EXISTS `db_measure_site_photos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_measure_site_photos` (
  `measure_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKf6ok2r7lupb8wvvfrp98b4vpi` (`measure_id`),
  CONSTRAINT `FKf6ok2r7lupb8wvvfrp98b4vpi` FOREIGN KEY (`measure_id`) REFERENCES `db_measure` (`measure_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_measure_site_photos`
--

LOCK TABLES `db_measure_site_photos` WRITE;
/*!40000 ALTER TABLE `db_measure_site_photos` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_measure_site_photos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_measure_structure_images`
--

DROP TABLE IF EXISTS `db_measure_structure_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_measure_structure_images` (
  `measure_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKp73xpy8vw0iha4w8q5xrbhyyu` (`measure_id`),
  CONSTRAINT `FKp73xpy8vw0iha4w8q5xrbhyyu` FOREIGN KEY (`measure_id`) REFERENCES `db_measure` (`measure_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_measure_structure_images`
--

LOCK TABLES `db_measure_structure_images` WRITE;
/*!40000 ALTER TABLE `db_measure_structure_images` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_measure_structure_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_notice`
--

DROP TABLE IF EXISTS `db_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_notice` (
  `notice_id` bigint NOT NULL,
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
  `biz_key` varchar(96) NOT NULL,
  `content` varchar(512) NOT NULL,
  `read_flag` bit(1) NOT NULL,
  `title` varchar(128) NOT NULL,
  `type` enum('Customer','Overdue','Project','Quote','System','Task') NOT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint DEFAULT NULL,
  `project_id` bigint DEFAULT NULL,
  `quote_id` bigint DEFAULT NULL,
  `receiver_id` bigint NOT NULL,
  PRIMARY KEY (`notice_id`),
  UNIQUE KEY `idx_notice_receiver_biz` (`receiver_id`,`biz_key`),
  KEY `idx_notice_receiver_read` (`receiver_id`,`read_flag`),
  KEY `FKyn4k5lcwksgg3umb3qsy5ptd` (`com_id`),
  KEY `FKopbowtkpmhcrbpad2kfq1h3d6` (`customer_id`),
  KEY `FKkgmq1p72vjq2k04qqmsh9ubon` (`project_id`),
  KEY `FKk3ks3yxnrtveod4i755w44v49` (`quote_id`),
  CONSTRAINT `FK7713ob9o0wov1ue0gfrmtr5bw` FOREIGN KEY (`receiver_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKk3ks3yxnrtveod4i755w44v49` FOREIGN KEY (`quote_id`) REFERENCES `quote` (`quote_id`),
  CONSTRAINT `FKkgmq1p72vjq2k04qqmsh9ubon` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKopbowtkpmhcrbpad2kfq1h3d6` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKyn4k5lcwksgg3umb3qsy5ptd` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_notice_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_notice_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_notice`
--

LOCK TABLES `db_notice` WRITE;
/*!40000 ALTER TABLE `db_notice` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_partner`
--

DROP TABLE IF EXISTS `db_partner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_partner` (
  `partner_id` bigint NOT NULL,
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
  `addr` varchar(256) DEFAULT NULL,
  `bank_account` varchar(64) DEFAULT NULL,
  `bank_name` varchar(64) DEFAULT NULL,
  `contact_name` varchar(32) DEFAULT NULL,
  `cutoff_day` int DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `settlement_days` int DEFAULT NULL,
  `settlement_mode` enum('MonthlyCutoff','ReceiptDays') DEFAULT NULL,
  `settlement_terms` varchar(64) DEFAULT NULL,
  `status` enum('Active','Disabled') NOT NULL,
  `tax_no` varchar(64) DEFAULT NULL,
  `tel_no` varchar(20) DEFAULT NULL,
  `type` enum('Factory','Installer','LaborTeam','Logistics','Service','Supplier') NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  PRIMARY KEY (`partner_id`),
  KEY `idx_partner_company` (`com_id`,`partner_id`),
  KEY `idx_partner_company_type` (`com_id`,`type`,`partner_id`),
  KEY `FKt0s4dyheiq5eo08nfhawr5i3u` (`creator_id`),
  CONSTRAINT `FKcl2j7c438bdwxeiyqt8bnce6y` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKt0s4dyheiq5eo08nfhawr5i3u` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_partner_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_partner_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_partner`
--

LOCK TABLES `db_partner` WRITE;
/*!40000 ALTER TABLE `db_partner` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_partner` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_partner_brand`
--

DROP TABLE IF EXISTS `db_partner_brand`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_partner_brand` (
  `partner_id` bigint NOT NULL,
  `brand` varchar(128) NOT NULL,
  PRIMARY KEY (`partner_id`,`brand`),
  CONSTRAINT `FK3v4dahewhy5awflxcsm29ymgu` FOREIGN KEY (`partner_id`) REFERENCES `db_partner` (`partner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_partner_brand`
--

LOCK TABLES `db_partner_brand` WRITE;
/*!40000 ALTER TABLE `db_partner_brand` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_partner_brand` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_partner_price`
--

DROP TABLE IF EXISTS `db_partner_price`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_partner_price` (
  `partner_price_id` bigint NOT NULL,
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
  `active` bit(1) NOT NULL,
  `effective_from` date DEFAULT NULL,
  `effective_to` date DEFAULT NULL,
  `labor_price` decimal(12,2) NOT NULL,
  `lead_time_days` int NOT NULL,
  `minimum_order` decimal(12,3) NOT NULL,
  `purchase_price` decimal(12,2) NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `settlement_days` int NOT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `partner_id` bigint NOT NULL,
  PRIMARY KEY (`partner_price_id`),
  KEY `idx_partner_price_company` (`com_id`,`partner_price_id`),
  KEY `idx_partner_price_partner` (`com_id`,`partner_id`,`partner_price_id`),
  KEY `idx_partner_price_item` (`com_id`,`item_id`,`partner_price_id`),
  KEY `FKc2u4hkvyginf5tlwadhvy4hre` (`item_id`),
  KEY `FKi2httgr5l0o1fwbmj64msjo8e` (`partner_id`),
  CONSTRAINT `FKc2u4hkvyginf5tlwadhvy4hre` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FKi2httgr5l0o1fwbmj64msjo8e` FOREIGN KEY (`partner_id`) REFERENCES `db_partner` (`partner_id`),
  CONSTRAINT `FKjdnx5qijy3rdmgj0av84r5bi` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_partner_price_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_partner_price_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_partner_price`
--

LOCK TABLES `db_partner_price` WRITE;
/*!40000 ALTER TABLE `db_partner_price` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_partner_price` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_platform_department`
--

DROP TABLE IF EXISTS `db_platform_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_platform_department` (
  `dept_id` bigint NOT NULL,
  `code` varchar(32) NOT NULL,
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
  `name` varchar(64) NOT NULL,
  `sort` int NOT NULL,
  `status` enum('Active','Suspended') NOT NULL,
  `org_id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  PRIMARY KEY (`dept_id`),
  UNIQUE KEY `uk_platform_dept_org_code` (`org_id`,`code`),
  KEY `idx_platform_dept_org` (`org_id`,`dept_id`),
  KEY `idx_platform_dept_parent` (`org_id`,`parent_id`,`dept_id`),
  KEY `FKa6om74i3rqbkd3s2iq00d5g61` (`parent_id`),
  CONSTRAINT `FK625rpc7q3qvryor57xlxk649k` FOREIGN KEY (`org_id`) REFERENCES `db_platform_org` (`org_id`),
  CONSTRAINT `FKa6om74i3rqbkd3s2iq00d5g61` FOREIGN KEY (`parent_id`) REFERENCES `db_platform_department` (`dept_id`),
  CONSTRAINT `db_platform_department_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_platform_department`
--

LOCK TABLES `db_platform_department` WRITE;
/*!40000 ALTER TABLE `db_platform_department` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_platform_department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_platform_operation_log`
--

DROP TABLE IF EXISTS `db_platform_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_platform_operation_log` (
  `log_id` bigint NOT NULL,
  `action` enum('Freeze','PlanChange','Renew','SeatAdjust','StatusChange','StoreAdjust','Unfreeze') NOT NULL,
  `after_value` varchar(2048) DEFAULT NULL,
  `before_value` varchar(2048) DEFAULT NULL,
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
  `reason` varchar(500) NOT NULL,
  `summary` varchar(256) NOT NULL,
  `target_com_id` bigint DEFAULT NULL,
  `target_company_title` varchar(128) DEFAULT NULL,
  `target_key` varchar(64) NOT NULL,
  `target_sub_id` bigint DEFAULT NULL,
  `target_type` varchar(32) NOT NULL,
  `user_id` bigint NOT NULL,
  `org_id` bigint NOT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_platform_log_org_time` (`org_id`,`create_date_time`,`log_id`),
  KEY `idx_platform_log_operator_time` (`user_id`,`create_date_time`,`log_id`),
  KEY `idx_platform_log_target_company` (`target_com_id`,`log_id`),
  CONSTRAINT `FK507arvwcnic39e7iahn073ei7` FOREIGN KEY (`user_id`) REFERENCES `db_platform_user` (`user_id`),
  CONSTRAINT `FKjdw3mjsflb1w7gv340km34b5n` FOREIGN KEY (`org_id`) REFERENCES `db_platform_org` (`org_id`),
  CONSTRAINT `db_platform_operation_log_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_platform_operation_log`
--

LOCK TABLES `db_platform_operation_log` WRITE;
/*!40000 ALTER TABLE `db_platform_operation_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_platform_operation_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_platform_org`
--

DROP TABLE IF EXISTS `db_platform_org`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_platform_org` (
  `org_id` bigint NOT NULL,
  `code` varchar(32) NOT NULL,
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
  `name` varchar(128) NOT NULL,
  `status` enum('Active','Suspended') NOT NULL,
  PRIMARY KEY (`org_id`),
  UNIQUE KEY `uk_platform_org_code` (`code`),
  CONSTRAINT `db_platform_org_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_platform_org`
--

LOCK TABLES `db_platform_org` WRITE;
/*!40000 ALTER TABLE `db_platform_org` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_platform_org` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_platform_user`
--

DROP TABLE IF EXISTS `db_platform_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_platform_user` (
  `user_id` bigint NOT NULL,
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
  `last_login_at` datetime(6) DEFAULT NULL,
  `name` varchar(32) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `role` enum('Auditor','BillingOperator','CustomerSuccess','GrowthOperator','OperationsManager','SreOperator','SuperAdmin') NOT NULL,
  `status` enum('Active','Locked','Suspended') NOT NULL,
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
  `dept_id` bigint DEFAULT NULL,
  `org_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_platform_user_phone` (`phone`),
  KEY `idx_platform_user_org` (`org_id`,`user_id`),
  KEY `idx_platform_user_department` (`org_id`,`dept_id`,`user_id`),
  KEY `idx_platform_user_phone` (`phone`,`status`),
  KEY `FKqhbqgebie7wjramccofri7aee` (`dept_id`),
  CONSTRAINT `FKevth82a8csu3piypy6m4llvc7` FOREIGN KEY (`org_id`) REFERENCES `db_platform_org` (`org_id`),
  CONSTRAINT `FKqhbqgebie7wjramccofri7aee` FOREIGN KEY (`dept_id`) REFERENCES `db_platform_department` (`dept_id`),
  CONSTRAINT `db_platform_user_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_platform_user_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_platform_user`
--

LOCK TABLES `db_platform_user` WRITE;
/*!40000 ALTER TABLE `db_platform_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_platform_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_project`
--

DROP TABLE IF EXISTS `db_project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_project` (
  `project_id` bigint NOT NULL,
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
  `actual_end_date` date DEFAULT NULL,
  `area` decimal(10,2) NOT NULL,
  `contract_amount` decimal(12,2) NOT NULL,
  `cost` decimal(12,2) NOT NULL,
  `name` varchar(128) NOT NULL,
  `no` varchar(64) NOT NULL,
  `plan_end_date` date DEFAULT NULL,
  `progress` int NOT NULL,
  `site_addr` varchar(256) NOT NULL,
  `start_date` date DEFAULT NULL,
  `status` enum('Accepting','Finished','Ongoing','Preparing','Settled','Suspended') NOT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `designer_id` bigint DEFAULT NULL,
  `manager_id` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `property_id` bigint NOT NULL,
  `quote_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `support_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`project_id`),
  UNIQUE KEY `uk_project_company_property` (`com_id`,`property_id`),
  UNIQUE KEY `UKqapajuk296t3xjh07qepyp2qi` (`no`),
  UNIQUE KEY `UKa6bb0cxupyh2ac83g5mydnyu0` (`quote_id`),
  KEY `idx_project_company_support` (`com_id`,`support_id`,`project_id`),
  KEY `idx_project_company` (`com_id`,`project_id`),
  KEY `idx_project_company_store` (`com_id`,`store_id`,`project_id`),
  KEY `idx_project_company_team` (`com_id`,`team_id`,`project_id`),
  KEY `idx_project_company_owner` (`com_id`,`owner_id`,`project_id`),
  KEY `FK5i1newcgdhk0ixaxwuywmrmgc` (`customer_id`),
  KEY `FKf1772i6lyoer3qoxo62tqrjsf` (`designer_id`),
  KEY `FKcxy8x69hfio7f2kgqx9tjl6wc` (`manager_id`),
  KEY `FK7ebvlnjp0y5aawg5fo5qmmgxa` (`owner_id`),
  KEY `FK7p996pfp5vtiv43q2e290lfr6` (`property_id`),
  KEY `FK4tmj9c4903fgkmayi5oqo7uh7` (`store_id`),
  KEY `FKlxmbydm28u2ircf2875nyhuxr` (`support_id`),
  KEY `FKnjuxrwusr231hu7pyv5uu2eua` (`team_id`),
  CONSTRAINT `FK301nmgaxbkb3nk3el7wvd6wf6` FOREIGN KEY (`quote_id`) REFERENCES `quote` (`quote_id`),
  CONSTRAINT `FK4tmj9c4903fgkmayi5oqo7uh7` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FK5i1newcgdhk0ixaxwuywmrmgc` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FK7ebvlnjp0y5aawg5fo5qmmgxa` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK7p996pfp5vtiv43q2e290lfr6` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKcxy8x69hfio7f2kgqx9tjl6wc` FOREIGN KEY (`manager_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKf1772i6lyoer3qoxo62tqrjsf` FOREIGN KEY (`designer_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKlxmbydm28u2ircf2875nyhuxr` FOREIGN KEY (`support_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKnjuxrwusr231hu7pyv5uu2eua` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKofo45maboyyymcpfjgw6aioow` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_project_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_project_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_project`
--

LOCK TABLES `db_project` WRITE;
/*!40000 ALTER TABLE `db_project` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_project_contract`
--

DROP TABLE IF EXISTS `db_project_contract`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_project_contract` (
  `contract_id` bigint NOT NULL,
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
  `baseline_amount` decimal(14,2) NOT NULL,
  `current_amount` decimal(14,2) NOT NULL,
  `effective_at` datetime(6) DEFAULT NULL,
  `no` varchar(64) NOT NULL,
  `payment_terms` varchar(128) DEFAULT NULL,
  `plan_end_date` date DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `signed_amount` decimal(14,2) DEFAULT NULL,
  `signed_at` date DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `status` enum('Active','Changed','Changing','Closed','Completed','Draft','Effective','PendingSign','Settled','Signed','Terminated') NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint DEFAULT NULL,
  `property_id` bigint NOT NULL,
  `quote_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`contract_id`),
  UNIQUE KEY `uk_contract_quote` (`quote_id`),
  UNIQUE KEY `uk_contract_property` (`property_id`),
  UNIQUE KEY `uk_contract_project` (`project_id`),
  KEY `idx_contract_company` (`com_id`,`contract_id`),
  KEY `idx_contract_company_project` (`com_id`,`project_id`,`contract_id`),
  KEY `idx_project_contract_company` (`com_id`,`contract_id`),
  KEY `idx_project_contract_company_store` (`com_id`,`store_id`,`contract_id`),
  KEY `idx_project_contract_company_team` (`com_id`,`team_id`,`contract_id`),
  KEY `idx_project_contract_company_owner` (`com_id`,`owner_id`,`contract_id`),
  KEY `FKl67bno6yga4c98mht65elkc3a` (`creator_id`),
  KEY `FKjcb9e6a9dka6a3njor8d5e9l9` (`customer_id`),
  KEY `FK15sxe6g0loewlmu57fdo1e2np` (`owner_id`),
  KEY `FKr8cj9ywmggogv6dm9gb7juihq` (`store_id`),
  KEY `FK1x5q7gjpxcxc0mxgym2y93qfo` (`team_id`),
  CONSTRAINT `FK15sxe6g0loewlmu57fdo1e2np` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK1x5q7gjpxcxc0mxgym2y93qfo` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FK25243awate4b2l0wnehenea7r` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FK4yex8phl48c9o7hh44ojdf7xb` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKcn6l0tgqbbxsceocrgam6tw6l` FOREIGN KEY (`quote_id`) REFERENCES `quote` (`quote_id`),
  CONSTRAINT `FKjcb9e6a9dka6a3njor8d5e9l9` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKl67bno6yga4c98mht65elkc3a` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKordeembtftbsk9j0pskkpsnkt` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKr8cj9ywmggogv6dm9gb7juihq` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_project_contract_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_project_contract_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_project_contract`
--

LOCK TABLES `db_project_contract` WRITE;
/*!40000 ALTER TABLE `db_project_contract` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_project_contract` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_project_report`
--

DROP TABLE IF EXISTS `db_project_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_project_report` (
  `report_id` bigint NOT NULL,
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
  `author_role` enum('Boss','Designer','Finance','Marketer','Pm','StoreManager','TeamLeader') NOT NULL,
  `content` varchar(1024) NOT NULL,
  `title` varchar(128) NOT NULL,
  `type` enum('AcceptDone','AcceptRequest','Checkin','Issue','Material','Notice','Progress','StageOpen','TaskDone') NOT NULL,
  `visible_to_customer` bit(1) NOT NULL,
  `emp_id` bigint NOT NULL,
  `change_id` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `stage_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `task_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`report_id`),
  KEY `idx_project_report_company` (`com_id`,`report_id`),
  KEY `idx_project_report_company_project` (`com_id`,`project_id`,`report_id`),
  KEY `idx_project_report_company_author` (`com_id`,`emp_id`,`report_id`),
  KEY `idx_project_report_company_store` (`com_id`,`store_id`,`report_id`),
  KEY `idx_project_report_company_team` (`com_id`,`team_id`,`report_id`),
  KEY `idx_project_report_company_owner` (`com_id`,`owner_id`,`report_id`),
  KEY `FKqyb6g41qaw1v7ne3wxmlx96iq` (`emp_id`),
  KEY `FKc5i9q8lbk7k7o31ror7lanqg` (`change_id`),
  KEY `FKmfm4vxg9vgr9ofd9qysd13bo0` (`owner_id`),
  KEY `FKc1x9fiu70ko7cwlnxgvqfb2ny` (`project_id`),
  KEY `FKjar0kgdq8pikyy1bsayl32qry` (`property_id`),
  KEY `FKt3sral5sxdyachx8bpwvyvbdn` (`stage_id`),
  KEY `FKd9pedvtjalxl7nrfw66m7bfxk` (`store_id`),
  KEY `FKcg6vut3imwyoces2y7etbw0n4` (`task_id`),
  KEY `FK8vnq1o1rqdwqi437dvjjx73lo` (`team_id`),
  CONSTRAINT `FK8vnq1o1rqdwqi437dvjjx73lo` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKc1x9fiu70ko7cwlnxgvqfb2ny` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKc5i9q8lbk7k7o31ror7lanqg` FOREIGN KEY (`change_id`) REFERENCES `db_contract_change` (`change_id`),
  CONSTRAINT `FKcg6vut3imwyoces2y7etbw0n4` FOREIGN KEY (`task_id`) REFERENCES `db_project_task` (`task_id`),
  CONSTRAINT `FKd9pedvtjalxl7nrfw66m7bfxk` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKjar0kgdq8pikyy1bsayl32qry` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKmfm4vxg9vgr9ofd9qysd13bo0` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKpnkmw09b8tsrod4qv6ouwhpbs` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKqyb6g41qaw1v7ne3wxmlx96iq` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKt3sral5sxdyachx8bpwvyvbdn` FOREIGN KEY (`stage_id`) REFERENCES `db_project_stage` (`stage_id`),
  CONSTRAINT `db_project_report_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_project_report_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_project_report`
--

LOCK TABLES `db_project_report` WRITE;
/*!40000 ALTER TABLE `db_project_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_project_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_project_report_image`
--

DROP TABLE IF EXISTS `db_project_report_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_project_report_image` (
  `report_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKex18ma2vnq63c4j8w12o20kxh` (`report_id`),
  CONSTRAINT `FKex18ma2vnq63c4j8w12o20kxh` FOREIGN KEY (`report_id`) REFERENCES `db_project_report` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_project_report_image`
--

LOCK TABLES `db_project_report_image` WRITE;
/*!40000 ALTER TABLE `db_project_report_image` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_project_report_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_project_stage`
--

DROP TABLE IF EXISTS `db_project_stage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_project_stage` (
  `stage_id` bigint NOT NULL,
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
  `actual_end` date DEFAULT NULL,
  `actual_start` date DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `planned_end` date DEFAULT NULL,
  `planned_start` date DEFAULT NULL,
  `progress` int NOT NULL,
  `sort` int NOT NULL,
  `stage_code` varchar(64) DEFAULT NULL,
  `status` enum('Accepting','Doing','Done','Todo') NOT NULL,
  `weight` decimal(10,2) NOT NULL,
  `com_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`stage_id`),
  KEY `idx_project_stage_company` (`com_id`,`stage_id`),
  KEY `idx_project_stage_company_store` (`com_id`,`store_id`,`stage_id`),
  KEY `idx_project_stage_company_team` (`com_id`,`team_id`,`stage_id`),
  KEY `idx_project_stage_company_owner` (`com_id`,`owner_id`,`stage_id`),
  KEY `idx_project_stage_company_code` (`com_id`,`stage_code`),
  KEY `FKecuk3dr9rjrqsc9xnpif9t0q9` (`owner_id`),
  KEY `FKcpis8333mlqlcisap5dbxef5q` (`project_id`),
  KEY `FKq1uwkxcqto8o60wdsm9qvhnre` (`property_id`),
  KEY `FK343hf75b99jrk4gys5p9jf9yn` (`store_id`),
  KEY `FK614jmdrue8rt0ocs9ujt9jpxu` (`team_id`),
  CONSTRAINT `FK343hf75b99jrk4gys5p9jf9yn` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FK614jmdrue8rt0ocs9ujt9jpxu` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKaer79ykile80c5xdcocmc2609` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKcpis8333mlqlcisap5dbxef5q` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKecuk3dr9rjrqsc9xnpif9t0q9` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKq1uwkxcqto8o60wdsm9qvhnre` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `db_project_stage_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_project_stage_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_project_stage`
--

LOCK TABLES `db_project_stage` WRITE;
/*!40000 ALTER TABLE `db_project_stage` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_project_stage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_project_task`
--

DROP TABLE IF EXISTS `db_project_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_project_task` (
  `task_id` bigint NOT NULL,
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
  `actual_end` date DEFAULT NULL,
  `actual_start` date DEFAULT NULL,
  `conceal` bit(1) NOT NULL,
  `cost` decimal(12,2) NOT NULL,
  `craft` varchar(512) DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `planned_end` date DEFAULT NULL,
  `planned_start` date DEFAULT NULL,
  `priority` varchar(16) DEFAULT NULL,
  `progress` int NOT NULL,
  `quantity` decimal(12,2) NOT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `sale_price` decimal(12,2) NOT NULL,
  `spec` varchar(512) DEFAULT NULL,
  `status` enum('Accepting','Cancelled','Doing','Done','Todo') NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') NOT NULL,
  `zone` varchar(32) DEFAULT NULL,
  `catalog_item_id` bigint DEFAULT NULL,
  `change_item_id` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `contract_line_id` bigint DEFAULT NULL,
  `creator_id` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `package_line_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `quote_item_id` bigint DEFAULT NULL,
  `stage_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`task_id`),
  KEY `idx_project_task_company` (`com_id`,`task_id`),
  KEY `idx_project_task_company_store` (`com_id`,`store_id`,`task_id`),
  KEY `idx_project_task_company_team` (`com_id`,`team_id`,`task_id`),
  KEY `idx_project_task_company_owner` (`com_id`,`owner_id`,`task_id`),
  KEY `FKnxgnkwtlrnmug1yyavnsdsexd` (`catalog_item_id`),
  KEY `FKt17yw7aqk7pw5gg3ulocc3b8b` (`change_item_id`),
  KEY `FK9ix9lodkw740fl8jp5jasj0jq` (`contract_line_id`),
  KEY `FK1u1xmbfn5vnnn4odsab22cko0` (`creator_id`),
  KEY `FKjlt50awwckh4bfsmd6oxrws60` (`owner_id`),
  KEY `FKbnbqa2ko8q55orygcavt7yhn4` (`package_line_id`),
  KEY `FKlhri2hfoiyvnugai8gh5j2jsx` (`project_id`),
  KEY `FK8nn5xwy49tdd6pli2uunnq69v` (`property_id`),
  KEY `FK9aogiy52k0dypxknq540ipnv` (`quote_item_id`),
  KEY `FK61bx8jy0mf2t2b2sp7bsf69it` (`stage_id`),
  KEY `FKb17bl4j6196iei7vpcuoulccr` (`store_id`),
  KEY `FKip6t7qgh2wc6k7oarb9gq10oc` (`team_id`),
  CONSTRAINT `FK1u1xmbfn5vnnn4odsab22cko0` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK61bx8jy0mf2t2b2sp7bsf69it` FOREIGN KEY (`stage_id`) REFERENCES `db_project_stage` (`stage_id`),
  CONSTRAINT `FK8nn5xwy49tdd6pli2uunnq69v` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FK9aogiy52k0dypxknq540ipnv` FOREIGN KEY (`quote_item_id`) REFERENCES `quote_line` (`item_id`),
  CONSTRAINT `FK9ix9lodkw740fl8jp5jasj0jq` FOREIGN KEY (`contract_line_id`) REFERENCES `db_contract_line` (`line_id`),
  CONSTRAINT `FKb17bl4j6196iei7vpcuoulccr` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKbnbqa2ko8q55orygcavt7yhn4` FOREIGN KEY (`package_line_id`) REFERENCES `pack_line` (`item_id`),
  CONSTRAINT `FKc1nss18wvqb7oei7836nd3y49` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKip6t7qgh2wc6k7oarb9gq10oc` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKjlt50awwckh4bfsmd6oxrws60` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKlhri2hfoiyvnugai8gh5j2jsx` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKnxgnkwtlrnmug1yyavnsdsexd` FOREIGN KEY (`catalog_item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FKt17yw7aqk7pw5gg3ulocc3b8b` FOREIGN KEY (`change_item_id`) REFERENCES `db_contract_change_item` (`change_item_id`),
  CONSTRAINT `db_project_task_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_project_task_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_project_task`
--

LOCK TABLES `db_project_task` WRITE;
/*!40000 ALTER TABLE `db_project_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_project_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_project_task_evidence`
--

DROP TABLE IF EXISTS `db_project_task_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_project_task_evidence` (
  `task_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKrgp77ukyri1hy5hk2v2y0e4g0` (`task_id`),
  CONSTRAINT `FKrgp77ukyri1hy5hk2v2y0e4g0` FOREIGN KEY (`task_id`) REFERENCES `db_project_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_project_task_evidence`
--

LOCK TABLES `db_project_task_evidence` WRITE;
/*!40000 ALTER TABLE `db_project_task_evidence` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_project_task_evidence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_property`
--

DROP TABLE IF EXISTS `db_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_property` (
  `property_id` bigint NOT NULL,
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
  `address` varchar(256) NOT NULL,
  `area` decimal(10,2) DEFAULT NULL,
  `building` varchar(64) DEFAULT NULL,
  `community` varchar(128) DEFAULT NULL,
  `latest_measure_at` datetime(6) DEFAULT NULL,
  `layout` varchar(128) DEFAULT NULL,
  `property_no` varchar(64) NOT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `room_no` varchar(64) DEFAULT NULL,
  `status` enum('Completed','Contracted','Designing','Measured','Measuring','PendingMeasure','Quoted','Serving') NOT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `designer_id` bigint DEFAULT NULL,
  `emp_id` bigint NOT NULL,
  `salesman_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`property_id`),
  KEY `idx_property_company` (`com_id`,`property_id`),
  KEY `idx_property_company_customer` (`com_id`,`customer_id`,`property_id`),
  KEY `idx_property_company_store` (`com_id`,`store_id`,`property_id`),
  KEY `idx_property_company_team` (`com_id`,`team_id`,`property_id`),
  KEY `idx_property_company_owner` (`com_id`,`emp_id`,`property_id`),
  KEY `FK4a26tn5ymiqvcio5011op0jod` (`customer_id`),
  KEY `FK9ehuf0pj1xecvw0k6647oq44g` (`designer_id`),
  KEY `FKtr7n4oil97vloqcqnslcimkfp` (`emp_id`),
  KEY `FKs55ib41f0vlge6v96p1ga7ecw` (`salesman_id`),
  KEY `FKpsqbsev19vaxk7ex4v54sd687` (`store_id`),
  KEY `FKd2pxyjkpuojx4awlthojef1hs` (`team_id`),
  CONSTRAINT `FK3fthufhtsbj80vucaqpp1kvoe` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK4a26tn5ymiqvcio5011op0jod` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FK9ehuf0pj1xecvw0k6647oq44g` FOREIGN KEY (`designer_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKd2pxyjkpuojx4awlthojef1hs` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKpsqbsev19vaxk7ex4v54sd687` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKs55ib41f0vlge6v96p1ga7ecw` FOREIGN KEY (`salesman_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKtr7n4oil97vloqcqnslcimkfp` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_property_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_property_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_property`
--

LOCK TABLES `db_property` WRITE;
/*!40000 ALTER TABLE `db_property` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_property` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_purchase_order`
--

DROP TABLE IF EXISTS `db_purchase_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_purchase_order` (
  `purchase_id` bigint NOT NULL,
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
  `amount` decimal(14,2) NOT NULL,
  `approval_remark` varchar(500) DEFAULT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `delivery_mode` enum('Direct','Warehouse') NOT NULL,
  `expected_at` date DEFAULT NULL,
  `no` varchar(64) NOT NULL,
  `ordered_at` date DEFAULT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `received_quantity` decimal(12,3) NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `status` enum('Approved','Cancelled','Closed','Draft','Ordered','PartiallyReceived','Received','Rejected','Submitted') NOT NULL,
  `supplier_name` varchar(128) NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `approver_id` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `contract_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `material_plan_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `quote_line_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `supplier_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  `warehouse_id` bigint DEFAULT NULL,
  PRIMARY KEY (`purchase_id`),
  UNIQUE KEY `UKs3r84orrf66wcdwmxocnko2ug` (`no`),
  KEY `idx_purchase_company` (`com_id`,`purchase_id`),
  KEY `idx_purchase_company_project` (`com_id`,`project_id`,`purchase_id`),
  KEY `idx_purchase_company_property` (`com_id`,`property_id`,`purchase_id`),
  KEY `idx_purchase_order_company` (`com_id`,`purchase_id`),
  KEY `idx_purchase_order_company_store` (`com_id`,`store_id`,`purchase_id`),
  KEY `idx_purchase_order_company_team` (`com_id`,`team_id`,`purchase_id`),
  KEY `idx_purchase_order_company_owner` (`com_id`,`owner_id`,`purchase_id`),
  KEY `FKrbdjlvqpk6l9gy8lqnidtr171` (`approver_id`),
  KEY `FKpngm22bydpl1wnie3dv330alh` (`contract_id`),
  KEY `FK9sloskw8n5qcurc7xuqxc5xxq` (`creator_id`),
  KEY `FKpr0mgu8rlqllo4lridyw9orf1` (`material_plan_id`),
  KEY `FKel065945r12ujggxjys4eypce` (`owner_id`),
  KEY `FK160mxgluua1kgxw4veeg7hql2` (`project_id`),
  KEY `FKek5ot3sgw6derym46kfj2fo1i` (`property_id`),
  KEY `FKse5l173v9a0ko8v5gf0wqa9ex` (`quote_line_id`),
  KEY `FKhloaeyej9qs316s4ypf6o89t4` (`store_id`),
  KEY `FKscipi89kq9lm4gjamsk6uexoe` (`supplier_id`),
  KEY `FKhvg4lfhi8uasdvr8bc12h2ivr` (`team_id`),
  KEY `FKns9ghir36y7qmt66vrleodduk` (`warehouse_id`),
  CONSTRAINT `FK160mxgluua1kgxw4veeg7hql2` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FK9sloskw8n5qcurc7xuqxc5xxq` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKek5ot3sgw6derym46kfj2fo1i` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKel065945r12ujggxjys4eypce` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKhloaeyej9qs316s4ypf6o89t4` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKhvg4lfhi8uasdvr8bc12h2ivr` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKmk67jdaxutd5jyq67h6pu3lhc` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKns9ghir36y7qmt66vrleodduk` FOREIGN KEY (`warehouse_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKpngm22bydpl1wnie3dv330alh` FOREIGN KEY (`contract_id`) REFERENCES `db_project_contract` (`contract_id`),
  CONSTRAINT `FKpr0mgu8rlqllo4lridyw9orf1` FOREIGN KEY (`material_plan_id`) REFERENCES `db_material_plan` (`plan_id`),
  CONSTRAINT `FKrbdjlvqpk6l9gy8lqnidtr171` FOREIGN KEY (`approver_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKscipi89kq9lm4gjamsk6uexoe` FOREIGN KEY (`supplier_id`) REFERENCES `db_partner` (`partner_id`),
  CONSTRAINT `FKse5l173v9a0ko8v5gf0wqa9ex` FOREIGN KEY (`quote_line_id`) REFERENCES `quote_line` (`item_id`),
  CONSTRAINT `db_purchase_order_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_purchase_order_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_purchase_order`
--

LOCK TABLES `db_purchase_order` WRITE;
/*!40000 ALTER TABLE `db_purchase_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_purchase_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_purchase_order_item`
--

DROP TABLE IF EXISTS `db_purchase_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_purchase_order_item` (
  `purchase_item_id` bigint NOT NULL,
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
  `amount` decimal(14,2) NOT NULL,
  `price_source` varchar(32) DEFAULT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `received_quantity` decimal(12,3) NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `status` enum('Approved','Cancelled','Closed','Draft','Ordered','PartiallyReceived','Received','Rejected','Submitted') NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `com_id` bigint NOT NULL,
  `contract_id` bigint DEFAULT NULL,
  `material_plan_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `purchase_order_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`purchase_item_id`),
  KEY `idx_purchase_item_company` (`com_id`,`purchase_item_id`),
  KEY `idx_purchase_item_company_order` (`com_id`,`purchase_order_id`,`purchase_item_id`),
  KEY `idx_purchase_item_company_plan` (`com_id`,`material_plan_id`,`purchase_item_id`),
  KEY `idx_purchase_item_company_store` (`com_id`,`store_id`,`purchase_item_id`),
  KEY `idx_purchase_item_company_team` (`com_id`,`team_id`,`purchase_item_id`),
  KEY `idx_purchase_item_company_owner` (`com_id`,`owner_id`,`purchase_item_id`),
  KEY `FKrptyisga31355b2okbq0r4st` (`contract_id`),
  KEY `FK6lwreu35po7g8jo3o2d6x5ojw` (`material_plan_id`),
  KEY `FKs57nbsf4jt33bkcv9ljdib0lg` (`owner_id`),
  KEY `FKakeqaacweevhb60itnh6lhw24` (`project_id`),
  KEY `FK3c331vfawr5fav7hb8adayrrs` (`property_id`),
  KEY `FK8qgehouprwdinvypon6depoqp` (`purchase_order_id`),
  KEY `FKd4y1b2xvur2ugws5agkbn1lqm` (`store_id`),
  KEY `FKi4frn2rjip6005w5li661cw5m` (`team_id`),
  CONSTRAINT `FK3c331vfawr5fav7hb8adayrrs` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FK6lwreu35po7g8jo3o2d6x5ojw` FOREIGN KEY (`material_plan_id`) REFERENCES `db_material_plan` (`plan_id`),
  CONSTRAINT `FK8qgehouprwdinvypon6depoqp` FOREIGN KEY (`purchase_order_id`) REFERENCES `db_purchase_order` (`purchase_id`),
  CONSTRAINT `FKakeqaacweevhb60itnh6lhw24` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKd4y1b2xvur2ugws5agkbn1lqm` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKi4frn2rjip6005w5li661cw5m` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKilttlgo7g6nouh8qpnggpxjwx` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKrptyisga31355b2okbq0r4st` FOREIGN KEY (`contract_id`) REFERENCES `db_project_contract` (`contract_id`),
  CONSTRAINT `FKs57nbsf4jt33bkcv9ljdib0lg` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_purchase_order_item_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_purchase_order_item_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_purchase_order_item`
--

LOCK TABLES `db_purchase_order_item` WRITE;
/*!40000 ALTER TABLE `db_purchase_order_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_purchase_order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_risk_disposition_log`
--

DROP TABLE IF EXISTS `db_risk_disposition_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_risk_disposition_log` (
  `log_id` bigint NOT NULL,
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
  `action` enum('Assign','Claim','Close','Comment','Reopen','Urge','Void') NOT NULL,
  `from_status` enum('Closed','Handling','Pending','Void') DEFAULT NULL,
  `note` varchar(500) DEFAULT NULL,
  `occurred_at` datetime(6) DEFAULT NULL,
  `to_status` enum('Closed','Handling','Pending','Void') DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `risk_id` bigint NOT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_risk_log_company` (`com_id`,`log_id`),
  KEY `idx_risk_log_company_risk` (`com_id`,`risk_id`,`log_id`),
  KEY `FKoe5mx63m4xw3amgfny48gfg6f` (`operator_id`),
  KEY `FKn69mogomp9rhq0f1wsolk3a79` (`risk_id`),
  CONSTRAINT `FKn69mogomp9rhq0f1wsolk3a79` FOREIGN KEY (`risk_id`) REFERENCES `db_risk_item` (`risk_id`),
  CONSTRAINT `FKoe5mx63m4xw3amgfny48gfg6f` FOREIGN KEY (`operator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKpnibqnk59rdyvmtte5mogauv4` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_risk_disposition_log_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_risk_disposition_log_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_risk_disposition_log`
--

LOCK TABLES `db_risk_disposition_log` WRITE;
/*!40000 ALTER TABLE `db_risk_disposition_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_risk_disposition_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_risk_item`
--

DROP TABLE IF EXISTS `db_risk_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_risk_item` (
  `risk_id` bigint NOT NULL,
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
  `alert_key` varchar(160) NOT NULL,
  `alert_type` enum('AbnormalChangeRate','MaterialOverIssue','OverdueReceivable','ProjectSuspensionRisk') NOT NULL,
  `assigned_at` datetime(6) DEFAULT NULL,
  `business_id` bigint DEFAULT NULL,
  `business_type` varchar(40) DEFAULT NULL,
  `close_result` varchar(500) DEFAULT NULL,
  `closed_at` datetime(6) DEFAULT NULL,
  `discovered_at` datetime(6) DEFAULT NULL,
  `due_at` datetime(6) DEFAULT NULL,
  `level_at_create` enum('High','Low','Medium') DEFAULT NULL,
  `recurrence_count` int DEFAULT NULL,
  `reopen_reason` varchar(300) DEFAULT NULL,
  `round_no` int DEFAULT NULL,
  `status` enum('Closed','Handling','Pending','Void') NOT NULL,
  `closed_by` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`risk_id`),
  UNIQUE KEY `uk_risk_item_alert_key` (`com_id`,`alert_key`),
  KEY `idx_risk_item_company` (`com_id`,`risk_id`),
  KEY `idx_risk_item_company_store` (`com_id`,`store_id`,`risk_id`),
  KEY `idx_risk_item_company_team` (`com_id`,`team_id`,`risk_id`),
  KEY `idx_risk_item_company_owner` (`com_id`,`owner_id`,`risk_id`),
  KEY `idx_risk_item_company_status_due` (`com_id`,`status`,`due_at`),
  KEY `idx_risk_item_company_project` (`com_id`,`project_id`,`risk_id`),
  KEY `FK7llfpyriev8nqm76g3gbvgje7` (`closed_by`),
  KEY `FKs98l5gx5qqocximprotfa03x1` (`owner_id`),
  KEY `FKfdkmpu8saonwx15fstherl2dh` (`project_id`),
  KEY `FKgueyo51nteehwccyjdx5d83qm` (`store_id`),
  KEY `FKfeoyajjfrjl5dqplm5xourkaa` (`team_id`),
  CONSTRAINT `FK7llfpyriev8nqm76g3gbvgje7` FOREIGN KEY (`closed_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKfdkmpu8saonwx15fstherl2dh` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKfeoyajjfrjl5dqplm5xourkaa` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKgueyo51nteehwccyjdx5d83qm` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKipqtd1pbktte85fyx8hwiptui` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKs98l5gx5qqocximprotfa03x1` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_risk_item_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_risk_item_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_risk_item`
--

LOCK TABLES `db_risk_item` WRITE;
/*!40000 ALTER TABLE `db_risk_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_risk_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_risk_log_evidence`
--

DROP TABLE IF EXISTS `db_risk_log_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_risk_log_evidence` (
  `log_id` bigint NOT NULL,
  `evidence_url` varchar(512) DEFAULT NULL,
  KEY `FK8ajqu6wsgjef0wkx5mahfvrxq` (`log_id`),
  CONSTRAINT `FK8ajqu6wsgjef0wkx5mahfvrxq` FOREIGN KEY (`log_id`) REFERENCES `db_risk_disposition_log` (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_risk_log_evidence`
--

LOCK TABLES `db_risk_log_evidence` WRITE;
/*!40000 ALTER TABLE `db_risk_log_evidence` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_risk_log_evidence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_stock_movement`
--

DROP TABLE IF EXISTS `db_stock_movement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_stock_movement` (
  `movement_id` bigint NOT NULL,
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
  `after_quantity` decimal(14,3) NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `before_quantity` decimal(14,3) NOT NULL,
  `occurred_at` datetime(6) DEFAULT NULL,
  `quantity` decimal(14,3) NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `source_no` varchar(64) DEFAULT NULL,
  `type` enum('Adjustment','Inbound','Issue','Loss','ReturnIn','ReturnOut') NOT NULL,
  `unit_cost` decimal(12,2) NOT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `material_plan_id` bigint DEFAULT NULL,
  `operator_id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  `purchase_id` bigint DEFAULT NULL,
  `quote_line_id` bigint DEFAULT NULL,
  `stock_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  PRIMARY KEY (`movement_id`),
  KEY `idx_stock_movement_company` (`com_id`,`movement_id`),
  KEY `idx_stock_movement_project` (`com_id`,`project_id`,`movement_id`),
  KEY `idx_stock_movement_stock` (`com_id`,`stock_id`,`movement_id`),
  KEY `FKec6ca7ea5v6kiegpnaguwyhlt` (`item_id`),
  KEY `FKkh3wpkyi2flbqyixnjqc8x3x9` (`material_plan_id`),
  KEY `FKqjdnfiudas2eeb2h2f07uxv0f` (`operator_id`),
  KEY `FKdb8kcilyyg2bypyn4u6dli0vb` (`project_id`),
  KEY `FKcysjakqc543sg7bd84bidxnhg` (`purchase_id`),
  KEY `FK515j1l4mg8nl7a2or7dx1lwjx` (`quote_line_id`),
  KEY `FKtdgahwq45fqlpj6avf2t1o54p` (`stock_id`),
  KEY `FK9pa1j1leb8kafj5w6igyl4o7j` (`store_id`),
  CONSTRAINT `FK515j1l4mg8nl7a2or7dx1lwjx` FOREIGN KEY (`quote_line_id`) REFERENCES `quote_line` (`item_id`),
  CONSTRAINT `FK9pa1j1leb8kafj5w6igyl4o7j` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKcysjakqc543sg7bd84bidxnhg` FOREIGN KEY (`purchase_id`) REFERENCES `db_purchase_order` (`purchase_id`),
  CONSTRAINT `FKdb8kcilyyg2bypyn4u6dli0vb` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKec6ca7ea5v6kiegpnaguwyhlt` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FKf6eob4gv2dhacha9h8ejtnv86` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKkh3wpkyi2flbqyixnjqc8x3x9` FOREIGN KEY (`material_plan_id`) REFERENCES `db_material_plan` (`plan_id`),
  CONSTRAINT `FKqjdnfiudas2eeb2h2f07uxv0f` FOREIGN KEY (`operator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKtdgahwq45fqlpj6avf2t1o54p` FOREIGN KEY (`stock_id`) REFERENCES `db_inventory_stock` (`stock_id`),
  CONSTRAINT `db_stock_movement_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_stock_movement_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_stock_movement`
--

LOCK TABLES `db_stock_movement` WRITE;
/*!40000 ALTER TABLE `db_stock_movement` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_stock_movement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_stock_movement_image`
--

DROP TABLE IF EXISTS `db_stock_movement_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_stock_movement_image` (
  `movement_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FK7mnx1erit9eb99il88tmd59cn` (`movement_id`),
  CONSTRAINT `FK7mnx1erit9eb99il88tmd59cn` FOREIGN KEY (`movement_id`) REFERENCES `db_stock_movement` (`movement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_stock_movement_image`
--

LOCK TABLES `db_stock_movement_image` WRITE;
/*!40000 ALTER TABLE `db_stock_movement_image` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_stock_movement_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_task_node`
--

DROP TABLE IF EXISTS `db_task_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_task_node` (
  `node_id` bigint NOT NULL,
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
  `node_key` enum('Conceal','Finish','Start') NOT NULL,
  `note` varchar(512) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `task_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`node_id`),
  KEY `idx_task_node_company` (`com_id`,`node_id`),
  KEY `idx_task_node_company_task` (`com_id`,`task_id`,`node_id`),
  KEY `idx_task_node_company_store` (`com_id`,`store_id`,`node_id`),
  KEY `idx_task_node_company_team` (`com_id`,`team_id`,`node_id`),
  KEY `idx_task_node_company_owner` (`com_id`,`owner_id`,`node_id`),
  KEY `FKp3og8qt4o9fb9jpykxp8yavmk` (`emp_id`),
  KEY `FKijmaxtwy1aypra5mub4j0nsi1` (`owner_id`),
  KEY `FK2hjabq3t2sluvghy7yyb3qkep` (`store_id`),
  KEY `FK2fokhq78l9k00q3qtu6bje9ki` (`task_id`),
  KEY `FKj3lpry0ncpp5m76eam7496p0x` (`team_id`),
  CONSTRAINT `FK2fokhq78l9k00q3qtu6bje9ki` FOREIGN KEY (`task_id`) REFERENCES `db_project_task` (`task_id`),
  CONSTRAINT `FK2hjabq3t2sluvghy7yyb3qkep` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKijmaxtwy1aypra5mub4j0nsi1` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKj3lpry0ncpp5m76eam7496p0x` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKp3og8qt4o9fb9jpykxp8yavmk` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKqjstg317h2posvymyme0mp1t5` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_task_node_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_task_node_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_task_node`
--

LOCK TABLES `db_task_node` WRITE;
/*!40000 ALTER TABLE `db_task_node` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_task_node` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_task_node_media`
--

DROP TABLE IF EXISTS `db_task_node_media`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_task_node_media` (
  `node_id` bigint NOT NULL,
  `url` varchar(512) DEFAULT NULL,
  KEY `FKa1td6i6wsyv0p88p8xxkxmirv` (`node_id`),
  CONSTRAINT `FKa1td6i6wsyv0p88p8xxkxmirv` FOREIGN KEY (`node_id`) REFERENCES `db_task_node` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_task_node_media`
--

LOCK TABLES `db_task_node_media` WRITE;
/*!40000 ALTER TABLE `db_task_node_media` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_task_node_media` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_tenant_balance_ledger`
--

DROP TABLE IF EXISTS `db_tenant_balance_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_balance_ledger` (
  `ledger_id` bigint NOT NULL,
  `balance_after` int NOT NULL,
  `biz_type` enum('ManualAdjust','RechargeGrant','TrialGrant') NOT NULL,
  `change_days` int NOT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `remark` varchar(256) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  PRIMARY KEY (`ledger_id`),
  KEY `FK21wr1akfvsfycd7gjrec2scj2` (`com_id`),
  KEY `FKispx3m9qwjtpdkcxjpq0e3mem` (`order_id`),
  CONSTRAINT `FK21wr1akfvsfycd7gjrec2scj2` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKispx3m9qwjtpdkcxjpq0e3mem` FOREIGN KEY (`order_id`) REFERENCES `db_tenant_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_tenant_balance_ledger`
--

LOCK TABLES `db_tenant_balance_ledger` WRITE;
/*!40000 ALTER TABLE `db_tenant_balance_ledger` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_tenant_balance_ledger` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_tenant_company`
--

DROP TABLE IF EXISTS `db_tenant_company`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_company` (
  `com_id` bigint NOT NULL,
  `addr` varchar(256) DEFAULT NULL,
  `color` varchar(12) DEFAULT NULL,
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
  `logo` varchar(256) DEFAULT NULL,
  `name` varchar(32) NOT NULL,
  `enable_store` bit(1) DEFAULT NULL,
  `grade_config` varchar(512) DEFAULT NULL,
  `size` enum('A','B','C','D') NOT NULL,
  `title` varchar(128) NOT NULL,
  PRIMARY KEY (`com_id`),
  CONSTRAINT `db_tenant_company_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_tenant_company`
--

LOCK TABLES `db_tenant_company` WRITE;
/*!40000 ALTER TABLE `db_tenant_company` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_tenant_company` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_tenant_credentials`
--

DROP TABLE IF EXISTS `db_tenant_credentials`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_credentials` (
  `cre_id` bigint NOT NULL,
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
  `failed_attempts` int NOT NULL DEFAULT '0',
  `id_key` varchar(64) NOT NULL,
  `id_secret` varchar(128) DEFAULT NULL,
  `last_time` datetime(6) DEFAULT NULL,
  `locked_until` datetime(6) DEFAULT NULL,
  `must_change_password` bit(1) NOT NULL DEFAULT b'0',
  `password_hash` varchar(100) DEFAULT NULL,
  `password_updated_at` datetime(6) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'Active',
  `type` enum('Password','Wx') NOT NULL,
  `emp_id` bigint DEFAULT NULL,
  PRIMARY KEY (`cre_id`),
  UNIQUE KEY `UK9svmdwraex6pgbbgujcq6iten` (`id_key`),
  UNIQUE KEY `UKg6ywcsictc2pgk900c3i99wqc` (`phone`),
  KEY `FK8aosmicij0xds6vq1gmubxo6` (`emp_id`),
  CONSTRAINT `FK8aosmicij0xds6vq1gmubxo6` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_tenant_credentials_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_tenant_credentials`
--

LOCK TABLES `db_tenant_credentials` WRITE;
/*!40000 ALTER TABLE `db_tenant_credentials` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_tenant_credentials` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_tenant_employee`
--

DROP TABLE IF EXISTS `db_tenant_employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_employee` (
  `emp_id` bigint NOT NULL,
  `avatar_url` varchar(512) DEFAULT NULL,
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
  `joined_at` datetime(6) DEFAULT NULL,
  `name` varchar(15) NOT NULL,
  `resign_reason` varchar(500) DEFAULT NULL,
  `resigned_at` datetime(6) DEFAULT NULL,
  `role` enum('Boss','Designer','Finance','Marketer','Pm','StoreManager','TeamLeader') NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'Active',
  `tel_no` varchar(12) NOT NULL,
  `com_id` bigint NOT NULL,
  `invite_id` bigint DEFAULT NULL,
  `store_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`emp_id`),
  KEY `idx_tenant_employee_company` (`com_id`,`emp_id`),
  KEY `idx_tenant_employee_store` (`com_id`,`store_id`,`emp_id`),
  KEY `idx_tenant_employee_team` (`com_id`,`team_id`,`emp_id`),
  KEY `idx_tenant_employee_company_phone_status` (`com_id`,`tel_no`,`status`),
  KEY `FK10gq6wj54vd4divp4ua9imkuj` (`invite_id`),
  KEY `FK2mgsat730eohj0jhepvwg44fb` (`store_id`),
  KEY `FKk4b2guj8m5q236x8rnxt3dqno` (`team_id`),
  CONSTRAINT `FK10gq6wj54vd4divp4ua9imkuj` FOREIGN KEY (`invite_id`) REFERENCES `db_tenant_invite` (`invite_id`),
  CONSTRAINT `FK2mgsat730eohj0jhepvwg44fb` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKk4b2guj8m5q236x8rnxt3dqno` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKpwsv4i6ywug87cpfu1xq2lah4` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_tenant_employee_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_tenant_employee`
--

LOCK TABLES `db_tenant_employee` WRITE;
/*!40000 ALTER TABLE `db_tenant_employee` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_tenant_employee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_tenant_invite`
--

DROP TABLE IF EXISTS `db_tenant_invite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_invite` (
  `invite_id` bigint NOT NULL,
  `accepted_at` datetime(6) DEFAULT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
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
  `expire_time` datetime(6) DEFAULT NULL,
  `invite_name` varchar(15) DEFAULT NULL,
  `rejected_at` datetime(6) DEFAULT NULL,
  `role` enum('Boss','Designer','Finance','Marketer','Pm','StoreManager','TeamLeader') NOT NULL,
  `status` enum('Accepted','Cancelled','Expired','Pending','Rejected') NOT NULL,
  `tel_no` varchar(11) NOT NULL,
  `accepted_user_id` bigint DEFAULT NULL,
  `cancelled_by_id` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `inviter_id` bigint NOT NULL,
  `store_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`invite_id`),
  KEY `FKnw1oenu2epknyqysagdsw0hrn` (`accepted_user_id`),
  KEY `FKrsllgio8nqe5jvpchcrj6d8w1` (`cancelled_by_id`),
  KEY `FK3g3924uqmg7w6l2d0638g4i5k` (`com_id`),
  KEY `FKsgiujrdtbsmdguws7rdy1a4ee` (`inviter_id`),
  KEY `FK57app8yq0da4976s9psn90ksf` (`store_id`),
  KEY `FKesi5e184fq1wwdl3hwcp8jik7` (`team_id`),
  CONSTRAINT `FK3g3924uqmg7w6l2d0638g4i5k` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK57app8yq0da4976s9psn90ksf` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKesi5e184fq1wwdl3hwcp8jik7` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKnw1oenu2epknyqysagdsw0hrn` FOREIGN KEY (`accepted_user_id`) REFERENCES `db_tenant_credentials` (`cre_id`),
  CONSTRAINT `FKrsllgio8nqe5jvpchcrj6d8w1` FOREIGN KEY (`cancelled_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKsgiujrdtbsmdguws7rdy1a4ee` FOREIGN KEY (`inviter_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_tenant_invite_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_tenant_invite`
--

LOCK TABLES `db_tenant_invite` WRITE;
/*!40000 ALTER TABLE `db_tenant_invite` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_tenant_invite` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_tenant_order`
--

DROP TABLE IF EXISTS `db_tenant_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_order` (
  `order_id` bigint NOT NULL,
  `amount` bigint NOT NULL,
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
  `days` int NOT NULL,
  `expire_time` datetime(6) DEFAULT NULL,
  `extra_seats` int NOT NULL,
  `extra_stores` int NOT NULL,
  `order_no` varchar(40) NOT NULL,
  `out_trade_no` varchar(64) DEFAULT NULL,
  `paid_time` datetime(6) DEFAULT NULL,
  `pay_channel` enum('Alipay','Offline','Wx') DEFAULT NULL,
  `status` enum('Cancelled','Expired','Granted','Paid','Pending','Refunded') NOT NULL,
  `transaction_id` varchar(64) DEFAULT NULL,
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
  `com_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `plan_version_id` bigint DEFAULT NULL,
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `UKdxt1mqtjx16rhfcv9w2ass401` (`order_no`),
  KEY `FKmegqc8nxjvqrvr0fyqwm2lpo7` (`com_id`),
  KEY `FKha1xf61jevds6chbjiur5glct` (`plan_id`),
  KEY `FKo55npif3k4dwj5c6c0mpjd7td` (`plan_version_id`),
  CONSTRAINT `FKha1xf61jevds6chbjiur5glct` FOREIGN KEY (`plan_id`) REFERENCES `db_tenant_plan` (`plan_id`),
  CONSTRAINT `FKmegqc8nxjvqrvr0fyqwm2lpo7` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKo55npif3k4dwj5c6c0mpjd7td` FOREIGN KEY (`plan_version_id`) REFERENCES `db_tenant_plan_version` (`version_id`),
  CONSTRAINT `db_tenant_order_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_tenant_order_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_tenant_order`
--

LOCK TABLES `db_tenant_order` WRITE;
/*!40000 ALTER TABLE `db_tenant_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_tenant_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_tenant_order_item`
--

DROP TABLE IF EXISTS `db_tenant_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_order_item` (
  `order_item_id` bigint NOT NULL,
  `amount` bigint NOT NULL,
  `days` int NOT NULL,
  `item_type` enum('Plan','Seats','Stores') NOT NULL,
  `product_code` varchar(32) NOT NULL,
  `product_name` varchar(64) NOT NULL,
  `quantity` int NOT NULL,
  `unit_price` bigint NOT NULL,
  `addon_id` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `plan_version_id` bigint DEFAULT NULL,
  PRIMARY KEY (`order_item_id`),
  KEY `idx_order_item_order` (`order_id`,`order_item_id`),
  KEY `idx_order_item_company` (`com_id`,`order_item_id`),
  KEY `FKqu485p3q1sjj3b7bglm6qu64n` (`addon_id`),
  KEY `FK79d2n342jiecofbg3j0hxeear` (`plan_version_id`),
  CONSTRAINT `FK1u94sekyfrhbtkmq4brm4r2yv` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK79d2n342jiecofbg3j0hxeear` FOREIGN KEY (`plan_version_id`) REFERENCES `db_tenant_plan_version` (`version_id`),
  CONSTRAINT `FKgifykdqk4avuop8agye97ybcg` FOREIGN KEY (`order_id`) REFERENCES `db_tenant_order` (`order_id`),
  CONSTRAINT `FKqu485p3q1sjj3b7bglm6qu64n` FOREIGN KEY (`addon_id`) REFERENCES `db_tenant_plan_addon` (`addon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_tenant_order_item`
--

LOCK TABLES `db_tenant_order_item` WRITE;
/*!40000 ALTER TABLE `db_tenant_order_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_tenant_order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_tenant_plan`
--

DROP TABLE IF EXISTS `db_tenant_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_plan` (
  `plan_id` bigint NOT NULL,
  `badge` varchar(32) DEFAULT NULL,
  `code` varchar(32) NOT NULL,
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
  `days` int NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `highlights` varchar(1024) DEFAULT NULL,
  `max_seats` int DEFAULT NULL,
  `max_stores` int NOT NULL,
  `modules` varchar(512) DEFAULT NULL,
  `name` varchar(64) NOT NULL,
  `one_time_only` bit(1) NOT NULL,
  `original_price` bigint DEFAULT NULL,
  `price` bigint NOT NULL,
  `priority` int NOT NULL,
  `recommended` bit(1) NOT NULL,
  `tagline` varchar(256) DEFAULT NULL,
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
  PRIMARY KEY (`plan_id`),
  UNIQUE KEY `UK60jfglxywipugpbv8iy7b2ri6` (`code`),
  CONSTRAINT `db_tenant_plan_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_tenant_plan_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_tenant_plan`
--

LOCK TABLES `db_tenant_plan` WRITE;
/*!40000 ALTER TABLE `db_tenant_plan` DISABLE KEYS */;
INSERT INTO `db_tenant_plan` VALUES (1790109187588856010,'体验版','free-quote',20260923,20260923043307,23,4,33,9,7,43307,2,2026,0,'1 家门店、2 个席位，全功能能力',_binary '','基础客户与报价能力;适合个人工作室首次上线;无需支付年费',2,1,'Crm,Design,Quote,Project,Supply,Finance,Analytics,Ai,Api','免费报价版',_binary '\0',0,0,0,_binary '\0','低门槛启用装企数字化',20260923,20260923043307,23,4,33,9,7,43307,2,2026),(1790109187635096067,'单店首选','annual-basic',20260923,20260923043307,23,4,33,9,7,43307,2,2026,365,'单店起步，全功能经营，限制为 1 家门店和 5 个席位',_binary '','注册赠送 14 天免费试用;客户到签约经营闭环;设计、报价、项目管理;5 个员工席位',5,1,'Crm,Design,Quote,Project,Supply,Finance,Analytics,Ai,Api','基础版年费',_binary '\0',598000,598000,10,_binary '\0','单店经营全流程，一次开通全年可用',20260923,20260923043307,23,4,33,9,7,43307,2,2026),(1790109187644751349,'最受欢迎','annual-pro',20260923,20260923043307,23,4,33,9,7,43307,2,2026,365,'成长型多店装企，包含完整经营、财务和供应链闭环',_binary '','3 家门店、15 个席位;供应链与财务结算;经营分析看板;适合成长型多店装企',15,3,'Crm,Design,Quote,Project,Supply,Finance,Analytics,Ai,Api','专业版年费',_binary '\0',1198000,1198000,20,_binary '','从增长到盈利，一套系统管全店',20260923,20260923043307,23,4,33,9,7,43307,2,2026),(1790109187654282858,'连锁进阶','annual-flagship',20260923,20260923043307,23,4,33,9,7,43307,2,2026,365,'连锁和多品牌装企，包含多门店、开放接口、实施陪跑和高级经营能力',_binary '','不限门店、30 个席位;AI 与开放接口;全模块经营体系;专属上线实施支持',30,9999,'Crm,Design,Quote,Project,Supply,Finance,Analytics,Ai,Api','旗舰版年费',_binary '\0',2180000,2180000,30,_binary '\0','连锁扩张与品牌管控的完整底座',20260923,20260923043307,23,4,33,9,7,43307,2,2026);
/*!40000 ALTER TABLE `db_tenant_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_tenant_plan_addon`
--

DROP TABLE IF EXISTS `db_tenant_plan_addon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_plan_addon` (
  `addon_id` bigint NOT NULL,
  `code` varchar(32) NOT NULL,
  `days` int NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `max_quantity` int NOT NULL,
  `name` varchar(64) NOT NULL,
  `priority` int NOT NULL,
  `type` enum('Seats','Stores') NOT NULL,
  `unit_price` bigint NOT NULL,
  PRIMARY KEY (`addon_id`),
  UNIQUE KEY `UKrg1qdyfgnh5dkso5nhqlehuq8` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_tenant_plan_addon`
--

LOCK TABLES `db_tenant_plan_addon` WRITE;
/*!40000 ALTER TABLE `db_tenant_plan_addon` DISABLE KEYS */;
INSERT INTO `db_tenant_plan_addon` VALUES (1790109187663563301,'seat-pack',365,'按账号加购，每账号每年 400 元',_binary '',200,'席位包',10,'Seats',40000),(1790109187670068298,'store-pack',365,'按门店加购，每门店每年 3,600 元',_binary '',50,'门店包',20,'Stores',360000);
/*!40000 ALTER TABLE `db_tenant_plan_addon` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_tenant_plan_version`
--

DROP TABLE IF EXISTS `db_tenant_plan_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_plan_version` (
  `version_id` bigint NOT NULL,
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
  `days` int NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `max_seats` int NOT NULL,
  `max_stores` int NOT NULL,
  `modules` varchar(512) NOT NULL,
  `original_price` bigint DEFAULT NULL,
  `plan_code` varchar(32) NOT NULL,
  `plan_name` varchar(64) NOT NULL,
  `price` bigint NOT NULL,
  `version_no` int NOT NULL,
  `plan_id` bigint NOT NULL,
  PRIMARY KEY (`version_id`),
  UNIQUE KEY `uk_plan_version` (`plan_id`,`version_no`),
  KEY `idx_plan_version_plan` (`plan_id`,`version_no`),
  CONSTRAINT `FKq1pxt8a8g963rfgomvbmwm4y8` FOREIGN KEY (`plan_id`) REFERENCES `db_tenant_plan` (`plan_id`),
  CONSTRAINT `db_tenant_plan_version_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_tenant_plan_version`
--

LOCK TABLES `db_tenant_plan_version` WRITE;
/*!40000 ALTER TABLE `db_tenant_plan_version` DISABLE KEYS */;
INSERT INTO `db_tenant_plan_version` VALUES (1790109187616420416,20260923,20260923043307,23,4,33,9,7,43307,2,2026,0,'1 家门店、2 个席位，全功能能力',2,1,'Crm,Design,Quote,Project,Supply,Finance,Analytics,Ai,Api',0,'free-quote','免费报价版',0,1,1790109187588856010),(1790109187638245271,20260923,20260923043307,23,4,33,9,7,43307,2,2026,365,'单店起步，全功能经营，限制为 1 家门店和 5 个席位',5,1,'Crm,Design,Quote,Project,Supply,Finance,Analytics,Ai,Api',598000,'annual-basic','基础版年费',598000,1,1790109187635096067),(1790109187648934101,20260923,20260923043307,23,4,33,9,7,43307,2,2026,365,'成长型多店装企，包含完整经营、财务和供应链闭环',15,3,'Crm,Design,Quote,Project,Supply,Finance,Analytics,Ai,Api',1198000,'annual-pro','专业版年费',1198000,1,1790109187644751349),(1790109187657830405,20260923,20260923043307,23,4,33,9,7,43307,2,2026,365,'连锁和多品牌装企，包含多门店、开放接口、实施陪跑和高级经营能力',30,9999,'Crm,Design,Quote,Project,Supply,Finance,Analytics,Ai,Api',2180000,'annual-flagship','旗舰版年费',2180000,1,1790109187654282858);
/*!40000 ALTER TABLE `db_tenant_plan_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_tenant_store`
--

DROP TABLE IF EXISTS `db_tenant_store`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_store` (
  `store_id` bigint NOT NULL,
  `addr` varchar(256) DEFAULT NULL,
  `color` varchar(12) DEFAULT NULL,
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
  `default_store` bit(1) NOT NULL,
  `name` varchar(16) NOT NULL,
  `company_com_id` bigint NOT NULL,
  PRIMARY KEY (`store_id`),
  KEY `FKiv3if8umjugca837cg5whj9j8` (`company_com_id`),
  CONSTRAINT `FKiv3if8umjugca837cg5whj9j8` FOREIGN KEY (`company_com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_tenant_store_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_tenant_store`
--

LOCK TABLES `db_tenant_store` WRITE;
/*!40000 ALTER TABLE `db_tenant_store` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_tenant_store` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_tenant_subscription`
--

DROP TABLE IF EXISTS `db_tenant_subscription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_subscription` (
  `sub_id` bigint NOT NULL,
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
  `extra_seats` int NOT NULL,
  `extra_stores` int NOT NULL,
  `last_renew_time` datetime(6) DEFAULT NULL,
  `service_end_date` int DEFAULT NULL,
  `service_end_date_time` bigint DEFAULT NULL,
  `service_end_day` int DEFAULT NULL,
  `service_end_hour` int DEFAULT NULL,
  `service_end_minute` int DEFAULT NULL,
  `service_end_month` int DEFAULT NULL,
  `service_end_second` int DEFAULT NULL,
  `service_end_time` int DEFAULT NULL,
  `service_end_week` tinyint DEFAULT NULL,
  `service_end_year` int DEFAULT NULL,
  `status` enum('Active','Closed','Expired','Frozen','Trial') NOT NULL,
  `trial_activated` bit(1) NOT NULL,
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
  `com_id` bigint NOT NULL,
  `plan_id` bigint DEFAULT NULL,
  `plan_version_id` bigint DEFAULT NULL,
  PRIMARY KEY (`sub_id`),
  UNIQUE KEY `UKdw8yth3rgy6kywpch2uk5i85` (`com_id`),
  KEY `FKem38swt7xxjeg7xe4tc0d38d4` (`plan_id`),
  KEY `FKk45xoc7us82l6xno245usdmp0` (`plan_version_id`),
  CONSTRAINT `FKbxgx7s5a8bh646l5ijdkm2ylf` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKem38swt7xxjeg7xe4tc0d38d4` FOREIGN KEY (`plan_id`) REFERENCES `db_tenant_plan` (`plan_id`),
  CONSTRAINT `FKk45xoc7us82l6xno245usdmp0` FOREIGN KEY (`plan_version_id`) REFERENCES `db_tenant_plan_version` (`version_id`),
  CONSTRAINT `db_tenant_subscription_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_tenant_subscription_chk_2` CHECK ((`service_end_week` between 0 and 6)),
  CONSTRAINT `db_tenant_subscription_chk_3` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_tenant_subscription`
--

LOCK TABLES `db_tenant_subscription` WRITE;
/*!40000 ALTER TABLE `db_tenant_subscription` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_tenant_subscription` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_tenant_team`
--

DROP TABLE IF EXISTS `db_tenant_team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_team` (
  `team_id` bigint NOT NULL,
  `color` varchar(12) DEFAULT NULL,
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
  `name` varchar(128) NOT NULL,
  `com_id` bigint NOT NULL,
  `leader_emp_id` bigint NOT NULL,
  `store_id` bigint NOT NULL,
  PRIMARY KEY (`team_id`),
  KEY `idx_tenant_team_company` (`com_id`,`team_id`),
  KEY `idx_tenant_team_store` (`com_id`,`store_id`,`team_id`),
  KEY `FKjguppjmoruiiemfdxuah5e6ao` (`leader_emp_id`),
  KEY `FKqkyfwsgkbmoe39bgrr5nmdcm6` (`store_id`),
  CONSTRAINT `FKcb21b73qbfi0toe9smks6ah26` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKjguppjmoruiiemfdxuah5e6ao` FOREIGN KEY (`leader_emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKqkyfwsgkbmoe39bgrr5nmdcm6` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_tenant_team_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_tenant_team`
--

LOCK TABLES `db_tenant_team` WRITE;
/*!40000 ALTER TABLE `db_tenant_team` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_tenant_team` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_user_security_log`
--

DROP TABLE IF EXISTS `db_user_security_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_user_security_log` (
  `log_id` bigint NOT NULL,
  `action` enum('AccountLock','AccountUnlock','BossTransfer','CompanyJoin','CompanyResign','CompanySwitch','InviteAccept','InviteReject','LimitTrigger','Login','Logout','PasswordChange','PasswordSet','PhoneBind','Register') NOT NULL,
  `com_id` bigint DEFAULT NULL,
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
  `emp_id` bigint DEFAULT NULL,
  `ip` varchar(64) DEFAULT NULL,
  `open_id` varchar(128) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `result` enum('Blocked','Failure','Success') NOT NULL,
  `user_agent` varchar(512) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_security_log_user` (`user_id`,`create_date_time`),
  KEY `idx_security_log_open` (`open_id`,`create_date_time`),
  KEY `idx_security_log_phone` (`phone`,`create_date_time`),
  CONSTRAINT `db_user_security_log_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_user_security_log`
--

LOCK TABLES `db_user_security_log` WRITE;
/*!40000 ALTER TABLE `db_user_security_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_user_security_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_voucher_item`
--

DROP TABLE IF EXISTS `db_voucher_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_voucher_item` (
  `item_id` bigint NOT NULL,
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
  `account` enum('Alipay','Cash','CompanyBank','Other','Wechat') NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `business_date` date NOT NULL,
  `category` varchar(32) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `source_id` bigint NOT NULL,
  `source_no` varchar(64) NOT NULL,
  `source_type` enum('Contract','Material','Payment','Quote','Receipt') NOT NULL,
  `com_id` bigint NOT NULL,
  `voucher_id` bigint NOT NULL,
  PRIMARY KEY (`item_id`),
  KEY `idx_voucher_item_company` (`com_id`,`item_id`),
  KEY `idx_voucher_item_voucher` (`voucher_id`,`item_id`),
  KEY `idx_voucher_item_source` (`com_id`,`source_type`,`source_id`),
  CONSTRAINT `FKjlc3v1ynco58hxf8drqh2k9qr` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKpbsvmba1gg36lpmiygweobkhl` FOREIGN KEY (`voucher_id`) REFERENCES `db_voucher_record` (`voucher_id`),
  CONSTRAINT `db_voucher_item_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_voucher_item_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_voucher_item`
--

LOCK TABLES `db_voucher_item` WRITE;
/*!40000 ALTER TABLE `db_voucher_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_voucher_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_voucher_record`
--

DROP TABLE IF EXISTS `db_voucher_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_voucher_record` (
  `voucher_id` bigint NOT NULL,
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
  `amount_words` varchar(128) NOT NULL,
  `content_type` varchar(64) NOT NULL,
  `file_name` varchar(256) NOT NULL,
  `generated_at` datetime(6) NOT NULL,
  `party_name` varchar(128) DEFAULT NULL,
  `path` varchar(512) NOT NULL,
  `sha256` varchar(64) NOT NULL,
  `size` bigint NOT NULL,
  `status` enum('Active','Voided') NOT NULL,
  `template_version` int NOT NULL,
  `title` varchar(128) NOT NULL,
  `total_amount` decimal(14,2) NOT NULL,
  `type` enum('Contract','Material','Payment','Quote','Receipt') NOT NULL,
  `voucher_no` varchar(32) NOT NULL,
  `com_id` bigint NOT NULL,
  `generated_by_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `partner_id` bigint DEFAULT NULL,
  `project_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  `template_id` bigint DEFAULT NULL,
  `worker_id` bigint DEFAULT NULL,
  PRIMARY KEY (`voucher_id`),
  UNIQUE KEY `uk_voucher_company_no` (`com_id`,`voucher_no`),
  KEY `idx_voucher_company` (`com_id`,`voucher_id`),
  KEY `idx_voucher_company_project` (`com_id`,`project_id`,`voucher_id`),
  KEY `idx_voucher_company_store` (`com_id`,`store_id`,`voucher_id`),
  KEY `idx_voucher_company_team` (`com_id`,`team_id`,`voucher_id`),
  KEY `idx_voucher_company_owner` (`com_id`,`owner_id`,`voucher_id`),
  KEY `FK1ut3obdkdy8xcu2dmkhlqsb4k` (`generated_by_id`),
  KEY `FKelaj4k633qgjxiwogbwi37o9e` (`owner_id`),
  KEY `FKrqfrs3e29muqlvv9j42pn01k2` (`partner_id`),
  KEY `FKfv6plwk14iy7wgbi7waq1wfm3` (`project_id`),
  KEY `FKt4ir61innljuwapxhp75utpl0` (`store_id`),
  KEY `FKne6qr1kqcp03ogifxdwulfgpr` (`team_id`),
  KEY `FK3b63u30j6p6s06pcguhids2v8` (`template_id`),
  KEY `FKe8ew2ksbdtf7wp9we1r8brd3v` (`worker_id`),
  CONSTRAINT `FK1ut3obdkdy8xcu2dmkhlqsb4k` FOREIGN KEY (`generated_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK3b63u30j6p6s06pcguhids2v8` FOREIGN KEY (`template_id`) REFERENCES `db_document_template` (`template_id`),
  CONSTRAINT `FKe8ew2ksbdtf7wp9we1r8brd3v` FOREIGN KEY (`worker_id`) REFERENCES `db_worker` (`worker_id`),
  CONSTRAINT `FKelaj4k633qgjxiwogbwi37o9e` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKfv6plwk14iy7wgbi7waq1wfm3` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKlureq06l28764a039cb9ujkg3` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKne6qr1kqcp03ogifxdwulfgpr` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKrqfrs3e29muqlvv9j42pn01k2` FOREIGN KEY (`partner_id`) REFERENCES `db_partner` (`partner_id`),
  CONSTRAINT `FKt4ir61innljuwapxhp75utpl0` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_voucher_record_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_voucher_record_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_voucher_record`
--

LOCK TABLES `db_voucher_record` WRITE;
/*!40000 ALTER TABLE `db_voucher_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_voucher_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `db_worker`
--

DROP TABLE IF EXISTS `db_worker`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_worker` (
  `worker_id` bigint NOT NULL,
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
  `bank_account` varchar(64) DEFAULT NULL,
  `bank_name` varchar(64) DEFAULT NULL,
  `id_card_no` varchar(32) DEFAULT NULL,
  `name` varchar(64) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `status` enum('Active','Disabled') NOT NULL,
  `trade` varchar(32) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`worker_id`),
  KEY `idx_worker_company` (`com_id`,`worker_id`),
  KEY `idx_worker_company_id_card` (`com_id`,`id_card_no`),
  CONSTRAINT `FKq1yaxhpd4k9ay0t1ddp0mcob8` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_worker_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_worker_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_worker`
--

LOCK TABLES `db_worker` WRITE;
/*!40000 ALTER TABLE `db_worker` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_worker` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pack_line`
--

DROP TABLE IF EXISTS `pack_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_line` (
  `item_id` bigint NOT NULL,
  `brand` varchar(128) DEFAULT NULL,
  `deductible` bit(1) NOT NULL,
  `included` bit(1) NOT NULL,
  `name` varchar(128) NOT NULL,
  `pricing_method` enum('FixedTotal','ManualOnly','MaterialAndManual','MaterialOnly') NOT NULL,
  `quantity` varchar(256) DEFAULT NULL,
  `quantity_factor` decimal(12,3) NOT NULL,
  `sort` int NOT NULL,
  `spec` varchar(128) DEFAULT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') NOT NULL,
  `com_id` bigint NOT NULL,
  `catalog_item_id` bigint NOT NULL,
  `section_id` bigint NOT NULL,
  `template_id` bigint NOT NULL,
  PRIMARY KEY (`item_id`),
  KEY `FK8o35y73tmf4buvs68f7t8dcd6` (`com_id`),
  KEY `FKd6uii99mjq145o6x8ab9mnsd2` (`catalog_item_id`),
  KEY `FKfsimrhrkk5odk7pdhmqnjs4t0` (`section_id`),
  KEY `FKsyi0rxxn2wx4t0vw2yjtkgm4t` (`template_id`),
  CONSTRAINT `FK8o35y73tmf4buvs68f7t8dcd6` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKd6uii99mjq145o6x8ab9mnsd2` FOREIGN KEY (`catalog_item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FKfsimrhrkk5odk7pdhmqnjs4t0` FOREIGN KEY (`section_id`) REFERENCES `pack_section` (`group_id`),
  CONSTRAINT `FKsyi0rxxn2wx4t0vw2yjtkgm4t` FOREIGN KEY (`template_id`) REFERENCES `pack_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pack_line`
--

LOCK TABLES `pack_line` WRITE;
/*!40000 ALTER TABLE `pack_line` DISABLE KEYS */;
/*!40000 ALTER TABLE `pack_line` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pack_line_criterions`
--

DROP TABLE IF EXISTS `pack_line_criterions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_line_criterions` (
  `item_id` bigint NOT NULL,
  `criterions` varchar(256) DEFAULT NULL,
  KEY `FKo4rqhonde0pmtywgr0rrs85xc` (`item_id`),
  CONSTRAINT `FKo4rqhonde0pmtywgr0rrs85xc` FOREIGN KEY (`item_id`) REFERENCES `pack_line` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pack_line_criterions`
--

LOCK TABLES `pack_line_criterions` WRITE;
/*!40000 ALTER TABLE `pack_line_criterions` DISABLE KEYS */;
/*!40000 ALTER TABLE `pack_line_criterions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pack_section`
--

DROP TABLE IF EXISTS `pack_section`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_section` (
  `group_id` bigint NOT NULL,
  `max_delete_num` int DEFAULT NULL,
  `name` varchar(64) NOT NULL,
  `priority` int DEFAULT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`group_id`),
  KEY `FK9i381u80nx5eowv4me473ru6m` (`com_id`),
  CONSTRAINT `FK9i381u80nx5eowv4me473ru6m` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pack_section`
--

LOCK TABLES `pack_section` WRITE;
/*!40000 ALTER TABLE `pack_section` DISABLE KEYS */;
/*!40000 ALTER TABLE `pack_section` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pack_section_space_types`
--

DROP TABLE IF EXISTS `pack_section_space_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_section_space_types` (
  `package_section_group_id` bigint NOT NULL,
  `space_types` tinyint DEFAULT NULL,
  KEY `FKoe01btbd8yjit4j042vfquo39` (`package_section_group_id`),
  CONSTRAINT `FKoe01btbd8yjit4j042vfquo39` FOREIGN KEY (`package_section_group_id`) REFERENCES `pack_section` (`group_id`),
  CONSTRAINT `pack_section_space_types_chk_1` CHECK ((`space_types` between 0 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pack_section_space_types`
--

LOCK TABLES `pack_section_space_types` WRITE;
/*!40000 ALTER TABLE `pack_section_space_types` DISABLE KEYS */;
/*!40000 ALTER TABLE `pack_section_space_types` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pack_template`
--

DROP TABLE IF EXISTS `pack_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_template` (
  `template_id` bigint NOT NULL,
  `area` decimal(38,2) NOT NULL,
  `cost` decimal(10,2) NOT NULL,
  `discount` decimal(10,2) NOT NULL,
  `name` varchar(128) NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`template_id`),
  KEY `FKg2itassrj71n0931gsu5tg64m` (`com_id`),
  CONSTRAINT `FKg2itassrj71n0931gsu5tg64m` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pack_template`
--

LOCK TABLES `pack_template` WRITE;
/*!40000 ALTER TABLE `pack_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `pack_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pack_template_criterions`
--

DROP TABLE IF EXISTS `pack_template_criterions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_template_criterions` (
  `package_template_id` bigint NOT NULL,
  `criterions` varchar(256) DEFAULT NULL,
  KEY `FKtmxkl06swjqkb14qxuadbkqer` (`package_template_id`),
  CONSTRAINT `FKtmxkl06swjqkb14qxuadbkqer` FOREIGN KEY (`package_template_id`) REFERENCES `pack_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pack_template_criterions`
--

LOCK TABLES `pack_template_criterions` WRITE;
/*!40000 ALTER TABLE `pack_template_criterions` DISABLE KEYS */;
/*!40000 ALTER TABLE `pack_template_criterions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pack_template_item`
--

DROP TABLE IF EXISTS `pack_template_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_template_item` (
  `item_id` bigint NOT NULL,
  `name` varchar(128) NOT NULL,
  `priority` int DEFAULT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') NOT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`item_id`),
  KEY `FKhug3p43alyr5jy5g5orb1w4gd` (`com_id`),
  CONSTRAINT `FKhug3p43alyr5jy5g5orb1w4gd` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pack_template_item`
--

LOCK TABLES `pack_template_item` WRITE;
/*!40000 ALTER TABLE `pack_template_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `pack_template_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pack_template_item_space_types`
--

DROP TABLE IF EXISTS `pack_template_item_space_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_template_item_space_types` (
  `item_id` bigint NOT NULL,
  `space_types` enum('Balcony','Bathroom','Bedroom','DiningRoom','Kitchen','LivingRoom') DEFAULT NULL,
  KEY `FK5mwb2bxawtjjf0hiiyoxayih7` (`item_id`),
  CONSTRAINT `FK5mwb2bxawtjjf0hiiyoxayih7` FOREIGN KEY (`item_id`) REFERENCES `pack_template_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pack_template_item_space_types`
--

LOCK TABLES `pack_template_item_space_types` WRITE;
/*!40000 ALTER TABLE `pack_template_item_space_types` DISABLE KEYS */;
/*!40000 ALTER TABLE `pack_template_item_space_types` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pack_template_measures`
--

DROP TABLE IF EXISTS `pack_template_measures`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_template_measures` (
  `package_template_id` bigint NOT NULL,
  `price` decimal(38,2) DEFAULT NULL,
  `remark` tinytext,
  `standard` decimal(38,2) DEFAULT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`package_template_id`,`item_id`),
  KEY `FKnfna6rf5lv3w321u87pb6dhxh` (`item_id`),
  CONSTRAINT `FKk25ij2x2fry8jk2q9are8fr0q` FOREIGN KEY (`package_template_id`) REFERENCES `pack_template` (`template_id`),
  CONSTRAINT `FKnfna6rf5lv3w321u87pb6dhxh` FOREIGN KEY (`item_id`) REFERENCES `pack_template_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pack_template_measures`
--

LOCK TABLES `pack_template_measures` WRITE;
/*!40000 ALTER TABLE `pack_template_measures` DISABLE KEYS */;
/*!40000 ALTER TABLE `pack_template_measures` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quote`
--

DROP TABLE IF EXISTS `quote`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quote` (
  `quote_id` bigint NOT NULL,
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
  `accepted_at` datetime(6) DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL,
  `area` decimal(10,2) NOT NULL,
  `converted_at` datetime(6) DEFAULT NULL,
  `cost` decimal(12,2) NOT NULL,
  `cost_complete` bit(1) NOT NULL,
  `design_fee` decimal(12,2) NOT NULL,
  `discount` decimal(10,4) NOT NULL,
  `management_fee` decimal(12,2) NOT NULL,
  `mode` enum('Custom','Hybrid','Package') NOT NULL,
  `no` varchar(64) NOT NULL,
  `rejected_at` datetime(6) DEFAULT NULL,
  `status` enum('Accepted','Converted','Draft','Rejected','Submitted','Superseded') NOT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `subtotal` decimal(12,2) NOT NULL,
  `tax_amount` decimal(12,2) NOT NULL,
  `tax_rate` decimal(8,4) NOT NULL,
  `title` varchar(128) DEFAULT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  `emp_id` bigint DEFAULT NULL,
  `customer_id` bigint NOT NULL,
  `design_id` bigint DEFAULT NULL,
  `package_template_id` bigint DEFAULT NULL,
  `property_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`quote_id`),
  UNIQUE KEY `UK71x59tnwiyo6t6xvrrwudqnpv` (`no`),
  KEY `idx_quote_company` (`com_id`,`quote_id`),
  KEY `idx_quote_company_store` (`com_id`,`store_id`,`quote_id`),
  KEY `idx_quote_company_team` (`com_id`,`team_id`,`quote_id`),
  KEY `idx_quote_company_owner` (`com_id`,`emp_id`,`quote_id`),
  KEY `idx_quote_company_design` (`com_id`,`design_id`,`quote_id`),
  KEY `FK396n99c10jswj2lbuxbgoty97` (`emp_id`),
  KEY `FKmwtprlb0k3keaj5po8wcjg4wu` (`customer_id`),
  KEY `FKl54a8939aa4m78f6xv6tudn3u` (`design_id`),
  KEY `FK39rbm5bfkng5x1x35mkr3b40r` (`package_template_id`),
  KEY `FKo9iyesrv0nmheipcdw86unt4b` (`property_id`),
  KEY `FK5cttl47je6gonafweclcv59k3` (`store_id`),
  KEY `FK9jomxwlxfskjat4x3lx7ie6wq` (`team_id`),
  CONSTRAINT `FK396n99c10jswj2lbuxbgoty97` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK39rbm5bfkng5x1x35mkr3b40r` FOREIGN KEY (`package_template_id`) REFERENCES `pack_template` (`template_id`),
  CONSTRAINT `FK5cttl47je6gonafweclcv59k3` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FK9jomxwlxfskjat4x3lx7ie6wq` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKjaxxdkdo6uly3uskd2yqr92oy` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKl54a8939aa4m78f6xv6tudn3u` FOREIGN KEY (`design_id`) REFERENCES `db_design` (`design_id`),
  CONSTRAINT `FKmwtprlb0k3keaj5po8wcjg4wu` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKo9iyesrv0nmheipcdw86unt4b` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `quote_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `quote_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quote`
--

LOCK TABLES `quote` WRITE;
/*!40000 ALTER TABLE `quote` DISABLE KEYS */;
/*!40000 ALTER TABLE `quote` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quote_line`
--

DROP TABLE IF EXISTS `quote_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quote_line` (
  `item_id` bigint NOT NULL,
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
  `actual_cost` decimal(14,2) NOT NULL,
  `actual_labor_cost` decimal(14,2) NOT NULL,
  `actual_material_cost` decimal(14,2) NOT NULL,
  `actual_other_cost` decimal(14,2) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `brand` varchar(128) DEFAULT NULL,
  `category` varchar(64) DEFAULT NULL,
  `cost` decimal(12,2) NOT NULL,
  `difference_status` enum('Confirmed','Ignored','None','Pending') NOT NULL,
  `difference_type` enum('Deduct','DesignOnly','Ignored','Matched','None','PackageOnly','QuantityChanged','Replace','Upgrade') NOT NULL,
  `discount_amount` decimal(12,2) NOT NULL,
  `formula` varchar(256) DEFAULT NULL,
  `included` bit(1) NOT NULL,
  `manual_price` decimal(12,2) NOT NULL,
  `material_price` decimal(12,2) NOT NULL,
  `name` varchar(128) NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `pricing_mode` enum('FixedTotal','ManualOnly','MaterialAndManual','MaterialOnly') NOT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `role` enum('Add','Base','Deduct','Gift','Option','Replace','Upgrade') NOT NULL,
  `sort` int NOT NULL,
  `spec` varchar(128) DEFAULT NULL,
  `type` enum('Appliance','Construction','Custom','General','Material','Soft') NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') NOT NULL,
  `catalog_item_id` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `design_item_id` bigint DEFAULT NULL,
  `emp_id` bigint DEFAULT NULL,
  `package_line_id` bigint DEFAULT NULL,
  `property_id` bigint NOT NULL,
  `quote_id` bigint NOT NULL,
  `space_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  KEY `idx_quote_item_company` (`com_id`,`item_id`),
  KEY `idx_quote_item_company_store` (`com_id`,`store_id`,`item_id`),
  KEY `idx_quote_item_company_team` (`com_id`,`team_id`,`item_id`),
  KEY `idx_quote_item_company_owner` (`com_id`,`emp_id`,`item_id`),
  KEY `idx_quote_item_company_quote` (`com_id`,`quote_id`,`item_id`),
  KEY `idx_quote_item_company_space` (`com_id`,`space_id`,`item_id`),
  KEY `FKn440jmxw4pffgbhhpyd40kqu9` (`catalog_item_id`),
  KEY `FKt8qauikbw81m4enceh9qiwyl` (`design_item_id`),
  KEY `FKirvl3sij6pi4rotqgu00pdfix` (`emp_id`),
  KEY `FKcpme49dvcb448749oe8fn2m2a` (`package_line_id`),
  KEY `FK9pyosq9mw9aa0v5xphunm8e8j` (`property_id`),
  KEY `FKnbxr28wot0665o7o2fgy4h0xr` (`quote_id`),
  KEY `FK31tv9i139w12wmsv55c40ehjs` (`space_id`),
  KEY `FKgitqq1lrsodp3ytckma87gct2` (`store_id`),
  KEY `FKdq0dk88c4oybbdsjgljpitvsm` (`team_id`),
  CONSTRAINT `FK31tv9i139w12wmsv55c40ehjs` FOREIGN KEY (`space_id`) REFERENCES `db_design_space` (`space_id`),
  CONSTRAINT `FK9pyosq9mw9aa0v5xphunm8e8j` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKcpme49dvcb448749oe8fn2m2a` FOREIGN KEY (`package_line_id`) REFERENCES `pack_line` (`item_id`),
  CONSTRAINT `FKdq0dk88c4oybbdsjgljpitvsm` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKgitqq1lrsodp3ytckma87gct2` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKidrhh8twgwrjsrbdfwyyjhx3b` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKirvl3sij6pi4rotqgu00pdfix` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKn440jmxw4pffgbhhpyd40kqu9` FOREIGN KEY (`catalog_item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FKnbxr28wot0665o7o2fgy4h0xr` FOREIGN KEY (`quote_id`) REFERENCES `quote` (`quote_id`),
  CONSTRAINT `FKt8qauikbw81m4enceh9qiwyl` FOREIGN KEY (`design_item_id`) REFERENCES `db_design_item` (`item_id`),
  CONSTRAINT `quote_line_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `quote_line_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quote_line`
--

LOCK TABLES `quote_line` WRITE;
/*!40000 ALTER TABLE `quote_line` DISABLE KEYS */;
/*!40000 ALTER TABLE `quote_line` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quote_line_components`
--

DROP TABLE IF EXISTS `quote_line_components`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quote_line_components` (
  `line_id` bigint NOT NULL,
  `cost_amount` decimal(12,2) NOT NULL,
  `formula` varchar(256) DEFAULT NULL,
  `component_name` varchar(128) DEFAULT NULL,
  `sale_amount` decimal(12,2) NOT NULL,
  `sort` int NOT NULL,
  `component_type` enum('Auxiliary','Freight','Installation','Labor','Machine','Material','Other','Production','Service','Tax') NOT NULL,
  KEY `FK2dvwwoor77ihre5kd69hxh7sh` (`line_id`),
  CONSTRAINT `FK2dvwwoor77ihre5kd69hxh7sh` FOREIGN KEY (`line_id`) REFERENCES `quote_line` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quote_line_components`
--

LOCK TABLES `quote_line_components` WRITE;
/*!40000 ALTER TABLE `quote_line_components` DISABLE KEYS */;
/*!40000 ALTER TABLE `quote_line_components` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'decoboon'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-23  5:02:38
