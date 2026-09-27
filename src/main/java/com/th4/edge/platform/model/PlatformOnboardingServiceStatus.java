package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 平台企业入驻服务状态，仅属于平台运营，不写回租户业务状态。 */
@Note("企业入驻服务状态")
public enum PlatformOnboardingServiceStatus implements Enumerable<String> {
    Pending("待联系"),
    Contacted("已联系"),
    Waiting("待客户行动"),
    Activated("已激活"),
    Closed("已关闭");

    private final String name;

    PlatformOnboardingServiceStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
