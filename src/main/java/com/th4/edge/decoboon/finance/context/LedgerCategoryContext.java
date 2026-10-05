package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.model.LedgerCategory;
import com.th4.edge.decoboon.finance.repository.LedgerCategoryRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 手工记账两级经营分类：按企业生成行业常用模板，并合并企业自定义分类。 */
@Component
public class LedgerCategoryContext {

    /**
     * 装得清家装经营分类模板 v2。
     * 仅一级 code 保持稳定；二级可随版本增量补充。该模板不是会计科目或法定口径。
     */
    private static final List<Template> TEMPLATES = List.of(
            // 收入
            top("INCOME_PROJECT", "工程款", LedgerDirection.Income, 10),
            child("INCOME_PROJECT", "INCOME_PROJECT_FIRST", "首期款", LedgerDirection.Income, 10),
            child("INCOME_PROJECT", "INCOME_PROJECT_PROGRESS", "进度款", LedgerDirection.Income, 20),
            child("INCOME_PROJECT", "INCOME_PROJECT_FINAL", "尾款", LedgerDirection.Income, 30),
            child("INCOME_PROJECT", "INCOME_PROJECT_RETENTION", "质保金", LedgerDirection.Income, 40),
            child("INCOME_PROJECT", "INCOME_PROJECT_EXTRA", "增项款", LedgerDirection.Income, 50),
            child("INCOME_PROJECT", "INCOME_PROJECT_OTHER", "其他工程款", LedgerDirection.Income, 90),

            top("INCOME_DESIGN", "设计费", LedgerDirection.Income, 20),
            child("INCOME_DESIGN", "INCOME_DESIGN_DEPOSIT", "设计定金", LedgerDirection.Income, 10),
            child("INCOME_DESIGN", "INCOME_DESIGN_FEE", "设计费", LedgerDirection.Income, 20),
            child("INCOME_DESIGN", "INCOME_DESIGN_DEEPEN", "效果图/深化设计费", LedgerDirection.Income, 30),
            child("INCOME_DESIGN", "INCOME_DESIGN_OTHER", "其他设计收入", LedgerDirection.Income, 90),

            top("INCOME_MATERIAL_SALE", "材料销售", LedgerDirection.Income, 30),
            child("INCOME_MATERIAL_SALE", "INCOME_MATERIAL_SALE_MAIN", "主材销售收入", LedgerDirection.Income, 10),
            child("INCOME_MATERIAL_SALE", "INCOME_MATERIAL_SALE_AUX", "辅材销售收入", LedgerDirection.Income, 20),
            child("INCOME_MATERIAL_SALE", "INCOME_MATERIAL_SALE_CUSTOM", "定制产品收入", LedgerDirection.Income, 30),
            child("INCOME_MATERIAL_SALE", "INCOME_MATERIAL_SALE_OTHER", "其他材料收入", LedgerDirection.Income, 90),

            top("INCOME_SERVICE", "服务收入", LedgerDirection.Income, 40),
            child("INCOME_SERVICE", "INCOME_SERVICE_MEASURE", "量房/验房服务费", LedgerDirection.Income, 10),
            child("INCOME_SERVICE", "INCOME_SERVICE_MANAGE", "项目管理费", LedgerDirection.Income, 20),
            child("INCOME_SERVICE", "INCOME_SERVICE_SUPERVISE", "监理服务费", LedgerDirection.Income, 30),
            child("INCOME_SERVICE", "INCOME_SERVICE_OTHER", "其他服务收入", LedgerDirection.Income, 90),

            top("INCOME_OTHER", "其他收入", LedgerDirection.Income, 90),
            child("INCOME_OTHER", "INCOME_OTHER_SUBSIDY", "补贴与返利", LedgerDirection.Income, 10),
            child("INCOME_OTHER", "INCOME_OTHER_COMPENSATION", "赔偿与罚款收入", LedgerDirection.Income, 20),
            child("INCOME_OTHER", "INCOME_OTHER_INTEREST", "利息收入", LedgerDirection.Income, 30),
            child("INCOME_OTHER", "INCOME_OTHER_DISPOSAL", "资产处置收入", LedgerDirection.Income, 40),
            child("INCOME_OTHER", "INCOME_OTHER_MISC", "其他杂项收入", LedgerDirection.Income, 90),

            // 支出
            top("EXPENSE_MATERIAL", "材料采购", LedgerDirection.Expense, 10),
            child("EXPENSE_MATERIAL", "EXPENSE_MATERIAL_MAIN", "主材采购", LedgerDirection.Expense, 10),
            child("EXPENSE_MATERIAL", "EXPENSE_MATERIAL_AUX", "辅材采购", LedgerDirection.Expense, 20),
            child("EXPENSE_MATERIAL", "EXPENSE_MATERIAL_HARDWARE", "五金采购", LedgerDirection.Expense, 30),
            child("EXPENSE_MATERIAL", "EXPENSE_MATERIAL_CUSTOM", "定制产品采购", LedgerDirection.Expense, 40),
            child("EXPENSE_MATERIAL", "EXPENSE_MATERIAL_FREIGHT", "材料运费/搬运", LedgerDirection.Expense, 50),
            child("EXPENSE_MATERIAL", "EXPENSE_MATERIAL_OTHER", "其他材料", LedgerDirection.Expense, 90),

            top("EXPENSE_LABOR", "人工费用", LedgerDirection.Expense, 20),
            child("EXPENSE_LABOR", "EXPENSE_LABOR_WORKER", "工人工资", LedgerDirection.Expense, 10),
            child("EXPENSE_LABOR", "EXPENSE_LABOR_TEAM", "班组结算", LedgerDirection.Expense, 20),
            child("EXPENSE_LABOR", "EXPENSE_LABOR_PM", "项目经理费用", LedgerDirection.Expense, 30),
            child("EXPENSE_LABOR", "EXPENSE_LABOR_TEMP", "临时用工", LedgerDirection.Expense, 40),
            child("EXPENSE_LABOR", "EXPENSE_LABOR_INSURANCE", "社保与意外险", LedgerDirection.Expense, 50),
            child("EXPENSE_LABOR", "EXPENSE_LABOR_OTHER", "其他人工", LedgerDirection.Expense, 90),

            top("EXPENSE_PROJECT", "项目费用", LedgerDirection.Expense, 30),
            child("EXPENSE_PROJECT", "EXPENSE_PROJECT_TRANSPORT", "运输费", LedgerDirection.Expense, 10),
            child("EXPENSE_PROJECT", "EXPENSE_PROJECT_CLEAN", "垃圾清运费", LedgerDirection.Expense, 20),
            child("EXPENSE_PROJECT", "EXPENSE_PROJECT_PROPERTY", "物业/进场费", LedgerDirection.Expense, 30),
            child("EXPENSE_PROJECT", "EXPENSE_PROJECT_SITE", "现场杂费", LedgerDirection.Expense, 40),
            child("EXPENSE_PROJECT", "EXPENSE_PROJECT_EQUIPMENT", "设备租赁", LedgerDirection.Expense, 50),
            child("EXPENSE_PROJECT", "EXPENSE_PROJECT_PROTECTION", "成品保护", LedgerDirection.Expense, 60),
            child("EXPENSE_PROJECT", "EXPENSE_PROJECT_INSPECTION", "检测/验收费用", LedgerDirection.Expense, 70),
            child("EXPENSE_PROJECT", "EXPENSE_PROJECT_AFTERSALE", "售后维修", LedgerDirection.Expense, 80),

            top("EXPENSE_OPERATION", "运营费用", LedgerDirection.Expense, 40),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_RENT", "房租", LedgerDirection.Expense, 10),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_UTILITY", "水电", LedgerDirection.Expense, 20),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_OFFICE", "办公费用", LedgerDirection.Expense, 30),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_TRAVEL", "差旅费", LedgerDirection.Expense, 40),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_MARKETING", "获客推广费", LedgerDirection.Expense, 50),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_CHANNEL", "渠道/返点", LedgerDirection.Expense, 60),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_SOFTWARE", "软件服务", LedgerDirection.Expense, 70),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_FINANCE", "财务费用", LedgerDirection.Expense, 80),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_PAYROLL", "薪酬福利", LedgerDirection.Expense, 90),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_TRAINING", "培训会议", LedgerDirection.Expense, 100),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_OTHER", "其他运营", LedgerDirection.Expense, 990),

            top("EXPENSE_TAX", "税费与其他", LedgerDirection.Expense, 90),
            child("EXPENSE_TAX", "EXPENSE_TAX_TAX", "税费", LedgerDirection.Expense, 10),
            child("EXPENSE_TAX", "EXPENSE_TAX_PENALTY", "违约金", LedgerDirection.Expense, 20),
            child("EXPENSE_TAX", "EXPENSE_TAX_DONATION", "捐赠支出", LedgerDirection.Expense, 30),
            child("EXPENSE_TAX", "EXPENSE_TAX_LOSS", "资产损失", LedgerDirection.Expense, 40),
            child("EXPENSE_TAX", "EXPENSE_TAX_OTHER", "其他支出", LedgerDirection.Expense, 90)
    );

    private final LedgerCategoryRepository repository;

    public LedgerCategoryContext(LedgerCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<LedgerCategory> tree(Employee current) {
        ensureBuiltIns(current.getCompany());
        List<LedgerCategory> all = repository
                .findByCompany_ComIdOrderByDirectionAscSortAscCategoryIdAsc(current.getCompany().getComId())
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .toList();
        Map<Long, LedgerCategory> byId = new HashMap<>();
        for (LedgerCategory category : all) {
            category.setChildren(new ArrayList<>());
            byId.put(category.getCategoryId(), category);
        }
        List<LedgerCategory> roots = new ArrayList<>();
        for (LedgerCategory category : all) {
            if (category.getParent() == null) {
                roots.add(category);
            } else {
                LedgerCategory parent = byId.get(category.getParent().getCategoryId());
                if (parent != null) parent.getChildren().add(category);
            }
        }
        roots.sort(Comparator.comparing(LedgerCategory::getDirection)
                .thenComparing(LedgerCategory::getSort)
                .thenComparing(LedgerCategory::getCategoryId));
        roots.forEach(root -> root.getChildren().sort(
                Comparator.comparing(LedgerCategory::getSort).thenComparing(LedgerCategory::getCategoryId)));
        return roots;
    }

    @Transactional
    public LedgerCategory saveCustom(Employee current, LedgerDirection direction, LedgerCategory parent, String name) {
        LedgerCategory category = new LedgerCategory();
        category.setCompany(current.getCompany());
        category.setDirection(direction);
        category.setParent(parent);
        category.setName(name.trim());
        category.setBuiltIn(false);
        category.setActive(true);
        category.setSort(1000);
        category.setCreatedBy(current);
        return repository.save(category);
    }

    /**
     * 幂等补齐内置模板：已存在的一级/二级只同步名称、排序与父子关系，不删除、不重建。
     */
    @Transactional
    public void ensureBuiltIns(Company company) {
        List<LedgerCategory> existing = repository.findByCompany_ComIdAndBuiltInTrue(company.getComId());
        Map<String, LedgerCategory> byCode = new LinkedHashMap<>();
        existing.forEach(item -> byCode.put(codeKey(item.getDirection(), item.getCode()), item));
        Map<String, LedgerCategory> roots = new HashMap<>();
        for (Template template : TEMPLATES) {
            if (template.parentCode() != null) continue;
            String key = codeKey(template.direction(), template.code());
            LedgerCategory category = byCode.get(key);
            if (category == null) {
                category = new LedgerCategory();
                category.setCompany(company);
                category.setName(template.name());
                category.setDirection(template.direction());
                category.setCode(template.code());
                category.setBuiltIn(true);
                category.setActive(true);
                category.setSort(template.sort());
                category.setParent(null);
                category = repository.save(category);
                byCode.put(key, category);
            }
            syncBuiltIn(category, template, null);
            roots.put(template.code(), category);
        }
        for (Template template : TEMPLATES) {
            if (template.parentCode() == null) continue;
            String key = codeKey(template.direction(), template.code());
            LedgerCategory category = byCode.get(key);
            LedgerCategory parent = roots.get(template.parentCode());
            if (category == null) {
                category = new LedgerCategory();
                category.setCompany(company);
                category.setName(template.name());
                category.setDirection(template.direction());
                category.setCode(template.code());
                category.setBuiltIn(true);
                category.setActive(true);
                category.setSort(template.sort());
                category.setParent(parent);
                category = repository.save(category);
                byCode.put(key, category);
            }
            syncBuiltIn(category, template, parent);
        }
    }

    private void syncBuiltIn(LedgerCategory category, Template template, LedgerCategory parent) {
        boolean changed = false;
        if (!template.name().equals(category.getName())) {
            category.setName(template.name());
            changed = true;
        }
        if (category.getDirection() != template.direction()) {
            category.setDirection(template.direction());
            changed = true;
        }
        if (!Boolean.TRUE.equals(category.getBuiltIn())) {
            category.setBuiltIn(true);
            changed = true;
        }
        if (!Boolean.TRUE.equals(category.getActive())) {
            category.setActive(true);
            changed = true;
        }
        if (category.getSort() == null || category.getSort() != template.sort()) {
            category.setSort(template.sort());
            changed = true;
        }
        boolean parentChanged = parent == null
                ? category.getParent() != null
                : category.getParent() == null || !parent.getCategoryId().equals(category.getParent().getCategoryId());
        if (parentChanged) {
            category.setParent(parent);
            changed = true;
        }
        if (changed) repository.save(category);
    }

    /**
     * 选择可记账分类：必须为本企业启用分类、方向一致，且不能是仍有启用子分类的一级分组。
     */
    @Transactional
    public LedgerCategory select(Employee current, Long categoryId, LedgerDirection direction) {
        if (categoryId == null || categoryId == 0L) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择业务分类"));
        }
        LedgerCategory category = repository.findByCompany_ComIdAndCategoryId(
                        current.getCompany().getComId(), categoryId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "业务分类不存在或无权访问")));
        validateSelectable(current, category, direction);
        return category;
    }

    @Transactional
    public void validateSelectable(Employee current, LedgerCategory category, LedgerDirection direction) {
        if (category == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择业务分类"));
        }
        if (!category.getCompany().getComId().equals(current.getCompany().getComId())
                || !Boolean.TRUE.equals(category.getActive())
                || category.getDirection() != direction) {
            throw new DefinedException(new DefaultErrorCode("400", "业务分类不存在、已停用或与收支方向不一致"));
        }
        boolean grouped = repository.existsByCompany_ComIdAndParent_CategoryIdAndActiveTrue(
                current.getCompany().getComId(), category.getCategoryId());
        if (grouped) {
            throw new DefinedException(new DefaultErrorCode("400", "该分类为一级分组，请选择二级业务分类"));
        }
    }

    private String codeKey(LedgerDirection direction, String code) {
        return direction.name() + ":" + code;
    }

    private static Template top(String code, String name, LedgerDirection direction, int sort) {
        return new Template(null, code, name, direction, sort);
    }

    private static Template child(String parentCode, String code, String name, LedgerDirection direction, int sort) {
        return new Template(parentCode, code, name, direction, sort);
    }

    private record Template(String parentCode, String code, String name, LedgerDirection direction, int sort) {
    }
}
