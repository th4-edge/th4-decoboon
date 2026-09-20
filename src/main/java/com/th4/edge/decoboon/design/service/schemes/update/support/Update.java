package com.th4.edge.decoboon.design.service.schemes.update.support;

import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.service.CadConvertService;
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
                        "description", "coverUrl", "cadUrl", "cadPreviewUrl", "cadConvertStatus", "originalPlanUrl", "demolitionUrl", "masonryUrl",
                        "optimizationUrl", "sitePhotos", "surveyPhotos", "originalPlanImages", "updateTime", "property"}),
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
    private java.util.List<String> originalPlanImages;

    @AutoInject
    private java.util.List<String> surveyPhotos;

    @User
    private Employee employee;

    private final DesignContext designContext;
    private final DesignRepository designRepository;
    private final CadConvertService cadConvertService;

    public Update(DesignContext designContext, DesignRepository designRepository, CadConvertService cadConvertService) {
        this.designContext = designContext;
        this.designRepository = designRepository;
        this.cadConvertService = cadConvertService;
    }

    @Override
    public Design call(Design request) {
        Design design = designContext.requireDesign(designId, employee);
        designContext.requireEditable(design);
        // 仅覆盖显式提交的字段：null 表示"不修改"，空集合表示"清空"。
        // 避免客户素材等局部编辑把方案名称、风格、封面等主数据误清空。
        if (name != null) design.setName(name);
        if (style != null) design.setStyle(style);
        if (layout != null) design.setLayout(layout);
        if (description != null) design.setDescription(description);
        if (coverUrl != null) design.setCoverUrl(coverUrl);
        // cadUrl 为 null 表示不修改；空字符串表示清除（同时清除预览图和转换状态）
        boolean cadChanged = cadUrl != null && !cadUrl.equals(design.getCadUrl());
        if (cadUrl != null) {
            design.setCadUrl(cadUrl);
            if (cadUrl.isBlank()) {
                design.setCadPreviewUrl(null);
                design.setCadConvertStatus(null);
            }
        }
        if (originalPlanUrl != null) design.setOriginalPlanUrl(originalPlanUrl);
        if (demolitionUrl != null) design.setDemolitionUrl(demolitionUrl);
        if (masonryUrl != null) design.setMasonryUrl(masonryUrl);
        if (optimizationUrl != null) design.setOptimizationUrl(optimizationUrl);
        if (sitePhotos != null) design.setSitePhotos(sitePhotos);
        if (originalPlanImages != null) design.setOriginalPlanImages(originalPlanImages);
        if (surveyPhotos != null) design.setSurveyPhotos(surveyPhotos);
        Design saved = designRepository.save(design);
        if (cadChanged && saved.getCadUrl() != null && !saved.getCadUrl().isBlank()) {
            cadConvertService.convertAsync(saved.getDesignId(), saved.getCadUrl());
        }
        return saved;
    }
}
