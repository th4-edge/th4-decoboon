package com.th4.edge.decoboon.document.service.templatever.page;

import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.decoboon.document.model.DocumentTemplateVersion;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

/** 企业单据模板版本分页。 */
public interface Page extends Provider<DocumentTemplate, List<DocumentTemplateVersion>>, Security {
}
