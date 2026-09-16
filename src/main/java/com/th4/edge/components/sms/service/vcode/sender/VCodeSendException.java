package com.th4.edge.components.sms.service.vcode.send;

/***
 * 短信验证码发送错误异常
 */
public class VCodeSendException extends Exception{

    private final String code;


    public VCodeSendException(String code,String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
