package com.th4.edge.decoboon.quote.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 报价场景白名单。
 *
 * <p>场景只裁剪字段、动作和展示密度，不参与数据范围判断，也不改变服务端分组结果。
 * 成本、内部公式等角色敏感字段即使场景允许，也必须继续经过既有角色权限裁剪。</p>
 */
@Note("报价视图场景")
public enum QuoteViewScene implements Enumerable<String> {
    EDIT("编辑", true, true, true, true, true, true, true, false,
            modes(QuoteViewMode.REGION, QuoteViewMode.TYPE), QuoteViewMode.REGION,
            QuoteViewAction.SAVE, QuoteViewAction.SUBMIT, QuoteViewAction.REVISE, QuoteViewAction.ADJUST),
    READ("内部详情", true, true, true, true, true, true, true, true,
            modes(QuoteViewMode.REGION, QuoteViewMode.TYPE), QuoteViewMode.REGION,
            QuoteViewAction.VIEW, QuoteViewAction.ACCEPT, QuoteViewAction.REJECT,
            QuoteViewAction.CREATE_CONTRACT, QuoteViewAction.PRINT),
    OWNER("业主只读", false, true, false, false, true, false, false, false,
            modes(QuoteViewMode.REGION, QuoteViewMode.TYPE), QuoteViewMode.REGION,
            QuoteViewAction.VIEW, QuoteViewAction.EXPAND_SOURCE, QuoteViewAction.PRINT),
    PROJECT("项目上下文", false, true, true, false, true, false, true, true,
            modes(QuoteViewMode.REGION, QuoteViewMode.TYPE), QuoteViewMode.REGION,
            QuoteViewAction.VIEW, QuoteViewAction.NAVIGATE),
    CONTRACT("合同宿主", false, false, false, false, true, false, true, true,
            modes(), null, QuoteViewAction.NAVIGATE),
    SUMMARY("客户/项目摘要", false, false, false, false, false, false, true, false,
            modes(), null, QuoteViewAction.NAVIGATE),
    PRINT("PDF 打印", false, true, false, false, true, false, false, false,
            modes(QuoteViewMode.REGION, QuoteViewMode.TYPE), QuoteViewMode.REGION,
            QuoteViewAction.GENERATE, QuoteViewAction.REGENERATE, QuoteViewAction.OPEN_ARCHIVE);

    private final String name;
    private final boolean rawItemsVisible;
    private final boolean groupedLinesVisible;
    private final boolean costVisibleToAuthorizedRole;
    private final boolean internalFormulaVisibleToAuthorizedRole;
    private final boolean acceptedAtVisible;
    private final boolean acceptedByVisible;
    private final boolean costSummaryVisibleToAuthorizedRole;
    private final boolean versionTrackVisible;
    private final Set<QuoteViewMode> groupModes;
    private final QuoteViewMode defaultGroupMode;
    private final Set<QuoteViewAction> actions;

    QuoteViewScene(String name,
                   boolean rawItemsVisible,
                   boolean groupedLinesVisible,
                   boolean costVisibleToAuthorizedRole,
                   boolean internalFormulaVisibleToAuthorizedRole,
                   boolean acceptedAtVisible,
                   boolean acceptedByVisible,
                   boolean costSummaryVisibleToAuthorizedRole,
                   boolean versionTrackVisible,
                   Set<QuoteViewMode> groupModes,
                   QuoteViewMode defaultGroupMode,
                   QuoteViewAction... actions) {
        this.name = name;
        this.rawItemsVisible = rawItemsVisible;
        this.groupedLinesVisible = groupedLinesVisible;
        this.costVisibleToAuthorizedRole = costVisibleToAuthorizedRole;
        this.internalFormulaVisibleToAuthorizedRole = internalFormulaVisibleToAuthorizedRole;
        this.acceptedAtVisible = acceptedAtVisible;
        this.acceptedByVisible = acceptedByVisible;
        this.costSummaryVisibleToAuthorizedRole = costSummaryVisibleToAuthorizedRole;
        this.versionTrackVisible = versionTrackVisible;
        this.groupModes = groupModes;
        this.defaultGroupMode = defaultGroupMode;
        this.actions = Collections.unmodifiableSet(EnumSet.copyOf(Set.of(actions)));
    }

    private static Set<QuoteViewMode> modes(QuoteViewMode... modes) {
        if (modes.length == 0) {
            return Set.of();
        }
        return Collections.unmodifiableSet(EnumSet.of(modes[0], modes));
    }

    public boolean supports(QuoteViewMode mode) {
        return mode != null && groupModes.contains(mode);
    }

    public boolean exposesRawItems() {
        return rawItemsVisible;
    }

    public boolean exposesGroupedLines() {
        return groupedLinesVisible;
    }

    public boolean exposesCostToAuthorizedRole() {
        return costVisibleToAuthorizedRole;
    }

    public boolean exposesInternalFormulaToAuthorizedRole() {
        return internalFormulaVisibleToAuthorizedRole;
    }

    public boolean exposesAcceptedAt() {
        return acceptedAtVisible;
    }

    public boolean exposesAcceptedBy() {
        return acceptedByVisible;
    }

    public boolean exposesCostSummaryToAuthorizedRole() {
        return costSummaryVisibleToAuthorizedRole;
    }

    public boolean exposesVersionTrack() {
        return versionTrackVisible;
    }

    public Set<QuoteViewMode> getGroupModes() {
        return groupModes;
    }

    public QuoteViewMode getDefaultGroupMode() {
        return defaultGroupMode;
    }

    public Set<QuoteViewAction> getActions() {
        return actions;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return name();
    }
}
