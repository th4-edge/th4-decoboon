package com.th4.edge.decoboon.pack.context;

import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 套餐列表只返回有界摘要，不加载完整项目行。 */
@Note("套餐配置摘要上下文")
@Component
public class PackageSummaryContext {

    private final PackageTemplateRepository templateRepository;

    public PackageSummaryContext(PackageTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<PackageTemplate> apply(List<PackageTemplate> templates, Long companyId) {
        Map<Long, Object[]> summaries = new HashMap<>();
        for (Object[] row : templateRepository.summarizeLinesByCompany(companyId)) {
            if (row == null || row.length < 3 || row[0] == null) continue;
            summaries.put(((Number) row[0]).longValue(), row);
        }
        for (PackageTemplate template : templates) {
            Object[] row = summaries.get(template.getTemplateId());
            int sectionCount = row == null ? 0 : ((Number) row[1]).intValue();
            int lineCount = row == null ? 0 : ((Number) row[2]).intValue();
            template.setSectionCount(sectionCount);
            template.setLineCount(lineCount);
            template.setReady(sectionCount > 0 && lineCount > 0);
        }
        return templates;
    }
}
