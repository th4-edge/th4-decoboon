package com.th4.edge.tenant.user.service.user.user.handover;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.MemberHandover;
public interface Handover extends Provider<Employee, MemberHandover>, Security {}
