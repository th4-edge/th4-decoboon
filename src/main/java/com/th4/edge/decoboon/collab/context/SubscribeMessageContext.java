package com.th4.edge.decoboon.collab.context;

import com.th4.edge.components.wechat.configure.WechatMiniProgramConfigure;
import com.th4.edge.components.wechat.configure.WechatSubscribeScenario;
import com.th4.edge.components.wechat.service.WechatMiniProgramClient;
import com.th4.edge.components.wechat.service.WechatSubscribeMessage;
import com.th4.edge.components.wechat.service.WechatSubscribeResult;
import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.decoboon.customer.repository.CustomerAccessRepository;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.CredentialsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信订阅消息触达上下文。
 *
 * <p>三条不可违反的约束：</p>
 * <ol>
 *   <li>未配置模板 ID 的场景直接跳过，本能力未上线不得影响既有业务；</li>
 *   <li>发送在事务提交后触发，不在数据库事务内做外部 HTTP 调用；</li>
 *   <li>发送失败只记日志，绝不抛异常回滚业务事实——通知是附赠，不是事实的一部分。</li>
 * </ol>
 *
 * <p>模板字段名（thing1、time2 等）由公众平台所选模板决定，改模板必须同步改此处，
 * 否则微信会以 47003（参数不合法）拒收。</p>
 */
@Note("微信订阅消息触达")
@Component
public class SubscribeMessageContext {

    private static final Logger log = LoggerFactory.getLogger(SubscribeMessageContext.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** 微信 thing 类型字段上限 20 个字符，超出会被拒收。 */
    private static final int THING_MAX_LENGTH = 20;

    private final WechatMiniProgramClient wechatClient;
    private final WechatMiniProgramConfigure configure;
    private final CredentialsRepository credentialsRepository;
    private final CustomerAccessRepository customerAccessRepository;

    public SubscribeMessageContext(WechatMiniProgramClient wechatClient,
                                   WechatMiniProgramConfigure configure,
                                   CredentialsRepository credentialsRepository,
                                   CustomerAccessRepository customerAccessRepository) {
        this.wechatClient = wechatClient;
        this.configure = configure;
        this.credentialsRepository = credentialsRepository;
        this.customerAccessRepository = customerAccessRepository;
    }

    /**
     * 业主：阶段验收待确认。
     *
     * <p>这是业主需要主动回小程序确认的节点，也是复访价值最高的一次触达。
     * 发送对象是显式授权的业主身份（CustomerAccess），不做手机号或姓名模糊匹配。</p>
     */
    public void ownerAcceptancePending(Acceptance acceptance) {
        String scenario = WechatSubscribeScenario.OWNER_ACCEPTANCE_PENDING;
        String templateId = configure.subscribeTemplate(scenario);
        if (templateId == null || acceptance == null || acceptance.getProject() == null) {
            return;
        }
        Project project = acceptance.getProject();
        List<CustomerAccess> accesses = customerAccessRepository
                .findByProject_ProjectIdAndStatus(project.getProjectId(), CustomerAccessStatus.Active);
        if (accesses.isEmpty()) {
            return;
        }
        Map<String, String> data = new LinkedHashMap<>();
        data.put("thing1", thing(project.getName()));
        data.put("thing2", thing(acceptance.getStage() == null ? "阶段验收" : acceptance.getStage().getName()));
        data.put("time3", TIME_FORMATTER.format(LocalDateTime.now()));
        data.put("thing4", thing("请进入小程序确认或发起整改"));
        String page = "subpackages/project/pages/owner-acceptance?projectId=" + project.getProjectId();
        for (CustomerAccess access : accesses) {
            dispatch(access.getCredential(), scenario, templateId, page, data);
        }
    }

    /** 员工：业主已确认或驳回阶段验收。 */
    public void acceptanceDecision(Employee receiver, Acceptance acceptance, boolean confirmed) {
        String scenario = WechatSubscribeScenario.EMPLOYEE_ACCEPTANCE_DECISION;
        String templateId = configure.subscribeTemplate(scenario);
        if (templateId == null || receiver == null || acceptance == null || acceptance.getProject() == null) {
            return;
        }
        Project project = acceptance.getProject();
        Map<String, String> data = new LinkedHashMap<>();
        data.put("thing1", thing(project.getName()));
        data.put("phrase2", confirmed ? "已确认" : "已驳回");
        data.put("thing3", thing(acceptance.getStage() == null ? "阶段验收" : acceptance.getStage().getName()));
        data.put("time4", TIME_FORMATTER.format(LocalDateTime.now()));
        String page = "subpackages/project/pages/detail?id=" + project.getProjectId();
        Credentials credential = credentialsRepository
                .findFirstByEmployee_EmpIdAndTypeOrderByCreIdDesc(receiver.getEmpId(), CredType.Wx);
        dispatch(credential, scenario, templateId, page, data);
    }

    /**
     * 统一出口：解析微信身份、在事务提交后调用微信接口、吞掉所有失败。
     *
     * <p>不做"本地订阅额度记账"——一次性订阅的额度由微信侧维护，服务端重复调用失败
     * 会返回 43101；本地再记一份只会与微信侧不一致。</p>
     */
    private void dispatch(Credentials credential, String scenario, String templateId,
                          String page, Map<String, String> data) {
        if (credential == null || credential.getType() != CredType.Wx
                || credential.getIdKey() == null || credential.getIdKey().isBlank()) {
            return;
        }
        String openId = credential.getIdKey();
        afterCommit(() -> send(openId, scenario, templateId, page, data));
    }

    private void send(String openId, String scenario, String templateId,
                      String page, Map<String, String> data) {
        WechatSubscribeResult result;
        try {
            result = wechatClient.subscribeMessage(
                    new WechatSubscribeMessage(openId, templateId, page, data));
        } catch (RuntimeException exception) {
            log.warn("订阅消息调用异常: 场景={} 用户={} 原因={}",
                    scenario, fingerprint(openId), exception.getMessage());
            return;
        }
        if (result.delivered()) {
            log.info("订阅消息已发送: 场景={} 用户={}", scenario, fingerprint(openId));
        } else if (result.notSubscribed()) {
            // 用户未授权、已退订或一次性额度已用尽：正常状态，不记故障。
            log.info("订阅消息未发送(用户未订阅或额度已用尽): 场景={} 用户={}", scenario, fingerprint(openId));
        } else {
            log.warn("订阅消息发送失败: 场景={} 用户={} 错误码={} 原因={}",
                    scenario, fingerprint(openId), result.errorCode(), result.errorMessage());
        }
    }

    /** 有事务则提交后执行，无事务立即执行。 */
    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private String thing(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        String trimmed = value.trim();
        return trimmed.length() <= THING_MAX_LENGTH ? trimmed : trimmed.substring(0, THING_MAX_LENGTH);
    }

    /** openId 指纹：只用于日志比对，不落原文。 */
    private String fingerprint(String value) {
        if (value == null || value.isBlank()) {
            return "(空)";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                builder.append(String.format("%02x", digest[i]));
            }
            return builder.toString();
        } catch (Exception exception) {
            return "(不可计算)";
        }
    }
}
