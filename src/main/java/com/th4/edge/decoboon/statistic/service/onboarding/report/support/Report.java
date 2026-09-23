package com.th4.edge.decoboon.statistic.service.onboarding.report.support;

import com.th4.edge.decoboon.statistic.context.OnboardingGuideContext;
import com.th4.edge.decoboon.statistic.model.GuideStep;
import com.th4.edge.decoboon.statistic.model.OnboardingGuide;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 当前角色启动引导状态投影实现。 */
@Note("全角色启动引导")
@Properties(allow = {
        @AllowProperties(model = OnboardingGuide.class,
                properties = {"audience", "audienceLabel", "title", "subtitle", "status", "requiredTotal",
                        "requiredCompleted", "nextAction", "message", "steps", "refreshedAt"}),
        @AllowProperties(model = GuideStep.class,
                properties = {"key", "label", "description", "required", "status", "count", "sourceProvider",
                        "completionPredicate", "action", "actionLabel"})
})
@Service("statistic/onboarding/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.onboarding.report.Report {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    private final OnboardingGuideContext guideContext;

    public Report(OnboardingGuideContext guideContext) {
        this.guideContext = guideContext;
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingGuide call(Credentials request) {
        return guideContext.guide(principal);
    }
}
