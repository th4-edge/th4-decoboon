package com.th4.edge.decoboon.design.service.talks.info.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.decoboon.design.model.TalkSection;
import com.th4.edge.decoboon.design.repository.TalkRepository;
import com.th4.edge.decoboon.design.repository.TalkSectionRepository;
import com.th4.edge.decoboon.quote.model.Quote;
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

/** 谈单 PPT 详情实现。 */
@Note("谈单PPT详情")
@Properties(allow = {
        @AllowProperties(model = Talk.class,
                properties = {"talkId", "no", "customer", "design", "property", "quote", "title", "version", "status",
                        "coverUrl", "outputUrl", "description", "builtAt", "owner", "sections", "createTime"}),
        @AllowProperties(model = TalkSection.class,
                properties = {"sectionId", "type", "title", "subtitle", "content", "images", "sort", "visible"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "style", "status", "area", "layout", "coverUrl"}),
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "version", "subtotal", "discount", "taxAmount", "amount", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "community", "addr"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("design/talk/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.design.service.talks.info.Info {

    @AutoInject
    @NotNull
    private Long talkId;

    @User
    private Employee employee;

    private final TalkRepository talkRepository;
    private final TalkSectionRepository sectionRepository;
    private final DataScopeContext dataScopeContext;

    public Info(TalkRepository talkRepository,
                TalkSectionRepository sectionRepository,
                DataScopeContext dataScopeContext) {
        this.talkRepository = talkRepository;
        this.sectionRepository = sectionRepository;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public Talk call(Talk request) {
        DataScope scope = dataScopeContext.requireScope(employee);
        Specification<Talk> byId = (root, query, builder) -> builder.equal(root.get("talkId"), talkId);
        Talk talk = talkRepository.findOne(byId.and(DataScopeSpecification.visible(Talk.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "谈单方案不存在或无权访问")));
        talk.setSections(sectionRepository.findByTalk_TalkIdOrderBySortAsc(talkId));
        return talk;
    }
}
