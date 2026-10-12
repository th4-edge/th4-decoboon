package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.service.contractchange.batchsave.support.BatchSave;
import com.th4.edge.decoboon.project.service.contractchange.preview.support.Preview;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 增减项预览 Provider 与 batch-save 共用草稿准备，且不写库。 */
class PreviewProviderTest {

    @Test
    void previewShouldDelegateToSharedDraftPreparationWithoutSaving() {
        ProjectContractRepository contractRepository = mock(ProjectContractRepository.class);
        ProjectAccessContext accessContext = mock(ProjectAccessContext.class);
        BatchSave batchSave = mock(BatchSave.class);
        Preview preview = new Preview(contractRepository, accessContext, batchSave);

        Employee employee = new Employee();
        employee.setEmpId(9L);
        Project project = new Project();
        project.setProjectId(7L);
        ProjectContract contract = new ProjectContract();
        contract.setContractId(3L);
        contract.setProject(project);
        ContractChangeItem item = new ContractChangeItem();
        item.setChangeType(ContractChangeType.Add);
        ContractChange draft = new ContractChange();
        draft.setAmount(new java.math.BigDecimal("100.00"));
        draft.setItems(List.of(item));

        when(accessContext.requireEmployee(employee)).thenReturn(employee);
        when(accessContext.visibleContract(employee)).thenReturn(Specification.where(null));
        when(contractRepository.findOne(any(Specification.class))).thenReturn(Optional.of(contract));
        when(accessContext.requireProject(7L, employee)).thenReturn(project);
        when(batchSave.prepareDraft(contract, project, ContractChangeType.Add, List.of(item))).thenReturn(draft);

        ReflectionTestUtils.setField(preview, "contractId", 3L);
        ReflectionTestUtils.setField(preview, "type", ContractChangeType.Add);
        ReflectionTestUtils.setField(preview, "items", List.of(item));
        ReflectionTestUtils.setField(preview, "employee", employee);

        ContractChange result = preview.call(new ContractChange());

        assertThat(result).isSameAs(draft);
        assertThat(result.getAmount()).isEqualByComparingTo("100.00");
    }
}
