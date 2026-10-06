package com.th4.edge.decoboon.document.service.template.disable;

import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 停用企业单据模板当前发布版本。 */
public interface Disable extends Provider<DocumentTemplate, DocumentTemplate>, Security {
}
