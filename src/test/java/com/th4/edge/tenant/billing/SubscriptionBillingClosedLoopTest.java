package com.th4.edge.tenant.billing;

import com.th4.edge.tenant.billing.configure.OrderItemType;
import com.th4.edge.tenant.billing.configure.OrderStatus;
import com.th4.edge.tenant.billing.configure.PayChannel;
import com.th4.edge.tenant.billing.configure.SubStatus;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.model.Order;
import com.th4.edge.tenant.billing.model.OrderItem;
import com.th4.edge.tenant.billing.model.Plan;
import com.th4.edge.tenant.billing.model.PlanAddon;
import com.th4.edge.tenant.billing.model.Subscription;
import com.th4.edge.tenant.billing.repository.BalanceLedgerRepository;
import com.th4.edge.tenant.billing.repository.OrderItemRepository;
import com.th4.edge.tenant.billing.repository.OrderRepository;
import com.th4.edge.tenant.billing.repository.PlanAddonRepository;
import com.th4.edge.tenant.billing.repository.PlanRepository;
import com.th4.edge.tenant.billing.repository.SubscriptionRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Setting;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 试用、到期、购买、支付履约和续费顺延的数据库闭环测试。
 */
@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:subscription-billing;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.components.lock.enabled=false",
        "logging.level.org.hibernate.SQL=OFF"
})
@Transactional
class SubscriptionBillingClosedLoopTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private PlanRepository planRepository;
    @Autowired private PlanAddonRepository planAddonRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private BalanceLedgerRepository balanceLedgerRepository;
    @Autowired private SubscriptionContext subscriptionContext;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    @Test
    void shouldCloseTrialToPaidSubscriptionAndRenewFromExistingEndTime() {
        Company company = company("订阅闭环测试企业");
        Store store = store(company, "订阅测试总店");
        Employee boss = boss(company, store, "13800009001", "订阅测试负责人");

        Subscription trial = subscriptionContext.grantTrial(company);
        assertThat(trial.getStatus()).isEqualTo(SubStatus.Trial);
        assertThat(trial.getTrialActivated()).isTrue();
        assertThat(trial.getPlan().getCode()).isEqualTo("annual-basic");
        assertThat(trial.getPlanVersion().getPrice()).isEqualTo(598000L);
        assertThat(subscriptionContext.remainingDays(trial)).isBetween(13, 14);

        LocalDateTime expiredAt = LocalDateTime.now().minusDays(1).withNano(0);
        adjustSubscriptionEnd(company, expiredAt);
        Subscription expired = subscriptionRepository.findByCompany_ComId(company.getComId());
        assertThat(expired.getStatus()).isEqualTo(SubStatus.Trial);
        assertThat(subscriptionContext.isExpired(company)).isTrue();
        assertThat(subscriptionContext.isWritable(company)).isFalse();

        Plan pro = planRepository.findByCode("annual-pro");
        PlanAddon seatPack = planAddonRepository.findByCode("seat-pack");
        assertThat(pro).isNotNull();
        assertThat(seatPack).isNotNull();

        Order purchase = subscriptionContext.createOrder(
                company, pro.getCode(), 1, 0, PayChannel.Wx);
        assertThat(purchase.getStatus()).isEqualTo(OrderStatus.Pending);
        assertThat(purchase.getOutTradeNo()).isEqualTo(purchase.getOrderNo());
        assertThat(purchase.getExpireTime()).isAfter(LocalDateTime.now());
        assertThat(purchase.getAmount()).isEqualTo(pro.getPrice() + seatPack.getUnitPrice());

        List<OrderItem> purchaseItems =
                orderItemRepository.findByOrder_OrderIdOrderByOrderItemIdAsc(purchase.getOrderId());
        assertThat(purchaseItems).extracting(OrderItem::getItemType)
                .containsExactly(OrderItemType.Plan, OrderItemType.Seats);

        Order grantedPurchase = subscriptionContext.fulfillOrder(
                purchase.getOrderNo(), purchase.getOutTradeNo(), "wx-tx-purchase-001",
                purchase.getAmount(), PayChannel.Wx);
        assertThat(grantedPurchase.getStatus()).isEqualTo(OrderStatus.Granted);
        assertThat(grantedPurchase.getTransactionId()).isEqualTo("wx-tx-purchase-001");
        assertThat(grantedPurchase.getPaidTime()).isNotNull();

        Subscription active = subscriptionRepository.findByCompany_ComId(company.getComId());
        assertThat(active.getStatus()).isEqualTo(SubStatus.Active);
        assertThat(active.getPlan().getCode()).isEqualTo("annual-pro");
        assertThat(active.getExtraSeats()).isEqualTo(1);
        assertThat(active.getServiceEndTime().toLocalDateTime()).isAfter(LocalDateTime.now().plusDays(360));
        assertThat(subscriptionContext.isWritable(company)).isTrue();

        LocalDateTime firstPaidEnd = active.getServiceEndTime().toLocalDateTime();
        adjustSubscriptionEnd(company, LocalDateTime.now().plusDays(30).withNano(0));
        Subscription nearExpiry = subscriptionRepository.findByCompany_ComId(company.getComId());
        LocalDateTime existingEnd = nearExpiry.getServiceEndTime().toLocalDateTime();
        assertThat(subscriptionContext.remainingDays(nearExpiry)).isBetween(29, 30);

        Order renewal = subscriptionContext.createOrder(
                company, pro.getCode(), 0, 0, PayChannel.Wx);
        Order grantedRenewal = subscriptionContext.fulfillOrder(
                renewal.getOrderNo(), renewal.getOutTradeNo(), "wx-tx-renew-002",
                renewal.getAmount(), PayChannel.Wx);
        assertThat(grantedRenewal.getStatus()).isEqualTo(OrderStatus.Granted);

        Subscription renewed = subscriptionRepository.findByCompany_ComId(company.getComId());
        assertThat(renewed.getStatus()).isEqualTo(SubStatus.Active);
        assertThat(renewed.getServiceEndTime().toLocalDateTime()).isEqualTo(existingEnd.plusDays(365));
        assertThat(renewed.getServiceEndTime().toLocalDateTime()).isAfter(firstPaidEnd);
        assertThat(renewed.getLastRenewTime()).isNotNull();

        assertThat(orderRepository.findByOrderNo(purchase.getOrderNo()).getStatus())
                .isEqualTo(OrderStatus.Granted);
        assertThat(balanceLedgerRepository.findByCompany_ComId(company.getComId())).hasSize(3);
        assertThat(boss.getCompany().getComId()).isEqualTo(company.getComId());
    }

    private Company company(String title) {
        Company company = new Company();
        company.setTitle(title);
        company.setName(title.substring(0, Math.min(12, title.length())));
        Setting setting = new Setting();
        setting.setEnableStore(false);
        company.setSetting(setting);
        return companyRepository.save(company);
    }

    private Store store(Company company, String name) {
        Store store = new Store();
        store.setCompany(company);
        store.setName(name);
        store.setDefaultStore(true);
        return storeRepository.save(store);
    }

    private Employee boss(Company company, Store store, String phone, String name) {
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setTelNo(phone);
        employee.setName(name);
        employee.setRole(EmployeeRole.Boss);
        employee.setStatus(EmployeeStatus.Active);
        employee.setJoinedAt(LocalDateTime.now());
        return employeeRepository.save(employee);
    }

    private void adjustSubscriptionEnd(Company company, LocalDateTime serviceEndTime) {
        entityManager.flush();
        long dateTime = Long.parseLong(serviceEndTime.format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        int date = serviceEndTime.getYear() * 10000
                + serviceEndTime.getMonthValue() * 100 + serviceEndTime.getDayOfMonth();
        int time = serviceEndTime.getHour() * 10000
                + serviceEndTime.getMinute() * 100 + serviceEndTime.getSecond();
        jdbcTemplate.update("""
                update db_tenant_subscription
                   set service_end_date_time = ?,
                       service_end_date = ?,
                       service_end_time = ?,
                       service_end_year = ?,
                       service_end_month = ?,
                       service_end_day = ?,
                       service_end_hour = ?,
                       service_end_minute = ?,
                       service_end_second = ?,
                       service_end_week = ?
                 where com_id = ?
                """,
                dateTime,
                date,
                time,
                serviceEndTime.getYear(),
                serviceEndTime.getMonthValue(),
                serviceEndTime.getDayOfMonth(),
                serviceEndTime.getHour(),
                serviceEndTime.getMinute(),
                serviceEndTime.getSecond(),
                serviceEndTime.getDayOfWeek().getValue() - 1,
                company.getComId());
        entityManager.clear();
    }
}
