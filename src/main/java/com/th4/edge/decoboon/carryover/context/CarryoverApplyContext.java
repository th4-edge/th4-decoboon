package com.th4.edge.decoboon.carryover.context;

import com.th4.edge.components.lock.DistributedLock;

import com.th4.edge.decoboon.carryover.configure.CarryoverProjectStatus;
import com.th4.edge.decoboon.carryover.model.CarryoverCostSnapshot;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.decoboon.carryover.repository.CarryoverCostSnapshotRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverProjectRepository;
import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 将已完成确认的承接项目生成正式业务对象。 */
@Note("承接项目落地")
@Component
public class CarryoverApplyContext {

    private final CarryoverProjectRepository itemRepository;
    private final CarryoverCostSnapshotRepository costRepository;
    private final CustomerRepository customerRepository;
    private final PropertyRepository propertyRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectContractRepository contractRepository;
    private final ProjectRepository projectRepository;
    private final ProjectStageRepository stageRepository;
    private final ReceivableRepository receivableRepository;
    private final PayableRepository payableRepository;
    private final ProjectPermissionContext permissionContext;

    private final DistributedLock distributedLock;

    public CarryoverApplyContext(CarryoverProjectRepository itemRepository,
                                 CarryoverCostSnapshotRepository costRepository,
                                 CustomerRepository customerRepository,
                                 PropertyRepository propertyRepository,
                                 QuoteRepository quoteRepository,
                                 ProjectContractRepository contractRepository,
                                 ProjectRepository projectRepository,
                                 ProjectStageRepository stageRepository,
                                 ReceivableRepository receivableRepository,
                                 PayableRepository payableRepository,
                                 ProjectPermissionContext permissionContext,
                DistributedLock distributedLock) {
        this.itemRepository = itemRepository;
        this.costRepository = costRepository;
        this.customerRepository = customerRepository;
        this.propertyRepository = propertyRepository;
        this.quoteRepository = quoteRepository;
        this.contractRepository = contractRepository;
        this.projectRepository = projectRepository;
        this.stageRepository = stageRepository;
        this.receivableRepository = receivableRepository;
        this.payableRepository = payableRepository;
        this.permissionContext = permissionContext;
        this.distributedLock = distributedLock;
    }

    public CarryoverProject apply(CarryoverProject item, Employee current) {
        String lockKey = "carryover:" + current.getCompany().getComId() + ":" + item.getItemId();
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "承接项目正在处理中，请稍后重试"));
        }
        try {
        CarryoverProject locked = itemRepository.findByItemId(item.getItemId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "承接项目不存在或无权访问")));
        if (locked.getStatus() == CarryoverProjectStatus.Applied
                || locked.getStatus() == CarryoverProjectStatus.Locked) {
            return locked;
        }
        if (locked.getStatus() != CarryoverProjectStatus.Confirmed) {
            throw new DefinedException(new DefaultErrorCode("400", "承接项目必须全部确认后才能落地"));
        }
        Customer customer = createCustomer(locked, current);
        Property property = createProperty(locked, customer);
        if (projectRepository.existsByCompany_ComIdAndProperty_PropertyId(
                current.getCompany().getComId(), property.getPropertyId())) {
            throw new DefinedException(new DefaultErrorCode("400", "该房屋已经存在正式项目，不能重复承接"));
        }
        Quote quote = createQuote(locked, customer, property, current);
        ProjectContract contract = createContract(locked, quote, customer, property, current);
        Project project = createProject(locked, quote, contract, customer, property);
        stageRepository.save(createStage(locked, project, property));
        Receivable receivable = createReceivable(locked, contract, project, current);
        Payable payable = createPayable(locked, project, current);
        CarryoverCostSnapshot snapshot = createCostSnapshot(locked, project, current);

        locked.setQuote(quote);
        locked.setContract(contract);
        locked.setProject(project);
        locked.setReceivable(receivable);
        locked.setPayable(payable);
        locked.setStatus(CarryoverProjectStatus.Applied);
        locked.setAppliedAt(LocalDateTime.now());
        itemRepository.save(locked);
        costRepository.save(snapshot);
        customer.setStatus(CustStatus.Serving);
        customerRepository.save(customer);
        property.setStatus(PropertyStatus.Serving);
        propertyRepository.save(property);
        return locked;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    private Customer createCustomer(CarryoverProject item, Employee current) {
        Customer customer = new Customer();
        customer.setCompany(current.getCompany());
        customer.setStore(item.getStore());
        customer.setTeam(item.getTeam());
        customer.setOwner(item.getOwner());
        customer.setDesigner(item.getDesigner());
        customer.setName(item.getCustomerName());
        customer.setTelNo(item.getCustomerPhone());
        customer.setSource(CustomerSource.Other);
        customer.setCommunity(item.getCommunity());
        customer.setAddr(item.getAddress());
        customer.setArea(item.getArea());
        customer.setStatus(CustStatus.Serving);
        customer.setRemark("业务承接，原系统项目：" + safe(item.getLegacyProjectNo()));
        return customerRepository.save(customer);
    }

    private Property createProperty(CarryoverProject item, Customer customer) {
        Property property = new Property();
        property.setCompany(customer.getCompany());
        property.setStore(customer.getStore());
        property.setTeam(customer.getTeam());
        property.setOwner(customer.getOwner());
        property.setCustomer(customer);
        property.setPropertyNo("HP" + System.currentTimeMillis());
        property.setCommunity(item.getCommunity());
        property.setAddress(item.getAddress());
        property.setArea(item.getArea() == null ? BigDecimal.ZERO : item.getArea());
        property.setStatus(PropertyStatus.Serving);
        property.setSalesman(customer.getOwner());
        property.setDesigner(item.getDesigner());
        property.setRemark("业务承接，原系统项目：" + safe(item.getLegacyProjectNo()));
        return propertyRepository.save(property);
    }

    private Quote createQuote(CarryoverProject item, Customer customer, Property property, Employee current) {
        Quote quote = new Quote();
        quote.setNo("CQ" + System.currentTimeMillis());
        quote.setTitle(item.getProjectName() + " · 接管基线");
        quote.setCompany(current.getCompany());
        quote.setStore(item.getStore());
        quote.setTeam(item.getTeam());
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(current);
        quote.setVersion(1);
        quote.setMode(QuoteMode.Custom);
        quote.setArea(item.getArea() == null ? BigDecimal.ZERO : item.getArea());
        BigDecimal amount = value(item.getCurrentAmount());
        quote.setSubtotal(amount);
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setTaxRate(BigDecimal.ZERO);
        quote.setAmount(amount);
        quote.setCost(value(item.getCostTotal()));
        quote.setCostComplete(false);
        quote.setDesignFee(BigDecimal.ZERO);
        quote.setManagementFee(BigDecimal.ZERO);
        quote.setStatus(QuoteStatus.Converted);
        quote.setAcceptedAt(LocalDateTime.now());
        quote.setConvertedAt(LocalDateTime.now());
        quote.setBusinessOrigin(BusinessOrigin.Carryover);
        return quoteRepository.save(quote);
    }

    private ProjectContract createContract(CarryoverProject item, Quote quote, Customer customer, Property property,
                                           Employee current) {
        ProjectContract contract = new ProjectContract();
        contract.setCompany(current.getCompany());
        contract.setStore(item.getStore());
        contract.setTeam(item.getTeam());
        contract.setOwner(item.getOwner());
        contract.setQuote(quote);
        contract.setCustomer(customer);
        contract.setProperty(property);
        contract.setNo("CC" + System.currentTimeMillis());
        contract.setBaselineAmount(value(item.getSignedAmount()).signum() > 0
                ? item.getSignedAmount() : value(item.getCurrentAmount()));
        contract.setSignedAmount(item.getSignedAmount());
        contract.setCurrentAmount(value(item.getCurrentAmount()));
        contract.setStatus(com.th4.edge.decoboon.project.configure.ContractStatus.Effective);
        contract.setSignedAt(item.getSignedAt());
        contract.setStartDate(item.getStartDate());
        contract.setPlanEndDate(item.getPlanEndDate());
        contract.setEffectiveAt(LocalDateTime.now());
        contract.setPaymentTerms("接管项目合同基线");
        contract.setRemark("原合同编号：" + safe(item.getContractNo()));
        contract.setCreatedBy(current);
        contract.setBusinessOrigin(BusinessOrigin.Carryover);
        return contractRepository.save(contract);
    }

    private Project createProject(CarryoverProject item, Quote quote, ProjectContract contract,
                                  Customer customer, Property property) {
        Project project = new Project();
        project.setNo("P" + System.currentTimeMillis());
        project.setName(item.getProjectName() == null || item.getProjectName().isBlank()
                ? customer.getName() + " · " + property.getAddress() : item.getProjectName());
        project.setCompany(customer.getCompany());
        project.setStore(customer.getStore());
        project.setTeam(customer.getTeam());
        project.setOwner(customer.getOwner());
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quote);
        project.setDesigner(item.getDesigner());
        project.setManager(item.getManager());
        project.setSiteAddr(property.getAddress());
        project.setArea(item.getArea() == null ? property.getArea() : item.getArea());
        project.setContractAmount(value(item.getCurrentAmount()));
        project.setCost(BigDecimal.ZERO);
        project.setStatus(item.getProjectStatus() == null ? ProjectStatus.Ongoing : item.getProjectStatus());
        project.setProgress(item.getProgress() == null ? 0 : item.getProgress());
        project.setBaselineProgress(item.getProgress() == null ? 0 : item.getProgress());
        project.setStartDate(item.getStartDate());
        project.setPlanEndDate(item.getPlanEndDate());
        project.setBusinessOrigin(BusinessOrigin.Carryover);
        Project saved = projectRepository.save(project);
        contract.setProject(saved);
        contractRepository.save(contract);
        return saved;
    }

    private ProjectStage createStage(CarryoverProject item, Project project, Property property) {
        ProjectStage stage = new ProjectStage();
        stage.setCompany(project.getCompany());
        stage.setStore(project.getStore());
        stage.setTeam(project.getTeam());
        stage.setOwner(project.getOwner());
        stage.setProject(project);
        stage.setProperty(property);
        stage.setName(item.getCurrentStageName());
        stage.setStageCode("CARRYOVER_BASELINE");
        stage.setSort(0);
        stage.setWeight(BigDecimal.ONE);
        stage.setStatus(stageStatus(item.getProjectStatus()));
        stage.setProgress(item.getProgress() == null ? 0 : item.getProgress());
        stage.setBaselineProgress(item.getProgress() == null ? 0 : item.getProgress());
        stage.setPlannedStart(item.getStartDate());
        stage.setPlannedEnd(item.getPlanEndDate());
        return stage;
    }

    private Receivable createReceivable(CarryoverProject item, ProjectContract contract, Project project,
                                        Employee current) {
        if (value(item.getReceivableOutstanding()).signum() <= 0) return null;
        Receivable receivable = new Receivable();
        receivable.setCompany(current.getCompany());
        receivable.setStore(project.getStore());
        receivable.setTeam(project.getTeam());
        receivable.setOwner(project.getOwner());
        receivable.setProject(project);
        receivable.setContract(contract);
        receivable.setProperty(project.getProperty());
        receivable.setCustomer(project.getCustomer());
        receivable.setName("期初未收工程款");
        receivable.setType(ReceivableType.Other);
        receivable.setAmount(item.getReceivableOutstanding());
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setDueDate(item.getReceivableDueDate() == null ? item.getBatch().getAsOfDate() : item.getReceivableDueDate());
        receivable.setStatus(ReceivableStatus.Pending);
        receivable.setInvoiceRequired(false);
        receivable.setRemark("业务承接原单号：" + safe(item.getReceivableSourceNo()));
        receivable.setCreatedBy(current);
        receivable.setBusinessOrigin(BusinessOrigin.Carryover);
        return receivableRepository.save(receivable);
    }

    private Payable createPayable(CarryoverProject item, Project project, Employee current) {
        if (value(item.getPayableOutstanding()).signum() <= 0) return null;
        Payable payable = new Payable();
        payable.setCompany(current.getCompany());
        payable.setStore(project.getStore());
        payable.setTeam(project.getTeam());
        payable.setOwner(project.getOwner());
        payable.setProject(project);
        payable.setProperty(project.getProperty());
        payable.setName("期初未付供应商款");
        payable.setType(PayableType.Supplier);
        payable.setPayeeName(item.getPayablePayeeName());
        payable.setAmount(item.getPayableOutstanding());
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setDueDate(item.getPayableDueDate() == null ? item.getBatch().getAsOfDate() : item.getPayableDueDate());
        payable.setStatus(PayableStatus.Pending);
        payable.setUrgent(false);
        payable.setSourceNo(item.getPayableSourceNo());
        payable.setRemark("业务承接期初应付");
        payable.setCreatedBy(current);
        payable.setBusinessOrigin(BusinessOrigin.Carryover);
        return payableRepository.save(payable);
    }

    private CarryoverCostSnapshot createCostSnapshot(CarryoverProject item, Project project, Employee current) {
        CarryoverCostSnapshot snapshot = new CarryoverCostSnapshot();
        snapshot.setCompany(current.getCompany());
        snapshot.setItem(item);
        snapshot.setProject(project);
        snapshot.setTotalCost(item.getCostTotal());
        snapshot.setCostStatus(item.getCostStatus());
        snapshot.setAsOfDate(item.getBatch().getAsOfDate());
        snapshot.setMissingNote(item.getCostMissingNote());
        snapshot.setConfirmedBy(current);
        snapshot.setConfirmedAt(LocalDateTime.now());
        return snapshot;
    }

    private StageStatus stageStatus(ProjectStatus status) {
        if (status == ProjectStatus.Accepting) return StageStatus.Accepting;
        if (status == ProjectStatus.Finished || status == ProjectStatus.Settled) return StageStatus.Done;
        if (status == ProjectStatus.Preparing) return StageStatus.Todo;
        return StageStatus.Doing;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
