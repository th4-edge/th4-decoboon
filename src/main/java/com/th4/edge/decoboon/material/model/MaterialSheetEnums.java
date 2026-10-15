package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

import java.util.List;

/**
 * 主材下单作业单枚举投影（供 App/Web 选择器使用，避免前端硬编码品类）。
 *
 * <p>品类来自平台行业基线（L1）；状态与单位来自领域枚举。</p>
 */
@Note("主材下单枚举")
public class MaterialSheetEnums {

    /** 品类基线（code + 中文名）。 */
    public record SheetType(String code, String name) {
    }

    /** 枚举项（值 + 中文名）。 */
    public record Option(String value, String name) {
    }

    @Note("可选品类（平台行业基线）") private List<SheetType> sheetTypes;
    @Note("作业单状态") private List<Option> statuses;
    @Note("计量单位") private List<Option> units;

    public List<SheetType> getSheetTypes() { return sheetTypes; }
    public void setSheetTypes(List<SheetType> sheetTypes) { this.sheetTypes = sheetTypes; }
    public List<Option> getStatuses() { return statuses; }
    public void setStatuses(List<Option> statuses) { this.statuses = statuses; }
    public List<Option> getUnits() { return units; }
    public void setUnits(List<Option> units) { this.units = units; }
}
