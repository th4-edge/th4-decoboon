package com.th4.edge.decoboon.project.context;

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
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
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
    private final QuoteLineRepository quoteLineRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyContext propertyContext;
    private final FinanceAccessContext financeAccessContext;
    private final ProjectPermissionContext permissionContext;
    private final ContractChangeFulfillmentContext fulfillmentContext;
    private final ProjectContext projectContext;

    public ProjectContractContext(ProjectContractRepository contractRepository,
                                  ContractChangeRepository changeRepository,
                                  ContractLineRepository lineRepository,
                                  ReceivableRepository receivableRepository,
                                  ProjectRepository projectRepository,
                                  QuoteLineRepository quoteLineRepository,
                                  PropertyRepository propertyRepository,
                                  PropertyContext propertyContext,
                                  FinanceAccessContext financeAccessContext,
                                  ProjectPermissionContext permissionContext,
                                  ContractChangeFulfillmentContext fulfillmentContext,
                                  ProjectContext projectContext) {
        this.contractRepository = contractRepository;
        this.changeRepository = changeRepository;
        this.lineRepository = lineRepository;
        this.receivableRepository = receivableRepository;
        this.projectRepository = projectRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.propertyRepository = propertyRepository;
        this.propertyContext = propertyContext;
        this.financeAccessContext = financeAccessContext;
        this.permissionContext = permissionContext;
        this.fulfillmentContext = fulfillmentContext;
        this.projectContext = projectContext;
    }

    /** 客户确认报价后先创建合同草稿/待签，不创建项目。 */
    @Transactional
    public ProjectContract createFromQuote(Quote quote, Employee employee, LocalDate signedAt,
                                           LocalDate startDate, LocalDate planEndDate, String remark) {
        ProjectContract existing = contractRepository.findByQuote_QuoteId(quote.getQuoteId()).orElse(null);
        if (existing != null) {
            return existing;
        }
        Property property = quote.getProperty() == null
                ? propertyContext.ensureForCustomer(quote.getCustomer(), quote.getCreatedBy())
                : quote.getProperty();
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
            contract.setStatus(signedAt == null ? ContractStatus.PendingSign : ContractStatus.Signed);
            contract.setSignedAt(signedAt);
            contract.setStartDate(startDate);
            contract.setPlanEndDate(planEndDate);
            contract.setCreatedBy(employee);
            contract.setRemark(remark);
            permissionContext.apply(contract, locked, quote.getCustomer());
            ProjectContract saved = contractRepository.save(contract);
            syncLines(saved, quoteLineRepository.findByQuote(quote));
            propertyContext.updateStatus(locked, com.th4.edge.decoboon.property.configure.PropertyStatus.Contracted);
            return saved;
        });
    }

    @Transactional
    public ProjectContract markEffective(ProjectContract contract) {
        contract.setStatus(ContractStatus.Effective);
        contract.setEffectiveAt(LocalDateTime.now());
        return contractRepository.save(contract);
    }

    @Transactional
    public ContractChange approve(ContractChange change, Employee approver, boolean approved, String conclusion) {
        change.setStatus(approved ? ContractChangeStatus.Approved : ContractChangeStatus.Rejected);
        change.setApprovedBy(approver);
        change.setConclusion(conclusion);
        change.setApprovedAt(LocalDateTime.now());
        if (approved) {
            ProjectContract contract = change.getContract();
            contract.setStatus(ContractStatus.Changing);
            contractRepository.save(contract);
        }
        return changeRepository.save(change);
    }

    /** 客户确认变更后，更新合同金额和项目预算，并同步应收计划。 */
    @Transactional
    public ContractChange confirmCustomer(ContractChange change, Employee operator) {
        if (change.getStatus() == ContractChangeStatus.CustomerConfirmed
                || Boolean.TRUE.equals(change.getCustomerConfirmed())) {
            return change;
        }
        if (change.getStatus() != ContractChangeStatus.Approved) {
            throw new DefinedException(new DefaultErrorCode("400", "变更单审批通过后才能进行客户确认"));
        }
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
        change.setCustomerConfirmed(true);
        change.setCustomerConfirmedAt(LocalDateTime.now());
        change.setStatus(ContractChangeStatus.CustomerConfirmed);
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

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
