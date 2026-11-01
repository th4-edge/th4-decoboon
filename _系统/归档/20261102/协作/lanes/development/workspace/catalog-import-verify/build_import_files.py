#!/usr/bin/env python3
"""按 V1.0 真实模板生成批量导入验证文件（不依赖 openpyxl，直接注入模板工作表 XML）。

用法: python3 build_import_files.py <模板 xlsx> <真实数据 tsv> <输出目录>
"""
import os
import re
import shutil
import sys
import zipfile

TYPE_CN = {
    "Construction": "施工", "Material": "主材", "Soft": "软装家具",
    "Custom": "全屋定制", "Appliance": "电器设备", "General": "综合其他",
}
UNIT_CN = {
    "Area": "㎡", "Count": "个", "Item": "项", "Place": "处", "Set": "套", "House": "户",
    "Metre": "米", "Centimeter": "厘米", "Step": "步", "Plate": "樘", "Block": "块",
    "Root": "根", "Hole": "扇", "Pcs": "台", "Stick": "支", "Grid": "格", "Sheet": "片",
}
MODE_CN = {
    "MaterialAndManual": "材料+人工", "MaterialOnly": "仅材料",
    "ManualOnly": "仅人工", "FixedTotal": "固定总价",
}
HEADERS = [
    "项目类型*", "项目编码", "项目分类*", "项目名称*", "品牌", "系列", "规格/型号", "计量单位*", "计价方式*",
    "材料单价（元）", "人工单价（元）", "固定总价（元）", "标准成本单价（元）", "施工工期（天，仅施工）",
    "损耗率（%，仅主材，填5代表5%）", "起订量（仅主材）", "交期（天，仅软装）", "保修月数（仅电器）",
    "柜体板材（仅全屋定制）", "台面（仅全屋定制）", "施工工艺", "验收标准", "项目说明",
]
COLUMNS = "ABCDEFGHIJKLMNOPQRSTUVW"


def esc(value):
    return (str(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace('"', "&quot;"))


def row_xml(row_no, values):
    cells = []
    for index, value in enumerate(values):
        if value is None or str(value).strip() == "":
            continue
        ref = f"{COLUMNS[index]}{row_no}"
        cells.append(f'<x:c r="{ref}" t="str"><x:v>{esc(value)}</x:v></x:c>')
    return f'<x:row r="{row_no}">' + "".join(cells) + "</x:row>"


def build(template, output, rows):
    with zipfile.ZipFile(template) as source:
        sheet = source.read("xl/worksheets/sheet2.xml").decode("utf-8")
        entries = {name: source.read(name) for name in source.namelist()}
    head_end = sheet.index("</x:row>") + len("</x:row>")
    data_rows = [row_xml(1, HEADERS)] + [row_xml(2 + index, row) for index, row in enumerate(rows)]
    sheet = sheet[:0] + re.sub(r"<x:row[^>]*>.*?</x:row>", "", sheet) if False else sheet
    # 保留行 1..最后一行结构不必要，直接重建 sheetData
    sheet_start = sheet.index("<x:sheetData>") + len("<x:sheetData>")
    sheet_end = sheet.index("</x:sheetData>")
    new_sheet = sheet[:sheet_start] + "".join(data_rows) + sheet[sheet_end:]
    entries["xl/worksheets/sheet2.xml"] = new_sheet.encode("utf-8")
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as target:
        for name, payload in entries.items():
            target.writestr(name, payload)
    print(f"built {output} rows={len(rows)}")


def clean_rows(tsv_path):
    rows = []
    for index, line in enumerate(open(tsv_path, encoding="utf-8")):
        code, type_, category, name, brand, series, spec, unit, mode, material, manual, cost = line.rstrip("\n").split("\t")
        if index >= 550:
            break
        # 历史数据中的固定总价存在材料/人工列，V1.0 模板要求写入独立固定总价列
        fixed_total = ""
        if mode == "FixedTotal":
            fixed_total = f"{float(material or 0) + float(manual or 0):.2f}"
            material = ""
            manual = ""
        rows.append([
            TYPE_CN.get(type_, "综合其他"), f"IMP-{code}", category or "其他", name,
            "" if brand == "NULL" else brand, "" if series == "NULL" else series,
            "" if spec == "NULL" else spec, UNIT_CN.get(unit, "项"), MODE_CN.get(mode, "材料+人工"),
            material, manual, fixed_total, cost, "", "", "", "", "", "", "",
            "批量导入验证", "", "",
        ])
    return rows


def main():
    template, tsv, out_dir = sys.argv[1], sys.argv[2], sys.argv[3]
    os.makedirs(out_dir, exist_ok=True)
    rows = clean_rows(tsv)
    build(template, os.path.join(out_dir, "clean-550.xlsx"), rows)
    # 固定总价行：仅固定总价列有值，材料/人工为空
    fixed = list(rows[0])
    fixed[0], fixed[1], fixed[3] = "全屋定制", "IMP-FIX-0001", "批量验证固定总价橱柜"
    fixed[8], fixed[9], fixed[10], fixed[11] = "固定总价", "", "", "1280"
    fixed[12] = ""          # 成本待维护
    fixed[18], fixed[19] = "PET门板", "石英石台面"
    # 成本空白行 + 重复编码行 + 错误单位行 + 编码冲突行（沿用真实 MH-CON-5）
    mixed = [
        ["施工", "IMP-MIX-0001", "拆除", "批量验证成本待维护", "", "", "", "㎡", "仅人工", "0", "10", "", "", "", "", "", "", "", "", "", "", "", ""],
        fixed,
        ["施工", "IMP-MIX-0001", "拆除", "批量验证重复编码", "", "", "", "㎡", "仅人工", "0", "10", "", "", "", "", "", "", "", "", "", "", "", ""],
        ["施工", "IMP-MIX-0002", "拆除", "批量验证单位错误", "", "", "", "尺", "仅人工", "0", "10", "", "", "", "", "", "", "", "", "", "", "", ""],
        ["施工", "MH-CON-5", "拆除", "批量验证编码冲突", "", "", "", "㎡", "仅人工", "0", "10", "", "", "", "", "", "", "", "", "", "", "", ""],
    ]
    build(template, os.path.join(out_dir, "mixed.xlsx"), mixed)
    # 非法表头：把“项目名称*”改成“项目称呼*”
    bad = os.path.join(out_dir, "clean-550.xlsx")
    with zipfile.ZipFile(bad) as source:
        entries = {name: source.read(name) for name in source.namelist()}
    sheet = entries["xl/worksheets/sheet2.xml"].decode("utf-8").replace("项目名称*", "项目称呼*", 1)
    entries["xl/worksheets/sheet2.xml"] = sheet.encode("utf-8")
    with zipfile.ZipFile(os.path.join(out_dir, "bad-header.xlsx"), "w", zipfile.ZIP_DEFLATED) as target:
        for name, payload in entries.items():
            target.writestr(name, payload)
    print("built bad-header.xlsx")


if __name__ == "__main__":
    main()
