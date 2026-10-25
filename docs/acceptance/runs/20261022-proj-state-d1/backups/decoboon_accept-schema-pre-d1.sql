-- MySQL dump 10.13  Distrib 8.4.0, for macos13.2 (arm64)
--
-- Host: 127.0.0.1    Database: decoboon_accept
-- ------------------------------------------------------
-- Server version	8.4.0

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
-- Table structure for table `carryover_adjustment`
--

DROP TABLE IF EXISTS `carryover_adjustment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carryover_adjustment` (
  `adjustment_id` bigint NOT NULL,
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
  `after_amount` decimal(14,2) DEFAULT NULL,
  `before_amount` decimal(14,2) DEFAULT NULL,
  `delta_amount` decimal(14,2) NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('Cost','Other','Payable','Receivable') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `created_by` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`adjustment_id`),
  KEY `idx_carryover_adjustment_item` (`com_id`,`item_id`,`adjustment_id`),
  KEY `FKhynb5906cxakd6h5pm5jife5k` (`created_by`),
  KEY `FKgt4o4bd5vigaxf7exviokgy74` (`item_id`),
  CONSTRAINT `FKai6kgxcd7hhh0c82jnc4bm6vf` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKgt4o4bd5vigaxf7exviokgy74` FOREIGN KEY (`item_id`) REFERENCES `carryover_project` (`item_id`),
  CONSTRAINT `FKhynb5906cxakd6h5pm5jife5k` FOREIGN KEY (`created_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `carryover_adjustment_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `carryover_adjustment_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carryover_attachment`
--

DROP TABLE IF EXISTS `carryover_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carryover_attachment` (
  `attachment_id` bigint NOT NULL,
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
  `category` enum('Contract','Finance','Other','Quote','Site') COLLATE utf8mb4_general_ci NOT NULL,
  `file_url` varchar(512) COLLATE utf8mb4_general_ci NOT NULL,
  `original_name` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `uploaded_by` bigint NOT NULL,
  PRIMARY KEY (`attachment_id`),
  KEY `idx_carryover_attachment_item` (`com_id`,`item_id`,`attachment_id`),
  KEY `FKjd4jgy5vuipgkgp5odwsxngxc` (`item_id`),
  KEY `FK8x55wdi549evlwf3u0cr3lp0u` (`uploaded_by`),
  CONSTRAINT `FK8x55wdi549evlwf3u0cr3lp0u` FOREIGN KEY (`uploaded_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKjd4jgy5vuipgkgp5odwsxngxc` FOREIGN KEY (`item_id`) REFERENCES `carryover_project` (`item_id`),
  CONSTRAINT `FKob1yw8xyqq1a2cj73aq07riss` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `carryover_attachment_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `carryover_attachment_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carryover_batch`
--

DROP TABLE IF EXISTS `carryover_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carryover_batch` (
  `batch_id` bigint NOT NULL,
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
  `as_of_date` date NOT NULL,
  `bookkeeping_survey_note` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `scope_confirmed_at` datetime(6) DEFAULT NULL,
  `status` enum('Active','Cancelled','Completed') COLLATE utf8mb4_general_ci NOT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  `created_by` bigint NOT NULL,
  PRIMARY KEY (`batch_id`),
  KEY `idx_carryover_batch_company` (`com_id`,`batch_id`),
  KEY `idx_carryover_batch_company_status` (`com_id`,`status`,`batch_id`),
  KEY `FK51w4lkxx6y0en6bjm8myy8lp4` (`created_by`),
  CONSTRAINT `FK51w4lkxx6y0en6bjm8myy8lp4` FOREIGN KEY (`created_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKlu77j46x2dki5qnpa53dumfqb` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `carryover_batch_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `carryover_batch_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carryover_confirmation`
--

DROP TABLE IF EXISTS `carryover_confirmation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carryover_confirmation` (
  `confirmation_id` bigint NOT NULL,
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
  `confirmed_at` datetime(6) NOT NULL,
  `confirmed_version` int NOT NULL,
  `proxy` bit(1) NOT NULL,
  `required_role` enum('Boss','Designer','Finance','Pm') COLLATE utf8mb4_general_ci NOT NULL,
  `summary` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `confirmed_by` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`confirmation_id`),
  UNIQUE KEY `uk_carryover_confirmation_role_version` (`com_id`,`item_id`,`required_role`,`confirmed_version`),
  KEY `idx_carryover_confirmation_item` (`com_id`,`item_id`,`required_role`),
  KEY `FKo64er8df2gllki05ba3ftkrx3` (`confirmed_by`),
  KEY `FKbi0iiecamcf17g4twhakdc9pb` (`item_id`),
  CONSTRAINT `FK7lhhbkhaqmrfs3d9cdyup67us` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKbi0iiecamcf17g4twhakdc9pb` FOREIGN KEY (`item_id`) REFERENCES `carryover_project` (`item_id`),
  CONSTRAINT `FKo64er8df2gllki05ba3ftkrx3` FOREIGN KEY (`confirmed_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `carryover_confirmation_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `carryover_confirmation_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carryover_cost_snapshot`
--

DROP TABLE IF EXISTS `carryover_cost_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carryover_cost_snapshot` (
  `snapshot_id` bigint NOT NULL,
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
  `as_of_date` date NOT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `cost_status` enum('Complete','TotalOnly','Unknown') COLLATE utf8mb4_general_ci NOT NULL,
  `missing_note` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `total_cost` decimal(14,2) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `confirmed_by` bigint DEFAULT NULL,
  `item_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`snapshot_id`),
  UNIQUE KEY `uk_carryover_cost_item` (`item_id`),
  UNIQUE KEY `uk_carryover_cost_project` (`project_id`),
  KEY `idx_carryover_cost_item` (`com_id`,`item_id`),
  KEY `FKqne8qh1hq3bw5tljtey2vpu6y` (`confirmed_by`),
  CONSTRAINT `FKlcc2lgdugin2e2b506xlqs3fs` FOREIGN KEY (`item_id`) REFERENCES `carryover_project` (`item_id`),
  CONSTRAINT `FKqne8qh1hq3bw5tljtey2vpu6y` FOREIGN KEY (`confirmed_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKr95tvvd68s72jwjda4gsmfwy0` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKsc8893aunax1cvw92gftxmono` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `carryover_cost_snapshot_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `carryover_cost_snapshot_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carryover_payment_plan`
--

DROP TABLE IF EXISTS `carryover_payment_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carryover_payment_plan` (
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
  `amount` decimal(14,2) NOT NULL,
  `counterparty` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `direction` enum('Payable','Receivable') COLLATE utf8mb4_general_ci NOT NULL,
  `planned_date` date DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sort` int NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint DEFAULT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`plan_id`),
  KEY `idx_carryover_payment_item` (`com_id`,`item_id`,`direction`,`plan_id`),
  KEY `idx_carryover_payment_company` (`com_id`,`plan_id`),
  KEY `FKbn3u4yml022i56v459n4nwnqx` (`creator_id`),
  KEY `FKc84nffx9jdt4hxeci8xdfc8ar` (`item_id`),
  CONSTRAINT `FKbn3u4yml022i56v459n4nwnqx` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKc84nffx9jdt4hxeci8xdfc8ar` FOREIGN KEY (`item_id`) REFERENCES `carryover_project` (`item_id`),
  CONSTRAINT `FKtr92mbnt5jd7wu0cnhyf0x12m` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `carryover_payment_plan_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `carryover_payment_plan_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carryover_plan_change`
--

DROP TABLE IF EXISTS `carryover_plan_change`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carryover_plan_change` (
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
  `action` enum('Confirm','StageCreate','StageDelete','StageReorder','StageUpdate') COLLATE utf8mb4_general_ci NOT NULL,
  `after_snapshot` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `before_snapshot` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `stage_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `created_by` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `stage_id` bigint DEFAULT NULL,
  PRIMARY KEY (`change_id`),
  KEY `idx_carryover_plan_change_item` (`com_id`,`item_id`,`change_id`),
  KEY `idx_carryover_plan_change_project` (`com_id`,`project_id`,`change_id`),
  KEY `FK92dah9obcuwqfe1wersr3pi79` (`created_by`),
  KEY `FKgmmopp2ylv2higqciyk9qya3f` (`item_id`),
  KEY `FKpx2yjnpgext441kfonp7t7i8j` (`project_id`),
  KEY `FKpcm27ev90hpdlx4623ij29vho` (`stage_id`),
  CONSTRAINT `FK92dah9obcuwqfe1wersr3pi79` FOREIGN KEY (`created_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKgmmopp2ylv2higqciyk9qya3f` FOREIGN KEY (`item_id`) REFERENCES `carryover_project` (`item_id`),
  CONSTRAINT `FKpcm27ev90hpdlx4623ij29vho` FOREIGN KEY (`stage_id`) REFERENCES `db_project_stage` (`stage_id`),
  CONSTRAINT `FKpx2yjnpgext441kfonp7t7i8j` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKqhbtfn1w2dog2q1hs15gxa8jm` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `carryover_plan_change_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `carryover_plan_change_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carryover_plan_confirmation`
--

DROP TABLE IF EXISTS `carryover_plan_confirmation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carryover_plan_confirmation` (
  `confirmation_id` bigint NOT NULL,
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
  `confirmed_at` datetime(6) NOT NULL,
  `plan_version` int NOT NULL,
  `proxy` bit(1) NOT NULL,
  `required_role` enum('Boss','Designer','Finance','Pm') COLLATE utf8mb4_general_ci NOT NULL,
  `summary` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `confirmed_by` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`confirmation_id`),
  UNIQUE KEY `uk_carryover_plan_confirmation_version` (`com_id`,`item_id`,`plan_version`),
  KEY `idx_carryover_plan_confirmation_item` (`com_id`,`item_id`,`plan_version`),
  KEY `FKdk71k47dowgcumecc9aeaqe8w` (`confirmed_by`),
  KEY `FKducuh9g77a5pn8v5glxxumdep` (`item_id`),
  KEY `FKohonol8dahufev4s6ry94sooe` (`project_id`),
  CONSTRAINT `FKdk71k47dowgcumecc9aeaqe8w` FOREIGN KEY (`confirmed_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKducuh9g77a5pn8v5glxxumdep` FOREIGN KEY (`item_id`) REFERENCES `carryover_project` (`item_id`),
  CONSTRAINT `FKohonol8dahufev4s6ry94sooe` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKs7qliqvocq42nuce03g0mrwrx` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `carryover_plan_confirmation_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `carryover_plan_confirmation_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carryover_project`
--

DROP TABLE IF EXISTS `carryover_project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carryover_project` (
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
  `address` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `applied_at` datetime(6) DEFAULT NULL,
  `area` decimal(10,2) DEFAULT NULL,
  `cancel_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `community` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `contract_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cost_missing_note` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cost_status` enum('Complete','TotalOnly','Unknown') COLLATE utf8mb4_general_ci NOT NULL,
  `cost_total` decimal(14,2) DEFAULT NULL,
  `current_amount` decimal(14,2) DEFAULT NULL,
  `current_issue` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `current_stage_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `customer_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `customer_phone` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `first_business_at` datetime(6) DEFAULT NULL,
  `first_business_type` enum('Acceptance','ContractChange','FinanceAdjustment','GoodsReceipt','Other','Payment','Purchase','Receipt','SiteReport','Stage','Task') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `legacy_account_note` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `legacy_id` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `legacy_project_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `locked_at` datetime(6) DEFAULT NULL,
  `payable_confirmed` bit(1) NOT NULL,
  `payable_due_date` date DEFAULT NULL,
  `payable_outstanding` decimal(14,2) DEFAULT NULL,
  `payable_payee_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `payable_source_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `plan_confirmed_at` datetime(6) DEFAULT NULL,
  `plan_end_date` date DEFAULT NULL,
  `plan_status` enum('Confirmed','Draft','Locked','NotStarted') COLLATE utf8mb4_general_ci NOT NULL,
  `plan_version` int NOT NULL,
  `progress` int NOT NULL,
  `project_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `project_status` enum('Accepting','Finished','Ongoing','Preparing','Settled','Suspended') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `receivable_confirmed` bit(1) NOT NULL,
  `receivable_due_date` date DEFAULT NULL,
  `receivable_outstanding` decimal(14,2) DEFAULT NULL,
  `receivable_source_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remaining_work` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `signed_amount` decimal(14,2) DEFAULT NULL,
  `signed_at` date DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `status` enum('Applied','Cancelled','Confirmed','Draft','Locked','Ready') COLLATE utf8mb4_general_ci NOT NULL,
  `version` int NOT NULL,
  `batch_id` bigint NOT NULL,
  `com_id` bigint NOT NULL,
  `contract_id` bigint DEFAULT NULL,
  `designer_id` bigint DEFAULT NULL,
  `locked_by` bigint DEFAULT NULL,
  `manager_id` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `payable_id` bigint DEFAULT NULL,
  `plan_confirmed_by` bigint DEFAULT NULL,
  `project_id` bigint DEFAULT NULL,
  `quote_id` bigint DEFAULT NULL,
  `receivable_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `UKgf1bf3n16ap997d1er5w6q99` (`contract_id`),
  UNIQUE KEY `UKofx41t8t9lt5auey2yxivwjdu` (`payable_id`),
  UNIQUE KEY `UKm3ppwdfwbyibtot0h8hvn9tis` (`project_id`),
  UNIQUE KEY `UK97qbkyaqf5iyri3ba4dugnmhk` (`quote_id`),
  UNIQUE KEY `UKdrj3rnump2ijr9b7ii4d6ut4j` (`receivable_id`),
  KEY `idx_carryover_project_company` (`com_id`,`item_id`),
  KEY `idx_carryover_project_batch` (`com_id`,`batch_id`,`item_id`),
  KEY `idx_carryover_project_status` (`com_id`,`status`,`item_id`),
  KEY `idx_carryover_project_store` (`com_id`,`store_id`,`item_id`),
  KEY `idx_carryover_project_team` (`com_id`,`team_id`,`item_id`),
  KEY `idx_carryover_project_owner` (`com_id`,`owner_id`,`item_id`),
  KEY `FK2n7mwt8qxu7ghgl7pkotpn0jy` (`batch_id`),
  KEY `FKb75lld5kc9dqaecubfhj6kwkt` (`designer_id`),
  KEY `FK7qkt7tu37xs593pfhp1mqxxd3` (`locked_by`),
  KEY `FK982dsngslg4hj7hmiweb9v8mn` (`manager_id`),
  KEY `FKd6h02ekshsu51arpseem7lxoa` (`owner_id`),
  KEY `FKreiltv9tnvj70817ex0bjm8l2` (`plan_confirmed_by`),
  KEY `FKnltiswqku7a93whadcfxykl13` (`store_id`),
  KEY `FKas854752ddth0fhwjv132a6pq` (`team_id`),
  CONSTRAINT `FK2n7mwt8qxu7ghgl7pkotpn0jy` FOREIGN KEY (`batch_id`) REFERENCES `carryover_batch` (`batch_id`),
  CONSTRAINT `FK7qkt7tu37xs593pfhp1mqxxd3` FOREIGN KEY (`locked_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK90km5ndvuo87kmexbp0ukryqy` FOREIGN KEY (`contract_id`) REFERENCES `db_project_contract` (`contract_id`),
  CONSTRAINT `FK982dsngslg4hj7hmiweb9v8mn` FOREIGN KEY (`manager_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKas854752ddth0fhwjv132a6pq` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKb75lld5kc9dqaecubfhj6kwkt` FOREIGN KEY (`designer_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKbejqeu4yc6ys0v15uqcik12lp` FOREIGN KEY (`quote_id`) REFERENCES `quote` (`quote_id`),
  CONSTRAINT `FKd6h02ekshsu51arpseem7lxoa` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKi2ey6cjx6ae04xqmqaq1st0jw` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKj2l41d2hbij0f21vser0jcq0g` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKnltiswqku7a93whadcfxykl13` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKo9hd7cd8vfjdn9dnhx9cjvdtm` FOREIGN KEY (`receivable_id`) REFERENCES `db_finance_receivable` (`receivable_id`),
  CONSTRAINT `FKqxbeordmer36vmatmjf572itb` FOREIGN KEY (`payable_id`) REFERENCES `db_finance_payable` (`payable_id`),
  CONSTRAINT `FKreiltv9tnvj70817ex0bjm8l2` FOREIGN KEY (`plan_confirmed_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `carryover_project_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `carryover_project_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carryover_reconciliation`
--

DROP TABLE IF EXISTS `carryover_reconciliation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carryover_reconciliation` (
  `reconciliation_id` bigint NOT NULL,
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
  `as_of_date` date NOT NULL,
  `closed_at` datetime(6) DEFAULT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `period_end` date NOT NULL,
  `period_start` date NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Closed','Confirmed','DiffHandling','Draft','Pending') COLLATE utf8mb4_general_ci NOT NULL,
  `batch_id` bigint NOT NULL,
  `com_id` bigint NOT NULL,
  `confirmed_by` bigint DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  PRIMARY KEY (`reconciliation_id`),
  KEY `idx_carryover_recon_company` (`com_id`,`reconciliation_id`),
  KEY `idx_carryover_recon_batch` (`com_id`,`batch_id`,`reconciliation_id`),
  KEY `FKkyjp7mlp46p8j72utkldss4j0` (`batch_id`),
  KEY `FK36wobfkdmad6vh2h4b8o6gq14` (`confirmed_by`),
  KEY `FKi6gtw9u9i1ax20cvail489dkl` (`created_by`),
  KEY `FKmb1ojk16d40ivum34l6swg33m` (`owner_id`),
  CONSTRAINT `FK36wobfkdmad6vh2h4b8o6gq14` FOREIGN KEY (`confirmed_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKgyckqpgeynjyrrewwanjq084q` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKi6gtw9u9i1ax20cvail489dkl` FOREIGN KEY (`created_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKkyjp7mlp46p8j72utkldss4j0` FOREIGN KEY (`batch_id`) REFERENCES `carryover_batch` (`batch_id`),
  CONSTRAINT `FKmb1ojk16d40ivum34l6swg33m` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `carryover_reconciliation_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `carryover_reconciliation_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carryover_reconciliation_attachment`
--

DROP TABLE IF EXISTS `carryover_reconciliation_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carryover_reconciliation_attachment` (
  `attachment_id` bigint NOT NULL,
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
  `category` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `file_url` varchar(1000) COLLATE utf8mb4_general_ci NOT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `reconciliation_id` bigint NOT NULL,
  `uploaded_by` bigint DEFAULT NULL,
  PRIMARY KEY (`attachment_id`),
  KEY `idx_carryover_recon_attachment` (`com_id`,`reconciliation_id`,`attachment_id`),
  KEY `FKhqlv7g76n324ow10ab1hiaa27` (`reconciliation_id`),
  KEY `FK9n7elk6924ypr7tb2peb14y6l` (`uploaded_by`),
  CONSTRAINT `FK8q5lm7bp3jrtg60182ay8whgt` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK9n7elk6924ypr7tb2peb14y6l` FOREIGN KEY (`uploaded_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKhqlv7g76n324ow10ab1hiaa27` FOREIGN KEY (`reconciliation_id`) REFERENCES `carryover_reconciliation` (`reconciliation_id`),
  CONSTRAINT `carryover_reconciliation_attachment_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `carryover_reconciliation_attachment_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carryover_reconciliation_item`
--

DROP TABLE IF EXISTS `carryover_reconciliation_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carryover_reconciliation_item` (
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
  `action` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `difference` decimal(14,2) DEFAULT NULL,
  `difference_status` enum('Accepted','Handling','Pending','Resolved') COLLATE utf8mb4_general_ci NOT NULL,
  `difference_type` enum('Entity','MissingData','Scope','Tax','Time') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `original_amount` decimal(14,2) DEFAULT NULL,
  `original_confirmed` bit(1) NOT NULL,
  `original_source` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `system_amount` decimal(14,2) NOT NULL,
  `theme` enum('Cash','Cost','Payable','Receivable') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `reconciliation_id` bigint NOT NULL,
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_carryover_recon_theme` (`reconciliation_id`,`theme`),
  KEY `idx_carryover_recon_item_company` (`com_id`,`reconciliation_id`,`theme`),
  KEY `idx_carryover_recon_item_owner` (`com_id`,`owner_id`,`item_id`),
  KEY `FK7vg41cokreyo3lp78wueta279` (`owner_id`),
  CONSTRAINT `FK7vg41cokreyo3lp78wueta279` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKp5osu7r1koexhihg7otdb0rhw` FOREIGN KEY (`reconciliation_id`) REFERENCES `carryover_reconciliation` (`reconciliation_id`),
  CONSTRAINT `FKp722v7hr299ryn8tcvxnnafbj` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `carryover_reconciliation_item_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `carryover_reconciliation_item_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `catalog_appliance_detail`
--

DROP TABLE IF EXISTS `catalog_appliance_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_appliance_detail` (
  `detail_id` bigint NOT NULL,
  `after_sales` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `dimensions` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `model` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `power` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `utility_requirement` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `warranty_months` int NOT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `UKewj4fdoqatc239p2t0aah30di` (`item_id`),
  KEY `FKn22phhchyofta5olg3fx26fdq` (`com_id`),
  CONSTRAINT `FKn22phhchyofta5olg3fx26fdq` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKqbw14gsneddje3evnqf2c6yx3` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `catalog_construction_detail`
--

DROP TABLE IF EXISTS `catalog_construction_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_construction_detail` (
  `detail_id` bigint NOT NULL,
  `concealed` bit(1) NOT NULL,
  `craft` longtext COLLATE utf8mb4_general_ci,
  `criterion` longtext COLLATE utf8mb4_general_ci,
  `duration_days` int NOT NULL,
  `measurement_formula` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `stage` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `UK7o9o601fow4ln42w68hfi7ot0` (`item_id`),
  KEY `FKsqgli60rqokvs6j7vovfogu2q` (`com_id`),
  CONSTRAINT `FKq4d0ytj3gwutwl66w97vpoolc` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FKsqgli60rqokvs6j7vovfogu2q` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `catalog_custom_detail`
--

DROP TABLE IF EXISTS `catalog_custom_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_custom_detail` (
  `detail_id` bigint NOT NULL,
  `board` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cabinet_type` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `countertop` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `door` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `hardware` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `parametric_formula` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `UK6kj9bsggt8xncmjf0p8ht9ltq` (`item_id`),
  KEY `FKsch3gm33d2sjstf0iypsg6bli` (`com_id`),
  CONSTRAINT `FK1lx5t6kuut7r0ytqb6npcajyy` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FKsch3gm33d2sjstf0iypsg6bli` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `catalog_general_detail`
--

DROP TABLE IF EXISTS `catalog_general_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_general_detail` (
  `detail_id` bigint NOT NULL,
  `allocation_mode` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `approval_required` bit(1) NOT NULL,
  `discountable` bit(1) NOT NULL,
  `fee_type` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `invoice_type` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `UK5sb2uui6f4epfvb3s4g77lc0g` (`item_id`),
  KEY `FKddt86meyxhes3lj12qg11s8xf` (`com_id`),
  CONSTRAINT `FKddt86meyxhes3lj12qg11s8xf` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKqllseilpr2d8kft2ey5vcogtr` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `catalog_import_batch`
--

DROP TABLE IF EXISTS `catalog_import_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_import_batch` (
  `batch_id` bigint NOT NULL,
  `batch_no` varchar(48) COLLATE utf8mb4_general_ci NOT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `content_type` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cost_pending_rows` int NOT NULL,
  `duplicate_policy` varchar(16) COLLATE utf8mb4_general_ci NOT NULL,
  `duplicate_rows` int NOT NULL,
  `error_rows` int NOT NULL,
  `expire_at` datetime(6) DEFAULT NULL,
  `file_sha256` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `finished_at` datetime(6) DEFAULT NULL,
  `imported_rows` int NOT NULL,
  `message` longtext COLLATE utf8mb4_general_ci,
  `original_filename` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `parsed_at` datetime(6) DEFAULT NULL,
  `partial_import` bit(1) NOT NULL,
  `ready_rows` int NOT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `skipped_rows` int NOT NULL,
  `source_path` varchar(512) COLLATE utf8mb4_general_ci NOT NULL,
  `status` enum('Expired','Failed','Imported','Importing','Parsed','PartiallyImported','PendingConfirm','Revoked','Uploaded') COLLATE utf8mb4_general_ci NOT NULL,
  `suspected_duplicate_rows` int NOT NULL,
  `template_version` varchar(24) COLLATE utf8mb4_general_ci NOT NULL,
  `total_rows` int NOT NULL,
  `validation_version` varchar(24) COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `created_by` bigint NOT NULL,
  PRIMARY KEY (`batch_id`),
  KEY `idx_catalog_import_batch_company` (`com_id`,`batch_id`),
  KEY `idx_catalog_import_batch_company_status` (`com_id`,`status`,`batch_id`),
  KEY `idx_catalog_import_batch_company_file` (`com_id`,`file_sha256`,`batch_id`),
  KEY `FKo1t3w24q8qll1dgsw6kc7bvv0` (`created_by`),
  CONSTRAINT `FK3wfq5aivxq5qiwqi57bbjwf1r` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKo1t3w24q8qll1dgsw6kc7bvv0` FOREIGN KEY (`created_by`) REFERENCES `db_tenant_employee` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `catalog_import_row`
--

DROP TABLE IF EXISTS `catalog_import_row`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_import_row` (
  `row_id` bigint NOT NULL,
  `cost_pending` bit(1) DEFAULT NULL,
  `error_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `error_field` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `error_reason` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `item_name` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `item_type` enum('Appliance','Construction','Custom','General','Material','Soft') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `pricing_mode` enum('FixedTotal','ManualOnly','MaterialAndManual','MaterialOnly') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `raw_data` longtext COLLATE utf8mb4_general_ci,
  `row_no` int NOT NULL,
  `sale_amount` decimal(12,2) DEFAULT NULL,
  `status` enum('Duplicate','Error','Imported','Ready','Skipped','SuspectedDuplicate') COLLATE utf8mb4_general_ci NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `batch_id` bigint NOT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint DEFAULT NULL,
  PRIMARY KEY (`row_id`),
  UNIQUE KEY `uk_catalog_import_row_batch_no` (`com_id`,`batch_id`,`row_no`),
  KEY `idx_catalog_import_row_company` (`com_id`,`row_id`),
  KEY `idx_catalog_import_row_batch_status` (`com_id`,`batch_id`,`status`,`row_no`),
  KEY `idx_catalog_import_row_item` (`com_id`,`item_id`),
  KEY `FK9iecnj676icwo3xqky6hg95mh` (`batch_id`),
  KEY `FKjj9dof2xsqg633988tr98nva6` (`item_id`),
  CONSTRAINT `FK9iecnj676icwo3xqky6hg95mh` FOREIGN KEY (`batch_id`) REFERENCES `catalog_import_batch` (`batch_id`),
  CONSTRAINT `FKgo4et6oc96vidnljortownw5e` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKjj9dof2xsqg633988tr98nva6` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `catalog_item`
--

DROP TABLE IF EXISTS `catalog_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_item` (
  `item_id` bigint NOT NULL,
  `active` bit(1) NOT NULL,
  `brand` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `category` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cost_price` decimal(12,2) DEFAULT NULL,
  `craft` longtext COLLATE utf8mb4_general_ci,
  `criterion` longtext COLLATE utf8mb4_general_ci,
  `description` longtext COLLATE utf8mb4_general_ci,
  `fixed_total_price` decimal(12,2) DEFAULT NULL,
  `formula` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `manual_price` decimal(12,2) NOT NULL,
  `material_price` decimal(12,2) NOT NULL,
  `name` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `name_initials` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `pricing_mode` enum('FixedTotal','ManualOnly','MaterialAndManual','MaterialOnly') COLLATE utf8mb4_general_ci NOT NULL,
  `series` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sort` int NOT NULL,
  `source_type` enum('Import','Manual','Migration') COLLATE utf8mb4_general_ci NOT NULL,
  `spec` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` enum('Appliance','Construction','Custom','General','Material','Soft') COLLATE utf8mb4_general_ci NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `import_batch_id` bigint DEFAULT NULL,
  `maintainer_id` bigint DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  KEY `idx_catalog_company` (`com_id`,`item_id`),
  KEY `idx_catalog_company_type_category` (`com_id`,`type`,`category`,`item_id`),
  KEY `idx_catalog_company_type_brand` (`com_id`,`type`,`category`,`brand`,`item_id`),
  KEY `idx_catalog_company_maintainer` (`com_id`,`maintainer_id`,`item_id`),
  KEY `FKc4i4u8ro0ofuqrt471pb9f5s9` (`import_batch_id`),
  KEY `FKol4l5c0yrnx8g2355jbl1w9w6` (`maintainer_id`),
  CONSTRAINT `FKc4i4u8ro0ofuqrt471pb9f5s9` FOREIGN KEY (`import_batch_id`) REFERENCES `catalog_import_batch` (`batch_id`),
  CONSTRAINT `FKjymracy97jde5o1j04hlopwtg` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKol4l5c0yrnx8g2355jbl1w9w6` FOREIGN KEY (`maintainer_id`) REFERENCES `db_tenant_employee` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `catalog_item_images`
--

DROP TABLE IF EXISTS `catalog_item_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_item_images` (
  `item_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKsfne1wa1nmwlaew2m4nsyh1bu` (`item_id`),
  CONSTRAINT `FKsfne1wa1nmwlaew2m4nsyh1bu` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `catalog_material_detail`
--

DROP TABLE IF EXISTS `catalog_material_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_material_detail` (
  `detail_id` bigint NOT NULL,
  `color` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `inventory_mode` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `loss_rate` decimal(10,4) NOT NULL,
  `material` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `minimum_order` decimal(12,3) NOT NULL,
  `model` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `UKol21wivlioy3jhdq6xn3jhrsh` (`item_id`),
  KEY `FK20ywa3pr760u17hik2mmvcxdn` (`com_id`),
  CONSTRAINT `FK20ywa3pr760u17hik2mmvcxdn` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKdt1f6vweepmvklsitj4p05tef` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `catalog_soft_detail`
--

DROP TABLE IF EXISTS `catalog_soft_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_soft_detail` (
  `detail_id` bigint NOT NULL,
  `composition` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `dimensions` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `installation_mode` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `lead_time_days` int NOT NULL,
  `material` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `style` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `UKlnc25358i78ivsd0ev51f9o3s` (`item_id`),
  KEY `FK5yyrpuy4iiif0pbqpvrwi9ffi` (`com_id`),
  CONSTRAINT `FK15a78rdx82fb55mmtc470mlf` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FK5yyrpuy4iiif0pbqpvrwi9ffi` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `addr` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `area` decimal(10,2) DEFAULT NULL,
  `budget` decimal(12,2) DEFAULT NULL,
  `community` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `dormant_at` datetime(6) DEFAULT NULL,
  `dormant_reason` enum('BudgetUndecided','ContactUnreachable','CustomerRequestedPause','DecisionPending','LeadPendingVerification','Other','PlanPostponed') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `dormant_reason_text` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `gender` enum('F','M') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `handed_off_at` datetime(6) DEFAULT NULL,
  `last_follow_at` datetime(6) DEFAULT NULL,
  `last_visit_at` datetime(6) DEFAULT NULL,
  `measure_at` datetime(6) DEFAULT NULL,
  `measure_requested_at` datetime(6) DEFAULT NULL,
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `next_follow_at` datetime(6) DEFAULT NULL,
  `pending_measure_at` datetime(6) DEFAULT NULL,
  `remark` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `restored_at` datetime(6) DEFAULT NULL,
  `source` enum('BatchPhone','Lead','OldCustomer','Other','Referral','Video','WalkIn') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Dormant','Finished','Following','Lost','New','PendingMeasure','Quoted','Serving','Signed') COLLATE utf8mb4_general_ci NOT NULL,
  `status_before_dormant` enum('Dormant','Finished','Following','Lost','New','PendingMeasure','Quoted','Serving','Signed') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tel_no` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `visit_count` int NOT NULL DEFAULT '0',
  `wechat_added` bit(1) NOT NULL,
  `wechat_added_at` datetime(6) DEFAULT NULL,
  `wechat_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `designer_id` bigint DEFAULT NULL,
  `dormant_by` bigint DEFAULT NULL,
  `emp_id` bigint DEFAULT NULL,
  `restored_by` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`cust_id`),
  KEY `idx_cust_customer_company` (`com_id`,`cust_id`),
  KEY `idx_cust_customer_company_store` (`com_id`,`store_id`,`cust_id`),
  KEY `idx_cust_customer_company_team` (`com_id`,`team_id`,`cust_id`),
  KEY `idx_cust_customer_company_owner` (`com_id`,`emp_id`,`cust_id`),
  KEY `idx_cust_customer_company_designer` (`com_id`,`designer_id`,`cust_id`),
  KEY `FKmtqer08cgq4uaqffagylo7tga` (`designer_id`),
  KEY `FK3leby0iwa9vlmv1mxrpgnwy8p` (`dormant_by`),
  KEY `FKeekkqipy5oxjk35het9tnf6jq` (`emp_id`),
  KEY `FKjn4ug1r9rhg4hkvpt51e4yf7h` (`restored_by`),
  KEY `FKjih66fbtm8oypu0k21t4ol2dl` (`store_id`),
  KEY `FKcuioiekid2onbjlxksrg9cudp` (`team_id`),
  CONSTRAINT `FK3leby0iwa9vlmv1mxrpgnwy8p` FOREIGN KEY (`dormant_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKcuioiekid2onbjlxksrg9cudp` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKeekkqipy5oxjk35het9tnf6jq` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKjih66fbtm8oypu0k21t4ol2dl` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKjn4ug1r9rhg4hkvpt51e4yf7h` FOREIGN KEY (`restored_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKmtqer08cgq4uaqffagylo7tga` FOREIGN KEY (`designer_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKpajh0po9gteo33b0obuqjhh84` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `cust_customer_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `cust_customer_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `display_name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `granted_at` datetime(6) DEFAULT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `status` enum('Active','Revoked') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cust_customer_feedback`
--

DROP TABLE IF EXISTS `cust_customer_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_customer_feedback` (
  `feedback_id` bigint NOT NULL,
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
  `closed_at` datetime(6) DEFAULT NULL,
  `content` varchar(2048) COLLATE utf8mb4_general_ci NOT NULL,
  `due_at` datetime(6) DEFAULT NULL,
  `kind` enum('Complaint','Opinion') COLLATE utf8mb4_general_ci NOT NULL,
  `responded_at` datetime(6) DEFAULT NULL,
  `response` varchar(2048) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `satisfaction` int DEFAULT NULL,
  `status` enum('Closed','Escalated','Processing','Resolved','Submitted') COLLATE utf8mb4_general_ci NOT NULL,
  `title` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('Design','Fee','Material','Other','Quality','Safety','Schedule','Service') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `cre_id` bigint NOT NULL,
  `cust_id` bigint NOT NULL,
  `handler_id` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `stage_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`feedback_id`),
  KEY `idx_customer_feedback_company` (`com_id`,`feedback_id`),
  KEY `idx_customer_feedback_project` (`com_id`,`project_id`,`feedback_id`),
  KEY `idx_customer_feedback_status` (`com_id`,`status`,`feedback_id`),
  KEY `idx_customer_feedback_credential` (`com_id`,`cre_id`,`feedback_id`),
  KEY `idx_customer_feedback_store` (`com_id`,`store_id`,`feedback_id`),
  KEY `idx_customer_feedback_team` (`com_id`,`team_id`,`feedback_id`),
  KEY `idx_customer_feedback_owner` (`com_id`,`owner_id`,`feedback_id`),
  KEY `FKpd6l9v9vgua6qlo9mihcibnyk` (`cre_id`),
  KEY `FKdhtt4waxy5xaarv0mqhycdis3` (`cust_id`),
  KEY `FKr29jvuk8tjmg8a7cbyb8u87nd` (`handler_id`),
  KEY `FKqaoxwf43yoeettaxr63jchjdv` (`owner_id`),
  KEY `FK9htuekdfmcd2skm7p3ep7lscn` (`project_id`),
  KEY `FKbsa5ssxx6pdh0jvgar3wni7mv` (`property_id`),
  KEY `FK4fg9d77aa2x1gu2gy8k9713rh` (`stage_id`),
  KEY `FKodih64yfldy9a7sqbympt25t` (`store_id`),
  KEY `FK1wg1oeogp0un1lkqpoo0dxmtw` (`team_id`),
  CONSTRAINT `FK1wg1oeogp0un1lkqpoo0dxmtw` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FK4fg9d77aa2x1gu2gy8k9713rh` FOREIGN KEY (`stage_id`) REFERENCES `db_project_stage` (`stage_id`),
  CONSTRAINT `FK5x2mlu2lhxgm33gwflqok9mqt` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK9htuekdfmcd2skm7p3ep7lscn` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKbsa5ssxx6pdh0jvgar3wni7mv` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKdhtt4waxy5xaarv0mqhycdis3` FOREIGN KEY (`cust_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKodih64yfldy9a7sqbympt25t` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKpd6l9v9vgua6qlo9mihcibnyk` FOREIGN KEY (`cre_id`) REFERENCES `db_tenant_credentials` (`cre_id`),
  CONSTRAINT `FKqaoxwf43yoeettaxr63jchjdv` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKr29jvuk8tjmg8a7cbyb8u87nd` FOREIGN KEY (`handler_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `cust_customer_feedback_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `cust_customer_feedback_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cust_customer_feedback_image`
--

DROP TABLE IF EXISTS `cust_customer_feedback_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_customer_feedback_image` (
  `feedback_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKj4cddb9vh79rumq831uvpjaig` (`feedback_id`),
  CONSTRAINT `FKj4cddb9vh79rumq831uvpjaig` FOREIGN KEY (`feedback_id`) REFERENCES `cust_customer_feedback` (`feedback_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `content` varchar(512) COLLATE utf8mb4_general_ci NOT NULL,
  `next_follow_at` datetime(6) DEFAULT NULL,
  `request_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `way` enum('Other','Phone','Video','Visit','Wechat') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`follow_id`),
  UNIQUE KEY `uk_customer_follow_company_request` (`com_id`,`request_id`),
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cust_customer_follow_image`
--

DROP TABLE IF EXISTS `cust_customer_follow_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_customer_follow_image` (
  `follow_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci NOT NULL,
  `sort_no` int NOT NULL,
  PRIMARY KEY (`follow_id`,`sort_no`),
  CONSTRAINT `FK7krldpmgxhnlm7tkrchumdn3u` FOREIGN KEY (`follow_id`) REFERENCES `cust_customer_follow` (`follow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `customer_name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `expire_time` datetime(6) DEFAULT NULL,
  `status` enum('Accepted','Cancelled','Expired','Pending','Rejected') COLLATE utf8mb4_general_ci NOT NULL,
  `tel_no` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `token` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cust_customer_status_log`
--

DROP TABLE IF EXISTS `cust_customer_status_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_customer_status_log` (
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
  `change_type` enum('Dormant','FollowWake','OwnerChange','Restore') COLLATE utf8mb4_general_ci NOT NULL,
  `changed_at` datetime(6) NOT NULL,
  `from_status` enum('Dormant','Finished','Following','Lost','New','PendingMeasure','Quoted','Serving','Signed') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `operator_role` enum('Boss','Designer','Finance','Marketer','Pm','StoreManager','TeamLeader') COLLATE utf8mb4_general_ci NOT NULL,
  `reason_code` enum('BudgetUndecided','ContactUnreachable','CustomerRequestedPause','DecisionPending','LeadPendingVerification','Other','PlanPostponed') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reason_text` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `request_id` varchar(96) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `to_status` enum('Dormant','Finished','Following','Lost','New','PendingMeasure','Quoted','Serving','Signed') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `cust_id` bigint NOT NULL,
  `operator_id` bigint NOT NULL,
  PRIMARY KEY (`log_id`),
  UNIQUE KEY `uk_customer_status_log_request` (`com_id`,`request_id`),
  KEY `idx_customer_status_log_customer` (`com_id`,`cust_id`,`log_id`),
  KEY `idx_customer_status_log_status` (`com_id`,`to_status`,`log_id`),
  KEY `FKeq8qpbqfyl8wki5asxclh47dc` (`cust_id`),
  KEY `FKhr7qe721quw3rnfv1gyn8b2vf` (`operator_id`),
  CONSTRAINT `FK8m4c4e2aely1u792nxga033s9` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKeq8qpbqfyl8wki5asxclh47dc` FOREIGN KEY (`cust_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKhr7qe721quw3rnfv1gyn8b2vf` FOREIGN KEY (`operator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `cust_customer_status_log_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `cust_customer_status_log_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `competitor` enum('ChosenOther','ComparingOneTwo','ComparingThreePlus','None') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `decision_role` enum('Children','Couple','Parents','Self','Unknown') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `eco` enum('Elder','Infant','None','Pregnant') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `house_condition` enum('Old','Pending','Refined','Rough') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `household` enum('Couple','Single','ThreeGenerations','ThreePlus','Unknown') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `payment` enum('Full','Installment','Loan','Unknown') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `purpose` enum('FirstHome','Improve','Investment','Partial','SchoolDistrict','Wedding') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `start_window` enum('HalfYearPlus','OneToThreeMonths','ThreeToSixMonths','Undecided','WithinOneMonth') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `style` enum('Cream','Industrial','Luxury','Modern','NewChinese','Undecided','Wood') COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cust_portrait_concern`
--

DROP TABLE IF EXISTS `cust_portrait_concern`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_portrait_concern` (
  `portrait_id` bigint NOT NULL,
  `concern` enum('AfterSales','Duration','Eco','Price','Quality') COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FK6vru5u1n2scr9jvvfmhyajjhs` (`portrait_id`),
  CONSTRAINT `FK6vru5u1n2scr9jvvfmhyajjhs` FOREIGN KEY (`portrait_id`) REFERENCES `cust_portrait` (`portrait_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cust_project_share_link`
--

DROP TABLE IF EXISTS `cust_project_share_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_project_share_link` (
  `share_id` bigint NOT NULL,
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
  `expire_at` datetime(6) DEFAULT NULL,
  `scope` enum('Family','Friend') COLLATE utf8mb4_general_ci NOT NULL,
  `show_amount` bit(1) NOT NULL,
  `status` enum('Active','Expired','Revoked') COLLATE utf8mb4_general_ci NOT NULL,
  `token` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `created_by` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`share_id`),
  UNIQUE KEY `UK1dk5t406e6yoqdac64mb92r59` (`token`),
  KEY `idx_share_link_company_project` (`com_id`,`project_id`),
  KEY `idx_share_link_company_status_expire` (`com_id`,`status`,`expire_at`),
  KEY `FKst49u2ykpbmjg9q5gchqxqud8` (`created_by`),
  KEY `FKgn8dda65jvnp5b2k159qoqiq6` (`project_id`),
  CONSTRAINT `FKgn8dda65jvnp5b2k159qoqiq6` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKlcmwih0lvcx9163y9jurvhdfo` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKst49u2ykpbmjg9q5gchqxqud8` FOREIGN KEY (`created_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `cust_project_share_link_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `cust_project_share_link_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `category` enum('Behavior','Demand','Intent','Preference','Risk','Source','Value') COLLATE utf8mb4_general_ci NOT NULL,
  `color` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `scope` enum('Manual','System') COLLATE utf8mb4_general_ci NOT NULL,
  `sort` int NOT NULL,
  `status` enum('Disabled','Enabled') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `idx_cust_tag_company_name` (`com_id`,`name`),
  KEY `idx_cust_tag_company` (`com_id`,`tag_id`),
  KEY `idx_cust_tag_company_category` (`com_id`,`category`,`status`),
  CONSTRAINT `FKdsp6s2hg276pv3e5q58pyqtj8` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `cust_tag_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `cust_tag_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `conclusion` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `rectification_due_date` date DEFAULT NULL,
  `rectification_type` enum('Construction','Design') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Confirmed','Pending','Rejected') COLLATE utf8mb4_general_ci NOT NULL,
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
  KEY `idx_acceptance_company_project_status` (`com_id`,`project_id`,`status`,`acceptance_id`),
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_acceptance_evidence`
--

DROP TABLE IF EXISTS `db_acceptance_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_acceptance_evidence` (
  `acceptance_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKnxrpkicmhev7q49equytx5vwb` (`acceptance_id`),
  CONSTRAINT `FKnxrpkicmhev7q49equytx5vwb` FOREIGN KEY (`acceptance_id`) REFERENCES `db_acceptance` (`acceptance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_acceptance_issue`
--

DROP TABLE IF EXISTS `db_acceptance_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_acceptance_issue` (
  `acceptance_id` bigint NOT NULL,
  `issue` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FK2dqml84qfmjfb8g1dqs5gfyg9` (`acceptance_id`),
  CONSTRAINT `FK2dqml84qfmjfb8g1dqs5gfyg9` FOREIGN KEY (`acceptance_id`) REFERENCES `db_acceptance` (`acceptance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_acceptance_rectification_issue_image`
--

DROP TABLE IF EXISTS `db_acceptance_rectification_issue_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_acceptance_rectification_issue_image` (
  `acceptance_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FK1p737d5io6fyig533ds7o2g08` (`acceptance_id`),
  CONSTRAINT `FK1p737d5io6fyig533ds7o2g08` FOREIGN KEY (`acceptance_id`) REFERENCES `db_acceptance` (`acceptance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `conclusion` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `rectification_due_date` date DEFAULT NULL,
  `rectification_note` tinytext COLLATE utf8mb4_general_ci,
  `rectification_type` enum('Construction','Design') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `result` enum('Confirmed','Pending','Rejected') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_acceptance_round_evidence`
--

DROP TABLE IF EXISTS `db_acceptance_round_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_acceptance_round_evidence` (
  `round_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKkfdqfgypf0yjlpkpjtl6i5ic1` (`round_id`),
  CONSTRAINT `FKkfdqfgypf0yjlpkpjtl6i5ic1` FOREIGN KEY (`round_id`) REFERENCES `db_acceptance_round` (`round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_acceptance_round_issue`
--

DROP TABLE IF EXISTS `db_acceptance_round_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_acceptance_round_issue` (
  `round_id` bigint NOT NULL,
  `issue` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FK1emoisjx525tr1e3hmpo2rc5e` (`round_id`),
  CONSTRAINT `FK1emoisjx525tr1e3hmpo2rc5e` FOREIGN KEY (`round_id`) REFERENCES `db_acceptance_round` (`round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_acceptance_round_rectification_evidence`
--

DROP TABLE IF EXISTS `db_acceptance_round_rectification_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_acceptance_round_rectification_evidence` (
  `round_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FK9agiikhqhuk769xogcvb35l6v` (`round_id`),
  CONSTRAINT `FK9agiikhqhuk769xogcvb35l6v` FOREIGN KEY (`round_id`) REFERENCES `db_acceptance_round` (`round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_acceptance_round_rectification_issue_image`
--

DROP TABLE IF EXISTS `db_acceptance_round_rectification_issue_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_acceptance_round_rectification_issue_image` (
  `round_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKqlwwprqc7lrgybinsnmfkjjxe` (`round_id`),
  CONSTRAINT `FKqlwwprqc7lrgybinsnmfkjjxe` FOREIGN KEY (`round_id`) REFERENCES `db_acceptance_round` (`round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_ai_rate_card_version`
--

DROP TABLE IF EXISTS `db_ai_rate_card_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_ai_rate_card_version` (
  `rate_card_id` bigint NOT NULL,
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
  `capability_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `effective_from` datetime(6) DEFAULT NULL,
  `effective_to` datetime(6) DEFAULT NULL,
  `model_multiplier` decimal(10,4) NOT NULL,
  `quality_multiplier` decimal(10,4) NOT NULL,
  `status` enum('Active','Disabled','Draft') COLLATE utf8mb4_general_ci NOT NULL,
  `unit_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `units_per_point` decimal(18,6) NOT NULL,
  `version_no` int NOT NULL,
  `approved_by` bigint DEFAULT NULL,
  PRIMARY KEY (`rate_card_id`),
  UNIQUE KEY `uk_ai_rate_card_version` (`capability_code`,`version_no`),
  KEY `FKj3ypldbgqbnnasf0qc3ijjmrx` (`approved_by`),
  CONSTRAINT `FKj3ypldbgqbnnasf0qc3ijjmrx` FOREIGN KEY (`approved_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_ai_rate_card_version_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_ai_rate_card_version_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_ai_resource_account`
--

DROP TABLE IF EXISTS `db_ai_resource_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_ai_resource_account` (
  `account_id` bigint NOT NULL,
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
  `adjusted_units` bigint NOT NULL,
  `available_units` bigint NOT NULL,
  `balance_version` bigint NOT NULL,
  `consumed_units` bigint NOT NULL,
  `refunded_units` bigint NOT NULL,
  `reserved_units` bigint NOT NULL,
  `status` enum('Active','Frozen') COLLATE utf8mb4_general_ci NOT NULL,
  `total_granted` bigint NOT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `uk_ai_resource_account_company` (`com_id`),
  KEY `idx_ai_resource_account_company` (`com_id`,`account_id`),
  CONSTRAINT `FKbfxcnq1fr0essbbhg5jbb96o` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_ai_resource_account_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_ai_resource_account_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_ai_resource_ledger`
--

DROP TABLE IF EXISTS `db_ai_resource_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_ai_resource_ledger` (
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
  `balance_after` bigint NOT NULL,
  `balance_before` bigint NOT NULL,
  `change_units` bigint NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `rate_card_version` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reason` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` enum('Adjust','Consume','Expire','Grant','Refund','Release','Reserve') COLLATE utf8mb4_general_ci NOT NULL,
  `allocation_store_id` bigint DEFAULT NULL,
  `call_id` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  PRIMARY KEY (`ledger_id`),
  KEY `idx_ai_ledger_company_time` (`com_id`,`occurred_at`,`ledger_id`),
  KEY `idx_ai_ledger_company_call` (`com_id`,`call_id`,`ledger_id`),
  KEY `idx_ai_ledger_company_store` (`com_id`,`allocation_store_id`,`occurred_at`),
  KEY `FKp3be4mau83h16wh5e5aa9iffb` (`allocation_store_id`),
  KEY `FKcs415l2fo99wlhp9qn7ahs9fj` (`call_id`),
  KEY `FK758jyjoww67liny8bcxhshux9` (`operator_id`),
  CONSTRAINT `FK758jyjoww67liny8bcxhshux9` FOREIGN KEY (`operator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKcs415l2fo99wlhp9qn7ahs9fj` FOREIGN KEY (`call_id`) REFERENCES `db_ai_usage_call` (`call_id`),
  CONSTRAINT `FKet4r6hl05qdn00poy9c48f5l3` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKp3be4mau83h16wh5e5aa9iffb` FOREIGN KEY (`allocation_store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_ai_resource_ledger_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_ai_resource_ledger_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_ai_usage_call`
--

DROP TABLE IF EXISTS `db_ai_usage_call`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_ai_usage_call` (
  `call_id` bigint NOT NULL,
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
  `allocation_rule_version` varchar(24) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `allocation_type` enum('BusinessStore','EmployeeStore','EnterprisePublic') COLLATE utf8mb4_general_ci NOT NULL,
  `billing_status` enum('Adjusted','Calculated','Pending','Returned','Void') COLLATE utf8mb4_general_ci NOT NULL,
  `business_id` bigint DEFAULT NULL,
  `business_module` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `business_type` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cancel_reason` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `capability_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `capability_version` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `duration_ms` bigint DEFAULT NULL,
  `estimated` bit(1) NOT NULL,
  `failure_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `failure_message` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `idempotency_key` varchar(160) COLLATE utf8mb4_general_ci NOT NULL,
  `input_units` bigint NOT NULL,
  `model_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `model_version` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `output_units` bigint NOT NULL,
  `platform_cost` bigint DEFAULT NULL,
  `provider_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `rate_card_version` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `refunded_units` bigint NOT NULL,
  `request_id` varchar(96) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `resource_units` bigint NOT NULL,
  `scene_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `shadow_billable_units` bigint NOT NULL,
  `started_at` datetime(6) NOT NULL,
  `status` enum('Cancelled','DuplicateBlocked','Expired','Failed','Requested','Reserved','Running','Succeeded','Validated') COLLATE utf8mb4_general_ci NOT NULL,
  `allocation_store_id` bigint DEFAULT NULL,
  `business_store_id` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `employee_id` bigint DEFAULT NULL,
  `employee_store_id` bigint DEFAULT NULL,
  `retry_of_call_id` bigint DEFAULT NULL,
  PRIMARY KEY (`call_id`),
  UNIQUE KEY `uk_ai_usage_call_idempotency` (`com_id`,`idempotency_key`),
  KEY `idx_ai_usage_call_company_time` (`com_id`,`started_at`,`call_id`),
  KEY `idx_ai_usage_call_store_time` (`com_id`,`allocation_store_id`,`started_at`),
  KEY `idx_ai_usage_call_capability_time` (`com_id`,`capability_code`,`started_at`),
  KEY `idx_ai_usage_call_module_time` (`com_id`,`business_module`,`started_at`),
  KEY `idx_ai_usage_call_business` (`com_id`,`business_type`,`business_id`),
  KEY `FKndk7hci7bx6ibabc76ct26q7b` (`allocation_store_id`),
  KEY `FKkfr07gdwoqeparuunclx9roba` (`business_store_id`),
  KEY `FKjaaahyfrqcf8l9h1ktjv72pui` (`employee_id`),
  KEY `FKml9u0bma7685ay82m6351uotj` (`employee_store_id`),
  KEY `FK6kumhhhv30vr037mc3yeea0a1` (`retry_of_call_id`),
  CONSTRAINT `FK6kumhhhv30vr037mc3yeea0a1` FOREIGN KEY (`retry_of_call_id`) REFERENCES `db_ai_usage_call` (`call_id`),
  CONSTRAINT `FKjaaahyfrqcf8l9h1ktjv72pui` FOREIGN KEY (`employee_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKkfr07gdwoqeparuunclx9roba` FOREIGN KEY (`business_store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKlv0vuqdns6ode5dsvjs4pocxo` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKml9u0bma7685ay82m6351uotj` FOREIGN KEY (`employee_store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKndk7hci7bx6ibabc76ct26q7b` FOREIGN KEY (`allocation_store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_ai_usage_call_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_ai_usage_call_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `store_id` bigint DEFAULT NULL,
  PRIMARY KEY (`target_id`),
  KEY `idx_business_target_company` (`com_id`,`target_id`),
  KEY `idx_business_target_company_month` (`com_id`,`target_month`,`target_id`),
  KEY `FKi800pmwva8rjxx702dsrqecv` (`store_id`),
  CONSTRAINT `FKi800pmwva8rjxx702dsrqecv` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKp64xod7bvm8falnuqj6d97c82` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_business_target_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_business_target_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `is_default` bit(1) NOT NULL,
  `description` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `sort` int NOT NULL,
  `source_baseline_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source_baseline_version` int DEFAULT NULL,
  `status` enum('Disabled','Draft','Enabled') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `acceptance_mode` enum('AcceptanceDone','ConstructionDone','None') COLLATE utf8mb4_general_ci NOT NULL,
  `code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `default_days` int NOT NULL,
  `description` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `phone` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `version` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`limit_id`),
  UNIQUE KEY `idx_join_limit_phone` (`phone`),
  UNIQUE KEY `idx_join_limit_user` (`user_id`),
  CONSTRAINT `FKcy5wqoxx5ox9kvxefy12chbtk` FOREIGN KEY (`user_id`) REFERENCES `db_tenant_credentials` (`cre_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_company_member_log`
--

DROP TABLE IF EXISTS `db_company_member_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_company_member_log` (
  `log_id` bigint NOT NULL,
  `action` enum('Handover','Join','Resign','Resume','RoleChange','StoreChange','Suspend','Switch','TeamChange') COLLATE utf8mb4_general_ci NOT NULL,
  `after_snapshot` longtext COLLATE utf8mb4_general_ci,
  `before_snapshot` longtext COLLATE utf8mb4_general_ci,
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
  `reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `offset_days` int NOT NULL,
  `ratio` decimal(10,4) NOT NULL,
  `sort` int NOT NULL,
  `trigger_stage_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `trigger_type` enum('ContractSigned','ProjectFinished','StageAccepted','StageDone','StageStarted') COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('Final','First','Other','Progress','Retention') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `is_default` bit(1) NOT NULL,
  `description` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `sort` int NOT NULL,
  `source_baseline_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source_baseline_version` int DEFAULT NULL,
  `status` enum('Disabled','Draft','Enabled') COLLATE utf8mb4_general_ci NOT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_company_payment_plan_code` (`com_id`,`code`),
  KEY `idx_company_payment_plan_company` (`com_id`,`template_id`),
  KEY `idx_company_payment_plan_status` (`com_id`,`status`,`is_default`),
  CONSTRAINT `FKn3rnf9hglyfqu5aap16ykonx5` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_company_payment_plan_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_company_payment_plan_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_content_asset`
--

DROP TABLE IF EXISTS `db_content_asset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_content_asset` (
  `asset_id` bigint NOT NULL,
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
  `content_type` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `data_version` int NOT NULL,
  `desensitized` bit(1) NOT NULL,
  `file_path` varchar(512) COLLATE utf8mb4_general_ci NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `media_type` enum('Audio','Image','Text','Video') COLLATE utf8mb4_general_ci NOT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source_type` enum('Album','Camera','Import','Project') COLLATE utf8mb4_general_ci NOT NULL,
  `status` enum('Blocked','Draft','Expired','InternalReady','MarketingReady','PublicReady','Revoked') COLLATE utf8mb4_general_ci NOT NULL,
  `tags` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `truth_type` enum('Demo','Hypothesis','Native','Test') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `uploader_id` bigint NOT NULL,
  PRIMARY KEY (`asset_id`),
  KEY `idx_content_asset_company` (`com_id`,`asset_id`),
  KEY `idx_content_asset_company_project` (`com_id`,`project_id`,`asset_id`),
  KEY `idx_content_asset_company_status` (`com_id`,`status`,`asset_id`),
  KEY `FKte7al7y7gbi8hqtfcdl7se5j6` (`customer_id`),
  KEY `FKfdpoyd7j5quyeq8gobbydstih` (`project_id`),
  KEY `FK4h1942wp2731byadr97x9lmu1` (`store_id`),
  KEY `FK3q29aigeakrtxk1o5drl4owpe` (`uploader_id`),
  CONSTRAINT `FK3q29aigeakrtxk1o5drl4owpe` FOREIGN KEY (`uploader_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK4h1942wp2731byadr97x9lmu1` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKdqlp9xm5rfupbpj3nmt1y673a` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKfdpoyd7j5quyeq8gobbydstih` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKte7al7y7gbi8hqtfcdl7se5j6` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `db_content_asset_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_content_asset_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_content_authorization`
--

DROP TABLE IF EXISTS `db_content_authorization`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_content_authorization` (
  `auth_id` bigint NOT NULL,
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
  `allow_ads` bit(1) NOT NULL,
  `allow_ai` bit(1) NOT NULL,
  `allow_modify` bit(1) NOT NULL,
  `allow_sublicense` bit(1) NOT NULL,
  `channels` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `evidence_path` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `purpose` enum('Internal','Public','ZnDqMarketing') COLLATE utf8mb4_general_ci NOT NULL,
  `region` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `revoke_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `status` enum('Authorized','Disabled','Expired','Expiring','Pending','Revoked') COLLATE utf8mb4_general_ci NOT NULL,
  `subject_ref_id` bigint NOT NULL,
  `subject_type` enum('Company','Customer','Employee') COLLATE utf8mb4_general_ci NOT NULL,
  `valid_from` date NOT NULL,
  `valid_to` date NOT NULL,
  `asset_id` bigint NOT NULL,
  `com_id` bigint NOT NULL,
  `revoked_by` bigint DEFAULT NULL,
  PRIMARY KEY (`auth_id`),
  KEY `idx_content_auth_company` (`com_id`,`auth_id`),
  KEY `idx_content_auth_asset` (`asset_id`,`purpose`,`status`),
  KEY `idx_content_auth_expiry` (`com_id`,`status`,`valid_to`),
  KEY `FKhioowtdicvcq2wfnlrtj7yugu` (`revoked_by`),
  CONSTRAINT `FK1ur38xvweldjejg3b5rtf7a5s` FOREIGN KEY (`asset_id`) REFERENCES `db_content_asset` (`asset_id`),
  CONSTRAINT `FKhioowtdicvcq2wfnlrtj7yugu` FOREIGN KEY (`revoked_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKt6vefbt597ul70n0q75hk717k` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_content_authorization_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_content_authorization_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_content_employee_content`
--

DROP TABLE IF EXISTS `db_content_employee_content`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_content_employee_content` (
  `content_id` bigint NOT NULL,
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
  `caption` text COLLATE utf8mb4_general_ci,
  `cover_file_path` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `publish_status` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `rejection_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `render_error` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `render_job_id` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `rendered_file_path` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `review_status` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `selected_asset_ids` text COLLATE utf8mb4_general_ci,
  `started_at` datetime(6) DEFAULT NULL,
  `status` enum('Backfilled','Drafting','NotStarted','PendingReview','Published','ReadyToPublish','Rejected','Rendering') COLLATE utf8mb4_general_ci NOT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `reviewed_by` bigint DEFAULT NULL,
  `task_id` bigint NOT NULL,
  PRIMARY KEY (`content_id`),
  UNIQUE KEY `uk_content_employee_task` (`task_id`,`emp_id`),
  KEY `idx_content_employee_company` (`com_id`,`content_id`),
  KEY `idx_content_employee_employee` (`emp_id`,`status`,`content_id`),
  KEY `FKhllx9eaw12u5w7025obx3nsht` (`reviewed_by`),
  CONSTRAINT `FK45qpvfudhnd6lh1h5ycaboalu` FOREIGN KEY (`task_id`) REFERENCES `db_content_task` (`task_id`),
  CONSTRAINT `FK62i8q4fiwpoaumuvoqv4nn66e` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK7yy0qe32w1yuo0muvnspegfri` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKhllx9eaw12u5w7025obx3nsht` FOREIGN KEY (`reviewed_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_content_employee_content_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_content_employee_content_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_content_lead_touch`
--

DROP TABLE IF EXISTS `db_content_lead_touch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_content_lead_touch` (
  `touch_id` bigint NOT NULL,
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
  `attribution_type` enum('Inference','Manual','Platform','SelfReport','UniqueCode') COLLATE utf8mb4_general_ci NOT NULL,
  `confidence` enum('High','Low','Medium') COLLATE utf8mb4_general_ci NOT NULL,
  `evidence` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `is_primary` bit(1) NOT NULL,
  `touched_at` datetime(6) NOT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `employee_content_id` bigint NOT NULL,
  `publication_id` bigint DEFAULT NULL,
  `task_id` bigint NOT NULL,
  PRIMARY KEY (`touch_id`),
  UNIQUE KEY `uk_content_touch_customer_content` (`customer_id`,`employee_content_id`),
  KEY `idx_content_touch_company` (`com_id`,`touch_id`),
  KEY `idx_content_touch_customer` (`customer_id`,`touch_id`),
  KEY `FKq40kukjvn78rc2mhfnv0g1uvb` (`emp_id`),
  KEY `FKjwogs17jv93k7tgmc96kfgotx` (`employee_content_id`),
  KEY `FKh99lv2oacklb6m8n12lusnym9` (`publication_id`),
  KEY `FK1gla5411mcv4a8cx8m9rnfq5a` (`task_id`),
  CONSTRAINT `FK1gla5411mcv4a8cx8m9rnfq5a` FOREIGN KEY (`task_id`) REFERENCES `db_content_task` (`task_id`),
  CONSTRAINT `FK8crcdcporlo6gh6rnbb9rpic2` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKh99lv2oacklb6m8n12lusnym9` FOREIGN KEY (`publication_id`) REFERENCES `db_content_publication` (`publication_id`),
  CONSTRAINT `FKjwogs17jv93k7tgmc96kfgotx` FOREIGN KEY (`employee_content_id`) REFERENCES `db_content_employee_content` (`content_id`),
  CONSTRAINT `FKlqleytxvv83ikg31bx7cijo52` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKq40kukjvn78rc2mhfnv0g1uvb` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_content_lead_touch_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_content_lead_touch_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_content_publication`
--

DROP TABLE IF EXISTS `db_content_publication`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_content_publication` (
  `publication_id` bigint NOT NULL,
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
  `backfill_evidence_path` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `platform` enum('Douyin','Other','WeChatChannels','Xiaohongshu') COLLATE utf8mb4_general_ci NOT NULL,
  `platform_post_id` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `post_url` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `status` varchar(24) COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `employee_content_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  PRIMARY KEY (`publication_id`),
  KEY `idx_content_publication_company` (`com_id`,`publication_id`),
  KEY `idx_content_publication_content` (`employee_content_id`,`publication_id`),
  KEY `FK8s5vnk2kptqblgpa4j02jf3t1` (`emp_id`),
  KEY `FKb4fobw69rp2wx76jumy79uiee` (`task_id`),
  CONSTRAINT `FK8s5vnk2kptqblgpa4j02jf3t1` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKaoxbwixnxswkd7k1ea8x9sk2y` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKb4fobw69rp2wx76jumy79uiee` FOREIGN KEY (`task_id`) REFERENCES `db_content_task` (`task_id`),
  CONSTRAINT `FKgdedk01hsliifcqqyhv2bb9pm` FOREIGN KEY (`employee_content_id`) REFERENCES `db_content_employee_content` (`content_id`),
  CONSTRAINT `db_content_publication_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_content_publication_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_content_task`
--

DROP TABLE IF EXISTS `db_content_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_content_task` (
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
  `assignee_emp_ids` text COLLATE utf8mb4_general_ci,
  `assignee_scope` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `closed_at` datetime(6) DEFAULT NULL,
  `deadline` datetime(6) NOT NULL,
  `forbidden_claims` text COLLATE utf8mb4_general_ci,
  `goal` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `material_ids` text COLLATE utf8mb4_general_ci,
  `platform` enum('Douyin','Other','WeChatChannels','Xiaohongshu') COLLATE utf8mb4_general_ci NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `required_claims` text COLLATE utf8mb4_general_ci,
  `status` enum('Claimed','Closed','Completed','Draft','Published') COLLATE utf8mb4_general_ci NOT NULL,
  `target_audience` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `created_by` bigint NOT NULL,
  `lead_owner_id` bigint NOT NULL,
  `reviewer_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `template_id` bigint NOT NULL,
  PRIMARY KEY (`task_id`),
  KEY `idx_content_task_company` (`com_id`,`task_id`),
  KEY `idx_content_task_company_status` (`com_id`,`status`,`task_id`),
  KEY `FKnkln5pkucoeprhfij37ns2r93` (`created_by`),
  KEY `FKfinvc6stf8ohxgkykyytsgby1` (`lead_owner_id`),
  KEY `FK30bsty2w5pjfx1alush19egox` (`reviewer_id`),
  KEY `FKparo7oheyckdp06psgp5bpfhm` (`store_id`),
  KEY `FKjx9t2qot38u2iwwtqycyi875t` (`template_id`),
  CONSTRAINT `FK30bsty2w5pjfx1alush19egox` FOREIGN KEY (`reviewer_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKfinvc6stf8ohxgkykyytsgby1` FOREIGN KEY (`lead_owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKixk1uad2ut5m86s9qvqnqlplp` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKjx9t2qot38u2iwwtqycyi875t` FOREIGN KEY (`template_id`) REFERENCES `db_content_template` (`template_id`),
  CONSTRAINT `FKnkln5pkucoeprhfij37ns2r93` FOREIGN KEY (`created_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKparo7oheyckdp06psgp5bpfhm` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_content_task_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_content_task_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_content_template`
--

DROP TABLE IF EXISTS `db_content_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_content_template` (
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
  `allowed_asset_types` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `clip_max` int NOT NULL,
  `clip_min` int NOT NULL,
  `cover_schema` text COLLATE utf8mb4_general_ci,
  `duration_sec` int NOT NULL,
  `format` varchar(16) COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `script_schema` text COLLATE utf8mb4_general_ci,
  `status` enum('Blocked','Draft','Expired','InternalReady','MarketingReady','PublicReady','Revoked') COLLATE utf8mb4_general_ci NOT NULL,
  `version` int NOT NULL,
  `com_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  PRIMARY KEY (`template_id`),
  KEY `idx_content_template_company` (`com_id`,`template_id`),
  KEY `FKf4sa80g96l63xbtq6jcdt3mdw` (`store_id`),
  CONSTRAINT `FKf4sa80g96l63xbtq6jcdt3mdw` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKkpden8nrbx6fvpf597l27s7k1` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_content_template_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_content_template_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `conclusion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cost_delta` decimal(14,2) DEFAULT NULL,
  `customer_confirm_method` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `customer_confirmed` bit(1) NOT NULL,
  `customer_confirmed_at` datetime(6) DEFAULT NULL,
  `customer_confirmed_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  `signature_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Approved','Cancelled','CustomerConfirmed','Pending','Rejected') COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('Add','Price','Quantity','Remove','Replace') COLLATE utf8mb4_general_ci NOT NULL,
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
  KEY `idx_contract_change_company_project_status` (`com_id`,`project_id`,`status`,`change_id`),
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_contract_change_image`
--

DROP TABLE IF EXISTS `db_contract_change_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_contract_change_image` (
  `change_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKenpabt0pxd1f18cv8rnbxs3bs` (`change_id`),
  CONSTRAINT `FKenpabt0pxd1f18cv8rnbxs3bs` FOREIGN KEY (`change_id`) REFERENCES `db_contract_change` (`change_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `change_type` enum('Add','Price','Quantity','Remove','Replace') COLLATE utf8mb4_general_ci NOT NULL,
  `cost_delta` decimal(14,2) DEFAULT NULL,
  `item_type` enum('Appliance','Construction','Custom','General','Material','Soft') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `new_price` decimal(12,2) NOT NULL,
  `new_quantity` decimal(12,3) NOT NULL,
  `old_price` decimal(12,2) NOT NULL,
  `old_quantity` decimal(12,3) NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source_baseline_time` datetime(6) DEFAULT NULL,
  `source_remaining_amount` decimal(14,2) DEFAULT NULL,
  `source_remaining_quantity` decimal(12,3) DEFAULT NULL,
  `source_unit_price` decimal(12,2) DEFAULT NULL,
  `target_amount` decimal(14,2) DEFAULT NULL,
  `target_brand` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `target_category` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `target_cost_amount` decimal(14,2) DEFAULT NULL,
  `target_cost_complete` bit(1) NOT NULL,
  `target_cost_price` decimal(12,2) DEFAULT NULL,
  `target_craft` longtext COLLATE utf8mb4_general_ci,
  `target_criterion` longtext COLLATE utf8mb4_general_ci,
  `target_fixed_total_price` decimal(12,2) DEFAULT NULL,
  `target_formula` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `target_manual_price` decimal(12,2) DEFAULT NULL,
  `target_material_price` decimal(12,2) DEFAULT NULL,
  `target_name` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `target_pricing_mode` enum('FixedTotal','ManualOnly','MaterialAndManual','MaterialOnly') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `target_quantity` decimal(12,3) DEFAULT NULL,
  `target_series` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `target_spec` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `target_type` enum('Appliance','Construction','Custom','General','Material','Soft') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `target_unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `change_id` bigint NOT NULL,
  `com_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `source_change_item_id` bigint DEFAULT NULL,
  `contract_line_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `target_catalog_item_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`change_item_id`),
  KEY `idx_change_item_company` (`com_id`,`change_item_id`),
  KEY `idx_change_item_change` (`com_id`,`change_id`,`change_item_id`),
  KEY `idx_contract_change_item_company` (`com_id`,`change_item_id`),
  KEY `idx_contract_change_item_company_store` (`com_id`,`store_id`,`change_item_id`),
  KEY `idx_contract_change_item_company_team` (`com_id`,`team_id`,`change_item_id`),
  KEY `idx_contract_change_item_company_owner` (`com_id`,`owner_id`,`change_item_id`),
  KEY `idx_change_item_company_source_line` (`com_id`,`contract_line_id`,`change_item_id`),
  KEY `idx_change_item_company_source_change` (`com_id`,`source_change_item_id`,`change_item_id`),
  KEY `idx_change_item_company_target_catalog` (`com_id`,`target_catalog_item_id`,`change_item_id`),
  KEY `FK47r679g8b25hipt0fayg38xte` (`change_id`),
  KEY `FK4ca3u5kscfom4fh6pn2ay2nke` (`owner_id`),
  KEY `FKoxlwc9gtf0rrxcaqntyo0n83w` (`source_change_item_id`),
  KEY `FK4m0y6xle0vgn7y2q1pchs8umb` (`contract_line_id`),
  KEY `FK9pu5npglfmb9xgb8dyrbrtmv3` (`store_id`),
  KEY `FKge6ela579qwhalw8emuu6triq` (`target_catalog_item_id`),
  KEY `FKdvwcl8dytsgl8w1mjprvygdaq` (`team_id`),
  CONSTRAINT `FK47r679g8b25hipt0fayg38xte` FOREIGN KEY (`change_id`) REFERENCES `db_contract_change` (`change_id`),
  CONSTRAINT `FK4ca3u5kscfom4fh6pn2ay2nke` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK4m0y6xle0vgn7y2q1pchs8umb` FOREIGN KEY (`contract_line_id`) REFERENCES `db_contract_line` (`line_id`),
  CONSTRAINT `FK9pu5npglfmb9xgb8dyrbrtmv3` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKdvwcl8dytsgl8w1mjprvygdaq` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKfktuftj4g81uaaqvtw6xq21nn` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKge6ela579qwhalw8emuu6triq` FOREIGN KEY (`target_catalog_item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FKoxlwc9gtf0rrxcaqntyo0n83w` FOREIGN KEY (`source_change_item_id`) REFERENCES `db_contract_change_item` (`change_item_id`),
  CONSTRAINT `db_contract_change_item_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_contract_change_item_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `source_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `source_version` int NOT NULL,
  `start_date` date NOT NULL,
  `status` enum('Confirmed','Draft','Superseded') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `contract_id` bigint NOT NULL,
  PRIMARY KEY (`plan_id`),
  UNIQUE KEY `uk_contract_construction_plan_contract` (`contract_id`),
  KEY `FKksoyfnxfoi17v17i8f09i6rkf` (`com_id`),
  CONSTRAINT `FKeuyl8oc3tuc8pelyk5i029ynq` FOREIGN KEY (`contract_id`) REFERENCES `db_project_contract` (`contract_id`),
  CONSTRAINT `FKksoyfnxfoi17v17i8f09i6rkf` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_contract_construction_plan_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_contract_construction_plan_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `acceptance_mode` enum('AcceptanceDone','ConstructionDone','None') COLLATE utf8mb4_general_ci NOT NULL,
  `code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `default_days` int NOT NULL,
  `description` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `brand` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `category` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `cost` decimal(12,2) NOT NULL,
  `formula` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `manual_price` decimal(12,2) NOT NULL,
  `material_price` decimal(12,2) NOT NULL,
  `name` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `pricing_mode` enum('FixedTotal','ManualOnly','MaterialAndManual','MaterialOnly') COLLATE utf8mb4_general_ci NOT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `role` enum('Add','Base','Deduct','Gift','Option','Replace','Upgrade') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sort` int NOT NULL,
  `space_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `spec` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` enum('Appliance','Construction','Custom','General','Material','Soft') COLLATE utf8mb4_general_ci NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `due_date` date NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `offset_days` int NOT NULL,
  `ratio` decimal(8,4) NOT NULL,
  `sort` int NOT NULL,
  `trigger_stage_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `trigger_stage_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `trigger_type` enum('ContractSigned','ProjectFinished','StageAccepted','StageDone','StageStarted') COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('Final','First','Other','Progress','Retention') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `source_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `source_version` int NOT NULL,
  `status` enum('Confirmed','Draft','Superseded') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `cad_convert_status` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cad_preview_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cad_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cover_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `demolition_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `description` longtext COLLATE utf8mb4_general_ci,
  `layout` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `masonry_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `optimization_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `original_plan_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Archived','Draft','Presented','Ready','Selected') COLLATE utf8mb4_general_ci NOT NULL,
  `style` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `design_template_version_id` bigint DEFAULT NULL,
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
  KEY `idx_design_company_property_status` (`com_id`,`property_id`,`status`,`design_id`),
  KEY `FKcm8i0si17xg8a0u18npt1lf92` (`customer_id`),
  KEY `FKcyxrd55dnxp3kurkfmeol8acd` (`design_template_version_id`),
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
  CONSTRAINT `FKcyxrd55dnxp3kurkfmeol8acd` FOREIGN KEY (`design_template_version_id`) REFERENCES `design_template_version` (`version_id`),
  CONSTRAINT `db_design_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_design_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_design_context_snapshot`
--

DROP TABLE IF EXISTS `db_design_context_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_context_snapshot` (
  `snapshot_id` bigint NOT NULL,
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
  `captured_at` datetime(6) DEFAULT NULL,
  `confidence` varchar(16) COLLATE utf8mb4_general_ci NOT NULL,
  `conflict_flags_json` longtext COLLATE utf8mb4_general_ci,
  `context_json` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `missing_fields_json` longtext COLLATE utf8mb4_general_ci,
  `source_digest` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `design_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `generation_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`snapshot_id`),
  KEY `idx_context_snapshot_company` (`com_id`,`snapshot_id`),
  KEY `idx_context_snapshot_company_task` (`com_id`,`generation_id`,`snapshot_id`),
  KEY `idx_context_snapshot_company_design` (`com_id`,`design_id`,`snapshot_id`),
  KEY `FKi52tpuigpyxkyjiqcr4c04i1f` (`customer_id`),
  KEY `FKlo11jjrg6l8uoksgtv4f4x3n6` (`design_id`),
  KEY `FK1m46rl76xuukyyy5t2p9r6f2g` (`emp_id`),
  KEY `FKdb9lw8v8mkpip8319vgj8mdx1` (`property_id`),
  KEY `FKlbe4gxbfvlivdsj8dt1rucq40` (`store_id`),
  KEY `FKre6ve0qeyh0c7fjowfj0xf9ay` (`generation_id`),
  KEY `FKgem8uj4wgr72s06rqtknts0co` (`team_id`),
  CONSTRAINT `FK1m46rl76xuukyyy5t2p9r6f2g` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKahmg36gux0ape5tba9ujv8q5p` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKdb9lw8v8mkpip8319vgj8mdx1` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKgem8uj4wgr72s06rqtknts0co` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKi52tpuigpyxkyjiqcr4c04i1f` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKlbe4gxbfvlivdsj8dt1rucq40` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKlo11jjrg6l8uoksgtv4f4x3n6` FOREIGN KEY (`design_id`) REFERENCES `db_design` (`design_id`),
  CONSTRAINT `FKre6ve0qeyh0c7fjowfj0xf9ay` FOREIGN KEY (`generation_id`) REFERENCES `db_design_generation_task` (`generation_id`),
  CONSTRAINT `db_design_context_snapshot_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_design_context_snapshot_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_design_generation_task`
--

DROP TABLE IF EXISTS `db_design_generation_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_generation_task` (
  `generation_id` bigint NOT NULL,
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
  `completed_at` datetime(6) DEFAULT NULL,
  `failure_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `failure_message` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `mode` enum('Concept','Precise') COLLATE utf8mb4_general_ci NOT NULL,
  `model_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `source_digest` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('ContextBuilding','DesignerConfirmed','DraftReady','Failed','FeedbackReceived','Generating','Locked','PromptReady','Revised','Shared') COLLATE utf8mb4_general_ci NOT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `design_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `selected_render_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`generation_id`),
  UNIQUE KEY `UKsbhyk9tg1ao2uw6wp7jmm4f34` (`no`),
  KEY `idx_generation_company` (`com_id`,`generation_id`),
  KEY `idx_generation_company_store` (`com_id`,`store_id`,`generation_id`),
  KEY `idx_generation_company_team` (`com_id`,`team_id`,`generation_id`),
  KEY `idx_generation_company_owner` (`com_id`,`emp_id`,`generation_id`),
  KEY `idx_generation_company_design` (`com_id`,`design_id`,`generation_id`),
  KEY `FKomd0ftopme33rf4ylmo3sxns4` (`customer_id`),
  KEY `FK4mm3ins1y813iy4b1h8u6020g` (`design_id`),
  KEY `FKml78on3lfih7hberq57nr73te` (`emp_id`),
  KEY `FKfi6l53e8a1sdm0v59m1inf6e9` (`property_id`),
  KEY `FKlv1f754jp7gf40x0ageaci4jt` (`selected_render_id`),
  KEY `FK31vy10p42m9dqmgfagmb98fy1` (`store_id`),
  KEY `FKohr38k24akw132sd0hkh8pr5f` (`team_id`),
  CONSTRAINT `FK31vy10p42m9dqmgfagmb98fy1` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FK4mm3ins1y813iy4b1h8u6020g` FOREIGN KEY (`design_id`) REFERENCES `db_design` (`design_id`),
  CONSTRAINT `FK9dk2qmveetw45igetmvjwwxdy` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKfi6l53e8a1sdm0v59m1inf6e9` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKlv1f754jp7gf40x0ageaci4jt` FOREIGN KEY (`selected_render_id`) REFERENCES `db_design_render_version` (`render_id`),
  CONSTRAINT `FKml78on3lfih7hberq57nr73te` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKohr38k24akw132sd0hkh8pr5f` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKomd0ftopme33rf4ylmo3sxns4` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `db_design_generation_task_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_design_generation_task_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `brand` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `category` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `cost` decimal(12,2) NOT NULL,
  `cost_price` decimal(12,2) DEFAULT NULL,
  `craft` longtext COLLATE utf8mb4_general_ci,
  `criterion` longtext COLLATE utf8mb4_general_ci,
  `fixed_total_price` decimal(12,2) DEFAULT NULL,
  `formula` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `manual_price` decimal(12,2) NOT NULL,
  `material_price` decimal(12,2) NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `pricing_mode` enum('FixedTotal','ManualOnly','MaterialAndManual','MaterialOnly') COLLATE utf8mb4_general_ci NOT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `quantity_formula` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `quantity_source` enum('Formula','Manual') COLLATE utf8mb4_general_ci NOT NULL,
  `remark` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sort` int NOT NULL,
  `spec` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` enum('Appliance','Construction','Custom','General','Material','Soft') COLLATE utf8mb4_general_ci NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_design_item_images`
--

DROP TABLE IF EXISTS `db_design_item_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_item_images` (
  `item_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKltvl336sfiu8451469fcbi4qa` (`item_id`),
  CONSTRAINT `FKltvl336sfiu8451469fcbi4qa` FOREIGN KEY (`item_id`) REFERENCES `db_design_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_design_original_plan_images`
--

DROP TABLE IF EXISTS `db_design_original_plan_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_original_plan_images` (
  `design_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKinhgvgyj3s4a2ejv97ke8dev2` (`design_id`),
  CONSTRAINT `FKinhgvgyj3s4a2ejv97ke8dev2` FOREIGN KEY (`design_id`) REFERENCES `db_design` (`design_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_design_prompt_snapshot`
--

DROP TABLE IF EXISTS `db_design_prompt_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_prompt_snapshot` (
  `prompt_id` bigint NOT NULL,
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
  `document_brief_json` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `model_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `model_parameters_json` longtext COLLATE utf8mb4_general_ci,
  `rendered_at` datetime(6) DEFAULT NULL,
  `talk_prompt` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `template_version` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `visual_prompt` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `snapshot_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `design_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `generation_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`prompt_id`),
  KEY `idx_prompt_snapshot_company` (`com_id`,`prompt_id`),
  KEY `idx_prompt_snapshot_company_task` (`com_id`,`generation_id`,`prompt_id`),
  KEY `idx_prompt_snapshot_company_design` (`com_id`,`design_id`,`prompt_id`),
  KEY `FK3ciurljqh5oksyscrbdhby4mu` (`snapshot_id`),
  KEY `FKdo11u9p5j5txit5h6f8qh018s` (`customer_id`),
  KEY `FKq0ikjr5rra3gcota43dsndjm7` (`design_id`),
  KEY `FK4a712o6gvjr1891gvn2olbp2e` (`emp_id`),
  KEY `FK8dkqk9gp8pine8byi7lq4215a` (`property_id`),
  KEY `FK5dl661q63r0u0avdsbwjkm565` (`store_id`),
  KEY `FKrup0n8pjq7tew4q0fg0yht6eq` (`generation_id`),
  KEY `FKctpku1bphavjxs4xbx8elmx3q` (`team_id`),
  CONSTRAINT `FK3ciurljqh5oksyscrbdhby4mu` FOREIGN KEY (`snapshot_id`) REFERENCES `db_design_context_snapshot` (`snapshot_id`),
  CONSTRAINT `FK4a712o6gvjr1891gvn2olbp2e` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK5dl661q63r0u0avdsbwjkm565` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FK8dkqk9gp8pine8byi7lq4215a` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKctpku1bphavjxs4xbx8elmx3q` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKdo11u9p5j5txit5h6f8qh018s` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKnankejo0w21656t278af9mxr0` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKq0ikjr5rra3gcota43dsndjm7` FOREIGN KEY (`design_id`) REFERENCES `db_design` (`design_id`),
  CONSTRAINT `FKrup0n8pjq7tew4q0fg0yht6eq` FOREIGN KEY (`generation_id`) REFERENCES `db_design_generation_task` (`generation_id`),
  CONSTRAINT `db_design_prompt_snapshot_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_design_prompt_snapshot_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_design_render_version`
--

DROP TABLE IF EXISTS `db_design_render_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_render_version` (
  `render_id` bigint NOT NULL,
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
  `confirmed_at` datetime(6) DEFAULT NULL,
  `cost_amount` decimal(12,4) DEFAULT NULL,
  `cost_currency` varchar(8) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `deliverable` bit(1) NOT NULL,
  `image_url` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `input_image_count` int DEFAULT NULL,
  `model_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `output_image_count` int DEFAULT NULL,
  `request_id` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sequence` int NOT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `status` enum('Confirmed','Failed','Ready','Superseded') COLLATE utf8mb4_general_ci NOT NULL,
  `usage_json` longtext COLLATE utf8mb4_general_ci,
  `com_id` bigint NOT NULL,
  `confirmed_by` bigint DEFAULT NULL,
  `customer_id` bigint NOT NULL,
  `design_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `prompt_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `generation_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`render_id`),
  KEY `idx_render_version_company` (`com_id`,`render_id`),
  KEY `idx_render_version_company_task` (`com_id`,`generation_id`,`render_id`),
  KEY `idx_render_version_company_design` (`com_id`,`design_id`,`render_id`),
  KEY `FK3q59adxqgnq1f7pgp7u392t0h` (`confirmed_by`),
  KEY `FKjiu7n71sp9da2s1s7ujd2e6x2` (`customer_id`),
  KEY `FKc2d5l9dox37sbg2k2qr6l7y30` (`design_id`),
  KEY `FK4spor2y8j2kum9gyey91t7677` (`emp_id`),
  KEY `FK1l9tqmbj4k4qo3m5yjomlm27m` (`prompt_id`),
  KEY `FK2vl1ivc7jxookw52inm71hrqc` (`property_id`),
  KEY `FKsr0fxm8orm2c3r10pncu8xpqt` (`store_id`),
  KEY `FK38vs6e6vnc6sa1uj80d6edkhp` (`generation_id`),
  KEY `FKhx2taijry4035r1c43uemtjpm` (`team_id`),
  CONSTRAINT `FK1l9tqmbj4k4qo3m5yjomlm27m` FOREIGN KEY (`prompt_id`) REFERENCES `db_design_prompt_snapshot` (`prompt_id`),
  CONSTRAINT `FK2vl1ivc7jxookw52inm71hrqc` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FK38vs6e6vnc6sa1uj80d6edkhp` FOREIGN KEY (`generation_id`) REFERENCES `db_design_generation_task` (`generation_id`),
  CONSTRAINT `FK3q59adxqgnq1f7pgp7u392t0h` FOREIGN KEY (`confirmed_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK4spor2y8j2kum9gyey91t7677` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK8me9t0brfkd5l903hvwkvq4ek` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKc2d5l9dox37sbg2k2qr6l7y30` FOREIGN KEY (`design_id`) REFERENCES `db_design` (`design_id`),
  CONSTRAINT `FKhx2taijry4035r1c43uemtjpm` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKjiu7n71sp9da2s1s7ujd2e6x2` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKsr0fxm8orm2c3r10pncu8xpqt` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_design_render_version_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_design_render_version_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `category` enum('Budget','Function','Material','Other','Quality','Schedule','Style') COLLATE utf8mb4_general_ci NOT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `detail` longtext COLLATE utf8mb4_general_ci,
  `kind` enum('Demand','Reject') COLLATE utf8mb4_general_ci NOT NULL,
  `priority` enum('High','Low','Medium') COLLATE utf8mb4_general_ci NOT NULL,
  `response` longtext COLLATE utf8mb4_general_ci,
  `source` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `space_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Confirmed','Pending','Resolved') COLLATE utf8mb4_general_ci NOT NULL,
  `title` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('Balcony','Bathroom','Bedroom','DiningRoom','Kitchen','LivingRoom') COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_design_site_photos`
--

DROP TABLE IF EXISTS `db_design_site_photos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_site_photos` (
  `design_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKc6vdlwgwuu2o9ygxhe1whcv4t` (`design_id`),
  CONSTRAINT `FKc6vdlwgwuu2o9ygxhe1whcv4t` FOREIGN KEY (`design_id`) REFERENCES `db_design` (`design_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `area` decimal(12,2) DEFAULT NULL,
  `area_source` enum('Estimated','Measured') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `height` decimal(10,3) DEFAULT NULL,
  `length_m` decimal(10,3) DEFAULT NULL,
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `perimeter` decimal(12,2) DEFAULT NULL,
  `remark` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sort` int NOT NULL,
  `source_measure_region_id` bigint DEFAULT NULL,
  `type` enum('Balcony','Bathroom','Bedroom','DiningRoom','Kitchen','LivingRoom') COLLATE utf8mb4_general_ci NOT NULL,
  `value_origin` enum('Measure','Override','TemplateReference') COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_design_space_images`
--

DROP TABLE IF EXISTS `db_design_space_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_space_images` (
  `space_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKhq8whiwlx4643al574ra9l5si` (`space_id`),
  CONSTRAINT `FKhq8whiwlx4643al574ra9l5si` FOREIGN KEY (`space_id`) REFERENCES `db_design_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_design_survey_photos`
--

DROP TABLE IF EXISTS `db_design_survey_photos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_survey_photos` (
  `design_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FK10s8n0t0x0wcw1ftj6jy14ixp` (`design_id`),
  CONSTRAINT `FK10s8n0t0x0wcw1ftj6jy14ixp` FOREIGN KEY (`design_id`) REFERENCES `db_design` (`design_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `cover_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `description` longtext COLLATE utf8mb4_general_ci,
  `no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `output_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Archived','Draft','Generated','Published') COLLATE utf8mb4_general_ci NOT NULL,
  `title` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `content` longtext COLLATE utf8mb4_general_ci,
  `sort` int NOT NULL,
  `subtitle` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `title` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('Appliance','Catalog','Community','Cover','Demolition','End','Investment','Masonry','Material','Optimization','OriginalPlan','Problem','Requirement','RoomDesign','Saving','SitePhoto','SoftDecor','Survey','SurveyReport') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_design_talk_section_images`
--

DROP TABLE IF EXISTS `db_design_talk_section_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_design_talk_section_images` (
  `section_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKhr7cwgjhspoc22asfoyddfwrp` (`section_id`),
  CONSTRAINT `FKhr7cwgjhspoc22asfoyddfwrp` FOREIGN KEY (`section_id`) REFERENCES `db_design_talk_section` (`section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `reason` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `address` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `bank_account` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `bank_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `credit_code` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `legal_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `legal_representative` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `logo` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `service_hotline` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `uk_document_profile_company` (`com_id`),
  CONSTRAINT `FKnbmjs114ay38t8adsrslyyv2x` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_document_profile_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_document_profile_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `audience` enum('Customer','Internal') COLLATE utf8mb4_general_ci NOT NULL,
  `business_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `content_type` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `file_name` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `generated_at` datetime(6) NOT NULL,
  `path` varchar(512) COLLATE utf8mb4_general_ci NOT NULL,
  `sha256` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `size` bigint NOT NULL,
  `status` enum('Active','Voided') COLLATE utf8mb4_general_ci NOT NULL,
  `template_version` int NOT NULL,
  `type` enum('Contract','Material','Payment','Quote','Receipt') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `designer_config` longtext COLLATE utf8mb4_general_ci,
  `field_config` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `layout_config` longtext COLLATE utf8mb4_general_ci,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `preset_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `published_version` int NOT NULL,
  `scope` enum('Company','Platform') COLLATE utf8mb4_general_ci NOT NULL,
  `status` enum('Active','Voided') COLLATE utf8mb4_general_ci NOT NULL,
  `style_config` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('Contract','Material','Payment','Quote','Receipt') COLLATE utf8mb4_general_ci NOT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  `published_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`template_id`),
  KEY `idx_document_template_company` (`com_id`,`template_id`),
  KEY `idx_document_template_company_type` (`com_id`,`type`,`version`),
  KEY `idx_document_template_company_preset` (`com_id`,`type`,`preset_code`,`version`),
  KEY `FKr4o1x1sc44pw1qc94kwye1kqg` (`published_by_id`),
  CONSTRAINT `FKjif5qdvxsk6fgm6l6c8qc70p7` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKr4o1x1sc44pw1qc94kwye1kqg` FOREIGN KEY (`published_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_document_template_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_document_template_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_document_template_version`
--

DROP TABLE IF EXISTS `db_document_template_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_document_template_version` (
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
  `designer_config` longtext COLLATE utf8mb4_general_ci,
  `field_config` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `layout_config` longtext COLLATE utf8mb4_general_ci,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `preset_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `status` enum('Disabled','Draft','Published','Superseded') COLLATE utf8mb4_general_ci NOT NULL,
  `style_config` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  `published_by_id` bigint DEFAULT NULL,
  `template_id` bigint NOT NULL,
  PRIMARY KEY (`version_id`),
  UNIQUE KEY `uk_document_template_version` (`com_id`,`template_id`,`version`),
  KEY `idx_document_template_version_company` (`com_id`,`version_id`),
  KEY `idx_document_template_version_template` (`template_id`,`version`),
  KEY `idx_document_template_version_status` (`com_id`,`template_id`,`status`,`version`),
  KEY `FKc6yt0e130q6tkdsijyty6nr0r` (`published_by_id`),
  CONSTRAINT `FK30ohxluwlioumba6638iv8add` FOREIGN KEY (`template_id`) REFERENCES `db_document_template` (`template_id`),
  CONSTRAINT `FKc6yt0e130q6tkdsijyty6nr0r` FOREIGN KEY (`published_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKcajdtjnu0q95fpj05nkawjfjj` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_document_template_version_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_document_template_version_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_finance_expense`
--

DROP TABLE IF EXISTS `db_finance_expense`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_expense` (
  `expense_id` bigint NOT NULL,
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
  `approval_note` longtext COLLATE utf8mb4_general_ci,
  `approved_at` datetime(6) DEFAULT NULL,
  `description` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `expense_date` date NOT NULL,
  `expense_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `idempotency_key` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `payment_mode` enum('Direct','Reimbursement') COLLATE utf8mb4_general_ci NOT NULL,
  `self_approved` bit(1) NOT NULL,
  `source_id` bigint DEFAULT NULL,
  `source_type` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Approved','Cancelled','Closed','Draft','Rejected','Returned','Submitted','Voided') COLLATE utf8mb4_general_ci NOT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `void_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `voided_at` datetime(6) DEFAULT NULL,
  `approved_by_id` bigint DEFAULT NULL,
  `category_id` bigint NOT NULL,
  `com_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `payee_emp_id` bigint DEFAULT NULL,
  `project_id` bigint DEFAULT NULL,
  `requester_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  `voided_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`expense_id`),
  UNIQUE KEY `uk_fin_expense_company_no` (`com_id`,`expense_no`),
  UNIQUE KEY `uk_fin_expense_company_idempotency` (`com_id`,`idempotency_key`),
  KEY `idx_fin_expense_company` (`com_id`,`expense_id`),
  KEY `idx_fin_expense_store_status` (`com_id`,`store_id`,`status`,`create_date_time`),
  KEY `idx_fin_expense_requester` (`com_id`,`requester_id`,`create_date_time`),
  KEY `idx_fin_expense_project` (`com_id`,`project_id`,`expense_date`),
  KEY `idx_fin_expense_payee` (`com_id`,`payee_emp_id`,`create_date_time`),
  KEY `idx_fin_expense_owner` (`com_id`,`owner_id`,`create_date_time`),
  KEY `FKq3bohr3272luyi9ct9iafe86b` (`approved_by_id`),
  KEY `FKw9f4b5lm80mwta2wyjyl50jp` (`category_id`),
  KEY `FK6y0upigcvy4jcfpvxrfpf0h84` (`owner_id`),
  KEY `FKbbfuuttjrqosoi97ildh9gvin` (`payee_emp_id`),
  KEY `FKcq9t2bs4d3dv75ip7elyjmxrw` (`project_id`),
  KEY `FKr7u8gvdrntj1hdct3r97j3rut` (`requester_id`),
  KEY `FKmqqk8pyqteyjqpsobibt2hung` (`store_id`),
  KEY `FKirf8c0xj3vumi5nvq2muxyspr` (`team_id`),
  KEY `FK4dvvbdasadiwagldmnv2cdv75` (`voided_by_id`),
  CONSTRAINT `FK4dvvbdasadiwagldmnv2cdv75` FOREIGN KEY (`voided_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK6y0upigcvy4jcfpvxrfpf0h84` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKbbfuuttjrqosoi97ildh9gvin` FOREIGN KEY (`payee_emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKcq9t2bs4d3dv75ip7elyjmxrw` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKirf8c0xj3vumi5nvq2muxyspr` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKmqqk8pyqteyjqpsobibt2hung` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKnak13bvgi4hoo04u0ww4uwt1v` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKq3bohr3272luyi9ct9iafe86b` FOREIGN KEY (`approved_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKr7u8gvdrntj1hdct3r97j3rut` FOREIGN KEY (`requester_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKw9f4b5lm80mwta2wyjyl50jp` FOREIGN KEY (`category_id`) REFERENCES `db_finance_expense_category` (`category_id`),
  CONSTRAINT `db_finance_expense_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_expense_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_finance_expense_attachment`
--

DROP TABLE IF EXISTS `db_finance_expense_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_expense_attachment` (
  `attachment_id` bigint NOT NULL,
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
  `document_type` enum('Invoice','Itinerary','Other','PaymentScreenshot','Receipt') COLLATE utf8mb4_general_ci NOT NULL,
  `file_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `file_url` varchar(512) COLLATE utf8mb4_general_ci NOT NULL,
  `sha256` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `uploaded_at` datetime(6) NOT NULL,
  `com_id` bigint NOT NULL,
  `expense_id` bigint NOT NULL,
  `uploaded_by_id` bigint NOT NULL,
  PRIMARY KEY (`attachment_id`),
  KEY `idx_fin_expense_attachment_company` (`com_id`,`attachment_id`),
  KEY `idx_fin_expense_attachment_expense` (`com_id`,`expense_id`,`attachment_id`),
  KEY `FK52ogk7e5napvant4s5xmbxtcf` (`expense_id`),
  KEY `FK58bju1w4aj5nvs0skb81f77ct` (`uploaded_by_id`),
  CONSTRAINT `FK52ogk7e5napvant4s5xmbxtcf` FOREIGN KEY (`expense_id`) REFERENCES `db_finance_expense` (`expense_id`),
  CONSTRAINT `FK58bju1w4aj5nvs0skb81f77ct` FOREIGN KEY (`uploaded_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKiq1cd7m6rrs6nywk1u1thq0ek` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_finance_expense_attachment_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_expense_attachment_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_finance_expense_category`
--

DROP TABLE IF EXISTS `db_finance_expense_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_expense_category` (
  `category_id` bigint NOT NULL,
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
  `built_in` bit(1) NOT NULL,
  `code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `sort_no` int NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint DEFAULT NULL,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uk_fin_expense_category_company_code` (`com_id`,`code`),
  KEY `idx_fin_expense_category_company` (`com_id`,`category_id`),
  KEY `idx_fin_expense_category_sort` (`com_id`,`active`,`sort_no`),
  KEY `FKridlgd6v03re150abtei8jwcy` (`creator_id`),
  CONSTRAINT `FKna2s9sbtn6ejy2pj9cli7xapc` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKridlgd6v03re150abtei8jwcy` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_finance_expense_category_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_expense_category_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_finance_expense_revision`
--

DROP TABLE IF EXISTS `db_finance_expense_revision`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_expense_revision` (
  `revision_id` bigint NOT NULL,
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
  `snapshot_json` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `submitted_at` datetime(6) NOT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  `expense_id` bigint NOT NULL,
  `submitted_by_id` bigint NOT NULL,
  PRIMARY KEY (`revision_id`),
  UNIQUE KEY `uk_fin_expense_revision_company_expense_version` (`com_id`,`expense_id`,`version`),
  KEY `idx_fin_expense_revision_company` (`com_id`,`revision_id`),
  KEY `idx_fin_expense_revision_expense` (`com_id`,`expense_id`,`version`),
  KEY `FKnd416rw4vk0objkaqbpxwe9sy` (`expense_id`),
  KEY `FK60a0yjndq8oeg8i1hi50q9sdj` (`submitted_by_id`),
  CONSTRAINT `FK60a0yjndq8oeg8i1hi50q9sdj` FOREIGN KEY (`submitted_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKj5qqgeukhmfhuutdoy1cwpalu` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKnd416rw4vk0objkaqbpxwe9sy` FOREIGN KEY (`expense_id`) REFERENCES `db_finance_expense` (`expense_id`),
  CONSTRAINT `db_finance_expense_revision_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_expense_revision_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `account` enum('Alipay','Cash','CompanyBank','Other','Wechat') COLLATE utf8mb4_general_ci NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `category` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `correction_group_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `counterparty` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `direction` enum('Expense','Income') COLLATE utf8mb4_general_ci NOT NULL,
  `occurred_at` date NOT NULL,
  `purpose` enum('CapitalTransfer','CompanyExpense','Other','OtherIncome','ProjectIncidentalCost') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source` enum('Adjustment','Manual','Payment','Receipt','ReceiptReversal','Refund') COLLATE utf8mb4_general_ci NOT NULL,
  `source_id` bigint DEFAULT NULL,
  `summary` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `void_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `voided` bit(1) NOT NULL,
  `voided_at` datetime(6) DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `original_ledger_id` bigint DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  `voided_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`ledger_id`),
  KEY `idx_fin_ledger_company` (`com_id`,`ledger_id`),
  KEY `idx_fin_ledger_project` (`com_id`,`project_id`,`ledger_id`),
  KEY `idx_fin_ledger_store` (`com_id`,`store_id`,`ledger_id`),
  KEY `idx_fin_ledger_team` (`com_id`,`team_id`,`ledger_id`),
  KEY `idx_fin_ledger_owner` (`com_id`,`owner_id`,`ledger_id`),
  KEY `FKg5919o4rsnomwmktqrj6nkwk1` (`category_id`),
  KEY `FK3hitoqenx4csywx24ehrqp2rs` (`creator_id`),
  KEY `FK9q1h3mihweybfhr1aesena03c` (`original_ledger_id`),
  KEY `FK7qc5ko3h4qxwa6ahlfoxa4lal` (`owner_id`),
  KEY `FKabveqxsl98rb0c652q8jwx3my` (`project_id`),
  KEY `FKba9167ripw83a5b5j7c22qy34` (`store_id`),
  KEY `FKnao31r3t8ct5xfjt1pr9g7wr9` (`team_id`),
  KEY `FKohrkek8yhf2f02bmorafj3yfq` (`voided_by_id`),
  CONSTRAINT `FK3hitoqenx4csywx24ehrqp2rs` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK7qc5ko3h4qxwa6ahlfoxa4lal` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK9q1h3mihweybfhr1aesena03c` FOREIGN KEY (`original_ledger_id`) REFERENCES `db_finance_ledger` (`ledger_id`),
  CONSTRAINT `FKa1bi95tuvd3pcuxq0s91uh13c` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKabveqxsl98rb0c652q8jwx3my` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKba9167ripw83a5b5j7c22qy34` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKg5919o4rsnomwmktqrj6nkwk1` FOREIGN KEY (`category_id`) REFERENCES `db_finance_ledger_category` (`category_id`),
  CONSTRAINT `FKnao31r3t8ct5xfjt1pr9g7wr9` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKohrkek8yhf2f02bmorafj3yfq` FOREIGN KEY (`voided_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_finance_ledger_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_ledger_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_finance_ledger_category`
--

DROP TABLE IF EXISTS `db_finance_ledger_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_ledger_category` (
  `category_id` bigint NOT NULL,
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
  `built_in` bit(1) NOT NULL,
  `code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `direction` enum('Expense','Income') COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `sort_no` int NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uk_fin_category_company_code` (`com_id`,`direction`,`code`),
  KEY `idx_fin_category_company_parent` (`com_id`,`parent_id`,`direction`,`sort_no`),
  KEY `idx_fin_category_company` (`com_id`,`category_id`),
  KEY `FK33q9jtl5tpnjexqihhnq33sl1` (`creator_id`),
  KEY `FKjb4to6wt0ogx7miht5fqkexnp` (`parent_id`),
  CONSTRAINT `FK33q9jtl5tpnjexqihhnq33sl1` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKj00qpqqni4r79kk4jlffbq619` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKjb4to6wt0ogx7miht5fqkexnp` FOREIGN KEY (`parent_id`) REFERENCES `db_finance_ledger_category` (`category_id`),
  CONSTRAINT `db_finance_ledger_category_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_ledger_category_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_finance_ledger_revision`
--

DROP TABLE IF EXISTS `db_finance_ledger_revision`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_ledger_revision` (
  `revision_id` bigint NOT NULL,
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
  `after_snapshot` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `before_snapshot` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('MetadataCorrection','ReverseAndRecreate','Void') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `ledger_id` bigint NOT NULL,
  `operated_by_id` bigint NOT NULL,
  PRIMARY KEY (`revision_id`),
  KEY `idx_fin_revision_company` (`com_id`,`revision_id`),
  KEY `idx_fin_revision_ledger` (`com_id`,`ledger_id`,`revision_id`),
  KEY `FKkrd1cwq00keu2u7e4b171sd2` (`ledger_id`),
  KEY `FKi4c8uujeck3eu798wrl8sbxwu` (`operated_by_id`),
  CONSTRAINT `FKi4c8uujeck3eu798wrl8sbxwu` FOREIGN KEY (`operated_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKkrd1cwq00keu2u7e4b171sd2` FOREIGN KEY (`ledger_id`) REFERENCES `db_finance_ledger` (`ledger_id`),
  CONSTRAINT `FKkvus97h7706892rxx1x2xrrbg` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_finance_ledger_revision_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_ledger_revision_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_finance_opening_balance`
--

DROP TABLE IF EXISTS `db_finance_opening_balance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_opening_balance` (
  `opening_id` bigint NOT NULL,
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
  `account` enum('Alipay','Cash','CompanyBank','Other','Wechat') COLLATE utf8mb4_general_ci NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `as_of_date` date NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint DEFAULT NULL,
  PRIMARY KEY (`opening_id`),
  UNIQUE KEY `uk_fin_opening_company_account_date` (`com_id`,`account`,`as_of_date`),
  KEY `idx_fin_opening_company_date` (`com_id`,`as_of_date`,`opening_id`),
  KEY `FKjumckkdgjhagvo41vto8uc2v4` (`creator_id`),
  CONSTRAINT `FKjumckkdgjhagvo41vto8uc2v4` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKob77i6cajpoglk0vj6kc7wa2a` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_finance_opening_balance_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_opening_balance_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `business_origin` enum('Carryover','New') COLLATE utf8mb4_general_ci NOT NULL,
  `due_date` date DEFAULT NULL,
  `estimated` bit(1) NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `paid_amount` decimal(14,2) NOT NULL,
  `payee_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `period` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `receipt_date` date DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `settled_amount` decimal(14,2) DEFAULT NULL,
  `settlement_mode` enum('MonthlyCutoff','ReceiptDays') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source_id` bigint DEFAULT NULL,
  `source_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source_type` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Closed','Overdue','Paid','Partial','Pending') COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('EmployeeCommission','EmployeeSalary','Expense','Material','Other','Project','Salary','Supplier') COLLATE utf8mb4_general_ci NOT NULL,
  `urgent` bit(1) NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `material_plan_id` bigint DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  `payee_emp_id` bigint DEFAULT NULL,
  `payee_partner_id` bigint DEFAULT NULL,
  `project_id` bigint DEFAULT NULL,
  `property_id` bigint DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `account` enum('Alipay','Cash','CompanyBank','Other','Wechat') COLLATE utf8mb4_general_ci NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `batch_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `idempotency_fingerprint` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `idempotency_key` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `paid_at` date NOT NULL,
  `payee_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `transaction_no` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `voucher_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `partner_id` bigint DEFAULT NULL,
  `payable_id` bigint NOT NULL,
  `payee_emp_id` bigint DEFAULT NULL,
  `project_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  `worker_id` bigint DEFAULT NULL,
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `uk_fin_payment_company_idempotency` (`com_id`,`idempotency_key`),
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
  KEY `FK3wr8al57x055og16kaybi740r` (`payee_emp_id`),
  KEY `FKnvwrjmvdh6umjaweqq81falmj` (`project_id`),
  KEY `FKrgjw750a7nklkhsbywubb3f3x` (`store_id`),
  KEY `FKo5w7jpnmsiudplqy1dvn3m5xd` (`team_id`),
  KEY `FK406o2oruc5a6nfh381glkxdeh` (`worker_id`),
  CONSTRAINT `FK3wr8al57x055og16kaybi740r` FOREIGN KEY (`payee_emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `account` enum('Alipay','Cash','CompanyBank','Other','Wechat') COLLATE utf8mb4_general_ci NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `idempotency_fingerprint` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `idempotency_key` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `payer_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `received_at` date NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reversal_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reversed_at` datetime(6) DEFAULT NULL,
  `status` enum('Active','Reversed') COLLATE utf8mb4_general_ci NOT NULL,
  `voucher_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `receivable_id` bigint NOT NULL,
  `reversed_by_id` bigint DEFAULT NULL,
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
  KEY `FKgr110nqdo4t1rcpb6ba1t3e0j` (`reversed_by_id`),
  KEY `FKt2huygd04sq41ffbvg2ugou86` (`store_id`),
  KEY `FKs994t8dgmwycmi4dbxqadcwr4` (`team_id`),
  CONSTRAINT `FK36ame5945ny8ye9w657sa1ins` FOREIGN KEY (`receivable_id`) REFERENCES `db_finance_receivable` (`receivable_id`),
  CONSTRAINT `FK6iw6ijm55bw9yx7w86mgy2gk5` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKgr110nqdo4t1rcpb6ba1t3e0j` FOREIGN KEY (`reversed_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKht1wubd6c3i2egae6qb1v2rfs` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKkfvyyur9t6bpi8xcfnpnf8yvk` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKnnpog2mmr79hkhavo44taba8i` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKo0i9r0abe0wkfmhwhujqll73q` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKs994t8dgmwycmi4dbxqadcwr4` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKt2huygd04sq41ffbvg2ugou86` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_finance_receipt_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_receipt_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_finance_receipt_reversal`
--

DROP TABLE IF EXISTS `db_finance_receipt_reversal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_receipt_reversal` (
  `reversal_id` bigint NOT NULL,
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
  `after_snapshot` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `before_snapshot` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `idempotency_fingerprint` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `idempotency_key` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  `reason_type` enum('AccountOrDateError','AmountError','Duplicate','NotReceived','Other','ReceivableError') COLLATE utf8mb4_general_ci NOT NULL,
  `reversal_at` date NOT NULL,
  `com_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `operator_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `receipt_id` bigint NOT NULL,
  `receivable_id` bigint NOT NULL,
  `replacement_receipt_id` bigint DEFAULT NULL,
  `reversal_ledger_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`reversal_id`),
  UNIQUE KEY `uk_fin_receipt_reversal_receipt` (`receipt_id`),
  UNIQUE KEY `uk_fin_receipt_reversal_idempotency` (`com_id`,`idempotency_key`),
  UNIQUE KEY `UK28jyr2fisv6fumeomxuhodq8d` (`reversal_ledger_id`),
  KEY `idx_fin_receipt_reversal_company` (`com_id`,`reversal_id`),
  KEY `idx_fin_receipt_reversal_project` (`com_id`,`project_id`,`reversal_id`),
  KEY `idx_fin_receipt_reversal_store` (`com_id`,`store_id`,`reversal_id`),
  KEY `idx_fin_receipt_reversal_date` (`com_id`,`reversal_at`,`reversal_id`),
  KEY `FK86hws8o20b7r8x4sp7vg691ih` (`customer_id`),
  KEY `FK5y06h9e5t3s5lldf41m0nyewt` (`operator_id`),
  KEY `FKsff9059rgpo00r2ldj6nvtaaf` (`owner_id`),
  KEY `FK9aax7uwlhlyri7b9w3usailaw` (`project_id`),
  KEY `FK1qfywcrutq6vkv5og6yl1sdjr` (`receivable_id`),
  KEY `FKd5nkpyj3uusyfhwic14xet3r5` (`replacement_receipt_id`),
  KEY `FK1eleisx0rbxem1342jkvb6ejb` (`store_id`),
  KEY `FK3wbfjneyfggfk440kp74pynoe` (`team_id`),
  CONSTRAINT `FK1eleisx0rbxem1342jkvb6ejb` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FK1qfywcrutq6vkv5og6yl1sdjr` FOREIGN KEY (`receivable_id`) REFERENCES `db_finance_receivable` (`receivable_id`),
  CONSTRAINT `FK3wbfjneyfggfk440kp74pynoe` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FK5y06h9e5t3s5lldf41m0nyewt` FOREIGN KEY (`operator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK86hws8o20b7r8x4sp7vg691ih` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FK9aax7uwlhlyri7b9w3usailaw` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKd5nkpyj3uusyfhwic14xet3r5` FOREIGN KEY (`replacement_receipt_id`) REFERENCES `db_finance_receipt` (`receipt_id`),
  CONSTRAINT `FKluh5o0jo72k3a99evn9gb91ka` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKp8xde0v6dilr0itrgwv14w6i0` FOREIGN KEY (`reversal_ledger_id`) REFERENCES `db_finance_ledger` (`ledger_id`),
  CONSTRAINT `FKsff9059rgpo00r2ldj6nvtaaf` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKt0q0hfx3vu4vy8sxawni7mqnl` FOREIGN KEY (`receipt_id`) REFERENCES `db_finance_receipt` (`receipt_id`),
  CONSTRAINT `db_finance_receipt_reversal_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_receipt_reversal_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `business_origin` enum('Carryover','New') COLLATE utf8mb4_general_ci NOT NULL,
  `due_date` date NOT NULL,
  `invoice_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `invoice_required` bit(1) NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `received_amount` decimal(14,2) NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `stage_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `stage_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Closed','Overdue','Partial','Pending','Received') COLLATE utf8mb4_general_ci NOT NULL,
  `trigger_type` enum('ContractSigned','ProjectFinished','StageAccepted','StageDone','StageStarted') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` enum('Final','First','Other','Progress','Retention') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_finance_refund`
--

DROP TABLE IF EXISTS `db_finance_refund`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_finance_refund` (
  `refund_id` bigint NOT NULL,
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
  `account` enum('Alipay','Cash','CompanyBank','Other','Wechat') COLLATE utf8mb4_general_ci NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `idempotency_fingerprint` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `idempotency_key` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `refund_at` date NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `receipt_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`refund_id`),
  UNIQUE KEY `uk_fin_refund_company_idempotency` (`com_id`,`idempotency_key`),
  KEY `idx_fin_refund_company` (`com_id`,`refund_id`),
  KEY `idx_fin_refund_project` (`com_id`,`project_id`,`refund_id`),
  KEY `idx_fin_refund_receipt` (`com_id`,`receipt_id`,`refund_id`),
  KEY `idx_fin_refund_customer` (`com_id`,`customer_id`,`refund_id`),
  KEY `idx_fin_refund_store` (`com_id`,`store_id`,`refund_id`),
  KEY `FKow89clx0hrdg3ly1mm77k6m6d` (`creator_id`),
  KEY `FKa9x93ekg239b7gb4gsmd7xlfs` (`customer_id`),
  KEY `FKqgcy3sr9uh84y4e0x54890i4h` (`owner_id`),
  KEY `FKitu8u14xos0by9r5otxk5ioxr` (`project_id`),
  KEY `FK4oe9f5fwk9as1lt6g3ffv9bll` (`receipt_id`),
  KEY `FKletxa51nk0e9si9ajoh19u7tw` (`store_id`),
  KEY `FKkfyaj6phm0divyjveqs8343e5` (`team_id`),
  CONSTRAINT `FK4oe9f5fwk9as1lt6g3ffv9bll` FOREIGN KEY (`receipt_id`) REFERENCES `db_finance_receipt` (`receipt_id`),
  CONSTRAINT `FKa9x93ekg239b7gb4gsmd7xlfs` FOREIGN KEY (`customer_id`) REFERENCES `cust_customer` (`cust_id`),
  CONSTRAINT `FKadg3y2jr760k090y2f52a221a` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKitu8u14xos0by9r5otxk5ioxr` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKkfyaj6phm0divyjveqs8343e5` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKletxa51nk0e9si9ajoh19u7tw` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKow89clx0hrdg3ly1mm77k6m6d` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKqgcy3sr9uh84y4e0x54890i4h` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_finance_refund_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_finance_refund_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `employee_commission_amount` decimal(14,2) NOT NULL,
  `employee_salary_amount` decimal(14,2) NOT NULL,
  `expense_amount` decimal(14,2) NOT NULL,
  `final_amount` decimal(14,2) NOT NULL,
  `gross_margin` decimal(8,2) DEFAULT NULL,
  `manual_cost_amount` decimal(14,2) NOT NULL,
  `manual_income_amount` decimal(14,2) NOT NULL,
  `material_amount` decimal(14,2) NOT NULL,
  `paid_amount` decimal(14,2) NOT NULL,
  `payable_amount` decimal(14,2) NOT NULL,
  `profit_amount` decimal(14,2) DEFAULT NULL,
  `project_amount` decimal(14,2) NOT NULL,
  `receivable_amount` decimal(14,2) NOT NULL,
  `received_amount` decimal(14,2) NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `retention_amount` decimal(14,2) NOT NULL,
  `retention_rate` decimal(6,2) NOT NULL,
  `retention_release_at` date DEFAULT NULL,
  `retention_released_at` date DEFAULT NULL,
  `reward_amount` decimal(14,2) NOT NULL,
  `salary_amount` decimal(14,2) NOT NULL,
  `status` enum('Closed','Confirmed','Draft','RetentionPending','RetentionReleased') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `batch_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_goods_receipt_images`
--

DROP TABLE IF EXISTS `db_goods_receipt_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_goods_receipt_images` (
  `receipt_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FK95i330n58tjnfb5mygbg5b5vx` (`receipt_id`),
  CONSTRAINT `FK95i330n58tjnfb5mygbg5b5vx` FOREIGN KEY (`receipt_id`) REFERENCES `db_goods_receipt` (`receipt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `batch_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_material_order_sheet`
--

DROP TABLE IF EXISTS `db_material_order_sheet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_material_order_sheet` (
  `sheet_id` bigint NOT NULL,
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
  `deliver_at` date DEFAULT NULL,
  `group_chat_at` date DEFAULT NULL,
  `install_at` date DEFAULT NULL,
  `manufacturer_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `measure_at` date DEFAULT NULL,
  `price_state` enum('Mixed','Priced','Settled','Unpriced') COLLATE utf8mb4_general_ci NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sheet_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `sheet_type` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `signatures` longtext COLLATE utf8mb4_general_ci,
  `status` enum('Arrived','Cancelled','Closed','Draft','Measured','Measuring','Ordered','PartiallyArrived','PartiallyOrdered','Pricing','Settled','Submitted') COLLATE utf8mb4_general_ci NOT NULL,
  `template_version` int DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `contract_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `material_category_id` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `supplier_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  `template_id` bigint DEFAULT NULL,
  PRIMARY KEY (`sheet_id`),
  UNIQUE KEY `uk_mos_company_no` (`com_id`,`sheet_no`),
  KEY `idx_mos_company` (`com_id`,`sheet_id`),
  KEY `idx_mos_company_project` (`com_id`,`project_id`,`sheet_id`),
  KEY `idx_mos_company_project_status` (`com_id`,`project_id`,`status`,`sheet_id`),
  KEY `idx_mos_company_store` (`com_id`,`store_id`,`sheet_id`),
  KEY `idx_mos_company_team` (`com_id`,`team_id`,`sheet_id`),
  KEY `idx_mos_company_owner` (`com_id`,`owner_id`,`sheet_id`),
  KEY `FKqq4axw77s1a7580xdmjsff2sa` (`contract_id`),
  KEY `FKlid7ylr9vyk89jg2xqwrujwg8` (`creator_id`),
  KEY `FK8nml91f3xxbcdt5ykwkh60xs1` (`material_category_id`),
  KEY `FK32233342k0jb14qgg7yvlab09` (`owner_id`),
  KEY `FKdjgsfk8nhk6shuqctf6j97qaj` (`project_id`),
  KEY `FK3kjf345a42k7een89gjmrv2s0` (`property_id`),
  KEY `FK92qeqxsfweluikx30jn52nbxl` (`store_id`),
  KEY `FKfo0gqqtukw2uxbved2y3cassw` (`supplier_id`),
  KEY `FKd5huc5i8w2u85q8ier9i0u9oi` (`team_id`),
  KEY `FKtlf8xr53i4ixgyuar1rtcbx7g` (`template_id`),
  CONSTRAINT `FK1xbpb819hfnkuio5wwn973vpj` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK32233342k0jb14qgg7yvlab09` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK3kjf345a42k7een89gjmrv2s0` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FK8nml91f3xxbcdt5ykwkh60xs1` FOREIGN KEY (`material_category_id`) REFERENCES `db_material_category` (`material_category_id`),
  CONSTRAINT `FK92qeqxsfweluikx30jn52nbxl` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKd5huc5i8w2u85q8ier9i0u9oi` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKdjgsfk8nhk6shuqctf6j97qaj` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKfo0gqqtukw2uxbved2y3cassw` FOREIGN KEY (`supplier_id`) REFERENCES `db_partner` (`partner_id`),
  CONSTRAINT `FKlid7ylr9vyk89jg2xqwrujwg8` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKqq4axw77s1a7580xdmjsff2sa` FOREIGN KEY (`contract_id`) REFERENCES `db_project_contract` (`contract_id`),
  CONSTRAINT `FKtlf8xr53i4ixgyuar1rtcbx7g` FOREIGN KEY (`template_id`) REFERENCES `db_material_sheet_template` (`sheet_template_id`),
  CONSTRAINT `db_material_order_sheet_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_material_order_sheet_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_material_order_sheet_attachments`
--

DROP TABLE IF EXISTS `db_material_order_sheet_attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_material_order_sheet_attachments` (
  `sheet_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FK6uvbgyd8w4dqko3qjouyqiaxk` (`sheet_id`),
  CONSTRAINT `FK6uvbgyd8w4dqko3qjouyqiaxk` FOREIGN KEY (`sheet_id`) REFERENCES `db_material_order_sheet` (`sheet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_material_order_sheet_item`
--

DROP TABLE IF EXISTS `db_material_order_sheet_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_material_order_sheet_item` (
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
  `brand` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `color` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `craft` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `glass` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `location_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `loss_quantity` decimal(12,2) NOT NULL,
  `loss_rate` decimal(8,4) NOT NULL,
  `material_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `model` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ordered_quantity` decimal(12,2) NOT NULL,
  `price_state` enum('Mixed','Priced','Settled','Unpriced') COLLATE utf8mb4_general_ci NOT NULL,
  `purchase_price` decimal(12,2) DEFAULT NULL,
  `quantity` decimal(12,2) NOT NULL,
  `quote_cost_price` decimal(12,2) DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `settlement_price` decimal(12,2) DEFAULT NULL,
  `size` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sort` int NOT NULL,
  `source_kind` enum('Plan') COLLATE utf8mb4_general_ci NOT NULL,
  `space_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `spec` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `standard_included` bit(1) NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `material_plan_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `sheet_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  KEY `idx_mosi_company_sheet` (`com_id`,`sheet_id`,`sort`),
  KEY `idx_mosi_company_plan` (`com_id`,`material_plan_id`),
  KEY `idx_mosi_company_project` (`com_id`,`project_id`,`item_id`),
  KEY `idx_mosi_company` (`com_id`,`item_id`),
  KEY `FKad3as6yryi97tkfil83fx5b7w` (`material_plan_id`),
  KEY `FKqa2cx0qcmv48qnulfmh0ocn09` (`owner_id`),
  KEY `FK2vm3s3orplol9hqo1o6fusn1u` (`project_id`),
  KEY `FK2dhh5ithoqtvdqp1y0f3x9u6f` (`property_id`),
  KEY `FK8mt8v0wukry76nvxkbxl2ci3w` (`sheet_id`),
  KEY `FKo44ai4mo0k1n9ih09vichm0e5` (`store_id`),
  KEY `FKi2hgyuseirx8vi7ut15cadddw` (`team_id`),
  CONSTRAINT `FK2dhh5ithoqtvdqp1y0f3x9u6f` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FK2vm3s3orplol9hqo1o6fusn1u` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FK8mt8v0wukry76nvxkbxl2ci3w` FOREIGN KEY (`sheet_id`) REFERENCES `db_material_order_sheet` (`sheet_id`),
  CONSTRAINT `FKad3as6yryi97tkfil83fx5b7w` FOREIGN KEY (`material_plan_id`) REFERENCES `db_material_plan` (`plan_id`),
  CONSTRAINT `FKi2hgyuseirx8vi7ut15cadddw` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKkf26h34579mncwpr3ie30l2bm` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKo44ai4mo0k1n9ih09vichm0e5` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKqa2cx0qcmv48qnulfmh0ocn09` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_material_order_sheet_item_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_material_order_sheet_item_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_material_order_sheet_item_attachments`
--

DROP TABLE IF EXISTS `db_material_order_sheet_item_attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_material_order_sheet_item_attachments` (
  `item_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKg09pw1jj5qrckjk5tnd7s8ek3` (`item_id`),
  CONSTRAINT `FKg09pw1jj5qrckjk5tnd7s8ek3` FOREIGN KEY (`item_id`) REFERENCES `db_material_order_sheet_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `material_key` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `net_quantity` decimal(12,2) DEFAULT NULL,
  `quantity` decimal(12,2) NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `required_at` date DEFAULT NULL,
  `selected_brand` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `selected_model` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `selected_spec` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source_type` enum('ChangeAdd','ChangeReduce','Selection') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Approved','Arrived','Cancelled','Closed','Ordered','PartiallyArrived','PartiallyOrdered','Planned','ReadyToOrder','Requested') COLLATE utf8mb4_general_ci NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci NOT NULL,
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
  KEY `idx_material_plan_company_project_status` (`com_id`,`project_id`,`status`,`plan_id`),
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `returned_at` date DEFAULT NULL,
  `status` enum('Cancelled','Confirmed','Pending') COLLATE utf8mb4_general_ci NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `content` tinytext COLLATE utf8mb4_general_ci NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `selection_json` longtext COLLATE utf8mb4_general_ci,
  `status` enum('Selected') COLLATE utf8mb4_general_ci NOT NULL,
  `title` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_material_selection_attachments`
--

DROP TABLE IF EXISTS `db_material_selection_attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_material_selection_attachments` (
  `selection_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKjokjkuvb9q6w4ke36yn2eneee` (`selection_id`),
  CONSTRAINT `FKjokjkuvb9q6w4ke36yn2eneee` FOREIGN KEY (`selection_id`) REFERENCES `db_material_selection` (`selection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_material_settlement`
--

DROP TABLE IF EXISTS `db_material_settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_material_settlement` (
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
  `cancel_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `period` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `scope` enum('Monthly','PerPurchaseOrder') COLLATE utf8mb4_general_ci NOT NULL,
  `settled_at` date DEFAULT NULL,
  `status` enum('Cancelled','Confirmed') COLLATE utf8mb4_general_ci NOT NULL,
  `total_amount` decimal(14,2) NOT NULL,
  `cancelled_by` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `confirmer_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `supplier_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`settlement_id`),
  UNIQUE KEY `uk_settlement_company_no` (`com_id`,`no`),
  KEY `idx_settlement_company` (`com_id`,`settlement_id`),
  KEY `idx_settlement_company_supplier` (`com_id`,`supplier_id`,`settlement_id`),
  KEY `idx_settlement_company_scope_period` (`com_id`,`scope`,`period`),
  KEY `idx_settlement_company_store` (`com_id`,`store_id`,`settlement_id`),
  KEY `idx_settlement_company_team` (`com_id`,`team_id`,`settlement_id`),
  KEY `idx_settlement_company_owner` (`com_id`,`owner_id`,`settlement_id`),
  KEY `FKkk3l9uim3km21jwaicwg2x7xh` (`cancelled_by`),
  KEY `FKel7w082onkvsrdl9xh36qxl5x` (`confirmer_id`),
  KEY `FK845ki9047jlankdyfhcy2t6uk` (`owner_id`),
  KEY `FKh1f4hbtqinlfkx2urxn67so74` (`project_id`),
  KEY `FKr9byhl573kc9ox9pdqkbm7vct` (`store_id`),
  KEY `FKernut7bw0g7b5egl401a6avpv` (`supplier_id`),
  KEY `FKd4m6362myfgd5ydlvuxukd52` (`team_id`),
  CONSTRAINT `FK26dihs6semr29j8bpns866ep0` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK845ki9047jlankdyfhcy2t6uk` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKd4m6362myfgd5ydlvuxukd52` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKel7w082onkvsrdl9xh36qxl5x` FOREIGN KEY (`confirmer_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKernut7bw0g7b5egl401a6avpv` FOREIGN KEY (`supplier_id`) REFERENCES `db_partner` (`partner_id`),
  CONSTRAINT `FKh1f4hbtqinlfkx2urxn67so74` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKkk3l9uim3km21jwaicwg2x7xh` FOREIGN KEY (`cancelled_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKr9byhl573kc9ox9pdqkbm7vct` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_material_settlement_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_material_settlement_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_material_settlement_item`
--

DROP TABLE IF EXISTS `db_material_settlement_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_material_settlement_item` (
  `settlement_item_id` bigint NOT NULL,
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
  `quantity` decimal(12,2) NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `settlement_price` decimal(12,2) NOT NULL,
  `com_id` bigint NOT NULL,
  `purchase_item_id` bigint NOT NULL,
  `settlement_id` bigint NOT NULL,
  PRIMARY KEY (`settlement_item_id`),
  UNIQUE KEY `uk_settlement_item_purchase_item` (`com_id`,`purchase_item_id`),
  KEY `idx_settlement_item_company` (`com_id`,`settlement_item_id`),
  KEY `idx_settlement_item_company_settlement` (`com_id`,`settlement_id`),
  KEY `FKb8lhl05ctrp1gwt6s23v8v6q9` (`purchase_item_id`),
  KEY `FKr01qgex1vxdrmmaxiubp8nehl` (`settlement_id`),
  CONSTRAINT `FKb8lhl05ctrp1gwt6s23v8v6q9` FOREIGN KEY (`purchase_item_id`) REFERENCES `db_purchase_order_item` (`purchase_item_id`),
  CONSTRAINT `FKja7xqw9f954fl76vf0osuycim` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKr01qgex1vxdrmmaxiubp8nehl` FOREIGN KEY (`settlement_id`) REFERENCES `db_material_settlement` (`settlement_id`),
  CONSTRAINT `db_material_settlement_item_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_material_settlement_item_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_material_sheet_template`
--

DROP TABLE IF EXISTS `db_material_sheet_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_material_sheet_template` (
  `sheet_template_id` bigint NOT NULL,
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
  `field_config` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `number_rule` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `print_template_id` bigint DEFAULT NULL,
  `published_version` int NOT NULL,
  `row_template_config` longtext COLLATE utf8mb4_general_ci,
  `rule_config` longtext COLLATE utf8mb4_general_ci,
  `scope` enum('Company','Platform') COLLATE utf8mb4_general_ci NOT NULL,
  `sheet_type` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `signature_config` longtext COLLATE utf8mb4_general_ci,
  `status` enum('Active','Disabled') COLLATE utf8mb4_general_ci NOT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `material_category_id` bigint DEFAULT NULL,
  PRIMARY KEY (`sheet_template_id`),
  UNIQUE KEY `uk_mst_company_type_version` (`com_id`,`sheet_type`,`version`),
  KEY `idx_mst_company` (`com_id`,`sheet_template_id`),
  KEY `idx_mst_company_scope` (`com_id`,`scope`,`sheet_type`),
  KEY `FKg9w772025t207ig3nvj7heo2g` (`creator_id`),
  KEY `FKh1hltptaqduw0bvb6j63gbtvu` (`material_category_id`),
  CONSTRAINT `FKg9w772025t207ig3nvj7heo2g` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKh1hltptaqduw0bvb6j63gbtvu` FOREIGN KEY (`material_category_id`) REFERENCES `db_material_category` (`material_category_id`),
  CONSTRAINT `FKt9sb7q47ayckc1wh11lo3cvcg` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_material_sheet_template_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_material_sheet_template_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `brand` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `effective_from` date DEFAULT NULL,
  `effective_to` date DEFAULT NULL,
  `lead_time_days` int NOT NULL,
  `material_key` varchar(512) COLLATE utf8mb4_general_ci NOT NULL,
  `material_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `minimum_order` decimal(12,3) NOT NULL,
  `model` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `purchase_price` decimal(12,2) NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `settlement_days` int DEFAULT NULL,
  `spec` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `customer_requirements` longtext COLLATE utf8mb4_general_ci,
  `designer_conclusion` longtext COLLATE utf8mb4_general_ci,
  `dimensions` longtext COLLATE utf8mb4_general_ci,
  `measure_time` datetime(6) DEFAULT NULL,
  `original_structure` longtext COLLATE utf8mb4_general_ci,
  `scheduled_at` datetime(6) DEFAULT NULL,
  `status` enum('Cancelled','Completed','Scheduled') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_measure_attachments`
--

DROP TABLE IF EXISTS `db_measure_attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_measure_attachments` (
  `measure_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKa6ivugx7c16r8pmfou17kp6ky` (`measure_id`),
  CONSTRAINT `FKa6ivugx7c16r8pmfou17kp6ky` FOREIGN KEY (`measure_id`) REFERENCES `db_measure` (`measure_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_measure_region`
--

DROP TABLE IF EXISTS `db_measure_region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_measure_region` (
  `region_id` bigint NOT NULL,
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
  `area` decimal(12,2) DEFAULT NULL,
  `area_source` enum('Estimated','Measured') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `height` decimal(10,3) DEFAULT NULL,
  `length_m` decimal(10,3) DEFAULT NULL,
  `measured_at` datetime(6) DEFAULT NULL,
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `perimeter` decimal(12,2) DEFAULT NULL,
  `remark` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `room_key` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `room_type` enum('Balcony','Bathroom','Bedroom','DiningRoom','Kitchen','LivingRoom') COLLATE utf8mb4_general_ci NOT NULL,
  `sort` int NOT NULL,
  `width` decimal(10,3) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `measure_id` bigint NOT NULL,
  `measured_by_id` bigint DEFAULT NULL,
  `emp_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`region_id`),
  KEY `idx_measure_region_company` (`com_id`,`region_id`),
  KEY `idx_measure_region_company_measure` (`com_id`,`measure_id`,`region_id`),
  KEY `idx_measure_region_company_property` (`com_id`,`property_id`,`region_id`),
  KEY `idx_measure_region_company_store` (`com_id`,`store_id`,`region_id`),
  KEY `idx_measure_region_company_owner` (`com_id`,`emp_id`,`region_id`),
  KEY `FKocxmeflr66cyxx3c5vbtjfqqf` (`measure_id`),
  KEY `FKfdode2bq5bsjfs3ni46trbfsq` (`measured_by_id`),
  KEY `FK5hl39cjdq8pe6mhc58bydhjc5` (`emp_id`),
  KEY `FK7v60cjkh0gin2cdkoy5xjakgt` (`property_id`),
  KEY `FKq7btr7g2yrcssbv5knvru4cv` (`store_id`),
  KEY `FKtmu5ku65f84goj8mk8dm3yuwt` (`team_id`),
  CONSTRAINT `FK5hl39cjdq8pe6mhc58bydhjc5` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK5pp3wwb3bq0ckxcgwtl1ul5x` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK7v60cjkh0gin2cdkoy5xjakgt` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FKfdode2bq5bsjfs3ni46trbfsq` FOREIGN KEY (`measured_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKocxmeflr66cyxx3c5vbtjfqqf` FOREIGN KEY (`measure_id`) REFERENCES `db_measure` (`measure_id`),
  CONSTRAINT `FKq7btr7g2yrcssbv5knvru4cv` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKtmu5ku65f84goj8mk8dm3yuwt` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `db_measure_region_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_measure_region_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_measure_site_photos`
--

DROP TABLE IF EXISTS `db_measure_site_photos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_measure_site_photos` (
  `measure_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKf6ok2r7lupb8wvvfrp98b4vpi` (`measure_id`),
  CONSTRAINT `FKf6ok2r7lupb8wvvfrp98b4vpi` FOREIGN KEY (`measure_id`) REFERENCES `db_measure` (`measure_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_measure_structure_images`
--

DROP TABLE IF EXISTS `db_measure_structure_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_measure_structure_images` (
  `measure_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKp73xpy8vw0iha4w8q5xrbhyyu` (`measure_id`),
  CONSTRAINT `FKp73xpy8vw0iha4w8q5xrbhyyu` FOREIGN KEY (`measure_id`) REFERENCES `db_measure` (`measure_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `biz_key` varchar(96) COLLATE utf8mb4_general_ci NOT NULL,
  `content` varchar(512) COLLATE utf8mb4_general_ci NOT NULL,
  `read_flag` bit(1) NOT NULL,
  `title` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('Customer','Overdue','Project','Quote','System','Task') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `addr` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `bank_account` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `bank_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `contact_name` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cutoff_day` int DEFAULT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `settlement_days` int DEFAULT NULL,
  `settlement_mode` enum('MonthlyCutoff','ReceiptDays') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `settlement_terms` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Active','Disabled') COLLATE utf8mb4_general_ci NOT NULL,
  `tax_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tel_no` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` enum('Factory','Installer','LaborTeam','Logistics','Service','Supplier') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_partner_brand`
--

DROP TABLE IF EXISTS `db_partner_brand`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_partner_brand` (
  `partner_id` bigint NOT NULL,
  `brand` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`partner_id`,`brand`),
  CONSTRAINT `FK3v4dahewhy5awflxcsm29ymgu` FOREIGN KEY (`partner_id`) REFERENCES `db_partner` (`partner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_payroll_attendance_summary`
--

DROP TABLE IF EXISTS `db_payroll_attendance_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_payroll_attendance_summary` (
  `summary_id` bigint NOT NULL,
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
  `absence_days` int NOT NULL,
  `annual_leave_days` int NOT NULL,
  `comp_leave_days` int NOT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `external_days` int NOT NULL,
  `normal_days` int NOT NULL,
  `payroll_month` varchar(7) COLLATE utf8mb4_general_ci NOT NULL,
  `personal_leave_days` int NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `scheduled_days` int NOT NULL,
  `sick_leave_days` int NOT NULL,
  `status` enum('Confirmed','Disputed','Draft','InPayroll','Pending','Rejected') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `confirmed_by` bigint DEFAULT NULL,
  `creator_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `store_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`summary_id`),
  UNIQUE KEY `uk_payroll_attendance_employee_month` (`com_id`,`emp_id`,`payroll_month`),
  KEY `idx_payroll_attendance_company` (`com_id`,`summary_id`),
  KEY `idx_payroll_attendance_employee` (`com_id`,`emp_id`,`payroll_month`,`summary_id`),
  KEY `idx_payroll_attendance_store` (`com_id`,`store_id`,`payroll_month`,`summary_id`),
  KEY `idx_payroll_attendance_team` (`com_id`,`team_id`,`payroll_month`,`summary_id`),
  KEY `FK939kvbgjosid6wshypfnkq7jw` (`confirmed_by`),
  KEY `FKikb38hefm79ybrivw2vam09xd` (`creator_id`),
  KEY `FKn5eyu8mity8ke3s5phehoei46` (`emp_id`),
  KEY `FKp4jk1xh1r71d9h12as3w4ihpi` (`store_id`),
  KEY `FKhbduu7jfiisyyxb6ml1xpa42i` (`team_id`),
  CONSTRAINT `FK939kvbgjosid6wshypfnkq7jw` FOREIGN KEY (`confirmed_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKbewge7cjim6crfpb9jqksqyrg` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKhbduu7jfiisyyxb6ml1xpa42i` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKikb38hefm79ybrivw2vam09xd` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKn5eyu8mity8ke3s5phehoei46` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKp4jk1xh1r71d9h12as3w4ihpi` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_payroll_attendance_summary_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_payroll_attendance_summary_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_payroll_commission_adjustment`
--

DROP TABLE IF EXISTS `db_payroll_commission_adjustment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_payroll_commission_adjustment` (
  `adjustment_id` bigint NOT NULL,
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
  `applied_amount` decimal(14,2) NOT NULL,
  `deduction_amount` decimal(14,2) NOT NULL,
  `idempotency_fingerprint` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `idempotency_key` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `occurred_at` date NOT NULL,
  `refund_amount` decimal(14,2) NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Cancelled','Included','Pending') COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('CrossPeriod','Refund','Resignation') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `original_commission_id` bigint NOT NULL,
  `payroll_item_id` bigint DEFAULT NULL,
  `project_id` bigint DEFAULT NULL,
  `refund_id` bigint DEFAULT NULL,
  `store_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`adjustment_id`),
  KEY `idx_payroll_adjustment_company` (`com_id`,`adjustment_id`),
  KEY `idx_payroll_adjustment_employee` (`com_id`,`emp_id`,`status`,`adjustment_id`),
  KEY `idx_payroll_adjustment_original` (`com_id`,`original_commission_id`,`adjustment_id`),
  KEY `idx_payroll_adjustment_project` (`com_id`,`project_id`,`adjustment_id`),
  KEY `idx_payroll_adjustment_payroll_item` (`com_id`,`payroll_item_id`,`adjustment_id`),
  KEY `idx_payroll_adjustment_store` (`com_id`,`store_id`,`adjustment_id`),
  KEY `FKgggw7071bp4j0ce5vwjvwopee` (`creator_id`),
  KEY `FKq8lyyn8p7tmp7rkqqmjvmstic` (`emp_id`),
  KEY `FKl3goyen4dtwyxdgi9wq5jweq0` (`original_commission_id`),
  KEY `FKsuw5m4pyyc4730fyu95l0dye1` (`payroll_item_id`),
  KEY `FK8idd5m78fqx2wtt4wuk1l1t7n` (`project_id`),
  KEY `FKbo2yt0nasjkhdb7c0xxei9oxn` (`refund_id`),
  KEY `FKfa4umww8r8oolequu5fphadun` (`store_id`),
  KEY `FKlfusn6u0qwgwq5vejvj4e187s` (`team_id`),
  CONSTRAINT `FK8idd5m78fqx2wtt4wuk1l1t7n` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKbo2yt0nasjkhdb7c0xxei9oxn` FOREIGN KEY (`refund_id`) REFERENCES `db_finance_refund` (`refund_id`),
  CONSTRAINT `FKfa4umww8r8oolequu5fphadun` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKgggw7071bp4j0ce5vwjvwopee` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKl3goyen4dtwyxdgi9wq5jweq0` FOREIGN KEY (`original_commission_id`) REFERENCES `db_payroll_commission_item` (`commission_id`),
  CONSTRAINT `FKlfusn6u0qwgwq5vejvj4e187s` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKo90k1mchh5i02gwo82n3emnal` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKq8lyyn8p7tmp7rkqqmjvmstic` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKsuw5m4pyyc4730fyu95l0dye1` FOREIGN KEY (`payroll_item_id`) REFERENCES `db_payroll_item` (`item_id`),
  CONSTRAINT `db_payroll_commission_adjustment_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_payroll_commission_adjustment_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_payroll_commission_item`
--

DROP TABLE IF EXISTS `db_payroll_commission_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_payroll_commission_item` (
  `commission_id` bigint NOT NULL,
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
  `accrued_amount` decimal(14,2) NOT NULL,
  `commission_amount` decimal(14,2) NOT NULL,
  `contract_amount` decimal(14,2) NOT NULL,
  `manual_rate` decimal(8,4) NOT NULL,
  `paid_amount` decimal(14,2) NOT NULL,
  `released_amount` decimal(14,2) NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Accrued','Adjusted','Closed','InPayment','Paid','PendingAccrual','Released') COLLATE utf8mb4_general_ci NOT NULL,
  `adjustment_of` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `payroll_item_id` bigint DEFAULT NULL,
  `project_id` bigint DEFAULT NULL,
  `store_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`commission_id`),
  KEY `idx_payroll_commission_company` (`com_id`,`commission_id`),
  KEY `idx_payroll_commission_employee` (`com_id`,`emp_id`,`status`,`commission_id`),
  KEY `idx_payroll_commission_project` (`com_id`,`project_id`,`commission_id`),
  KEY `idx_payroll_commission_store` (`com_id`,`store_id`,`commission_id`),
  KEY `FKmp0phjbs7ohkkflom7kwu2pqg` (`adjustment_of`),
  KEY `FKm0x7urmpnycew3373e9s97t5q` (`creator_id`),
  KEY `FK7969sxtc4rf9qx7jx0emvmpbv` (`emp_id`),
  KEY `FKp392rhoojjaexr7v3m8uosrdw` (`payroll_item_id`),
  KEY `FK3qyclokgxakd21h97kgev8379` (`project_id`),
  KEY `FKgp5kb9d8gney8mqvaun20attt` (`store_id`),
  KEY `FKnnxvii9j9fujux9t74c7j4rw2` (`team_id`),
  CONSTRAINT `FK3qyclokgxakd21h97kgev8379` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FK4se24h2dlrxfwhw8ao6y29b4j` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK7969sxtc4rf9qx7jx0emvmpbv` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKgp5kb9d8gney8mqvaun20attt` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKm0x7urmpnycew3373e9s97t5q` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKmp0phjbs7ohkkflom7kwu2pqg` FOREIGN KEY (`adjustment_of`) REFERENCES `db_payroll_commission_item` (`commission_id`),
  CONSTRAINT `FKnnxvii9j9fujux9t74c7j4rw2` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKp392rhoojjaexr7v3m8uosrdw` FOREIGN KEY (`payroll_item_id`) REFERENCES `db_payroll_item` (`item_id`),
  CONSTRAINT `db_payroll_commission_item_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_payroll_commission_item_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_payroll_commission_release`
--

DROP TABLE IF EXISTS `db_payroll_commission_release`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_payroll_commission_release` (
  `release_id` bigint NOT NULL,
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
  `released_at` date NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reverse_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reversed` bit(1) NOT NULL,
  `reversed_at` datetime(6) DEFAULT NULL,
  `commission_id` bigint NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `receipt_id` bigint NOT NULL,
  `reversed_by_id` bigint DEFAULT NULL,
  `store_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`release_id`),
  UNIQUE KEY `uk_payroll_release_commission_receipt` (`com_id`,`commission_id`,`receipt_id`),
  KEY `idx_payroll_release_company` (`com_id`,`release_id`),
  KEY `idx_payroll_release_commission` (`com_id`,`commission_id`,`release_id`),
  KEY `idx_payroll_release_employee` (`com_id`,`emp_id`,`release_id`),
  KEY `idx_payroll_release_receipt` (`com_id`,`receipt_id`,`release_id`),
  KEY `FKlufhvxnypg5u4a97n4dvpcrek` (`commission_id`),
  KEY `FKqafm2sh10aw1sixwrlg1yfjfn` (`creator_id`),
  KEY `FKg0hvy6j8sn1nvpdwwi782hx47` (`emp_id`),
  KEY `FK1scfgno1w9l87vqhc575nd1dm` (`receipt_id`),
  KEY `FKni5vaojcteskx7sjoi61k4ulh` (`reversed_by_id`),
  KEY `FKmqnc6st01br31vysv9pkwcoig` (`store_id`),
  KEY `FKr4ew09yfkayejskkdnm6q3caa` (`team_id`),
  CONSTRAINT `FK1scfgno1w9l87vqhc575nd1dm` FOREIGN KEY (`receipt_id`) REFERENCES `db_finance_receipt` (`receipt_id`),
  CONSTRAINT `FKg0hvy6j8sn1nvpdwwi782hx47` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKlufhvxnypg5u4a97n4dvpcrek` FOREIGN KEY (`commission_id`) REFERENCES `db_payroll_commission_item` (`commission_id`),
  CONSTRAINT `FKmqnc6st01br31vysv9pkwcoig` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKni5vaojcteskx7sjoi61k4ulh` FOREIGN KEY (`reversed_by_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKqafm2sh10aw1sixwrlg1yfjfn` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKr4ew09yfkayejskkdnm6q3caa` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKtmg1s1ja4hn0prcd0g3hqlaev` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_payroll_commission_release_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_payroll_commission_release_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_payroll_compensation_scheme`
--

DROP TABLE IF EXISTS `db_payroll_compensation_scheme`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_payroll_compensation_scheme` (
  `scheme_id` bigint NOT NULL,
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
  `base_salary` decimal(14,2) NOT NULL,
  `effective_from` date NOT NULL,
  `effective_to` date DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `scheduled_days` int NOT NULL,
  `status` enum('Active','Draft','Expired') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `store_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`scheme_id`),
  UNIQUE KEY `uk_payroll_comp_employee_effective` (`com_id`,`emp_id`,`effective_from`),
  KEY `idx_payroll_comp_company` (`com_id`,`scheme_id`),
  KEY `idx_payroll_comp_employee` (`com_id`,`emp_id`,`scheme_id`),
  KEY `idx_payroll_comp_store` (`com_id`,`store_id`,`scheme_id`),
  KEY `idx_payroll_comp_team` (`com_id`,`team_id`,`scheme_id`),
  KEY `FKqawjeo1iayolufxplphjmt1a7` (`creator_id`),
  KEY `FK527x5udp9rlbpwe9twv5aipc` (`emp_id`),
  KEY `FK3i72uraocv88teeykv1nxn710` (`store_id`),
  KEY `FKtmnppa99yynghvhu2t9aueatf` (`team_id`),
  CONSTRAINT `FK3i72uraocv88teeykv1nxn710` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FK527x5udp9rlbpwe9twv5aipc` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKmdycxjwhado1armsexfj5u1t0` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKqawjeo1iayolufxplphjmt1a7` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKtmnppa99yynghvhu2t9aueatf` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `db_payroll_compensation_scheme_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_payroll_compensation_scheme_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_payroll_item`
--

DROP TABLE IF EXISTS `db_payroll_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_payroll_item` (
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
  `allowance_amount` decimal(14,2) NOT NULL,
  `attendance_deduction` decimal(14,2) NOT NULL,
  `attribution` enum('Company','Project') COLLATE utf8mb4_general_ci NOT NULL,
  `attribution_key` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `base_salary_amount` decimal(14,2) NOT NULL,
  `bonus_amount` decimal(14,2) NOT NULL,
  `commission_amount` decimal(14,2) NOT NULL,
  `deduction_amount` decimal(14,2) NOT NULL,
  `gross_amount` decimal(14,2) NOT NULL,
  `net_amount` decimal(14,2) NOT NULL,
  `other_deduction` decimal(14,2) NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `social_insurance_amount` decimal(14,2) NOT NULL,
  `status` enum('Confirmed','Draft','Paid','Void') COLLATE utf8mb4_general_ci NOT NULL,
  `tax_amount` decimal(14,2) NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  `sheet_id` bigint NOT NULL,
  `store_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_payroll_item_sheet_employee_attribution` (`com_id`,`sheet_id`,`emp_id`,`attribution_key`),
  KEY `idx_payroll_item_company` (`com_id`,`item_id`),
  KEY `idx_payroll_item_sheet` (`com_id`,`sheet_id`,`item_id`),
  KEY `idx_payroll_item_employee` (`com_id`,`emp_id`,`sheet_id`,`item_id`),
  KEY `idx_payroll_item_project` (`com_id`,`project_id`,`item_id`),
  KEY `idx_payroll_item_store` (`com_id`,`store_id`,`item_id`),
  KEY `FKm4if3epho1rpbn043ajou1d9k` (`creator_id`),
  KEY `FKqn660vfxu0db7imxht63qdlqi` (`emp_id`),
  KEY `FKfjohr43fchiy51ogwi4l3fr8k` (`project_id`),
  KEY `FKiakia76lays7y2fa4khxc42x` (`sheet_id`),
  KEY `FKrof9jxq5kqcax4pub6fdwyrc5` (`store_id`),
  KEY `FK1km0nvijsqe9cu3skctdxyk3f` (`team_id`),
  CONSTRAINT `FK1km0nvijsqe9cu3skctdxyk3f` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKfjohr43fchiy51ogwi4l3fr8k` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKiakia76lays7y2fa4khxc42x` FOREIGN KEY (`sheet_id`) REFERENCES `db_payroll_sheet` (`sheet_id`),
  CONSTRAINT `FKm4if3epho1rpbn043ajou1d9k` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKqn660vfxu0db7imxht63qdlqi` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKrof9jxq5kqcax4pub6fdwyrc5` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKsa35quhm8fmj974dcnsqeoetb` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_payroll_item_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_payroll_item_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_payroll_payslip`
--

DROP TABLE IF EXISTS `db_payroll_payslip`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_payroll_payslip` (
  `payslip_id` bigint NOT NULL,
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
  `deduction_amount` decimal(14,2) NOT NULL,
  `gross_amount` decimal(14,2) NOT NULL,
  `net_amount` decimal(14,2) NOT NULL,
  `period` varchar(7) COLLATE utf8mb4_general_ci NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `status` enum('PendingPayment','Published','Void') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `emp_id` bigint NOT NULL,
  `sheet_id` bigint NOT NULL,
  `store_id` bigint NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`payslip_id`),
  UNIQUE KEY `uk_payroll_payslip_sheet_employee` (`com_id`,`sheet_id`,`emp_id`),
  KEY `idx_payroll_payslip_company` (`com_id`,`payslip_id`),
  KEY `idx_payroll_payslip_sheet` (`com_id`,`sheet_id`,`payslip_id`),
  KEY `idx_payroll_payslip_employee` (`com_id`,`emp_id`,`period`,`payslip_id`),
  KEY `idx_payroll_payslip_published` (`com_id`,`emp_id`,`published_at`,`payslip_id`),
  KEY `FKgdp8g6clqkyfj484bmwn11ky8` (`creator_id`),
  KEY `FKd852j2idhdtxkl73ibem441i4` (`emp_id`),
  KEY `FKoqriejvxmnwg7jv2px9k1mttk` (`sheet_id`),
  KEY `FKqvw4t8nq0yauiarsbef0clxy1` (`store_id`),
  KEY `FKbtys4oej7fq7tkkm6lylpe4yb` (`team_id`),
  CONSTRAINT `FK3ato6kbw9nh1cqmgi3k1ney93` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKbtys4oej7fq7tkkm6lylpe4yb` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKd852j2idhdtxkl73ibem441i4` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKgdp8g6clqkyfj484bmwn11ky8` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKoqriejvxmnwg7jv2px9k1mttk` FOREIGN KEY (`sheet_id`) REFERENCES `db_payroll_sheet` (`sheet_id`),
  CONSTRAINT `FKqvw4t8nq0yauiarsbef0clxy1` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `db_payroll_payslip_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_payroll_payslip_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_payroll_sheet`
--

DROP TABLE IF EXISTS `db_payroll_sheet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_payroll_sheet` (
  `sheet_id` bigint NOT NULL,
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
  `attendance_complete` bit(1) NOT NULL,
  `commission_complete` bit(1) NOT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `period` varchar(7) COLLATE utf8mb4_general_ci NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Closed','Confirmed','Draft','Paid','PartialPaid','Pending','Void') COLLATE utf8mb4_general_ci NOT NULL,
  `total_deduction_amount` decimal(14,2) NOT NULL,
  `total_gross_amount` decimal(14,2) NOT NULL,
  `total_net_amount` decimal(14,2) NOT NULL,
  `com_id` bigint NOT NULL,
  `confirmed_by` bigint DEFAULT NULL,
  `creator_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`sheet_id`),
  UNIQUE KEY `uk_payroll_sheet_company_period` (`com_id`,`period`),
  KEY `idx_payroll_sheet_company` (`com_id`,`sheet_id`),
  KEY `idx_payroll_sheet_period` (`com_id`,`period`,`sheet_id`),
  KEY `idx_payroll_sheet_owner` (`com_id`,`owner_id`,`sheet_id`),
  KEY `FKt0q030lqi4ipbpoh577ej75f4` (`confirmed_by`),
  KEY `FKsohlqcemejjx6kxi4lm268ma2` (`creator_id`),
  KEY `FKbghtmrihldjbecgqln66pgi8j` (`owner_id`),
  KEY `FKl5c80k57sx0dr45ae124lney3` (`store_id`),
  KEY `FK98sdncsbypkt3lsngu4ke4hjd` (`team_id`),
  CONSTRAINT `FK98sdncsbypkt3lsngu4ke4hjd` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKbghtmrihldjbecgqln66pgi8j` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKl5c80k57sx0dr45ae124lney3` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKr4xej9s5f41og4uvmjuy0nouw` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKsohlqcemejjx6kxi4lm268ma2` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKt0q030lqi4ipbpoh577ej75f4` FOREIGN KEY (`confirmed_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_payroll_sheet_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_payroll_sheet_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_platform_department`
--

DROP TABLE IF EXISTS `db_platform_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_platform_department` (
  `dept_id` bigint NOT NULL,
  `code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
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
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `sort` int NOT NULL,
  `status` enum('Active','Suspended') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_platform_onboarding_service`
--

DROP TABLE IF EXISTS `db_platform_onboarding_service`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_platform_onboarding_service` (
  `service_id` bigint NOT NULL,
  `contact_result` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
  `next_action` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  `next_follow_at` datetime(6) DEFAULT NULL,
  `remark` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `service_status` enum('Activated','Closed','Contacted','Pending','Waiting') COLLATE utf8mb4_general_ci NOT NULL,
  `target_com_id` bigint NOT NULL,
  `target_company_title` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `org_id` bigint NOT NULL,
  PRIMARY KEY (`service_id`),
  KEY `idx_platform_onboarding_company_time` (`org_id`,`target_com_id`,`create_date_time`,`service_id`),
  KEY `idx_platform_onboarding_operator_time` (`org_id`,`user_id`,`create_date_time`),
  KEY `FK5lek45ka0psvgaq9fbkqdiil2` (`user_id`),
  CONSTRAINT `FK5lek45ka0psvgaq9fbkqdiil2` FOREIGN KEY (`user_id`) REFERENCES `db_platform_user` (`user_id`),
  CONSTRAINT `FKhj8lkc34nqmngme5mrpk6gg6o` FOREIGN KEY (`org_id`) REFERENCES `db_platform_org` (`org_id`),
  CONSTRAINT `db_platform_onboarding_service_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_platform_operation_log`
--

DROP TABLE IF EXISTS `db_platform_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_platform_operation_log` (
  `log_id` bigint NOT NULL,
  `action` enum('Freeze','OnboardingServiceUpdate','OwnerPhoneReveal','PlanChange','Renew','SeatAdjust','StatusChange','StoreAdjust','Unfreeze') COLLATE utf8mb4_general_ci NOT NULL,
  `after_value` varchar(2048) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `before_value` varchar(2048) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
  `reason` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  `summary` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `target_com_id` bigint DEFAULT NULL,
  `target_company_title` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `target_key` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `target_sub_id` bigint DEFAULT NULL,
  `target_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `org_id` bigint NOT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_platform_log_org_time` (`org_id`,`create_date_time`,`log_id`),
  KEY `idx_platform_log_operator_time` (`user_id`,`create_date_time`,`log_id`),
  KEY `idx_platform_log_target_company` (`target_com_id`,`log_id`),
  CONSTRAINT `FK507arvwcnic39e7iahn073ei7` FOREIGN KEY (`user_id`) REFERENCES `db_platform_user` (`user_id`),
  CONSTRAINT `FKjdw3mjsflb1w7gv340km34b5n` FOREIGN KEY (`org_id`) REFERENCES `db_platform_org` (`org_id`),
  CONSTRAINT `db_platform_operation_log_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_platform_org`
--

DROP TABLE IF EXISTS `db_platform_org`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_platform_org` (
  `org_id` bigint NOT NULL,
  `code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
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
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `status` enum('Active','Suspended') COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`org_id`),
  UNIQUE KEY `uk_platform_org_code` (`code`),
  CONSTRAINT `db_platform_org_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `password_hash` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `role` enum('Auditor','BillingOperator','CustomerSuccess','GrowthOperator','OperationsManager','SreOperator','SuperAdmin') COLLATE utf8mb4_general_ci NOT NULL,
  `status` enum('Active','Locked','Suspended') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `baseline_progress` int DEFAULT NULL,
  `business_origin` enum('Carryover','New') COLLATE utf8mb4_general_ci NOT NULL,
  `contract_amount` decimal(12,2) NOT NULL,
  `cost` decimal(12,2) NOT NULL,
  `expected_end_date` date DEFAULT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `plan_end_date` date DEFAULT NULL,
  `progress` int NOT NULL,
  `site_addr` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `start_date` date DEFAULT NULL,
  `status` enum('Accepting','Finished','Ongoing','Preparing','Settled','Suspended') COLLATE utf8mb4_general_ci NOT NULL,
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
  KEY `idx_project_company_designer_status` (`com_id`,`designer_id`,`status`,`project_id`),
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `business_origin` enum('Carryover','New') COLLATE utf8mb4_general_ci NOT NULL,
  `current_amount` decimal(14,2) NOT NULL,
  `effective_at` datetime(6) DEFAULT NULL,
  `no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `payment_terms` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `plan_end_date` date DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `signed_amount` decimal(14,2) DEFAULT NULL,
  `signed_at` date DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `status` enum('Active','Changed','Changing','Closed','Completed','Draft','Effective','PendingSign','Settled','Signed','Terminated') COLLATE utf8mb4_general_ci NOT NULL,
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
  KEY `idx_project_contract_company_customer_status` (`com_id`,`customer_id`,`status`,`contract_id`),
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_project_owner_preview_log`
--

DROP TABLE IF EXISTS `db_project_owner_preview_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_project_owner_preview_log` (
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
  `employee_name` varchar(15) COLLATE utf8mb4_general_ci NOT NULL,
  `employee_role` enum('Boss','Designer','Finance','Marketer','Pm','StoreManager','TeamLeader') COLLATE utf8mb4_general_ci NOT NULL,
  `opened_at` datetime(6) NOT NULL,
  `request_id` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `requested_project_id` bigint NOT NULL,
  `result` enum('Error','Forbidden','NotFound','Success') COLLATE utf8mb4_general_ci NOT NULL,
  `source` enum('App') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `employee_id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_owner_preview_company_project` (`com_id`,`project_id`,`log_id`),
  KEY `idx_owner_preview_company_requested` (`com_id`,`requested_project_id`,`log_id`),
  KEY `idx_owner_preview_company_employee` (`com_id`,`employee_id`,`log_id`),
  KEY `idx_owner_preview_company_opened` (`com_id`,`opened_at`),
  KEY `FK5skca8vecua7idlp0jrisrpgj` (`employee_id`),
  KEY `FK6p77thmvhevtcrjxk2ixgtb0l` (`project_id`),
  CONSTRAINT `FK5skca8vecua7idlp0jrisrpgj` FOREIGN KEY (`employee_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK6p77thmvhevtcrjxk2ixgtb0l` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKluk8t5ttgv78mkvjuxvdmlwcw` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_project_owner_preview_log_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_project_owner_preview_log_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_project_plan_deviation`
--

DROP TABLE IF EXISTS `db_project_plan_deviation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_project_plan_deviation` (
  `deviation_id` bigint NOT NULL,
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
  `base_end_date` date NOT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `deviation_days` int DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `expected_end_date` date NOT NULL,
  `new_base_end_date` date DEFAULT NULL,
  `reason` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reason_type` enum('Customer','Internal') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `receivable_recalculated` bit(1) NOT NULL,
  `schedule_strategy` enum('Compress','Extend') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Confirmed','Pending','Rejected','RoutedToChange','Superseded') COLLATE utf8mb4_general_ci NOT NULL,
  `visible_to_customer` bit(1) NOT NULL,
  `com_id` bigint NOT NULL,
  `confirmed_by` bigint DEFAULT NULL,
  `contract_id` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `property_id` bigint NOT NULL,
  `responsible_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`deviation_id`),
  KEY `idx_plan_deviation_company` (`com_id`,`deviation_id`),
  KEY `idx_plan_deviation_company_store` (`com_id`,`store_id`,`deviation_id`),
  KEY `idx_plan_deviation_company_team` (`com_id`,`team_id`,`deviation_id`),
  KEY `idx_plan_deviation_company_owner` (`com_id`,`owner_id`,`deviation_id`),
  KEY `idx_plan_deviation_company_project_status` (`com_id`,`project_id`,`status`,`deviation_id`),
  KEY `FK9ur7p30nlv04ey80gajjmn5o7` (`confirmed_by`),
  KEY `FKsgs3xkswyx9h7r5p1h73b5h1f` (`contract_id`),
  KEY `FK1eyaaxcenjh484nd7m2mhviad` (`owner_id`),
  KEY `FK3lnin8pi7l6bjvtqhm7xyi20p` (`project_id`),
  KEY `FK2rvx2prhixdufu712k1c1vvjd` (`property_id`),
  KEY `FKo13ukb5ytkulkf8pb4rp594dy` (`responsible_id`),
  KEY `FKjjcpnt3goj57wjnw16fnlsqwb` (`store_id`),
  KEY `FKnf21udfl6w5hot94pdfnwy1dx` (`team_id`),
  CONSTRAINT `FK1eyaaxcenjh484nd7m2mhviad` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK2rvx2prhixdufu712k1c1vvjd` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `FK3lnin8pi7l6bjvtqhm7xyi20p` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FK9n72khvw9745gxnbmp2irppu9` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK9ur7p30nlv04ey80gajjmn5o7` FOREIGN KEY (`confirmed_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKjjcpnt3goj57wjnw16fnlsqwb` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `FKnf21udfl6w5hot94pdfnwy1dx` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKo13ukb5ytkulkf8pb4rp594dy` FOREIGN KEY (`responsible_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKsgs3xkswyx9h7r5p1h73b5h1f` FOREIGN KEY (`contract_id`) REFERENCES `db_project_contract` (`contract_id`),
  CONSTRAINT `db_project_plan_deviation_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_project_plan_deviation_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `author_role` enum('Boss','Designer','Finance','Marketer','Pm','StoreManager','TeamLeader') COLLATE utf8mb4_general_ci NOT NULL,
  `content` varchar(1024) COLLATE utf8mb4_general_ci NOT NULL,
  `customer_summary` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `title` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('AcceptDone','AcceptRequest','Checkin','Issue','Material','Notice','Progress','StageOpen','TaskDone') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_project_report_image`
--

DROP TABLE IF EXISTS `db_project_report_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_project_report_image` (
  `report_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKex18ma2vnq63c4j8w12o20kxh` (`report_id`),
  CONSTRAINT `FKex18ma2vnq63c4j8w12o20kxh` FOREIGN KEY (`report_id`) REFERENCES `db_project_report` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `acceptance_mode` enum('AcceptanceDone','ConstructionDone','None') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `actual_end` date DEFAULT NULL,
  `actual_start` date DEFAULT NULL,
  `baseline_progress` int DEFAULT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `not_applicable_at` datetime(6) DEFAULT NULL,
  `not_applicable_reason` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `planned_end` date DEFAULT NULL,
  `planned_start` date DEFAULT NULL,
  `progress` int NOT NULL,
  `sort` int NOT NULL,
  `stage_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Accepting','Doing','Done','NotApplicable','Todo') COLLATE utf8mb4_general_ci NOT NULL,
  `weight` decimal(10,2) NOT NULL,
  `com_id` bigint NOT NULL,
  `not_applicable_by` bigint DEFAULT NULL,
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
  KEY `idx_project_stage_company_project_status` (`com_id`,`project_id`,`status`,`stage_id`),
  KEY `FKes1h4d33jrkmrfqny5qscshtf` (`not_applicable_by`),
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
  CONSTRAINT `FKes1h4d33jrkmrfqny5qscshtf` FOREIGN KEY (`not_applicable_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKq1uwkxcqto8o60wdsm9qvhnre` FOREIGN KEY (`property_id`) REFERENCES `db_property` (`property_id`),
  CONSTRAINT `db_project_stage_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_project_stage_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `craft` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `planned_end` date DEFAULT NULL,
  `planned_start` date DEFAULT NULL,
  `priority` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `progress` int NOT NULL,
  `quantity` decimal(12,2) NOT NULL,
  `remark` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sale_price` decimal(12,2) NOT NULL,
  `spec` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Accepting','Cancelled','Doing','Done','Todo') COLLATE utf8mb4_general_ci NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci NOT NULL,
  `zone` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_project_task_evidence`
--

DROP TABLE IF EXISTS `db_project_task_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_project_task_evidence` (
  `task_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKrgp77ukyri1hy5hk2v2y0e4g0` (`task_id`),
  CONSTRAINT `FKrgp77ukyri1hy5hk2v2y0e4g0` FOREIGN KEY (`task_id`) REFERENCES `db_project_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `address` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `area` decimal(10,2) DEFAULT NULL,
  `building` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `community` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `latest_measure_at` datetime(6) DEFAULT NULL,
  `layout` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `property_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `remark` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `room_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Completed','Contracted','Designing','Measured','Measuring','PendingMeasure','Quoted','Serving') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `approval_remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `delivery_mode` enum('Direct','Warehouse') COLLATE utf8mb4_general_ci NOT NULL,
  `expected_at` date DEFAULT NULL,
  `no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `ordered_at` date DEFAULT NULL,
  `price_state` enum('Mixed','Priced','Settled','Unpriced') COLLATE utf8mb4_general_ci NOT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `received_quantity` decimal(12,3) NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Approved','Cancelled','Closed','Draft','Ordered','PartiallyReceived','Received','Rejected','Submitted') COLLATE utf8mb4_general_ci NOT NULL,
  `supplier_name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `price_source` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `price_state` enum('Mixed','Priced','Settled','Unpriced') COLLATE utf8mb4_general_ci NOT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `received_quantity` decimal(12,3) NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `settlement_price` decimal(12,2) DEFAULT NULL,
  `status` enum('Approved','Cancelled','Closed','Draft','Ordered','PartiallyReceived','Received','Rejected','Submitted') COLLATE utf8mb4_general_ci NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `action` enum('Assign','Claim','Close','Comment','Reopen','Urge','Void') COLLATE utf8mb4_general_ci NOT NULL,
  `from_status` enum('Closed','Handling','Pending','Void') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `note` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `occurred_at` datetime(6) DEFAULT NULL,
  `to_status` enum('Closed','Handling','Pending','Void') COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `alert_key` varchar(160) COLLATE utf8mb4_general_ci NOT NULL,
  `alert_type` enum('AbnormalChangeRate','MaterialOverIssue','OverdueReceivable','ProjectSuspensionRisk') COLLATE utf8mb4_general_ci NOT NULL,
  `assigned_at` datetime(6) DEFAULT NULL,
  `business_id` bigint DEFAULT NULL,
  `business_type` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `close_result` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `closed_at` datetime(6) DEFAULT NULL,
  `discovered_at` datetime(6) DEFAULT NULL,
  `due_at` datetime(6) DEFAULT NULL,
  `level_at_create` enum('High','Low','Medium') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `recurrence_count` int DEFAULT NULL,
  `reopen_reason` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `round_no` int DEFAULT NULL,
  `status` enum('Closed','Handling','Pending','Void') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_risk_log_evidence`
--

DROP TABLE IF EXISTS `db_risk_log_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_risk_log_evidence` (
  `log_id` bigint NOT NULL,
  `evidence_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FK8ajqu6wsgjef0wkx5mahfvrxq` (`log_id`),
  CONSTRAINT `FK8ajqu6wsgjef0wkx5mahfvrxq` FOREIGN KEY (`log_id`) REFERENCES `db_risk_disposition_log` (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_site_lead`
--

DROP TABLE IF EXISTS `db_site_lead`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_site_lead` (
  `lead_id` bigint NOT NULL,
  `consent` bit(1) NOT NULL,
  `contact` varchar(120) COLLATE utf8mb4_general_ci NOT NULL,
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
  `contact_name` varchar(80) COLLATE utf8mb4_general_ci NOT NULL,
  `note` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `page_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `status` enum('Closed','Contacted','New') COLLATE utf8mb4_general_ci NOT NULL,
  `topic` varchar(80) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`lead_id`),
  KEY `idx_site_lead_status_time` (`status`,`create_date_time`,`lead_id`),
  CONSTRAINT `db_site_lead_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` enum('Adjustment','Inbound','Issue','Loss','ReturnIn','ReturnOut') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_stock_movement_image`
--

DROP TABLE IF EXISTS `db_stock_movement_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_stock_movement_image` (
  `movement_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FK7mnx1erit9eb99il88tmd59cn` (`movement_id`),
  CONSTRAINT `FK7mnx1erit9eb99il88tmd59cn` FOREIGN KEY (`movement_id`) REFERENCES `db_stock_movement` (`movement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `node_key` enum('Conceal','Finish','Start') COLLATE utf8mb4_general_ci NOT NULL,
  `note` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_task_node_media`
--

DROP TABLE IF EXISTS `db_task_node_media`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_task_node_media` (
  `node_id` bigint NOT NULL,
  `url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKa1td6i6wsyv0p88p8xxkxmirv` (`node_id`),
  CONSTRAINT `FKa1td6i6wsyv0p88p8xxkxmirv` FOREIGN KEY (`node_id`) REFERENCES `db_task_node` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_tenant_balance_ledger`
--

DROP TABLE IF EXISTS `db_tenant_balance_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_balance_ledger` (
  `ledger_id` bigint NOT NULL,
  `balance_after` int NOT NULL,
  `biz_type` enum('ManualAdjust','RechargeGrant','TrialGrant') COLLATE utf8mb4_general_ci NOT NULL,
  `change_days` int NOT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `remark` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  PRIMARY KEY (`ledger_id`),
  KEY `FK21wr1akfvsfycd7gjrec2scj2` (`com_id`),
  KEY `FKispx3m9qwjtpdkcxjpq0e3mem` (`order_id`),
  CONSTRAINT `FK21wr1akfvsfycd7gjrec2scj2` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKispx3m9qwjtpdkcxjpq0e3mem` FOREIGN KEY (`order_id`) REFERENCES `db_tenant_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_tenant_company`
--

DROP TABLE IF EXISTS `db_tenant_company`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_company` (
  `com_id` bigint NOT NULL,
  `addr` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `color` varchar(12) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
  `logo` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `after_sale_tel` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `brand_primary` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `custodian_credit` bit(1) DEFAULT NULL,
  `enable_store` bit(1) DEFAULT NULL,
  `grade_config` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `intro` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `qualification` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `service_area` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `service_tel` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `uscc` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `size` enum('A','B','C','D') COLLATE utf8mb4_general_ci NOT NULL,
  `title` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`com_id`),
  CONSTRAINT `db_tenant_company_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `id_key` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `id_secret` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `last_time` datetime(6) DEFAULT NULL,
  `locked_until` datetime(6) DEFAULT NULL,
  `must_change_password` bit(1) NOT NULL DEFAULT b'0',
  `password_hash` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `password_updated_at` datetime(6) DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'Active',
  `type` enum('Password','Wx') COLLATE utf8mb4_general_ci NOT NULL,
  `emp_id` bigint DEFAULT NULL,
  PRIMARY KEY (`cre_id`),
  UNIQUE KEY `UK9svmdwraex6pgbbgujcq6iten` (`id_key`),
  UNIQUE KEY `UKg6ywcsictc2pgk900c3i99wqc` (`phone`),
  KEY `FK8aosmicij0xds6vq1gmubxo6` (`emp_id`),
  CONSTRAINT `FK8aosmicij0xds6vq1gmubxo6` FOREIGN KEY (`emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_tenant_credentials_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_tenant_employee`
--

DROP TABLE IF EXISTS `db_tenant_employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_employee` (
  `emp_id` bigint NOT NULL,
  `avatar_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
  `name` varchar(15) COLLATE utf8mb4_general_ci NOT NULL,
  `resign_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `resigned_at` datetime(6) DEFAULT NULL,
  `role` enum('Boss','Designer','Finance','Marketer','Pm','StoreManager','TeamLeader') COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'Active',
  `tel_no` varchar(12) COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `invite_name` varchar(15) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `rejected_at` datetime(6) DEFAULT NULL,
  `role` enum('Boss','Designer','Finance','Marketer','Pm','StoreManager','TeamLeader') COLLATE utf8mb4_general_ci NOT NULL,
  `status` enum('Accepted','Cancelled','Expired','Pending','Rejected') COLLATE utf8mb4_general_ci NOT NULL,
  `tel_no` varchar(11) COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `order_no` varchar(40) COLLATE utf8mb4_general_ci NOT NULL,
  `out_trade_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `paid_time` datetime(6) DEFAULT NULL,
  `pay_channel` enum('Alipay','Offline','Wx') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Cancelled','Expired','Granted','Paid','Pending','Refunded') COLLATE utf8mb4_general_ci NOT NULL,
  `transaction_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `item_type` enum('Plan','Seats','Stores') COLLATE utf8mb4_general_ci NOT NULL,
  `product_code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `product_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_tenant_plan`
--

DROP TABLE IF EXISTS `db_tenant_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_plan` (
  `plan_id` bigint NOT NULL,
  `badge` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
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
  `description` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `highlights` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `max_seats` int DEFAULT NULL,
  `max_stores` int NOT NULL,
  `modules` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `one_time_only` bit(1) NOT NULL,
  `original_price` bigint DEFAULT NULL,
  `price` bigint NOT NULL,
  `priority` int NOT NULL,
  `recommended` bit(1) NOT NULL,
  `tagline` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_tenant_plan_addon`
--

DROP TABLE IF EXISTS `db_tenant_plan_addon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_plan_addon` (
  `addon_id` bigint NOT NULL,
  `code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `days` int NOT NULL,
  `description` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `max_quantity` int NOT NULL,
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `priority` int NOT NULL,
  `type` enum('Seats','Stores') COLLATE utf8mb4_general_ci NOT NULL,
  `unit_price` bigint NOT NULL,
  PRIMARY KEY (`addon_id`),
  UNIQUE KEY `UKrg1qdyfgnh5dkso5nhqlehuq8` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `description` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `max_seats` int NOT NULL,
  `max_stores` int NOT NULL,
  `modules` varchar(512) COLLATE utf8mb4_general_ci NOT NULL,
  `original_price` bigint DEFAULT NULL,
  `plan_code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `plan_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `price` bigint NOT NULL,
  `version_no` int NOT NULL,
  `plan_id` bigint NOT NULL,
  PRIMARY KEY (`version_id`),
  UNIQUE KEY `uk_plan_version` (`plan_id`,`version_no`),
  KEY `idx_plan_version_plan` (`plan_id`,`version_no`),
  CONSTRAINT `FKq1pxt8a8g963rfgomvbmwm4y8` FOREIGN KEY (`plan_id`) REFERENCES `db_tenant_plan` (`plan_id`),
  CONSTRAINT `db_tenant_plan_version_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_tenant_store`
--

DROP TABLE IF EXISTS `db_tenant_store`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_store` (
  `store_id` bigint NOT NULL,
  `addr` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `color` varchar(12) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
  `name` varchar(16) COLLATE utf8mb4_general_ci NOT NULL,
  `sort` int NOT NULL,
  `status` enum('Active','Disabled') COLLATE utf8mb4_general_ci NOT NULL,
  `company_com_id` bigint NOT NULL,
  PRIMARY KEY (`store_id`),
  KEY `FKiv3if8umjugca837cg5whj9j8` (`company_com_id`),
  CONSTRAINT `FKiv3if8umjugca837cg5whj9j8` FOREIGN KEY (`company_com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_tenant_store_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `status` enum('Active','Closed','Expired','Frozen','Trial') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_tenant_team`
--

DROP TABLE IF EXISTS `db_tenant_team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_tenant_team` (
  `team_id` bigint NOT NULL,
  `color` varchar(12) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_user_security_log`
--

DROP TABLE IF EXISTS `db_user_security_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_user_security_log` (
  `log_id` bigint NOT NULL,
  `action` enum('AccountLock','AccountUnlock','BossTransfer','CompanyJoin','CompanyResign','CompanySwitch','InviteAccept','InviteReject','LimitTrigger','Login','Logout','PasswordChange','PasswordSet','PhoneBind','Register','StoreStatusChange') COLLATE utf8mb4_general_ci NOT NULL,
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
  `ip` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `open_id` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `result` enum('Blocked','Failure','Success') COLLATE utf8mb4_general_ci NOT NULL,
  `user_agent` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_security_log_user` (`user_id`,`create_date_time`),
  KEY `idx_security_log_open` (`open_id`,`create_date_time`),
  KEY `idx_security_log_phone` (`phone`,`create_date_time`),
  CONSTRAINT `db_user_security_log_chk_1` CHECK ((`create_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `account` enum('Alipay','Cash','CompanyBank','Other','Wechat') COLLATE utf8mb4_general_ci NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `business_date` date NOT NULL,
  `category` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source_id` bigint NOT NULL,
  `source_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `source_type` enum('Contract','Material','Payment','Quote','Receipt') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `amount_words` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `content_type` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `file_name` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `generated_at` datetime(6) NOT NULL,
  `party_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `path` varchar(512) COLLATE utf8mb4_general_ci NOT NULL,
  `sha256` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `size` bigint NOT NULL,
  `status` enum('Active','Voided') COLLATE utf8mb4_general_ci NOT NULL,
  `template_version` int NOT NULL,
  `title` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `total_amount` decimal(14,2) NOT NULL,
  `type` enum('Contract','Material','Payment','Quote','Receipt') COLLATE utf8mb4_general_ci NOT NULL,
  `voucher_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_voucher_revision`
--

DROP TABLE IF EXISTS `db_voucher_revision`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_voucher_revision` (
  `revision_id` bigint NOT NULL,
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
  `change_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `changed_at` datetime(6) NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `after_worker_id` bigint DEFAULT NULL,
  `before_worker_id` bigint DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `operator_id` bigint NOT NULL,
  `voucher_id` bigint NOT NULL,
  PRIMARY KEY (`revision_id`),
  KEY `idx_voucher_revision_company` (`com_id`,`revision_id`),
  KEY `idx_voucher_revision_voucher` (`com_id`,`voucher_id`,`revision_id`),
  KEY `idx_voucher_revision_operator` (`com_id`,`operator_id`,`revision_id`),
  KEY `FKcmfan8q3ta6ilp0af6kj7lb8e` (`after_worker_id`),
  KEY `FK8xe39h258ohjcdqihrop6h8i0` (`before_worker_id`),
  KEY `FKtpjl9b20aygg7ig09up2xj2` (`operator_id`),
  KEY `FKow3pkchvc5bapl0gtpp9l7xk5` (`voucher_id`),
  CONSTRAINT `FK51ewrp3rk727ueih4a0xrbpg3` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FK8xe39h258ohjcdqihrop6h8i0` FOREIGN KEY (`before_worker_id`) REFERENCES `db_worker` (`worker_id`),
  CONSTRAINT `FKcmfan8q3ta6ilp0af6kj7lb8e` FOREIGN KEY (`after_worker_id`) REFERENCES `db_worker` (`worker_id`),
  CONSTRAINT `FKow3pkchvc5bapl0gtpp9l7xk5` FOREIGN KEY (`voucher_id`) REFERENCES `db_voucher_record` (`voucher_id`),
  CONSTRAINT `FKtpjl9b20aygg7ig09up2xj2` FOREIGN KEY (`operator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `db_voucher_revision_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_voucher_revision_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `bank_account` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `bank_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `id_card_no` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Active','Disabled') COLLATE utf8mb4_general_ci NOT NULL,
  `trade` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`worker_id`),
  KEY `idx_worker_company` (`com_id`,`worker_id`),
  KEY `idx_worker_company_id_card` (`com_id`,`id_card_no`),
  CONSTRAINT `FKq1yaxhpd4k9ay0t1ddp0mcob8` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `db_worker_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `db_worker_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `design_template`
--

DROP TABLE IF EXISTS `design_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `design_template` (
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
  `remark` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `scope` enum('Company','Personal','Store') COLLATE utf8mb4_general_ci NOT NULL,
  `status` enum('Disabled','Draft','Published') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  `current_version_id` bigint DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  `source_template_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  PRIMARY KEY (`template_id`),
  KEY `FK4r04sb1n63877akufg0gvug1r` (`com_id`),
  KEY `FKdxp2fikl17b9qad9ujj8rq7u5` (`current_version_id`),
  KEY `FKje2dka2s86juj395xjae7qdii` (`owner_id`),
  KEY `FK26y9i4cujko4rr70ii2jjbjdy` (`source_template_id`),
  KEY `FKkogx99pc3ptb0j0u2cj1wcuug` (`store_id`),
  CONSTRAINT `FK26y9i4cujko4rr70ii2jjbjdy` FOREIGN KEY (`source_template_id`) REFERENCES `design_template` (`template_id`),
  CONSTRAINT `FK4r04sb1n63877akufg0gvug1r` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKdxp2fikl17b9qad9ujj8rq7u5` FOREIGN KEY (`current_version_id`) REFERENCES `design_template_version` (`version_id`),
  CONSTRAINT `FKje2dka2s86juj395xjae7qdii` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKkogx99pc3ptb0j0u2cj1wcuug` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`),
  CONSTRAINT `design_template_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `design_template_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `design_template_item`
--

DROP TABLE IF EXISTS `design_template_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `design_template_item` (
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
  `default_quantity` decimal(12,3) DEFAULT NULL,
  `quantity_formula` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `room_key` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sort_no` int NOT NULL,
  `catalog_item_id` bigint NOT NULL,
  `com_id` bigint NOT NULL,
  `version_id` bigint NOT NULL,
  PRIMARY KEY (`item_id`),
  KEY `FK24e5d6xbui68r5davto3s9m5q` (`catalog_item_id`),
  KEY `FK36edo5d3e53g0w6817174e45c` (`com_id`),
  KEY `FKm3032y8prhxsfl16va4g2jnef` (`version_id`),
  CONSTRAINT `FK24e5d6xbui68r5davto3s9m5q` FOREIGN KEY (`catalog_item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FK36edo5d3e53g0w6817174e45c` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKm3032y8prhxsfl16va4g2jnef` FOREIGN KEY (`version_id`) REFERENCES `design_template_version` (`version_id`),
  CONSTRAINT `design_template_item_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `design_template_item_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `design_template_room`
--

DROP TABLE IF EXISTS `design_template_room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `design_template_room` (
  `room_id` bigint NOT NULL,
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
  `room_count` int NOT NULL,
  `door_area_total` decimal(10,3) DEFAULT NULL,
  `door_width_total` decimal(10,3) DEFAULT NULL,
  `height` decimal(10,3) DEFAULT NULL,
  `length_m` decimal(10,3) DEFAULT NULL,
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `reference_area` decimal(12,3) DEFAULT NULL,
  `reference_perimeter` decimal(12,3) DEFAULT NULL,
  `remark` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `room_key` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `room_type` enum('Balcony','Bathroom','Bedroom','DiningRoom','Kitchen','LivingRoom') COLLATE utf8mb4_general_ci NOT NULL,
  `sort_no` int NOT NULL,
  `wall_rise` decimal(10,3) DEFAULT NULL,
  `width` decimal(10,3) DEFAULT NULL,
  `window_area_total` decimal(10,3) DEFAULT NULL,
  `com_id` bigint NOT NULL,
  `version_id` bigint NOT NULL,
  PRIMARY KEY (`room_id`),
  KEY `FKjhx3b90iovrxtnl70o2n9umq3` (`com_id`),
  KEY `FKcn1997txpw3r526timau8pbhj` (`version_id`),
  CONSTRAINT `FKcn1997txpw3r526timau8pbhj` FOREIGN KEY (`version_id`) REFERENCES `design_template_version` (`version_id`),
  CONSTRAINT `FKjhx3b90iovrxtnl70o2n9umq3` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `design_template_room_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `design_template_room_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `design_template_version`
--

DROP TABLE IF EXISTS `design_template_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `design_template_version` (
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
  `area_max` decimal(12,2) DEFAULT NULL,
  `area_min` decimal(12,2) DEFAULT NULL,
  `house_type` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `status` enum('Disabled','Draft','Published') COLLATE utf8mb4_general_ci NOT NULL,
  `tags` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  `published_by` bigint DEFAULT NULL,
  `template_id` bigint NOT NULL,
  PRIMARY KEY (`version_id`),
  KEY `FK31p4s50vwa50c9vipggknxj9w` (`com_id`),
  KEY `FKdh1olhxp2f90uqadryjv5ir37` (`published_by`),
  KEY `FKsjmyfh70xhge5bii4xsln1sai` (`template_id`),
  CONSTRAINT `FK31p4s50vwa50c9vipggknxj9w` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKdh1olhxp2f90uqadryjv5ir37` FOREIGN KEY (`published_by`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKsjmyfh70xhge5bii4xsln1sai` FOREIGN KEY (`template_id`) REFERENCES `design_template` (`template_id`),
  CONSTRAINT `design_template_version_chk_1` CHECK ((`create_week` between 0 and 6)),
  CONSTRAINT `design_template_version_chk_2` CHECK ((`update_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pack_line`
--

DROP TABLE IF EXISTS `pack_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_line` (
  `item_id` bigint NOT NULL,
  `brand` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `catalog_category_snapshot` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `catalog_type_snapshot` enum('Appliance','Construction','Custom','General','Material','Soft') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cost_price_snapshot` decimal(12,2) DEFAULT NULL,
  `craft_snapshot` tinytext COLLATE utf8mb4_general_ci,
  `deductible` bit(1) NOT NULL,
  `included` bit(1) NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `pricing_method` enum('FixedTotal','ManualOnly','MaterialAndManual','MaterialOnly') COLLATE utf8mb4_general_ci NOT NULL,
  `quantity` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `quantity_factor` decimal(12,3) NOT NULL,
  `section_name_snapshot` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sort` int NOT NULL,
  `spec` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pack_line_criterions`
--

DROP TABLE IF EXISTS `pack_line_criterions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_line_criterions` (
  `item_id` bigint NOT NULL,
  `criterions` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKo4rqhonde0pmtywgr0rrs85xc` (`item_id`),
  CONSTRAINT `FKo4rqhonde0pmtywgr0rrs85xc` FOREIGN KEY (`item_id`) REFERENCES `pack_line` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pack_section`
--

DROP TABLE IF EXISTS `pack_section`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_section` (
  `group_id` bigint NOT NULL,
  `max_delete_num` int DEFAULT NULL,
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `priority` int DEFAULT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`group_id`),
  KEY `FK9i381u80nx5eowv4me473ru6m` (`com_id`),
  CONSTRAINT `FK9i381u80nx5eowv4me473ru6m` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pack_template`
--

DROP TABLE IF EXISTS `pack_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_template` (
  `template_id` bigint NOT NULL,
  `area` decimal(38,2) NOT NULL,
  `code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `cost` decimal(10,2) NOT NULL,
  `is_current_published` bit(1) NOT NULL,
  `discount` decimal(10,2) NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `snapshot_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `snapshot_json` longtext COLLATE utf8mb4_general_ci,
  `status` enum('Disabled','Draft','Published') COLLATE utf8mb4_general_ci NOT NULL,
  `version` int NOT NULL,
  `com_id` bigint NOT NULL,
  `published_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`template_id`),
  KEY `FKg2itassrj71n0931gsu5tg64m` (`com_id`),
  KEY `FKj077uha0jm0c1h9yx8ioihin2` (`published_by_id`),
  CONSTRAINT `FKg2itassrj71n0931gsu5tg64m` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKj077uha0jm0c1h9yx8ioihin2` FOREIGN KEY (`published_by_id`) REFERENCES `db_tenant_employee` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pack_template_criterions`
--

DROP TABLE IF EXISTS `pack_template_criterions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_template_criterions` (
  `package_template_id` bigint NOT NULL,
  `criterions` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FKtmxkl06swjqkb14qxuadbkqer` (`package_template_id`),
  CONSTRAINT `FKtmxkl06swjqkb14qxuadbkqer` FOREIGN KEY (`package_template_id`) REFERENCES `pack_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pack_template_item`
--

DROP TABLE IF EXISTS `pack_template_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_template_item` (
  `item_id` bigint NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `priority` int DEFAULT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci NOT NULL,
  `com_id` bigint NOT NULL,
  PRIMARY KEY (`item_id`),
  KEY `FKhug3p43alyr5jy5g5orb1w4gd` (`com_id`),
  CONSTRAINT `FKhug3p43alyr5jy5g5orb1w4gd` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pack_template_item_space_types`
--

DROP TABLE IF EXISTS `pack_template_item_space_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_template_item_space_types` (
  `item_id` bigint NOT NULL,
  `space_types` enum('Balcony','Bathroom','Bedroom','DiningRoom','Kitchen','LivingRoom') COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `FK5mwb2bxawtjjf0hiiyoxayih7` (`item_id`),
  CONSTRAINT `FK5mwb2bxawtjjf0hiiyoxayih7` FOREIGN KEY (`item_id`) REFERENCES `pack_template_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pack_template_measures`
--

DROP TABLE IF EXISTS `pack_template_measures`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_template_measures` (
  `package_template_id` bigint NOT NULL,
  `price` decimal(38,2) DEFAULT NULL,
  `remark` tinytext COLLATE utf8mb4_general_ci,
  `standard` decimal(38,2) DEFAULT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`package_template_id`,`item_id`),
  KEY `FKnfna6rf5lv3w321u87pb6dhxh` (`item_id`),
  CONSTRAINT `FKk25ij2x2fry8jk2q9are8fr0q` FOREIGN KEY (`package_template_id`) REFERENCES `pack_template` (`template_id`),
  CONSTRAINT `FKnfna6rf5lv3w321u87pb6dhxh` FOREIGN KEY (`item_id`) REFERENCES `pack_template_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `business_origin` enum('Carryover','New') COLLATE utf8mb4_general_ci NOT NULL,
  `converted_at` datetime(6) DEFAULT NULL,
  `cost` decimal(12,2) NOT NULL,
  `cost_complete` bit(1) NOT NULL,
  `design_fee` decimal(12,2) NOT NULL,
  `discount` decimal(10,4) NOT NULL,
  `management_fee` decimal(12,2) NOT NULL,
  `mode` enum('Custom','Hybrid','Package') COLLATE utf8mb4_general_ci NOT NULL,
  `no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `package_template_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `package_template_snapshot_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `package_template_version` int DEFAULT NULL,
  `rejected_at` datetime(6) DEFAULT NULL,
  `status` enum('Accepted','Converted','Draft','Rejected','Submitted','Superseded') COLLATE utf8mb4_general_ci NOT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `subtotal` decimal(12,2) NOT NULL,
  `tax_amount` decimal(12,2) NOT NULL,
  `tax_rate` decimal(8,4) NOT NULL,
  `title` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `version` int NOT NULL,
  `accepted_by` bigint DEFAULT NULL,
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
  KEY `FK1m6i7kijq15npghsjowipgqow` (`accepted_by`),
  KEY `FK396n99c10jswj2lbuxbgoty97` (`emp_id`),
  KEY `FKmwtprlb0k3keaj5po8wcjg4wu` (`customer_id`),
  KEY `FKl54a8939aa4m78f6xv6tudn3u` (`design_id`),
  KEY `FK39rbm5bfkng5x1x35mkr3b40r` (`package_template_id`),
  KEY `FKo9iyesrv0nmheipcdw86unt4b` (`property_id`),
  KEY `FK5cttl47je6gonafweclcv59k3` (`store_id`),
  KEY `FK9jomxwlxfskjat4x3lx7ie6wq` (`team_id`),
  CONSTRAINT `FK1m6i7kijq15npghsjowipgqow` FOREIGN KEY (`accepted_by`) REFERENCES `db_tenant_employee` (`emp_id`),
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `brand` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `category` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cost` decimal(12,2) NOT NULL,
  `craft` longtext COLLATE utf8mb4_general_ci,
  `difference_status` enum('Confirmed','Ignored','None','Pending') COLLATE utf8mb4_general_ci NOT NULL,
  `difference_type` enum('Deduct','DesignOnly','Ignored','Matched','None','PackageOnly','QuantityChanged','Replace','Upgrade') COLLATE utf8mb4_general_ci NOT NULL,
  `discount_amount` decimal(12,2) NOT NULL,
  `fixed_total_price` decimal(12,2) DEFAULT NULL,
  `formula` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `included` bit(1) NOT NULL,
  `manual_price` decimal(12,2) NOT NULL,
  `material_price` decimal(12,2) NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `pricing_mode` enum('FixedTotal','ManualOnly','MaterialAndManual','MaterialOnly') COLLATE utf8mb4_general_ci NOT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `quantity_formula` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `role` enum('Add','Base','Deduct','Gift','Option','Replace','Upgrade') COLLATE utf8mb4_general_ci NOT NULL,
  `sort` int NOT NULL,
  `spec` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` enum('Appliance','Construction','Custom','General','Material','Soft') COLLATE utf8mb4_general_ci NOT NULL,
  `unit` enum('Area','Block','Centimeter','Count','Grid','Hole','House','Item','Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `quote_line_components`
--

DROP TABLE IF EXISTS `quote_line_components`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quote_line_components` (
  `line_id` bigint NOT NULL,
  `cost_amount` decimal(12,2) NOT NULL,
  `formula` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `component_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sale_amount` decimal(12,2) NOT NULL,
  `sort` int NOT NULL,
  `component_type` enum('Auxiliary','Freight','Installation','Labor','Machine','Material','Other','Production','Service','Tax') COLLATE utf8mb4_general_ci NOT NULL,
  KEY `FK2dvwwoor77ihre5kd69hxh7sh` (`line_id`),
  CONSTRAINT `FK2dvwwoor77ihre5kd69hxh7sh` FOREIGN KEY (`line_id`) REFERENCES `quote_line` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-22 23:40:05
