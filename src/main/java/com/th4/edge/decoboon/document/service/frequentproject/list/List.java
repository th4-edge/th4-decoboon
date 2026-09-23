package com.th4.edge.decoboon.document.service.frequentproject.list;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 常用项目：按凭证出单频次排序，供凭证筛选优先选择。 */
public interface List extends ModelService<Project, java.util.List<Project>>, Security {
}
