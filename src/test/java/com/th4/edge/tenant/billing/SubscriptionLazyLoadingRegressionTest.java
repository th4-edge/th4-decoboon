package com.th4.edge.tenant.billing;

import com.th4.edge.tenant.billing.configure.PlanModule;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.model.Subscription;
import com.th4.edge.tenant.billing.repository.SubscriptionRepository;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Setting;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 生产懒加载回归：订阅校验链路在事务外访问 planVersion/plan。
 *
 * <p>生产 {@code application-prod.yml} 设置 {@code spring.jpa.open-in-view=false}，
 * 而 dev/test 默认 true 会把该缺陷掩盖。因此本测试显式关闭 open-in-view，
 * 复现 {@code SubscriptionAccessRequestFilter -> SubscriptionContext.requireModule}
 * 在事务外触碰未初始化代理导致的 LazyInitializationException。</p>
 *
 * <p>注意：测试方法**不能**加 {@code @Transactional}，否则 Session 会覆盖整个方法，
 * 缺陷将无法复现。</p>
 */
@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:subscription-lazy-regression;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        // 关键：与生产一致，关闭 Open Session In View
        "spring.jpa.open-in-view=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.components.lock.enabled=false",
        "logging.level.org.hibernate.SQL=OFF"
})
class SubscriptionLazyLoadingRegressionTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private SubscriptionContext subscriptionContext;

    /** 查询路由：filter 调用 requireModule → effectiveVersion → planVersion.getModules()。 */
    @Test
    void requireModuleMustWorkOutsideTransactionWhenOpenInViewDisabled() {
        Company company = company("懒加载回归企业A");
        subscriptionContext.grantTrial(company);
        assertLoaderPresent(company);

        assertThatCode(() -> subscriptionContext.requireModule(company, PlanModule.Project))
                .as("requireModule 在事务外访问 planVersion 不应抛 LazyInitializationException")
                .doesNotThrowAnyException();
    }

    /** 席位校验：requireSeatCapacity → effectiveMaxSeats → effectiveVersion → planVersion.getMaxSeats()。 */
    @Test
    void requireSeatCapacityMustWorkOutsideTransactionWhenOpenInViewDisabled() {
        Company company = company("懒加载回归企业B");
        subscriptionContext.grantTrial(company);
        assertLoaderPresent(company);

        assertThatCode(() -> subscriptionContext.requireSeatCapacity(company, 1))
                .as("requireSeatCapacity 在事务外访问 planVersion 不应抛 LazyInitializationException")
                .doesNotThrowAnyException();
    }

    /** 门店校验：requireStoreCapacity → effectiveMaxStores → effectiveVersion → planVersion.getMaxStores()。 */
    @Test
    void requireStoreCapacityMustWorkOutsideTransactionWhenOpenInViewDisabled() {
        Company company = company("懒加载回归企业C");
        subscriptionContext.grantTrial(company);
        assertLoaderPresent(company);

        assertThatCode(() -> subscriptionContext.requireStoreCapacity(company, 0))
                .as("requireStoreCapacity 在事务外访问 planVersion 不应抛 LazyInitializationException")
                .doesNotThrowAnyException();
    }

    /**
     * 断言订阅确实关联了 planVersion（否则用例会因“无版本走 null 分支”而假通过）。
     */
    private void assertLoaderPresent(Company company) {
        Subscription stored = subscriptionRepository.findByCompany_ComId(company.getComId());
        assertThat(stored).isNotNull();
        assertThat(stored.getPlanVersion()).isNotNull();
        assertThat(stored.getPlan()).isNotNull();
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
}
