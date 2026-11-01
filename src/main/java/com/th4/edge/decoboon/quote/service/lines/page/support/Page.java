package com.th4.edge.decoboon.quote.service.lines.page.support;

import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.quote.configure.QuoteLineRole;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
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
import com.th4.edge.framework.service.annotation.ServiceName;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 报价明细分页实现；同时服务报价行选择器的轻量投影。 */
@Note("报价明细分页")
@Properties(allow = {
        @AllowProperties(model = QuoteLine.class,
                properties = {"itemId", "catalogSourceId", "catalogSourceCode", "catalogSourceRevision", "space", "type", "category", "packageSectionNameSnapshot", "name", "brand", "spec", "unit", "quantity",
                        "materialPrice", "manualPrice", "pricingMode", "price", "amount", "cost", "formula", "quantityFormula", "role",
                        "differenceType", "differenceStatus", "included", "discountAmount", "craft", "sort"}),
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "type", "name", "length", "width", "height", "area", "perimeter", "areaSource", "valueOrigin", "sourceMeasureRegionId"})
})
@Service("quote/line/page")
@ServiceName("page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.quote.service.lines.page.Page {
    @AutoInject private Long quoteId;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private String keyword;
    @AutoInject private String category;
    @AutoInject private QuoteLineRole role;
    @User private Employee employee;

    private final QuoteRepository quoteRepository;
    private final QuoteLineRepository lineRepository;
    private final DataScopeContext dataScopeContext;

    public Page(QuoteRepository quoteRepository, QuoteLineRepository lineRepository,
                DataScopeContext dataScopeContext) {
        this.quoteRepository = quoteRepository;
        this.lineRepository = lineRepository;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<QuoteLine> call(QuoteLine request) {
        if (quoteId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "报价索引不能为空"));
        }
        Employee currentEmployee = dataScopeContext.requireEmployee(employee);
        requireQuote(currentEmployee);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 50);
        Specification<QuoteLine> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("quote").get("quoteId"), quoteId));
            predicates.add(builder.isTrue(root.get("included")));
            if (role != null) predicates.add(builder.equal(root.get("role"), role));
            if (category != null && !category.isBlank()) {
                predicates.add(builder.equal(root.get("category"), category.trim()));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("category")), pattern),
                        builder.like(builder.lower(root.get("brand")), pattern),
                        builder.like(builder.lower(root.get("spec")), pattern),
                        builder.like(builder.lower(root.get("space").get("name")), pattern)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        org.springframework.data.domain.Page<QuoteLine> result = lineRepository.findAll(
                specification,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "sort").and(Sort.by(Sort.Direction.ASC, "itemId")))
        );
        return new PageImpl<>(result.getContent(), PageRequest.of(page, pageSize), result.getTotalElements());
    }

    private Quote requireQuote(Employee currentEmployee) {
        if (currentEmployee.getRole() == EmployeeRole.Marketer) {
            return quoteRepository.findMarketerVisibleById(quoteId, currentEmployee.getCompany().getComId(), currentEmployee.getEmpId())
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        }
        DataScope scope = dataScopeContext.resolve(currentEmployee);
        Specification<Quote> byId = (root, query, builder) -> builder.equal(root.get("quoteId"), quoteId);
        return quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
    }
}
