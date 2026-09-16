package com.th4.edge.decoboon.design.service.schemes.update.support;

import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 设计方案基础信息更新实现。 */
@Note("设计方案更新")
@Properties(allow = {
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "style", "status", "area", "layout",
                        "description", "coverUrl", "cadUrl", "originalPlanUrl", "demolitionUrl", "masonryUrl",
                        "optimizationUrl", "sitePhotos", "surveyPhotos", "updateTime", "property"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
})
@Service("design/scheme/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.design.service.schemes.update.Update {

    @AutoInject
    @NotNull
    private Long designId;

    @AutoInject
    @NotNull
    private String name;

    @AutoInject
    private String style;

    @AutoInject
    private String layout;

    @AutoInject
    private String description;

    @AutoInject
    private String coverUrl;

    @AutoInject
    private String cadUrl;

    @AutoInject
    private String originalPlanUrl;

    @AutoInject
    private String demolitionUrl;

    @AutoInject
    private String masonryUrl;

    @AutoInject
    private String optimizationUrl;

    @AutoInject
    private java.util.List<String> sitePhotos;

    @AutoInject
    private java.util.List<String> surveyPhotos;

    @User
    private Employee employee;

    private final DesignContext designContext;
    private final DesignRepository designRepository;

    public Update(DesignContext designContext, DesignRepository designRepository) {
        this.designContext = designContext;
        this.designRepository = designRepository;
    }

    @Override
    public Design call(Design request) {
        Design design = designContext.requireDesign(designId, employee);
        designContext.requireEditable(design);
        design.setName(name);
        design.setStyle(style);
        design.setLayout(layout);
        design.setDescription(description);
        design.setCoverUrl(coverUrl);
        design.setCadUrl(cadUrl);
        design.setOriginalPlanUrl(originalPlanUrl);
        design.setDemolitionUrl(demolitionUrl);
        design.setMasonryUrl(masonryUrl);
        design.setOptimizationUrl(optimizationUrl);
        design.setSitePhotos(sitePhotos);
        design.setSurveyPhotos(surveyPhotos);
        return designRepository.save(design);
    }
}
