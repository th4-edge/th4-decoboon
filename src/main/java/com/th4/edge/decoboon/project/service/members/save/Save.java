package com.th4.edge.decoboon.project.service.members.save;

import com.th4.edge.decoboon.project.model.ProjectMember;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 项目成员保存服务。 */
public interface Save extends ModelService<ProjectMember, ProjectMember>, Security {
}
