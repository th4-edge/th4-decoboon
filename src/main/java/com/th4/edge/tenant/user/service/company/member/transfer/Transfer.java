package com.th4.edge.tenant.user.service.company.member.transfer;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.MemberHandover;
public interface Transfer extends Provider<Employee, MemberHandover>, Security {}
