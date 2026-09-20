package com.th4.edge.decoboon.quote.context;

import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteComponent;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 基于历史报价生成新的草稿版本，原报价保持不变。 */
@Note("报价版本重报上下文")
@Component
public class QuoteCopyContext {

    private final QuoteRepository quoteRepository;
    private final QuoteLineRepository lineRepository;
    private final DataScopeContext dataScopeContext;
    private final ProjectRepository projectRepository;

    public QuoteCopyContext(QuoteRepository quoteRepository,
                            QuoteLineRepository lineRepository,
                            DataScopeContext dataScopeContext,
                            ProjectRepository projectRepository) {
        this.quoteRepository = quoteRepository;
        this.lineRepository = lineRepository;
        this.dataScopeContext = dataScopeContext;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public Quote rebase(Long sourceQuoteId, Employee principal) {
        Employee current = dataScopeContext.requireEmployee(principal);
        DataScope scope = dataScopeContext.resolve(current);
        Specification<Quote> byId = (root, query, builder) -> builder.equal(root.get("quoteId"), sourceQuoteId);
        Quote source = quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        if (!QuoteStatus.Rejected.equals(source.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "只有客户已驳回的报价可以重新生成版本"));
        }
        Long propertyId = source.getProperty().getPropertyId();
        Long comId = source.getCompany().getComId();
        if (projectRepository.existsByCompany_ComIdAndProperty_PropertyId(comId, propertyId)
                || quoteRepository.existsByProperty_PropertyIdAndStatusIn(propertyId,
                List.of(QuoteStatus.Accepted, QuoteStatus.Converted))) {
            throw new DefinedException(new DefaultErrorCode("400", "该房屋已有客户确认报价或项目，不能重新生成报价"));
        }

        Quote copy = new Quote();
        copy.setNo("Q" + System.currentTimeMillis());
        copy.setTitle(source.getTitle());
        copy.setCompany(source.getCompany());
        copy.setStore(source.getStore());
        copy.setTeam(source.getTeam());
        copy.setProperty(source.getProperty());
        copy.setCustomer(source.getCustomer());
        copy.setDesign(source.getDesign());
        copy.setPackageTemplate(source.getPackageTemplate());
        copy.setCreatedBy(EmployeeRole.Boss.equals(current.getRole()) ? source.getCreatedBy() : current);
        copy.setVersion(nextVersion(source, current));
        copy.setMode(source.getMode());
        copy.setArea(zero(source.getArea()));
        copy.setSubtotal(zero(source.getSubtotal()));
        copy.setDiscount(source.getDiscount() == null ? BigDecimal.ONE : source.getDiscount());
        copy.setTaxRate(source.getTaxRate() == null ? new BigDecimal("0.05") : source.getTaxRate());
        copy.setTaxAmount(zero(source.getTaxAmount()));
        copy.setAmount(zero(source.getAmount()));
        copy.setCost(zero(source.getCost()));
        copy.setCostComplete(Boolean.TRUE.equals(source.getCostComplete()));
        copy.setDesignFee(zero(source.getDesignFee()));
        copy.setManagementFee(zero(source.getManagementFee()));
        copy.setStatus(QuoteStatus.Draft);
        Quote saved = quoteRepository.save(copy);

        List<QuoteLine> copiedLines = new ArrayList<>();
        for (QuoteLine sourceLine : lineRepository.findByQuote(source)) {
            copiedLines.add(copyLine(sourceLine, saved));
        }
        if (!copiedLines.isEmpty()) {
            lineRepository.saveAll(copiedLines);
        }
        saved.setItems(copiedLines);
        return saved;
    }

    private int nextVersion(Quote source, Employee current) {
        Long comId = source.getCompany().getComId();
        Long propertyId = source.getProperty() == null ? null : source.getProperty().getPropertyId();
        if (source.getDesign() != null && source.getDesign().getDesignId() != null) {
            return (int) quoteRepository.countByDesign_DesignId(source.getDesign().getDesignId()) + 1;
        }
        if (source.getPackageTemplate() != null && source.getPackageTemplate().getTemplateId() != null) {
            if (propertyId != null) {
                return (int) quoteRepository.countByCompany_ComIdAndProperty_PropertyIdAndPackageTemplate_TemplateId(
                        comId, propertyId, source.getPackageTemplate().getTemplateId()) + 1;
            }
            return (int) quoteRepository.countByCompany_ComIdAndCustomer_CustIdAndPackageTemplate_TemplateId(
                    comId, source.getCustomer().getCustId(), source.getPackageTemplate().getTemplateId()) + 1;
        }
        if (propertyId != null) {
            return (int) quoteRepository.countByCompany_ComIdAndProperty_PropertyId(comId, propertyId) + 1;
        }
        return (int) quoteRepository.countByCompany_ComIdAndCustomer_CustId(comId, source.getCustomer().getCustId()) + 1;
    }

    private QuoteLine copyLine(QuoteLine source, Quote quote) {
        QuoteLine copy = new QuoteLine();
        copy.setCompany(quote.getCompany());
        copy.setStore(quote.getStore());
        copy.setTeam(quote.getTeam());
        copy.setOwner(quote.getCreatedBy());
        copy.setQuote(quote);
        copy.setProperty(quote.getProperty());
        copy.setDesignItem(source.getDesignItem());
        copy.setCatalogItem(source.getCatalogItem());
        copy.setSpace(source.getSpace());
        copy.setPackageLine(source.getPackageLine());
        copy.setRole(source.getRole());
        copy.setIncluded(source.getIncluded() == null || source.getIncluded());
        copy.setDiscountAmount(zero(source.getDiscountAmount()));
        copy.setComponents(copyComponents(source.getComponents()));
        copy.setType(source.getType());
        copy.setCategory(source.getCategory());
        copy.setName(source.getName());
        copy.setBrand(source.getBrand());
        copy.setSpec(source.getSpec());
        copy.setUnit(source.getUnit());
        copy.setQuantity(zero(source.getQuantity()));
        copy.setMaterialPrice(zero(source.getMaterialPrice()));
        copy.setManualPrice(zero(source.getManualPrice()));
        copy.setPricingMode(source.getPricingMode());
        copy.setPrice(zero(source.getPrice()));
        copy.setAmount(zero(source.getAmount()));
        copy.setCost(zero(source.getCost()));
        copy.setActualMaterialCost(zero(source.getActualMaterialCost()));
        copy.setActualLaborCost(zero(source.getActualLaborCost()));
        copy.setActualOtherCost(zero(source.getActualOtherCost()));
        copy.setActualCost(zero(source.getActualCost()));
        copy.setFormula(source.getFormula());
        copy.setSort(source.getSort());
        return copy;
    }

    private List<QuoteComponent> copyComponents(List<QuoteComponent> source) {
        if (source == null) {
            return null;
        }
        List<QuoteComponent> result = new ArrayList<>();
        for (QuoteComponent item : source) {
            QuoteComponent copy = new QuoteComponent();
            copy.setType(item.getType());
            copy.setName(item.getName());
            copy.setSaleAmount(zero(item.getSaleAmount()));
            copy.setCostAmount(zero(item.getCostAmount()));
            copy.setFormula(item.getFormula());
            copy.setSort(item.getSort());
            result.add(copy);
        }
        return result;
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
