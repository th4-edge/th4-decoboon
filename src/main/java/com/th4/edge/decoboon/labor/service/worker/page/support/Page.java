package com.th4.edge.decoboon.labor.service.worker.page.support;

import com.th4.edge.decoboon.labor.configure.WorkerStatus;
import com.th4.edge.decoboon.labor.context.WorkerPrivacyContext;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.decoboon.labor.repository.WorkerRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/** 工人档案分页实现：服务端搜索、状态过滤和数据库分页。 */
@Note("工人档案分页")
@Properties(allow = @AllowProperties(model = Worker.class,
        properties = {"workerId", "name", "idCardNo", "trade", "phone", "bankName", "bankAccount",
                "status", "remark", "createTime"}))
@Service("labor/worker/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.labor.service.worker.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private String keyword;
    @AutoInject
    private String status;

    private final WorkerRepository workerRepository;
    private final WorkerPrivacyContext privacyContext;

    public Page(WorkerRepository workerRepository, WorkerPrivacyContext privacyContext) {
        this.workerRepository = workerRepository;
        this.privacyContext = privacyContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Worker> call(Worker request) {
        Employee viewer = privacyContext.requireViewer(employee);
        Long comId = viewer.getCompany().getComId();
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        String keywordPattern = kw.isBlank() ? null : "%" + kw + "%";
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 50);
        return workerRepository.findPickerPage(
                        comId,
                        statusFilter(status),
                        keywordPattern,
                        PageRequest.of(page, pageSize))
                .map(item -> privacyContext.mask(item, viewer));
    }

    private List<WorkerStatus> statusFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim())) {
            return Arrays.asList(WorkerStatus.values());
        }
        try {
            return List.of(WorkerStatus.valueOf(value.trim()));
        } catch (IllegalArgumentException ignored) {
            return List.of();
        }
    }
}
