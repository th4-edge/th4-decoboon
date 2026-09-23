package com.th4.edge.decoboon.labor.service.worker.save.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
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
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** 新增工人档案。 */
@Note("新增工人档案")
@Properties(allow = @AllowProperties(model = Worker.class,
        properties = {"workerId", "name", "idCardNo", "trade", "phone", "bankName", "bankAccount",
                "status", "remark", "createTime"}))
@Service("labor/worker/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.labor.service.worker.save.Save {

    @User
    private Employee employee;
    @AutoInject @NotEmpty @Length(max = 64)
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
    @AutoInject @Length(max = 500)
    private String remark;

    private final WorkerRepository workerRepository;
    private final WorkerPrivacyContext privacyContext;
    private final ModelIdentityContext modelIdentityContext;

    public Save(WorkerRepository workerRepository, WorkerPrivacyContext privacyContext,
                ModelIdentityContext modelIdentityContext) {
        this.workerRepository = workerRepository;
        this.privacyContext = privacyContext;
        this.modelIdentityContext = modelIdentityContext;
    }

    @Override
    @Transactional
    public Worker call(Worker request) {
        Employee current = privacyContext.requireMaintainer(employee);
        Long comId = current.getCompany().getComId();
        String normalizedIdCard = blankToNull(idCardNo);
        requireUniqueIdCard(comId, normalizedIdCard, null);

        request.setCompany(current.getCompany());
        request.setName(name.trim());
        request.setIdCardNo(normalizedIdCard);
        request.setTrade(blankToNull(trade));
        request.setPhone(blankToNull(phone));
        request.setBankName(blankToNull(bankName));
        request.setBankAccount(blankToNull(bankAccount));
        request.setStatus(WorkerStatus.Active);
        request.setRemark(blankToNull(remark));
        Worker saved = workerRepository.save(modelIdentityContext.prepare(request));
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
