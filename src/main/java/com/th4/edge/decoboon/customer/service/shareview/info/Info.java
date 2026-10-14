package com.th4.edge.decoboon.customer.service.shareview.info;

import com.th4.edge.decoboon.customer.model.ProjectShareLink;
import com.th4.edge.decoboon.customer.model.ShareJourneyView;
import com.th4.edge.framework.provider.Provider;

/**
 * 亲友只读视图（**免登录**）。
 *
 * 刻意**不继承 Security**：这是本能力存在的意义——家人/好友点开分享链接时
 * 还没有账号，登录墙会让分享功能失效。参照 site/lead/submit 的免登录先例。
 *
 * 安全依赖：令牌唯一 + 带时效 + 可撤销 + 服务端脱敏 + 不区分"不存在/已过期/已撤销"。
 */
public interface Info extends Provider<ProjectShareLink, ShareJourneyView> {
}
