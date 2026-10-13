package com.th4.edge.decoboon.project.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 增减项可变更来源与当前有效剩余投影（DEC-055 / BR-041 / DEC-068）。
 *
 * <p>只读视图：把 {@code ContractChangeRangeContext.RangeSnapshot} 的来源清单与
 * 「当前有效剩余 / 待审批占用 / 可变更量」下发给 App 与 Web，供录入辅助与前端预校验。
 * 服务端仍是唯一权威：提交与审批时一律重新投影，前端展示值不得作为提交依据。</p>
 */
@Note("增减项可变更来源与当前有效剩余")
public class ContractChangeRange {

    @Note("合同")
    private ProjectContract contract;

    @Note("可变更来源清单")
    private List<Source> sources = new ArrayList<>();

    @Note("指定来源（按 sourceLineId 或 sourceChangeItemId 指定的那一条）")
    private Source selected;

    @Note("全部来源是否满足可变更条件")
    private Boolean confirmable;

    public ProjectContract getContract() { return contract; }
    public void setContract(ProjectContract contract) { this.contract = contract; }

    public List<Source> getSources() { return sources; }
    public void setSources(List<Source> sources) { this.sources = sources; }

    public Source getSelected() { return selected; }
    public void setSelected(Source selected) { this.selected = selected; }

    public Boolean getConfirmable() { return confirmable; }
    public void setConfirmable(Boolean confirmable) { this.confirmable = confirmable; }

    /** 单条可变更来源（原合同行或已生效变更目标）+ 服务端范围投影。 */
    @Note("可变更来源")
    public static class Source {

        @Note("来源分组：contract=原合同行，effective=已生效变更目标")
        private String group;

        @Note("原合同行ID")
        private Long sourceLineId;

        @Note("已生效变更目标ID")
        private Long sourceChangeItemId;

        @Note("变更单ID")
        private Long changeId;

        @Note("变更单号")
        private String changeNo;

        @Note("名称")
        private String name;

        @Note("分项类型")
        private String itemType;

        @Note("品牌")
        private String brand;

        @Note("规格")
        private String spec;

        @Note("区域（房屋区域名，原合同行自带；已生效变更目标取其来源行区域，无来源时为 null）")
        private String spaceName;

        @Note("单位文案（展示用；单位枚举名称，如 Area → m²）")
        private String unit;

        @Note("单位枚举键（供前端按既有映射转换展示文案，如 Area → ㎡）")
        private String unitKey;

        @Note("范围初始有效量")
        private BigDecimal initialQuantity;

        @Note("当前有效剩余量")
        private BigDecimal effectiveRemainingQuantity;

        @Note("待审批占用")
        private BigDecimal pendingReservedQuantity;

        @Note("可变更量 = 当前有效剩余 - 待审批占用")
        private BigDecimal availableQuantity;

        @Note("当前有效剩余金额")
        private BigDecimal effectiveRemainingAmount;

        @Note("当前有效单价")
        private BigDecimal currentUnitPrice;

        @Note("本条来源是否可变更")
        private Boolean confirmable;

        @Note("不可变更原因")
        private String blockReason;

        public String getGroup() { return group; }
        public void setGroup(String group) { this.group = group; }

        public Long getSourceLineId() { return sourceLineId; }
        public void setSourceLineId(Long sourceLineId) { this.sourceLineId = sourceLineId; }

        public Long getSourceChangeItemId() { return sourceChangeItemId; }
        public void setSourceChangeItemId(Long sourceChangeItemId) { this.sourceChangeItemId = sourceChangeItemId; }

        public Long getChangeId() { return changeId; }
        public void setChangeId(Long changeId) { this.changeId = changeId; }

        public String getChangeNo() { return changeNo; }
        public void setChangeNo(String changeNo) { this.changeNo = changeNo; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }

        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }

        public String getSpec() { return spec; }
        public void setSpec(String spec) { this.spec = spec; }

        public String getSpaceName() { return spaceName; }
        public void setSpaceName(String spaceName) { this.spaceName = spaceName; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public String getUnitKey() { return unitKey; }
        public void setUnitKey(String unitKey) { this.unitKey = unitKey; }

        public BigDecimal getInitialQuantity() { return initialQuantity; }
        public void setInitialQuantity(BigDecimal initialQuantity) { this.initialQuantity = initialQuantity; }

        public BigDecimal getEffectiveRemainingQuantity() { return effectiveRemainingQuantity; }
        public void setEffectiveRemainingQuantity(BigDecimal effectiveRemainingQuantity) { this.effectiveRemainingQuantity = effectiveRemainingQuantity; }

        public BigDecimal getPendingReservedQuantity() { return pendingReservedQuantity; }
        public void setPendingReservedQuantity(BigDecimal pendingReservedQuantity) { this.pendingReservedQuantity = pendingReservedQuantity; }

        public BigDecimal getAvailableQuantity() { return availableQuantity; }
        public void setAvailableQuantity(BigDecimal availableQuantity) { this.availableQuantity = availableQuantity; }

        public BigDecimal getEffectiveRemainingAmount() { return effectiveRemainingAmount; }
        public void setEffectiveRemainingAmount(BigDecimal effectiveRemainingAmount) { this.effectiveRemainingAmount = effectiveRemainingAmount; }

        public BigDecimal getCurrentUnitPrice() { return currentUnitPrice; }
        public void setCurrentUnitPrice(BigDecimal currentUnitPrice) { this.currentUnitPrice = currentUnitPrice; }

        public Boolean getConfirmable() { return confirmable; }
        public void setConfirmable(Boolean confirmable) { this.confirmable = confirmable; }

        public String getBlockReason() { return blockReason; }
        public void setBlockReason(String blockReason) { this.blockReason = blockReason; }
    }
}
