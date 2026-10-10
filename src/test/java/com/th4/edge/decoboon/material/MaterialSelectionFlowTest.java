package com.th4.edge.decoboon.material;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.material.context.MaterialSelectionContext;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.context.MaterialPlanContext;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 主材选材现场代录流程单元测试（DEC-056）：保存即已选材、交付财务下单、上下文门禁与金额不变。
 */
@ExtendWith(MockitoExtension.class)
class MaterialSelectionFlowTest {

    @Mock private MaterialSelectionContext context;
    @Mock private MaterialSelectionRepository repository;
    @Mock private MaterialPlanRepository planRepository;
    @Mock private Employee employee;

    private com.th4.edge.decoboon.material.service.selections.save.support.Save save;
    /** 模拟持久化上下文返回的同一条记录。 */
    private MaterialSelection persisted;

    @BeforeEach
    void setUp() {
        save = new com.th4.edge.decoboon.material.service.selections.save.support.Save(
                context, repository, planRepository, mock(MaterialPlanContext.class));
        persisted = null;
    }

    @Test
    void firstSaveMarksSelectedWithoutTouchingContractAmount() throws Exception {
        Property property = property();
        ProjectContract contract = contract(property);
        stubMaintain(property, contract);
        when(context.plansOf(null, null)).thenReturn(List.of());
        when(repository.findFirstByCompany_ComIdAndProperty_PropertyIdAndContract_ContractIdOrderByVersionDesc(1L, 7L, 9L))
                .thenReturn(Optional.empty());
        stubPersist(101L);

        inject(save, "propertyId", 7L);
        inject(save, "contractId", 9L);
        inject(save, "content", "厨房墙砖：浅灰哑光 300x600；卫生间地砖：防滑米白");
        inject(save, "employee", employee);

        MaterialSelection result = save.call(new MaterialSelection());

        assertEquals(1, result.getVersion());
        assertEquals(SelectionStatus.Selected, result.getStatus());
        assertEquals(0, new BigDecimal("120000.00").compareTo(contract.getSignedAmount()),
                "保存选材文本不得改动合同签约金额");
    }

    @Test
    void ongoingSelectionCreatesNewVersionInsteadOfOverwritingHistory() throws Exception {
        Property property = property();
        ProjectContract contract = contract(property);
        stubMaintain(property, contract);
        MaterialSelection ongoing = new MaterialSelection();
        ongoing.setSelectionId(50L);
        ongoing.setVersion(1);
        ongoing.setStatus(SelectionStatus.Selected);
        when(context.plansOf(null, null)).thenReturn(List.of());
        when(repository.findFirstByCompany_ComIdAndProperty_PropertyIdAndContract_ContractIdOrderByVersionDesc(1L, 7L, 9L))
                .thenReturn(Optional.of(ongoing));
        stubPersist(51L);

        inject(save, "propertyId", 7L);
        inject(save, "contractId", 9L);
        inject(save, "content", "客厅地板：原木色多层实木");
        inject(save, "employee", employee);

        MaterialSelection result = save.call(new MaterialSelection());

        assertEquals(2, result.getVersion(), "同一分项再次保存必须追加新版本");
        assertEquals(SelectionStatus.Selected, result.getStatus());
    }

    @Test
    void saveWithDeliverPromotesPlannedPlanToReadyToOrder() throws Exception {
        Property property = property();
        ProjectContract contract = contract(property);
        stubMaintain(property, contract);
        MaterialPlan plan = new MaterialPlan();
        plan.setPlanId(1L);
        plan.setStatus(MaterialPlanStatus.Planned);
        when(context.plansOf(null, null)).thenReturn(List.of(plan));
        when(context.selectionLocked(plan)).thenReturn(false);
        when(repository.findFirstByCompany_ComIdAndProperty_PropertyIdAndContract_ContractIdOrderByVersionDesc(1L, 7L, 9L))
                .thenReturn(Optional.empty());
        stubPersist(70L);

        inject(save, "propertyId", 7L);
        inject(save, "contractId", 9L);
        inject(save, "content", "主卧地板：深胡桃色");
        inject(save, "deliver", Boolean.TRUE);
        inject(save, "employee", employee);

        MaterialSelection result = save.call(new MaterialSelection());

        assertEquals(SelectionStatus.Selected, result.getStatus());
        assertEquals(MaterialPlanStatus.ReadyToOrder, plan.getStatus(), "交付财务下单后计划行进入可下单");
        assertEquals(result, plan.getSelection());
        verify(planRepository).save(plan);
    }

    @Test
    void saveWithoutDeliverKeepsPlanPlanned() throws Exception {
        Property property = property();
        ProjectContract contract = contract(property);
        stubMaintain(property, contract);
        MaterialPlan plan = new MaterialPlan();
        plan.setPlanId(2L);
        plan.setStatus(MaterialPlanStatus.Planned);
        when(context.plansOf(null, null)).thenReturn(List.of(plan));
        when(context.selectionLocked(plan)).thenReturn(false);
        when(repository.findFirstByCompany_ComIdAndProperty_PropertyIdAndContract_ContractIdOrderByVersionDesc(1L, 7L, 9L))
                .thenReturn(Optional.empty());
        stubPersist(71L);

        inject(save, "propertyId", 7L);
        inject(save, "contractId", 9L);
        inject(save, "content", "次卧地板：浅橡木色");
        inject(save, "employee", employee);

        save.call(new MaterialSelection());

        assertEquals(MaterialPlanStatus.Planned, plan.getStatus(), "仅保存不交付时不推进计划行");
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
        when(context.plansOf(project, line)).thenReturn(List.of());
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
    void blankContentIsRejectedBeforeAnyWrite() throws Exception {
        inject(save, "content", "   ");
        assertThrows(DefinedException.class, () -> save.call(new MaterialSelection()));
        verify(repository, never()).saveAndFlush(any(MaterialSelection.class));
    }

    @Test
    void lockedPlanRejectsSelectionChange() throws Exception {
        Property property = property();
        ProjectContract contract = contract(property);
        stubMaintain(property, contract);
        MaterialPlan ordered = new MaterialPlan();
        ordered.setPlanId(9L);
        ordered.setStatus(MaterialPlanStatus.Ordered);
        when(context.plansOf(null, null)).thenReturn(List.of(ordered));
        org.mockito.Mockito.doThrow(new DefinedException(
                        new com.th4.edge.framework.exception.DefaultErrorCode("400", "该材料已进入采购，不能再修改选材")))
                .when(context).requireSelectionEditable(any());

        inject(save, "propertyId", 7L);
        inject(save, "contractId", 9L);
        inject(save, "content", "试图改已下单材料");
        inject(save, "employee", employee);

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
