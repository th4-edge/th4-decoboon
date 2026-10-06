package com.th4.edge.decoboon.document.context;

/** 凭证受控版式默认值。 */
public final class DocumentLayoutDefaults {

    public static final String JSON = """
            {
              "pageSize":"A4",
              "orientation":"portrait",
              "marginMm":12,
              "blocks":[
                {"key":"company","visible":true,"sort":10},
                {"key":"title","visible":true,"sort":20},
                {"key":"meta","visible":true,"sort":30},
                {"key":"amount","visible":true,"sort":40},
                {"key":"detail","visible":true,"sort":50},
                {"key":"sign","visible":true,"sort":60},
                {"key":"footer","visible":true,"sort":70}
              ],
              "signatures":["制单","审核","领款/付款人"],
              "footer":"本凭证为款项收付单据打印稿，用于企业留档；不属于会计记账凭证，不替代会计做账。"
            }
            """.trim();

    private DocumentLayoutDefaults() {
    }
}
