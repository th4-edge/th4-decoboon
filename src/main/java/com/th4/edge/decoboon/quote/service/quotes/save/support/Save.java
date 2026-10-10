package com.th4.edge.decoboon.quote.service.quotes.save.support;

import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.quote.model.QuoteGroup;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.quote.context.QuoteContext;
import com.th4.edge.decoboon.quote.context.QuoteCopyContext;
import com.th4.edge.decoboon.quote.context.HybridQuoteBuilder;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 报价创建 Provider。
 *
 * <p>前端只提交客户、套餐、面积和负责人标识，金额与成本由 QuoteContext 计算，
 * 防止客户端篡改报价金额。</p>
 */
@Note("报价创建")
@Description("根据 BIM 套餐和客户面积创建报价快照")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "customer", "design", "packageTemplate", "createdBy", "version", "mode", "area",
                        "subtotal", "amount", "designFee", "managementFee", "discount", "taxRate", "taxAmount", "status",
                        "submittedAt", "rejectedAt", "acceptedAt", "convertedAt",
                        "items", "regionGroups", "constructionGroups", "materialGroups", "otherGroups", "createTime", "property"}),
        @AllowProperties(model = QuoteGroup.class,
                properties = {"key", "name", "type", "category", "unit", "quantity", "amount", "items"}),
        @AllowProperties(model = QuoteLine.class,
                properties = {"itemId", "space", "type", "category", "name", "brand", "spec", "unit", "quantity",
                        "materialPrice", "manualPrice", "pricingMode", "price", "amount", "formula", "role",
                        "differenceType", "differenceStatus", "included", "discountAmount", "components", "sort"}),
        @AllowProperties(model = com.th4.edge.decoboon.quote.model.QuoteComponent.class,
                properties = {"type", "name", "saleAmount", "formula", "sort"}),
        @AllowProperties(model = PackageTemplate.class, properties = {"templateId", "name", "area", "price", "discount"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "style", "status", "area", "layout", "coverUrl"}),
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "type", "name", "length", "width", "height", "area", "perimeter", "areaSource"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("quote/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.quote.service.quotes.save.Save {

    /** 框架注入：当前登录员工。 */
    @User
    private Employee employee;

    @AutoInject
    private Long customerId;

    @AutoInject
    private Long propertyId;

    @AutoInject
    private Long templateId;

    @AutoInject
    private Long designId;

    @AutoInject
    private Long sourceQuoteId;

    @AutoInject
    private BigDecimal area;

    @AutoInject
    private BigDecimal discount;

    @AutoInject
    private Long empId;

    private final CustomerRepository customerRepository;
    private final PackageTemplateRepository giftPackRepository;
    private final EmployeeRepository employeeRepository;
    private final QuoteContext quoteContext;
    private final QuoteCopyContext quoteCopyContext;
    private final HybridQuoteBuilder hybridQuoteBuilder;
    private final DataScopeContext dataScopeContext;
    private final DesignContext designContext;
    private final PropertyContext propertyContext;

    public Save(CustomerRepository customerRepository,
                PackageTemplateRepository giftPackRepository,
                EmployeeRepository employeeRepository,
                QuoteContext quoteContext,
                QuoteCopyContext quoteCopyContext,
                HybridQuoteBuilder hybridQuoteBuilder,
                DataScopeContext dataScopeContext,
                DesignContext designContext,
                PropertyContext propertyContext) {
        this.customerRepository = customerRepository;
        this.giftPackRepository = giftPackRepository;
        this.employeeRepository = employeeRepository;
        this.quoteContext = quoteContext;
        this.quoteCopyContext = quoteCopyContext;
        this.hybridQuoteBuilder = hybridQuoteBuilder;
        this.dataScopeContext = dataScopeContext;
        this.designContext = designContext;
        this.propertyContext = propertyContext;
    }

    @Override
    public Quote call(Quote request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        if (current.getRole() == EmployeeRole.Marketer) {
            throw new DefinedException(new DefaultErrorCode("403", "业务员不参与方案报价，客户交接后仅可查看业务流转"));
        }
        DataScope scope = dataScopeContext.resolve(current);
        Long companyId = scope.comId();

        if (sourceQuoteId != null) {
            return quoteCopyContext.rebase(sourceQuoteId, employee);
        }

        if (designId != null) {
            Design design = designContext.requireDesign(designId, employee);
            if (customerId != null && !customerId.equals(design.getCustomer().getCustId())) {
                throw new DefinedException(new DefaultErrorCode("403", "方案与客户不匹配"));
            }
            if (templateId != null) {
                PackageTemplate packageTemplate = giftPackRepository.findById(templateId)
                        .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "套餐不存在")));
                if (packageTemplate.getCompany() == null || !packageTemplate.getCompany().getComId().equals(companyId)) {
                    throw new DefinedException(new DefaultErrorCode("403", "套餐不属于当前企业"));
                }
                return hybridQuoteBuilder.build(design, packageTemplate, discount);
            }
            return quoteContext.create(design, discount);
        }
        if (templateId == null || customerId == null || area == null) {
            throw new DefinedException(new DefaultErrorCode("400", "设计报价或套餐报价参数不完整"));
        }

        Specification<Customer> byId = (root, query, builder) ->
                builder.equal(root.get("custId"), customerId);
        Customer customer = customerRepository.findOne(
                        byId.and(DataScopeSpecification.visible(Customer.class, scope)))
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "客户不存在或无权访问")));
        PackageTemplate packageTemplate = giftPackRepository.findById(templateId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "套餐不存在")));
        if (packageTemplate.getCompany() == null || !packageTemplate.getCompany().getComId().equals(companyId)) {
            throw new DefinedException(new DefaultErrorCode("403", "客户或套餐不属于当前企业"));
        }

        Employee quoteEmployee = current;
        if (current.getRole() == EmployeeRole.Boss && empId != null) {
            quoteEmployee = employeeRepository.findByEmpIdAndCompany_ComId(empId, companyId);
            if (quoteEmployee == null) {
                throw new DefinedException(new DefaultErrorCode("400", "报价负责人不属于当前企业"));
            }
        }
        Property property = propertyContext.resolve(propertyId, customer, current);
        return quoteContext.create(customer, packageTemplate, property, area, discount, quoteEmployee);
    }
}
