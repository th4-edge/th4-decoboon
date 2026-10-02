package com.th4.edge.decoboon.quote.service.workbook.importer.support;

import com.th4.edge.decoboon.quote.context.QuoteWorkbookImportContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.model.QuoteWorkbookImportRequest;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.quote.service.workbook.importer.ImportResult;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Note("报价工作簿导入预览")
@Properties(allow = {
        @AllowProperties(model = ImportResult.class, properties = {"total", "valid", "failed", "rows"}),
        @AllowProperties(model = ImportResult.Row.class,
                properties = {"excelRow", "itemId", "region", "name", "quantity", "quantityFormula",
                        "materialPrice", "manualPrice", "importable", "errors"})
})
@Service("quote/workbook/import")
@Scope(ScopeTypes.PROTOTYPE)
public class Import implements com.th4.edge.decoboon.quote.service.workbook.importer.Import {

    @AutoInject private Long quoteId;
    @AutoInject private String fileName;
    @AutoInject private String contentBase64;

    @User private Employee employee;

    private final QuoteRepository quoteRepository;
    private final QuoteLineRepository lineRepository;
    private final QuoteWorkbookImportContext importContext;
    private final DataScopeContext dataScopeContext;

    public Import(QuoteRepository quoteRepository, QuoteLineRepository lineRepository,
                  QuoteWorkbookImportContext importContext, DataScopeContext dataScopeContext) {
        this.quoteRepository = quoteRepository;
        this.lineRepository = lineRepository;
        this.importContext = importContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public ImportResult call(@ModelSaveInject QuoteWorkbookImportRequest request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        if (current.getRole() != EmployeeRole.Designer) {
            throw new DefinedException(new DefaultErrorCode("403", "仅设计师可导入报价工作簿"));
        }
        Long requestQuoteId = request == null ? quoteId : request.getQuoteId();
        if (requestQuoteId == null) throw new DefinedException(new DefaultErrorCode("400", "缺少报价编号"));
        DataScope scope = dataScopeContext.resolve(current);
        Specification<Quote> byId = (root, query, builder) -> builder.equal(root.get("quoteId"), requestQuoteId);
        Quote quote = quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        if (quote.getCreatedBy() == null || !quote.getCreatedBy().getEmpId().equals(current.getEmpId())) {
            throw new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问"));
        }
        if (!com.th4.edge.decoboon.quote.configure.QuoteStatus.Draft.equals(quote.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "只有草稿报价可以导入工作簿"));
        }
        List<QuoteLine> lines = lineRepository.findByQuote(quote);
        QuoteWorkbookImportRequest parsed = request == null ? new QuoteWorkbookImportRequest() : request;
        if (parsed.getQuoteId() == null) parsed.setQuoteId(quoteId);
        if (parsed.getFileName() == null) parsed.setFileName(fileName);
        if (parsed.getContentBase64() == null) parsed.setContentBase64(contentBase64);
        return importContext.parse(parsed, lines);
    }
}
