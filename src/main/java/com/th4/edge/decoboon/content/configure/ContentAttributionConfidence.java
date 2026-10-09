package com.th4.edge.decoboon.content.configure;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;
/** 内容线索归因置信度。 */
@Note("内容线索归因置信度")
public enum ContentAttributionConfidence implements Enumerable<String> {
    High("高"), Medium("中"), Low("低");
    private final String name;
    ContentAttributionConfidence(String name) { this.name = name; }
    @Override public String getName() { return name; }
    @Override public String getValue() { return name(); }
}
