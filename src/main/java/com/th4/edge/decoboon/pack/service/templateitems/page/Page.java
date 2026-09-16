package com.th4.edge.decoboon.pack.service.templateitems.page;

import com.th4.edge.decoboon.pack.model.PackageTemplateItem;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/**
 * 套餐差项分页服务。
 */
public interface Page extends ModelService<PackageTemplateItem, org.springframework.data.domain.Page<PackageTemplateItem>>, Security {
}
