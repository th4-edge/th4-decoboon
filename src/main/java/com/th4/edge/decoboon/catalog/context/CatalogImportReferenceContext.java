package com.th4.edge.decoboon.catalog.context;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 导入批次撤销前的下游引用检查（PRD §9）。
 *
 * <p>任意一条导入项目已被设计分项、套餐、报价、任务、材料计划、库存、供货价或库存流水引用，
 * 整批撤销必须失败，不做部分删除。</p>
 */
@Component
public class CatalogImportReferenceContext {

    private final EntityManager entityManager;

    public CatalogImportReferenceContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** 返回首个引用该项目的下游名称；无引用时返回 null。 */
    public String findReference(CatalogItem item) {
        if (item == null || item.getItemId() == null) {
            return null;
        }
        List<Reference> references = List.of(
                new Reference("设计分项", "select count(d) from DesignItem d where d.catalogItem.itemId = :itemId"),
                new Reference("套餐配置", "select count(p) from PackageLine p where p.item.itemId = :itemId"),
                new Reference("报价", "select count(q) from QuoteLine q where q.catalogItem.itemId = :itemId"),
                new Reference("施工任务", "select count(t) from ProjectTask t where t.catalogItem.itemId = :itemId"),
                new Reference("材料计划", "select count(m) from MaterialPlan m where m.catalogItem.itemId = :itemId"),
                new Reference("库存", "select count(s) from InventoryStock s where s.item.itemId = :itemId"),
                new Reference("库存流水", "select count(sm) from StockMovement sm where sm.item.itemId = :itemId"),
                new Reference("供应商供货", "select count(ms) from MaterialSupplier ms where ms.catalogItem.itemId = :itemId"),
                new Reference("供应商采购价", "select count(pp) from PartnerPrice pp where pp.item.itemId = :itemId"));
        for (Reference reference : references) {
            long count = entityManager.createQuery(reference.query(), Long.class)
                    .setParameter("itemId", item.getItemId())
                    .getSingleResult();
            if (count > 0) {
                return reference.label();
            }
        }
        return null;
    }

    private record Reference(String label, String query) { }
}
