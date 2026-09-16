package com.th4.edge.decoboon.pack.service.sections.page;

import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/**
 * 套餐空间分页服务。
 */
public interface Page extends ModelService<PackageSection, org.springframework.data.domain.Page<PackageSection>>, Security {
}
