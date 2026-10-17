package com.th4.edge.decoboon.blueprint.service.check.report;

import com.th4.edge.decoboon.material.model.MaterialHealthQuery;
import com.th4.edge.decoboon.material.model.MaterialHealthReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 项目材料业务体检报告。 */
public interface Report extends Provider<MaterialHealthQuery, MaterialHealthReport>, Security {
}
