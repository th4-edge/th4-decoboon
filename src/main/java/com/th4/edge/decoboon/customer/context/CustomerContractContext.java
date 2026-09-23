package com.th4.edge.decoboon.customer.context;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业主合同访问上下文。
 *
 * <p>只允许通过当前微信身份已生效的项目授权访问合同；合同 ID 不能作为绕过
 * CustomerAccess 的依据。</p>
 */
@Component
public class CustomerContractContext {

    private final CustomerAccessContext accessContext;
    private final ProjectContractRepository contractRepository;

    public CustomerContractContext(CustomerAccessContext accessContext,
                                   ProjectContractRepository contractRepository) {
        this.accessContext = accessContext;
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public ProjectContract requireContract(Credentials credential, Long projectId, Long contractId) {
        if (projectId != null) {
            Project project = accessContext.requireProject(credential, projectId);
            return contractRepository.findByProject(project)
                    .orElseThrow(() -> new DefinedException(
                            new DefaultErrorCode("404", "当前项目尚未生成合同基线")));
        }
        if (contractId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择项目或合同"));
        }

        ProjectContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "合同不存在或无权访问")));
        Project project = contract.getProject();
        if (project == null || project.getProjectId() == null) {
            throw new DefinedException(new DefaultErrorCode("404", "合同不存在或无权访问"));
        }
        accessContext.requireProject(credential, project.getProjectId());
        return contract;
    }
}
