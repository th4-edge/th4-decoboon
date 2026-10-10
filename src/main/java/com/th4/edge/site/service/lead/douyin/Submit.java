package com.th4.edge.site.service.lead.douyin;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.site.model.SiteLead;

/**
 * 抖音渠道线索提交。
 *
 * <p>L1 自营获客小程序（装得清 · 经营体检）的公开留资入口。
 * 线索进入平台域 {@link SiteLead}，不进入任何租户客户/房屋主链（DEC-031）。
 * 内部项目代号「玄关」，禁止对外。</p>
 */
public interface Submit extends Provider<SiteLead, SiteLead> {
}
