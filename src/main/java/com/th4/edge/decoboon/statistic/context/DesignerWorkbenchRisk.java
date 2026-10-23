package com.th4.edge.decoboon.statistic.context;

/** 设计师工作队列的唯一风险与下一步判定。仅供服务端投影消费。 */
public final class DesignerWorkbenchRisk {

    private DesignerWorkbenchRisk() {
    }

    public record QuoteDecision(String riskLevel, String riskReason, String nextActionKey, String nextActionLabel) {
    }

    public record SignDecision(String riskLevel, String riskReason, String nextActionKey, String nextActionLabel) {
    }

    public static QuoteDecision quote(String status, long stayDays) {
        boolean draft = "Draft".equals(status);
        boolean submitted = "Submitted".equals(status);
        boolean rejected = "Rejected".equals(status);
        boolean danger = (draft && stayDays >= 7)
                || (submitted && stayDays >= 3)
                || (rejected && stayDays >= 1);
        String reason = danger ? switch (status) {
            case "Draft" -> "草稿已停留 " + stayDays + " 天，请尽快完善并提交";
            case "Submitted" -> "待客户确认已停留 " + stayDays + " 天，请尽快跟进";
            case "Rejected" -> "客户已驳回 " + stayDays + " 天，请修订后重报";
            default -> null;
        } : null;
        if (draft) return new QuoteDecision(danger ? "Danger" : "Normal", reason, "continueEdit", "继续编辑并提交");
        if (submitted) return new QuoteDecision(danger ? "Danger" : "Normal", reason, "urgeCustomer", "跟进客户确认");
        return new QuoteDecision(danger ? "Danger" : "Normal", reason, "reviseAndResubmit", "修订后重新提交");
    }

    public static SignDecision sign(boolean converted, String contractStatus, boolean hasProject, long stayDays) {
        if (hasProject) {
            return new SignDecision("Normal", null, "viewProject", "查看项目");
        }
        if ("Effective".equals(contractStatus) || "Active".equals(contractStatus)
                || "Changing".equals(contractStatus) || "Changed".equals(contractStatus)
                || "Completed".equals(contractStatus) || "Settled".equals(contractStatus)
                || "Closed".equals(contractStatus) || "Terminated".equals(contractStatus)) {
            return new SignDecision("Warning", "合同已生效但项目尚未建立，请检查生效流程",
                    "waitProjectCreated", "合同已生效待项目建立");
        }
        if ("Signed".equals(contractStatus)) {
            return new SignDecision("Normal", null, "waitEnterpriseEffective", "待企业确认生效");
        }
        if (converted && (contractStatus == null || contractStatus.isBlank())) {
            return new SignDecision("Normal", null, "viewContract", "查看合同");
        }
        String level;
        String reason;
        if (stayDays >= 7) {
            level = "Danger";
            reason = "报价确认后已停留 " + stayDays + " 天，存在较高流失风险，请立即跟进";
        } else if (stayDays >= 3) {
            level = "Warning";
            reason = "报价确认后已停留 " + stayDays + " 天，建议尽快推动签约";
        } else {
            level = "Normal";
            reason = null;
        }
        if (contractStatus == null || contractStatus.isBlank()) {
            return new SignDecision(level, reason, "createContract", "发起合同");
        }
        if ("PendingSign".equals(contractStatus)) {
            return new SignDecision(level, reason, "waitEnterpriseSign", "待企业确认签约");
        }
        if ("Signed".equals(contractStatus)) {
            return new SignDecision(level, reason, "waitEnterpriseEffective", "待企业确认生效");
        }
        return new SignDecision(level, reason, "viewContract", "查看合同进度");
    }
}
