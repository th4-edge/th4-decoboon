package com.th4.edge.decoboon.pack.context;

import com.th4.edge.components.lock.DistributedLock;
import com.th4.edge.decoboon.pack.configure.PackageTemplateStatus;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageRule;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.model.PackageTemplateItem;
import com.th4.edge.decoboon.pack.model.PackageTemplateSnapshot;
import com.th4.edge.decoboon.pack.repository.PackageLineRepository;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 套餐模板生命周期与版本控制。 */
@Component
public class PackageLifecycleContext {

    private final PackageTemplateRepository templateRepository;
    private final PackageLineRepository lineRepository;
    private final PackageContext packageContext;
    private final PackageSnapshotContext snapshotContext;
    private final TenantContext tenantContext;
    private final DistributedLock distributedLock;
    private final TransactionTemplate transactionTemplate;

    public PackageLifecycleContext(PackageTemplateRepository templateRepository,
                                   PackageLineRepository lineRepository,
                                   PackageContext packageContext,
                                   PackageSnapshotContext snapshotContext,
                                   TenantContext tenantContext,
                                   DistributedLock distributedLock,
                                   TransactionTemplate transactionTemplate) {
        this.templateRepository = templateRepository;
        this.lineRepository = lineRepository;
        this.packageContext = packageContext;
        this.snapshotContext = snapshotContext;
        this.tenantContext = tenantContext;
        this.distributedLock = distributedLock;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public PackageTemplate createDraft(PackageTemplate request, String name, java.math.BigDecimal area,
                                       java.math.BigDecimal price, java.math.BigDecimal discount, Employee principal) {
        Employee current = requireMaintainer(principal);
        request.setCompany(current.getCompany());
        request.setCode("PACK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        request.setVersion(1);
        request.setStatus(PackageTemplateStatus.Draft);
        request.setCurrentPublished(Boolean.FALSE);
        request.setName(name);
        request.setArea(area);
        request.setPrice(price);
        request.setDiscount(discount == null ? java.math.BigDecimal.ONE : discount);
        request.setCost(packageContext.calculateCost(request));
        return templateRepository.save(request);
    }

    @Transactional(readOnly = true)
    public PackageTemplate requireDraft(Long templateId, Employee principal) {
        Employee current = requireMaintainer(principal);
        PackageTemplate template = requireTemplate(templateId, current.getCompany().getComId());
        if (template.getStatus() != PackageTemplateStatus.Draft) {
            throw new DefinedException(new DefaultErrorCode("409", "已发布或已停用的套餐不能原地修改，请先创建新版本"));
        }
        PackageTemplate latest = templateRepository
                .findFirstByCompany_ComIdAndCodeOrderByVersionDesc(current.getCompany().getComId(), template.getCode())
                .orElse(template);
        if (!latest.getTemplateId().equals(template.getTemplateId())) {
            throw new DefinedException(new DefaultErrorCode("409", "只能修改套餐最新草稿"));
        }
        return template;
    }

    @Transactional(readOnly = true)
    public PackageTemplate requireTemplate(Long templateId, Long comId) {
        return templateRepository.findByTemplateIdAndCompany_ComId(templateId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "套餐不存在或无权访问")));
    }

    public PackageTemplate deleteDraft(Long templateId, Employee principal) {
        Employee current = requireMaintainer(principal);
        Long comId = current.getCompany().getComId();
        PackageTemplate base = requireTemplate(templateId, comId);
        String lockKey = lockKey(comId, base.getCode());
        lock(lockKey);
        try {
            return transactionTemplate.execute(status -> {
                PackageTemplate template = requireDraft(templateId, current);
                if (templateRepository.countBusinessReferences(templateId) > 0) {
                    throw new DefinedException(new DefaultErrorCode("409", "套餐草稿已被业务引用，不能删除"));
                }
                templateRepository.delete(template);
                return template;
            });
        } catch (DataIntegrityViolationException ex) {
            throw new DefinedException(new DefaultErrorCode("409", "套餐草稿仍被业务引用或正在被其他操作更新，请刷新后重试"));
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    public PackageTemplate buildNextVersion(Long templateId, Employee principal) {
        Employee current = requireMaintainer(principal);
        Long comId = current.getCompany().getComId();
        PackageTemplate base = requireTemplate(templateId, comId);
        String lockKey = lockKey(comId, base.getCode());
        lock(lockKey);
        try {
            return executeVersionWrite(() -> {
                PackageTemplate latest = templateRepository
                        .findFirstByCompany_ComIdAndCodeOrderByVersionDesc(comId, base.getCode())
                        .orElseThrow(() -> new DefinedException(new DefaultErrorCode("409", "套餐版本已变化，请刷新后重试")));
                if (!latest.getTemplateId().equals(templateId)) {
                    throw new DefinedException(new DefaultErrorCode("409", "套餐版本已变化，请刷新后重试"));
                }
                if (latest.getStatus() == PackageTemplateStatus.Draft) return latest;
                PackageTemplate draft = copyHead(latest);
                draft.setVersion(latest.getVersion() + 1);
                draft.setStatus(PackageTemplateStatus.Draft);
                draft.setCurrentPublished(Boolean.FALSE);
                draft.setPublishedAt(null);
                draft.setPublishedBy(null);
                draft.setSnapshotJson(null);
                draft.setSnapshotHash(null);
                draft = templateRepository.save(draft);
                copyLines(latest, draft);
                return templateRepository.findByTemplateIdAndCompany_ComId(draft.getTemplateId(), comId).orElse(draft);
            });
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Transactional
    public PackageTemplate copyTemplate(Long templateId, Employee principal) {
        Employee current = requireMaintainer(principal);
        Long comId = current.getCompany().getComId();
        PackageTemplate source = requireTemplate(templateId, comId);
        PackageTemplate latest = templateRepository
                .findFirstByCompany_ComIdAndCodeOrderByVersionDesc(comId, source.getCode())
                .orElse(source);
        if (!latest.getTemplateId().equals(source.getTemplateId())) {
            throw new DefinedException(new DefaultErrorCode("409", "只能复制套餐最新版本"));
        }
        PackageTemplate copy = copyHead(source);
        copy.setCode("PACK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        copy.setVersion(1);
        copy.setStatus(PackageTemplateStatus.Draft);
        copy.setCurrentPublished(Boolean.FALSE);
        copy.setName(copyName(source.getName()));
        copy.setPublishedAt(null);
        copy.setPublishedBy(null);
        copy.setSnapshotJson(null);
        copy.setSnapshotHash(null);
        PackageTemplate saved = templateRepository.save(copy);
        copyLines(source, saved);
        return templateRepository.findByTemplateIdAndCompany_ComId(saved.getTemplateId(), comId).orElse(saved);
    }

    public PackageTemplate publish(Long templateId, Employee principal) {
        Employee current = requireMaintainer(principal);
        Long comId = current.getCompany().getComId();
        PackageTemplate base = requireTemplate(templateId, comId);
        String lockKey = lockKey(comId, base.getCode());
        lock(lockKey);
        try {
            return executeVersionWrite(() -> {
                PackageTemplate template = requireTemplate(templateId, comId);
                if (template.getStatus() == PackageTemplateStatus.Published && Boolean.TRUE.equals(template.getCurrentPublished())) {
                    return ensureSnapshot(template);
                }
                if (template.getStatus() != PackageTemplateStatus.Draft) {
                    throw new DefinedException(new DefaultErrorCode("409", "只有草稿套餐可以发布"));
                }
                PackageTemplate latest = templateRepository
                        .findFirstByCompany_ComIdAndCodeOrderByVersionDesc(comId, template.getCode())
                        .orElse(template);
                if (!latest.getTemplateId().equals(template.getTemplateId())) {
                    throw new DefinedException(new DefaultErrorCode("409", "只能发布套餐最新版本"));
                }
                List<PackageLine> lines = lineRepository
                        .findByTemplate_TemplateIdAndCompany_ComIdOrderBySortAscItemIdAsc(templateId, comId);
                if (lines.isEmpty()) {
                    throw new DefinedException(new DefaultErrorCode("400", "套餐至少需要一个项目行后才能发布"));
                }
                attachLines(template, lines);
                template.setCost(packageContext.calculateCost(template));
                PackageTemplateSnapshot snapshot = snapshotContext.snapshot(template);
                List<PackageTemplate> existing = templateRepository
                        .findByCompany_ComIdAndCodeAndCurrentPublishedTrue(comId, template.getCode());
                for (PackageTemplate item : existing) {
                    if (!item.getTemplateId().equals(templateId)) {
                        item.setCurrentPublished(Boolean.FALSE);
                    }
                }
                templateRepository.saveAll(existing);
                template.setStatus(PackageTemplateStatus.Published);
                template.setCurrentPublished(Boolean.TRUE);
                template.setPublishedAt(LocalDateTime.now());
                template.setPublishedBy(current);
                template.setSnapshotJson(snapshot.getJson());
                template.setSnapshotHash(snapshot.getHash());
                return templateRepository.save(template);
            });
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    public PackageTemplate disable(Long templateId, Employee principal) {
        Employee current = requireMaintainer(principal);
        Long comId = current.getCompany().getComId();
        PackageTemplate base = requireTemplate(templateId, comId);
        String lockKey = lockKey(comId, base.getCode());
        lock(lockKey);
        try {
            return executeVersionWrite(() -> {
                PackageTemplate template = requireTemplate(templateId, comId);
                if (template.getStatus() == PackageTemplateStatus.Disabled) {
                    template.setCurrentPublished(Boolean.FALSE);
                    return templateRepository.save(template);
                }
                if (template.getStatus() != PackageTemplateStatus.Published
                        || !Boolean.TRUE.equals(template.getCurrentPublished())) {
                    throw new DefinedException(new DefaultErrorCode("409", "只有当前发布套餐可以停用"));
                }
                template.setStatus(PackageTemplateStatus.Disabled);
                template.setCurrentPublished(Boolean.FALSE);
                return templateRepository.save(template);
            });
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Transactional
    public PackageTemplate requireQuoteSource(Long templateId, Long comId) {
        PackageTemplate template = requireTemplate(templateId, comId);
        if (template.getStatus() != PackageTemplateStatus.Published
                || !Boolean.TRUE.equals(template.getCurrentPublished())) {
            throw new DefinedException(new DefaultErrorCode("409", "该套餐不是当前发布版本，不能用于新报价"));
        }
        return ensureSnapshot(template);
    }

    private PackageTemplate ensureSnapshot(PackageTemplate template) {
        if (template.getSnapshotJson() != null && !template.getSnapshotJson().isBlank()
                && template.getSnapshotHash() != null && !template.getSnapshotHash().isBlank()) {
            return template;
        }
        List<PackageLine> lines = lineRepository
                .findByTemplate_TemplateIdAndCompany_ComIdOrderBySortAscItemIdAsc(
                        template.getTemplateId(), template.getCompany().getComId());
        if (lines.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("409", "套餐缺少项目行，不能作为报价来源"));
        }
        attachLines(template, lines);
        template.setCost(packageContext.calculateCost(template));
        PackageTemplateSnapshot snapshot = snapshotContext.snapshot(template);
        template.setSnapshotJson(snapshot.getJson());
        template.setSnapshotHash(snapshot.getHash());
        return templateRepository.save(template);
    }

    private PackageTemplate executeVersionWrite(java.util.function.Supplier<PackageTemplate> action) {
        try {
            return transactionTemplate.execute(status -> action.get());
        } catch (DataIntegrityViolationException ex) {
            throw new DefinedException(new DefaultErrorCode("409", "套餐版本正在被其他操作更新，请刷新后重试"));
        }
    }

    private Employee requireMaintainer(Employee principal) {
        return tenantContext.requireRole(principal, com.th4.edge.tenant.user.configure.EmployeeRole.Boss,
                com.th4.edge.tenant.user.configure.EmployeeRole.StoreManager);
    }

    private void attachLines(PackageTemplate template, List<PackageLine> lines) {
        if (template.getItems() == null) {
            template.setItems(lines);
        } else {
            template.getItems().clear();
            template.getItems().addAll(lines);
        }
    }

    private String copyName(String sourceName) {
        String base = sourceName == null || sourceName.isBlank() ? "套餐" : sourceName.trim();
        String suffix = "（副本）";
        int maxBaseLength = Math.max(0, 128 - suffix.length());
        return (base.length() > maxBaseLength ? base.substring(0, maxBaseLength) : base) + suffix;
    }

    private PackageTemplate copyHead(PackageTemplate source) {
        PackageTemplate target = new PackageTemplate();
        target.setCompany(source.getCompany());
        target.setCode(source.getCode());
        target.setName(source.getName());
        target.setArea(source.getArea());
        target.setPrice(source.getPrice());
        target.setDiscount(source.getDiscount());
        target.setCost(source.getCost());
        target.setCriterions(source.getCriterions() == null ? List.of() : List.copyOf(source.getCriterions()));
        target.setMeasures(copyMeasures(source.getMeasures()));
        return target;
    }

    private Map<PackageTemplateItem, PackageRule> copyMeasures(Map<PackageTemplateItem, PackageRule> source) {
        Map<PackageTemplateItem, PackageRule> result = new LinkedHashMap<>();
        if (source == null) return result;
        for (Map.Entry<PackageTemplateItem, PackageRule> entry : source.entrySet()) {
            PackageRule value = entry.getValue();
            PackageRule copy = new PackageRule();
            if (value != null) {
                copy.setPrice(value.getPrice());
                copy.setStandard(value.getStandard());
                copy.setRemark(value.getRemark());
            }
            result.put(entry.getKey(), copy);
        }
        return result;
    }

    private void copyLines(PackageTemplate source, PackageTemplate target) {
        List<PackageLine> sourceLines = lineRepository
                .findByTemplate_TemplateIdAndCompany_ComIdOrderBySortAscItemIdAsc(
                        source.getTemplateId(), source.getCompany().getComId());
        List<PackageLine> copies = new ArrayList<>();
        for (PackageLine line : sourceLines) {
            PackageLine copy = new PackageLine();
            copy.setCompany(target.getCompany());
            copy.setTemplate(target);
            copy.setSection(line.getSection());
            copy.setItem(line.getItem());
            copy.setName(line.getName());
            copy.setBrand(line.getBrand());
            copy.setSpec(line.getSpec());
            copy.setUnit(line.getUnit());
            copy.setQuantity(line.getQuantity());
            copy.setQuantityFactor(line.getQuantityFactor());
            copy.setPricingMethod(line.getPricingMethod());
            copy.setIncluded(line.getIncluded());
            copy.setDeductible(line.getDeductible());
            copy.setSort(line.getSort());
            copy.setCriterions(line.getCriterions() == null ? List.of() : List.copyOf(line.getCriterions()));
            copy.setSectionNameSnapshot(line.getSectionNameSnapshot());
            copy.setCatalogTypeSnapshot(line.getCatalogTypeSnapshot());
            copy.setCatalogCategorySnapshot(line.getCatalogCategorySnapshot());
            copy.setCraftSnapshot(line.getCraftSnapshot());
            copy.setCostPriceSnapshot(line.getCostPriceSnapshot());
            copies.add(copy);
        }
        lineRepository.saveAll(copies);
    }

    private void lock(String lockKey) {
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "套餐版本正在被处理，请稍后重试"));
        }
    }

    private String lockKey(Long comId, String code) {
        return "pack-template:" + comId + ":" + code;
    }
}
