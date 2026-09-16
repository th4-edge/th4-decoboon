package com.th4.edge.tenant.user.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 安全审计结果。 */
@Note("安全审计结果")
public enum SecurityResult implements Enumerable<String> {
    Success("成功"),
    Failure("失败"),
    Blocked("已拦截");

    private final String name;

    SecurityResult(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
