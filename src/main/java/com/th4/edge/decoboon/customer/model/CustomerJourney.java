package com.th4.edge.decoboon.customer.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 业主项目全生命周期旅程。 */
@Note("业主项目生命周期旅程")
public class CustomerJourney {

    @Note("项目索引") private Long projectId;
    @Note("项目编号") private String projectNo;
    @Note("项目名称") private String projectName;
    @Note("项目状态") private String projectStatus;
    @Note("项目状态名称") private String projectStatusName;
    @Note("整体进度") private Integer progress = 0;
    @Note("当前阶段名称") private String currentStageName;
    @Note("房屋地址") private String propertyAddress;
    @Note("数据时间") private LocalDateTime asOf;
    @Note("身份生命周期分区") private List<Section> sections = new ArrayList<>();
    @Note("待业主操作") private List<Action> actions = new ArrayList<>();
    @Note("未关闭反馈数") private Integer openFeedbackCount = 0;
    @Note("待办数量") private Integer pendingActionCount = 0;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectNo() { return projectNo; }
    public void setProjectNo(String projectNo) { this.projectNo = projectNo; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectStatus() { return projectStatus; }
    public void setProjectStatus(String projectStatus) { this.projectStatus = projectStatus; }
    public String getProjectStatusName() { return projectStatusName; }
    public void setProjectStatusName(String projectStatusName) { this.projectStatusName = projectStatusName; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public String getCurrentStageName() { return currentStageName; }
    public void setCurrentStageName(String currentStageName) { this.currentStageName = currentStageName; }
    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }
    public LocalDateTime getAsOf() { return asOf; }
    public void setAsOf(LocalDateTime asOf) { this.asOf = asOf; }
    public List<Section> getSections() { return sections; }
    public void setSections(List<Section> sections) { this.sections = sections; }
    public List<Action> getActions() { return actions; }
    public void setActions(List<Action> actions) { this.actions = actions; }
    public Integer getOpenFeedbackCount() { return openFeedbackCount; }
    public void setOpenFeedbackCount(Integer openFeedbackCount) { this.openFeedbackCount = openFeedbackCount; }
    public Integer getPendingActionCount() { return pendingActionCount; }
    public void setPendingActionCount(Integer pendingActionCount) { this.pendingActionCount = pendingActionCount; }

    /** 生命周期分区。 */
    public static class Section {
        private String key;
        private String title;
        private String subtitle;
        private String status;
        private String statusName;
        private String summary;
        private List<Fact> facts = new ArrayList<>();
        private List<Action> actions = new ArrayList<>();
        private List<Item> items = new ArrayList<>();

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSubtitle() { return subtitle; }
        public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusName() { return statusName; }
        public void setStatusName(String statusName) { this.statusName = statusName; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public List<Fact> getFacts() { return facts; }
        public void setFacts(List<Fact> facts) { this.facts = facts; }
        public List<Action> getActions() { return actions; }
        public void setActions(List<Action> actions) { this.actions = actions; }
        public List<Item> getItems() { return items; }
        public void setItems(List<Item> items) { this.items = items; }
    }

    /** 生命周期明细项：量房、方案、报价、选材、变更、付款节点等。 */
    public static class Item {
        private String key;
        private String title;
        private String subtitle;
        private String status;
        private String statusName;
        private String amount;
        private String date;
        private String businessType;
        private Long businessId;
        private List<String> images = new ArrayList<>();
        private List<String> lines = new ArrayList<>();
        private List<Fact> facts = new ArrayList<>();
        @Note("业务留痕影像") private List<Media> media = new ArrayList<>();
        @Note("区域尺寸") private List<Region> regions = new ArrayList<>();
        @Note("按区域分组的清单") private List<PriceGroup> priceGroups = new ArrayList<>();

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSubtitle() { return subtitle; }
        public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusName() { return statusName; }
        public void setStatusName(String statusName) { this.statusName = statusName; }
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }
        public Long getBusinessId() { return businessId; }
        public void setBusinessId(Long businessId) { this.businessId = businessId; }
        public List<String> getImages() { return images; }
        public void setImages(List<String> images) { this.images = images; }
        public List<String> getLines() { return lines; }
        public void setLines(List<String> lines) { this.lines = lines; }
        public List<Fact> getFacts() { return facts; }
        public void setFacts(List<Fact> facts) { this.facts = facts; }
        public List<Media> getMedia() { return media; }
        public void setMedia(List<Media> media) { this.media = media; }
        public List<Region> getRegions() { return regions; }
        public void setRegions(List<Region> regions) { this.regions = regions; }
        public List<PriceGroup> getPriceGroups() { return priceGroups; }
        public void setPriceGroups(List<PriceGroup> priceGroups) { this.priceGroups = priceGroups; }
    }

    /** 分区事实。 */
    public static class Fact {
        private String key;
        private String label;
        private String value;
        private String valueType;

        public Fact() {}
        public Fact(String key, String label, String value, String valueType) {
            this.key = key; this.label = label; this.value = value; this.valueType = valueType;
        }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getValueType() { return valueType; }
        public void setValueType(String valueType) { this.valueType = valueType; }
    }

    /** 待操作的业务动作。 */
    public static class Action {
        private String key;
        private String type;
        private String title;
        private String description;
        private String targetType;
        private Long targetId;

        public Action() {}
        public Action(String key, String type, String title, String description, String targetType, Long targetId) {
            this.key = key; this.type = type; this.title = title; this.description = description;
            this.targetType = targetType; this.targetId = targetId;
        }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTargetType() { return targetType; }
        public void setTargetType(String targetType) { this.targetType = targetType; }
        public Long getTargetId() { return targetId; }
        public void setTargetId(Long targetId) { this.targetId = targetId; }
    }

    /** 业务留痕影像：量房原样照片、设计图纸、现场记录等。 */
    public static class Media {
        private String key;
        private String url;
        @Note("类型") private String kind;
        @Note("标题") private String title;
        @Note("拍摄/生成时间") private String time;
        @Note("所属区域") private String room;

        public Media() {}
        public Media(String key, String url, String kind, String title, String time, String room) {
            this.key = key; this.url = url; this.kind = kind; this.title = title; this.time = time; this.room = room;
        }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getRoom() { return room; }
        public void setRoom(String room) { this.room = room; }
    }

    /** 区域尺寸：房屋设计的原型表达，避免用纯文本描述。 */
    public static class Region {
        private String key;
        @Note("区域名称") private String name;
        @Note("区域类型") private String type;
        @Note("长") private String length;
        @Note("宽") private String width;
        @Note("高") private String height;
        @Note("面积") private String area;
        @Note("周长") private String perimeter;
        @Note("面积口径来源") private String areaSource;
        @Note("设计数量") private Integer itemCount = 0;
        @Note("区域图片") private List<String> images = new ArrayList<>();
        @Note("设计说明") private String remark;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getLength() { return length; }
        public void setLength(String length) { this.length = length; }
        public String getWidth() { return width; }
        public void setWidth(String width) { this.width = width; }
        public String getHeight() { return height; }
        public void setHeight(String height) { this.height = height; }
        public String getArea() { return area; }
        public void setArea(String area) { this.area = area; }
        public String getPerimeter() { return perimeter; }
        public void setPerimeter(String perimeter) { this.perimeter = perimeter; }
        public String getAreaSource() { return areaSource; }
        public void setAreaSource(String areaSource) { this.areaSource = areaSource; }
        public Integer getItemCount() { return itemCount; }
        public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
        public List<String> getImages() { return images; }
        public void setImages(List<String> images) { this.images = images; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    /** 按区域分组的报价/设计分项清单。 */
    public static class PriceGroup {
        private String key;
        @Note("区域名称") private String name;
        @Note("区域小计") private String total;
        @Note("区域合计说明") private String summary;
        @Note("明细行") private List<PriceRow> rows = new ArrayList<>();

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTotal() { return total; }
        public void setTotal(String total) { this.total = total; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public List<PriceRow> getRows() { return rows; }
        public void setRows(List<PriceRow> rows) { this.rows = rows; }
    }

    /** 区域下的单条清单行。 */
    public static class PriceRow {
        private String key;
        @Note("项目名称") private String name;
        @Note("分类") private String category;
        @Note("品牌") private String brand;
        @Note("规格") private String spec;
        @Note("单位") private String unit;
        @Note("数量") private String quantity;
        @Note("单价") private String price;
        @Note("金额") private String amount;
        @Note("角色") private String role;
        @Note("类型") private String type;
        @Note("施工工艺") private String craft;
        @Note("工艺/验收标准") private String criterion;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public String getSpec() { return spec; }
        public void setSpec(String spec) { this.spec = spec; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getQuantity() { return quantity; }
        public void setQuantity(String quantity) { this.quantity = quantity; }
        public String getPrice() { return price; }
        public void setPrice(String price) { this.price = price; }
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getCraft() { return craft; }
        public void setCraft(String craft) { this.craft = craft; }
        public String getCriterion() { return criterion; }
        public void setCriterion(String criterion) { this.criterion = criterion; }
    }
}
