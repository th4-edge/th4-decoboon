package com.th4.edge.decoboon.customer.service.project.album.page;

import com.th4.edge.decoboon.customer.model.ProjectAlbumEntry;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主工地相册：合并现场汇报与阶段验收，按发生时间倒序分页。 */
public interface Page extends Provider<Project, org.springframework.data.domain.Page<ProjectAlbumEntry>>, Security {
}
