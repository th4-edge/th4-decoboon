package com.th4.edge.decoboon.design.context;

import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.configure.RequirementKind;
import com.th4.edge.decoboon.design.configure.TalkSectionType;
import com.th4.edge.decoboon.design.configure.TalkStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.decoboon.design.model.TalkSection;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.design.repository.TalkRepository;
import com.th4.edge.decoboon.design.repository.TalkSectionRepository;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** 谈单 PPT 内容生成上下文。 */
@Note("谈单PPT生成上下文")
@Component
public class TalkContext {

    private static final List<DesignStatus> TALKABLE_STATUSES =
            List.of(DesignStatus.Ready, DesignStatus.Presented, DesignStatus.Selected);

    private final TalkRepository talkRepository;
    private final TalkSectionRepository sectionRepository;
    private final DesignRepository designRepository;
    private final DesignItemRepository designItemRepository;
    private final SpaceRepository spaceRepository;
    private final RequirementRepository requirementRepository;
    private final DesignContext designContext;
    private final TalkPptContext talkPptContext;

    public TalkContext(TalkRepository talkRepository,
                       TalkSectionRepository sectionRepository,
                       DesignRepository designRepository,
                       DesignItemRepository designItemRepository,
                       SpaceRepository spaceRepository,
                       RequirementRepository requirementRepository,
                       DesignContext designContext,
                       TalkPptContext talkPptContext) {
        this.talkRepository = talkRepository;
        this.sectionRepository = sectionRepository;
        this.designRepository = designRepository;
        this.designItemRepository = designItemRepository;
        this.spaceRepository = spaceRepository;
        this.requirementRepository = requirementRepository;
        this.designContext = designContext;
        this.talkPptContext = talkPptContext;
    }

    /**
     * 生成或重建谈单内容。
     *
     * <p>同一客户至少有两套可讲方案，且必须有设计报价，避免在方案不足时提前白嫖设计。</p>
     */
    @Transactional
    public Talk build(Talk talk, Design design, Quote quote) {
        if (design == null || design.getDesignId() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "主推方案不能为空"));
        }
        if (!TALKABLE_STATUSES.contains(design.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "主推方案尚未达到可谈单状态"));
        }
        Long comId = design.getCompany().getComId();
        Long customerId = design.getCustomer().getCustId();
        long readyCount = designRepository.countByCompany_ComIdAndCustomer_CustIdAndStatusIn(
                comId, customerId, TALKABLE_STATUSES);
        if (readyCount < 2) {
            throw new DefinedException(new DefaultErrorCode("400", "至少完成两套设计方案后才能生成谈单PPT"));
        }
        if (quote == null || quote.getQuoteId() == null || quote.getDesign() == null
                || !design.getDesignId().equals(quote.getDesign().getDesignId())) {
            throw new DefinedException(new DefaultErrorCode("400", "请先为主推方案生成设计报价"));
        }

        boolean replacing = talk.getTalkId() != null;
        if (!replacing) {
            talk.setNo("T" + System.currentTimeMillis());
            talk.setVersion(1);
        } else {
            talk.setVersion(talk.getVersion() == null ? 1 : talk.getVersion() + 1);
        }
        designContext.apply(talk, design);
        talk.setQuote(quote);
        talk.setTitle(talk.getTitle() == null || talk.getTitle().isBlank()
                ? design.getName() + (design.getName().endsWith("方案") ? "讲解" : "方案讲解")
                : talk.getTitle());
        talk.setCoverUrl(talk.getCoverUrl() == null ? design.getCoverUrl() : talk.getCoverUrl());
        talk.setStatus(TalkStatus.Generated);
        talk.setBuiltAt(LocalDateTime.now());
        Talk saved = talkRepository.save(talk);

        List<TalkSection> sections = generate(saved, design, quote);
        sectionRepository.deleteByTalk_TalkId(saved.getTalkId());
        sectionRepository.saveAll(sections);
        saved.setOutputUrl(talkPptContext.export(saved, sections));
        saved.setSections(sections);
        return talkRepository.save(saved);
    }

    private List<TalkSection> generate(Talk saved, Design design, Quote quote) {
        List<Requirement> requirements = requirementRepository
                .findByCompany_ComIdAndCustomer_CustIdOrderByRequirementIdDesc(
                        design.getCompany().getComId(), design.getCustomer().getCustId());
        List<Space> spaces = spaceRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId());
        List<DesignItem> items = designItemRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId());

        List<TalkSection> sections = new ArrayList<>();
        int[] sort = {0};
        sections.add(imageSection(saved, TalkSectionType.Cover, design.getName(),
                design.getCustomer().getCommunity(), copyImages(design.getCoverUrl()), sort));
        sections.add(textSection(saved, TalkSectionType.Catalog, "设计目录",
                "从量房问题到方案、材料与投入的完整讲解", catalog(), sort));
        sections.add(textSection(saved, TalkSectionType.Community, "小区景观",
                design.getCustomer().getCommunity(), List.of(), sort));
        sections.add(textSection(saved, TalkSectionType.Requirement, "装修诉求",
                "本页只讲客户亲自说过、在意或排斥的内容", requirementText(requirements), sort));
        sections.add(imageSection(saved, TalkSectionType.SitePhoto, "现场照片",
                "量房现场与原始状态", design.getSitePhotos(), sort));
        sections.add(imageSection(saved, TalkSectionType.Survey, "现场勘测",
                "尺寸、垂直度、空鼓、渗水等问题记录", design.getSurveyPhotos(), sort));
        sections.add(textSection(saved, TalkSectionType.SurveyReport, "量房质量报告",
                "每个区域的面积、周长与层高", surveyText(spaces), sort));
        sections.add(imageSection(saved, TalkSectionType.OriginalPlan, "原始结构图",
                "以 CAD 和现场勘测共同确认原房结构", copyImages(design.getOriginalPlanUrl()), sort));
        sections.add(textSection(saved, TalkSectionType.Problem, "户型问题解析",
                "把现场问题转译为施工和设计应对", problemText(requirements), sort));
        sections.add(imageSection(saved, TalkSectionType.Demolition, "拆改图",
                "拆改范围和结构安全说明", copyImages(design.getDemolitionUrl()), sort));
        sections.add(imageSection(saved, TalkSectionType.Masonry, "砌筑图",
                "新建墙体、门洞和空间尺度调整", copyImages(design.getMasonryUrl()), sort));
        sections.add(imageSection(saved, TalkSectionType.Optimization, "优化图",
                "动线、收纳、采光和家庭成员功能优化", copyImages(design.getOptimizationUrl()), sort));

        for (Space space : spaces) {
            List<DesignItem> spaceItems = items.stream()
                    .filter(item -> item.getSpace() != null && space.getSpaceId().equals(item.getSpace().getSpaceId()))
                    .toList();
            sections.add(roomSection(saved, space.getName(), dimensionText(space), roomText(spaceItems), space.getImages(), sort));
        }

        sections.add(textSection(saved, TalkSectionType.SoftDecor, "软装设计",
                "家具、窗帘、灯具与陈设统一搭配", itemText(items, ItemType.Soft), sort));
        sections.add(textSection(saved, TalkSectionType.Material, "材料搭配",
                "按区域列出主材、品牌、规格和用量", materialText(items), sort));
        sections.add(textSection(saved, TalkSectionType.Appliance, "电器配置",
                "电器设备清单和安装条件", itemText(items, ItemType.Appliance), sort));
        sections.add(textSection(saved, TalkSectionType.Investment, "装修投入",
                "设计报价仅作为讲解顺序，正式金额以签字报价单为准", investmentText(quote), sort));
        sections.add(textSection(saved, TalkSectionType.Saving, "省钱方案",
                "优先把钱花在影响居住质量和长期维修的位置", savingText(items), sort));
        sections.add(textSection(saved, TalkSectionType.End, "感谢您的观看",
                design.getCustomer().getName() + "，期待与您一起把家落地", List.of(), sort));
        return sections;
    }

    private TalkSection textSection(Talk talk, TalkSectionType type, String title, String subtitle,
                                     List<String> contentLines, int[] sort) {
        TalkSection section = new TalkSection();
        designContext.apply(section, talk);
        section.setType(type);
        section.setTitle(title);
        section.setSubtitle(subtitle);
        section.setContent(contentLines == null ? null : String.join("\n", contentLines));
        section.setImages(new ArrayList<>());
        section.setSort(sort[0]++);
        section.setVisible(Boolean.TRUE);
        return section;
    }

    private TalkSection imageSection(Talk talk, TalkSectionType type, String title, String subtitle,
                                      List<String> images, int[] sort) {
        TalkSection section = new TalkSection();
        designContext.apply(section, talk);
        section.setType(type);
        section.setTitle(title);
        section.setSubtitle(subtitle);
        section.setImages(images == null ? new ArrayList<>() : new ArrayList<>(images));
        section.setSort(sort[0]++);
        section.setVisible(Boolean.TRUE);
        return section;
    }

    private TalkSection roomSection(Talk talk, String title, String subtitle,
                                    List<String> contentLines, List<String> images, int[] sort) {
        TalkSection section = textSection(talk, TalkSectionType.RoomDesign, title, subtitle, contentLines, sort);
        section.setImages(images == null ? new ArrayList<>() : new ArrayList<>(images));
        return section;
    }

    private List<String> copyImages(String url) {
        return url == null || url.isBlank() ? List.of() : List.of(url);
    }

    private List<String> catalog() {
        return List.of("量房与现场问题", "客户诉求与设计回应", "区域设计说明", "效果图与材料搭配", "装修投入与执行计划");
    }

    private List<String> requirementText(List<Requirement> requirements) {
        if (requirements.isEmpty()) {
            return List.of("暂未记录结构化诉求，请设计师在沟通后补充。");
        }
        return requirements.stream()
                .sorted(Comparator.comparing(Requirement::getPriority))
                .map(item -> (item.getKind() == RequirementKind.Reject ? "排斥：" : "诉求：")
                        + item.getTitle() + (item.getSpaceName() == null ? "" : "（" + item.getSpaceName() + "）"))
                .toList();
    }

    private List<String> problemText(List<Requirement> requirements) {
        List<String> rejects = requirements.stream()
                .filter(item -> item.getKind() == RequirementKind.Reject)
                .map(item -> item.getTitle() + "：" + text(item.getResponse(), "设计中已规避"))
                .toList();
        return rejects.isEmpty() ? List.of("现场问题将在量房报告中逐项说明。") : rejects;
    }

    private List<String> surveyText(List<Space> spaces) {
        if (spaces.isEmpty()) {
            return List.of("尚未录入区域尺寸。");
        }
        return spaces.stream().map(space -> space.getName() + "：" + dimensionText(space)).toList();
    }

    private String dimensionText(Space space) {
        return value(space.getLength()) + "m × " + value(space.getWidth()) + "m × " + value(space.getHeight())
                + "m，面积 " + value(space.getArea()) + "㎡，周长 " + value(space.getPerimeter()) + "m";
    }

    private List<String> roomText(List<DesignItem> items) {
        if (items.isEmpty()) {
            return List.of("本区域设计说明待补充。");
        }
        return items.stream().map(item -> item.getType().getName() + "｜" + item.getName() + "："
                + value(item.getQuantity()) + item.getUnit().getName() + "，金额 " + money(item.getAmount()) + "元").toList();
    }

    private List<String> itemText(List<DesignItem> items, ItemType type) {
        List<String> lines = items.stream().filter(item -> item.getType() == type)
                .map(item -> item.getName() + " " + text(item.getBrand(), "") + " " + text(item.getSpec(), ""))
                .toList();
        return lines.isEmpty() ? List.of("本期暂未配置。") : lines;
    }

    private List<String> materialText(List<DesignItem> items) {
        List<String> lines = items.stream()
                .filter(item -> item.getType() == ItemType.Material || item.getType() == ItemType.Custom)
                .map(item -> item.getCategory() + "｜" + item.getName() + "｜"
                        + text(item.getBrand(), "品牌待定") + "｜" + text(item.getSpec(), "规格待定"))
                .toList();
        return lines.isEmpty() ? List.of("本期暂未配置主材。") : lines;
    }

    private List<String> investmentText(Quote quote) {
        return List.of("分项合计：" + money(quote.getSubtotal()) + "元",
                "折扣：" + (quote.getDiscount() == null ? "100" : quote.getDiscount().multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString()) + "%",
                "税金：" + money(quote.getTaxAmount()) + "元",
                "报价合计：" + money(quote.getAmount()) + "元",
                "说明：具体项目和数量以双方确认的报价清单为准。");
    }

    private List<String> savingText(List<DesignItem> items) {
        List<DesignItem> top = items.stream()
                .filter(item -> item.getAmount() != null)
                .sorted(Comparator.comparing(DesignItem::getAmount).reversed())
                .limit(3)
                .toList();
        if (top.isEmpty()) {
            return List.of("先明确必要项，再选择材料档次和个性化造型。");
        }
        return top.stream().map(item -> "重点投入：" + item.getName() + "，预算 " + money(item.getAmount()) + "元。")
                .collect(Collectors.toList());
    }

    private String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String value(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String money(BigDecimal value) {
        BigDecimal money = value == null ? BigDecimal.ZERO : value;
        return money.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
