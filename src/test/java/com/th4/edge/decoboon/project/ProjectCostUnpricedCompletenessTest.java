package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.ProjectCostContext;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 无价采购的成本完整性守卫（BR-045 / DEC-072）。
 *
 * <p>口径：无价下单时价格留空并形成暂估应付，实际成本要等结算价回填才收敛；
 * 在此之前成本必须判为**不完整**，利润与毛利率为 null，**禁止按 0 成本计算伪利润**。</p>
 */
class ProjectCostUnpricedCompletenessTest {

    private QuoteLineRepository quoteLineRepository;
    private ContractLineRepository contractLineRepository;
    private PurchaseOrderRepository purchaseOrderRepository;
    private ProjectCostContext context;

    private Project project;
    private QuoteLine quoteLine;
    private ContractLine contractLine;
    private PurchaseOrder order;

    @BeforeEach
    void setUp() {
        quoteLineRepository = mock(QuoteLineRepository.class);
        contractLineRepository = mock(ContractLineRepository.class);
        purchaseOrderRepository = mock(PurchaseOrderRepository.class);
        context = new ProjectCostContext(quoteLineRepository, contractLineRepository, purchaseOrderRepository);

        project = new Project();
        project.setProjectId(9L);
        // ProjectCostContext 的合同口径分支要求项目挂报价（无报价时走项目级回退并直接判为不完整）
        project.setQuote(new com.th4.edge.decoboon.quote.model.Quote());

        CatalogItem catalogItem = new CatalogItem();
        catalogItem.setItemId(77L);
        catalogItem.setType(com.th4.edge.decoboon.catalog.configure.ItemType.Material);
        catalogItem.setCostPrice(new BigDecimal("100"));
        catalogItem.setMaterialPrice(new BigDecimal("200"));
        catalogItem.setManualPrice(BigDecimal.ZERO);

        quoteLine = new QuoteLine();
        quoteLine.setItemId(1L);
        quoteLine.setQuantity(new BigDecimal("10"));
        quoteLine.setCost(new BigDecimal("1000"));
        quoteLine.setAmount(new BigDecimal("2000"));
        quoteLine.setActualCost(new BigDecimal("1000"));
        quoteLine.setCatalogItem(catalogItem);

        com.th4.edge.decoboon.project.model.ProjectContract contract =
                new com.th4.edge.decoboon.project.model.ProjectContract();
        contract.setProject(project);

        contractLine = new ContractLine();
        contractLine.setContract(contract);
        contractLine.setSourceQuoteLine(quoteLine);
        contractLine.setQuantity(new BigDecimal("10"));
        contractLine.setType(com.th4.edge.decoboon.catalog.configure.ItemType.Material);
        contractLine.setCost(new BigDecimal("1000"));
        contractLine.setAmount(new BigDecimal("2000"));

        MaterialPlan plan = new MaterialPlan();
        plan.setPlanId(5L);

        order = new PurchaseOrder();
        order.setPurchaseId(3L);
        order.setProject(project);
        order.setQuoteLine(quoteLine);
        order.setMaterialPlan(plan);
        order.setStatus(PurchaseStatus.Received);
        order.setQuantity(new BigDecimal("10"));
        order.setReceivedQuantity(new BigDecimal("10"));
        order.setPriceState(SheetPriceState.Priced);

        when(contractLineRepository.findByContract_Project_ProjectIdInOrderBySortAsc(any()))
                .thenReturn(List.of(contractLine));
        when(purchaseOrderRepository.findByProject_ProjectIdInOrderByPurchaseIdDesc(any()))
                .thenReturn(List.of(order));
    }

    @Test
    void pricedAndFullyReceivedPurchaseShouldBeComplete() {
        ProjectCostContext.CostSummary summary = context.summarize(project);
        assertThat(summary.complete()).isTrue();
        assertThat(summary.incompleteItemCount()).isZero();
    }

    @Test
    void unpricedPurchaseMustMakeCostIncomplete() {
        order.setPriceState(SheetPriceState.Unpriced);
        ProjectCostContext.CostSummary summary = context.summarize(project);
        assertThat(summary.complete())
                .as("无价（未定价）采购在结算收敛前必须判为成本不完整，否则会算出伪利润")
                .isFalse();
        assertThat(summary.incompleteItemCount()).isPositive();
    }

    @Test
    void unpricedAfterSettlementBackfillShouldBeCompleteAgain() {
        order.setPriceState(SheetPriceState.Settled);
        ProjectCostContext.CostSummary summary = context.summarize(project);
        assertThat(summary.complete()).isTrue();
    }
}
