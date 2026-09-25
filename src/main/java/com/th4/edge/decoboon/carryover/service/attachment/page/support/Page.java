package com.th4.edge.decoboon.carryover.service.attachment.page.support;

import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.model.CarryoverAttachment;
import com.th4.edge.decoboon.carryover.repository.CarryoverAttachmentRepository;
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

import java.util.List;

@Note("承接附件列表")
@Properties(allow = {
        @AllowProperties(model = CarryoverAttachment.class,
                properties = {"attachmentId", "category", "fileUrl", "originalName", "uploadedBy", "createTime"}),
        @AllowProperties(model = com.th4.edge.decoboon.carryover.model.CarryoverProject.class,
                properties = {"itemId", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/attachment/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.carryover.service.attachment.page.Page {

    @User
    private Employee employee;

    @AutoInject @NotNull private Long itemId;

    private final CarryoverAccessContext accessContext;
    private final CarryoverAttachmentRepository attachmentRepository;

    public Page(CarryoverAccessContext accessContext, CarryoverAttachmentRepository attachmentRepository) {
        this.accessContext = accessContext;
        this.attachmentRepository = attachmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarryoverAttachment> call(CarryoverAttachment request) {
        Employee current = accessContext.requireBoss(employee);
        accessContext.requireItem(current, itemId);
        return attachmentRepository.findByItem_ItemIdOrderByAttachmentIdAsc(itemId);
    }
}
