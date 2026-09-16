from __future__ import annotations

from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = 1080, 1920
HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
APP_ROOT = REPO.parent.parent / "前端代码" / "th4-decoboon-app"
LOGO_DIR = APP_ROOT / "logo-export"

FONT_REGULAR = "/System/Library/Fonts/Hiragino Sans GB.ttc"
FONT_INDEX_REGULAR = 0
FONT_INDEX_BOLD = 2

PRIMARY = "#16A394"
PRIMARY_LIGHT = "#2BC4B4"
PRIMARY_DARK = "#0E8A7D"
PRIMARY_DEEP = "#096C64"
INK = "#17212B"
TEXT_2 = "#465260"
TEXT_3 = "#778390"
BORDER = "#DDE8E7"
WHITE = "#FFFFFF"
OFF_WHITE = "#F4FAF9"
DARK_BG = "#082B2B"
DARK_CARD = "#0E3A39"
WARNING = "#E67E22"
PURPLE = "#6958C8"


def rgb(value: str) -> tuple[int, int, int]:
    value = value.lstrip("#")
    return tuple(int(value[i : i + 2], 16) for i in (0, 2, 4))


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(
        FONT_REGULAR,
        size=size,
        index=FONT_INDEX_BOLD if bold else FONT_INDEX_REGULAR,
    )


def gradient(size: tuple[int, int], start: str, end: str, vertical: bool = True) -> Image.Image:
    a, b = rgb(start), rgb(end)
    w, h = size
    image = Image.new("RGB", size)
    draw = ImageDraw.Draw(image)
    limit = h if vertical else w
    for i in range(limit):
        t = i / max(1, limit - 1)
        color = tuple(round(a[j] * (1 - t) + b[j] * t) for j in range(3))
        if vertical:
            draw.line((0, i, w, i), fill=color)
        else:
            draw.line((i, 0, i, h), fill=color)
    return image.convert("RGBA")


def overlay_grid(base: Image.Image, dark: bool = False, step: int = 72) -> None:
    layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    color = (255, 255, 255, 11) if dark else (22, 163, 148, 17)
    for x in range(0, W, step):
        draw.line((x, 0, x, H), fill=color, width=1)
    for y in range(0, H, step):
        draw.line((0, y, W, y), fill=color, width=1)
    base.alpha_composite(layer)


def add_glow(base: Image.Image, center: tuple[int, int], radius: int, color: str, alpha: int = 55) -> None:
    layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    c = rgb(color)
    x, y = center
    draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=(*c, alpha))
    layer = layer.filter(ImageFilter.GaussianBlur(radius // 3))
    base.alpha_composite(layer)


def rounded_shadow(
    base: Image.Image,
    box: tuple[int, int, int, int],
    radius: int,
    fill: str | tuple[int, int, int, int],
    *,
    shadow_alpha: int = 35,
    shadow_offset: tuple[int, int] = (0, 16),
    blur: int = 18,
    outline: str | None = None,
    outline_width: int = 1,
) -> None:
    shadow = Image.new("RGBA", base.size, (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow)
    shifted = (
        box[0] + shadow_offset[0],
        box[1] + shadow_offset[1],
        box[2] + shadow_offset[0],
        box[3] + shadow_offset[1],
    )
    sdraw.rounded_rectangle(shifted, radius=radius, fill=(0, 0, 0, shadow_alpha))
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur))
    base.alpha_composite(shadow)

    layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    f = rgb(fill[1:]) + (fill[0],) if isinstance(fill, tuple) else (*rgb(fill), 255)
    draw.rounded_rectangle(box, radius=radius, fill=f, outline=rgb(outline) + (200,) if outline else None, width=outline_width)
    base.alpha_composite(layer)


def text(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int],
    value: str,
    size: int,
    *,
    color: str = INK,
    bold: bool = False,
    anchor: str = "lt",
    spacing: int = 0,
) -> None:
    fnt = font(size, bold)
    if "\n" in value:
        draw.multiline_text(
            xy,
            value,
            font=fnt,
            fill=rgb(color),
            spacing=spacing,
            align="left" if anchor.startswith("l") else "center",
        )
    else:
        draw.text(xy, value, font=fnt, fill=rgb(color), anchor=anchor)


def pill(
    base: Image.Image,
    xy: tuple[int, int],
    label: str,
    *,
    size: int = 26,
    fill: str = PRIMARY,
    text_color: str = WHITE,
    pad_x: int = 28,
    pad_y: int = 15,
    outline: str | None = None,
) -> tuple[int, int, int, int]:
    fnt = font(size, True)
    bbox = ImageDraw.Draw(base).textbbox((0, 0), label, font=fnt)
    width = bbox[2] - bbox[0] + pad_x * 2
    height = bbox[3] - bbox[1] + pad_y * 2
    x, y = xy
    box = (x, y, x + width, y + height)
    rounded_shadow(
        base,
        box,
        height // 2,
        fill,
        shadow_alpha=0 if outline else 22,
        shadow_offset=(0, 7),
        blur=10,
        outline=outline,
    )
    d = ImageDraw.Draw(base)
    text(d, (x + width // 2, y + height // 2), label, size, color=text_color, bold=True, anchor="mm")
    return box


def paste_logo(base: Image.Image, dark: bool, x: int = 82, y: int = 72) -> None:
    if dark:
        logo = Image.open(LOGO_DIR / "logo-mark-white@2x.png").convert("RGBA")
        logo.thumbnail((96, 96), Image.Resampling.LANCZOS)
        base.alpha_composite(logo, (x, y))
        d = ImageDraw.Draw(base)
        text(d, (x + 118, y + 17), "装得清", 52, color=WHITE, bold=True)
        text(d, (x + 120, y + 72), "装企数字化协同管理平台", 20, color="#BFD7D4")
    else:
        logo = Image.open(LOGO_DIR / "logo-horizontal-color@2x.png").convert("RGBA")
        logo.thumbnail((376, 142), Image.Resampling.LANCZOS)
        base.alpha_composite(logo, (x, y))


def header(base: Image.Image, code: str, category: str, dark: bool) -> None:
    paste_logo(base, dark)
    pill(
        base,
        (760, 80),
        code,
        size=24,
        fill=("#123C3A" if dark else WHITE),
        text_color=("#D7F5F1" if dark else PRIMARY_DARK),
        outline=("#3A6663" if dark else BORDER),
        pad_x=22,
    )
    d = ImageDraw.Draw(base)
    text(d, (82, 188), category, 24, color=("#9FC7C3" if dark else TEXT_3), bold=True)


def footer(base: Image.Image, dark: bool, note: str = "internal_draft · 仅限内部评审") -> None:
    d = ImageDraw.Draw(base)
    y = 1815
    d.line((82, y - 40, 998, y - 40), fill=(*rgb("#315B58"), 150) if dark else (*rgb(BORDER), 220), width=1)
    text(d, (82, y - 12), note, 21, color=("#8FB5B2" if dark else TEXT_3), bold=False)
    text(d, (998, y - 12), "非发布版本", 21, color=("#8FB5B2" if dark else TEXT_3), anchor="rt")


def title_block(draw: ImageDraw.ImageDraw, title: str, subtitle: str, dark: bool) -> None:
    text(draw, (82, 282), title, 82, color=(WHITE if dark else INK), bold=True, spacing=12)
    text(draw, (86, 512), subtitle, 30, color=("#B9D8D5" if dark else TEXT_2), bold=False, spacing=7)


def arrow(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int], color: str = PRIMARY, width: int = 5) -> None:
    draw.line((*start, *end), fill=rgb(color), width=width)
    x1, y1 = start
    x2, y2 = end
    import math

    angle = math.atan2(y2 - y1, x2 - x1)
    length = 18
    spread = 0.55
    p1 = (x2 - length * math.cos(angle - spread), y2 - length * math.sin(angle - spread))
    p2 = (x2 - length * math.cos(angle + spread), y2 - length * math.sin(angle + spread))
    draw.polygon([end, p1, p2], fill=rgb(color))


def node(
    base: Image.Image,
    box: tuple[int, int, int, int],
    label: str,
    sub: str | None,
    *,
    dark: bool = False,
    accent: bool = False,
    icon: str | None = None,
) -> None:
    fill = PRIMARY if accent else (DARK_CARD if dark else WHITE)
    outline = None if accent else ("#315B58" if dark else BORDER)
    rounded_shadow(base, box, 24, fill, shadow_alpha=38 if not dark else 25, shadow_offset=(0, 13), blur=16, outline=outline)
    d = ImageDraw.Draw(base)
    x1, y1, x2, y2 = box
    label_color = WHITE if accent or dark else INK
    sub_color = "#D7F5F1" if accent else ("#91BAB6" if dark else TEXT_3)
    if icon:
        circle_fill = (255, 255, 255, 50) if accent else (22, 163, 148, 30)
        d.ellipse((x1 + 22, y1 + 24, x1 + 68, y1 + 70), fill=circle_fill)
        text(d, (x1 + 45, y1 + 47), icon, 24, color=(WHITE if accent else PRIMARY), bold=True, anchor="mm")
    text(d, (x1 + (88 if icon else 28), y1 + 24), label, 30, color=label_color, bold=True)
    if sub:
        text(d, (x1 + (88 if icon else 28), y1 + 70), sub, 21, color=sub_color)


def canvas_light(code: str, category: str) -> Image.Image:
    base = gradient((W, H), "#FAFDFC", "#EDF7F5")
    add_glow(base, (1020, -50), 520, PRIMARY, 34)
    overlay_grid(base, dark=False, step=72)
    header(base, code, category, dark=False)
    return base


def canvas_dark(code: str, category: str) -> Image.Image:
    base = gradient((W, H), "#0C3B39", "#061C23")
    add_glow(base, (980, 240), 470, PRIMARY_LIGHT, 46)
    add_glow(base, (-120, 1380), 520, PRIMARY_DARK, 32)
    overlay_grid(base, dark=True, step=72)
    header(base, code, category, dark=True)
    return base


def cover_t01() -> Image.Image:
    base = canvas_light("T01", "产品定位 · internal_draft")
    d = ImageDraw.Draw(base)
    title_block(d, "装得清\n是什么、不是什么", "先看经营链路是否连得上，再判断系统是否适合", False)
    pill(base, (82, 620), "官方一句话待产品负责人确认", size=22, fill="#FFF8EA", text_color="#A35C11", outline="#F1D7AC")
    rounded_shadow(base, (82, 760, 998, 1390), 42, WHITE, shadow_alpha=30, blur=24, outline=BORDER)
    text(d, (126, 808), "不是两类工具", 25, color=TEXT_3, bold=True)
    node(base, (126, 866, 500, 1006), "设计表达工具", "解决效果呈现", dark=False)
    node(base, (580, 866, 954, 1006), "传统进销存", "解决货品记录", dark=False)
    arrow(d, (513, 936), (567, 936), color="#B7CAC8", width=4)
    text(d, (126, 1065), "而是", 25, color=PRIMARY_DARK, bold=True)
    node(base, (126, 1120, 954, 1295), "经营型数字化系统", "客户 → 房屋 → 设计 → 报价 → 合同 → 项目 → 材料 → 财务", dark=False, accent=True, icon="J")
    footer(base, False)
    return base


def cover_t02() -> Image.Image:
    base = canvas_dark("T02", "经营主链路 · internal_draft")
    d = ImageDraw.Draw(base)
    title_block(d, "一张图看懂\n家装经营主链路", "客户与房屋是根，报价是经营中枢，履约与资金继续承接", True)
    pill(base, (82, 620), "产品设计示意 · 非真实客户案例", size=22, fill="#123C3A", text_color="#D7F5F1", outline="#3A6663")
    panel = (70, 748, 1010, 1460)
    rounded_shadow(base, panel, 42, "#0B3433", shadow_alpha=44, blur=26, outline="#315B58")
    labels = [
        ("客户", "经营关系"),
        ("房屋", "具体执行"),
        ("设计", "方案与区域"),
        ("报价", "版本与基线"),
        ("合同", "确认与变更"),
        ("项目", "阶段与任务"),
        ("材料", "计划与到场"),
        ("财务", "应收与台账"),
    ]
    positions = [
        (130, 823), (130, 990), (130, 1157), (130, 1324),
        (580, 823), (580, 990), (580, 1157), (580, 1324),
    ]
    for i, ((name, sub), (x, y)) in enumerate(zip(labels, positions)):
        accent = name in {"房屋", "报价"}
        node(base, (x, y, x + 370, y + 116), name, sub, dark=True, accent=accent, icon=str(i + 1))
        if i in (0, 2, 4, 6):
            arrow(d, (500, y + 58), (570, y + 58), color="#4D8883", width=4)
    arrow(d, (315, 940), (315, 985), color=PRIMARY_LIGHT, width=4)
    arrow(d, (315, 1107), (315, 1152), color=PRIMARY_LIGHT, width=4)
    arrow(d, (315, 1274), (315, 1319), color=PRIMARY_LIGHT, width=4)
    arrow(d, (765, 940), (765, 985), color=PRIMARY_LIGHT, width=4)
    arrow(d, (765, 1107), (765, 1152), color=PRIMARY_LIGHT, width=4)
    arrow(d, (765, 1274), (765, 1319), color=PRIMARY_LIGHT, width=4)
    text(d, (88, 1510), "关键交接：方案 → 报价 · 报价 → 合同 · 合同 → 项目与财务", 25, color="#B9D8D5", bold=True)
    footer(base, True)
    return base


def cover_t03() -> Image.Image:
    base = canvas_light("T03", "报价中枢 · internal_draft")
    d = ImageDraw.Draw(base)
    title_block(d, "报价不只是\n一张价格表", "版本、快照与成本基线，承接合同、项目和财务", False)
    pill(base, (82, 620), "产品设计示意 · 金额需使用独立演示数据", size=22, fill="#E9F7F5", text_color=PRIMARY_DEEP, outline="#B7E3DD")
    source_labels = [("客户", ""), ("房屋", ""), ("设计方案", ""), ("区域工程量", "")]
    for i, (name, sub) in enumerate(source_labels):
        x = 82 + i * 234
        node(base, (x, 760, x + 205, 874), name, sub or None, dark=False, icon=str(i + 1))
        arrow(d, (x + 102, 876), (x + 102, 922), color="#9DCBC5", width=4)
    rounded_shadow(base, (82, 940, 998, 1260), 38, WHITE, shadow_alpha=30, blur=24, outline="#B7E3DD")
    text(d, (126, 990), "报价版本", 27, color=TEXT_3, bold=True)
    text(d, (126, 1032), "V1 / V2 / …", 62, color=INK, bold=True)
    pill(base, (680, 1000), "快照冻结", size=22, fill="#E9F7F5", text_color=PRIMARY_DEEP)
    d.line((126, 1150, 954, 1150), fill=rgb(BORDER), width=2)
    for i, label in enumerate(["报价行", "价格组件", "成本与公式"]):
        text(d, (126 + i * 285, 1180), label, 25, color=TEXT_2, bold=True)
    outputs = [("合同基线", "变更不改写历史"), ("项目执行", "引用同一基线"), ("财务台账", "保留资金事实")]
    for i, (name, sub) in enumerate(outputs):
        node(base, (82 + i * 306, 1350, 370 + i * 306, 1486), name, sub, dark=False, accent=i == 0, icon=str(i + 1))
    footer(base, False)
    return base


def cover_t04() -> Image.Image:
    base = canvas_dark("T04", "企业项目库 · internal_draft")
    d = ImageDraw.Draw(base)
    title_block(d, "三层分工\n别混成一张 Excel", "企业基础资料、设计选择、报价快照各归其位", True)
    pill(base, (82, 620), "537 条为联调数据 · 非行业标准库", size=22, fill="#3A2B17", text_color="#FFD79A", outline="#7A5B2B")
    layers = [
        ("01", "企业项目库", "施工 / 主材 / 软装 / 定制 / 电器 / 综合"),
        ("02", "设计选择", "在具体房屋、区域和方案中选择项目与工程量"),
        ("03", "报价快照", "冻结名称、规格、价格、公式与成本事实"),
    ]
    for i, (num, name, sub) in enumerate(layers):
        y = 790 + i * 245
        rounded_shadow(base, (82, y, 998, y + 196), 34, "#0F3A39", shadow_alpha=42, blur=24, outline="#315B58")
        pill(base, (118, y + 38), num, size=22, fill=PRIMARY, text_color=WHITE)
        text(d, (230, y + 32), name, 42, color=WHITE, bold=True)
        text(d, (230, y + 96), sub, 25, color="#A7CCC8")
        if i < 2:
            arrow(d, (540, y + 196), (540, y + 235), color=PRIMARY_LIGHT, width=5)
    text(d, (82, 1540), "设计师负责选择，不直接覆盖企业基础价格", 27, color="#D7F5F1", bold=True)
    footer(base, True)
    return base


def cover_t05() -> Image.Image:
    base = canvas_light("T05", "设计转化 · internal_draft")
    d = ImageDraw.Draw(base)
    title_block(d, "效果图之后\n还要交出去什么？", "诉求、方案、区域、选材、工程量与报价依据继续交接", False)
    pill(base, (82, 620), "联调样例 · 非真实客户案例", size=22, fill="#FFF4E8", text_color="#A35C11", outline="#F2D1AD")
    rounded_shadow(base, (82, 764, 452, 1180), 38, "#DFF1EE", shadow_alpha=28, blur=22, outline="#B7DCD7")
    d.rectangle((142, 824, 392, 1038), fill=rgb("#C4E5DF"))
    d.line((142, 1038, 392, 824), fill=rgb(PRIMARY_DARK), width=5)
    text(d, (267, 1085), "效果图", 34, color=INK, bold=True, anchor="mm")
    text(d, (267, 1132), "视觉表达", 23, color=TEXT_3, anchor="mm")
    arrow(d, (472, 970), (544, 970), color=PRIMARY, width=6)
    chain = [
        ("诉求 / 排斥点", "想要什么，不接受什么"),
        ("设计方案", "按版本组织"),
        ("区域尺寸", "落到实际空间"),
        ("设计分项", "选择与工程量"),
        ("报价版本", "快照与基线"),
        ("谈单材料", "结构化讲解"),
    ]
    for i, (name, sub) in enumerate(chain):
        y = 742 + i * 120
        node(base, (566, y, 998, y + 98), name, sub, dark=False, accent=i in {0, 4}, icon=str(i + 1))
        if i < len(chain) - 1:
            arrow(d, (782, y + 98), (782, y + 116), color="#9DCBC5", width=4)
    text(d, (82, 1375), "不自动出图 · 不自动成交 · 不评价设计效果", 25, color=TEXT_2, bold=True)
    footer(base, False)
    return base


def cover_t06() -> Image.Image:
    base = canvas_dark("T06", "项目闭环 · internal_draft")
    d = ImageDraw.Draw(base)
    title_block(d, "项目进度\n别只剩一个百分比", "阶段、任务、节点、日报和验收，共同解释过程事实", True)
    pill(base, (82, 620), "可追溯 ≠ 无风险", size=23, fill="#123C3A", text_color="#D7F5F1", outline="#3A6663")
    rounded_shadow(base, (82, 772, 998, 1455), 42, "#0B3433", shadow_alpha=44, blur=26, outline="#315B58")
    d.line((220, 872, 220, 1360), fill=rgb("#4D8883"), width=7)
    items = [
        ("阶段", "组织施工范围"),
        ("任务", "承接执行与来源"),
        ("节点", "保留过程事实"),
        ("日报", "连接现场材料与问题"),
        ("验收", "确认、驳回与整改"),
    ]
    for i, (name, sub) in enumerate(items):
        y = 825 + i * 112
        d.ellipse((198, y + 17, 242, y + 61), fill=rgb(PRIMARY if i in {2, 4} else PRIMARY_DARK), outline=rgb("#A8E4DD"), width=3)
        text(d, (276, y), name, 36, color=WHITE, bold=True)
        text(d, (276, y + 48), sub, 23, color="#A7CCC8")
    text(d, (820, 830), "%", 170, color="#1F5C58", bold=True, anchor="mt")
    pill(base, (700, 1070), "过程拆解", size=23, fill=PRIMARY, text_color=WHITE)
    pill(base, (670, 1160), "事实有上下文", size=23, fill="#123C3A", text_color="#D7F5F1", outline="#3A6663")
    pill(base, (655, 1250), "不承诺零延期", size=23, fill="#3A2B17", text_color="#FFD79A", outline="#7A5B2B")
    footer(base, True)
    return base


def cover_t07() -> Image.Image:
    base = canvas_light("T07", "客户与房屋 · internal_draft")
    d = ImageDraw.Draw(base)
    title_block(d, "一个客户\n不一定只有一套房", "客户关系与房屋执行分层，跟进和量房各自留痕", False)
    pill(base, (82, 620), "不含真实客户信息 · 产品设计示意", size=22, fill="#E9F7F5", text_color=PRIMARY_DEEP, outline="#B7E3DD")
    node(base, (82, 780, 424, 920), "客户", "经营关系", dark=False, accent=True, icon="C")
    arrow(d, (424, 850), (510, 850), color="#9DCBC5", width=5)
    text(d, (540, 792), "一个客户可关联多套房屋", 30, color=INK, bold=True)
    for i, label in enumerate(["房屋 A", "房屋 B"]):
        y = 858 + i * 144
        node(base, (540, y, 998, y + 120), label, "独立量房 / 设计 / 报价 / 履约", dark=False, icon=str(i + 1))
    rounded_shadow(base, (82, 1180, 998, 1450), 38, WHITE, shadow_alpha=28, blur=22, outline=BORDER)
    text(d, (126, 1228), "关系层状态", 29, color=INK, bold=True)
    text(d, (126, 1285), "客户表达经营关系", 24, color=TEXT_3)
    text(d, (570, 1228), "执行层状态", 29, color=INK, bold=True)
    text(d, (570, 1285), "房屋表达具体执行进度", 24, color=TEXT_3)
    d.line((518, 1230, 518, 1400), fill=rgb(BORDER), width=2)
    text(d, (82, 1510), "跟进记录只追加 · 转交同步归属 · 不编造转化率", 25, color=TEXT_2, bold=True)
    footer(base, False)
    return base


def cover_t08() -> Image.Image:
    base = canvas_dark("T08", "财务闭环 · internal_draft")
    d = ImageDraw.Draw(base)
    title_block(d, "合同生效后\n钱和项目怎么接", "合同基线连接项目履约、应收、应付和经营台账", True)
    pill(base, (82, 620), "能力边界待确认 · 金额需用演示数据", size=22, fill="#3A2B17", text_color="#FFD79A", outline="#7A5B2B")
    node(base, (300, 770, 780, 930), "合同基线", "报价确认后进入合同；变更不改写历史", dark=True, accent=True, icon="C")
    d.line((540, 930, 540, 1035), fill=rgb(PRIMARY_LIGHT), width=6)
    names = [
        ("项目履约", "阶段与任务"),
        ("应收", "收款核销"),
        ("应付", "付款核销"),
        ("台账", "项目资金事实"),
    ]
    for i, (name, sub) in enumerate(names):
        x = 65 + i * 252
        node(base, (x, 1080, x + 224, 1236), name, sub, dark=True, accent=i in {0, 1}, icon=str(i + 1))
        d.line((540, 1035, x + 112, 1035), fill=rgb("#4D8883"), width=5)
        d.line((x + 112, 1035, x + 112, 1069), fill=rgb("#4D8883"), width=5)
    rounded_shadow(base, (82, 1320, 998, 1515), 34, "#0F3A39", shadow_alpha=42, blur=24, outline="#315B58")
    text(d, (126, 1365), "结算条件", 29, color=WHITE, bold=True)
    text(d, (126, 1420), "项目完工 · 应收收清 · 应付付清", 27, color="#D7F5F1", bold=True)
    text(d, (778, 1420), "边界待确认", 23, color="#FFD79A", bold=True, anchor="rt")
    footer(base, True)
    return base


def write_contact_sheet(paths: Iterable[Path]) -> Path:
    thumbs = [Image.open(p).convert("RGB").resize((405, 720), Image.Resampling.LANCZOS) for p in paths]
    gap = 24
    sheet = Image.new("RGB", (4 * 405 + 5 * gap, 2 * 720 + 3 * gap), rgb("#EAF3F2"))
    for i, thumb in enumerate(thumbs):
        x = gap + (i % 4) * (405 + gap)
        y = gap + (i // 4) * (720 + gap)
        sheet.paste(thumb, (x, y))
    out = HERE / "T01-T08-cover-contact-sheet.png"
    sheet.save(out, quality=96)
    return out


def main() -> None:
    covers = {
        "T01": cover_t01(),
        "T02": cover_t02(),
        "T03": cover_t03(),
        "T04": cover_t04(),
        "T05": cover_t05(),
        "T06": cover_t06(),
        "T07": cover_t07(),
        "T08": cover_t08(),
    }
    paths: list[Path] = []
    for code, image in covers.items():
        path = HERE / f"{code}-cover-draft.png"
        image.convert("RGB").save(path, quality=96)
        paths.append(path)
    write_contact_sheet(paths)
    print("\n".join(str(p) for p in [*paths, HERE / "T01-T08-cover-contact-sheet.png"]))


if __name__ == "__main__":
    main()
