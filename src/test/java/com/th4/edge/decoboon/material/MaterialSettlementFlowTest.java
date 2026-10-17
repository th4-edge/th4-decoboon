package com.th4.edge.decoboon.material;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.material.configure.SettlementScope;
import com.th4.edge.decoboon.material.configure.SettlementStatus;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.material.context.MaterialSettlementContext;
import com.th4.edge.decoboon.material.context.MaterialSheetPriceProjectionContext;
import com.th4.edge.decoboon.material.model.MaterialSettlement;
import com.th4.edge.decoboon.material.model.MaterialSettlementItem;
import com.th4.edge.decoboon.material.model.SettlementPreviewRow;
import com.th4.edge.decoboon.material.repository.MaterialSettlementItemRepository;
import com.th4.edge.decoboon.material.repository.MaterialSettlementRepository;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.MaterialPurchaseContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 主材采购结算流程（BR-045 / DEC-073）。
 *
 * <p>覆盖：逐单结算回填（结算价 + 暂估应付修正 + 实际成本差额补记）、**结算互斥**（同一采购明细不得重复计入）、
 * **月结兜底**（已逐单结算的明细不再进入月结候选集）。</p>
 */
class MaterialSettlementFlowTest {

    private final MaterialSettlementRepository settlementRepository = mock(MaterialSettlementRepository.class);
    private final MaterialSettlementItemRepository settlementItemRepository = mock(MaterialSettlementItemRepository.class);
    private final PurchaseOrderRepository purchaseOrderRepository = mock(PurchaseOrderRepository.class);
    private final PurchaseOrderItemRepository purchaseItemRepository = mock(PurchaseOrderItemRepository.class);
    private final PayableRepository payableRepository = mock(PayableRepository.class);
    private final QuoteLineRepository quoteLineRepository = mock(QuoteLineRepository.class);
    private final PartnerRepository partnerRepository = mock(PartnerRepository.class);
    private final ProjectAccessContext accessContext = mock(ProjectAccessContext.class);
    private final MaterialPurchaseContext purchaseContext = mock(MaterialPurchaseContext.class);
    private final ModelIdentityContext identityContext = mock(ModelIdentityContext.class);
    private final MaterialSheetPriceProjectionContext priceProjectionContext = mock(MaterialSheetPriceProjectionContext.class);

    private final MaterialSettlementContext context = new MaterialSettlementContext(
            settlementRepository, settlementItemRepository, purchaseOrderRepository, purchaseItemRepository,
            payableRepository, quoteLineRepository, partnerRepository, accessContext, purchaseContext, identityContext,
            priceProjectionContext);

    private Company company;
    private Employee finance;
    private Partner supplier;
    private PurchaseOrder order;
    private PurchaseOrderItem itemPriced;
    private PurchaseOrderItem itemUnpriced;
    private Payable payable;
    private QuoteLine quoteLine;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setComId(1L);
        finance = new Employee();
        finance.setEmpId(3L);
        finance.setRole(EmployeeRole.Finance);
        finance.setCompany(company);
        supplier = new Partner();
        supplier.setPartnerId(4L);
        supplier.setCompany(company);
        supplier.setName("利家福");

        Project project = new Project();
        project.setProjectId(9L);
        project.setCompany(company);

        quoteLine = new QuoteLine();
        quoteLine.setItemId(1L);
        quoteLine.setQuantity(new BigDecimal("10"));
        quoteLine.setCost(new BigDecimal("680"));
        quoteLine.setAmount(new BigDecimal("1000"));
        quoteLine.setActualMaterialCost(BigDecimal.ZERO);
        quoteLine.setActualLaborCost(BigDecimal.ZERO);
        quoteLine.setActualOtherCost(BigDecimal.ZERO);

        MaterialPlan plan = new MaterialPlan();
        plan.setPlanId(5L);
        plan.setProject(project);
        plan.setQuoteItem(quoteLine);

        order = new PurchaseOrder();
        order.setPurchaseId(2000L);
        order.setCompany(company);
        order.setProject(project);
        order.setSupplier(supplier);
        order.setSupplierName("利家福");
        order.setNo("PO-1");
        order.setQuantity(new BigDecimal("20"));
        order.setUnit(Unit.Sheet);
        order.setStatus(PurchaseStatus.Received);
        order.setPriceState(SheetPriceState.Unpriced);

        itemPriced = new PurchaseOrderItem();
        itemPriced.setPurchaseItemId(21L);
        itemPriced.setCompany(company);
        itemPriced.setPurchaseOrder(order);
        itemPriced.setMaterialPlan(plan);
        itemPriced.setQuantity(new BigDecimal("10"));
        itemPriced.setUnitPrice(new BigDecimal("68"));
        itemPriced.setReceivedQuantity(new BigDecimal("10"));
        itemPriced.setPriceState(SheetPriceState.Priced);

        itemUnpriced = new PurchaseOrderItem();
        itemUnpriced.setPurchaseItemId(22L);
        itemUnpriced.setCompany(company);
        itemUnpriced.setPurchaseOrder(order);
        itemUnpriced.setMaterialPlan(plan);
        itemUnpriced.setQuantity(new BigDecimal("10"));
        itemUnpriced.setUnitPrice(BigDecimal.ZERO);
        itemUnpriced.setReceivedQuantity(new BigDecimal("10"));
        itemUnpriced.setPriceState(SheetPriceState.Unpriced);

        payable = new Payable();
        payable.setPayableId(3000L);
        payable.setCompany(company);
        payable.setPurchaseOrder(order);
        payable.setAmount(BigDecimal.ZERO);
        payable.setEstimated(Boolean.TRUE);
        payable.setPaidAmount(BigDecimal.ZERO);

        when(accessContext.requireEmployee(any())).thenReturn(finance);
        when(partnerRepository.findById(4L)).thenReturn(Optional.of(supplier));
        when(identityContext.prepare(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(settlementRepository.save(any())).thenAnswer(invocation -> {
            MaterialSettlement settlement = invocation.getArgument(0);
            if (settlement.getSettlementId() == null) settlement.setSettlementId(900L);
            return settlement;
        });
        when(settlementItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(settlementItemRepository.findByPurchaseItem_PurchaseItemIdIn(any())).thenReturn(List.of());
        when(purchaseItemRepository.findAllById(any())).thenReturn(List.of(itemPriced, itemUnpriced));
        when(purchaseItemRepository.findByPurchaseOrderOrderByPurchaseItemIdAsc(any()))
                .thenReturn(List.of(itemPriced, itemUnpriced));
        when(purchaseItemRepository.findByPurchaseOrderInOrderByPurchaseItemIdAsc(any()))
                .thenReturn(List.of(itemPriced, itemUnpriced));
        when(purchaseItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseOrderRepository.findById(2000L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findByCompany_ComIdAndSupplier_PartnerIdOrderByPurchaseIdDesc(any(), any()))
                .thenReturn(List.of(order));
        when(payableRepository.findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(any(), any()))
                .thenReturn(Optional.of(payable));
        when(quoteLineRepository.findAllById(any())).thenReturn(List.of(quoteLine));
        when(quoteLineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(settlementRepository.findFirstBySettlementIdAndCompany_ComId(any(), any()))
                .thenReturn(Optional.of(settlement()));
        when(settlementItemRepository.findBySettlement_SettlementIdOrderBySettlementItemIdAsc(any()))
                .thenReturn(List.of(existingItem()));
    }

    /** 已确认的结算单（撤销用例夹具）。 */
    private MaterialSettlement settlement() {
        MaterialSettlement settlement = new MaterialSettlement();
        settlement.setSettlementId(900L);
        settlement.setCompany(company);
        settlement.setNo("ST-1");
        settlement.setSupplier(supplier);
        settlement.setStatus(SettlementStatus.Confirmed);
        settlement.setTotalAmount(new BigDecimal("700"));
        return settlement;
    }

    /** 撤销用例夹具：一条已结算明细（采购明细 21）。 */
    private MaterialSettlementItem existingItem() {
        MaterialSettlementItem item = new MaterialSettlementItem();
        item.setSettlementItemId(1L);
        item.setCompany(company);
        item.setPurchaseItem(itemPriced);
        item.setQuantity(new BigDecimal("10"));
        item.setSettlementPrice(new BigDecimal("70"));
        item.setAmount(new BigDecimal("700"));
        return item;
    }

    @Test
    void cancelShouldRollBackSettlementAndAllowResettle() {
        itemPriced.setSettlementPrice(new BigDecimal("70"));
        itemPriced.setPriceState(SheetPriceState.Settled);
        quoteLine.setActualMaterialCost(new BigDecimal("20"));

        MaterialSettlement cancelled = context.cancel(finance, 900L, "对账口径有误");

        assertThat(cancelled.getStatus()).isEqualTo(SettlementStatus.Cancelled);
        assertThat(cancelled.getCancelReason()).isEqualTo("对账口径有误");
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(itemPriced.getSettlementPrice())
                .as("撤销后采购明细结算价应清空，允许重新结算")
                .isNull();
        assertThat(itemPriced.getPriceState()).isEqualTo(SheetPriceState.Priced);
        assertThat(payable.getEstimated())
                .as("撤销后应付回到暂估，成本重新判为不完整")
                .isTrue();
        assertThat(quoteLine.getActualMaterialCost())
                .as("已补记的实际成本差额应被冲回")
                .isEqualByComparingTo("0");
        org.mockito.Mockito.verify(settlementItemRepository).deleteAll(any());
    }

    @Test
    void confirmMustRejectCrossCompanyPurchaseItem() {
        Company otherCompany = new Company();
        otherCompany.setComId(2L);
        itemPriced.setCompany(otherCompany);

        assertThatThrownBy(() -> context.confirm(finance, 4L, SettlementScope.PerPurchaseOrder, null,
                List.of(command(21L, "70")), null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("无权访问");
    }

    @Test
    void cancelFullyPricedSettlementShouldNotLeavePayableEstimated() {
        itemPriced.setSettlementPrice(new BigDecimal("70"));
        itemPriced.setPriceState(SheetPriceState.Settled);
        when(purchaseItemRepository.findByPurchaseOrderOrderByPurchaseItemIdAsc(any()))
                .thenReturn(List.of(itemPriced));

        context.cancel(finance, 900L, "对账口径有误");

        assertThat(payable.getEstimated())
                .as("全部行有采购价时，撤销结算后应付应回到确定金额，而不是继续暂估")
                .isFalse();
    }

    @Test
    void storeManagerShouldNotSeeOtherStoreSettlementCandidates() {
        Store ownStore = new Store();
        ownStore.setStoreId(1L);
        Store otherStore = new Store();
        otherStore.setStoreId(2L);
        Employee storeManager = new Employee();
        storeManager.setEmpId(4L);
        storeManager.setRole(EmployeeRole.StoreManager);
        storeManager.setCompany(company);
        storeManager.setStore(ownStore);
        order.setStore(otherStore);
        when(accessContext.requireEmployee(any())).thenReturn(storeManager);
        when(purchaseOrderRepository.findByCompany_ComIdOrderByPurchaseIdDesc(any())).thenReturn(List.of(order));

        assertThat(context.preview(storeManager, null, null, null, null, null)).isEmpty();
    }

    @Test
    void financeQueueShouldListAllSettleableItemsWithoutSupplier() {
        when(purchaseOrderRepository.findByCompany_ComIdOrderByPurchaseIdDesc(any())).thenReturn(List.of(order));
        when(purchaseItemRepository.findByPurchaseOrderInOrderByPurchaseItemIdAsc(any()))
                .thenReturn(List.of(itemPriced, itemUnpriced));

        List<SettlementPreviewRow> rows = context.preview(finance, null, null, null, null, null);

        assertThat(rows)
                .as("财务工作台无需先选供应商：返回企业全部可结算明细")
                .hasSize(2);
        assertThat(rows).anyMatch(row -> row.getPurchaseItemId().equals(22L) && row.isUnpriced());
    }

    @Test
    void financeQueueShouldExcludeAlreadySettledItems() {
        when(purchaseOrderRepository.findByCompany_ComIdOrderByPurchaseIdDesc(any())).thenReturn(List.of(order));
        when(purchaseItemRepository.findByPurchaseOrderInOrderByPurchaseItemIdAsc(any()))
                .thenReturn(List.of(itemPriced, itemUnpriced));
        when(settlementItemRepository.findByPurchaseItem_PurchaseItemIdIn(any())).thenReturn(List.of(existingItem()));

        List<SettlementPreviewRow> rows = context.preview(finance, null, null, null, null, null);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getPurchaseItemId()).isEqualTo(22L);
    }

    @Test
    void settlementBeforeReceiptMustBeRejected() {
        itemPriced.setReceivedQuantity(BigDecimal.ZERO);

        assertThatThrownBy(() -> context.confirm(finance, 4L, SettlementScope.PerPurchaseOrder, null,
                List.of(command(21L, "70")), null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("尚未到场");
    }

    @Test
    void previewMustExcludeItemsNotReceived() {
        itemPriced.setReceivedQuantity(BigDecimal.ZERO);
        when(purchaseOrderRepository.findByCompany_ComIdOrderByPurchaseIdDesc(any())).thenReturn(List.of(order));
        when(purchaseItemRepository.findByPurchaseOrderInOrderByPurchaseItemIdAsc(any()))
                .thenReturn(List.of(itemPriced));

        assertThat(context.preview(finance, null, null, null, null, null)).isEmpty();
    }

    @Test
    void settlementQuantityCannotExceedReceivedQuantity() {
        MaterialSettlementItem command = command(21L, "70");
        command.setQuantity(new BigDecimal("11"));

        assertThatThrownBy(() -> context.confirm(finance, 4L, SettlementScope.PerPurchaseOrder, null,
                List.of(command), null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不能超过已到场数量");
    }

    private MaterialSettlementItem command(Long purchaseItemId, String price) {
        PurchaseOrderItem ref = new PurchaseOrderItem();
        ref.setPurchaseItemId(purchaseItemId);
        MaterialSettlementItem line = new MaterialSettlementItem();
        line.setPurchaseItem(ref);
        line.setSettlementPrice(new BigDecimal(price));
        return line;
    }

    @Test
    void perOrderSettlementShouldBackfillPriceAndFixEstimatedPayable() {
        MaterialSettlement settlement = context.confirm(finance, 4L, SettlementScope.PerPurchaseOrder, null,
                List.of(command(21L, "70")), "对账确认");

        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.Confirmed);
        assertThat(settlement.getTotalAmount()).isEqualByComparingTo("700");
        assertThat(itemPriced.getSettlementPrice()).isEqualByComparingTo("70");
        assertThat(itemPriced.getPriceState()).isEqualTo(SheetPriceState.Settled);
        assertThat(payable.getEstimated())
                .as("仍有未定价明细时，应付必须保持暂估（否则成本会被误判为完整）")
                .isTrue();
        assertThat(payable.getSettledAmount()).isEqualByComparingTo("700");   // 已结算明细 70×10；未定价明细仍待结算
        assertThat(quoteLine.getActualMaterialCost())
                .as("实际成本按「结算价 − 采购价」差额补记：70 − 68 = 2/片 × 10 片")
                .isEqualByComparingTo("20");
    }

    @Test
    void settledPurchaseItemMustNotBeSettledAgain() {
        MaterialSettlementItem existing = new MaterialSettlementItem();
        PurchaseOrderItem ref = new PurchaseOrderItem();
        ref.setPurchaseItemId(21L);
        existing.setPurchaseItem(ref);
        when(settlementItemRepository.findByPurchaseItem_PurchaseItemIdIn(any())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> context.confirm(finance, 4L, SettlementScope.PerPurchaseOrder, null,
                List.of(command(21L, "70")), null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("结算互斥");
    }

    @Test
    void monthlyPreviewShouldExcludeAlreadySettledItems() {
        MaterialSettlementItem existing = new MaterialSettlementItem();
        PurchaseOrderItem ref = new PurchaseOrderItem();
        ref.setPurchaseItemId(21L);
        existing.setPurchaseItem(ref);
        when(settlementItemRepository.findByPurchaseItem_PurchaseItemIdIn(any())).thenReturn(List.of(existing));

        List<SettlementPreviewRow> rows = context.preview(finance, 4L, SettlementScope.Monthly, null, null);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getPurchaseItemId())
                .as("月结兜底：已逐单结算的明细不再进入月结候选集")
                .isEqualTo(22L);
        assertThat(rows.get(0).isUnpriced()).isTrue();
        assertThat(rows.get(0).getSuggestedSettlementPrice())
                .as("未定价明细需人工填结算价")
                .isNull();
    }

    @Test
    void monthlySettlementRequiresSupplier() {
        assertThatThrownBy(() -> context.preview(finance, null, SettlementScope.Monthly, null, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("月结必须指定供应商");
    }

    @Test
    void perOrderSettlementRequiresPurchaseOrder() {
        assertThatThrownBy(() -> context.preview(finance, 4L, SettlementScope.PerPurchaseOrder, null, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("逐单结算必须指定采购单");
    }
}
