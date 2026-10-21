package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 业主视角预览来源。P0 仅开放 App。 */
@Note("业主视角预览来源")
public enum OwnerPreviewSource implements Enumerable<String> {
    App("App");

    private final String name;

    OwnerPreviewSource(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return name();
    }
}
