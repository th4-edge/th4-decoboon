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

    private static final List<Template> TEMPLATES = List.of(
            top("INCOME_PROJECT", "工程款", LedgerDirection.Income, 10),
            top("INCOME_DESIGN", "设计费", LedgerDirection.Income, 20),
            top("INCOME_MATERIAL_SALE", "材料销售", LedgerDirection.Income, 30),
            top("INCOME_OTHER", "其他收入", LedgerDirection.Income, 90),

            top("EXPENSE_MATERIAL", "材料采购", LedgerDirection.Expense, 10),
            child("EXPENSE_MATERIAL", "EXPENSE_MATERIAL_MAIN", "主材", LedgerDirection.Expense, 10),
            child("EXPENSE_MATERIAL", "EXPENSE_MATERIAL_AUX", "辅材", LedgerDirection.Expense, 20),
            child("EXPENSE_MATERIAL", "EXPENSE_MATERIAL_HARDWARE", "五金", LedgerDirection.Expense, 30),
            child("EXPENSE_MATERIAL", "EXPENSE_MATERIAL_OTHER", "其他材料", LedgerDirection.Expense, 90),

            top("EXPENSE_LABOR", "人工费用", LedgerDirection.Expense, 20),
            child("EXPENSE_LABOR", "EXPENSE_LABOR_WORKER", "工人工资", LedgerDirection.Expense, 10),
            child("EXPENSE_LABOR", "EXPENSE_LABOR_TEAM", "班组结算", LedgerDirection.Expense, 20),
            child("EXPENSE_LABOR", "EXPENSE_LABOR_PM", "项目经理费用", LedgerDirection.Expense, 30),

            top("EXPENSE_PROJECT", "项目费用", LedgerDirection.Expense, 30),
            child("EXPENSE_PROJECT", "EXPENSE_PROJECT_TRANSPORT", "运输费", LedgerDirection.Expense, 10),
            child("EXPENSE_PROJECT", "EXPENSE_PROJECT_CLEAN", "垃圾清运费", LedgerDirection.Expense, 20),
            child("EXPENSE_PROJECT", "EXPENSE_PROJECT_PROPERTY", "物业费", LedgerDirection.Expense, 30),
            child("EXPENSE_PROJECT", "EXPENSE_PROJECT_SITE", "现场杂费", LedgerDirection.Expense, 90),

            top("EXPENSE_OPERATION", "运营费用", LedgerDirection.Expense, 40),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_RENT", "房租", LedgerDirection.Expense, 10),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_UTILITY", "水电", LedgerDirection.Expense, 20),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_OFFICE", "办公费用", LedgerDirection.Expense, 30),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_TRAVEL", "差旅费", LedgerDirection.Expense, 40),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_MARKETING", "推广费", LedgerDirection.Expense, 50),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_SOFTWARE", "软件服务", LedgerDirection.Expense, 60),
            child("EXPENSE_OPERATION", "EXPENSE_OPERATION_FINANCE", "财务费用", LedgerDirection.Expense, 70),

            top("EXPENSE_TAX", "税费与其他", LedgerDirection.Expense, 90),
            child("EXPENSE_TAX", "EXPENSE_TAX_TAX", "税费", LedgerDirection.Expense, 10),
            child("EXPENSE_TAX", "EXPENSE_TAX_PENALTY", "违约金", LedgerDirection.Expense, 20),
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
                category = repository.save(category);
                byCode.put(key, category);
            }
            roots.put(template.code(), category);
        }
        for (Template template : TEMPLATES) {
            if (template.parentCode() == null) continue;
            String key = codeKey(template.direction(), template.code());
            LedgerCategory category = byCode.get(key);
            if (category == null) {
                category = new LedgerCategory();
                category.setCompany(company);
                category.setName(template.name());
                category.setDirection(template.direction());
                category.setCode(template.code());
                category.setParent(roots.get(template.parentCode()));
                category.setBuiltIn(true);
                category.setActive(true);
                category.setSort(template.sort());
                category = repository.save(category);
                byCode.put(key, category);
            }
        }
    }

    @Transactional
    public LedgerCategory select(Employee current, Long categoryId, LedgerDirection direction) {
        if (categoryId == null || categoryId == 0L) return null;
        LedgerCategory category = repository.findByCompany_ComIdAndCategoryId(
                        current.getCompany().getComId(), categoryId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "业务分类不存在或无权访问")));
        validateSelectable(current, category, direction);
        return category;
    }

    @Transactional
    public void validateSelectable(Employee current, LedgerCategory category, LedgerDirection direction) {
        if (category == null) return;
        if (!category.getCompany().getComId().equals(current.getCompany().getComId())
                || !Boolean.TRUE.equals(category.getActive())
                || category.getDirection() != direction) {
            throw new DefinedException(new DefaultErrorCode("400", "业务分类不存在、已停用或与收支方向不一致"));
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
