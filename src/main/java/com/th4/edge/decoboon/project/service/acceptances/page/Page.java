package com.th4.edge.decoboon.project.service.acceptances.page;

import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.framework.provider.security.Security;

/**
 * 验收单分页服务。
 */
public interface Page extends ModelService<Acceptance, org.springframework.data.domain.Page<Acceptance>>, Security {
}
