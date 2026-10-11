package com.th4.edge.tenant.user.service.auth.session.devownerlogin;

import com.th4.edge.tenant.user.model.Credentials;

/**
 * 本地开发业主会话入口（仅 dev）。
 *
 * <p>业主正式登录只有微信授权一条路：openid 换码 → 绑定手机号 → 建立凭证。
 * 本地无法伪造微信授权（微信换码必须使用真实 code），导致业主侧链路（阶段验收确认/驳回、
 * 业主旅程、业主意见投诉）长期无法在本地端到端验证。本入口只做一件事：
 * 为**已存在、已绑定项目授权、且不关联员工**的微信业主凭证签发一次会话，
 * 让本地可以用真实业主身份走真实 Provider 链路。</p>
 *
 * <p>安全边界（必须同时满足才可用）：</p>
 * <ol>
 *   <li>配置开关 {@code decoboon.dev-owner-login.enabled=true}（默认 false）；</li>
 *   <li>当前 Spring profile 含 {@code dev}；</li>
 *   <li>手机号对应的凭证 type=Wx、status=Active、emp_id 为空（不关联任何员工）；</li>
 *   <li>该凭证至少存在一条 Active 的业主项目授权。</li>
 * </ol>
 */
public interface DevOwnerLogin extends com.th4.edge.framework.provider.security.login.Login<Credentials> {}
