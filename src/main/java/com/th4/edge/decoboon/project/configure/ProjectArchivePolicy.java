package com.th4.edge.decoboon.project.configure;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.project.model.Project;

/** 历史接管项目的只读归档口径。 */
public final class ProjectArchivePolicy {

    public static final String ARCHIVE_LABEL = "历史接管项目 / 历史归档";
    public static final String SETTLEMENT_LABEL = "历史已结算，明细未接管";

    private ProjectArchivePolicy() {
    }

    /**
     * 历史接管项目只认“接管基线 + 已结算”这一种组合，不覆盖正常新项目或其他接管项目。
     */
    public static boolean isHistoricalArchive(Project project) {
        return project != null
                && project.getBusinessOrigin() == BusinessOrigin.Carryover
                && project.getStatus() == ProjectStatus.Settled;
    }
}
