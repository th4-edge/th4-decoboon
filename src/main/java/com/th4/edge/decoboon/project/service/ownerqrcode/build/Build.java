package com.th4.edge.decoboon.project.service.ownerqrcode.build;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主项目码生成/获取（一项目一码；扫码进入业主端）。 */
public interface Build extends Provider<Project, Project>, Security {
}
