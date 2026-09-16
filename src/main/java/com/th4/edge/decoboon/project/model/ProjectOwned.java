package com.th4.edge.decoboon.project.model;

import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;

/** 项目域统一经营归属契约。 */
public interface ProjectOwned {

    Company getCompany();

    void setCompany(Company company);

    Store getStore();

    void setStore(Store store);

    Team getTeam();

    void setTeam(Team team);

    Employee getOwner();

    void setOwner(Employee owner);
}
