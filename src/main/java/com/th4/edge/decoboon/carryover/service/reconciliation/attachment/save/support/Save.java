package com.th4.edge.decoboon.carryover.service.reconciliation.attachment.save.support;

import com.th4.edge.decoboon.carryover.context.CarryoverReconciliationContext;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationAttachment;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 双轨对账附件保存实现。 */
@Note("保存双轨对账附件")
@Properties(allow = @AllowProperties(model = CarryoverReconciliationAttachment.class,
        properties = {"attachmentId", "category", "fileUrl", "originalName", "uploadedBy", "createTime"}))
@Service("carryover/reconciliation/attachment/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.carryover.service.reconciliation.attachment.save.Save {

    @User private Employee employee;
    @AutoInject @NotNull private Long reconciliationId;
    @AutoInject @Length(max = 32) private String category;
    @AutoInject @NotEmpty @Length(max = 1000) private String fileUrl;
    @AutoInject @Length(max = 255) private String originalName;
    private final CarryoverReconciliationContext context;

    public Save(CarryoverReconciliationContext context) { this.context = context; }

    @Override
    @Transactional
    public CarryoverReconciliationAttachment call(CarryoverReconciliationAttachment request) {
        return context.saveAttachment(employee, reconciliationId, category, fileUrl, originalName);
    }
}
