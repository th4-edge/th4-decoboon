package com.th4.edge.decoboon.design.context;

/** AI效果图生成客户端异常。 */
public class DesignRenderException extends RuntimeException {

    private final String code;

    public DesignRenderException(String code, String message) {
        super(message);
        this.code = code;
    }

    public DesignRenderException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
