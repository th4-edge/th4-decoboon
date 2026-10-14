package com.th4.edge.decoboon.customer.service.sharelink.update.support;

import com.th4.edge.decoboon.customer.configure.ShareStatus;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.model.ProjectShareLink;
import com.th4.edge.decoboon.customer.repository.ProjectShareLinkRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 业主撤销亲友观看链接：撤销后立即失效（读取端不做缓存）。 */
@Note("撤销亲友观看链接")
@Properties(allow = @AllowProperties(model = ProjectShareLink.class,
        properties = {"shareId", "status", "expireAt"}))
@Service("customer/sharelink/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.customer.service.sharelink.update.Update {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    @NotNull
    private Long shareId;

    private final CustomerAccessContext accessContext;
    private final ProjectShareLinkRepository repository;

    public Update(CustomerAccessContext accessContext, ProjectShareLinkRepository repository) {
        this.accessContext = accessContext;
        this.repository = repository;
    }

    @Override
    @Transactional
    public ProjectShareLink call(ProjectShareLink request) {
        Credentials credential = accessContext.requireCredential(principal);
        ProjectShareLink link = repository.findById(shareId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "观看链接不存在")));
        // 复用业主-项目授权校验：只能撤销自己有权访问的项目链接
        accessContext.requireProject(credential, link.getProject().getProjectId());
        link.setStatus(ShareStatus.Revoked);
        return repository.save(link);
    }
}
