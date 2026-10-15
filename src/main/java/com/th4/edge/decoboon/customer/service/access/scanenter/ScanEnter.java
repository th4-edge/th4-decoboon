package com.th4.edge.decoboon.customer.service.access.scanenter;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Credentials;

/** 扫码开通业主端（项目码 + 手机号一致）。 */
public interface ScanEnter extends Provider<Credentials, Credentials>, Security {
}
