package com.th4.edge.decoboon.quote.context;

import com.th4.edge.decoboon.common.model.BusinessDerivedLabel;
import com.th4.edge.decoboon.common.model.BusinessNextAction;
import com.th4.edge.decoboon.common.model.BusinessStateView;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 报价服务端状态视图。 */
@Note("报价服务端状态视图")
@Component
public class QuoteBusinessStateContext {

    public void enrich(Quote quote) {
        if (quote == null || quote.getQuoteId() == null || quote.getStatus() == null) {
            return;
        }
        BusinessStateView view = new BusinessStateView();
        view.setObjectKey("quote");
        view.setObjectId(quote.getQuoteId());
        view.setStatus(quote.getStatus().name());
        view.setStatusLabel(quote.getStatus().getName());
        view.setStatusTone(tone(quote.getStatus()));
        view.setDerivedLabels(derivedLabels(quote));
        view.setNextAction(nextAction(quote));
        view.setBlockedReasons(List.of());
        view.setNextOwner(quote.getCreatedBy() == null ? null : quote.getCreatedBy().getName());
        view.setUpdatedAt(quote.getUpdateTime() == null ? null : quote.getUpdateTime().toLocalDateTime());
        view.setBusinessRefs(List.of("BR-001", "BR-002", "MET-001"));
        quote.setBusinessStateView(view);
    }

    private List<BusinessDerivedLabel> derivedLabels(Quote quote) {
        List<BusinessDerivedLabel> labels = new ArrayList<>();
        if (!Boolean.TRUE.equals(quote.getCostComplete())) {
            labels.add(new BusinessDerivedLabel("costIncomplete", "成本未完整", "pending"));
        }
        if (quote.getVersion() != null && quote.getVersion() > 0) {
            labels.add(new BusinessDerivedLabel("version", "V" + quote.getVersion(), "info"));
        }
        return labels;
    }

    private BusinessNextAction nextAction(Quote quote) {
        BusinessNextAction action = new BusinessNextAction();
        action.setFromStatus(quote.getStatus().name());
        action.setBusinessRefs(List.of("BR-002"));
        action.setBlockedReasons(List.of());
        action.setMissingFields(List.of());
        switch (quote.getStatus()) {
            case Draft -> {
                action.setKey("quote/continue");
                action.setLabel("继续报价");
                action.setHint("完善报价行并提交客户确认。");
                action.setOwnerRole("设计师");
                action.setToStatus("Submitted");
                action.setReloadScopes(List.of("quote", "design", "contract", "project"));
            }
            case Submitted -> {
                action.setKey("quote/follow-decision");
                action.setLabel("跟进客户确认");
                action.setHint("报价已提交，等待客户接受或驳回。");
                action.setOwnerRole("设计师");
                action.setToStatus("Accepted|Rejected");
                action.setReloadScopes(List.of("quote", "customer", "property", "design"));
            }
            case Accepted -> {
                action.setKey("contract/handoff");
                action.setLabel("进入合同交接");
                action.setHint("客户已接受，推进合同签署与生效。");
                action.setOwnerRole("设计师 / 企业负责人 / 店长");
                action.setToStatus("Converted");
                action.setReloadScopes(List.of("quote", "contract", "project", "receivable"));
            }
            case Rejected -> {
                action.setKey("quote/revise");
                action.setLabel("按反馈调整");
                action.setHint("基于驳回反馈生成新版本，不覆盖历史报价。");
                action.setOwnerRole("设计师");
                action.setToStatus("Draft");
                action.setReloadScopes(List.of("quote", "design", "contract", "project"));
            }
            case Superseded -> {
                action.setKey("quote/history");
                action.setLabel("查看历史版本");
                action.setHint("已被其他报价替代，仅保留历史查看。");
                action.setOwnerRole("设计师");
                action.setToStatus("Superseded");
                action.setEnabled(Boolean.FALSE);
                action.setReloadScopes(List.of("quote"));
            }
            case Converted -> {
                action.setKey("project/open");
                action.setLabel("查看项目");
                action.setHint("报价已转项目，进入项目履约。");
                action.setOwnerRole("项目参与角色");
                action.setToStatus("Converted");
                action.setReloadScopes(List.of("quote", "contract", "project"));
            }
        }
        return action;
    }

    private String tone(QuoteStatus status) {
        return switch (status) {
            case Draft -> "todo";
            case Submitted -> "pending";
            case Accepted -> "done";
            case Rejected -> "blocked";
            case Superseded -> "closed";
            case Converted -> "dark";
        };
    }
}
