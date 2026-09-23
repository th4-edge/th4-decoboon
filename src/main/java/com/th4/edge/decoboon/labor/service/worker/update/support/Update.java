package com.th4.edge.decoboon.labor.service.worker.update.support;

import com.th4.edge.decoboon.labor.configure.WorkerStatus;
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
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** 编辑或停用工人档案。 */
@Note("编辑工人档案")
@Properties(allow = @AllowProperties(model = Worker.class,
        properties = {"workerId", "name", "idCardNo", "trade", "phone", "bankName", "bankAccount",
                "status", "remark", "createTime"}))
@Service("labor/worker/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.labor.service.worker.update.Update {

    @User
    private Employee employee;
    @AutoInject @NotNull
    private Long workerId;
    @AutoInject @Length(max = 64)
    private String name;
    @AutoInject @Length(max = 32)
    private String idCardNo;
    @AutoInject @Length(max = 32)
    private String trade;
    @AutoInject @Length(max = 20)
    private String phone;
    @AutoInject @Length(max = 64)
    private String bankName;
    @AutoInject @Length(max = 64)
    private String bankAccount;
    @AutoInject
    private WorkerStatus status;
    @AutoInject @Length(max = 500)
    private String remark;

    private final WorkerRepository workerRepository;
    private final WorkerPrivacyContext privacyContext;

    public Update(WorkerRepository workerRepository, WorkerPrivacyContext privacyContext) {
        this.workerRepository = workerRepository;
        this.privacyContext = privacyContext;
    }

    @Override
    @Transactional
    public Worker call(Worker request) {
        Employee current = privacyContext.requireMaintainer(employee);
        Long comId = current.getCompany().getComId();
        Worker target = workerRepository.findByWorkerIdAndCompany_ComId(workerId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "工人档案不存在或无权访问")));

        String normalizedIdCard = blankToNull(idCardNo);
        requireUniqueIdCard(comId, normalizedIdCard, target.getWorkerId());

        if (name != null && !name.isBlank()) target.setName(name.trim());
        target.setIdCardNo(normalizedIdCard);
        target.setTrade(blankToNull(trade));
        target.setPhone(blankToNull(phone));
        target.setBankName(blankToNull(bankName));
        target.setBankAccount(blankToNull(bankAccount));
        target.setRemark(blankToNull(remark));
        if (status != null) target.setStatus(status);
        Worker saved = workerRepository.save(target);
        return privacyContext.mask(saved, current);
    }

    private void requireUniqueIdCard(Long comId, String idCardNo, Long selfId) {
        if (idCardNo == null) return;
        Optional<Worker> existed = workerRepository.findFirstByCompany_ComIdAndIdCardNo(comId, idCardNo);
        if (existed.isPresent() && !existed.get().getWorkerId().equals(selfId)) {
            throw new DefinedException(new DefaultErrorCode("409", "该工人已建档，请直接在工人档案中编辑"));
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
