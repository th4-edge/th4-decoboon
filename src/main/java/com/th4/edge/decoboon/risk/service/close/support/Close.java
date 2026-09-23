package com.th4.edge.decoboon.risk.service.close.support;

import com.th4.edge.decoboon.risk.context.RiskDispositionContext;
import com.th4.edge.decoboon.risk.model.RiskDispositionRequest;
import com.th4.edge.decoboon.risk.model.RiskItem;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;

import java.util.List;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/** 关闭风险实现。 */
@Note("关闭风险")
@Properties(allow = {
        @AllowProperties(model = RiskItem.class,
                properties = {"riskId", "alertKey", "status", "owner", "closedAt", "closedBy", "closeResult", "roundNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("risk/item/close")
@Scope(ScopeTypes.PROTOTYPE)
public class Close implements com.th4.edge.decoboon.risk.service.close.Close {

    @User
    private Employee employee;

    @AutoInject
    private Long riskId;

    @AutoInject
    private String result;

    @AutoInject
    private String note;

    @AutoInject
    private List<String> evidenceUrls;


    private final RiskDispositionContext context;

    public Close(RiskDispositionContext context) { this.context = context; }

    @Override
    @Transactional
    public RiskItem call(RiskDispositionRequest request) {
        String value = result == null || result.isBlank() ? note : result;
        return context.close(employee, riskId, value, evidenceUrls);
    }
}
