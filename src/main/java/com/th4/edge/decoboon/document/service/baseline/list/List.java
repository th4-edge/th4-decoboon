package com.th4.edge.decoboon.document.service.baseline.list;

import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 平台常用单据模板库。 */
public interface List extends ModelService<DocumentTemplate, java.util.List<DocumentTemplate>>, Security {
}
