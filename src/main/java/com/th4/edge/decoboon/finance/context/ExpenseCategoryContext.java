package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.decoboon.finance.repository.ExpenseCategoryRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 费用分类初始化、查询与企业自定义维护。 */
@Component
public class ExpenseCategoryContext {

    private static final List<Template> TEMPLATES = List.of(
            new Template("TRAVEL", "差旅费", 10, "交通、住宿、出差补贴等差旅支出。"),
            new Template("OFFICE", "办公与房租水电", 20, "办公用品、房租、水电、物业等日常经营支出。"),
            new Template("MARKETING", "推广费", 30, "获客推广、广告投放、渠道合作等支出。"),
            new Template("HOSPITALITY", "客户招待", 40, "客户接待、商务宴请等经营支出。"),
            new Template("PROJECT_INCIDENTAL", "项目零星费", 50, "项目现场发生的零星其他支出；必须关联项目。"),
            new Template("OTHER", "其他费用", 90, "无法归入其他分类的费用；应补充说明。")
    );

    private final ExpenseCategoryRepository repository;

    public ExpenseCategoryContext(ExpenseCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<ExpenseCategory> list(Employee current) {
        ensureDefaults(current.getCompany());
        return repository.findByCompany_ComIdOrderBySortAscCategoryIdAsc(current.getCompany().getComId());
    }

    @Transactional
    public ExpenseCategory select(Employee current, Long categoryId) {
        if (categoryId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择费用分类"));
        }
        ensureDefaults(current.getCompany());
        ExpenseCategory category = repository.findByCompany_ComIdAndCategoryId(
                        current.getCompany().getComId(), categoryId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "费用分类不存在或无权访问")));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new DefinedException(new DefaultErrorCode("400", "费用分类已停用"));
        }
        return category;
    }

    @Transactional
    public ExpenseCategory saveCustom(Employee current, String name, String description, Integer sort) {
        ensureDefaults(current.getCompany());
        String normalized = required(name, "分类名称", 64);
        String code = customCode(normalized);
        if (repository.existsByCompany_ComIdAndCode(current.getCompany().getComId(), code)) {
            throw new DefinedException(new DefaultErrorCode("400", "费用分类已存在"));
        }
        ExpenseCategory category = new ExpenseCategory();
        category.setCompany(current.getCompany());
        category.setCode(code);
        category.setName(normalized);
        category.setDescription(optional(description, 500));
        category.setBuiltIn(false);
        category.setActive(true);
        category.setSort(sort == null ? 1000 : sort);
        category.setCreatedBy(current);
        return repository.save(category);
    }

    @Transactional
    public ExpenseCategory update(Employee current, Long categoryId, String name, String description,
                                  Boolean active, Integer sort) {
        ExpenseCategory category = requireCategory(current, categoryId);
        if (Boolean.TRUE.equals(category.getBuiltIn())) {
            if (name != null && !category.getName().equals(name.trim())) {
                throw new DefinedException(new DefaultErrorCode("400", "平台基础分类名称不可修改"));
            }
            if (description != null && !Objects.equals(optional(description, 500), category.getDescription())) {
                throw new DefinedException(new DefaultErrorCode("400", "平台基础分类说明不可修改"));
            }
        } else {
            if (name != null) category.setName(required(name, "分类名称", 64));
            if (description != null) category.setDescription(optional(description, 500));
        }
        if (active != null) category.setActive(active);
        if (sort != null) category.setSort(sort);
        return repository.save(category);
    }

    @Transactional
    public ExpenseCategory deactivate(Employee current, Long categoryId) {
        ExpenseCategory category = requireCategory(current, categoryId);
        category.setActive(false);
        return repository.save(category);
    }

    @Transactional
    public void ensureDefaults(Company company) {
        if (company == null || company.getComId() == null) return;
        List<ExpenseCategory> existing = repository.findByCompany_ComIdAndBuiltInTrue(company.getComId());
        Map<String, ExpenseCategory> byCode = new LinkedHashMap<>();
        existing.forEach(item -> byCode.put(item.getCode(), item));
        List<ExpenseCategory> save = new ArrayList<>();
        for (Template template : TEMPLATES) {
            ExpenseCategory category = byCode.get(template.code());
            if (category == null) {
                category = new ExpenseCategory();
                category.setCompany(company);
                category.setCode(template.code());
                category.setName(template.name());
                category.setDescription(template.description());
                category.setBuiltIn(true);
                category.setActive(true);
                category.setSort(template.sort());
                save.add(category);
            } else if (category.getDescription() == null || category.getDescription().isBlank()) {
                category.setDescription(template.description());
                save.add(category);
            }
        }
        if (!save.isEmpty()) repository.saveAll(save);
    }

    private ExpenseCategory requireCategory(Employee current, Long categoryId) {
        if (categoryId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择费用分类"));
        }
        return repository.findByCompany_ComIdAndCategoryId(current.getCompany().getComId(), categoryId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "费用分类不存在或无权访问")));
    }

    private String customCode(String name) {
        String normalized = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) normalized = "CUSTOM";
        return normalized + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String required(String value, String label, int max) {
        if (value == null || value.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "请填写" + label));
        }
        String normalized = value.trim();
        if (normalized.length() > max) {
            throw new DefinedException(new DefaultErrorCode("400", label + "不能超过" + max + "个字符"));
        }
        return normalized;
    }

    private String optional(String value, int max) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.length() > max ? normalized.substring(0, max) : normalized;
    }

    private record Template(String code, String name, int sort, String description) { }
}
