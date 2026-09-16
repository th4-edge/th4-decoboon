package com.th4.edge.decoboon.pack.service.templates.page;

import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.framework.provider.security.Security;

/**
 * 装修套餐分页服务。
 */
public interface Page extends ModelService<PackageTemplate, org.springframework.data.domain.Page<PackageTemplate>>, Security {
}
