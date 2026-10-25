package com.th4.edge.decoboon.statistic.service.workbench.sign.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.context.DesignerWorkbenchRisk;
import com.th4.edge.decoboon.statistic.model.SignWorkbenchItem;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 签约工作台：SQL 先过滤并分页，页面只装配当前页报价、合同和项目。 */
@Note("签约工作台")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "version", "amount", "status", "acceptedAt", "convertedAt", "customer", "property"}),
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "status", "signedAt", "effectiveAt", "project"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "name", "no", "status"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "status", "designer"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "address", "area", "designer"}),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name", "store"})
})
@Service("statistic/workbench/sign/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.statistic.service.workbench.sign.page.Page {

    private static final Set<ContractStatus> SIGNED_CONTRACT_STATUSES = EnumSet.of(
            ContractStatus.Signed, ContractStatus.Effective, ContractStatus.Active, ContractStatus.Changing,
            ContractStatus.Changed, ContractStatus.Completed, ContractStatus.Settled, ContractStatus.Closed,
            ContractStatus.Terminated);
    private static final Set<String> VALID_FILTERS = Set.of(
            "all", "toCreate", "waitingConfirm", "effective", "pending", "waiting", "fulfilling", "done");

    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private String keyword;
    /** all / toCreate / waitingConfirm / effective / pending / fulfilling / done */
    @AutoInject
    private String filter;
    /** 深链目标报价；存在时只返回该报价。 */
    @AutoInject
    private Long quoteId;

    private final TenantContext tenantContext;
    private final QuoteRepository quoteRepository;
    private final ProjectContractRepository contractRepository;

    public Page(TenantContext tenantContext,
                QuoteRepository quoteRepository,
                ProjectContractRepository contractRepository) {
        this.tenantContext = tenantContext;
        this.quoteRepository = quoteRepository;
        this.contractRepository = contractRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<SignWorkbenchItem> call(@User Employee employee) {
        Employee operator = tenantContext.requireRole(employee,
                EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.TeamLeader,
                EmployeeRole.Designer, EmployeeRole.Marketer);
        Long comId = operator.getCompany().getComId();
        boolean allCompany = operator.getRole() == EmployeeRole.Boss;
        Long storeId = operator.getRole() == EmployeeRole.StoreManager && operator.getStore() != null
                ? operator.getStore().getStoreId() : null;
        Long teamId = operator.getRole() == EmployeeRole.TeamLeader && operator.getTeam() != null
                ? operator.getTeam().getTeamId() : null;
        Long creatorId = operator.getRole() == EmployeeRole.Designer
                || operator.getRole() == EmployeeRole.Marketer ? operator.getEmpId() : null;
        int pageIndex = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        PageRequest pageable = PageRequest.of(pageIndex, pageSize);

        String normalizedFilter = normalizeFilter(filter);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String pattern = "%" + normalizedKeyword + "%";
        var idPage = quoteRepository.findWorkbenchSignQuoteIds(
                comId, allCompany, storeId, teamId, creatorId,
                normalizedFilter, quoteId, normalizedKeyword, pattern, pageable);
        if (idPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }

        Map<Long, Quote> quoteById = new LinkedHashMap<>();
        quoteRepository.findSignWorkbenchQuotes(idPage.getContent())
                .forEach(quote -> quoteById.put(quote.getQuoteId(), quote));
        Map<Long, ProjectContract> contractByQuote = contractRepository
                .findWorkbenchByQuoteIds(idPage.getContent())
                .stream()
                .collect(Collectors.toMap(contract -> contract.getQuote().getQuoteId(), Function.identity(), (left, right) -> left));

        LocalDateTime now = LocalDateTime.now();
        List<SignWorkbenchItem> items = new ArrayList<>(idPage.getNumberOfElements());
        for (Long id : idPage.getContent()) {
            Quote quote = quoteById.get(id);
            if (quote != null) items.add(item(quote, contractByQuote.get(id), now));
        }
        return new PageImpl<>(items, pageable, idPage.getTotalElements());
    }

    private SignWorkbenchItem item(Quote quote, ProjectContract contract, LocalDateTime now) {
        Customer customer = quote.getCustomer();
        Property property = quote.getProperty();
        Project project = contract == null ? null : contract.getProject();
        boolean converted = quote.getStatus() == QuoteStatus.Converted
                || project != null
                || (contract != null && SIGNED_CONTRACT_STATUSES.contains(contract.getStatus()));

        SignWorkbenchItem item = new SignWorkbenchItem();
        item.setCustId(customer.getCustId());
        item.setPropertyId(property.getPropertyId());
        item.setCustomerName(customer.getName());
        item.setTelNo(customer.getTelNo());
        item.setCommunity(firstNonBlank(property.getCommunity(), customer.getCommunity()));
        item.setAddr(firstNonBlank(property.getAddress(), customer.getAddr()));
        item.setArea(property.getArea() == null ? customer.getArea() : property.getArea());
        item.setCustomerStatus(customer.getStatus() == null ? null : customer.getStatus().name());
        item.setCustomerStatusName(customer.getStatus() == null ? null : customer.getStatus().getName());
        var designer = property.getDesigner() != null
                ? property.getDesigner()
                : customer.getDesigner() != null
                ? customer.getDesigner()
                : quote.getDesign() == null ? null : quote.getDesign().getOwner();
        if (designer != null) {
            item.setDesignerId(designer.getEmpId());
            item.setDesignerName(designer.getName());
            item.setDesignerStoreName(designer.getStore() == null ? null : designer.getStore().getName());
        }

        item.setQuoteId(quote.getQuoteId());
        item.setQuoteNo(quote.getNo());
        item.setVersion(quote.getVersion());
        item.setAcceptedAmount(quote.getAmount());
        item.setAcceptedAt(acceptedAt(quote));
        item.setConverted(converted);
        if (contract != null) {
            item.setContractId(contract.getContractId());
            item.setContractNo(contract.getNo());
            item.setContractStatus(contract.getStatus().name());
            item.setContractStatusName(contract.getStatus().getName());
            item.setSignedAt(contract.getSignedAt());
            item.setCurrentAmount(contract.getCurrentAmount());
        }
        if (project != null) {
            item.setProjectId(project.getProjectId());
            item.setProjectName(project.getName());
            item.setProjectStatus(project.getStatus() == null ? null : project.getStatus().name());
            item.setProjectStatusName(project.getStatus() == null ? null : project.getStatus().getName());
        }

        LocalDateTime stageStart = item.getAcceptedAt() == null ? now : item.getAcceptedAt();
        long stayDays = Math.max(0, Duration.between(stageStart, now).toDays());
        item.setStayDays(stayDays);
        DesignerWorkbenchRisk.SignDecision risk = DesignerWorkbenchRisk.sign(
                Boolean.TRUE.equals(item.getConverted()), item.getContractStatus(), item.getProjectId() != null, stayDays);
        item.setRiskLevel(risk.riskLevel());
        item.setRiskReason(risk.riskReason());
        item.setNextActionKey(risk.nextActionKey());
        item.setNextActionLabel(risk.nextActionLabel());
        return item;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) return "all";
        String normalized = value.trim();
        if (!VALID_FILTERS.contains(normalized)) {
            throw new DefinedException(new DefaultErrorCode("400", "签约筛选条件无效"));
        }
        return normalized;
    }

    private LocalDateTime acceptedAt(Quote quote) {
        if (quote.getAcceptedAt() != null) return quote.getAcceptedAt();
        if (quote.getUpdateTime() != null) return quote.getUpdateTime().toLocalDateTime();
        if (quote.getCreateTime() != null) return quote.getCreateTime().toLocalDateTime();
        return null;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
