package com.th4.edge.decoboon.project.service.members.list;

import com.th4.edge.decoboon.project.model.ProjectMember;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 项目成员列表服务。 */
public interface List extends ModelService<ProjectMember, java.util.List<ProjectMember>>, Security {
}
