package com.th4.edge.decoboon.project.context;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.PartnerCredit;
import com.th4.edge.decoboon.finance.repository.PartnerCreditRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.MaterialReturnStatus;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.MaterialReturn;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.MaterialReturnRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 材料扣减与退货冲减单元测试（DEC-MP-02）。 */
class MaterialDeductionContextTest {

    private MaterialReturnRepository returnRepository;
    private PurchaseOrderRepository purchaseRepository;
    private MaterialPlanRepository materialPlanRepository;
    private PayableRepository payableRepository;
    private MaterialDeductionContext context;
    private ModelIdentityContext identityContext;
    private PurchaseOrderItemRepository purchaseItemRepository;
    private PartnerCreditRepository partnerCreditRepository;
    private Company company;
    private Employee operator;
    private Project project;
    private MaterialPlan plan;

    @BeforeEach
    void setUp() {
        returnRepository = mock(MaterialReturnRepository.class);
        purchaseRepository = mock(PurchaseOrderRepository.class);
        materialPlanRepository = mock(MaterialPlanRepository.class);
        payableRepository = mock(PayableRepository.class);
        when(returnRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(materialPlanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(payableRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        identityContext = mock(ModelIdentityContext.class);
        when(identityContext.prepare(any())).thenAnswer(invocation -> invocation.getArgument(0));
        purchaseItemRepository = mock(PurchaseOrderItemRepository.class);
        partnerCreditRepository = mock(PartnerCreditRepository.class);
        when(purchaseItemRepository.findByMaterialPlanOrderByPurchaseItemIdAsc(any())).thenReturn(List.of());
        when(purchaseItemRepository.findByPurchaseOrderOrderByPurchaseItemIdAsc(any())).thenReturn(List.of());
        when(partnerCreditRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var planStatusContext = new MaterialPlanStatusContext(purchaseRepository, purchaseItemRepository);
        context = new MaterialDeductionContext(returnRepository, purchaseRepository, purchaseItemRepository,
                materialPlanRepository, payableRepository, partnerCreditRepository, planStatusContext,
                new ProjectPermissionContext(), identityContext,
                mock(com.th4.edge.decoboon.supply.context.InventoryContext.class),
                mock(com.th4.edge.decoboon.supply.repository.InventoryStockRepository.class));

        company = new Company();
        company.setComId(1L);
        operator = new Employee();
        operator.setEmpId(2L);
        operator.setCompany(company);
        project = new Project();
        project.setProjectId(3L);
        project.setCompany(company);

        plan = new MaterialPlan();
        plan.setPlanId(4L);
        plan.setCompany(company);
        plan.setProject(project);
        plan.setNetQuantity(new BigDecimal("100"));
        plan.setLossQuantity(new BigDecimal("8"));
        plan.setQuantity(new BigDecimal("108"));
        plan.setCost(new BigDecimal("4000"));
        plan.setStatus(MaterialPlanStatus.Ordered);
    }

    @Test
    void shouldCutLossThenNetWhenNotOrdered() {
        when(purchaseRepository.findByMaterialPlanOrderByPurchaseIdAsc(plan)).thenReturn(List.of());

        context.reduce(plan, new BigDecimal("10"), "减项", operator);

        // 先冲损耗 8，再冲净用量 2
        assertThat(plan.getLossQuantity()).isEqualByComparingTo("0");
        assertThat(plan.getNetQuantity()).isEqualByComparingTo("98");
        assertThat(plan.getQuantity()).isEqualByComparingTo("98");
        assertThat(plan.getStatus()).isEqualTo(MaterialPlanStatus.ReadyToOrder);
    }

    @Test
    void shouldCancelPlanWhenDeductAll() {
        when(purchaseRepository.findByMaterialPlanOrderByPurchaseIdAsc(plan)).thenReturn(List.of());

        context.reduce(plan, new BigDecimal("200"), "全部减项", operator);

        assertThat(plan.getQuantity()).isEqualByComparingTo("0");
        assertThat(plan.getStatus()).isEqualTo(MaterialPlanStatus.Cancelled);
    }

    @Test
    void shouldCreateReturnAndOffsetPayableWhenAlreadyReceived() {
        PurchaseOrder purchase = receivedPurchase("40", "40", "100");
        when(purchaseRepository.findByMaterialPlanOrderByPurchaseIdAsc(plan)).thenReturn(List.of(purchase));
        when(purchaseRepository.findByMaterialPlanOrderByPurchaseIdAsc(plan)).thenReturn(List.of(purchase));
        Payable payable = payable(new BigDecimal("4000"), new BigDecimal("0"));
        when(payableRepository.findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(1L, 10L))
                .thenReturn(Optional.of(payable));

        List<MaterialReturn> returns = context.reduce(plan, new BigDecimal("20"), "减项", operator);

        assertThat(returns).hasSize(1);
        MaterialReturn entity = returns.get(0);
        assertThat(entity.getQuantity()).isEqualByComparingTo("20");
        assertThat(entity.getAmount()).isEqualByComparingTo("2000");
        assertThat(entity.getStatus()).isEqualTo(MaterialReturnStatus.Confirmed);
        assertThat(purchase.getReceivedQuantity()).isEqualByComparingTo("20");
        assertThat(payable.getAmount()).isEqualByComparingTo("2000");
        assertThat(plan.getCost()).isEqualByComparingTo("2000");
    }

    @Test
    void shouldOffsetOtherPayablesThenRecordPartnerCreditWhenFullyPaid() {
        PurchaseOrder purchase = receivedPurchase("40", "40", "100");
        when(purchaseRepository.findByMaterialPlanOrderByPurchaseIdAsc(plan)).thenReturn(List.of(purchase));
        Payable payable = payable(new BigDecimal("4000"), new BigDecimal("4000"));
        when(payableRepository.findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(1L, 10L))
                .thenReturn(Optional.of(payable));
        // 同一供应商另一张未结清应付：可冲抵 1500
        Payable other = payable(new BigDecimal("2500"), new BigDecimal("1000"));
        other.setPayableId(21L);
        other.setSourceNo("PO-OTHER");
        other.setPayeePartner(supplier());
        payable.setPayeePartner(supplier());
        when(payableRepository.findByCompany_ComIdAndPayeePartner_PartnerIdOrderByDueDateAsc(1L, 50L))
                .thenReturn(List.of(other));
        ArgumentCaptor<PartnerCredit> creditCaptor = ArgumentCaptor.forClass(PartnerCredit.class);

        List<MaterialReturn> returns = context.reduce(plan, new BigDecimal("20"), "减项", operator);

        assertThat(returns).hasSize(1);
        // 本单应付已付清 → 冲抵同供应商其他应付 1500，剩余 500 转供应商往来余额
        assertThat(returns.get(0).getStatus()).isEqualTo(MaterialReturnStatus.Confirmed);
        assertThat(payable.getAmount()).isEqualByComparingTo("4000");
        assertThat(other.getAmount()).isEqualByComparingTo("1000");
        verify(partnerCreditRepository).save(creditCaptor.capture());
        assertThat(creditCaptor.getValue().getAmount()).isEqualByComparingTo("500");
    }

    private com.th4.edge.decoboon.supply.model.Partner supplier() {
        com.th4.edge.decoboon.supply.model.Partner partner = new com.th4.edge.decoboon.supply.model.Partner();
        partner.setPartnerId(50L);
        partner.setCompany(company);
        return partner;
    }

    @Test
    void shouldAdjustPurchaseOrderWhenOrderedNotReceived() {
        PurchaseOrder purchase = receivedPurchase("40", "0", "100");
        purchase.setStatus(PurchaseStatus.Ordered);
        when(purchaseRepository.findByMaterialPlanOrderByPurchaseIdAsc(plan)).thenReturn(List.of(purchase));
        Payable payable = payable(new BigDecimal("4000"), new BigDecimal("0"));
        when(payableRepository.findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(1L, 10L))
                .thenReturn(Optional.of(payable));

        List<MaterialReturn> returns = context.reduce(plan, new BigDecimal("20"), "减项", operator);

        assertThat(returns).isEmpty();
        assertThat(purchase.getQuantity()).isEqualByComparingTo("20");
        assertThat(purchase.getAmount()).isEqualByComparingTo("2000");
        assertThat(payable.getAmount()).isEqualByComparingTo("2000");
    }

    private PurchaseOrder receivedPurchase(String quantity, String received, String unitPrice) {
        PurchaseOrder purchase = new PurchaseOrder();
        purchase.setPurchaseId(10L);
        purchase.setNo("PO-TEST-1");
        purchase.setCompany(company);
        purchase.setProject(project);
        purchase.setMaterialPlan(plan);
        purchase.setQuantity(new BigDecimal(quantity));
        purchase.setReceivedQuantity(new BigDecimal(received));
        purchase.setUnitPrice(new BigDecimal(unitPrice));
        purchase.setAmount(new BigDecimal(quantity).multiply(new BigDecimal(unitPrice)));
        purchase.setSupplier(supplier());
        purchase.setStatus(PurchaseStatus.Received);
        return purchase;
    }

    private Payable payable(BigDecimal amount, BigDecimal paid) {
        Payable payable = new Payable();
        payable.setPayableId(20L);
        payable.setCompany(company);
        payable.setProject(project);
        payable.setType(PayableType.Material);
        payable.setAmount(amount);
        payable.setPaidAmount(paid);
        payable.setStatus(PayableStatus.Pending);
        return payable;
    }
}
