package com.th4.edge.decoboon.statistic.service.workbench.site.report;

import com.th4.edge.decoboon.statistic.model.SiteWorkbench;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/**
 * 项目工地工作台聚合服务。
 *
 * <p>面向项目经理、财务（含履约协同）、店长与企业负责人：返回三栏工作项、
 * 栏位计数、责任人分组与顶部指标，供工地工作台一屏编排。</p>
 */
public interface Report extends Provider<Employee, SiteWorkbench>, Security {
}
