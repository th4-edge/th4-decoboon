package com.th4.edge.decoboon.quote.service.workbook.build.support;

import com.th4.edge.decoboon.quote.context.QuoteWorkbookFileContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.model.QuoteWorkbookFile;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.ScopeTypes;
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
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

/** 设计师报价工作簿导出。 */
@Note("报价工作簿导出")
@Properties(allow = @AllowProperties(model = QuoteWorkbookFile.class,
        properties = {"fileName", "contentType", "contentBase64", "sha256"}))
@Service("quote/workbook/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.quote.service.workbook.build.Build {

    @AutoInject @NotNull private Long quoteId;
    @User private Employee employee;

    private final QuoteRepository quoteRepository;
    private final QuoteLineRepository lineRepository;
    private final QuoteWorkbookFileContext fileContext;
    private final DataScopeContext dataScopeContext;

    public Build(QuoteRepository quoteRepository, QuoteLineRepository lineRepository,
                 QuoteWorkbookFileContext fileContext, DataScopeContext dataScopeContext) {
        this.quoteRepository = quoteRepository;
        this.lineRepository = lineRepository;
        this.fileContext = fileContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public QuoteWorkbookFile call(Quote request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        if (current.getRole() != EmployeeRole.Designer) {
            throw new DefinedException(new DefaultErrorCode("403", "仅设计师可导出报价工作簿"));
        }
        DataScope scope = dataScopeContext.resolve(current);
        Specification<Quote> byId = (root, query, builder) -> builder.equal(root.get("quoteId"), quoteId);
        Quote quote = quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        if (quote.getCreatedBy() == null || !quote.getCreatedBy().getEmpId().equals(current.getEmpId())) {
            throw new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问"));
        }
        List<QuoteLine> lines = lineRepository.findByQuote(quote);
        return fileContext.export(quote, lines);
    }
}
