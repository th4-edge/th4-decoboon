package com.th4.edge.decoboon.design.service.schemes.save.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.context.DesignCopyContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.service.CadConvertService;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** 设计方案保存实现。 */
@Note("设计方案保存")
@Properties(allow = {
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "customer", "owner", "version", "style", "status", "area",
                        "layout", "description", "coverUrl", "cadUrl", "cadPreviewUrl", "cadConvertStatus", "originalPlanUrl", "demolitionUrl",
                        "masonryUrl", "optimizationUrl", "sitePhotos", "surveyPhotos", "originalPlanImages", "createTime", "property"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "community", "addr"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("design/scheme/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.design.service.schemes.save.Save {

    @AutoInject
    private Long customerId;

    @AutoInject
    private Long propertyId;

    @AutoInject
    private Long targetPropertyId;

    @AutoInject
    private Long ownerId;

    @AutoInject
    private Long copyFromId;

    @User
    private Employee employee;

    private final DesignContext designContext;
    private final DesignCopyContext designCopyContext;
    private final DesignRepository designRepository;
    private final PropertyContext propertyContext;
    private final CadConvertService cadConvertService;

    public Save(DesignContext designContext,
                DesignCopyContext designCopyContext,
                DesignRepository designRepository,
                PropertyContext propertyContext,
                CadConvertService cadConvertService) {
        this.designContext = designContext;
        this.designCopyContext = designCopyContext;
        this.designRepository = designRepository;
        this.propertyContext = propertyContext;
        this.cadConvertService = cadConvertService;
    }

    @Override
    public Design call(@ModelSaveInject Design request) {
        Employee current = designContext.requireDesignWriteAccess(employee);
        if (copyFromId != null) {
            return designCopyContext.copy(copyFromId, employee, current);
        }
        if (customerId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "新增设计方案必须指定客户"));
        }
        Customer customer = designContext.requireCustomer(customerId, employee);
        Employee fallback = customer.getDesigner() == null ? current : customer.getDesigner();
        Employee owner = designContext.resolveOwner(current, ownerId, fallback);

        Design design = new Design();
        design.setNo("D" + System.currentTimeMillis());
        design.setName(request.getName() == null || request.getName().isBlank()
                ? customer.getName() + "装修方案" : request.getName());
        design.setVersion((int) designRepository.countByCompany_ComIdAndCustomer_CustId(
                owner.getCompany().getComId(), customer.getCustId()) + 1);
        design.setStyle(request.getStyle());
        design.setLayout(request.getLayout());
        design.setDescription(request.getDescription());
        design.setCoverUrl(request.getCoverUrl());
        design.setCadUrl(request.getCadUrl());
        design.setOriginalPlanUrl(request.getOriginalPlanUrl());
        design.setDemolitionUrl(request.getDemolitionUrl());
        design.setMasonryUrl(request.getMasonryUrl());
        design.setOptimizationUrl(request.getOptimizationUrl());
        design.setSitePhotos(request.getSitePhotos());
        design.setSurveyPhotos(request.getSurveyPhotos());
        design.setOriginalPlanImages(request.getOriginalPlanImages());
        design.setStatus(DesignStatus.Draft);
        Long resolvedPropertyId = targetPropertyId != null ? targetPropertyId : propertyId;
        if (resolvedPropertyId == null && request.getProperty() != null) {
            resolvedPropertyId = request.getProperty().getPropertyId();
        }
        Property property = propertyContext.resolve(resolvedPropertyId, customer, current);
        designContext.requirePropertyOpen(property);
        design.setArea(initialArea(request.getArea(), customer.getArea(), property.getArea()));
        designContext.apply(design, customer, owner, property);
        Design saved = designRepository.save(design);
        if (saved.getCadUrl() != null && !saved.getCadUrl().isBlank()) {
            cadConvertService.convertAsync(saved.getDesignId(), saved.getCadUrl());
        }
        return saved;
    }

    private BigDecimal initialArea(BigDecimal requested, BigDecimal customerArea, BigDecimal propertyArea) {
        if (requested != null && requested.signum() > 0) return requested;
        if (customerArea != null && customerArea.signum() > 0) return customerArea;
        if (propertyArea != null && propertyArea.signum() > 0) return propertyArea;
        return BigDecimal.ZERO;
    }
}
