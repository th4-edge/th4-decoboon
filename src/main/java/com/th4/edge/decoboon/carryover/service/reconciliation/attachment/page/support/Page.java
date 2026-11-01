package com.th4.edge.decoboon.carryover.service.reconciliation.attachment.page.support;

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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 双轨对账附件分页实现。 */
@Note("双轨对账附件分页")
@Properties(allow = {
        @AllowProperties(model = CarryoverReconciliationAttachment.class,
        properties = {"attachmentId", "category", "fileUrl", "originalName", "uploadedBy", "createTime"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Employee.class, properties = {"empId", "name", "role", "status"})
})
@Service("carryover/reconciliation/attachment/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.carryover.service.reconciliation.attachment.page.Page {

    @User private Employee employee;
    @AutoInject @NotNull private Long reconciliationId;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    private final CarryoverReconciliationContext context;

    public Page(CarryoverReconciliationContext context) { this.context = context; }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CarryoverReconciliationAttachment> call(CarryoverReconciliationAttachment request) {
        List<CarryoverReconciliationAttachment> items = context.attachments(employee, reconciliationId);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
