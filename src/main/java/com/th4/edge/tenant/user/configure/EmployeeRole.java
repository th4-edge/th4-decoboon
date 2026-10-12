package com.th4.edge.tenant.user.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 企业成员角色。
 */
@Note("员工角色")
public enum EmployeeRole implements Enumerable<String> {

    Boss("企业负责人", DataScopeType.Company),
    StoreManager("店长", DataScopeType.Store),
    Designer("设计师", DataScopeType.Self),
    Marketer("业务员", DataScopeType.Self),
    Pm("项目经理", DataScopeType.Self),
    // DEC-065 FIN-STORE-01：财务数据范围由企业收窄为归属门店；
    // 归属「默认总店」的财务由 DataScopeContext 升为总部财务（企业范围，FIN-STORE-03）。
    Finance("财务", DataScopeType.Store),
    TeamLeader("团队负责人", DataScopeType.Team);

    private final String name;
    private final DataScopeType dataScopeType;

    EmployeeRole(String name, DataScopeType dataScopeType) {
        this.name = name;
        this.dataScopeType = dataScopeType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return name();
    }

    public DataScopeType getDataScopeType() {
        return dataScopeType;
    }
}
