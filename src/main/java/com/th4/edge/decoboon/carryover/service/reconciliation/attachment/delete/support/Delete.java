package com.th4.edge.decoboon.carryover.service.reconciliation.attachment.delete.support;

import com.th4.edge.decoboon.carryover.context.CarryoverReconciliationContext;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationAttachment;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 双轨对账附件删除实现。 */
@Note("删除双轨对账附件")
@Properties(allow = @AllowProperties(model = CarryoverReconciliationAttachment.class,
        properties = {"attachmentId", "category", "fileUrl", "originalName"}))
@Service("carryover/reconciliation/attachment/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.carryover.service.reconciliation.attachment.delete.Delete {

    @User private Employee employee;
    @AutoInject @NotNull private Long attachmentId;
    private final CarryoverReconciliationContext context;

    public Delete(CarryoverReconciliationContext context) { this.context = context; }

    @Override
    @Transactional
    public CarryoverReconciliationAttachment call(CarryoverReconciliationAttachment request) {
        context.deleteAttachment(employee, attachmentId);
        return request;
    }
}
