package com.th4.edge.decoboon.ai.context;

import com.th4.edge.decoboon.ai.configure.AiBillingStatus;
import com.th4.edge.decoboon.ai.configure.AiStoreAllocation;
import com.th4.edge.decoboon.ai.configure.AiUsageStatus;
import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.decoboon.ai.repository.AiUsageCallRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * AI 调用底账写入上下文。
 *
 * <p>P0 只记录调用、资源单位、影子点数和门店归属，不扣真实点数。
 * 所有 AI 能力应通过该上下文上报，禁止各业务 Provider 自行拼装统计口径。</p>
 */
@Component
public class AiUsageContext {

    private static final String DEFAULT_RULE_VERSION = "store-allocation-v1";
    private static final String DEFAULT_RATE_CARD = "shadow-direct-v1";

    private final TenantContext tenantContext;
    private final AiUsageCallRepository repository;

    public AiUsageContext(TenantContext tenantContext, AiUsageCallRepository repository) {
        this.tenantContext = tenantContext;
        this.repository = repository;
    }

    /**
     * 记录或幂等返回一次 AI 调用。
     */
    @Transactional
    public AiUsageCall record(Employee principal, AiUsageCall request) {
        Employee current = tenantContext.requireEmployee(principal);
        if (request == null) {
            throw new DefinedException(new DefaultErrorCode("400", "AI调用记录不能为空"));
        }
        String idempotencyKey = trim(request.getIdempotencyKey());
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new DefinedException(new DefaultErrorCode("400", "AI调用幂等键不能为空"));
        }
        if (!StringUtils.hasText(trim(request.getBusinessModule()))) {
            throw new DefinedException(new DefaultErrorCode("400", "AI业务模块不能为空"));
        }
        if (!StringUtils.hasText(trim(request.getCapabilityCode()))) {
            throw new DefinedException(new DefaultErrorCode("400", "AI能力编码不能为空"));
        }
        Company company = current.getCompany();
        if (company == null || company.getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "当前员工未归属企业"));
        }
        AiUsageCall existed = repository.findByCompany_ComIdAndIdempotencyKey(company.getComId(), idempotencyKey)
                .orElse(null);
        if (existed != null) {
            return existed;
        }

        Store businessStore = request.getBusinessStore();
        requireSameCompany(company, businessStore);
        request.setIdempotencyKey(idempotencyKey);
        request.setCompany(company);
        request.setEmployee(current);
        request.setEmployeeStore(current.getStore());
        applyAllocation(request, businessStore, current.getStore());
        normalizeUnits(request);
        normalizeStatus(request, current);
        return repository.save(request);
    }

    private void applyAllocation(AiUsageCall request, Store businessStore, Store employeeStore) {
        request.setBusinessStore(businessStore);
        if (businessStore != null) {
            request.setAllocationStore(businessStore);
            request.setAllocationType(AiStoreAllocation.BusinessStore);
        } else if (request.getAllocationType() == AiStoreAllocation.EnterprisePublic) {
            request.setAllocationStore(null);
            request.setAllocationType(AiStoreAllocation.EnterprisePublic);
        } else if (employeeStore != null) {
            request.setAllocationStore(employeeStore);
            request.setAllocationType(AiStoreAllocation.EmployeeStore);
        } else {
            request.setAllocationStore(null);
            request.setAllocationType(AiStoreAllocation.EnterprisePublic);
        }
        request.setAllocationRuleVersion(DEFAULT_RULE_VERSION);
    }

    private void normalizeUnits(AiUsageCall request) {
        long input = nonNegative(request.getInputUnits());
        long output = nonNegative(request.getOutputUnits());
        long resource = request.getResourceUnits() == null
                ? input + output : Math.max(0L, request.getResourceUnits());
        request.setInputUnits(input);
        request.setOutputUnits(output);
        request.setResourceUnits(resource);
        request.setRefundedUnits(nonNegative(request.getRefundedUnits()));
        request.setEstimated(Boolean.TRUE.equals(request.getEstimated()));
        if (!StringUtils.hasText(request.getRateCardVersion())) {
            request.setRateCardVersion(DEFAULT_RATE_CARD);
        }
        if (request.getShadowBillableUnits() == null) {
            request.setShadowBillableUnits(request.getStatus() == AiUsageStatus.Succeeded ? resource : 0L);
        } else {
            request.setShadowBillableUnits(Math.max(0L, request.getShadowBillableUnits()));
        }
    }

    private void normalizeStatus(AiUsageCall request, Employee current) {
        if (request.getStatus() == null) {
            request.setStatus(AiUsageStatus.Requested);
        }
        if (request.getStartedAt() == null) {
            request.setStartedAt(LocalDateTime.now());
        }
        if (request.getCompletedAt() != null && request.getDurationMs() == null) {
            request.setDurationMs(Math.max(0L, Duration.between(request.getStartedAt(), request.getCompletedAt()).toMillis()));
        }
        if (request.getBillingStatus() == null) {
            if (request.getStatus() == AiUsageStatus.Succeeded) {
                request.setBillingStatus(request.getShadowBillableUnits() > 0
                        ? AiBillingStatus.Calculated : AiBillingStatus.Void);
            } else if (request.getStatus() == AiUsageStatus.Failed
                    || request.getStatus() == AiUsageStatus.Cancelled
                    || request.getStatus() == AiUsageStatus.Expired
                    || request.getStatus() == AiUsageStatus.DuplicateBlocked) {
                request.setBillingStatus(AiBillingStatus.Void);
            } else {
                request.setBillingStatus(AiBillingStatus.Pending);
            }
        }
        if (request.getEmployee() == null) {
            request.setEmployee(current);
        }
    }

    private void requireSameCompany(Company company, Store store) {
        if (store == null || store.getCompany() == null) {
            return;
        }
        if (!Objects.equals(company.getComId(), store.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("403", "不能关联其他企业的门店"));
        }
    }

    private long nonNegative(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
