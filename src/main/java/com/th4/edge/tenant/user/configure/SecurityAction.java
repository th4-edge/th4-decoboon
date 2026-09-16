package com.th4.edge.tenant.user.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 用户安全审计动作。 */
@Note("安全审计动作")
public enum SecurityAction implements Enumerable<String> {
    Login("登录"),
    Logout("退出登录"),
    PasswordSet("首次设置密码"),
    PasswordChange("修改密码"),
    AccountLock("账号锁定"),
    AccountUnlock("账号解锁"),
    Register("注册"),
    PhoneBind("手机号绑定"),
    InviteAccept("接受邀请"),
    InviteReject("拒绝邀请"),
    CompanyJoin("加入企业"),
    CompanyResign("退出企业"),
    CompanySwitch("切换企业"),
    BossTransfer("负责人转让"),
    LimitTrigger("触发风控限制");

    private final String name;

    SecurityAction(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
