package com.th4.edge.components.sms.service.vcode.send;

/***
 * 短信验证码发送接口,用于定义业务场景进行短信验证码发送的业务逻辑。
 * 1、涉及到短信模版、短信内容等信息是以场景进行映射进行分逻辑处理。
 * 2、scene字段在不同厂商的短信引擎里应该投影成枚举字段，通过枚举固定化逻辑场景需求。
 * 3、短信验证码发送成功后应该存储在redis并根据场景进行设定有效期
 */
public interface Sender{

    /***
     * 短信验证码发送
     * @param telNo 手机号码
     * @param scene 验证场景
     * @param vcode 验证码
     */
    void send(String telNo,String scene,String vcode);
}
