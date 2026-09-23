package com.th4.edge.decoboon.document.service.profile.info;

import com.th4.edge.decoboon.document.model.DocumentProfile;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

public interface Info extends Provider<Employee, DocumentProfile>, Security {
}
