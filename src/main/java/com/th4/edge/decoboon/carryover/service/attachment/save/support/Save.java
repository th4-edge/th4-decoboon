package com.th4.edge.decoboon.carryover.service.attachment.save.support;

import com.th4.edge.decoboon.carryover.configure.CarryoverAttachmentCategory;
import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.model.CarryoverAttachment;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.decoboon.carryover.repository.CarryoverAttachmentRepository;
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

@Note("保存承接附件")
@Properties(allow = {
        @AllowProperties(model = CarryoverAttachment.class,
                properties = {"attachmentId", "category", "fileUrl", "originalName", "uploadedBy", "createTime"}),
        @AllowProperties(model = com.th4.edge.decoboon.carryover.model.CarryoverProject.class,
                properties = {"itemId", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/attachment/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.carryover.service.attachment.save.Save {

    @User
    private Employee employee;

    @AutoInject @NotNull private Long itemId;
    @AutoInject @NotNull private CarryoverAttachmentCategory category;
    @AutoInject @NotEmpty @Length(max = 512) private String fileUrl;
    @AutoInject @Length(max = 256) private String originalName;

    private final CarryoverAccessContext accessContext;
    private final CarryoverAttachmentRepository attachmentRepository;

    public Save(CarryoverAccessContext accessContext, CarryoverAttachmentRepository attachmentRepository) {
        this.accessContext = accessContext;
        this.attachmentRepository = attachmentRepository;
    }

    @Override
    @Transactional
    public CarryoverAttachment call(CarryoverAttachment request) {
        Employee current = accessContext.requireBoss(employee);
        CarryoverProject item = accessContext.requireItem(current, itemId);
        CarryoverAttachment attachment = new CarryoverAttachment();
        attachment.setCompany(current.getCompany());
        attachment.setItem(item);
        attachment.setCategory(category);
        attachment.setFileUrl(fileUrl.trim());
        attachment.setOriginalName(originalName);
        attachment.setUploadedBy(current);
        return attachmentRepository.save(attachment);
    }
}
