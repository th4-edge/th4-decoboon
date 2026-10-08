package com.th4.edge.decoboon.statistic.service.workbench.sign.page.support;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.model.SignWorkbenchItem;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 签约工作台实现，按角色数据范围区分待签约与已签约生效。 */
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
    public List<SignWorkbenchItem> call(@User Employee employee) {
        Employee current = tenantContext.requireEmployee(employee);
        Long comId = current.getCompany().getComId();
        Long empId = current.getEmpId();

        List<QuoteStatus> statuses = List.of(QuoteStatus.Accepted, QuoteStatus.Converted);
        List<Quote> quotes = switch (current.getRole()) {
            case Boss -> quoteRepository.findByCompany_ComIdAndStatusInOrderByQuoteIdDesc(comId, statuses);
            case StoreManager -> current.getStore() == null || current.getStore().getStoreId() == null
                    ? List.of()
                    : quoteRepository.findByCompany_ComIdAndCustomer_Store_StoreIdAndStatusInOrderByQuoteIdDesc(
                            comId, current.getStore().getStoreId(), statuses);
            case TeamLeader -> current.getTeam() == null || current.getTeam().getTeamId() == null
                    ? List.of()
                    : quoteRepository.findByCompany_ComIdAndCustomer_Team_TeamIdAndStatusInOrderByQuoteIdDesc(
                            comId, current.getTeam().getTeamId(), statuses);
            case Designer, Marketer -> quoteRepository.findByCompany_ComIdAndCreatedBy_EmpIdAndStatusInOrderByQuoteIdDesc(
                    comId, empId, statuses);
            default -> List.of();
        };
        if (quotes.isEmpty()) {
            return List.of();
        }

        Map<Long, ProjectContract> contractByQuote = contractRepository
                .findByQuote_QuoteIdIn(quotes.stream().map(Quote::getQuoteId).toList())
                .stream()
                .collect(Collectors.toMap(contract -> contract.getQuote().getQuoteId(), Function.identity(), (left, right) -> left));

        List<SignWorkbenchItem> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Quote quote : quotes) {
            Customer customer = quote.getCustomer();
            Property property = quote.getProperty();
            if (customer == null || property == null || customer.getStatus() == CustStatus.Dormant) {
                continue;
            }
            ProjectContract contract = contractByQuote.get(quote.getQuoteId());
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
            item.setCustomerStatus(customer.getStatus().name());
            item.setCustomerStatusName(customer.getStatus().getName());
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
            item.setStayDays(Math.max(0, Duration.between(stageStart, now).toDays()));
            items.add(item);
        }

        items.sort(Comparator.comparing(SignWorkbenchItem::getConverted)
                .thenComparing(SignWorkbenchItem::getStayDays, Comparator.reverseOrder())
                .thenComparing(SignWorkbenchItem::getQuoteId, Comparator.reverseOrder()));
        return items;
    }

    private LocalDateTime acceptedAt(Quote quote) {
        if (quote.getAcceptedAt() != null) {
            return quote.getAcceptedAt();
        }
        if (quote.getUpdateTime() != null) {
            return quote.getUpdateTime().toLocalDateTime();
        }
        if (quote.getCreateTime() != null) {
            return quote.getCreateTime().toLocalDateTime();
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
