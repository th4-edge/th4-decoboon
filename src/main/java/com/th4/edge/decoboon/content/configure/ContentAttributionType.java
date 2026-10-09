package com.th4.edge.decoboon.content.configure;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;
/** 内容线索归因方式。 */
@Note("内容线索归因方式")
public enum ContentAttributionType implements Enumerable<String> {
    Platform("平台回传"), UniqueCode("专属入口"), Manual("人工确认"), SelfReport("用户自述"), Inference("推断");
    private final String name;
    ContentAttributionType(String name) { this.name = name; }
    @Override public String getName() { return name; }
    @Override public String getValue() { return name(); }
}
