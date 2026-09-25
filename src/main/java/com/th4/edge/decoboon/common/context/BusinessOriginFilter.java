package com.th4.edge.decoboon.common.context;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;

/** 正常经营口径的数据来源判断。 */
public final class BusinessOriginFilter {

    private BusinessOriginFilter() {
    }

    public static boolean isNormal(BusinessOrigin origin) {
        return origin == null || origin == BusinessOrigin.New;
    }

    public static boolean isCarryover(BusinessOrigin origin) {
        return origin == BusinessOrigin.Carryover;
    }
}
