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
  `type` enum('EmployeeCommission','EmployeeSalary','Expense','Material','Other','Project','Salary','Supplier') NOT NULL,
  `urgent` bit(1) NOT NULL,
  `com_id` bigint NOT NULL,
  `business_origin` varchar(16) NOT NULL DEFAULT 'New' COMMENT '业务来源：New/Carryover',
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
  `source_id` bigint DEFAULT NULL,
  `source_type` varchar(32) DEFAULT NULL,
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
  KEY `idx_fin_payable_company_origin` (`com_id`,`business_origin`,`payable_id`),
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
  CONSTRAINT `FKt3eat6h9tqwg6woci3m5g8634` FOREIGN KEY (`material_plan_id`) REFERENCES `db_material_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_finance_payable`
--

LOCK TABLES `db_finance_payable` WRITE;
/*!40000 ALTER TABLE `db_finance_payable` DISABLE KEYS */;
INSERT INTO `db_finance_payable` VALUES (229343918272020480,20260926,20260926045051,26,4,50,9,51,45051,5,2026,20260926,20260926045051,26,4,50,9,51,45051,5,2026,50000.00,'2026-10-20','期初未付供应商款',0.00,'验收供应商',NULL,NULL,'业务承接期初应付',NULL,'AP-E2E-001','Pending','Supplier',_binary '\0',1790000000000000001,'Carryover',1790000000000000101,NULL,1790000000000000101,NULL,NULL,229343918263631872,229343918225883136,NULL,1790000000000000002,NULL,NULL,NULL),(229353590550953984,20260926,20260926052917,26,5,29,9,17,52917,5,2026,20260926,20260926052917,26,5,29,9,17,52917,5,2026,42000.00,'2026-09-26','期初未付供应商款',0.00,'供应商',NULL,NULL,'业务承接期初应付',NULL,NULL,'Pending','Supplier',_binary '\0',1790360206049052593,'Carryover',1790360206094792644,NULL,1790360206094792644,NULL,NULL,229353590450290688,229353590240575488,NULL,1790360206074505364,NULL,NULL,NULL),(1790210542611130378,20260924,20260924084222,24,8,42,9,22,84222,3,2026,20260924,20260924084222,24,8,42,9,22,84222,3,2026,1650.00,'2026-10-24','材料采购 · 昭青演示材料商',0.00,'昭青演示材料商',NULL,'2026-09-24','确认下单自动形成应付','ReceiptDays','PO1790210542567','Pending','Material',_binary '\0',1790000000000000001,'New',1790000000000000102,228668996491149312,1790000000000000104,NULL,1790210541965059783,228668996403068928,228668993030848512,1790210542568173513,1790000000000000002,NULL,NULL,NULL),(1790210543199942507,20260924,20260924084223,24,8,42,9,23,84223,3,2026,20260924,20260924084223,24,8,42,9,23,84223,3,2026,1470.00,'2026-10-24','材料采购 · 昭青演示材料商',0.00,'昭青演示材料商',NULL,'2026-09-24','确认下单自动形成应付','ReceiptDays','PO1790210543167','Pending','Material',_binary '\0',1790000000000000001,'New',1790000000000000102,228669001570451456,1790000000000000104,NULL,1790210541965059783,228669001515925504,228668997900435456,1790210543168176514,1790000000000000002,NULL,NULL,NULL),(1790210543763331233,20260924,20260924084223,24,8,42,9,23,84223,3,2026,20260924,20260924084223,24,8,42,9,23,84223,3,2026,720.00,'2026-10-24','材料采购 · 昭青演示材料商',0.00,'昭青演示材料商',NULL,'2026-09-24','确认下单自动形成应付','ReceiptDays','PO1790210543733','Pending','Material',_binary '\0',1790000000000000001,'New',1790000000000000102,228669006083522560,1790000000000000104,NULL,1790210541965059783,228669006024802304,228669002849714176,1790210543733717132,1790000000000000002,NULL,NULL,NULL),(1790210544271667825,20260924,20260924084224,24,8,42,9,24,84224,3,2026,20260924,20260924084224,24,8,42,9,24,84224,3,2026,2550.00,NULL,'材料采购 · 昭青演示材料商',0.00,'昭青演示材料商',NULL,NULL,'确认下单自动形成应付','ReceiptDays','PO1790210544235','Pending','Material',_binary '\0',1790000000000000001,'New',1790000000000000102,228669010646925312,1790000000000000104,NULL,1790210541965059783,228669010604982272,228669007455059968,1790210544235553991,1790000000000000002,NULL,NULL,NULL),(1790210544715477895,20260924,20260924084224,24,8,42,9,24,84224,3,2026,20260924,20260924084224,24,8,42,9,24,84224,3,2026,1470.00,NULL,'材料采购 · 昭青演示材料商',0.00,'昭青演示材料商',NULL,NULL,'确认下单自动形成应付','ReceiptDays','PO1790210544689','Pending','Material',_binary '\0',1790000000000000001,'New',1790000000000000102,228669014925115392,1790000000000000104,NULL,1790210541965059783,228669014883172352,228669011808747520,1790210544689342731,1790000000000000002,NULL,NULL,NULL),(1790210545117316628,20260924,20260924084225,24,8,42,9,25,84225,3,2026,20260924,20260924084225,24,8,42,9,25,84225,3,2026,12000.00,NULL,'材料采购 · 昭青演示材料商',0.00,'昭青演示材料商',NULL,NULL,'确认下单自动形成应付','ReceiptDays','PO1790210545090','Pending','Material',_binary '\0',1790000000000000001,'New',1790000000000000102,228669019698233344,1790000000000000104,NULL,1790210541965059783,228669019656290304,228669016778997760,1790210545090973081,1790000000000000002,NULL,NULL,NULL);
/*!40000 ALTER TABLE `db_finance_payable` ENABLE KEYS */;
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
  `idempotency_fingerprint` varchar(64) DEFAULT NULL,
  `idempotency_key` varchar(128) DEFAULT NULL,
  `batch_no` varchar(64) DEFAULT NULL,
  `payee_emp_id` bigint DEFAULT NULL,
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
  KEY `FKnvwrjmvdh6umjaweqq81falmj` (`project_id`),
  KEY `FKrgjw750a7nklkhsbywubb3f3x` (`store_id`),
  KEY `FKo5w7jpnmsiudplqy1dvn3m5xd` (`team_id`),
  KEY `FK406o2oruc5a6nfh381glkxdeh` (`worker_id`),
  KEY `FK3wr8al57x055og16kaybi740r` (`payee_emp_id`),
  CONSTRAINT `FK3wr8al57x055og16kaybi740r` FOREIGN KEY (`payee_emp_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK406o2oruc5a6nfh381glkxdeh` FOREIGN KEY (`worker_id`) REFERENCES `db_worker` (`worker_id`),
  CONSTRAINT `FK483ytvwlmn5pskj27rmaqvll5` FOREIGN KEY (`partner_id`) REFERENCES `db_partner` (`partner_id`),
  CONSTRAINT `FK5x3u42xid7yfx6myax3sb9jdq` FOREIGN KEY (`owner_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FK8a3inpj4t37laa8uu85vmu6b2` FOREIGN KEY (`payable_id`) REFERENCES `db_finance_payable` (`payable_id`),
  CONSTRAINT `FKamkoey6ludb6egey1rmomke13` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`),
  CONSTRAINT `FKmcdjgewoyuuq79m43v95b7hiu` FOREIGN KEY (`creator_id`) REFERENCES `db_tenant_employee` (`emp_id`),
  CONSTRAINT `FKnvwrjmvdh6umjaweqq81falmj` FOREIGN KEY (`project_id`) REFERENCES `db_project` (`project_id`),
  CONSTRAINT `FKo5w7jpnmsiudplqy1dvn3m5xd` FOREIGN KEY (`team_id`) REFERENCES `db_tenant_team` (`team_id`),
  CONSTRAINT `FKrgjw750a7nklkhsbywubb3f3x` FOREIGN KEY (`store_id`) REFERENCES `db_tenant_store` (`store_id`)
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
  `manual_cost_amount` decimal(14,2) NOT NULL,
  `manual_income_amount` decimal(14,2) NOT NULL,
  `employee_commission_amount` decimal(14,2) NOT NULL,
  `employee_salary_amount` decimal(14,2) NOT NULL,
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
  CONSTRAINT `FKt5mf9533cgb3kgi0qsmjrvwk8` FOREIGN KEY (`com_id`) REFERENCES `db_tenant_company` (`com_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `db_finance_settlement`
--

LOCK TABLES `db_finance_settlement` WRITE;
/*!40000 ALTER TABLE `db_finance_settlement` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_finance_settlement` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-07  2:03:13
