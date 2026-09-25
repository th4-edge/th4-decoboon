package com.th4.edge.decoboon.carryover.service.attachment.delete.support;

import com.th4.edge.decoboon.carryover.configure.CarryoverProjectStatus;
import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.model.CarryoverAttachment;
import com.th4.edge.decoboon.carryover.repository.CarryoverAttachmentRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("删除承接附件")
@Service("carryover/attachment/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.carryover.service.attachment.delete.Delete {

    @User
    private Employee employee;

    @AutoInject @NotNull private Long attachmentId;

    private final CarryoverAccessContext accessContext;
    private final CarryoverAttachmentRepository attachmentRepository;

    public Delete(CarryoverAccessContext accessContext, CarryoverAttachmentRepository attachmentRepository) {
        this.accessContext = accessContext;
        this.attachmentRepository = attachmentRepository;
    }

    @Override
    @Transactional
    public Boolean call(CarryoverAttachment request) {
        Employee current = accessContext.requireBoss(employee);
        CarryoverAttachment attachment = attachmentRepository.findById(attachmentId)
                .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "附件不存在或无权访问")));
        CarryoverProjectStatus status = attachment.getItem().getStatus();
        if (status != CarryoverProjectStatus.Draft && status != CarryoverProjectStatus.Ready
                && status != CarryoverProjectStatus.Confirmed) {
            throw new DefinedException(new DefaultErrorCode("400", "项目已承接，附件只能补充不能直接删除"));
        }
        attachmentRepository.delete(attachment);
        return true;
    }
}
