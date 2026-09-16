package com.th4.edge.decoboon.statistic.service.target.save.support;

import com.th4.edge.decoboon.statistic.model.BusinessTarget;
import com.th4.edge.decoboon.statistic.repository.BusinessTargetRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 企业月度经营目标保存实现。 */
@Note("企业月度经营目标保存")
@Properties(allow = {
        @AllowProperties(model = BusinessTarget.class,
                properties = {"targetId", "targetMonth", "signedAmount", "receivedAmount", "grossProfit",
                        "activeProjectCount", "createTime", "updateTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("statistic/target/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.statistic.service.target.save.Save {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private LocalDate month;
    @AutoInject
    private BigDecimal signedAmount;
    @AutoInject
    private BigDecimal receivedAmount;
    @AutoInject
    private BigDecimal grossProfit;
    @AutoInject
    private Integer activeProjectCount;

    private final TenantContext tenantContext;
    private final BusinessTargetRepository targetRepository;

    public Save(TenantContext tenantContext, BusinessTargetRepository targetRepository) {
        this.tenantContext = tenantContext;
        this.targetRepository = targetRepository;
    }

    @Override
    @Transactional
    public BusinessTarget call(BusinessTarget request) {
        Employee current = tenantContext.requireBoss(employee);
        LocalDate targetMonth = month.withDayOfMonth(1);
        requireNonNegative(signedAmount, "签约目标");
        requireNonNegative(receivedAmount, "回款目标");
        requireNonNegative(grossProfit, "毛利目标");
        if (activeProjectCount != null && activeProjectCount < 0) {
            throw new DefinedException(new DefaultErrorCode("400", "在建项目目标数不能小于 0"));
        }

        BusinessTarget target = targetRepository
                .findByCompany_ComIdAndTargetMonth(current.getCompany().getComId(), targetMonth)
                .orElseGet(BusinessTarget::new);
        target.setCompany(current.getCompany());
        target.setTargetMonth(targetMonth);
        target.setSignedAmount(value(signedAmount));
        target.setReceivedAmount(value(receivedAmount));
        target.setGrossProfit(value(grossProfit));
        target.setActiveProjectCount(activeProjectCount == null ? 0 : activeProjectCount);
        return targetRepository.save(target);
    }

    private void requireNonNegative(BigDecimal value, String label) {
        if (value != null && value.signum() < 0) {
            throw new DefinedException(new DefaultErrorCode("400", label + "不能小于 0"));
        }
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
