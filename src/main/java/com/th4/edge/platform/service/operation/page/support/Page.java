package com.th4.edge.platform.service.operation.page.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.platform.context.PlatformAccessContext;
import com.th4.edge.platform.model.PlatformAction;
import com.th4.edge.platform.model.PlatformDepartment;
import com.th4.edge.platform.model.PlatformOperationLog;
import com.th4.edge.platform.model.PlatformPermission;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.platform.repository.PlatformOperationLogRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 平台操作审计分页实现。 */
@Note("平台操作审计分页")
@Properties(allow = {
        @AllowProperties(model = PlatformOperationLog.class, properties = {
                "logId", "org", "operator", "action", "targetType", "targetKey", "targetComId",
                "targetCompanyTitle", "targetSubId", "summary", "beforeValue", "afterValue", "reason", "createTime"
        }),
        @AllowProperties(model = PlatformUser.class, properties = {"userId", "name", "phone", "role", "department"}),
        @AllowProperties(model = PlatformDepartment.class, properties = {"deptId", "code", "name"})
})
@Service("platform/operation/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.platform.service.operation.page.Page {
    @AutoInject private Long companyId;
    @AutoInject private PlatformAction action;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    private final PlatformAccessContext accessContext;
    private final PlatformOperationLogRepository operationLogRepository;

    public Page(PlatformAccessContext accessContext, PlatformOperationLogRepository operationLogRepository) {
        this.accessContext = accessContext;
        this.operationLogRepository = operationLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PlatformOperationLog> call(@User PlatformUser user) {
        PlatformUser operator = accessContext.requirePermission(user, PlatformPermission.AuditRead);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        return operationLogRepository.pageLogs(operator.getOrg().getOrgId(), companyId, action, PageRequest.of(page, pageSize));
    }
}
