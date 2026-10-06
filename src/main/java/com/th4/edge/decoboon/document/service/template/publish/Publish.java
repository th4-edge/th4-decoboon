package com.th4.edge.decoboon.document.service.template.publish;

import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 发布企业单据模板当前版本。 */
public interface Publish extends Provider<DocumentTemplate, DocumentTemplate>, Security {
}
