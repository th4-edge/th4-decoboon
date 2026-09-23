package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.configure.BusinessEventType;
import com.th4.edge.decoboon.statistic.model.BusinessEvent;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 企业负责人经营动态聚合。 */
@Component
public class BusinessEventContext {

    private static final int SCAN_LIMIT = 200;
    private static final int EVENT_WINDOW_DAYS = 180;

    private final TenantContext tenantContext;
    private final CustomerRepository customerRepository;
    private final CustomerFollowRepository customerFollowRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectContractRepository contractRepository;
    private final ReceiptRepository receiptRepository;
    private final PaymentRepository paymentRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final ContractChangeRepository contractChangeRepository;
    private final ProjectReportRepository reportRepository;

    public BusinessEventContext(TenantContext tenantContext,
                                CustomerRepository customerRepository,
                                CustomerFollowRepository customerFollowRepository,
                                QuoteRepository quoteRepository,
                                ProjectContractRepository contractRepository,
                                ReceiptRepository receiptRepository,
                                PaymentRepository paymentRepository,
                                AcceptanceRepository acceptanceRepository,
                                ContractChangeRepository contractChangeRepository,
                                ProjectReportRepository reportRepository) {
        this.tenantContext = tenantContext;
        this.customerRepository = customerRepository;
        this.customerFollowRepository = customerFollowRepository;
        this.quoteRepository = quoteRepository;
        this.contractRepository = contractRepository;
        this.receiptRepository = receiptRepository;
        this.paymentRepository = paymentRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.contractChangeRepository = contractChangeRepository;
        this.reportRepository = reportRepository;
    }

    /** 构造最近经营事件，事件来源必须是真实领域对象。 */
    public List<BusinessEvent> events(Employee principal, LocalDate asOf, Integer requestedLimit) {
        Employee current = tenantContext.requireBoss(principal);
        Long comId = current.getCompany().getComId();
        LocalDate effectiveAsOf = asOf == null ? LocalDate.now() : asOf;
        LocalDateTime earliest = effectiveAsOf.minusDays(EVENT_WINDOW_DAYS).atStartOfDay();

        List<BusinessEvent> events = new ArrayList<>();
        addCustomerEvents(events, customerRepository.findTop200ByCompany_ComIdOrderByCustIdDesc(comId));
        addFollowEvents(events, customerFollowRepository.findTop200ByCompany_ComIdOrderByFollowIdDesc(comId));
        addQuoteEvents(events, quoteRepository.findTop200ByCompany_ComIdOrderByQuoteIdDesc(comId));
        addContractEvents(events, contractRepository.findTop200ByCompany_ComIdOrderByContractIdDesc(comId));
        addReceiptEvents(events, receiptRepository.findTop200ByCompany_ComIdOrderByReceivedAtDesc(comId));
        addPaymentEvents(events, paymentRepository.findTop200ByCompany_ComIdOrderByPaidAtDesc(comId));
        addAcceptanceEvents(events, acceptanceRepository.findTop200ByCompany_ComIdOrderByAcceptanceIdDesc(comId));
        addContractChangeEvents(events, contractChangeRepository.findTop200ByCompany_ComIdOrderByChangeIdDesc(comId));
        addReportEvents(events, reportRepository.findByCompany_ComIdOrderByCreateTime_DateTimeDesc(
                comId, PageRequest.of(0, SCAN_LIMIT)).getContent());

        Map<String, BusinessEvent> unique = new LinkedHashMap<>();
        events.stream()
                .filter(event -> event.getOccurredAt() != null)
                .filter(event -> !event.getOccurredAt().isBefore(earliest))
                .filter(event -> !event.getOccurredAt().isAfter(effectiveAsOf.plusDays(1).atStartOfDay()))
                .sorted(Comparator.comparing(BusinessEvent::getOccurredAt).reversed()
                        .thenComparing(BusinessEvent::getEventId))
                .forEach(event -> unique.putIfAbsent(event.getEventId(), event));

        int limit = requestedLimit == null ? 6 : Math.max(1, Math.min(requestedLimit, 100));
        return unique.values().stream().limit(limit).toList();
    }

    private void addCustomerEvents(List<BusinessEvent> events, List<Customer> customers) {
        customers.stream().limit(SCAN_LIMIT).forEach(customer -> events.add(event(
                BusinessEventType.CustomerCreated,
                customer.getCustId(),
                created(customer),
                customer.getName() + " 已登记为装修客户",
                null,
                "Customer",
                customer.getCustId(),
                customer,
                null,
                customer.getOwner()
        )));
    }

    private void addFollowEvents(List<BusinessEvent> events, List<CustomerFollow> records) {
        records.stream().limit(SCAN_LIMIT).forEach(follow -> events.add(event(
                BusinessEventType.CustomerFollowed,
                follow.getFollowId(),
                created(follow),
                trim(follow.getContent(), 120),
                null,
                "Customer",
                follow.getCustomer() == null ? null : follow.getCustomer().getCustId(),
                follow.getCustomer(),
                null,
                follow.getEmp()
        )));
    }

    private void addQuoteEvents(List<BusinessEvent> events, List<Quote> quotes) {
        quotes.stream().limit(SCAN_LIMIT).forEach(quote -> {
            addQuoteEvent(events, quote, BusinessEventType.QuoteSubmitted, quote.getSubmittedAt());
            addQuoteEvent(events, quote, BusinessEventType.QuoteAccepted, quote.getAcceptedAt());
            addQuoteEvent(events, quote, BusinessEventType.QuoteRejected, quote.getRejectedAt());
            addQuoteEvent(events, quote, BusinessEventType.QuoteConverted, quote.getConvertedAt());
        });
    }

    private void addQuoteEvent(List<BusinessEvent> events, Quote quote,
                               BusinessEventType type, LocalDateTime occurredAt) {
        if (occurredAt == null) return;
        events.add(event(type, quote.getQuoteId(), occurredAt,
                quote.getNo() + " · " + quote.getTitle(),
                quote.getAmount(), "Quote", quote.getQuoteId(),
                quote.getCustomer(), null, quote.getCreatedBy()));
    }

    private void addContractEvents(List<BusinessEvent> events, List<ProjectContract> contracts) {
        contracts.stream().limit(SCAN_LIMIT).forEach(contract -> {
            if (contract.getSignedAt() != null) {
                events.add(event(BusinessEventType.ContractSigned, contract.getContractId(),
                        contract.getSignedAt().atStartOfDay(), contract.getNo() + " 已完成签约",
                        contractRevenue(contract), "ProjectContract", contract.getContractId(),
                        contract.getCustomer(), contract.getProject(), contract.getCreatedBy()));
            }
            if (contract.getEffectiveAt() != null) {
                events.add(event(BusinessEventType.ContractEffective, contract.getContractId(),
                        contract.getEffectiveAt(), contract.getNo() + " 已生效",
                        contractRevenue(contract), "ProjectContract", contract.getContractId(),
                        contract.getCustomer(), contract.getProject(), contract.getCreatedBy()));
            }
        });
    }

    private void addReceiptEvents(List<BusinessEvent> events, List<Receipt> receipts) {
        receipts.stream().limit(SCAN_LIMIT).forEach(receipt -> events.add(event(
                BusinessEventType.ReceiptReceived,
                receipt.getReceiptId(),
                receipt.getReceivedAt() == null ? null : receipt.getReceivedAt().atStartOfDay(),
                receipt.getPayerName() == null ? "客户工程款已到账" : receipt.getPayerName() + " 工程款已到账",
                receipt.getAmount(),
                "Receipt",
                receipt.getReceiptId(),
                receipt.getCustomer(),
                receipt.getProject(),
                receipt.getCreatedBy()
        )));
    }

    private void addPaymentEvents(List<BusinessEvent> events, List<Payment> payments) {
        payments.stream().limit(SCAN_LIMIT).forEach(payment -> events.add(event(
                BusinessEventType.PaymentPaid,
                payment.getPaymentId(),
                payment.getPaidAt() == null ? null : payment.getPaidAt().atStartOfDay(),
                payment.getPayeeName() == null ? "项目付款已登记" : "付款给 " + payment.getPayeeName(),
                payment.getAmount(),
                "Payment",
                payment.getPaymentId(),
                null,
                payment.getProject(),
                payment.getCreatedBy()
        )));
    }

    private void addAcceptanceEvents(List<BusinessEvent> events, List<Acceptance> acceptances) {
        acceptances.stream().limit(SCAN_LIMIT).forEach(acceptance -> {
            LocalDateTime requestedAt = created(acceptance);
            events.add(event(BusinessEventType.AcceptanceRequested, acceptance.getAcceptanceId(), requestedAt,
                    "阶段验收已提交", null, "Acceptance", acceptance.getAcceptanceId(),
                    null, acceptance.getProject(), acceptance.getSubmittedBy()));
            if (acceptance.getConfirmedAt() != null) {
                BusinessEventType type = acceptance.getStatus() == AcceptStatus.Rejected
                        ? BusinessEventType.AcceptanceRejected : BusinessEventType.AcceptanceCompleted;
                events.add(event(type, acceptance.getAcceptanceId(), acceptance.getConfirmedAt(),
                        acceptance.getConclusion() == null || acceptance.getConclusion().isBlank()
                                ? type.getName() : trim(acceptance.getConclusion(), 120),
                        null, "Acceptance", acceptance.getAcceptanceId(), null,
                        acceptance.getProject(), acceptance.getSubmittedBy()));
            }
        });
    }

    private void addContractChangeEvents(List<BusinessEvent> events, List<ContractChange> changes) {
        changes.stream().limit(SCAN_LIMIT).forEach(change -> {
            BusinessEventType type = switch (change.getStatus()) {
                case Approved, CustomerConfirmed -> BusinessEventType.ContractChangeApproved;
                case Rejected -> BusinessEventType.ContractChangeRejected;
                default -> BusinessEventType.ContractChangeSubmitted;
            };
            LocalDateTime occurredAt = change.getApprovedAt() != null ? change.getApprovedAt()
                    : change.getCustomerConfirmedAt() != null ? change.getCustomerConfirmedAt() : created(change);
            events.add(event(type, change.getChangeId(), occurredAt,
                    change.getNo() + " · " + (change.getReason() == null ? type.getName() : trim(change.getReason(), 100)),
                    change.getAmount(), "ContractChange", change.getChangeId(),
                    null, change.getProject(), change.getRequestedBy()));
        });
    }

    private void addReportEvents(List<BusinessEvent> events, List<ProjectReport> reports) {
        reports.forEach(report -> {
            BusinessEventType type = reportType(report.getType());
            if (type == null || report.getProject() == null || report.getChange() != null) return;
            events.add(event(type, report.getReportId(), created(report),
                    report.getTitle() == null || report.getTitle().isBlank()
                            ? type.getName() : report.getTitle(),
                    null, "Project", report.getProject().getProjectId(), null,
                    report.getProject(), report.getAuthor()));
        });
    }

    private BusinessEventType reportType(ReportType type) {
        if (type == null) return null;
        return switch (type) {
            case StageOpen -> BusinessEventType.StageStarted;
            case TaskDone -> BusinessEventType.TaskCompleted;
            case Progress -> BusinessEventType.ProjectProgress;
            case AcceptRequest, AcceptDone -> null;
            case Issue -> BusinessEventType.IssueReported;
            case Material -> BusinessEventType.MaterialArrived;
            case Notice -> BusinessEventType.ProjectNotice;
            case Checkin -> null;
        };
    }

    private BusinessEvent event(BusinessEventType type,
                                Long sourceId,
                                LocalDateTime occurredAt,
                                String content,
                                BigDecimal amount,
                                String businessType,
                                Long businessId,
                                Customer customer,
                                Project projectForTarget,
                                Employee owner) {
        BusinessEvent event = new BusinessEvent();
        event.setEventId(type.name() + ":" + sourceId + ":" + occurredAt);
        event.setCategory(type.getCategory());
        event.setType(type);
        event.setLevel(type.getLevel());
        event.setTitle(type.getName());
        event.setContent(content == null ? "" : content);
        event.setOccurredAt(occurredAt);
        event.setAmount(amount);
        event.setBusinessType(businessType);
        event.setBusinessId(businessId);
        event.setProjectId(projectForTarget == null ? null : projectForTarget.getProjectId());
        event.setProjectName(projectForTarget == null ? null : projectForTarget.getName());
        event.setCustomerId(customer == null ? null : customer.getCustId());
        event.setCustomerName(customer == null ? null : customer.getName());
        event.setOwnerName(owner == null ? null : owner.getName());
        return event;
    }

    private BigDecimal contractRevenue(ProjectContract contract) {
        BigDecimal current = contract.getCurrentAmount();
        return current == null || current.signum() == 0
                ? (contract.getBaselineAmount() == null ? BigDecimal.ZERO : contract.getBaselineAmount()) : current;
    }

    private LocalDateTime created(AuditModel model) {
        return model.getCreateTime() == null ? null : model.getCreateTime().toLocalDateTime();
    }

    private String trim(String value, int max) {
        if (value == null) return "";
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max) + "…";
    }
}
