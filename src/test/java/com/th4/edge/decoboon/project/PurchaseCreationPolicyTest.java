package com.th4.edge.decoboon.project;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.configure.DeliveryMode;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 采购创建时企业负责人直接确认，其他岗位进入审批。 */
class PurchaseCreationPolicyTest {

    private MaterialPlanRepository planRepository;
    private ProjectContractRepository contractRepository;
    private PurchaseOrderRepository purchaseRepository;
    private PartnerRepository partnerRepository;
    private StoreRepository storeRepository;
    private ProjectAccessContext accessContext;
    private ModelIdentityContext identityContext;
    private com.th4.edge.decoboon.project.service.purchases.save.support.Save save;

    @BeforeEach
    void setUp() {
        planRepository = mock(MaterialPlanRepository.class);
        contractRepository = mock(ProjectContractRepository.class);
        purchaseRepository = mock(PurchaseOrderRepository.class);
        partnerRepository = mock(PartnerRepository.class);
        storeRepository = mock(StoreRepository.class);
        accessContext = mock(ProjectAccessContext.class);
        identityContext = mock(ModelIdentityContext.class);
        ProjectPermissionContext permissionContext = mock(ProjectPermissionContext.class);
        save = new com.th4.edge.decoboon.project.service.purchases.save.support.Save(
                planRepository, contractRepository, purchaseRepository, partnerRepository,
                storeRepository, accessContext, identityContext, permissionContext,
                mock(com.th4.edge.decoboon.material.repository.MaterialSelectionRepository.class),
                mock(com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository.class));
        when(identityContext.prepare(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Specification<MaterialPlan> visible = (root, query, builder) -> builder.conjunction();
        when(accessContext.visibleChild(any(), eq(MaterialPlan.class), eq("project"))).thenReturn(visible);
    }

    @Test
    void bossCreationShouldBeDirectAndAutoApproved() {
        PurchaseOrder result = create(EmployeeRole.Boss, DeliveryMode.Direct);
        assertThat(result.getStatus()).isEqualTo(PurchaseStatus.Approved);
        assertThat(result.getDeliveryMode()).isEqualTo(DeliveryMode.Direct);
        assertThat(result.getWarehouse()).isNull();
    }

    @Test
    void financeCreationShouldBeDirectAndAutoApproved() {
        PurchaseOrder result = create(EmployeeRole.Finance, DeliveryMode.Direct);
        assertThat(result.getStatus()).isEqualTo(PurchaseStatus.Approved);
        assertThat(result.getApprovedBy()).isNotNull();
    }

    @Test
    void pmCreationShouldWaitForApproval() {
        PurchaseOrder result = create(EmployeeRole.Pm, DeliveryMode.Direct);
        assertThat(result.getStatus()).isEqualTo(PurchaseStatus.Submitted);
        assertThat(result.getApprovedBy()).isNull();
    }

    private PurchaseOrder create(EmployeeRole role, DeliveryMode mode) {
        Company company = new Company();
        company.setComId(10L);
        Employee employee = new Employee();
        employee.setEmpId(20L);
        employee.setRole(role);
        employee.setCompany(company);
        employee.setStore(new Store());
        Project project = new Project();
        project.setProjectId(30L);
        project.setCompany(company);
        MaterialPlan plan = new MaterialPlan();
        plan.setPlanId(40L);
        plan.setProject(project);
        plan.setCatalogItem(new CatalogItem());
        plan.setUnit(Unit.Item);
        ProjectContract contract = new ProjectContract();
        Partner supplier = new Partner();
        supplier.setPartnerId(50L);
        supplier.setCompany(company);
        supplier.setName("测试材料商");

        when(accessContext.requireEmployee(any())).thenReturn(employee);
        when(planRepository.findOne(any(Specification.class))).thenReturn(Optional.of(plan));
        when(contractRepository.findByProject(project)).thenReturn(Optional.of(contract));
        when(partnerRepository.findById(50L)).thenReturn(Optional.of(supplier));
        ReflectionTestUtils.setField(save, "materialPlanId", 40L);
        ReflectionTestUtils.setField(save, "supplierId", 50L);
        ReflectionTestUtils.setField(save, "supplierName", supplier.getName());
        ReflectionTestUtils.setField(save, "quantity", new BigDecimal("10"));
        ReflectionTestUtils.setField(save, "unitPrice", new BigDecimal("20"));
        ReflectionTestUtils.setField(save, "deliveryMode", mode);
        ReflectionTestUtils.setField(save, "employee", employee);
        return save.call(new PurchaseOrder());
    }
}
