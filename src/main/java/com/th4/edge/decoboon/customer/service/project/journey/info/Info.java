package com.th4.edge.decoboon.customer.service.project.journey.info;

import com.th4.edge.decoboon.customer.model.CustomerJourney;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主项目生命周期旅程。 */
public interface Info extends Provider<Project, CustomerJourney>, Security {
}
