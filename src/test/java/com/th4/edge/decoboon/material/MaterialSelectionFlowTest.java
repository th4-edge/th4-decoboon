package com.th4.edge.decoboon.material;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.material.context.MaterialSelectionContext;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 主材选材业务流程单元测试：版本保留、确认幂等、上下文门禁与金额不变。 */
@ExtendWith(MockitoExtension.class)
class MaterialSelectionFlowTest {

    @Mock private MaterialSelectionContext context;
    @Mock private MaterialSelectionRepository repository;
    @Mock private Employee employee;

    private com.th4.edge.decoboon.material.service.selections.save.support.Save save;
    private com.th4.edge.decoboon.material.service.selections.confirm.support.Confirm confirm;
    /** 模拟持久化上下文返回的同一条记录。 */
    private MaterialSelection persisted;

    @BeforeEach
    void setUp() {
        save = new com.th4.edge.decoboon.material.service.selections.save.support.Save(context, repository);
        confirm = new com.th4.edge.decoboon.material.service.selections.confirm.support.Confirm(context, repository, mock(com.th4.edge.decoboon.project.repository.MaterialPlanRepository.class), new com.th4.edge.decoboon.project.context.MaterialPlanContext());
        persisted = null;
    }

    @Test
    void firstSaveCreatesVersionOneDraftWithoutTouchingContractAmount() throws Exception {
        Property property = property();
        ProjectContract contract = contract(property);
        stubMaintain(property, contract);
        when(repository.findFirstByCompany_ComIdAndProperty_PropertyIdAndContract_ContractIdOrderByVersionDesc(1L, 7L, 9L))
                .thenReturn(Optional.empty());
        stubPersist(101L);

        inject(save, "propertyId", 7L);
        inject(save, "contractId", 9L);
        inject(save, "content", "厨房墙砖：浅灰哑光 300x600；卫生间地砖：防滑米白");
        inject(save, "employee", employee);

        MaterialSelection result = save.call(new MaterialSelection());

        assertEquals(1, result.getVersion());
        assertEquals(SelectionStatus.Draft, result.getStatus());
        assertEquals(0, new BigDecimal("120000.00").compareTo(contract.getSignedAmount()),
                "保存选材文本不得改动合同签约金额");
    }

    @Test
    void ongoingDraftIsSavedInPlaceInsteadOfStackingVersions() throws Exception {
        Property property = property();
        ProjectContract contract = contract(property);
        stubMaintain(property, contract);
        MaterialSelection draft = new MaterialSelection();
        draft.setSelectionId(50L);
        draft.setVersion(1);
        draft.setStatus(SelectionStatus.Draft);
        when(repository.findFirstByCompany_ComIdAndProperty_PropertyIdAndContract_ContractIdOrderByVersionDesc(1L, 7L, 9L))
                .thenReturn(Optional.of(draft));
        stubPersist(50L);

        inject(save, "propertyId", 7L);
        inject(save, "contractId", 9L);
        inject(save, "status", SelectionStatus.PendingConfirm);
        inject(save, "content", "客厅地板：原木色多层实木");
        inject(save, "employee", employee);

        MaterialSelection result = save.call(new MaterialSelection());

        assertEquals(1, result.getVersion(), "未确认版本原地保存，不产生新版本");
        assertEquals(SelectionStatus.PendingConfirm, result.getStatus());
    }

    @Test
    void confirmedVersionIsKeptAndChangeFormsNewVersion() throws Exception {
        Property property = property();
        ProjectContract contract = contract(property);
        stubMaintain(property, contract);
        MaterialSelection confirmed = new MaterialSelection();
        confirmed.setSelectionId(80L);
        confirmed.setVersion(3);
        confirmed.setStatus(SelectionStatus.Confirmed);
        when(repository.findFirstByCompany_ComIdAndProperty_PropertyIdAndContract_ContractIdOrderByVersionDesc(1L, 7L, 9L))
                .thenReturn(Optional.of(confirmed));
        stubPersist(81L);

        inject(save, "propertyId", 7L);
        inject(save, "contractId", 9L);
        inject(save, "content", "主卧地板改为深胡桃色");
        inject(save, "employee", employee);

        MaterialSelection result = save.call(new MaterialSelection());

        assertEquals(4, result.getVersion(), "已确认版本保留，变更形成新版本");
        assertEquals(3, confirmed.getVersion(), "历史已确认版本不被覆盖");
        assertEquals(SelectionStatus.Confirmed, confirmed.getStatus());
    }

    @Test
    void contractLineSavePersistsJsonAndUsesItemVersionScope() throws Exception {
        Property property = property();
        Project project = new Project();
        project.setProjectId(20L);
        ProjectContract contract = contract(property);
        contract.setProject(project);
        ContractLine line = new ContractLine();
        line.setLineId(30L);
        line.setCompany(property.getCompany());
        line.setType(ItemType.Material);
        line.setName("厨房墙砖");
        line.setContract(contract);
        when(context.requireEmployee(employee)).thenReturn(employee);
        when(context.requireProperty(7L, employee)).thenReturn(property);
        when(context.requireProject(20L, property, employee)).thenReturn(project);
        when(context.requireContractLine(30L, project, employee)).thenReturn(line);
        when(repository.findFirstByCompany_ComIdAndSourceContractLine_LineIdOrderByVersionDesc(1L, 30L))
                .thenReturn(Optional.empty());
        stubPersist(90L);

        inject(save, "propertyId", 7L);
        inject(save, "projectId", 20L);
        inject(save, "contractLineId", 30L);
        inject(save, "content", "颜色/花色：黄绿色；规格/型号：800×800");
        inject(save, "selectionJson", "{\"color\":\"黄绿色\",\"spec\":\"800×800\"}");
        inject(save, "employee", employee);

        MaterialSelection result = save.call(new MaterialSelection());

        assertEquals(line, result.getSourceContractLine());
        assertEquals("{\"color\":\"黄绿色\",\"spec\":\"800×800\"}", result.getSelectionJson());
        verify(repository).findFirstByCompany_ComIdAndSourceContractLine_LineIdOrderByVersionDesc(1L, 30L);
    }

    @Test
    void confirmIsIdempotentAndRecordsClientName() throws Exception {
        MaterialSelection pending = new MaterialSelection();
        pending.setSelectionId(60L);
        pending.setStatus(SelectionStatus.PendingConfirm);
        pending.setContract(new ProjectContract());
        when(context.requireEmployee(employee)).thenReturn(employee);
        when(context.require(60L, employee)).thenReturn(pending);
        stubPersist(60L);
        when(repository.findBySelectionId(60L)).thenAnswer(invocation -> Optional.of(pending));

        inject(confirm, "selectionId", 60L);
        inject(confirm, "confirmerName", "王先生");
        inject(confirm, "employee", employee);

        MaterialSelection result = confirm.call(new MaterialSelection());
        assertEquals(SelectionStatus.Confirmed, result.getStatus());
        assertEquals("王先生", result.getConfirmerName());
        assertNotNull(result.getConfirmedAt());

        MaterialSelection again = confirm.call(new MaterialSelection());
        assertEquals(SelectionStatus.Confirmed, again.getStatus());
        verify(repository, times(1)).saveAndFlush(any(MaterialSelection.class));
    }

    @Test
    void confirmWithoutContractOrProjectIsRejected() throws Exception {
        MaterialSelection draft = new MaterialSelection();
        draft.setSelectionId(61L);
        draft.setStatus(SelectionStatus.Draft);
        when(context.requireEmployee(employee)).thenReturn(employee);
        when(context.require(61L, employee)).thenReturn(draft);

        inject(confirm, "selectionId", 61L);
        inject(confirm, "confirmerName", "李女士");
        inject(confirm, "employee", employee);

        assertThrows(DefinedException.class, () -> confirm.call(new MaterialSelection()));
        verify(repository, never()).saveAndFlush(any(MaterialSelection.class));
    }

    @Test
    void blankContentIsRejectedBeforeAnyWrite() throws Exception {
        inject(save, "content", "   ");
        assertThrows(DefinedException.class, () -> save.call(new MaterialSelection()));
        verify(repository, never()).saveAndFlush(any(MaterialSelection.class));
    }

    private void stubMaintain(Property property, ProjectContract contract) throws Exception {
        when(context.requireEmployee(employee)).thenReturn(employee);
        when(context.requireProperty(7L, employee)).thenReturn(property);
        when(context.requireContract(9L, property, employee)).thenReturn(contract);
        when(context.requireProject(null, property, employee)).thenReturn(null);
    }

    private void stubPersist(Long selectionId) {
        when(repository.saveAndFlush(any(MaterialSelection.class))).thenAnswer(invocation -> {
            MaterialSelection saved = invocation.getArgument(0);
            saved.setSelectionId(selectionId);
            persisted = saved;
            return saved;
        });
        when(repository.findBySelectionId(anyLong())).thenAnswer(invocation -> Optional.ofNullable(persisted));
    }

    private Property property() {
        Property property = new Property();
        property.setPropertyId(7L);
        Company company = new Company();
        company.setComId(1L);
        property.setCompany(company);
        return property;
    }

    private ProjectContract contract(Property property) {
        ProjectContract contract = new ProjectContract();
        contract.setContractId(9L);
        contract.setProperty(property);
        contract.setSignedAmount(new BigDecimal("120000.00"));
        return contract;
    }

    private void inject(Object target, String field, Object value) throws Exception {
        Field declared = target.getClass().getDeclaredField(field);
        declared.setAccessible(true);
        declared.set(target, value);
    }
}
