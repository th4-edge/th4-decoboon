package com.th4.edge.decoboon.quote.service.quotes.update.support;

import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.context.QuoteContext;
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
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 报价状态推进 Provider。 */
@Note("报价状态推进")
@Description("提交、接受、驳回或转换客户报价")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "mode", "version", "status", "area", "subtotal", "discount",
                        "taxRate", "taxAmount", "amount", "designFee", "managementFee", "submittedAt", "rejectedAt",
                        "acceptedAt", "acceptedBy", "convertedAt", "updateTime", "property"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("quote/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.quote.service.quotes.update.Update {

    @AutoInject
    @NotNull
    private Long quoteId;

    @AutoInject
    @NotNull
    private QuoteStatus status;

    @User
    private Employee employee;

    private final QuoteContext quoteContext;
    private final com.th4.edge.decoboon.quote.repository.QuoteRepository quoteRepository;
    private final DataScopeContext dataScopeContext;

    public Update(QuoteContext quoteContext, DataScopeContext dataScopeContext,
                  com.th4.edge.decoboon.quote.repository.QuoteRepository quoteRepository) {
        this.quoteContext = quoteContext;
        this.dataScopeContext = dataScopeContext;
        this.quoteRepository = quoteRepository;
    }

    @Override
    public Quote call(Quote request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        if (current.getRole() == EmployeeRole.Marketer) {
            throw new DefinedException(new DefaultErrorCode("403", "业务员不参与方案报价状态推进，仅可查看业务流转"));
        }
        DataScope scope = dataScopeContext.resolve(current);
        org.springframework.data.jpa.domain.Specification<Quote> byId =
                (root, query, builder) -> builder.equal(root.get("quoteId"), quoteId);
        Quote quote = quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        if (current.getRole() == EmployeeRole.TeamLeader && status == QuoteStatus.Converted) {
            throw new DefinedException(new DefaultErrorCode("403", "报价转项目需由设计师或企业负责人确认"));
        }
        return switch (status) {
            case Submitted -> quoteContext.submit(quoteId);
            case Accepted -> quoteContext.accept(quoteId, current);
            case Rejected -> quoteContext.reject(quoteId);
            case Converted -> throw new DefinedException(new DefaultErrorCode("400", "请通过合同签署并生效生成项目，不能直接转换报价"));
            case Draft -> quote;
            case Superseded -> throw new DefinedException(new DefaultErrorCode("400", "已被替代的历史报价不能继续推进"));
        };
    }
}
