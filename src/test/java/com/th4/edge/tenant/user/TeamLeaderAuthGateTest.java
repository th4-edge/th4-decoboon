package com.th4.edge.tenant.user;

import com.th4.edge.components.wechat.service.WechatMiniProgramClient;
import com.th4.edge.components.wechat.service.WechatSession;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.UserStatus;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.service.auth.session.login.support.Login;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** DEC-082：微信登录不得给历史 TeamLeader 签发应用会话。 */
class TeamLeaderAuthGateTest {

    @Test
    void wechatLoginMustRejectTeamLeaderBeforeRecordingSuccessfulLogin() {
        WechatMiniProgramClient wechatClient = mock(WechatMiniProgramClient.class);
        UserIdentityContext userContext = mock(UserIdentityContext.class);
        Login login = new Login(wechatClient, userContext);
        ReflectionTestUtils.setField(login, "code", "wx-code");

        Credentials credential = new Credentials();
        credential.setStatus(UserStatus.Active);
        Employee employee = new Employee();
        employee.setEmpId(900001L);
        employee.setRole(EmployeeRole.TeamLeader);
        employee.setStatus(EmployeeStatus.Active);

        when(wechatClient.session("wx-code")).thenReturn(new WechatSession("open-id", "union-id"));
        when(userContext.findWxByOpenId("open-id")).thenReturn(credential);
        when(userContext.activeEmployee(credential)).thenReturn(employee);

        assertThatThrownBy(() -> login.call(new Credentials()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("团队负责人角色已冻结");
        verify(userContext, never()).touchLogin(credential);
    }
}
