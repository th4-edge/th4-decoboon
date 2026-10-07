package com.th4.edge.decoboon.pack.context;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.model.PackageTemplateItem;
import com.th4.edge.decoboon.pack.repository.PackageLineRepository;
import com.th4.edge.decoboon.pack.repository.PackageSectionRepository;
import com.th4.edge.decoboon.pack.repository.PackageTemplateItemRepository;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** 套餐模板分区、项目行和差价项维护上下文。 */
@Note("套餐模板配置上下文")
@Component
public class PackageConfigContext {

    private final PackageTemplateRepository templateRepository;
    private final PackageSectionRepository sectionRepository;
    private final PackageLineRepository lineRepository;
    private final PackageTemplateItemRepository templateItemRepository;
    private final CatalogItemRepository catalogRepository;
    private final QuoteLineRepository quoteLineRepository;
    private final PackageContext packageContext;
    private final TenantContext tenantContext;

    public PackageConfigContext(PackageTemplateRepository templateRepository,
                                PackageSectionRepository sectionRepository,
                                PackageLineRepository lineRepository,
                                PackageTemplateItemRepository templateItemRepository,
                                CatalogItemRepository catalogRepository,
                                QuoteLineRepository quoteLineRepository,
                                PackageContext packageContext,
                                TenantContext tenantContext) {
        this.templateRepository = templateRepository;
        this.sectionRepository = sectionRepository;
        this.lineRepository = lineRepository;
        this.templateItemRepository = templateItemRepository;
        this.catalogRepository = catalogRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.packageContext = packageContext;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public PackageSection saveSection(PackageSection source, Employee principal) {
        Employee current = tenantContext.requireBoss(principal);
        Long comId = current.getCompany().getComId();
        PackageSection target = source.getGroupId() == null ? new PackageSection()
                : sectionRepository.findByGroupIdAndCompany_ComId(source.getGroupId(), comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "套餐分区不存在或无权访问")));
        target.setCompany(current.getCompany());
        target.setName(source.getName());
        target.setSpaceTypes(source.getSpaceTypes());
        target.setMaxDeleteNum(source.getMaxDeleteNum() == null ? 1 : source.getMaxDeleteNum());
        target.setPriority(source.getPriority() == null ? 0 : source.getPriority());
        return sectionRepository.save(target);
    }

    @Transactional
    public void deleteSection(Long groupId, Employee principal) {
        Employee current = tenantContext.requireBoss(principal);
        PackageSection section = sectionRepository.findByGroupIdAndCompany_ComId(
                        groupId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "套餐分区不存在或无权访问")));
        if (lineRepository.countBySection_GroupId(groupId) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "套餐分区下存在项目行，不能删除"));
        }
        sectionRepository.delete(section);
    }

    @Transactional
    public PackageLine saveLine(PackageLine source, Long templateId, Long sectionId,
                                Long catalogItemId, Employee principal) {
        Employee current = tenantContext.requireBoss(principal);
        Long comId = current.getCompany().getComId();
        PackageTemplate template = templateRepository.findByTemplateIdAndCompany_ComId(templateId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "套餐不存在或无权访问")));
        PackageSection section = sectionRepository.findByGroupIdAndCompany_ComId(sectionId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "套餐分区不存在或无权访问")));
        CatalogItem catalog = catalogRepository.findByItemIdAndCompany_ComId(catalogItemId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价项目不存在或无权访问")));
        if (!Boolean.TRUE.equals(catalog.getActive())) {
            throw new DefinedException(new DefaultErrorCode("400", "报价项目已停用"));
        }
        PackageLine target = source.getItemId() == null ? new PackageLine()
                : lineRepository.findByItemIdAndCompany_ComId(source.getItemId(), comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "套餐项目行不存在或无权访问")));
        target.setCompany(current.getCompany());
        target.setTemplate(template);
        target.setSection(section);
        target.setItem(catalog);
        target.setName(source.getName() == null || source.getName().isBlank() ? catalog.getName() : source.getName());
        target.setBrand(source.getBrand() == null ? catalog.getBrand() : source.getBrand());
        target.setSpec(source.getSpec() == null ? catalog.getSpec() : source.getSpec());
        target.setUnit(source.getUnit() == null ? catalog.getUnit() : source.getUnit());
        target.setQuantity(source.getQuantity());
        target.setQuantityFactor(source.getQuantityFactor() == null ? BigDecimal.ONE : source.getQuantityFactor());
        target.setPricingMethod(source.getPricingMethod() == null ? catalog.getPricingMode() : source.getPricingMethod());
        target.setIncluded(source.getIncluded() == null || source.getIncluded());
        target.setDeductible(source.getDeductible() == null || source.getDeductible());
        target.setSort(source.getSort() == null ? 0 : source.getSort());
        target.setCriterions(source.getCriterions());
        PackageLine saved = lineRepository.save(target);
        recalculate(template);
        return saved;
    }

    @Transactional
    public void deleteLine(Long itemId, Employee principal) {
        Employee current = tenantContext.requireBoss(principal);
        PackageLine line = lineRepository.findByItemIdAndCompany_ComId(itemId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "套餐项目行不存在或无权访问")));
        if (quoteLineRepository.existsByPackageLine_ItemId(itemId)) {
            throw new DefinedException(new DefaultErrorCode("400", "套餐项目行已生成历史报价快照，不能删除"));
        }
        PackageTemplate template = line.getTemplate();
        lineRepository.delete(line);
        lineRepository.flush();
        recalculate(template);
    }

    @Transactional
    public PackageTemplateItem saveTemplateItem(PackageTemplateItem source, Employee principal) {
        Employee current = tenantContext.requireBoss(principal);
        Long comId = current.getCompany().getComId();
        PackageTemplateItem target = source.getItemId() == null ? new PackageTemplateItem()
                : templateItemRepository.findByItemIdAndCompany_ComId(source.getItemId(), comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "套餐差价项不存在或无权访问")));
        target.setCompany(current.getCompany());
        target.setName(source.getName());
        target.setUnit(source.getUnit());
        target.setSpaceTypes(source.getSpaceTypes());
        target.setPriority(source.getPriority() == null ? 0 : source.getPriority());
        return templateItemRepository.save(target);
    }

    private void recalculate(PackageTemplate template) {
        List<PackageLine> lines = lineRepository.findByTemplate_TemplateIdOrderBySortAscItemIdAsc(
                template.getTemplateId());
        // 历史坑：FIX-034 items 带 orphanRemoval，直接替换集合会触发 Hibernate
        // "A collection with orphan deletion was no longer referenced"；必须复用原集合。
        if (template.getItems() == null) {
            template.setItems(lines);
        } else {
            template.getItems().clear();
            template.getItems().addAll(lines);
        }
        template.setCost(packageContext.calculateCost(template));
        templateRepository.save(template);
    }
}
