package com.th4.edge.components.wechat.configure;

/**
 * 订阅消息场景键。
 *
 * <p>场景键是"业务时机"与"微信模板 ID"之间的绑定关系：新增一个触达场景只需要在
 * 配置里补一行模板 ID，不需要改动发送代码。未配置模板 ID 的场景一律跳过发送，
 * 因此本能力上线时不会影响任何既有业务。</p>
 */
public final class WechatSubscribeScenario {

    /** 业主：阶段验收待确认（需要业主回到小程序确认） */
    public static final String OWNER_ACCEPTANCE_PENDING = "owner.acceptance-pending";

    /** 员工：业主已确认或驳回验收 */
    public static final String EMPLOYEE_ACCEPTANCE_DECISION = "employee.acceptance-decision";

    /** 员工：施工任务逾期 */
    public static final String EMPLOYEE_TASK_OVERDUE = "employee.task-overdue";

    /** 业主：计划偏差已确认（超窗处理结论需客户知晓） */
    public static final String OWNER_PLAN_DEVIATION_CONFIRMED = "owner.plan-deviation-confirmed";

    /** 员工：材料到货待确认 */
    public static final String EMPLOYEE_MATERIAL_ARRIVAL = "employee.material-arrival";

    private WechatSubscribeScenario() {
    }
}
