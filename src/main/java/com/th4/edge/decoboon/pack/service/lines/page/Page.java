package com.th4.edge.decoboon.pack.service.lines.page;

import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/**
 * 套餐分项分页服务。
 */
public interface Page extends ModelService<PackageLine, org.springframework.data.domain.Page<PackageLine>>, Security {
}
