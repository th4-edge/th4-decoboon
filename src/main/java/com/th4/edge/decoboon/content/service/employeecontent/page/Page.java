package com.th4.edge.decoboon.content.service.employeecontent.page;
import com.th4.edge.decoboon.content.model.EmployeeContent;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
/** 员工内容分页。 */
public interface Page extends Provider<EmployeeContent, org.springframework.data.domain.Page<EmployeeContent>>, Security {}
