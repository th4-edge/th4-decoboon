package com.th4.edge.tenant.user.service.invite.invitation.cancel;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Invite;
public interface Cancel extends Provider<Employee, Invite>, Security {}
