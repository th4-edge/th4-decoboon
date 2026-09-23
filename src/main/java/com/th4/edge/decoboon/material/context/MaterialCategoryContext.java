package com.th4.edge.decoboon.material.context;

import com.th4.edge.decoboon.material.model.MaterialCategory;
import com.th4.edge.decoboon.material.repository.MaterialCategoryRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.model.Company;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 主材分类损耗率解析。
 *
 * <p>DEC-MP-09：材料计划行按主材分类继承默认损耗率；分类未配置或已停用时返回 0。</p>
 */
@Note("主材分类损耗率解析")
@Component
public class MaterialCategoryContext {

    private final MaterialCategoryRepository repository;

    public MaterialCategoryContext(MaterialCategoryRepository repository) {
        this.repository = repository;
    }

    /** 按分类名解析默认损耗率（百分比）；找不到返回 0。 */
    @Transactional(readOnly = true)
    public BigDecimal lossRate(Company company, String categoryName) {
        if (company == null || company.getComId() == null
                || categoryName == null || categoryName.isBlank()) {
            return BigDecimal.ZERO;
        }
        Optional<MaterialCategory> category = repository
                .findFirstByCompany_ComIdAndNameIgnoreCaseAndActiveTrue(
                        company.getComId(), categoryName.trim());
        return category.map(MaterialCategory::getDefaultLossRate)
                .filter(rate -> rate != null && rate.signum() > 0)
                .orElse(BigDecimal.ZERO);
    }
}
