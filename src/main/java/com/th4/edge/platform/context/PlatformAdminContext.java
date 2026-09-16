package com.th4.edge.platform.context;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.platform.model.PlatformAction;
import com.th4.edge.platform.model.PlatformOperationLog;
import com.th4.edge.platform.repository.PlatformOperationLogRepository;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.model.Subscription;
import com.th4.edge.tenant.billing.repository.SubscriptionRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** 平台运营权限、租户快照和人工操作审计上下文。 */
@Note("平台运营上下文")
@Component
public class PlatformAdminContext {

    private final TenantContext tenantContext;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final StoreRepository storeRepository;
    private final SubscriptionContext subscriptionContext;
    private final SubscriptionRepository subscriptionRepository;
    private final PlatformOperationLogRepository operationLogRepository;

    public PlatformAdminContext(TenantContext tenantContext,
                                CompanyRepository companyRepository,
                                EmployeeRepository employeeRepository,
                                StoreRepository storeRepository,
                                SubscriptionContext subscriptionContext,
                                SubscriptionRepository subscriptionRepository,
                                PlatformOperationLogRepository operationLogRepository) {
        this.tenantContext = tenantContext;
        this.companyRepository = companyRepository;
        this.employeeRepository = employeeRepository;
        this.storeRepository = storeRepository;
        this.subscriptionContext = subscriptionContext;
        this.subscriptionRepository = subscriptionRepository;
        this.operationLogRepository = operationLogRepository;
    }

    /**
     * 平台运维只允许平台运营企业中的负责人账号访问。
     * 该权限是平台基础设施能力，不复用普通租户的数据范围，必须先验证企业白名单。
     */
    @Transactional(readOnly = true)
    public Employee requireAdmin(Employee principal) {
        Employee operator = tenantContext.requireEmployee(principal);
        Company platformCompany = operator.getCompany();
        if (platformCompany == null || !Boolean.TRUE.equals(platformCompany.getPlatform())) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号不是平台运营账号"));
        }
        if (operator.getRole() != EmployeeRole.Boss) {
            throw new DefinedException(new DefaultErrorCode("403", "平台运营账号必须具备负责人权限"));
        }
        return operator;
    }

    @Transactional(readOnly = true)
    public Company requireTenant(Long comId) {
        Company company = companyRepository.findByComId(comId);
        if (company == null || Boolean.TRUE.equals(company.getPlatform())) {
            throw new DefinedException(new DefaultErrorCode("404", "企业不存在"));
        }
        return company;
    }

    @Transactional(readOnly = true)
    public Subscription requireSubscription(Long subId) {
        Subscription subscription = subscriptionRepositoryFind(subId);
        if (subscription == null || subscription.getCompany() == null
                || Boolean.TRUE.equals(subscription.getCompany().getPlatform())) {
            throw new DefinedException(new DefaultErrorCode("404", "订阅不存在"));
        }
        enrich(subscription);
        return subscription;
    }

    @Transactional(readOnly = true)
    public void enrich(Company company) {
        if (company == null) return;
        Subscription subscription = subscriptionContext.current(company);
        if (subscription != null) enrich(subscription);
        company.setPlanName(subscription != null && subscription.getPlanVersion() != null
                ? subscription.getPlanVersion().getPlanName()
                : subscription != null && subscription.getPlan() != null ? subscription.getPlan().getName() : "未订阅");
        company.setSubscriptionStatus(subscription != null && subscription.getStatus() != null
                ? subscription.getStatus().name() : "None");
        company.setServiceEndTime(subscription == null ? null : subscription.getServiceEndTime());
        company.setRemainingDays(subscription == null ? null : subscriptionContext.remainingDays(subscription));
        company.setUsedSeats(employeeRepository.countByCompany_ComId(company.getComId()));
        company.setEffectiveMaxSeats(subscription == null ? null : subscriptionContext.effectiveMaxSeats(subscription));
        company.setUsedStores(storeRepository.countByCompany_ComId(company.getComId()));
        company.setEffectiveMaxStores(subscription == null ? null : subscriptionContext.effectiveMaxStores(subscription));
    }

    public void enrich(Subscription subscription) {
        if (subscription == null) return;
        Company company = subscription.getCompany();
        subscription.setRemainingDays(subscriptionContext.remainingDays(subscription));
        subscription.setUsedSeats(company == null ? 0L : employeeRepository.countByCompany_ComId(company.getComId()));
        subscription.setUsedStores(company == null ? 0L : storeRepository.countByCompany_ComId(company.getComId()));
        subscription.setEffectiveMaxSeats(subscriptionContext.effectiveMaxSeats(subscription));
        subscription.setEffectiveMaxStores(subscriptionContext.effectiveMaxStores(subscription));
        subscription.setWritable(subscriptionContext.isWritable(company));
        subscription.setExpired(subscriptionContext.isExpired(company));
    }

    @Transactional
    public PlatformOperationLog audit(Employee operator,
                                      Company company,
                                      PlatformAction action,
                                      String targetType,
                                      String targetKey,
                                      String summary,
                                      Map<String, Object> before,
                                      Map<String, Object> after,
                                      String reason) {
        PlatformOperationLog log = new PlatformOperationLog();
        log.setOperator(operator);
        log.setCompany(company);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetKey(targetKey);
        log.setSummary(summary);
        log.setBeforeValue(before == null ? null : before.toString());
        log.setAfterValue(after == null ? null : after.toString());
        log.setReason(reason);
        return operationLogRepository.save(log);
    }

    public Map<String, Object> subscriptionSnapshot(Subscription subscription) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("subId", subscription.getSubId());
        value.put("planCode", subscription.getPlanVersion() != null
                ? subscription.getPlanVersion().getPlanCode()
                : subscription.getPlan() == null ? null : subscription.getPlan().getCode());
        value.put("status", subscription.getStatus() == null ? null : subscription.getStatus().name());
        value.put("serviceEndTime", subscription.getServiceEndTime() == null
                ? null : subscription.getServiceEndTime().toLocalDateTime());
        value.put("extraSeats", subscription.getExtraSeats());
        value.put("extraStores", subscription.getExtraStores());
        return value;
    }

    private Subscription subscriptionRepositoryFind(Long subId) {
        return subscriptionRepository.findBySubId(subId);
    }
}
