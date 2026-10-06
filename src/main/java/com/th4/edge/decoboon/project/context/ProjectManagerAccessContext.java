package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目经理项目关联访问上下文。
 *
 * <p>项目经理不是客户或报价的所有者，不能套用 Self 数据权限；其可访问对象由
 * “本人负责的项目”反向确定。本上下文统一承载该关系判断，防止各 Provider 自行放宽范围。</p>
 */
@Component
public class ProjectManagerAccessContext {

    private final TenantContext tenantContext;
    private final ProjectRepository projectRepository;

    public ProjectManagerAccessContext(TenantContext tenantContext,
                                       ProjectRepository projectRepository) {
        this.tenantContext = tenantContext;
        this.projectRepository = projectRepository;
    }

    /** 项目经理内部能力；BOSS 依 DEC-032 / BR-028 自动具备。 */
    public Employee requireManager(Employee principal) {
        return tenantContext.requireInternalRole(principal, EmployeeRole.Pm);
    }

    /** 项目经理本人负责的项目；BOSS 返回本企业全部项目。 */
    public List<Project> managedProjects(Employee principal) {
        Employee current = requireManager(principal);
        if (current.getCompany() == null || current.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        if (current.getRole() == EmployeeRole.Boss) {
            return projectRepository.findByCompany_ComIdOrderByProjectIdDesc(current.getCompany().getComId());
        }
        return projectRepository.findByCompany_ComIdAndManager_EmpIdOrderByProjectIdDesc(
                current.getCompany().getComId(), current.getEmpId());
    }

    /** 当前项目经理负责项目关联的去重客户。 */
    public List<Customer> managedCustomers(Employee principal) {
        Map<Long, Customer> result = new LinkedHashMap<>();
        for (Project project : managedProjects(principal)) {
            Customer customer = project.getCustomer();
            if (customer != null && customer.getCustId() != null) {
                result.put(customer.getCustId(), customer);
            }
        }
        return List.copyOf(result.values());
    }

    /** 仅允许访问本人负责项目关联的客户。 */
    public Customer requireCustomer(Long customerId, Employee principal) {
        if (customerId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "客户索引不能为空"));
        }
        return managedCustomers(principal).stream()
                .filter(customer -> customerId.equals(customer.getCustId()))
                .findFirst()
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "客户不存在或不属于当前项目经理负责的项目")));
    }

    /** 当前项目经理负责项目的去重来源报价。 */
    public List<Quote> managedQuotes(Employee principal) {
        Map<Long, Quote> result = new LinkedHashMap<>();
        for (Project project : managedProjects(principal)) {
            Quote quote = project.getQuote();
            if (quote != null && quote.getQuoteId() != null) {
                result.put(quote.getQuoteId(), quote);
            }
        }
        return List.copyOf(result.values());
    }

    /** 仅允许访问本人负责项目的来源报价。 */
    public Quote requireQuote(Long quoteId, Employee principal) {
        if (quoteId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "报价索引不能为空"));
        }
        return managedQuotes(principal).stream()
                .filter(quote -> quoteId.equals(quote.getQuoteId()))
                .findFirst()
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "报价不存在或不属于当前项目经理负责的项目")));
    }
}
