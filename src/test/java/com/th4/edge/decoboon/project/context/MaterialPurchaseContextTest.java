package com.th4.edge.decoboon.project.context;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.finance.context.PartnerCreditContext;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.supply.model.MaterialSupplier;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.MaterialSupplierRepository;
import com.th4.edge.decoboon.supply.repository.PartnerPriceRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.decoboon.common.access.StoreAttributionTestSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 材料采购确认角色、超订和价格来源口径。 */
class MaterialPurchaseContextTest {

    private final PurchaseOrderRepository purchaseRepository = mock(PurchaseOrderRepository.class);
    private final PurchaseOrderItemRepository purchaseItemRepository = mock(PurchaseOrderItemRepository.class);
    private final MaterialPlanRepository materialPlanRepository = mock(MaterialPlanRepository.class);
    private final MaterialPlanStatusContext materialPlanStatusContext = mock(MaterialPlanStatusContext.class);
    private final PayableRepository payableRepository = mock(PayableRepository.class);
    private final ModelIdentityContext modelIdentityContext = mock(ModelIdentityContext.class);
    private final PartnerCreditContext partnerCreditContext = mock(PartnerCreditContext.class);
    private final MaterialSupplierRepository materialSupplierRepository = mock(MaterialSupplierRepository.class);
    private final PartnerPriceRepository partnerPriceRepository = mock(PartnerPriceRepository.class);

    private MaterialPurchaseContext context() {
        return StoreAttributionTestSupport.inject(new MaterialPurchaseContext(purchaseRepository,
                purchaseItemRepository, materialPlanRepository,
                materialPlanStatusContext, payableRepository, modelIdentityContext, partnerCreditContext,
                materialSupplierRepository, partnerPriceRepository));
    }

    @Test
    void onlyBossStoreManagerFinanceCanConfirmPurchase() {
        MaterialPurchaseContext context = context();
        Employee finance = employee(EmployeeRole.Finance);
        Employee pm = employee(EmployeeRole.Pm);

        context.requireOperator(finance);
        assertThatThrownBy(() -> context.requireOperator(pm))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("只有企业负责人、店长或财务");
    }

    @Test
    void purchaseQuantityMustNotExceedRemainingDemand() {
        MaterialPurchaseContext context = context();
        MaterialPlan plan = new MaterialPlan();
        plan.setQuantity(new BigDecimal("108.00"));
        when(materialPlanStatusContext.orderedQuantity(plan)).thenReturn(new BigDecimal("80.00"));

        assertThat(context.remainingQuantity(plan)).isEqualByComparingTo("28.00");
        context.requireOrderQuantity(plan, new BigDecimal("28.00"));
        assertThatThrownBy(() -> context.requireOrderQuantity(plan, new BigDecimal("28.01")))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("超过可下单数量");
    }

    @Test
    void confirmOrderShouldCreateMaterialPayableInSameTransaction() {
        MaterialPurchaseContext context = context();
        var company = new com.th4.edge.tenant.user.model.Company();
        company.setComId(10L);
        Employee finance = employee(EmployeeRole.Finance);
        finance.setCompany(company);
        Partner supplier = new Partner();
        supplier.setPartnerId(20L);
        supplier.setName("测试材料商");
        MaterialPlan plan = new MaterialPlan();
        plan.setPlanId(30L);
        plan.setCompany(company);
        PurchaseOrder purchase = new PurchaseOrder();
        purchase.setPurchaseId(40L);
        purchase.setNo("PO-40");
        purchase.setCompany(company);
        purchase.setSupplier(supplier);
        purchase.setSupplierName(supplier.getName());
        purchase.setMaterialPlan(plan);
        purchase.setAmount(new BigDecimal("680.00"));
        when(purchaseItemRepository.findByPurchaseOrderOrderByPurchaseItemIdAsc(purchase)).thenReturn(java.util.List.of());
        when(materialPlanStatusContext.resolve(plan)).thenReturn(com.th4.edge.decoboon.project.configure.MaterialPlanStatus.ReadyToOrder);
        when(purchaseRepository.save(purchase)).thenReturn(purchase);
        when(modelIdentityContext.prepare(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        AtomicReference<com.th4.edge.decoboon.finance.model.Payable> savedPayable = new AtomicReference<>();
        when(payableRepository.findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(10L, 40L))
                .thenReturn(Optional.empty(), Optional.of(new com.th4.edge.decoboon.finance.model.Payable()));
        when(payableRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            com.th4.edge.decoboon.finance.model.Payable payable = invocation.getArgument(0);
            savedPayable.set(payable);
            return payable;
        });

        PurchaseOrder confirmed = context.confirmOrder(purchase, finance);

        assertThat(confirmed.getStatus()).isEqualTo(PurchaseStatus.Ordered);
        assertThat(savedPayable.get()).isNotNull();
        assertThat(savedPayable.get().getAmount()).isEqualByComparingTo("680.00");
        assertThat(savedPayable.get().getPurchaseOrder()).isSameAs(purchase);
        assertThat(savedPayable.get().getType()).isEqualTo(com.th4.edge.decoboon.finance.configure.PayableType.Material);
    }

    @Test
    void executionPriceShouldRecordSupplyRelationWhenMatched() {
        MaterialPurchaseContext context = context();
        MaterialPlan plan = new MaterialPlan();
        plan.setMaterialKey("brand:m&b|spec:750x1500|unit:Area");
        var company = new com.th4.edge.tenant.user.model.Company();
        company.setComId(10L);
        plan.setCompany(company);
        Partner supplier = new Partner();
        supplier.setPartnerId(20L);
        MaterialSupplier relation = new MaterialSupplier();
        relation.setActive(true);
        relation.setPurchasePrice(new BigDecimal("68.00"));
        when(materialSupplierRepository.findFirstByCompany_ComIdAndMaterialKeyAndPartner_PartnerId(
                10L, plan.getMaterialKey(), 20L)).thenReturn(Optional.of(relation));

        assertThat(context.resolvePriceSource(plan, supplier, new BigDecimal("68.00")))
                .isEqualTo(MaterialPurchaseContext.PRICE_SOURCE_SUPPLY_RELATION);
        assertThat(context.resolvePriceSource(plan, supplier, new BigDecimal("66.00")))
                .isEqualTo(MaterialPurchaseContext.PRICE_SOURCE_MANUAL);
    }

    private Employee employee(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setRole(role);
        return employee;
    }
}
