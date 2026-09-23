package com.th4.edge.decoboon.labor.service.worker.page.support;

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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 工人档案分页实现。 */
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
    public org.springframework.data.domain.Page<Worker> call(Worker request) {
        Employee viewer = privacyContext.requireViewer(employee);
        Long comId = viewer.getCompany().getComId();
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Worker> items = workerRepository.findByCompany_ComIdOrderByWorkerIdDesc(comId).stream()
                .filter(item -> status == null || status.isBlank()
                        || item.getStatus().name().equalsIgnoreCase(status))
                .filter(item -> kw.isBlank()
                        || contains(item.getName(), kw)
                        || contains(item.getPhone(), kw)
                        || contains(item.getTrade(), kw)
                        || contains(item.getIdCardNo(), kw))
                .sorted(Comparator.comparing(Worker::getStatus, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Worker::getWorkerId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(item -> privacyContext.mask(item, viewer))
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }

    private boolean contains(String value, String kw) {
        return value != null && value.toLowerCase().contains(kw);
    }
}
