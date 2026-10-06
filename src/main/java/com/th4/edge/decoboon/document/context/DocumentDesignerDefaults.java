package com.th4.edge.decoboon.document.context;

/** 凭证设计器默认画布。 */
public final class DocumentDesignerDefaults {

    public static final String JSON = """
            {
              "schemaVersion":1,
              "page":{"widthMm":210,"heightMm":297,"orientation":"portrait","marginMm":12,"gridSizeMm":5},
              "elements":[],
              "tables":[],
              "print":{"scale":100,"offsetXMm":0,"offsetYMm":0}
            }
            """.trim();

    private DocumentDesignerDefaults() {
    }
}
