package com.th4.edge.decoboon.labor.service.worker.info.support;

import com.th4.edge.decoboon.labor.context.WorkerPrivacyContext;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.decoboon.labor.repository.WorkerRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 工人档案详情实现：按主键 + 企业一次加载，避免列表分页导致编辑页取不到人。 */
@Note("工人档案详情")
@Properties(allow = @AllowProperties(model = Worker.class,
        properties = {"workerId", "name", "idCardNo", "trade", "phone", "bankName", "bankAccount",
                "status", "remark", "createTime"}))
@Service("labor/worker/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.labor.service.worker.info.Info {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long workerId;

    private final WorkerRepository workerRepository;
    private final WorkerPrivacyContext privacyContext;

    public Info(WorkerRepository workerRepository, WorkerPrivacyContext privacyContext) {
        this.workerRepository = workerRepository;
        this.privacyContext = privacyContext;
    }

    @Override
    public Worker call(Worker request) {
        Employee viewer = privacyContext.requireViewer(employee);
        Worker worker = workerRepository
                .findByWorkerIdAndCompany_ComId(workerId, viewer.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "工人档案不存在或无权访问")));
        return privacyContext.mask(worker, viewer);
    }
}
