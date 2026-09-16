package com.th4.edge.tenant.user.service.invite.invitation.save;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Invite;
public interface Save extends Provider<Employee, Invite>, Security {}
