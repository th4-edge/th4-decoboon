package com.th4.edge.tenant.billing.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时补齐订阅套餐的席位、门店和功能模块权益。
 */
@Note("套餐权益初始化")
@Component
public class SubscriptionPlanInitializer implements ApplicationRunner {

    private final SubscriptionContext subscriptionContext;

    public SubscriptionPlanInitializer(SubscriptionContext subscriptionContext) {
        this.subscriptionContext = subscriptionContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        subscriptionContext.initializePlans();
    }
}
