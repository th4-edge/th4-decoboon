package com.th4.edge.platform.configure;

import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.platform.context.PlatformPasswordContext;
import com.th4.edge.platform.model.PlatformDepartment;
import com.th4.edge.platform.model.PlatformDepartmentStatus;
import com.th4.edge.platform.model.PlatformOrg;
import com.th4.edge.platform.model.PlatformOrgStatus;
import com.th4.edge.platform.model.PlatformRole;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.platform.model.PlatformUserStatus;
import com.th4.edge.platform.repository.PlatformDepartmentRepository;
import com.th4.edge.platform.repository.PlatformOrgRepository;
import com.th4.edge.platform.repository.PlatformUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 显式启用的独立平台组织与超级管理员初始化器。 */
@Note("平台组织初始化")
@Description("只在显式提供环境变量时创建平台独立组织、部门、超级管理员和 BCrypt 密码")
@Component
public class PlatformBootstrap implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformBootstrap.class);

    @Value("${decoboon.platform.bootstrap.enabled:false}") private boolean enabled;
    @Value("${decoboon.platform.bootstrap.org-code:DECOBOON}") private String orgCode;
    @Value("${decoboon.platform.bootstrap.org-name:装得清平台运营中心}") private String orgName;
    @Value("${decoboon.platform.bootstrap.department-code:PLATFORM}") private String departmentCode;
    @Value("${decoboon.platform.bootstrap.department-name:平台运营中心}") private String departmentName;
    @Value("${decoboon.platform.bootstrap.phone:}") private String phone;
    @Value("${decoboon.platform.bootstrap.username:平台超级管理员}") private String username;
    @Value("${decoboon.platform.bootstrap.password:}") private String password;

    private final PlatformOrgRepository orgRepository;
    private final PlatformDepartmentRepository departmentRepository;
    private final PlatformUserRepository userRepository;
    private final PlatformPasswordContext passwordContext;

    public PlatformBootstrap(PlatformOrgRepository orgRepository,
                             PlatformDepartmentRepository departmentRepository,
                             PlatformUserRepository userRepository,
                             PlatformPasswordContext passwordContext) {
        this.orgRepository = orgRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.passwordContext = passwordContext;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        if (phone == null || phone.isBlank() || password == null || password.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("PLATFORM_BOOTSTRAP_INVALID",
                    "平台初始化必须提供手机号和密码"));
        }
        if (orgCode == null || orgCode.isBlank() || orgName == null || orgName.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("PLATFORM_BOOTSTRAP_INVALID", "平台组织配置不完整"));
        }
        passwordContext.validate(password);
        PlatformOrg org = orgRepository.findByCode(orgCode.trim());
        if (org == null) {
            org = new PlatformOrg();
            org.setCode(orgCode.trim());
            org.setName(orgName.trim());
            org.setStatus(PlatformOrgStatus.Active);
            org = orgRepository.save(org);
        } else if (org.getStatus() != PlatformOrgStatus.Active) {
            org.setStatus(PlatformOrgStatus.Active);
            org = orgRepository.save(org);
        }

        PlatformDepartment department = departmentRepository.findByOrg_OrgIdAndCode(org.getOrgId(), departmentCode.trim());
        if (department == null) {
            department = new PlatformDepartment();
            department.setOrg(org);
            department.setCode(departmentCode.trim());
            department.setName(departmentName == null || departmentName.isBlank() ? "平台运营中心" : departmentName.trim());
            department.setSort(1);
            department.setStatus(PlatformDepartmentStatus.Active);
            department = departmentRepository.save(department);
        }

        PlatformUser user = userRepository.findByPhone(phone.trim());
        if (user != null && (user.getOrg() == null || !org.getOrgId().equals(user.getOrg().getOrgId()))) {
            throw new DefinedException(new DefaultErrorCode("PLATFORM_BOOTSTRAP_CONFLICT",
                    "该手机号已属于其他平台组织，禁止自动迁移"));
        }
        if (user == null) {
            user = new PlatformUser();
            user.setOrg(org);
            user.setDepartment(department);
            user.setName(username == null || username.isBlank() ? "平台超级管理员" : username.trim());
            user.setPhone(phone.trim());
            user.setRole(PlatformRole.SuperAdmin);
            user.setStatus(PlatformUserStatus.Active);
        } else {
            if (user.getRole() != PlatformRole.SuperAdmin) {
                throw new DefinedException(new DefaultErrorCode("PLATFORM_BOOTSTRAP_CONFLICT",
                        "已有平台用户不是超级管理员，禁止自动提升角色"));
            }
            user.setStatus(PlatformUserStatus.Active);
            if (department != null) user.setDepartment(department);
        }
        user.setPasswordHash(passwordContext.encode(password));
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        LOGGER.info("平台独立组织初始化完成，组织={}，手机号后四位={}", org.getName(), phone.substring(Math.max(0, phone.length() - 4)));
    }
}
