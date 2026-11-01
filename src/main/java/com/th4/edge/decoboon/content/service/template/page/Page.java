package com.th4.edge.decoboon.content.service.template.page;

import com.th4.edge.decoboon.content.model.ContentTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 内容模板选择器分页。 */
public interface Page extends Provider<ContentTemplate, org.springframework.data.domain.Page<ContentTemplate>>, Security {
}
