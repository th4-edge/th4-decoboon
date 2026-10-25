package com.th4.edge.decoboon.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerManualPurpose;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.configure.ReceiptStatus;
import com.th4.edge.decoboon.finance.configure.SettlementStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.context.FinanceSummaryAggregateContext;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRefundRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.finance.repository.SettlementRepository;
import com.th4.edge.decoboon.payroll.configure.CommissionStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.context.CommissionReportContext;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
import com.th4.edge.decoboon.finance.context.FinanceProjectLedgerContext;
import com.th4.edge.decoboon.finance.context.PartnerLedgerAggregateContext;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.InviteStatus;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.configure.UserStatus;
import com.th4.edge.tenant.user.context.PasswordContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Invite;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.CredentialsRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.InviteRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * DEC-065 财务门店口径 HTTP 级独立验收：真实登录态 + 真实 Provider 路由 + 真实序列化。
 *
 * <p>覆盖：门店财务/店长只读本店、总部财务与 BOSS 全门店并可下钻、跨店与跨企业越权拒绝、
 * 店长记账写动作成功且归属本店、无关角色被拒。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:finance-store-scope-http;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.components.storage.provider=local",
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/finance-store-scope-http-upload",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
@AutoConfigureMockMvc
@Transactional
class FinanceStoreScopeHttpAcceptanceTest {

    private static final String PASSWORD = "Accept1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordContext passwordContext;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private InviteRepository inviteRepository;
    @Autowired private CredentialsRepository credentialsRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ReceivableRepository receivableRepository;
    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private PayableRepository payableRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private LedgerRepository ledgerRepository;
    @Autowired private ReceiptRefundRepository receiptRefundRepository;
    @Autowired private PayrollSheetRepository payrollSheetRepository;
    @Autowired private PayrollItemRepository payrollItemRepository;
    @Autowired private CommissionItemRepository commissionItemRepository;
    @Autowired private CommissionReportContext commissionReportContext;
    @Autowired private PartnerLedgerAggregateContext partnerLedgerAggregateContext;
    @Autowired private FinanceProjectLedgerContext financeProjectLedgerContext;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private SettlementRepository settlementRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private FinanceSummaryAggregateContext aggregateContext;

    private Scope scope;

    @BeforeEach
    void setUp() {
        scope = createScope();
    }

    @Test
    void companyPayrollSheetIsVisibleFromStoreThatHasPayrollItems() throws Exception {
        Company company = companyRepository.findByComId(
                employeeRepository.findByTelNo(scope.bossPhone()).getCompany().getComId());
        Employee boss = employeeRepository.findByTelNo(scope.bossPhone());
        Employee storeFinance = employeeRepository.findByTelNo(scope.storeFinancePhone());

        PayrollSheet sheet = new PayrollSheet();
        sheet.setCompany(company);
        sheet.setStore(storeRepository.findByStoreId(scope.storeAId()));
        sheet.setOwner(boss);
        sheet.setPeriod("2026-10");
        sheet.setStatus(PayrollSheetStatus.Draft);
        sheet.setAttendanceComplete(false);
        sheet.setCommissionComplete(false);
        sheet.setTotalGrossAmount(new BigDecimal("100.00"));
        sheet.setTotalDeductionAmount(BigDecimal.ZERO);
        sheet.setTotalNetAmount(new BigDecimal("100.00"));
        sheet.setCreatedBy(boss);
        sheet = payrollSheetRepository.save(sheet);

        PayrollItem item = new PayrollItem();
        item.setCompany(company);
        item.setStore(storeRepository.findByStoreId(scope.storeBId()));
        item.setEmployee(storeFinance);
        item.setSheet(sheet);
        item.setAttributionKey("COMPANY");
        item.setBaseSalaryAmount(new BigDecimal("100.00"));
        item.setGrossAmount(new BigDecimal("100.00"));
        item.setDeductionAmount(BigDecimal.ZERO);
        item.setNetAmount(new BigDecimal("100.00"));
        item.setCreatedBy(boss);
        payrollItemRepository.save(item);
        entityManager.flush();

        JsonNode page = api("payroll/sheet/page",
                "{\"current\":1,\"size\":20,\"storeId\":%d}".formatted(scope.storeBId()), login(scope.bossPhone()));
        assertThat(page.path("success").asBoolean()).as(page.toString()).isTrue();
        assertThat(page.path("data").path("totalElements").asLong()).isEqualTo(2);
        assertThat(page.path("data").path("content").findValuesAsText("period")).contains("2026-10");

        JsonNode workbench = api("payroll/workbench/report",
                "{\"period\":\"2026-10\",\"storeId\":%d}".formatted(scope.storeBId()), login(scope.bossPhone()));
        assertThat(workbench.path("success").asBoolean()).as(workbench.toString()).isTrue();
        assertThat(workbench.path("data").path("latestSheetId").asLong()).isEqualTo(sheet.getSheetId());
    }

    @Test
    void storeCandidatesFollowDataScopeOverHttp() throws Exception {
        JsonNode headquarters = api("user/store/list", "{}", login(scope.headFinancePhone()));
        assertThat(headquarters.path("success").asBoolean()).as(headquarters.toString()).isTrue();
        assertThat(headquarters.path("data").size()).isEqualTo(2);

        JsonNode store = api("user/store/list", "{}", login(scope.storeFinancePhone()));
        assertThat(store.path("success").asBoolean()).as(store.toString()).isTrue();
        assertThat(store.path("data").size()).isEqualTo(1);
        assertThat(store.path("data").get(0).path("storeId").asLong()).isEqualTo(scope.storeBId());
    }

    @Test
    void bossMustSelectStoreWhenInvitingFinanceOverHttp() throws Exception {
        String token = login(scope.bossPhone());
        String phone = "5" + String.format("%09d", Math.abs(System.nanoTime() % 1_000_000_000L));
        JsonNode missing = api("invite/invitation/save",
                "{\"inviteName\":\"缺门店财务\",\"invitePhone\":\"%s\",\"role\":\"Finance\"}".formatted(phone), token);
        assertThat(missing.path("success").asBoolean()).as(missing.toString()).isFalse();

        JsonNode selected = api("invite/invitation/save",
                "{\"inviteName\":\"二店财务\",\"invitePhone\":\"%s\",\"role\":\"Finance\",\"storeId\":%d}"
                        .formatted(phone, scope.storeBId()), token);
        assertThat(selected.path("success").asBoolean()).as(selected.toString()).isTrue();
        assertThat(selected.path("data").path("store").path("storeId").asLong()).isEqualTo(scope.storeBId());
    }

    @Test
    void bossCanMoveFinanceAcrossStoresButStoreManagerCannotOverHttp() throws Exception {
        Employee finance = employeeRepository.findByTelNo(scope.storeFinancePhone());
        JsonNode managerMove = api("company/member/update",
                "{\"empId\":%d,\"role\":\"Finance\",\"storeId\":%d}"
                        .formatted(finance.getEmpId(), scope.storeAId()), login(scope.storeManagerPhone()));
        assertThat(managerMove.path("success").asBoolean()).as(managerMove.toString()).isFalse();

        JsonNode bossMove = api("company/member/update",
                "{\"empId\":%d,\"role\":\"Finance\",\"storeId\":%d}"
                        .formatted(finance.getEmpId(), scope.storeAId()), login(scope.bossPhone()));
        assertThat(bossMove.path("success").asBoolean()).as(bossMove.toString()).isTrue();
        assertThat(bossMove.path("data").path("store").path("storeId").asLong()).isEqualTo(scope.storeAId());

        JsonNode summary = api("finance/summary/report", "{}", login(scope.storeFinancePhone()));
        assertThat(summary.path("success").asBoolean()).as(summary.toString()).isTrue();
        assertThat(summary.path("data").path("receivableAmount").decimalValue()).isEqualByComparingTo("300.00");
    }

    @Test
    void storeFinanceSeesOnlyOwnStoreOverHttp() throws Exception {
        String token = login(scope.storeFinancePhone());
        JsonNode response = api("finance/summary/report", "{}", token);
        assertThat(response.path("success").asBoolean()).as(response.toString()).isTrue();
        assertThat(response.path("data").path("receivableAmount").decimalValue())
                .isEqualByComparingTo("200.00");
        assertThat(response.path("data").path("projectCount").asInt()).isEqualTo(1);
    }

    @Test
    void financeCanUpdateCatalogCostAndSalePriceOverHttp() throws Exception {
        Employee finance = employeeRepository.findByTelNo(scope.storeFinancePhone());
        Invite invite = new Invite();
        invite.setCompany(finance.getCompany());
        invite.setStore(finance.getStore());
        invite.setInviter(finance);
        invite.setInviteName(finance.getName());
        invite.setTelNo(finance.getTelNo());
        invite.setRole(finance.getRole());
        invite.setStatus(InviteStatus.Accepted);
        invite.setExpireTime(LocalDateTime.now().plusDays(1));
        invite = inviteRepository.save(invite);
        finance.setInvite(invite);
        employeeRepository.save(finance);
        entityManager.flush();

        String token = login(scope.storeFinancePhone());
        JsonNode created = api("catalog/item/save", """
                {"type":"Construction","code":"HTTP-CATALOG-COST","category":"木工",
                 "name":"HTTP报价库财务维护","unit":"Item","pricingMode":"ManualOnly",
                 "materialPrice":0,"manualPrice":20.00,"costPrice":12.00,"images":[]}
                """, token);
        assertThat(created.path("success").asBoolean()).as(created.toString()).isTrue();
        long itemId = created.path("data").path("itemId").asLong();

        JsonNode response = api("catalog/item/update", """
                {"itemId":%s,"type":"Construction","code":"HTTP-CATALOG-COST","category":"木工",
                 "name":"HTTP报价库财务维护","unit":"Item","pricingMode":"ManualOnly",
                 "materialPrice":0,"manualPrice":25.50,"costPrice":15.00,"active":true,"sort":0,"images":[]}
                """.formatted(itemId), token);

        assertThat(response.path("success").asBoolean()).as(response.toString()).isTrue();
        assertThat(response.toString().length()).as("单条报价项目保存响应不得膨胀").isLessThan(10_000);
        assertThat(response.path("data").path("manualPrice").decimalValue()).isEqualByComparingTo("25.50");
        assertThat(response.path("data").path("costPrice").decimalValue()).isEqualByComparingTo("15.00");

        JsonNode importBatches = api("catalog/import-batch/page", "{\"current\":1,\"size\":3}", token);
        assertThat(importBatches.path("success").asBoolean()).as(importBatches.toString()).isTrue();

        JsonNode deleted = api("catalog/item/delete", "{\"itemId\":%s}".formatted(itemId), token);
        assertThat(deleted.path("success").asBoolean()).as(deleted.toString()).isTrue();
    }

    @Test
    void financeCanUseCustomerMenuAndCustomerActionsOverHttp() throws Exception {
        Employee storeFinance = employeeRepository.findByTelNo(scope.storeFinancePhone());
        Employee storeManager = employeeRepository.findByTelNo(scope.storeManagerPhone());
        String storeFinanceToken = login(scope.storeFinancePhone());

        JsonNode storePage = api("customer/customer/page", "{\"current\":1,\"size\":50}", storeFinanceToken);
        assertThat(storePage.path("success").asBoolean()).as(storePage.toString()).isTrue();
        assertThat(storePage.path("data").path("totalElements").asLong()).isEqualTo(1);
        long customerId = storePage.path("data").path("content").get(0).path("custId").asLong();

        JsonNode propertyPage = api("property/property/page", "{\"customerId\":%s}".formatted(customerId), storeFinanceToken);
        assertThat(propertyPage.path("success").asBoolean()).as(propertyPage.toString()).isTrue();
        long propertyId = propertyPage.path("data").path("content").get(0).path("propertyId").asLong();

        JsonNode transferred = api("customer/customer/update", """
                {"customerId":%s,"ownerId":%s,"reason":"财务客户权限验收"}
                """.formatted(customerId, storeManager.getEmpId()), storeFinanceToken);
        assertThat(transferred.path("success").asBoolean()).as(transferred.toString()).isTrue();
        assertThat(transferred.path("data").path("owner").path("empId").asLong()).isEqualTo(storeManager.getEmpId());

        JsonNode handedOff = api("customer/customer/handoff", """
                {"customerId":%s,"designerId":%s,"propertyId":%s}
                """.formatted(customerId, storeManager.getEmpId(), propertyId), storeFinanceToken);
        assertThat(handedOff.path("success").asBoolean()).as(handedOff.toString()).isTrue();

        String headFinanceToken = login(scope.headFinancePhone());
        JsonNode headPage = api("customer/customer/page", "{\"current\":1,\"size\":50}", headFinanceToken);
        assertThat(headPage.path("success").asBoolean()).as(headPage.toString()).isTrue();
        assertThat(headPage.path("data").path("totalElements").asLong()).isEqualTo(2);
    }

    @Test
    void headFinanceAndBossSeeAllStoresAndCanDrillDownOverHttp() throws Exception {
        String headToken = login(scope.headFinancePhone());
        String bossToken = login(scope.bossPhone());

        assertThat(api("finance/summary/report", "{}", headToken).path("data").path("receivableAmount").decimalValue())
                .isEqualByComparingTo("300.00");
        assertThat(api("finance/summary/report", "{}", bossToken).path("data").path("receivableAmount").decimalValue())
                .isEqualByComparingTo("300.00");

        String drill = "{\"storeId\":\"%s\"}".formatted(scope.storeBId());
        assertThat(api("finance/summary/report", drill, headToken).path("data").path("receivableAmount").decimalValue())
                .isEqualByComparingTo("200.00");
        assertThat(api("finance/summary/report", drill, bossToken).path("data").path("receivableAmount").decimalValue())
                .isEqualByComparingTo("200.00");
    }

    @Test
    void receiptAndPaymentListsAndReportsUseTheSameServerScopeAndFilters() throws Exception {
        String bossToken = login(scope.bossPhone());
        String storeFinanceToken = login(scope.storeFinancePhone());

        JsonNode receiptReport = api("finance/receipt/report", "{}", bossToken);
        assertThat(receiptReport.path("success").asBoolean()).as(receiptReport.toString()).isTrue();
        assertThat(receiptReport.path("data").path("receiptCount").asInt()).isEqualTo(3);
        assertThat(receiptReport.path("data").path("grossReceiptAmount").decimalValue()).isEqualByComparingTo("320.00");
        assertThat(receiptReport.path("data").path("reversalAmount").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(receiptReport.path("data").path("receiptAmount").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(receiptReport.path("data").path("asOf").asText()).isNotBlank();

        JsonNode storeReceiptReport = api("finance/receipt/report", "{}", storeFinanceToken);
        assertThat(storeReceiptReport.path("data").path("receiptCount").asInt()).isEqualTo(1);
        assertThat(storeReceiptReport.path("data").path("grossReceiptAmount").decimalValue()).isEqualByComparingTo("200.00");

        JsonNode filteredReceiptReport = api("finance/receipt/report",
                "{\"keyword\":\"专属付款方-12\"}", bossToken);
        assertThat(filteredReceiptReport.path("data").path("receiptCount").asInt()).isEqualTo(1);
        assertThat(filteredReceiptReport.path("data").path("grossReceiptAmount").decimalValue()).isEqualByComparingTo("200.00");

        JsonNode filteredReceiptPage = api("finance/receipt/page",
                "{\"current\":1,\"size\":1,\"keyword\":\"专属付款方-12\"}", bossToken);
        assertThat(filteredReceiptPage.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(filteredReceiptPage.path("data").path("content").size()).isEqualTo(1);
        assertThat(filteredReceiptPage.path("data").path("content").get(0).path("payerName").asText())
                .isEqualTo("专属付款方-12");

        JsonNode secondReceiptPage = api("finance/receipt/page", "{\"current\":2,\"size\":1}", bossToken);
        assertThat(secondReceiptPage.path("data").path("totalElements").asLong()).isEqualTo(3);
        assertThat(secondReceiptPage.path("data").path("content").size()).isEqualTo(1);
        assertThat(secondReceiptPage.path("data").path("content").get(0).path("receiptId").asText())
                .isNotEqualTo(filteredReceiptPage.path("data").path("content").get(0).path("receiptId").asText());

        JsonNode paymentReport = api("finance/payment/report", "{}", bossToken);
        assertThat(paymentReport.path("success").asBoolean()).as(paymentReport.toString()).isTrue();
        assertThat(paymentReport.path("data").path("paymentCount").asInt()).isEqualTo(2);
        assertThat(paymentReport.path("data").path("paymentAmount").decimalValue()).isEqualByComparingTo("70.00");
        assertThat(paymentReport.path("data").path("asOf").asText()).isNotBlank();

        JsonNode storePaymentReport = api("finance/payment/report", "{}", storeFinanceToken);
        assertThat(storePaymentReport.path("data").path("paymentCount").asInt()).isEqualTo(1);
        assertThat(storePaymentReport.path("data").path("paymentAmount").decimalValue()).isEqualByComparingTo("40.00");

        JsonNode filteredPaymentReport = api("finance/payment/report",
                "{\"keyword\":\"PAY-11\"}", bossToken);
        assertThat(filteredPaymentReport.path("data").path("paymentCount").asInt()).isEqualTo(1);
        assertThat(filteredPaymentReport.path("data").path("paymentAmount").decimalValue()).isEqualByComparingTo("30.00");

        JsonNode filteredPaymentPage = api("finance/payment/page",
                "{\"current\":1,\"size\":1,\"keyword\":\"PAY-11\"}", bossToken);
        assertThat(filteredPaymentPage.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(filteredPaymentPage.path("data").path("content").size()).isEqualTo(1);
        assertThat(filteredPaymentPage.path("data").path("content").get(0).path("transactionNo").asText())
                .isEqualTo("PAY-11");
    }

    @Test
    void receivablePayableAndLedgerPagesAndReportsUseTheSameServerScopeAndFilters() throws Exception {
        String bossToken = login(scope.bossPhone());
        String storeFinanceToken = login(scope.storeFinancePhone());

        JsonNode receivableReport = api("finance/receivable/report", "{}", bossToken);
        assertThat(receivableReport.path("success").asBoolean()).as(receivableReport.toString()).isTrue();
        assertThat(receivableReport.path("data").path("count").asInt()).isEqualTo(2);
        assertThat(receivableReport.path("data").path("totalAmount").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(receivableReport.path("data").path("receivedAmount").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(receivableReport.path("data").path("outstandingAmount").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(receivableReport.path("data").path("overdueAmount").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(receivableReport.path("data").path("asOf").asText()).isNotBlank();

        JsonNode storeReceivableReport = api("finance/receivable/report", "{}", storeFinanceToken);
        assertThat(storeReceivableReport.path("data").path("count").asInt()).isEqualTo(1);
        assertThat(storeReceivableReport.path("data").path("totalAmount").decimalValue()).isEqualByComparingTo("200.00");

        JsonNode filteredReceivableReport = api("finance/receivable/report",
                "{\"type\":\"First\",\"status\":\"Pending\",\"keyword\":\"HTTP项目12\"}", bossToken);
        assertThat(filteredReceivableReport.path("data").path("count").asInt()).isEqualTo(1);
        assertThat(filteredReceivableReport.path("data").path("totalAmount").decimalValue()).isEqualByComparingTo("200.00");

        JsonNode receivablePage = api("finance/receivable/page",
                "{\"current\":1,\"size\":1,\"keyword\":\"HTTP项目12\"}", bossToken);
        assertThat(receivablePage.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(receivablePage.path("data").path("content").size()).isEqualTo(1);

        JsonNode payableReport = api("finance/payable/report", "{}", bossToken);
        assertThat(payableReport.path("success").asBoolean()).as(payableReport.toString()).isTrue();
        assertThat(payableReport.path("data").path("count").asInt()).isEqualTo(3);
        assertThat(payableReport.path("data").path("totalAmount").decimalValue()).isEqualByComparingTo("85.00");
        assertThat(payableReport.path("data").path("paidAmount").decimalValue()).isEqualByComparingTo("60.00");
        assertThat(payableReport.path("data").path("outstandingAmount").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(payableReport.path("data").path("urgentAmount").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(payableReport.path("data").path("asOf").asText()).isNotBlank();

        JsonNode storePayableReport = api("finance/payable/report", "{}", storeFinanceToken);
        assertThat(storePayableReport.path("data").path("count").asInt()).isEqualTo(1);
        assertThat(storePayableReport.path("data").path("totalAmount").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(storePayableReport.path("data").path("outstandingAmount").decimalValue()).isEqualByComparingTo("0.00");

        JsonNode filteredPayableReport = api("finance/payable/report",
                "{\"urgent\":true,\"keyword\":\"紧急未付\"}", bossToken);
        assertThat(filteredPayableReport.path("data").path("count").asInt()).isEqualTo(1);
        assertThat(filteredPayableReport.path("data").path("outstandingAmount").decimalValue()).isEqualByComparingTo("25.00");

        JsonNode payablePage = api("finance/payable/page",
                "{\"current\":1,\"size\":1,\"urgent\":true}", bossToken);
        assertThat(payablePage.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(payablePage.path("data").path("content").size()).isEqualTo(1);

        JsonNode ledgerReport = api("finance/ledger/report", "{}", bossToken);
        assertThat(ledgerReport.path("success").asBoolean()).as(ledgerReport.toString()).isTrue();
        assertThat(ledgerReport.path("data").path("count").asInt()).isEqualTo(3);
        assertThat(ledgerReport.path("data").path("totalAmount").decimalValue()).isEqualByComparingTo("170.00");
        assertThat(ledgerReport.path("data").path("incomeAmount").decimalValue()).isEqualByComparingTo("140.00");
        assertThat(ledgerReport.path("data").path("expenseAmount").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(ledgerReport.path("data").path("netCashFlowAmount").decimalValue()).isEqualByComparingTo("110.00");
        assertThat(ledgerReport.path("data").path("asOf").asText()).isNotBlank();

        JsonNode storeLedgerReport = api("finance/ledger/report", "{}", storeFinanceToken);
        assertThat(storeLedgerReport.path("data").path("count").asInt()).isEqualTo(1);
        assertThat(storeLedgerReport.path("data").path("expenseAmount").decimalValue()).isEqualByComparingTo("0.00");

        JsonNode expenseReport = api("finance/ledger/report", "{\"direction\":\"Expense\"}", bossToken);
        assertThat(expenseReport.path("data").path("count").asInt()).isEqualTo(1);
        assertThat(expenseReport.path("data").path("expenseAmount").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(expenseReport.path("data").path("netCashFlowAmount").decimalValue()).isEqualByComparingTo("-30.00");

        JsonNode filteredLedgerPage = api("finance/ledger/page",
                "{\"current\":1,\"size\":1,\"keyword\":\"HTTP台账收入-12\"}", bossToken);
        assertThat(filteredLedgerPage.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(filteredLedgerPage.path("data").path("content").size()).isEqualTo(1);
    }

    @Test
    void refundPayrollAndCommissionPagesUseServerPagingAndReports() throws Exception {
        String bossToken = login(scope.bossPhone());
        String storeFinanceToken = login(scope.storeFinancePhone());

        JsonNode refundReport = api("finance/refund/report", "{}", bossToken);
        assertThat(refundReport.path("success").asBoolean()).as(refundReport.toString()).isTrue();
        assertThat(refundReport.path("data").path("count").asInt()).isEqualTo(2);
        assertThat(refundReport.path("data").path("totalAmount").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(refundReport.path("data").path("asOf").asText()).isNotBlank();

        JsonNode storeRefundReport = api("finance/refund/report", "{}", storeFinanceToken);
        assertThat(storeRefundReport.path("data").path("count").asInt()).isEqualTo(1);
        assertThat(storeRefundReport.path("data").path("totalAmount").decimalValue()).isEqualByComparingTo("20.00");

        JsonNode refundPage = api("finance/refund/page", "{\"current\":2,\"size\":1}", bossToken);
        assertThat(refundPage.path("data").path("totalElements").asLong()).isEqualTo(2);
        assertThat(refundPage.path("data").path("content").size()).isEqualTo(1);

        JsonNode sheetPage = api("payroll/sheet/page", "{\"current\":2,\"size\":1}", bossToken);
        assertThat(sheetPage.path("success").asBoolean()).as(sheetPage.toString()).isTrue();
        assertThat(sheetPage.path("data").path("totalElements").asLong()).isEqualTo(2);
        assertThat(sheetPage.path("data").path("content").size()).isEqualTo(1);

        JsonNode storeSheetPage = api("payroll/sheet/page", "{\"current\":1,\"size\":20}", storeFinanceToken);
        assertThat(storeSheetPage.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(storeSheetPage.path("data").path("content").get(0).path("period").asText()).isEqualTo("2026-09");

        JsonNode commissionReport = api("payroll/commission/report", "{}", bossToken);
        assertThat(commissionReport.path("success").asBoolean()).as(commissionReport.toString()).isTrue();
        assertThat(commissionReport.path("data").path("count").asInt()).isEqualTo(2);
        assertThat(commissionReport.path("data").path("commissionAmount").decimalValue()).isEqualByComparingTo("110.00");
        assertThat(commissionReport.path("data").path("accruedAmount").decimalValue()).isEqualByComparingTo("70.00");
        assertThat(commissionReport.path("data").path("releasedAmount").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(commissionReport.path("data").path("paidAmount").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(commissionReport.path("data").path("asOf").asText()).isNotBlank();

        JsonNode storeCommissionReport = api("payroll/commission/report", "{}", storeFinanceToken);
        assertThat(storeCommissionReport.path("data").path("count").asInt()).isEqualTo(1);
        assertThat(storeCommissionReport.path("data").path("commissionAmount").decimalValue()).isEqualByComparingTo("50.00");

        JsonNode commissionPage = api("payroll/commission/page", "{\"current\":2,\"size\":1}", bossToken);
        assertThat(commissionPage.path("data").path("totalElements").asLong()).isEqualTo(2);
        assertThat(commissionPage.path("data").path("content").size()).isEqualTo(1);
    }

    @Test
    void partnerLedgerUsesServerPagingFilteringAndAggregateSummary() throws Exception {
        String bossToken = login(scope.bossPhone());
        String storeFinanceToken = login(scope.storeFinancePhone());

        JsonNode partnerReport = api("finance/partner-ledger/report",
                "{\"summaryOnly\":true}", bossToken);
        assertThat(partnerReport.path("success").asBoolean()).as(partnerReport.toString()).isTrue();
        assertThat(partnerReport.path("data").path("partnerCount").asInt()).isEqualTo(2);
        assertThat(partnerReport.path("data").path("payableAmount").decimalValue()).isEqualByComparingTo("85.00");
        assertThat(partnerReport.path("data").path("paidAmount").decimalValue()).isEqualByComparingTo("60.00");
        assertThat(partnerReport.path("data").path("outstandingAmount").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(partnerReport.path("data").path("overdueAmount").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(partnerReport.path("data").path("asOf").asText()).isNotBlank();

        JsonNode storePartnerReport = api("finance/partner-ledger/report",
                "{\"summaryOnly\":true}", storeFinanceToken);
        assertThat(storePartnerReport.path("data").path("partnerCount").asInt()).isEqualTo(1);
        assertThat(storePartnerReport.path("data").path("outstandingAmount").decimalValue()).isEqualByComparingTo("0.00");

        JsonNode filteredPartnerReport = api("finance/partner-ledger/report",
                "{\"summaryOnly\":true,\"keyword\":\"HTTP供应商-12\"}", bossToken);
        assertThat(filteredPartnerReport.path("data").path("partnerCount").asInt()).isEqualTo(1);
        assertThat(filteredPartnerReport.path("data").path("payableAmount").decimalValue()).isEqualByComparingTo("30.00");

        JsonNode overduePartnerReport = api("finance/partner-ledger/report",
                "{\"summaryOnly\":true,\"overdueOnly\":true}", bossToken);
        assertThat(overduePartnerReport.path("data").path("partnerCount").asInt()).isEqualTo(1);
        assertThat(overduePartnerReport.path("data").path("overdueAmount").decimalValue()).isEqualByComparingTo("25.00");

        JsonNode partnerPage = api("finance/partner-ledger/page",
                "{\"current\":2,\"size\":1}", bossToken);
        assertThat(partnerPage.path("success").asBoolean()).as(partnerPage.toString()).isTrue();
        assertThat(partnerPage.path("data").path("totalElements").asLong()).isEqualTo(2);
        assertThat(partnerPage.path("data").path("content").size()).isEqualTo(1);

        JsonNode overduePartnerPage = api("finance/partner-ledger/page",
                "{\"current\":1,\"size\":20,\"overdueOnly\":true}", bossToken);
        assertThat(overduePartnerPage.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(overduePartnerPage.path("data").path("content").get(0).path("overdueAmount").decimalValue())
                .isEqualByComparingTo("25.00");

        JsonNode appCompatibleReport = api("finance/partner-ledger/report", "{}", bossToken);
        assertThat(appCompatibleReport.path("data").path("rows").size()).isEqualTo(2);
    }

    @Test
    void projectLedgerUsesServerPagingFiltersAndAggregateSummary() throws Exception {
        String bossToken = login(scope.bossPhone());
        String storeFinanceToken = login(scope.storeFinancePhone());

        JsonNode projectSummary = api("finance/project-ledger/report", "{}", bossToken);
        assertThat(projectSummary.path("success").asBoolean()).as(projectSummary.toString()).isTrue();
        assertThat(projectSummary.path("data").path("projectCount").asInt()).isEqualTo(2);
        assertThat(projectSummary.path("data").path("activeProjectCount").asInt()).isEqualTo(2);
        assertThat(projectSummary.path("data").path("contractAmount").decimalValue()).isEqualByComparingTo("200000.00");
        assertThat(projectSummary.path("data").path("receivableAmount").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(projectSummary.path("data").path("costComplete").asBoolean()).isFalse();
        assertThat(projectSummary.path("data").path("costIncompleteProjectCount").asInt()).isEqualTo(2);
        assertThat(projectSummary.path("data").path("asOf").asText()).isNotBlank();

        JsonNode storeProjectSummary = api("finance/project-ledger/report", "{}", storeFinanceToken);
        assertThat(storeProjectSummary.path("data").path("projectCount").asInt()).isEqualTo(1);
        assertThat(storeProjectSummary.path("data").path("contractAmount").decimalValue()).isEqualByComparingTo("100000.00");

        JsonNode filteredProjectSummary = api("finance/project-ledger/report",
                "{\"keyword\":\"HTTP项目12\"}", bossToken);
        assertThat(filteredProjectSummary.path("data").path("projectCount").asInt()).isEqualTo(1);
        assertThat(filteredProjectSummary.path("data").path("contractAmount").decimalValue()).isEqualByComparingTo("100000.00");

        JsonNode riskSummary = api("finance/project-ledger/report", "{\"filter\":\"risk\"}", bossToken);
        assertThat(riskSummary.path("data").path("projectCount").asInt()).isEqualTo(2);

        JsonNode settleSummary = api("finance/project-ledger/report", "{\"filter\":\"settle\"}", bossToken);
        assertThat(settleSummary.path("data").path("projectCount").asInt()).isEqualTo(1);

        JsonNode projectPage = api("finance/project/page", "{\"current\":2,\"size\":1}", bossToken);
        assertThat(projectPage.path("success").asBoolean()).as(projectPage.toString()).isTrue();
        assertThat(projectPage.path("data").path("totalElements").asLong()).isEqualTo(2);
        assertThat(projectPage.path("data").path("content").size()).isEqualTo(1);

        JsonNode settlePage = api("finance/project/page",
                "{\"current\":1,\"size\":20,\"filter\":\"settle\"}", bossToken);
        assertThat(settlePage.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(settlePage.path("data").path("content").get(0).path("settlement").path("status").path("key").asText())
                .isEqualTo("Draft");
    }

    @Test
    void transactionReportsUseAggregateQueriesWithoutRowByRowQueries() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        entityManager.flush();
        statistics.clear();
        Employee boss = employeeRepository.findByTelNo(scope.bossPhone());

        aggregateContext.receipts(boss,
                new FinanceSummaryAggregateContext.ReceiptCriteria(null, null, 0L));
        long afterReceipts = statistics.getPrepareStatementCount();
        aggregateContext.payments(boss,
                new FinanceSummaryAggregateContext.PaymentCriteria(null, null, 0L));
        long afterPayments = statistics.getPrepareStatementCount();
        aggregateContext.receivables(boss,
                new FinanceSummaryAggregateContext.ReceivableCriteria(null, null, null, null, 0L));
        long afterReceivables = statistics.getPrepareStatementCount();
        aggregateContext.payables(boss,
                new FinanceSummaryAggregateContext.PayableCriteria(null, null, null, null, null, null, 0L));
        long afterPayables = statistics.getPrepareStatementCount();
        aggregateContext.ledgers(boss,
                new FinanceSummaryAggregateContext.LedgerCriteria(null, null, null, null,
                        null, null, null, null, null, 0L));
        long afterLedgers = statistics.getPrepareStatementCount();
        aggregateContext.refunds(boss,
                new FinanceSummaryAggregateContext.RefundCriteria(null, 0L));
        long afterRefunds = statistics.getPrepareStatementCount();
        commissionReportContext.aggregate(boss, 0L);
        long afterCommissions = statistics.getPrepareStatementCount();
        partnerLedgerAggregateContext.summary(boss, null, null, null, 0L);
        long afterPartners = statistics.getPrepareStatementCount();
        financeProjectLedgerContext.summary(boss, null, null, null, null, "all");
        long afterProjectLedger = statistics.getPrepareStatementCount();

        assertThat(afterReceipts)
                .as("首个汇总调用的权限解析 + 聚合查询应为固定小常数，不能随收款明细数增长")
                .isLessThanOrEqualTo(4);
        assertThat(afterPayments - afterReceipts)
                .as("付款汇总不得因明细行逐条查库")
                .isLessThanOrEqualTo(2);
        assertThat(afterReceivables - afterPayments)
                .as("应收汇总不得因明细行逐条查库")
                .isLessThanOrEqualTo(2);
        assertThat(afterPayables - afterReceivables)
                .as("应付汇总不得因明细行逐条查库")
                .isLessThanOrEqualTo(2);
        assertThat(afterLedgers - afterPayables)
                .as("资金台账汇总不得因明细行逐条查库")
                .isLessThanOrEqualTo(2);
        assertThat(afterRefunds - afterLedgers)
                .as("退款汇总不得因明细行逐条查库")
                .isLessThanOrEqualTo(2);
        assertThat(afterCommissions - afterRefunds)
                .as("提成汇总不得因明细行逐条查库")
                .isLessThanOrEqualTo(2);
        assertThat(afterPartners - afterCommissions)
                .as("供应商往来汇总不得因供应商逐家查库")
                .isLessThanOrEqualTo(2);
        assertThat(afterProjectLedger - afterPartners)
                .as("项目账本汇总必须批量读取财务事实，查询条数不得随项目数线性增长")
                .isLessThanOrEqualTo(20);
    }

    @Test
    void crossStoreAndCrossCompanyStoreAreRejectedOverHttp() throws Exception {
        String storeToken = login(scope.storeFinancePhone());
        String headToken = login(scope.headFinancePhone());

        // 门店范围下钻他店 → 拒绝
        JsonNode otherStore = api("finance/summary/report", "{\"storeId\":\"%s\"}".formatted(scope.storeAId()), storeToken);
        assertThat(otherStore.path("success").asBoolean()).isFalse();
        assertThat(otherStore.path("errorCode").path("code").asText()).isEqualTo("403");

        // 企业范围下钻他企业门店 → 拒绝
        JsonNode foreignStore = api("finance/summary/report",
                "{\"storeId\":\"%s\"}".formatted(scope.foreignStoreId()), headToken);
        assertThat(foreignStore.path("success").asBoolean()).isFalse();
        assertThat(foreignStore.path("errorCode").path("code").asText()).isEqualTo("403");
    }

    @Test
    void storeManagerCanBookkeepAndRecordLandsInOwnStoreOverHttp() throws Exception {
        String token = login(scope.storeManagerPhone());
        JsonNode created = api("finance/receivable/save", """
                {"projectId":%s,"name":"店长记账验收","stageName":"首期款","type":"First","amount":88.00,"dueDate":"2026-11-30"}
                """.formatted(scope.projectBId()), token);

        assertThat(created.path("success").asBoolean()).as(created.toString()).isTrue();
        assertThat(created.path("data").path("amount").decimalValue()).isEqualByComparingTo("88.00");

        // 归属核对：店长新记账单必须落在本店范围（换用门店财务账号按本店范围核对可见性）
        String financeToken = login(scope.storeFinancePhone());
        JsonNode page = api("finance/receivable/page", "{\"current\":1,\"size\":50}", financeToken);
        assertThat(page.path("success").asBoolean()).as(page.toString()).isTrue();
        JsonNode items = page.path("data").path("list");
        if (items.isMissingNode()) {
            items = page.path("data").path("content");
        }
        // 本店 2 笔（种子 200 + 店长新记 88），他店 100 不可见
        assertThat(items.size()).isEqualTo(2);
        assertThat(items).anySatisfy(item ->
                assertThat(item.path("amount").decimalValue()).isEqualByComparingTo("88.00"));
        assertThat(items).noneSatisfy(item ->
                assertThat(item.path("amount").decimalValue()).isEqualByComparingTo("100.00"));
    }

    /**
     * P1-a2 · 新补门店入参的 7 个页面（`DESIGN-BIZ-20261013-005` §1）：
     * 全部门店不传参 / 企业范围下钻本店 / 下钻他企业门店 403 / **门店范围角色（店长）传他店 403**。
     */
    @Test
    void newStoreScopePagesResolveStoreDimensionOverHttp() throws Exception {
        String bossToken = login(scope.bossPhone());
        String storeManagerToken = login(scope.storeManagerPhone());
        String storeFinanceToken = login(scope.storeFinancePhone());
        String[] endpoints = {
                "finance/refund/page", "payroll/sheet/page", "payroll/item/page", "payroll/commission/page",
                "payroll/store-payroll/page", "document/voucher/page", "finance/partner-ledger/report"
        };
        for (String endpoint : endpoints) {
            // 1) 全部门店：不传 storeId（绝不传状态值 '0'）
            JsonNode all = api(endpoint, "{\"current\":1,\"size\":20}", bossToken);
            assertThat(all.path("success").asBoolean()).as(endpoint + " 全部门店: " + all).isTrue();

            if ("payroll/store-payroll/page".equals(endpoint)) {
                JsonNode financeOwnStore = api(endpoint, "{\"current\":1,\"size\":20}", storeFinanceToken);
                assertThat(financeOwnStore.path("success").asBoolean())
                        .as(endpoint + " 门店财务本店: " + financeOwnStore).isTrue();
                JsonNode financeCrossStore = api(endpoint,
                        "{\"storeId\":\"%s\"}".formatted(scope.storeAId()), storeFinanceToken);
                assertThat(financeCrossStore.path("success").asBoolean())
                        .as(endpoint + " 门店财务跨店: " + financeCrossStore).isFalse();
                assertThat(financeCrossStore.path("errorCode").path("code").asText()).isEqualTo("403");
            }

            // 2) 下钻本店维度 → 通过（企业范围任意本店；门店范围角色为其本店）
            JsonNode drill = api(endpoint,
                    "{\"current\":1,\"size\":20,\"storeId\":\"%s\"}".formatted(scope.storeBId()), bossToken);
            assertThat(drill.path("success").asBoolean()).as(endpoint + " 下钻本店: " + drill).isTrue();

            // 3) 企业范围下钻他企业门店 → 403
            JsonNode foreign = api(endpoint, "{\"storeId\":\"%s\"}".formatted(scope.foreignStoreId()), bossToken);
            assertThat(foreign.path("success").asBoolean()).as(endpoint + " 他企业门店: " + foreign).isFalse();
            assertThat(foreign.path("errorCode").path("code").asText()).as(endpoint + " 他企业门店码").isEqualTo("403");

            // 4) 门店范围角色（店长）传他店 → 403（锁本店，不得静默忽略）
            JsonNode crossStore = api(endpoint, "{\"storeId\":\"%s\"}".formatted(scope.storeAId()), storeManagerToken);
            assertThat(crossStore.path("success").asBoolean()).as(endpoint + " 店长跨店: " + crossStore).isFalse();
            assertThat(crossStore.path("errorCode").path("code").asText()).as(endpoint + " 店长跨店码").isEqualTo("403");
        }
    }

    @Test
    void storeManagerCannotTouchAnotherStoreProjectOverHttp() throws Exception {
        String token = login(scope.storeManagerPhone());
        JsonNode rejected = api("finance/receivable/save", """
                {"projectId":%s,"name":"越权记账","stageName":"首期款","type":"First","amount":66.00,"dueDate":"2026-11-30"}
                """.formatted(scope.projectAId()), token);

        assertThat(rejected.path("success").asBoolean()).isFalse();
        assertThat(rejected.path("errorCode").path("code").asText()).isIn("403", "404");
    }

    @Test
    void idWritesRejectCrossStoreTargetsAndMixedStoreVouchers() throws Exception {
        Employee manager = employeeRepository.findByTelNo(scope.storeManagerPhone());
        Employee headFinance = employeeRepository.findByTelNo(scope.headFinancePhone());
        Long comId = manager.getCompany().getComId();
        String managerToken = login(scope.storeManagerPhone());
        String storeFinanceToken = login(scope.storeFinancePhone());
        String headFinanceToken = login(scope.headFinancePhone());

        Receivable receivableA = receivableRepository.findAll().stream()
                .filter(item -> item.getStore() != null && scope.storeAId().equals(item.getStore().getStoreId()))
                .findFirst().orElseThrow();
        Payable payableA = payableRepository.findAll().stream()
                .filter(item -> item.getStore() != null && scope.storeAId().equals(item.getStore().getStoreId()))
                .filter(item -> item.getPaidAmount() == null
                        || item.getPaidAmount().compareTo(item.getAmount()) < 0)
                .findFirst().orElseThrow();
        Receipt receiptA = receiptRepository.findAll().stream()
                .filter(item -> item.getStore() != null && scope.storeAId().equals(item.getStore().getStoreId()))
                .filter(item -> item.getStatus() == ReceiptStatus.Active)
                .findFirst().orElseThrow();
        Settlement settlementA = settlementRepository.findAll().stream()
                .filter(item -> item.getStore() != null && scope.storeAId().equals(item.getStore().getStoreId()))
                .findFirst().orElseThrow();
        Ledger ledgerA = ledgerRepository.findAll().stream()
                .filter(item -> item.getStore() != null && scope.storeAId().equals(item.getStore().getStoreId()))
                .findFirst().orElseThrow();
        Ledger ledgerB = ledgerRepository.findAll().stream()
                .filter(item -> item.getStore() != null && scope.storeBId().equals(item.getStore().getStoreId()))
                .findFirst().orElseThrow();
        Payable payableB = payableRepository.findAll().stream()
                .filter(item -> item.getStore() != null && scope.storeBId().equals(item.getStore().getStoreId()))
                .findFirst().orElseThrow();
        payableB.setPaidAmount(BigDecimal.ZERO);
        payableB.setStatus(PayableStatus.Pending);
        payableRepository.save(payableB);

        assertCrossStoreRejected(api("finance/receivable/update", """
                {"receivableId":%s,"name":"跨店应收修改"}
                """.formatted(receivableA.getReceivableId()), managerToken), "应收修改");
        assertCrossStoreRejected(api("finance/payable/update", """
                {"payableId":%s,"name":"跨店应付修改"}
                """.formatted(payableA.getPayableId()), managerToken), "应付修改");
        assertCrossStoreRejected(api("finance/payment/save", """
                {"payableId":%s,"amount":1.00,"paidAt":"2026-10-22","idempotencyKey":"scope-payment-a"}
                """.formatted(payableA.getPayableId()), managerToken), "付款");
        assertCrossStoreRejected(api("finance/refund/save", """
                {"receiptId":%s,"amount":1.00,"idempotencyKey":"scope-refund-a"}
                """.formatted(receiptA.getReceiptId()), managerToken), "退款");
        assertCrossStoreRejected(api("finance/settlement/update", """
                {"settlementId":%s,"status":"Confirmed"}
                """.formatted(settlementA.getSettlementId()), managerToken), "结算修改");
        assertCrossStoreRejected(api("finance/settlement/retention-release", """
                {"settlementId":%s}
                """.formatted(settlementA.getSettlementId()), managerToken), "质保释放");
        assertCrossStoreRejected(api("finance/ledger/update", """
                {"ledgerId":%s,"category":"跨店分类修改","reason":"越权验收"}
                """.formatted(ledgerA.getLedgerId()), managerToken), "台账更新");
        assertCrossStoreRejected(api("document/voucher/build", """
                {"ledgerIds":[%s]}
                """.formatted(ledgerA.getLedgerId()), storeFinanceToken), "跨店生成凭证");

        JsonNode mixedPayment = api("finance/payment/save", """
                {"payableIds":[%s,%s],"amount":1.00,"paidAt":"2026-10-22","idempotencyKey":"scope-mixed-payment"}
                """.formatted(payableA.getPayableId(), payableB.getPayableId()), headFinanceToken);
        assertThat(mixedPayment.path("success").asBoolean()).as(mixedPayment.toString()).isFalse();
        assertThat(mixedPayment.path("errorCode").path("code").asText()).isEqualTo("400");
        assertThat(mixedPayment.path("errorCode").path("message").asText()).contains("同一门店");

        JsonNode mixed = api("document/voucher/build", """
                {"ledgerIds":[%s,%s]}
                """.formatted(ledgerA.getLedgerId(), ledgerB.getLedgerId()), headFinanceToken);
        assertThat(mixed.path("success").asBoolean()).as(mixed.toString()).isFalse();
        assertThat(mixed.path("errorCode").path("code").asText()).isEqualTo("400");
        assertThat(mixed.path("errorCode").path("message").asText()).contains("同一门店");

        JsonNode paidB = api("finance/payment/save", """
                {"payableId":%s,"amount":1.00,"paidAt":"2026-10-22","idempotencyKey":"scope-own-store-payment-b"}
                """.formatted(payableB.getPayableId()), headFinanceToken);
        assertThat(paidB.path("success").asBoolean()).as(paidB.toString()).isTrue();
        long paymentBId = paidB.path("data").path("paymentId").asLong();
        Payment paymentB = paymentRepository.findById(paymentBId).orElseThrow();
        assertThat(paymentB.getStore().getStoreId()).isEqualTo(scope.storeBId());
        Ledger paymentLedger = ledgerRepository
                .findFirstByCompany_ComIdAndSourceAndSourceIdOrderByLedgerIdAsc(
                        comId, LedgerSource.Payment, paymentBId)
                .orElseThrow();
        assertThat(paymentLedger.getStore().getStoreId()).isEqualTo(scope.storeBId());

        Employee payee = employeeRepository.findByTelNo(scope.bossPhone());
        Payable employeeSalaryA = new Payable();
        employeeSalaryA.setCompany(manager.getCompany());
        employeeSalaryA.setStore(storeRepository.findByStoreId(scope.storeAId()));
        employeeSalaryA.setOwner(manager);
        employeeSalaryA.setProject(payableA.getProject());
        employeeSalaryA.setProperty(payableA.getProperty());
        employeeSalaryA.setName("HTTP跨店员工工资应付");
        employeeSalaryA.setType(PayableType.EmployeeSalary);
        employeeSalaryA.setPayeeEmployee(payee);
        employeeSalaryA.setPayeeName(payee.getName());
        employeeSalaryA.setAmount(new BigDecimal("10.00"));
        employeeSalaryA.setPaidAmount(BigDecimal.ZERO);
        employeeSalaryA.setDueDate(java.time.LocalDate.now());
        employeeSalaryA.setStatus(PayableStatus.Pending);
        employeeSalaryA.setUrgent(false);
        employeeSalaryA.setCreatedBy(manager);
        employeeSalaryA = payableRepository.save(employeeSalaryA);
        entityManager.flush();

        assertCrossStoreRejected(api("finance/payment/employee/save", """
                {"payableIds":[%s],"amount":1.00,"paidAt":"2026-10-22","idempotencyKey":"scope-employee-payment-a"}
                """.formatted(employeeSalaryA.getPayableId()), storeFinanceToken), "员工工资发放");

        Employee storeFinance = employeeRepository.findByTelNo(scope.storeFinancePhone());
        Payable employeeSalaryB = new Payable();
        employeeSalaryB.setCompany(manager.getCompany());
        employeeSalaryB.setStore(storeRepository.findByStoreId(scope.storeBId()));
        employeeSalaryB.setOwner(storeFinance);
        employeeSalaryB.setName("HTTP本店员工工资应付");
        employeeSalaryB.setType(PayableType.EmployeeSalary);
        employeeSalaryB.setPayeeEmployee(storeFinance);
        employeeSalaryB.setPayeeName(storeFinance.getName());
        employeeSalaryB.setAmount(new BigDecimal("10.00"));
        employeeSalaryB.setPaidAmount(BigDecimal.ZERO);
        employeeSalaryB.setDueDate(java.time.LocalDate.now());
        employeeSalaryB.setStatus(PayableStatus.Pending);
        employeeSalaryB.setUrgent(false);
        employeeSalaryB.setCreatedBy(manager);
        employeeSalaryB = payableRepository.save(employeeSalaryB);
        entityManager.flush();

        JsonNode employeePaidB = api("finance/payment/employee/save", """
                {"payableIds":[%s],"amount":1.00,"paidAt":"2026-10-22","idempotencyKey":"scope-employee-payment-b"}
                """.formatted(employeeSalaryB.getPayableId()), headFinanceToken);
        assertThat(employeePaidB.path("success").asBoolean()).as(employeePaidB.toString()).isTrue();
        long employeePaymentBId = employeePaidB.path("data").path("paymentId").asLong();
        Payment employeePaymentB = paymentRepository.findById(employeePaymentBId).orElseThrow();
        assertThat(employeePaymentB.getStore().getStoreId()).isEqualTo(scope.storeBId());
        Ledger employeeLedgerB = ledgerRepository
                .findFirstByCompany_ComIdAndSourceAndSourceIdOrderByLedgerIdAsc(
                        comId, LedgerSource.Payment, employeePaymentBId)
                .orElseThrow();
        assertThat(employeeLedgerB.getStore().getStoreId()).isEqualTo(scope.storeBId());

        Payable historicalNoStore = new Payable();
        historicalNoStore.setCompany(manager.getCompany());
        historicalNoStore.setOwner(employeeRepository.findByTelNo(scope.bossPhone()));
        historicalNoStore.setName("HTTP历史空门店应付");
        historicalNoStore.setType(PayableType.Other);
        historicalNoStore.setPayeeName("历史待核验供应商");
        historicalNoStore.setAmount(new BigDecimal("5.00"));
        historicalNoStore.setPaidAmount(BigDecimal.ZERO);
        historicalNoStore.setDueDate(java.time.LocalDate.now());
        historicalNoStore.setStatus(PayableStatus.Pending);
        historicalNoStore.setUrgent(false);
        historicalNoStore.setCreatedBy(manager);
        historicalNoStore = payableRepository.save(historicalNoStore);
        entityManager.flush();

        JsonNode historicalPaid = api("finance/payment/save", """
                {"payableId":%s,"amount":1.00,"paidAt":"2026-10-22","idempotencyKey":"scope-historical-no-store-payment"}
                """.formatted(historicalNoStore.getPayableId()), headFinanceToken);
        assertThat(historicalPaid.path("success").asBoolean()).as(historicalPaid.toString()).isTrue();
        long historicalPaymentId = historicalPaid.path("data").path("paymentId").asLong();
        Payment historicalPayment = paymentRepository.findById(historicalPaymentId).orElseThrow();
        assertThat(historicalPayment.getStore()).isNull();
        Ledger historicalLedger = ledgerRepository
                .findFirstByCompany_ComIdAndSourceAndSourceIdOrderByLedgerIdAsc(
                        comId, LedgerSource.Payment, historicalPaymentId)
                .orElseThrow();
        assertThat(historicalLedger.getStore()).isNull();

        assertThat(comId).isNotNull();
        assertThat(headFinance.getCompany().getComId()).isEqualTo(comId);
    }

    private void assertCrossStoreRejected(JsonNode response, String action) {
        assertThat(response.path("success").asBoolean()).as(action + ": " + response).isFalse();
        assertThat(response.path("errorCode").path("code").asText()).as(action + " 错误码").isIn("403", "404");
    }

    @Test
    void storeManagerCanIssueAndReprintOwnStoreVoucherButNotOtherStore() throws Exception {
        String managerToken = login(scope.storeManagerPhone());
        String designerToken = login(scope.designerPhone());
        Ledger ownLedger = ledgerRepository.findAll().stream()
                .filter(item -> item.getStore() != null && scope.storeBId().equals(item.getStore().getStoreId()))
                .findFirst().orElseThrow();
        Ledger otherLedger = ledgerRepository.findAll().stream()
                .filter(item -> item.getStore() != null && scope.storeAId().equals(item.getStore().getStoreId()))
                .findFirst().orElseThrow();

        JsonNode first = api("document/voucher/build", """
                {"ledgerIds":[%s]}
                """.formatted(ownLedger.getLedgerId()), managerToken);
        assertThat(first.path("success").asBoolean()).as(first.toString()).isTrue();
        String voucherId = first.path("data").path("voucherId").asText();
        String voucherNo = first.path("data").path("voucherNo").asText();
        assertThat(voucherId).isNotBlank();
        assertThat(voucherNo).isNotBlank();

        JsonNode reprint = api("document/voucher/build", """
                {"ledgerIds":[%s]}
                """.formatted(ownLedger.getLedgerId()), managerToken);
        assertThat(reprint.path("success").asBoolean()).as(reprint.toString()).isTrue();
        assertThat(reprint.path("data").path("voucherId").asText()).isEqualTo(voucherId);
        assertThat(reprint.path("data").path("voucherNo").asText()).isEqualTo(voucherNo);

        JsonNode page = api("document/voucher/page",
                "{\"current\":1,\"size\":20,\"storeId\":%d}".formatted(scope.storeBId()), managerToken);
        assertThat(page.path("success").asBoolean()).as(page.toString()).isTrue();
        assertThat(page.path("data").path("content").findValuesAsText("voucherNo")).contains(voucherNo);

        JsonNode designerPage = api("document/voucher/page", "{}", designerToken);
        assertThat(designerPage.path("success").asBoolean()).as(designerPage.toString()).isFalse();
        assertThat(designerPage.path("errorCode").path("code").asText()).isEqualTo("403");

        JsonNode crossStore = api("document/voucher/build", """
                {"ledgerIds":[%s]}
                """.formatted(otherLedger.getLedgerId()), managerToken);
        assertThat(crossStore.path("success").asBoolean()).as(crossStore.toString()).isFalse();
        assertThat(crossStore.path("errorCode").path("code").asText()).isIn("403", "404");
    }

    @Test
    void designerIsRejectedFromFinanceWorkbenchOverHttp() throws Exception {
        String token = login(scope.designerPhone());
        JsonNode response = api("finance/summary/report", "{}", token);
        assertThat(response.path("success").asBoolean()).isFalse();
        assertThat(response.path("errorCode").path("code").asText()).isEqualTo("403");
    }

    @Test
    void anonymousIsRejectedOverHttp() throws Exception {
        JsonNode response = api("finance/summary/report", "{}", null);
        assertThat(response.path("success").asBoolean()).isFalse();
        assertThat(response.path("errorCode").path("code").asText())
                .isIn("401", "tokenIsUndefined", "tokenInvalid", "tokenExpired");
    }


    // ---------- helpers ----------

    private JsonNode api(String route, String body, String token) throws Exception {
        var request = post("/api/" + route)
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "finance-store-scope-acceptance")
                .content(body);
        if (token != null) {
            request.header("token", token);
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        String content = result.getResponse().getContentAsString();
        return content == null || content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    private String login(String phone) throws Exception {
        JsonNode response = api("auth/session/password-login",
                "{\"phone\":\"%s\",\"password\":\"%s\"}".formatted(phone, PASSWORD), null);
        assertThat(response.path("success").asBoolean()).as(response.toString()).isTrue();
        String token = response.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private Scope createScope() {
        long seq = System.nanoTime();
        Company company = company("财务门店范围HTTP验收", seq);
        Store storeA = store(company, "HTTP总店", true);
        Store storeB = store(company, "HTTP城北店", false);
        Company otherCompany = company("财务门店范围HTTP他企", seq + 7);
        Store foreignStore = store(otherCompany, "HTTP他企门店", false);

        Employee boss = employee(company, storeA, EmployeeRole.Boss, "HTTP负责人", seq, 1);
        Employee headFinance = employee(company, storeA, EmployeeRole.Finance, "HTTP总部财务", seq, 2);
        Employee storeFinance = employee(company, storeB, EmployeeRole.Finance, "HTTP门店财务", seq, 3);
        Employee storeManager = employee(company, storeB, EmployeeRole.StoreManager, "HTTP店长", seq, 4);
        Employee designer = employee(company, storeA, EmployeeRole.Designer, "HTTP设计师", seq, 5);

        Project projectA = project(company, storeA, boss, seq, 11, "100.00");
        Project projectB = project(company, storeB, boss, seq, 12, "200.00");
        entityManager.flush();

        return new Scope(storeA.getStoreId(), storeB.getStoreId(), foreignStore.getStoreId(),
                projectA.getProjectId(), projectB.getProjectId(),
                boss.getTelNo(), headFinance.getTelNo(), storeFinance.getTelNo(),
                storeManager.getTelNo(), designer.getTelNo());
    }

    private Company company(String title, long seq) {
        Company company = new Company();
        company.setTitle(title + Math.abs(seq % 1000));
        company.setName("财务门店范围HTTP企业");
        company.setSize(Size.A);
        return companyRepository.save(company);
    }

    private Store store(Company company, String name, boolean defaultStore) {
        Store store = new Store();
        store.setCompany(company);
        store.setName(name);
        store.setDefaultStore(defaultStore);
        return storeRepository.save(store);
    }

    private Employee employee(Company company, Store store, EmployeeRole role, String name, long seq, int offset) {
        String phone = "3" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L));
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        employee.setName(name);
        employee.setTelNo(phone);
        employee.setJoinedAt(LocalDateTime.now());
        employee = employeeRepository.save(employee);

        Credentials credential = new Credentials();
        credential.setType(CredType.Password);
        credential.setIdKey(phone);
        credential.setIdSecret("password");
        credential.setPhone(phone);
        credential.setStatus(UserStatus.Active);
        credential.setEmployee(employee);
        credential.setPasswordHash(passwordContext.encode(PASSWORD));
        credential.setPasswordUpdatedAt(LocalDateTime.now());
        credential.setFailedAttempts(0);
        credential.setMustChangePassword(false);
        credentialsRepository.save(credential);
        return employee;
    }

    private Project project(Company company, Store store, Employee operator, long seq, int offset,
                            String receivableAmount) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(operator);
        customer.setName("HTTP客户" + offset);
        customer.setTelNo("4" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        customer.setArea(new BigDecimal("120"));
        customer.setAddr("财务门店范围HTTP地址");
        customer = customerRepository.save(customer);

        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(operator);
        property.setCustomer(customer);
        property.setPropertyNo("PROP-HTTP-" + offset + "-" + Math.abs(seq % 100000));
        property.setAddress("财务门店范围HTTP小区 " + offset + " 栋 101");
        property.setArea(new BigDecimal("120"));
        property.setStatus(PropertyStatus.Serving);
        property = propertyRepository.save(property);

        Quote quote = new Quote();
        quote.setNo("QUOTE-HTTP-" + offset + "-" + Math.abs(seq % 100000));
        quote.setTitle("财务门店范围HTTP报价");
        quote.setCompany(company);
        quote.setStore(store);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(operator);
        quote.setVersion(1);
        quote.setMode(QuoteMode.Custom);
        quote.setArea(new BigDecimal("120"));
        quote.setSubtotal(new BigDecimal("100000"));
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setTaxRate(new BigDecimal("0.05"));
        quote.setAmount(new BigDecimal("100000"));
        quote.setCost(new BigDecimal("80000"));
        quote.setDesignFee(BigDecimal.ZERO);
        quote.setManagementFee(BigDecimal.ZERO);
        quote.setStatus(QuoteStatus.Accepted);
        quote = quoteRepository.save(quote);

        Project project = new Project();
        project.setNo("PROJECT-HTTP-" + offset + "-" + Math.abs(seq % 100000));
        project.setName("财务门店范围HTTP项目" + offset);
        project.setCompany(company);
        project.setStore(store);
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quote);
        project.setDesigner(operator);
        project.setManager(operator);
        project.setSupport(operator);
        project.setOwner(operator);
        project.setSiteAddr(property.getAddress());
        project.setArea(new BigDecimal("120"));
        project.setContractAmount(new BigDecimal("100000"));
        project.setCost(new BigDecimal("80000"));
        project.setStatus(ProjectStatus.Ongoing);
        project.setProgress(10);
        project.setStartDate(LocalDate.now());
        project.setPlanEndDate(LocalDate.now().plusDays(90));
        project = projectRepository.save(project);

        if (offset == 11) {
            Settlement settlement = new Settlement();
            settlement.setCompany(company);
            settlement.setStore(store);
            settlement.setOwner(operator);
            settlement.setProject(project);
            settlement.setContractAmount(new BigDecimal("100000.00"));
            settlement.setStatus(SettlementStatus.Draft);
            settlement.setCreatedBy(operator);
            settlementRepository.save(settlement);
        }

        Receivable receivable = new Receivable();
        receivable.setCompany(company);
        receivable.setStore(store);
        receivable.setOwner(operator);
        receivable.setProject(project);
        receivable.setCustomer(customer);
        receivable.setProperty(property);
        receivable.setName("HTTP首期款" + offset);
        receivable.setStageName("首期款");
        receivable.setType(ReceivableType.First);
        receivable.setAmount(new BigDecimal(receivableAmount));
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setDueDate(LocalDate.now().plusDays(30));
        receivable.setStatus(ReceivableStatus.Pending);
        receivable.setCreatedBy(operator);
        receivable = receivableRepository.save(receivable);

        Receipt receipt = new Receipt();
        receipt.setCompany(company);
        receipt.setStore(store);
        receipt.setOwner(operator);
        receipt.setProject(project);
        receipt.setReceivable(receivable);
        receipt.setCustomer(customer);
        receipt.setAmount(new BigDecimal(receivableAmount));
        receipt.setAccount(FundAccount.CompanyBank);
        receipt.setReceivedAt(LocalDate.now().minusDays(1));
        receipt.setPayerName("专属付款方-" + offset);
        receipt.setRemark("HTTP收款验收-" + offset);
        receipt.setCreatedBy(operator);
        receipt.setStatus(ReceiptStatus.Active);
        receipt = receiptRepository.save(receipt);

        ReceiptRefund refund = new ReceiptRefund();
        refund.setCompany(company);
        refund.setStore(store);
        refund.setOwner(operator);
        refund.setProject(project);
        refund.setReceipt(receipt);
        refund.setCustomer(customer);
        refund.setAmount(new BigDecimal(offset == 11 ? "10.00" : "20.00"));
        refund.setAccount(FundAccount.CompanyBank);
        refund.setRefundAt(LocalDate.now().minusDays(1));
        refund.setReason("HTTP退款验收-" + offset);
        refund.setCreatedBy(operator);
        receiptRefundRepository.save(refund);

        if (offset == 11) {
            Receipt reversed = new Receipt();
            reversed.setCompany(company);
            reversed.setStore(store);
            reversed.setOwner(operator);
            reversed.setProject(project);
            reversed.setReceivable(receivable);
            reversed.setCustomer(customer);
            reversed.setAmount(new BigDecimal("20.00"));
            reversed.setAccount(FundAccount.CompanyBank);
            reversed.setReceivedAt(LocalDate.now().minusDays(2));
            reversed.setPayerName("已冲正付款方-11");
            reversed.setRemark("HTTP冲正验收-11");
            reversed.setCreatedBy(operator);
            reversed.setStatus(ReceiptStatus.Reversed);
            receiptRepository.save(reversed);
        }

        Partner partner = new Partner();
        partner.setCompany(company);
        partner.setName("HTTP供应商-" + offset);
        partner.setCreatedBy(operator);
        partner = partnerRepository.save(partner);

        Payable payable = new Payable();
        payable.setCompany(company);
        payable.setStore(store);
        payable.setOwner(operator);
        payable.setProject(project);
        payable.setName("HTTP应付-" + offset);
        payable.setType(PayableType.Material);
        payable.setPayeeName("HTTP供应商-" + offset);
        payable.setPayeePartner(partner);
        payable.setAmount(new BigDecimal("30.00"));
        payable.setPaidAmount(new BigDecimal("30.00"));
        payable.setDueDate(LocalDate.now());
        payable.setStatus(PayableStatus.Paid);
        payable.setUrgent(false);
        payable.setCreatedBy(operator);
        payable = payableRepository.save(payable);

        if (offset == 11) {
            Payable urgentUnpaid = new Payable();
            urgentUnpaid.setCompany(company);
            urgentUnpaid.setStore(store);
            urgentUnpaid.setOwner(operator);
            urgentUnpaid.setProject(project);
            urgentUnpaid.setName("HTTP紧急未付-" + offset);
            urgentUnpaid.setType(PayableType.Material);
            urgentUnpaid.setPayeeName("HTTP紧急供应商-" + offset);
            urgentUnpaid.setPayeePartner(partner);
            urgentUnpaid.setAmount(new BigDecimal("25.00"));
            urgentUnpaid.setPaidAmount(BigDecimal.ZERO);
            urgentUnpaid.setDueDate(LocalDate.now().minusDays(1));
            urgentUnpaid.setStatus(PayableStatus.Overdue);
            urgentUnpaid.setUrgent(true);
            urgentUnpaid.setCreatedBy(operator);
            payableRepository.save(urgentUnpaid);
        }

        Payment payment = new Payment();
        payment.setCompany(company);
        payment.setStore(store);
        payment.setOwner(operator);
        payment.setProject(project);
        payment.setPayable(payable);
        payment.setAmount(new BigDecimal(offset == 11 ? "30.00" : "40.00"));
        payment.setAccount(FundAccount.CompanyBank);
        payment.setPaidAt(LocalDate.now());
        payment.setPayeeName("HTTP付款方-" + offset);
        payment.setTransactionNo("PAY-" + offset);
        payment.setRemark("HTTP付款验收-" + offset);
        payment.setCreatedBy(operator);
        paymentRepository.save(payment);

        PayrollSheet sheet = new PayrollSheet();
        sheet.setCompany(company);
        sheet.setStore(store);
        sheet.setOwner(operator);
        sheet.setPeriod(offset == 11 ? "2026-08" : "2026-09");
        sheet.setStatus(PayrollSheetStatus.Draft);
        sheet.setAttendanceComplete(false);
        sheet.setCommissionComplete(false);
        sheet.setTotalGrossAmount(new BigDecimal("80.00"));
        sheet.setTotalDeductionAmount(new BigDecimal("5.00"));
        sheet.setTotalNetAmount(new BigDecimal("75.00"));
        sheet.setCreatedBy(operator);
        payrollSheetRepository.save(sheet);

        CommissionItem commission = new CommissionItem();
        commission.setCompany(company);
        commission.setStore(store);
        commission.setTeam(operator.getTeam());
        commission.setEmployee(operator);
        commission.setProject(project);
        commission.setContractAmount(new BigDecimal("1000.00"));
        commission.setManualRate(new BigDecimal("5.0000"));
        commission.setCommissionAmount(new BigDecimal(offset == 11 ? "60.00" : "50.00"));
        commission.setAccruedAmount(new BigDecimal(offset == 11 ? "40.00" : "30.00"));
        commission.setReleasedAmount(new BigDecimal(offset == 11 ? "30.00" : "20.00"));
        commission.setPaidAmount(new BigDecimal("10.00"));
        commission.setStatus(CommissionStatus.Released);
        commission.setCreatedBy(operator);
        commissionItemRepository.save(commission);

        ledger(company, store, operator, project, LedgerDirection.Income,
                new BigDecimal(offset == 11 ? "100.00" : "40.00"), "HTTP台账收入-" + offset);
        if (offset == 11) {
            ledger(company, store, operator, project, LedgerDirection.Expense,
                    new BigDecimal("30.00"), "HTTP台账支出-" + offset);
        }
        return project;
    }

    private void ledger(Company company, Store store, Employee operator, Project project,
                        LedgerDirection direction, BigDecimal amount, String summary) {
        Ledger ledger = new Ledger();
        ledger.setCompany(company);
        ledger.setStore(store);
        ledger.setOwner(operator);
        ledger.setProject(project);
        ledger.setDirection(direction);
        ledger.setCategory(direction == LedgerDirection.Income ? "项目收款" : "项目支出");
        ledger.setPurpose(LedgerManualPurpose.ProjectIncidentalCost);
        ledger.setSource(LedgerSource.Manual);
        ledger.setAmount(amount);
        ledger.setAccount(FundAccount.CompanyBank);
        ledger.setOccurredAt(LocalDate.now());
        ledger.setCounterparty("HTTP对方");
        ledger.setSummary(summary);
        ledger.setRemark("HTTP台账验收");
        ledger.setCreatedBy(operator);
        ledger.setVoided(false);
        ledgerRepository.save(ledger);
    }

    private record Scope(Long storeAId, Long storeBId, Long foreignStoreId,
                         Long projectAId, Long projectBId,
                         String bossPhone, String headFinancePhone, String storeFinancePhone,
                         String storeManagerPhone, String designerPhone) {
    }
}
