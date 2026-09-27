package com.th4.edge.decoboon.ai.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** AI 调用的门店归属方式。 */
@Note("AI调用门店归属方式")
public enum AiStoreAllocation implements Enumerable<String> {

    BusinessStore("业务门店"),
    EmployeeStore("执行门店"),
    EnterprisePublic("企业公共");

    private final String name;

    AiStoreAllocation(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
