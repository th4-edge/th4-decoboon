package com.th4.edge.decoboon.designtemplate.service.templates.page;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 设计模版分页服务。 */
public interface Page extends ModelService<DesignTemplate, org.springframework.data.domain.Page<DesignTemplate>>, Security {
}
