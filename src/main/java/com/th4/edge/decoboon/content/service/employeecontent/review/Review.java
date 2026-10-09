package com.th4.edge.decoboon.content.service.employeecontent.review;
import com.th4.edge.decoboon.content.model.EmployeeContent;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
/** 审核员工成片。 */
public interface Review extends Provider<EmployeeContent, EmployeeContent>, Security {}
