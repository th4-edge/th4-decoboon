package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.context.DesignLifecycleContext;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.components.lock.DistributedLocked;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.plan.context.ContractPlanContext;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 项目合同基线、合同行与变更金额上下文。 */
@Component
public class ProjectContractContext {

    private final ProjectContractRepository contractRepository;
    private final ContractChangeRepository changeRepository;
    private final ContractLineRepository lineRepository;
    private final ReceivableRepository receivableRepository;
    private final ProjectRepository projectRepository;
    private final CustomerRepository customerRepository;
    private final QuoteLineRepository quoteLineRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyContext propertyContext;
    private final FinanceAccessContext financeAccessContext;
    private final ProjectPermissionContext permissionContext;
    private final ContractChangeFulfillmentContext fulfillmentContext;
    private final ProjectContext projectContext;
    private final ContractPlanContext contractPlanContext;
    private final DesignLifecycleContext designLifecycleContext;

    public ProjectContractContext(ProjectContractRepository contractRepository,
                                  ContractChangeRepository changeRepository,
                                  ContractLineRepository lineRepository,
                                  ReceivableRepository receivableRepository,
                                  ProjectRepository projectRepository,
                                  CustomerRepository customerRepository,
                                  QuoteLineRepository quoteLineRepository,
                                  PropertyRepository propertyRepository,
                                  PropertyContext propertyContext,
                                  FinanceAccessContext financeAccessContext,
                                  ProjectPermissionContext permissionContext,
                                  ContractChangeFulfillmentContext fulfillmentContext,
                                  ProjectContext projectContext,
                                  ContractPlanContext contractPlanContext,
                                  DesignLifecycleContext designLifecycleContext) {
        this.contractRepository = contractRepository;
        this.changeRepository = changeRepository;
        this.lineRepository = lineRepository;
        this.receivableRepository = receivableRepository;
        this.projectRepository = projectRepository;
        this.customerRepository = customerRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.propertyRepository = propertyRepository;
        this.propertyContext = propertyContext;
        this.financeAccessContext = financeAccessContext;
        this.permissionContext = permissionContext;
        this.fulfillmentContext = fulfillmentContext;
        this.projectContext = projectContext;
        this.contractPlanContext = contractPlanContext;
        this.designLifecycleContext = designLifecycleContext;
    }

    /** 客户确认报价后先创建合同草稿/待签，不创建项目。 */
    @DistributedLocked(key = "#root.contractLockKey(#quote)")
    @Transactional
    public ProjectContract createFromQuote(Quote quote, Employee employee, LocalDate signedAt,
                                           LocalDate startDate, LocalDate planEndDate, String remark) {
        return createFromQuote(quote, employee, signedAt, quote == null ? null : quote.getAmount(),
                startDate, planEndDate, remark);
    }

    /** 客户确认报价后先创建合同草稿/待签，不创建项目。 */
    @DistributedLocked(key = "#root.contractLockKey(#quote)")
    @Transactional
    public ProjectContract createFromQuote(Quote quote, Employee employee, LocalDate signedAt, BigDecimal signedAmount,
                                           LocalDate startDate, LocalDate planEndDate, String remark) {
        return createFromQuote(quote, employee, signedAt, signedAmount, startDate, planEndDate, remark, null, null);
    }

    @DistributedLocked(key = "#root.contractLockKey(#quote)")
    @Transactional
    public ProjectContract createFromQuote(Quote quote, Employee employee, LocalDate signedAt, BigDecimal signedAmount,
                                           LocalDate startDate, LocalDate planEndDate, String remark,
                                           String paymentPlanCode, String constructionPlanCode) {
        ProjectContract existing = contractRepository.findByQuote_QuoteId(quote.getQuoteId()).orElse(null);
        if (existing != null) {
            return existing;
        }
        Property property = quote.getProperty();
        if (property == null || property.getPropertyId() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "报价缺少客户房屋，请先建立并选择房屋档案"));
        }
        Property locked = propertyRepository.findByPropertyIdAndCompany_ComId(
                        property.getPropertyId(), quote.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "房屋不存在或无权访问")));
        if (contractRepository.existsByProperty_PropertyId(locked.getPropertyId())) {
            throw new DefinedException(new DefaultErrorCode("400", "该房屋已经生成唯一合同，不能重复签约"));
        }
        return contractRepository.findByQuote_QuoteId(quote.getQuoteId()).orElseGet(() -> {
            ProjectContract contract = new ProjectContract();
            contract.setCompany(quote.getCompany());
            contract.setProperty(locked);
            contract.setCustomer(quote.getCustomer());
            contract.setQuote(quote);
            contract.setNo("C" + System.currentTimeMillis());
            contract.setBaselineAmount(quote.getAmount());
            contract.setCurrentAmount(quote.getAmount());
            contract.setStatus(ContractStatus.PendingSign);
            contract.setSignedAt(null);
            contract.setStartDate(startDate);
            contract.setPlanEndDate(planEndDate);
            contract.setCreatedBy(employee);
            contract.setRemark(remark);
            permissionContext.apply(contract, locked, quote.getCustomer());
            ProjectContract saved = contractRepository.save(contract);
            syncLines(saved, quoteLineRepository.findByQuote(quote));
            contractPlanContext.select(saved, paymentPlanCode, constructionPlanCode);
            return signedAt == null ? saved : markSigned(saved, signedAt, signedAmount);
        });
    }

    @Transactional
    public ProjectContract markSigned(ProjectContract contract) {
        return markSigned(contract, LocalDate.now(), contract == null ? null : contract.getBaselineAmount());
    }

    @Transactional
    public ProjectContract markSigned(ProjectContract contract, LocalDate signedAt) {
        return markSigned(contract, signedAt, contract == null ? null : contract.getBaselineAmount());
    }

    @Transactional
    public ProjectContract markSigned(ProjectContract contract, LocalDate signedAt, BigDecimal signedAmount) {
        if (contract == null || contract.getContractId() == null) {
            throw new DefinedException(new DefaultErrorCode("404", "合同不存在或无权访问"));
        }
        if (contract.getStatus() != ContractStatus.Draft && contract.getStatus() != ContractStatus.PendingSign) {
            throw new DefinedException(new DefaultErrorCode("400", "只有草稿或待签约合同可以签约"));
        }
        BigDecimal finalSignedAmount = money(signedAmount);
        if (finalSignedAmount.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "最终签约价格必须大于 0"));
        }
        contractPlanContext.ensure(contract);
        snapshotSignedCost(contract);
        contract.setSignedAmount(finalSignedAmount);
        contract.setCurrentAmount(finalSignedAmount);
        contractPlanContext.repricePaymentPlan(contract);
        contract.setStatus(ContractStatus.Signed);
        contract.setSignedAt(signedAt == null ? LocalDate.now() : signedAt);
        markCustomerStatus(contract.getQuote(), CustStatus.Signed);
        if (contract.getProperty() != null) {
            propertyContext.updateStatus(contract.getProperty(),
                    com.th4.edge.decoboon.property.configure.PropertyStatus.Contracted);
        }
        ProjectContract saved = contractRepository.save(contract);
        // 合同签约即客户已经确定方案，补齐设计方案状态，防止历史或补录路径断链。
        designLifecycleContext.markSelected(saved.getQuote() == null ? null : saved.getQuote().getDesign());
        return saved;
    }

    @Transactional
    public ProjectContract markEffective(ProjectContract contract) {
        if (contract == null || contract.getContractId() == null) {
            throw new DefinedException(new DefaultErrorCode("404", "合同不存在或无权访问"));
        }
        if (contract.getSignedAt() == null
                || (contract.getStatus() != ContractStatus.Signed
                && contract.getStatus() != ContractStatus.Effective
                && contract.getStatus() != ContractStatus.Active)) {
            throw new DefinedException(new DefaultErrorCode("400", "合同签约后才能生效"));
        }
        contractPlanContext.ensure(contract);
        contract.setStatus(ContractStatus.Effective);
        contract.setEffectiveAt(LocalDateTime.now());
        markCustomerStatus(contract.getQuote(), CustStatus.Serving);
        ProjectContract saved = contractRepository.save(contract);
        designLifecycleContext.markSelected(saved.getQuote() == null ? null : saved.getQuote().getDesign());
        return saved;
    }

    private void markCustomerStatus(Quote quote, CustStatus status) {
        if (quote == null || quote.getCustomer() == null || quote.getCustomer().getCustId() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "合同缺少有效客户"));
        }
        quote.getCustomer().setStatus(status);
        customerRepository.save(quote.getCustomer());
    }

    /**
     * 审批增减项变更单。
     *
     * <p>DEC-041 / BR-035：客户线下确认是创建前置事实，企业审批通过即生效——
     * 审批通过时在同一事务内完成合同金额、项目合同额、应收、任务、材料与成本联动；
     * 审批驳回只落状态。历史 {@code Approved}（待客户确认）数据仍由
     * {@link #confirmCustomer} 兼容处理。</p>
     */
    @Transactional
    public ContractChange approve(ContractChange change, Employee approver, boolean approved, String conclusion) {
        if (change.getStatus() == ContractChangeStatus.CustomerConfirmed) {
            return change; // 已生效（新口径），幂等返回，不重复结算。
        }
        change.setApprovedBy(approver);
        change.setConclusion(conclusion);
        change.setApprovedAt(LocalDateTime.now());
        if (!approved) {
            change.setStatus(ContractChangeStatus.Rejected);
            return changeRepository.save(change);
        }
        if (!Boolean.TRUE.equals(change.getCustomerConfirmed())) {
            throw new DefinedException(new DefaultErrorCode("400", "缺少客户线下确认，不能审批生效"));
        }
        return settle(change, approver, ContractChangeStatus.CustomerConfirmed);
    }

    /**
     * 历史口径兼容：审批通过后再登记客户确认。
     *
     * <p>新口径审批通过即生效，不再产生 {@code Approved} 待确认数据；
     * 此入口仅用于存量单据与旧客户端过渡。重复调用幂等。</p>
     */
    @Transactional
    public ContractChange confirmCustomer(ContractChange change, Employee operator) {
        if (change.getStatus() == ContractChangeStatus.CustomerConfirmed) {
            return change;
        }
        if (change.getStatus() == ContractChangeStatus.Approved && Boolean.TRUE.equals(change.getCustomerConfirmed())) {
            return change; // 新口径已生效，幂等。
        }
        if (change.getStatus() != ContractChangeStatus.Approved) {
            throw new DefinedException(new DefaultErrorCode("400", "变更单审批通过后才能进行客户确认"));
        }
        change.setCustomerConfirmed(true);
        change.setCustomerConfirmedAt(LocalDateTime.now());
        return settle(change, operator, ContractChangeStatus.CustomerConfirmed);
    }

    /**
     * 合同变更生效结算：合同当前额、项目合同额、应收、任务、材料、成本一致联动。
     *
     * <p>调用方必须先完成客户确认与审批前置校验；本方法只做一次结算，
     * 通过状态与 {@code customerConfirmed} 双保险避免重复生成应收/任务/材料。</p>
     */
    private ContractChange settle(ContractChange change, Employee operator, ContractChangeStatus targetStatus) {
        ProjectContract contract = change.getContract();
        BigDecimal current = value(contract.getCurrentAmount());
        BigDecimal delta = value(change.getAmount());
        BigDecimal next = current.add(delta);
        if (next.signum() < 0) {
            throw new DefinedException(new DefaultErrorCode("400", "变更后合同金额不能小于 0"));
        }
        contract.setCurrentAmount(next);
        contract.setStatus(ContractStatus.Changed);
        contractRepository.save(contract);
        Project project = change.getProject();
        project.setContractAmount(next);
        projectRepository.save(project);
        fulfillmentContext.apply(change, operator);
        projectContext.refreshProject(project);
        syncReceivable(change, operator, delta);
        if (!Boolean.TRUE.equals(change.getCustomerConfirmed())) {
            change.setCustomerConfirmed(true);
        }
        if (change.getCustomerConfirmedAt() == null) {
            change.setCustomerConfirmedAt(LocalDateTime.now());
        }
        change.setStatus(targetStatus);
        return changeRepository.save(change);
    }

    public ProjectContract enrich(ProjectContract contract) {
        if (contract == null) return null;
        List<Receivable> receivables = contract.getProject() == null ? List.of()
                : receivableRepository.findByProjectOrderByDueDateAsc(contract.getProject());
        contract.setReceivableCount(receivables.size());
        contract.setReceivableAmount(receivables.stream().map(Receivable::getAmount)
                .filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add));
        contract.setReceivedAmount(receivables.stream().map(Receivable::getReceivedAmount)
                .filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add));
        contract.setPendingChangeCount((int) changeRepository.findByContractOrderByChangeIdDesc(contract).stream()
                .filter(item -> item.getStatus() == ContractChangeStatus.Pending
                        || item.getStatus() == ContractChangeStatus.Approved).count());
        List<ContractLine> lines = lineRepository.findByContractOrderBySortAsc(contract);
        BigDecimal signedCost = BigDecimal.ZERO;
        boolean complete = !lines.isEmpty();
        for (ContractLine line : lines) {
            BigDecimal lineCost = value(line.getCost());
            signedCost = signedCost.add(lineCost);
            if (requiresCost(line.getType()) && lineCost.signum() <= 0) {
                complete = false;
            }
        }
        contract.setSignedCost(money(signedCost));
        contract.setCostComplete(complete);
        contract.setPaymentPlan(contractPlanContext.paymentPlan(contract));
        contract.setConstructionPlan(contractPlanContext.constructionPlan(contract));
        return contract;
    }

    private void syncReceivable(ContractChange change, Employee operator, BigDecimal delta) {
        if (delta.signum() == 0) {
            return;
        }
        List<Receivable> receivables = receivableRepository.findByProjectOrderByDueDateAsc(change.getProject());
        if (delta.signum() > 0) {
            Receivable receivable = new Receivable();
            receivable.setCompany(change.getCompany());
            receivable.setProject(change.getProject());
            receivable.setProperty(change.getProperty());
            receivable.setContract(change.getContract());
            receivable.setName("变更款 · " + change.getNo());
            receivable.setStageName("合同变更");
            receivable.setType(ReceivableType.Progress);
            receivable.setAmount(delta);
            receivable.setReceivedAmount(BigDecimal.ZERO);
            receivable.setDueDate(LocalDate.now().plusDays(7));
            receivable.setStatus(ReceivableStatus.Pending);
            receivable.setInvoiceRequired(false);
            receivable.setRemark(change.getReason());
            receivable.setCreatedBy(operator);
            financeAccessContext.applyOwnership(receivable, operator, change.getProject());
            receivableRepository.save(receivable);
            return;
        }

        BigDecimal remaining = delta.abs();
        for (int i = receivables.size() - 1; i >= 0 && remaining.signum() > 0; i--) {
            Receivable receivable = receivables.get(i);
            BigDecimal received = value(receivable.getReceivedAmount());
            BigDecimal adjustable = value(receivable.getAmount()).subtract(received);
            if (adjustable.signum() <= 0) continue;
            BigDecimal reduced = adjustable.min(remaining);
            receivable.setAmount(value(receivable.getAmount()).subtract(reduced));
            financeAccessContext.refreshReceivableStatus(receivable);
            receivableRepository.save(receivable);
            remaining = remaining.subtract(reduced);
        }
        if (remaining.signum() > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "减项金额超过未收应收余额，请先调整应收计划"));
        }
    }

    private void syncLines(ProjectContract contract, List<QuoteLine> quoteLines) {
        List<ContractLine> lines = quoteLines.stream().map(source -> {
            ContractLine line = new ContractLine();
            line.setCompany(contract.getCompany());
            line.setProperty(contract.getProperty());
            line.setContract(contract);
            permissionContext.apply(line, contract.getProperty(), contract.getCustomer());
            line.setSourceQuoteLine(source);
            line.setType(source.getType());
            line.setCategory(source.getCategory());
            line.setName(source.getName());
            line.setBrand(source.getBrand());
            line.setSpec(source.getSpec());
            line.setUnit(source.getUnit());
            line.setQuantity(source.getQuantity());
            line.setMaterialPrice(source.getMaterialPrice());
            line.setManualPrice(source.getManualPrice());
            line.setPricingMode(source.getPricingMode());
            line.setAmount(source.getAmount());
            line.setCost(source.getCost());
            line.setFormula(source.getFormula());
            line.setRole(source.getRole());
            line.setSpaceName(source.getSpace() == null ? null : source.getSpace().getName());
            line.setSort(source.getSort());
            return line;
        }).toList();
        lineRepository.saveAll(lines);
    }

    private boolean requiresCost(com.th4.edge.decoboon.catalog.configure.ItemType type) {
        return type == com.th4.edge.decoboon.catalog.configure.ItemType.Construction
                || type == com.th4.edge.decoboon.catalog.configure.ItemType.Material
                || type == com.th4.edge.decoboon.catalog.configure.ItemType.Custom
                || type == com.th4.edge.decoboon.catalog.configure.ItemType.Soft
                || type == com.th4.edge.decoboon.catalog.configure.ItemType.Appliance;
    }

    private void snapshotSignedCost(ProjectContract contract) {
        List<ContractLine> lines = lineRepository.findByContractOrderBySortAsc(contract);
        for (ContractLine line : lines) {
            QuoteLine source = line.getSourceQuoteLine();
            BigDecimal lineCost = value(line.getCost());
            if (lineCost.signum() <= 0 && source != null) {
                lineCost = value(source.getCost());
            }
            if (lineCost.signum() <= 0) {
                BigDecimal unitCost = source == null || source.getCatalogItem() == null
                        || source.getCatalogItem().getCostPrice() == null
                        ? BigDecimal.ZERO : source.getCatalogItem().getCostPrice();
                BigDecimal quantity = value(line.getQuantity());
                lineCost = line.getPricingMode() == com.th4.edge.decoboon.catalog.configure.PricingMethod.FixedTotal
                        ? unitCost : unitCost.multiply(quantity);
            }
            // 签约时保留已有冻结成本；只有缺失时才落当前标准成本。
            line.setCost(money(lineCost));
        }
        if (!lines.isEmpty()) {
            lineRepository.saveAll(lines);
        }
    }

    /**
     * 同一房屋串行建合同：house 锁与报价确认、立项共用同一键，替代原房屋行级悲观锁。
     * key 不可解析时退化为报价维度，保证仍有非空锁键并由业务方法返回既有错误口径。
     */
    public String contractLockKey(Quote quote) {
        if (quote == null) {
            return "property-contract:unknown";
        }
        Property property = quote.getProperty();
        if (property == null || property.getPropertyId() == null) {
            return "property-contract:quote:" + quote.getQuoteId();
        }
        Long comId = property.getCompany() != null ? property.getCompany().getComId()
                : (quote.getCompany() == null ? null : quote.getCompany().getComId());
        return "property:" + comId + ":" + property.getPropertyId();
    }

    private BigDecimal money(BigDecimal value) {
        return value(value).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
