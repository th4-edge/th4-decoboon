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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 手工记账“其他账务”一级分类。
 *
 * <p>手工记账只承接应收应付闭环之外的、装企日常不常见或个性化账务。分类平铺为一级，
 * 不使用父子层级，也不把工程款、材料采购、工资、项目款等应收应付业务作为可选项。</p>
 */
@Component
public class LedgerCategoryContext {

    /**
     * 新手工记账分类模板。旧的 {@code INCOME_PROJECT/EXPENSE_MATERIAL/EXPENSE_LABOR}
     * 等两级模板仅保留在数据库中用于历史流水，不再作为新记账候选返回。
     */
    private static final List<Template> TEMPLATES = List.of(
            new Template("MANUAL_PROJECT_INCIDENTAL_COST", "项目零星其他支出", LedgerDirection.Expense, 10,
                    "项目现场发生的零星、个性化其他支出；必须关联项目，不包含采购、工资、项目款等有来源单据的支出。"),
            new Template("MANUAL_COMPANY_OTHER_EXPENSE", "企业其他费用", LedgerDirection.Expense, 20,
                    "装企日常不常见或个性化的其他费用；不包含采购、工资、工程款等已有应收应付闭环项。"),
            new Template("MANUAL_CAPITAL_TRANSFER_OUT", "资金往来支出", LedgerDirection.Expense, 30,
                    "借款、还款、押金、备用金、股东往来等资金往来支出；不作为项目成本或利润。"),
            new Template("MANUAL_OTHER_EXPENSE", "其他支出", LedgerDirection.Expense, 90,
                    "确实无法归入其他其他账务的兜底支出；不应作为常态分类，长期使用应新增一级分类。"),
            new Template("MANUAL_OTHER_INCOME", "其他收入", LedgerDirection.Income, 10,
                    "非工程款、非应收闭环的其他收入；不包含合同款、客户工程款等应收款项。"),
            new Template("MANUAL_CAPITAL_TRANSFER_IN", "资金往来收入", LedgerDirection.Income, 20,
                    "借款归还、押金退还、备用金收回、股东往来等资金往来收入；不作为项目收入或利润。")
    );

    private static final Set<String> FLAT_CODES = TEMPLATES.stream()
            .map(Template::code)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private final LedgerCategoryRepository repository;

    public LedgerCategoryContext(LedgerCategoryRepository repository) {
        this.repository = repository;
    }

    /**
     * 返回当前企业可选的“其他账务”一级分类。
     *
     * <p>旧两级分类、旧应收应付分类和自定义子分类不返回；历史流水仍按原 category 文本和
     * category_id 回读，不受影响。</p>
     */
    @Transactional
    public List<LedgerCategory> tree(Employee current) {
        ensureBuiltIns(current.getCompany());
        return repository.findByCompany_ComIdOrderByDirectionAscSortAscCategoryIdAsc(current.getCompany().getComId())
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .filter(this::manualSelectable)
                .peek(item -> item.setChildren(new ArrayList<>()))
                .sorted(Comparator.comparing(LedgerCategory::getDirection)
                        .thenComparing(LedgerCategory::getSort)
                        .thenComparing(LedgerCategory::getCategoryId))
                .toList();
    }

    @Transactional
    public LedgerCategory saveCustom(Employee current, LedgerDirection direction,
                                     String name, String description) {
        LedgerCategory category = new LedgerCategory();
        category.setCompany(current.getCompany());
        category.setDirection(direction);
        category.setParent(null);
        category.setName(name.trim());
        category.setDescription(description.trim());
        category.setBuiltIn(false);
        category.setActive(true);
        category.setSort(1000);
        category.setCreatedBy(current);
        return repository.save(category);
    }

    /**
     * 幂等补齐一级内置模板。只补缺失编码和空说明，不覆盖企业改名或说明。
     */
    @Transactional
    public void ensureBuiltIns(Company company) {
        List<LedgerCategory> existing = repository.findByCompany_ComIdAndBuiltInTrue(company.getComId());
        Map<String, LedgerCategory> byCode = new LinkedHashMap<>();
        existing.forEach(item -> byCode.put(codeKey(item.getDirection(), item.getCode()), item));
        List<LedgerCategory> backfill = new ArrayList<>();
        for (Template template : TEMPLATES) {
            String key = codeKey(template.direction(), template.code());
            LedgerCategory category = byCode.get(key);
            if (category == null) {
                category = new LedgerCategory();
                category.setCompany(company);
                category.setName(template.name());
                category.setDescription(template.description());
                category.setDirection(template.direction());
                category.setCode(template.code());
                category.setBuiltIn(true);
                category.setActive(true);
                category.setSort(template.sort());
                category.setParent(null);
                repository.save(category);
                byCode.put(key, category);
            } else if (category.getDescription() == null || category.getDescription().isBlank()) {
                category.setDescription(template.description());
                backfill.add(category);
            }
        }
        if (!backfill.isEmpty()) repository.saveAll(backfill);
    }

    /**
     * 选择可记账分类：必须为本企业启用的一级“其他账务”分类，方向一致。
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
        if (!manualSelectable(category)) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择其他账务的一级业务分类"));
        }
        boolean hasChildren = repository.existsByCompany_ComIdAndParent_CategoryIdAndActiveTrue(
                current.getCompany().getComId(), category.getCategoryId());
        if (hasChildren) {
            throw new DefinedException(new DefaultErrorCode("400", "手工记账分类只能是一级分类"));
        }
    }

    private boolean manualSelectable(LedgerCategory category) {
        if (category.getParent() != null) return false;
        if (Boolean.TRUE.equals(category.getBuiltIn())) return FLAT_CODES.contains(category.getCode());
        return true;
    }

    private String codeKey(LedgerDirection direction, String code) {
        return direction.name() + ":" + code;
    }

    private record Template(String code, String name, LedgerDirection direction, int sort, String description) {
    }
}
