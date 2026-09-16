package com.th4.edge.tenant.user.service.user.team.list;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Team;

/** 当前账号可见团队列表服务。 */
public interface List extends Provider<Employee, java.util.List<Team>>, Security {
}
