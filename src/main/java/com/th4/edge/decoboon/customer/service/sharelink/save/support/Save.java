package com.th4.edge.decoboon.customer.service.sharelink.save.support;

import com.th4.edge.decoboon.customer.configure.ShareScope;
import com.th4.edge.decoboon.customer.configure.ShareStatus;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.model.ProjectShareLink;
import com.th4.edge.decoboon.customer.repository.ProjectShareLinkRepository;
import com.th4.edge.decoboon.project.model.Project;
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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 业主生成亲友观看链接。
 *
 * 免登录通道的安全前提：令牌一次性生成、带时效、可撤销、不缓存。
 * 每项目并发有效链接上限 5 条，超出要求先撤销旧的，避免令牌无限累积。
 */
@Note("生成亲友观看链接")
@Properties(allow = {
        @AllowProperties(model = ProjectShareLink.class,
                properties = {"shareId", "token", "scope", "showAmount", "status", "expireAt", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"})
})
@Service("customer/sharelink/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.customer.service.sharelink.save.Save {

    private static final int MAX_ACTIVE_LINKS = 5;
    private static final int DEFAULT_EXPIRE_DAYS = 30;

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    @NotNull
    private Long projectId;

    /** 缺省按家人范围；Friend 强制不展示金额。 */
    @AutoInject
    private ShareScope scope;

    @AutoInject
    private Boolean showAmount;

    @AutoInject
    private Integer expireDays;

    private final CustomerAccessContext accessContext;
    private final ProjectShareLinkRepository repository;

    public Save(CustomerAccessContext accessContext, ProjectShareLinkRepository repository) {
        this.accessContext = accessContext;
        this.repository = repository;
    }

    @Override
    @Transactional
    public ProjectShareLink call(ProjectShareLink request) {
        Credentials credential = accessContext.requireCredential(principal);
        Project project = accessContext.requireProject(credential, projectId);
        Long comId = project.getCompany().getComId();

        long active = repository.countByProject_ProjectIdAndCompany_ComIdAndStatus(
                project.getProjectId(), comId, ShareStatus.Active);
        if (active >= MAX_ACTIVE_LINKS) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "这个项目已有 " + MAX_ACTIVE_LINKS + " 条有效观看链接，请先撤销不再需要的"));
        }

        ShareScope targetScope = scope == null ? ShareScope.Family : scope;
        int days = expireDays == null || expireDays <= 0 ? DEFAULT_EXPIRE_DAYS : Math.min(expireDays, 90);

        ProjectShareLink link = new ProjectShareLink();
        link.setCompany(project.getCompany());
        link.setProject(project);
        link.setCreatedBy(null);
        link.setToken(UUID.randomUUID().toString().replace("-", ""));
        link.setScope(targetScope);
        // Friend 范围恒不展示金额，避免"好友能看钱"这种误配
        link.setShowAmount(targetScope == ShareScope.Friend ? Boolean.FALSE : Boolean.TRUE.equals(showAmount));
        link.setStatus(ShareStatus.Active);
        link.setExpireAt(LocalDateTime.now().plusDays(days));
        return repository.save(link);
    }
}
