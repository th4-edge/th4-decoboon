package com.th4.edge.decoboon.project.service.ownerpreview.info;

import com.th4.edge.decoboon.customer.model.CustomerJourney;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 员工以自身身份查看本项目业主视角只读投影。 */
public interface Info extends Provider<Project, CustomerJourney>, Security {
}
