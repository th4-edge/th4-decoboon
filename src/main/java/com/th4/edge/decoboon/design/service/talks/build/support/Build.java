package com.th4.edge.decoboon.design.service.talks.build.support;

import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.context.TalkContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.decoboon.design.model.TalkSection;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.repository.TalkRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.property.model.Property;
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
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 谈单 PPT 内容生成实现。 */
@Note("谈单PPT生成")
@Properties(allow = {
        @AllowProperties(model = Talk.class,
                properties = {"talkId", "no", "customer", "design", "property", "quote", "title", "version", "status",
                        "coverUrl", "outputUrl", "description", "builtAt", "owner", "sections", "createTime"}),
        @AllowProperties(model = TalkSection.class,
                properties = {"sectionId", "type", "title", "subtitle", "content", "images", "sort", "visible"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "style", "status", "area", "layout", "coverUrl"}),
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "version", "subtotal", "discount", "taxAmount", "amount", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("design/talk/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.design.service.talks.build.Build {

    @AutoInject
    @NotNull
    private Long designId;

    @AutoInject
    private Long quoteId;

    @AutoInject
    private Long talkId;

    @User
    private Employee employee;

    private final DesignContext designContext;
    private final TalkContext talkContext;
    private final QuoteRepository quoteRepository;
    private final TalkRepository talkRepository;
    private final DataScopeContext dataScopeContext;

    public Build(DesignContext designContext,
                 TalkContext talkContext,
                 QuoteRepository quoteRepository,
                 TalkRepository talkRepository,
                 DataScopeContext dataScopeContext) {
        this.designContext = designContext;
        this.talkContext = talkContext;
        this.quoteRepository = quoteRepository;
        this.talkRepository = talkRepository;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public Talk call(Talk request) {
        Design design = designContext.requireDesign(designId, employee);
        designContext.requireEditable(design);
        Quote quote = requireQuote(design);
        Talk talk = talkId == null ? new Talk() : requireTalk(talkId, employee);
        if (request.getTitle() != null) {
            talk.setTitle(request.getTitle());
        }
        return talkContext.build(talk, design, quote);
    }

    private Quote requireQuote(Design design) {
        List<Quote> quotes = quoteRepository.findByDesign_DesignIdOrderByVersionDesc(design.getDesignId());
        return quotes.stream()
                .filter(item -> quoteId == null || quoteId.equals(item.getQuoteId()))
                .filter(item -> !QuoteStatus.Rejected.equals(item.getStatus()))
                .max(Comparator.comparing(Quote::getVersion))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("400", "主推方案尚未生成设计报价")));
    }

    private Talk requireTalk(Long id, Employee employee) {
        DataScope scope = dataScopeContext.requireScope(employee);
        Specification<Talk> byId = (root, query, builder) -> builder.equal(root.get("talkId"), id);
        return talkRepository.findOne(byId.and(DataScopeSpecification.visible(Talk.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "谈单方案不存在或无权访问")));
    }
}
