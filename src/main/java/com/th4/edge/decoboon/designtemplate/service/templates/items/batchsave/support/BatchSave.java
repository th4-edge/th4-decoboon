package com.th4.edge.decoboon.designtemplate.service.templates.items.batchsave.support;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateAccessContext;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateFormulaContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateItem;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateItemRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRoomRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateVersionRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 设计模版分项批量保存。
 *
 * <p>前端「模版分项配置」一次提交整张草稿表：新增/更新行与删除行在同一事务内完成。
 * 先对整批做版本、区域、项目库、公式语法与单位维度校验，全部通过后才写入；
 * 任一行不合法整批拒绝，避免出现“只保存了一部分却提示成功”的静默丢行。</p>
 */
@Note("设计模版分项批量保存")
@Description("整批保存草稿版本的模版分项，新增/更新/删除同事务，任一行失败整批回滚")
@Properties(allow = {
        @AllowProperties(model = DesignTemplate.class,
                properties = {"templateId", "scope", "status", "currentVersion", "sourceTemplate", "remark",
                        "rooms", "items", "owner"}),
        @AllowProperties(model = DesignTemplateVersion.class,
                properties = {"versionId", "version", "status", "name", "houseType", "areaMin", "areaMax",
                        "tags", "publishedAt", "publishedBy"}),
        @AllowProperties(model = DesignTemplateItem.class,
                properties = {"itemId", "roomKey", "quantityFormula", "defaultQuantity", "sortNo", "remark",
                        "catalogItem"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "name", "unit", "pricingMode", "category", "brand", "spec",
                        "materialPrice", "manualPrice", "fixedTotalPrice", "formula", "craft", "criterion"}),
        @AllowProperties(model = Unit.class, properties = {"name", "value"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("design/template/items/batchsave")
@Scope(ScopeTypes.PROTOTYPE)
public class BatchSave implements com.th4.edge.decoboon.designtemplate.service.templates.items.batchsave.BatchSave {

    @AutoInject
    private Long versionId;

    @AutoInject
    private List<Long> deletedItemIds;

    @AutoInject
    private List<DesignTemplateItem> items;

    @User
    private Employee employee;

    private final DesignTemplateVersionRepository versionRepository;
    private final DesignTemplateItemRepository itemRepository;
    private final DesignTemplateRoomRepository roomRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final DesignTemplateFormulaContext formulaContext;
    private final DesignTemplateAccessContext accessContext;

    public BatchSave(DesignTemplateVersionRepository versionRepository,
                     DesignTemplateItemRepository itemRepository,
                     DesignTemplateRoomRepository roomRepository,
                     CatalogItemRepository catalogItemRepository,
                     DesignTemplateFormulaContext formulaContext,
                     DesignTemplateAccessContext accessContext) {
        this.versionRepository = versionRepository;
        this.itemRepository = itemRepository;
        this.roomRepository = roomRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.formulaContext = formulaContext;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public DesignTemplate call(DesignTemplate ignored) {
        Employee me = accessContext.requireEmployee(employee);
        Long comId = me.getCompany().getComId();
        if (versionId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "versionId不能为空"));
        }
        DesignTemplateVersion version = versionRepository.findByVersionIdAndCompany_ComId(versionId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版版本不存在")));
        DesignTemplate template = version.getTemplate();
        accessContext.requireMaintain(me, template);
        if (version.getStatus() != DesignTemplate.Status.Draft) {
            throw new DefinedException(new DefaultErrorCode("400", "已发布版本不可修改，请新建版本"));
        }

        List<DesignTemplateItem> existing = itemRepository
                .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(versionId, comId);
        Map<Long, DesignTemplateItem> existingById = new HashMap<>();
        for (DesignTemplateItem item : existing) {
            existingById.put(item.getItemId(), item);
        }
        Set<String> roomKeys = new HashSet<>();
        for (DesignTemplateRoom room : roomRepository
                .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(versionId, comId)) {
            roomKeys.add(room.getRoomKey());
        }

        List<DesignTemplateItem> toDelete = prepareDeletions(existingById);
        List<PreparedRow> toWrite = prepareRows(version, comId, existingById, roomKeys);

        // 全部校验通过后才写入；任一行失败整批回滚。
        for (DesignTemplateItem doomed : toDelete) {
            itemRepository.delete(doomed);
        }
        for (PreparedRow row : toWrite) {
            applyRow(row, version, me);
            itemRepository.save(row.target());
        }

        template.setItems(itemRepository.findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(versionId, comId));
        return template;
    }

    private List<DesignTemplateItem> prepareDeletions(Map<Long, DesignTemplateItem> existingById) {
        List<DesignTemplateItem> result = new ArrayList<>();
        if (deletedItemIds == null) {
            return result;
        }
        for (Long itemId : deletedItemIds) {
            if (itemId == null) {
                continue;
            }
            DesignTemplateItem item = existingById.get(itemId);
            if (item == null) {
                throw new DefinedException(new DefaultErrorCode("404", "模版分项不存在或不属于当前版本"));
            }
            result.add(item);
        }
        return result;
    }

    private List<PreparedRow> prepareRows(DesignTemplateVersion version,
                                          Long comId,
                                          Map<Long, DesignTemplateItem> existingById,
                                          Set<String> roomKeys) {
        List<PreparedRow> prepared = new ArrayList<>();
        List<DesignTemplateItem> incoming = items == null ? List.of() : items;
        for (DesignTemplateItem row : incoming) {
            DesignTemplateItem target;
            if (row.getItemId() == null) {
                target = new DesignTemplateItem();
            } else {
                target = existingById.get(row.getItemId());
                if (target == null) {
                    throw new DefinedException(new DefaultErrorCode("404", "模版分项不存在或不属于当前版本"));
                }
            }

            Long catalogId = row.getCatalogItem() == null ? null : row.getCatalogItem().getItemId();
            if (catalogId == null) {
                throw new DefinedException(new DefaultErrorCode("400", "必须选择报价项目库条目"));
            }
            CatalogItem catalog = catalogItemRepository.findByItemIdAndCompany_ComId(catalogId, comId)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价项目库条目不存在")));
            if (!Boolean.TRUE.equals(catalog.getActive())) {
                throw new DefinedException(new DefaultErrorCode("400", "报价项目库条目已停用，请更换后保存"));
            }

            String roomKey = row.getRoomKey();
            if (roomKey != null && !roomKey.isBlank() && !roomKeys.contains(roomKey)) {
                throw new DefinedException(new DefaultErrorCode("400", "归属区域不存在：" + roomKey));
            }

            boolean hasFormula = row.getQuantityFormula() != null && !row.getQuantityFormula().isBlank();
            if (hasFormula) {
                formulaContext.validate(row.getQuantityFormula());
                formulaContext.requireDimension(catalog.getUnit(), row.getQuantityFormula());
            } else if (row.getDefaultQuantity() == null || row.getDefaultQuantity().signum() <= 0) {
                throw new DefinedException(new DefaultErrorCode("400", "分项必须填写工程量公式或大于 0 的固定数量"));
            }

            prepared.add(new PreparedRow(target, row, catalog, roomKey, hasFormula));
        }
        return prepared;
    }

    private void applyRow(PreparedRow row, DesignTemplateVersion version, Employee me) {
        DesignTemplateItem target = row.target();
        if (target.getItemId() == null) {
            target.setCompany(me.getCompany());
            target.setVersion(version);
        }
        target.setRoomKey(row.roomKey() == null || row.roomKey().isBlank() ? null : row.roomKey().trim());
        target.setCatalogItem(row.catalog());
        target.setQuantityFormula(row.hasFormula() ? row.source().getQuantityFormula().trim() : null);
        target.setDefaultQuantity(row.hasFormula() ? null : row.source().getDefaultQuantity());
        target.setSortNo(row.source().getSortNo() == null ? 0 : row.source().getSortNo());
        target.setRemark(row.source().getRemark());
    }

    private record PreparedRow(
            DesignTemplateItem target,
            DesignTemplateItem source,
            CatalogItem catalog,
            String roomKey,
            boolean hasFormula) {
    }
}
