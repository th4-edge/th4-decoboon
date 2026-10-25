package com.th4.edge.decoboon.catalog.context;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.model.detail.ApplianceItemDetail;
import com.th4.edge.decoboon.catalog.model.detail.ConstructionItemDetail;
import com.th4.edge.decoboon.catalog.model.detail.CustomFurnitureItemDetail;
import com.th4.edge.decoboon.catalog.model.detail.GeneralItemDetail;
import com.th4.edge.decoboon.catalog.model.detail.MaterialItemDetail;
import com.th4.edge.decoboon.catalog.model.detail.SoftFurnitureItemDetail;
import com.th4.edge.decoboon.catalog.repository.detail.ApplianceItemDetailRepository;
import com.th4.edge.decoboon.catalog.repository.detail.ConstructionItemDetailRepository;
import com.th4.edge.decoboon.catalog.repository.detail.CustomFurnitureItemDetailRepository;
import com.th4.edge.decoboon.catalog.repository.detail.GeneralItemDetailRepository;
import com.th4.edge.decoboon.catalog.repository.detail.MaterialItemDetailRepository;
import com.th4.edge.decoboon.catalog.repository.detail.SoftFurnitureItemDetailRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** 可售项目根与六类业务明细的同步上下文。 */
@Component
public class CatalogDetailContext {

    private final ConstructionItemDetailRepository constructionRepository;
    private final MaterialItemDetailRepository materialRepository;
    private final SoftFurnitureItemDetailRepository softRepository;
    private final CustomFurnitureItemDetailRepository customRepository;
    private final ApplianceItemDetailRepository applianceRepository;
    private final GeneralItemDetailRepository generalRepository;

    public CatalogDetailContext(ConstructionItemDetailRepository constructionRepository,
                                MaterialItemDetailRepository materialRepository,
                                SoftFurnitureItemDetailRepository softRepository,
                                CustomFurnitureItemDetailRepository customRepository,
                                ApplianceItemDetailRepository applianceRepository,
                                GeneralItemDetailRepository generalRepository) {
        this.constructionRepository = constructionRepository;
        this.materialRepository = materialRepository;
        this.softRepository = softRepository;
        this.customRepository = customRepository;
        this.applianceRepository = applianceRepository;
        this.generalRepository = generalRepository;
    }

    @Transactional
    public void sync(CatalogItem item) {
        if (item == null || item.getItemId() == null || item.getType() == null) return;
        switch (item.getType()) {
            case Construction -> syncConstruction(item);
            case Material -> syncMaterial(item);
            case Soft -> syncSoft(item);
            case Custom -> syncCustom(item);
            case Appliance -> syncAppliance(item);
            case General -> syncGeneral(item);
        }
    }

    /** 撤销导入批次时删除该项目的类型明细。 */
    @Transactional
    public void remove(CatalogItem item) {
        if (item == null || item.getItemId() == null || item.getType() == null) return;
        switch (item.getType()) {
            case Construction -> constructionRepository.findByItem_ItemId(item.getItemId())
                    .ifPresent(constructionRepository::delete);
            case Material -> materialRepository.findByItem_ItemId(item.getItemId())
                    .ifPresent(materialRepository::delete);
            case Soft -> softRepository.findByItem_ItemId(item.getItemId())
                    .ifPresent(softRepository::delete);
            case Custom -> customRepository.findByItem_ItemId(item.getItemId())
                    .ifPresent(customRepository::delete);
            case Appliance -> applianceRepository.findByItem_ItemId(item.getItemId())
                    .ifPresent(applianceRepository::delete);
            case General -> generalRepository.findByItem_ItemId(item.getItemId())
                    .ifPresent(generalRepository::delete);
        }
    }

    /** 读取类型明细到传输字段，用于变更比较与完整修订快照。 */
    @Transactional(readOnly = true)
    public void hydrate(CatalogItem item) {
        if (item == null || item.getItemId() == null || item.getType() == null) return;
        switch (item.getType()) {
            case Construction -> constructionRepository.findByItem_ItemId(item.getItemId())
                    .ifPresent(detail -> item.setDurationDays(detail.getDurationDays()));
            case Material -> materialRepository.findByItem_ItemId(item.getItemId())
                    .ifPresent(detail -> {
                        item.setLossRate(detail.getLossRate());
                        item.setMinimumOrder(detail.getMinimumOrder());
                    });
            case Soft -> softRepository.findByItem_ItemId(item.getItemId())
                    .ifPresent(detail -> item.setLeadTimeDays(detail.getLeadTimeDays()));
            case Custom -> customRepository.findByItem_ItemId(item.getItemId())
                    .ifPresent(detail -> {
                        item.setBoard(detail.getBoard());
                        item.setCountertop(detail.getCountertop());
                    });
            case Appliance -> applianceRepository.findByItem_ItemId(item.getItemId())
                    .ifPresent(detail -> item.setWarrantyMonths(detail.getWarrantyMonths()));
            case General -> { }
        }
    }

    private void syncConstruction(CatalogItem item) {
        ConstructionItemDetail detail = constructionRepository.findByItem_ItemId(item.getItemId())
                .orElseGet(ConstructionItemDetail::new);
        detail.setCompany(item.getCompany());
        detail.setItem(item);
        detail.setStage(item.getCategory());
        detail.setMeasurementFormula(item.getFormula());
        detail.setCraft(item.getCraft());
        detail.setCriterion(item.getCriterion());
        detail.setConcealed(text(item).contains("隐蔽") || text(item).contains("防水") || text(item).contains("水电"));
        if (item.getDurationDays() != null) detail.setDurationDays(item.getDurationDays());
        constructionRepository.save(detail);
    }

    private void syncMaterial(CatalogItem item) {
        MaterialItemDetail detail = materialRepository.findByItem_ItemId(item.getItemId()).orElseGet(MaterialItemDetail::new);
        detail.setCompany(item.getCompany());
        detail.setItem(item);
        detail.setModel(item.getSpec());
        detail.setLossRate(item.getLossRate() == null ? BigDecimal.ZERO : item.getLossRate());
        detail.setMinimumOrder(item.getMinimumOrder() == null ? BigDecimal.ONE : item.getMinimumOrder());
        item.setLossRate(detail.getLossRate());
        item.setMinimumOrder(detail.getMinimumOrder());
        materialRepository.save(detail);
    }

    private void syncSoft(CatalogItem item) {
        SoftFurnitureItemDetail detail = softRepository.findByItem_ItemId(item.getItemId()).orElseGet(SoftFurnitureItemDetail::new);
        detail.setCompany(item.getCompany());
        detail.setItem(item);
        detail.setDimensions(item.getSpec());
        if (item.getLeadTimeDays() != null) detail.setLeadTimeDays(item.getLeadTimeDays());
        softRepository.save(detail);
    }

    private void syncCustom(CatalogItem item) {
        CustomFurnitureItemDetail detail = customRepository.findByItem_ItemId(item.getItemId()).orElseGet(CustomFurnitureItemDetail::new);
        detail.setCompany(item.getCompany());
        detail.setItem(item);
        detail.setCabinetType(item.getCategory());
        detail.setParametricFormula(item.getFormula());
        if (item.getBoard() != null) detail.setBoard(item.getBoard());
        if (item.getCountertop() != null) detail.setCountertop(item.getCountertop());
        customRepository.save(detail);
    }

    private void syncAppliance(CatalogItem item) {
        ApplianceItemDetail detail = applianceRepository.findByItem_ItemId(item.getItemId()).orElseGet(ApplianceItemDetail::new);
        detail.setCompany(item.getCompany());
        detail.setItem(item);
        detail.setModel(item.getSpec());
        if (item.getWarrantyMonths() != null) detail.setWarrantyMonths(item.getWarrantyMonths());
        applianceRepository.save(detail);
    }

    private void syncGeneral(CatalogItem item) {
        GeneralItemDetail detail = generalRepository.findByItem_ItemId(item.getItemId()).orElseGet(GeneralItemDetail::new);
        detail.setCompany(item.getCompany());
        detail.setItem(item);
        detail.setFeeType(item.getCategory());
        generalRepository.save(detail);
    }

    private String text(CatalogItem item) {
        return ((item.getName() == null ? "" : item.getName()) + " " + (item.getCraft() == null ? "" : item.getCraft())).toLowerCase();
    }
}
