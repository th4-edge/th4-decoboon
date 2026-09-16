package com.th4.edge.tenant.user.context;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.InviteStatus;
import com.th4.edge.tenant.user.configure.MemberLogAction;
import com.th4.edge.tenant.user.configure.SecurityAction;
import com.th4.edge.tenant.user.configure.SecurityResult;
import com.th4.edge.tenant.user.model.CompanyJoinLimit;
import com.th4.edge.tenant.user.model.CompanyMemberLog;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Invite;
import com.th4.edge.tenant.user.model.MemberHandover;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.repository.CompanyJoinLimitRepository;
import com.th4.edge.tenant.user.repository.CompanyMemberLogRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.InviteRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import com.th4.edge.tenant.user.repository.TeamRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/** 邀请、加入、退出、切换、停用和企业归属限制。 */
@Component
public class MembershipContext {

    private static final int COOLDOWN_DAYS = 30;
    private static final int BLACKLIST_DAYS = 90;
    private static final int JOIN_WINDOW_DAYS = 180;
    private static final int MAX_JOINS = 3;
    private static final int INVITE_WINDOW_DAYS = 180;
    private static final int MAX_PHONE_INVITES = 10;
    private static final int PHONE_INVITE_WEEK = 7;
    private static final int MAX_WEEK_INVITES = 3;
    private static final int COMPANY_PENDING_LIMIT = 50;

    private final UserIdentityContext userContext;
    private final EmployeeRepository employeeRepository;
    private final InviteRepository inviteRepository;
    private final TeamRepository teamRepository;
    private final StoreRepository storeRepository;
    private final CompanyMemberLogRepository memberLogRepository;
    private final CompanyJoinLimitRepository joinLimitRepository;
    private final SubscriptionContext subscriptionContext;
    private final ModelIdentityContext modelIdentityContext;
    private final CustomerRepository customerRepository;
    private final PropertyRepository propertyRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;
    private final ReceivableRepository receivableRepository;

    public MembershipContext(UserIdentityContext userContext,
                             EmployeeRepository employeeRepository,
                             InviteRepository inviteRepository,
                             TeamRepository teamRepository,
                             StoreRepository storeRepository,
                             CompanyMemberLogRepository memberLogRepository,
                             CompanyJoinLimitRepository joinLimitRepository,
                             SubscriptionContext subscriptionContext,
                             ModelIdentityContext modelIdentityContext,
                             CustomerRepository customerRepository,
                             PropertyRepository propertyRepository,
                             QuoteRepository quoteRepository,
                             ProjectRepository projectRepository,
                             ProjectTaskRepository taskRepository,
                             ReceivableRepository receivableRepository) {
        this.userContext = userContext;
        this.employeeRepository = employeeRepository;
        this.inviteRepository = inviteRepository;
        this.teamRepository = teamRepository;
        this.storeRepository = storeRepository;
        this.memberLogRepository = memberLogRepository;
        this.joinLimitRepository = joinLimitRepository;
        this.subscriptionContext = subscriptionContext;
        this.modelIdentityContext = modelIdentityContext;
        this.customerRepository = customerRepository;
        this.propertyRepository = propertyRepository;
        this.quoteRepository = quoteRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.receivableRepository = receivableRepository;
    }

    public void validateJoin(Credentials user) {
        validateJoinLimit(user, user.getPhone());
    }

    @Transactional
    public void recordJoin(Credentials user, String phone) {
        updateJoinLimit(user, phone, true);
    }

    @Transactional
    public Invite createInvite(Employee inviter, String inviteName, String phone,
                               EmployeeRole role, Long teamId, String teamName) {
        EmployeeRole inviteRole = role == null ? EmployeeRole.Marketer : role;
        requireInviteScope(inviter, inviteRole);
        validateInviteFrequency(inviter, phone);
        subscriptionContext.requireWritable(inviter.getCompany());
        subscriptionContext.lockCurrent(inviter.getCompany());
        long occupiedSeats = employeeRepository.countByCompany_ComIdAndStatus(
                inviter.getCompany().getComId(), EmployeeStatus.Active)
                + inviteRepository.countByCompany_ComIdAndStatus(
                inviter.getCompany().getComId(), InviteStatus.Pending);
        subscriptionContext.requireSeatCapacity(inviter.getCompany(), occupiedSeats);

        Team team = resolveTeam(inviter, inviteRole, teamId, teamName);
        Invite invite = new Invite();
        invite.setCompany(inviter.getCompany());
        invite.setStore(inviter.getStore());
        invite.setTeam(team);
        invite.setInviter(inviter);
        invite.setInviteName(inviteName == null || inviteName.isBlank() ? phone : inviteName.trim());
        invite.setTelNo(phone);
        invite.setRole(inviteRole);
        invite.setStatus(InviteStatus.Pending);
        invite.setExpireTime(LocalDateTime.now().plusDays(7));
        return inviteRepository.save(modelIdentityContext.prepare(invite));
    }

    public Invite requireInvite(Long inviteId) {
        Invite invite = inviteRepository.findByInviteId(inviteId);
        if (invite == null) {
            throw new DefinedException(new DefaultErrorCode("404", "邀请不存在"));
        }
        if (invite.getStatus() == InviteStatus.Pending
                && invite.getExpireTime() != null
                && invite.getExpireTime().isBefore(LocalDateTime.now())) {
            invite.setStatus(InviteStatus.Expired);
            inviteRepository.save(invite);
        }
        return invite;
    }

    @Transactional
    public Employee accept(Long inviteId, String openId, String unionId, String phone,
                           String name, boolean confirmSwitch, String reason, String avatarUrl) {
        Invite invite = requireInvite(inviteId);
        if (invite.getStatus() != InviteStatus.Pending) {
            throw new DefinedException(new DefaultErrorCode("400", inviteExpiredMessage(invite.getStatus())));
        }
        if (!Objects.equals(invite.getTelNo(), phone)) {
            throw new DefinedException(new DefaultErrorCode("400", "授权手机号与邀请手机号不一致"));
        }

        Credentials user = userContext.identify(openId, unionId, phone,
                invite.getInviteName(), null);
        user = userContext.lock(user.getCreId());
        Employee active = userContext.activeEmployee(user);
        if (active != null) {
            if (active.getCompany() != null
                    && invite.getCompany() != null
                    && Objects.equals(active.getCompany().getComId(), invite.getCompany().getComId())) {
                throw new DefinedException(new DefaultErrorCode("400", "当前用户已经加入该企业"));
            }
            if (!confirmSwitch) {
                throw new DefinedException(new DefaultErrorCode("409", "当前用户已加入其他企业，请确认切换企业"));
            }
            validateJoinLimit(user, phone);
            resignInternal(active, user, reason, true, MemberLogAction.Switch);
        } else {
            validateJoinLimit(user, phone);
        }

        subscriptionContext.requireWritable(invite.getCompany());
        subscriptionContext.lockCurrent(invite.getCompany());
        long activeSeats = employeeRepository.countByCompany_ComIdAndStatus(
                invite.getCompany().getComId(), EmployeeStatus.Active);
        subscriptionContext.requireSeatCapacity(invite.getCompany(), activeSeats);

        Employee employee = new Employee();
        employee.setName(name == null || name.isBlank() ? invite.getInviteName() : name.trim());
        employee.setTelNo(phone);
        employee.setStore(invite.getStore());
        employee.setTeam(invite.getTeam());
        employee.setCompany(invite.getCompany());
        employee.setRole(invite.getRole());
        employee.setStatus(EmployeeStatus.Active);
        employee.setJoinedAt(LocalDateTime.now());
        employee.setAvatarUrl(avatarUrl);
        employee.setInvite(invite);
        employee = employeeRepository.save(modelIdentityContext.prepare(employee));
        userContext.linkEmployee(user, employee);

        if (invite.getRole() == EmployeeRole.TeamLeader && invite.getTeam() != null) {
            invite.getTeam().setLeader(employee);
            teamRepository.save(invite.getTeam());
        }
        invite.setStatus(InviteStatus.Accepted);
        invite.setAcceptedAt(LocalDateTime.now());
        invite.setAcceptedBy(user);
        inviteRepository.save(invite);
        memberLog(user, invite.getCompany(), employee, MemberLogAction.Join, null, snapshot(employee),
                null, "接受邀请加入企业");
        updateJoinLimit(user, phone, true);
        userContext.log(user, SecurityAction.InviteAccept, SecurityResult.Success, employee, null,
                "接受企业邀请");
        userContext.log(user, SecurityAction.CompanyJoin, SecurityResult.Success, employee, null,
                invite.getCompany().getName());
        return employee;
    }


    @Transactional
    public Invite reject(Long inviteId, String phone, String reason) {
        Invite invite = requireInvite(inviteId);
        if (invite.getStatus() != InviteStatus.Pending) {
            throw new DefinedException(new DefaultErrorCode("400", inviteExpiredMessage(invite.getStatus())));
        }
        if (!Objects.equals(invite.getTelNo(), phone)) {
            throw new DefinedException(new DefaultErrorCode("400", "手机号与邀请不一致"));
        }
        invite.setStatus(InviteStatus.Rejected);
        invite.setRejectedAt(LocalDateTime.now());
        Invite saved = inviteRepository.save(invite);
        Credentials user = userRepositoryForPhone(phone);
        if (user != null) {
            userContext.log(user, SecurityAction.InviteReject, SecurityResult.Success, null, reason, null);
        }
        return saved;
    }

    @Transactional
    public Invite cancel(Employee operator, Long inviteId, String reason) {
        Invite invite = requireInvite(inviteId);
        if (invite.getStatus() != InviteStatus.Pending) {
            throw new DefinedException(new DefaultErrorCode("400", inviteExpiredMessage(invite.getStatus())));
        }
        boolean sameCompany = Objects.equals(operator.getCompany().getComId(), invite.getCompany().getComId());
        if (!sameCompany || (operator.getRole() != EmployeeRole.Boss
                && !Objects.equals(operator.getEmpId(), invite.getInviter().getEmpId()))) {
            throw new DefinedException(new DefaultErrorCode("403", "无权取消该邀请"));
        }
        invite.setStatus(InviteStatus.Cancelled);
        invite.setCancelledAt(LocalDateTime.now());
        invite.setCancelledBy(operator);
        return inviteRepository.save(invite);
    }

    private Credentials userRepositoryForPhone(String phone) {
        return userContext.findWxByPhone(phone);
    }

    @Transactional
    public MemberHandover resign(Employee current, String reason, boolean forceHandover) {
        return resignInternal(current, userContext.requireWx(current), reason, forceHandover, MemberLogAction.Resign);
    }

    @Transactional
    public MemberHandover adminResign(Employee operator, Long empId, String reason, boolean forceHandover) {
        Employee target = employeeRepository.findByEmpId(empId);
        if (target == null) {
            throw new DefinedException(new DefaultErrorCode("404", "员工不存在"));
        }
        requireManageScope(operator, target);
        if (Objects.equals(operator.getEmpId(), target.getEmpId())) {
            throw new DefinedException(new DefaultErrorCode("400", "不能注销自己"));
        }
        return resignInternal(target, userContext.requireWx(target), reason, forceHandover, MemberLogAction.Resign);
    }

    @Transactional
    public Employee suspend(Employee operator, Long empId) {
        Employee target = requireTarget(operator, empId);
        requireNotLastBoss(target);
        if (target.getStatus() != EmployeeStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("400", "仅在职员工可以停用"));
        }
        String before = snapshot(target);
        target.setStatus(EmployeeStatus.Suspended);
        Employee saved = employeeRepository.save(target);
        memberLog(userContext.requireWx(target), target.getCompany(), target, MemberLogAction.Suspend,
                before, snapshot(target), operator, "管理员停用员工");
        return saved;
    }

    @Transactional
    public Employee resume(Employee operator, Long empId) {
        Employee target = requireTarget(operator, empId);
        if (target.getStatus() != EmployeeStatus.Suspended) {
            throw new DefinedException(new DefaultErrorCode("400", "仅停用员工可以恢复"));
        }
        if (userContext.requireWx(target) != null && userContext.activeEmployee(userContext.requireWx(target)) != null) {
            throw new DefinedException(new DefaultErrorCode("400", "该用户已有其他在职企业"));
        }
        String before = snapshot(target);
        target.setStatus(EmployeeStatus.Active);
        target.setJoinedAt(LocalDateTime.now());
        Employee saved = employeeRepository.save(target);
        memberLog(userContext.requireWx(target), target.getCompany(), target, MemberLogAction.Resume,
                before, snapshot(target), operator, "管理员恢复员工");
        return saved;
    }

    @Transactional
    public MemberHandover transfer(Employee operator, Long fromEmpId, Long toEmpId, boolean transferProjects) {
        Employee from = requireTarget(operator, fromEmpId);
        Employee to = employeeRepository.findByEmpIdAndCompany_ComId(toEmpId, operator.getCompany().getComId());
        if (to == null || to.getStatus() != EmployeeStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("404", "接收员工不存在或非在职"));
        }
        if (transferProjects && operator.getRole() != EmployeeRole.Boss) {
            throw new DefinedException(new DefaultErrorCode("403", "项目负责人转移需企业负责人确认"));
        }
        MemberHandover summary = handover(from);
        Long comId = from.getCompany().getComId();
        List<Customer> customers = customerRepository.findByCompany_ComIdAndOwner_EmpId(comId, from.getEmpId());
        for (Customer customer : customers) {
            customer.setOwner(to);
            customer.setStore(to.getStore());
            customer.setTeam(to.getTeam());
            customerRepository.save(customer);
        }
        List<Property> properties = propertyRepository.findByCompany_ComIdAndOwner_EmpIdOrderByPropertyIdDesc(comId, from.getEmpId());
        for (Property property : properties) {
            property.setOwner(to);
            property.setStore(to.getStore());
            property.setTeam(to.getTeam());
            propertyRepository.save(property);
        }
        if (transferProjects) {
            List<Project> projects = projectRepository.findByCompany_ComIdAndManager_EmpIdOrderByProjectIdDesc(comId, from.getEmpId());
            for (Project project : projects) {
                project.setManager(to);
                projectRepository.save(project);
                for (ProjectTask task : taskRepository.findByProject(project)) {
                    if (task.getOwner() != null && Objects.equals(task.getOwner().getEmpId(), from.getEmpId())) {
                        task.setOwner(to);
                        taskRepository.save(task);
                    }
                }
            }
        }
        memberLog(userContext.requireWx(to), operator.getCompany(), from, MemberLogAction.Handover,
                snapshot(from), "receiver=" + to.getEmpId(), operator, "客户/房屋等业务归属交接");
        return summary;
    }

    public Employee memberInfo(Employee operator, Long empId) {
        return requireTarget(operator, empId);
    }

    @Transactional
    public Employee updateMember(Employee operator, Long empId, EmployeeRole role, Long storeId, Long teamId) {
        Employee target = requireTarget(operator, empId);
        String before = snapshot(target);
        EmployeeRole targetRole = role == null ? target.getRole() : role;
        if (targetRole == EmployeeRole.Boss && operator.getRole() != EmployeeRole.Boss) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人可以设置负责人角色"));
        }
        if (target.getRole() == EmployeeRole.Boss && targetRole != EmployeeRole.Boss) {
            requireNotLastBoss(target);
        }
        if (storeId != null) {
            com.th4.edge.tenant.user.model.Store store = storeRepository.findByStoreId(storeId);
            if (store == null || store.getCompany() == null
                    || !Objects.equals(store.getCompany().getComId(), operator.getCompany().getComId())) {
                throw new DefinedException(new DefaultErrorCode("404", "门店不存在"));
            }
            target.setStore(store);
        }
        if (teamId != null) {
            Team team = teamRepository.findByTeamIdAndCompany_ComId(teamId, operator.getCompany().getComId());
            if (team == null) throw new DefinedException(new DefaultErrorCode("404", "团队不存在"));
            if (target.getStore() != null && team.getStore() != null
                    && !Objects.equals(target.getStore().getStoreId(), team.getStore().getStoreId())) {
                throw new DefinedException(new DefaultErrorCode("400", "团队不属于员工门店"));
            }
            target.setTeam(team);
        }
        boolean roleChanged = target.getRole() != targetRole;
        target.setRole(targetRole);
        Employee saved = employeeRepository.save(target);
        memberLog(userContext.requireWx(target), target.getCompany(), saved,
                roleChanged ? MemberLogAction.RoleChange : MemberLogAction.TeamChange,
                before, snapshot(saved), operator, "管理员调整成员组织或角色");
        return saved;
    }

    public MemberHandover handoverFor(Employee operator, Long empId) {
        return handover(requireTarget(operator, empId));
    }

    public MemberHandover handover(Employee employee) {
        MemberHandover summary = new MemberHandover();
        summary.setEmployee(employee);
        Long comId = employee.getCompany().getComId();
        Long empId = employee.getEmpId();
        summary.setCustomerCount(customerRepository.findByCompany_ComIdAndOwner_EmpId(comId, empId).size());
        summary.setPropertyCount(propertyRepository.findByCompany_ComIdAndOwner_EmpIdOrderByPropertyIdDesc(comId, empId).size());
        summary.setQuoteCount(quoteRepository.findByCompany_ComIdAndCreatedBy_EmpIdOrderByQuoteIdDesc(comId, empId).size());
        List<Project> projects = projectRepository.findByCompany_ComIdAndManager_EmpIdOrderByProjectIdDesc(comId, empId);
        summary.setProjectCount(projects.size());
        int tasks = 0;
        int receivables = 0;
        for (Project project : projects) {
            tasks += (int) taskRepository.findByProject(project).stream()
                    .filter(task -> task.getStatus() != com.th4.edge.decoboon.project.configure.TaskStatus.Done
                            && task.getStatus() != com.th4.edge.decoboon.project.configure.TaskStatus.Cancelled)
                    .count();
            receivables += receivableRepository.findByProjectOrderByDueDateAsc(project).size();
        }
        summary.setTaskCount(tasks);
        summary.setReceivableCount(receivables);
        return summary;
    }

    private MemberHandover resignInternal(Employee employee, Credentials user, String reason,
                                          boolean forceHandover, MemberLogAction action) {
        if (user == null && employee.getTelNo() != null) {
            user = userContext.identify("phone:" + employee.getTelNo(), null, employee.getTelNo(),
                    employee.getName(), employee.getAvatarUrl());
            userContext.linkEmployee(user, employee);
        }
        if (employee.getStatus() != EmployeeStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("400", "当前员工不是在职状态"));
        }
        requireNotLastBoss(employee);
        MemberHandover handover = handover(employee);
        if (!forceHandover && handover.total() > 0) {
            throw new DefinedException(new DefaultErrorCode("409",
                    "存在未交接的客户、房屋、报价、项目或应收，请先完成交接或确认强制离职"));
        }
        String before = snapshot(employee);
        employee.setStatus(EmployeeStatus.Resigned);
        employee.setResignedAt(LocalDateTime.now());
        employee.setResignReason(reason);
        Employee saved = employeeRepository.save(employee);
        memberLog(user, employee.getCompany(), saved, action, before, snapshot(saved), employee, reason);
        if (user != null) {
            updateJoinLimit(user, employee.getTelNo(), false);
            userContext.log(user, action == MemberLogAction.Switch
                            ? SecurityAction.CompanySwitch : SecurityAction.CompanyResign,
                    SecurityResult.Success, saved, null, reason);
        }
        return handover;
    }

    private void validateJoinLimit(Credentials user, String phone) {
        CompanyJoinLimit limit = joinLimit(user, phone);
        LocalDateTime now = LocalDateTime.now();
        if (limit.getBlacklistUntil() != null && limit.getBlacklistUntil().isAfter(now)) {
            blocked(user, phone, "当前用户处于恶意离职黑名单期");
        }
        if (limit.getLastResignAt() != null
                && limit.getLastResignAt().plusDays(COOLDOWN_DAYS).isAfter(now)) {
            blocked(user, phone, "退出企业后 " + COOLDOWN_DAYS + " 天内不能加入新企业");
        }
        if (limit.getLastInviteAcceptAt() != null
                && limit.getLastInviteAcceptAt().plusDays(COOLDOWN_DAYS).isAfter(now)) {
            blocked(user, phone, "接受邀请后 " + COOLDOWN_DAYS + " 天内不能再次加入企业");
        }
        long joins = memberLogRepository.countRecentLogs(
                user.getCreId(), MemberLogAction.Join,
                now.minusDays(JOIN_WINDOW_DAYS).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        if (joins >= MAX_JOINS) {
            blocked(user, phone, "最近 " + JOIN_WINDOW_DAYS + " 天加入企业次数已达上限");
        }
    }

    private void validateInviteFrequency(Employee inviter, String phone) {
        Long comId = inviter.getCompany().getComId();
        LocalDateTime now = LocalDateTime.now();
        List<Invite> companyInvites = inviteRepository.findByCompany_ComIdOrderByInviteIdDesc(comId);
        long pending = companyInvites.stream()
                .filter(invite -> invite.getStatus() == InviteStatus.Pending)
                .filter(invite -> invite.getExpireTime() == null || invite.getExpireTime().isAfter(now))
                .filter(invite -> after(invite, now.minusDays(7)))
                .count();
        if (pending >= COMPANY_PENDING_LIMIT) {
            throw new DefinedException(new DefaultErrorCode("429", "企业待接受邀请已达上限"));
        }
        Invite pendingSamePhone = inviteRepository
                .findFirstByCompany_ComIdAndTelNoAndStatusOrderByInviteIdDesc(comId, phone, InviteStatus.Pending);
        if (pendingSamePhone != null) {
            throw new DefinedException(new DefaultErrorCode("400", "该手机号已有待接受邀请"));
        }
        List<Invite> phoneInvites = inviteRepository.findByTelNoOrderByInviteIdDesc(phone);
        long total = phoneInvites.stream().filter(invite -> after(invite, now.minusDays(INVITE_WINDOW_DAYS))).count();
        if (total >= MAX_PHONE_INVITES) {
            throw new DefinedException(new DefaultErrorCode("429", "该手机号邀请次数已达上限"));
        }
        long weekly = phoneInvites.stream().filter(invite -> after(invite, now.minusDays(PHONE_INVITE_WEEK))).count();
        if (weekly >= MAX_WEEK_INVITES) {
            throw new DefinedException(new DefaultErrorCode("429", "该手机号 7 天内邀请次数已达上限"));
        }
        boolean rejected = phoneInvites.stream()
                .filter(invite -> invite.getStatus() == InviteStatus.Rejected)
                .anyMatch(invite -> after(invite, now.minusDays(PHONE_INVITE_WEEK)));
        if (rejected) {
            throw new DefinedException(new DefaultErrorCode("429", "对方已拒绝邀请，7 天内不能重复邀请"));
        }
    }

    private void requireInviteScope(Employee inviter, EmployeeRole inviteRole) {
        if (inviter.getRole() != EmployeeRole.Boss
                && inviter.getRole() != EmployeeRole.StoreManager
                && inviter.getRole() != EmployeeRole.TeamLeader) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号无权邀请员工"));
        }
        if (inviteRole == EmployeeRole.Boss) {
            throw new DefinedException(new DefaultErrorCode("400", "不能邀请其他负责人"));
        }
        if (inviter.getRole() == EmployeeRole.StoreManager && inviteRole == EmployeeRole.StoreManager) {
            throw new DefinedException(new DefaultErrorCode("403", "门店店长不能邀请其他店长"));
        }
        if (inviter.getRole() == EmployeeRole.TeamLeader
                && (inviteRole == EmployeeRole.StoreManager || inviteRole == EmployeeRole.TeamLeader)) {
            throw new DefinedException(new DefaultErrorCode("403", "团队负责人只能邀请团队成员"));
        }
    }

    private Team resolveTeam(Employee inviter, EmployeeRole inviteRole, Long teamId, String teamName) {
        if (inviter.getRole() == EmployeeRole.TeamLeader) return inviter.getTeam();
        if (teamId != null && teamId > 0) {
            Team team = teamRepository.findByTeamIdAndCompany_ComId(teamId, inviter.getCompany().getComId());
            if (team == null) throw new DefinedException(new DefaultErrorCode("404", "团队不存在"));
            if (inviter.getStore() != null && team.getStore() != null
                    && !Objects.equals(inviter.getStore().getStoreId(), team.getStore().getStoreId())) {
                throw new DefinedException(new DefaultErrorCode("400", "团队不属于当前门店"));
            }
            return team;
        }
        if (inviteRole == EmployeeRole.TeamLeader) {
            if (teamName == null || teamName.isBlank()) {
                throw new DefinedException(new DefaultErrorCode("400", "团队负责人必须指定团队名称"));
            }
            Team team = new Team();
            team.setName(teamName.trim());
            team.setColor("#2F54EB");
            team.setLeader(inviter);
            team.setStore(inviter.getStore());
            team.setCompany(inviter.getCompany());
            return teamRepository.save(modelIdentityContext.prepare(team));
        }
        return null;
    }

    private Employee requireTarget(Employee operator, Long empId) {
        Employee target = employeeRepository.findByEmpId(empId);
        if (target == null) throw new DefinedException(new DefaultErrorCode("404", "员工不存在"));
        requireManageScope(operator, target);
        return target;
    }

    private void requireManageScope(Employee operator, Employee target) {
        if (Objects.equals(operator.getEmpId(), target.getEmpId())) {
            throw new DefinedException(new DefaultErrorCode("400", "不能操作自己的员工记录"));
        }
        if (!Objects.equals(operator.getCompany().getComId(), target.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("404", "员工不存在"));
        }
        if (operator.getRole() == EmployeeRole.Boss) return;
        if (operator.getRole() == EmployeeRole.StoreManager) {
            if (target.getRole() == EmployeeRole.Boss || target.getRole() == EmployeeRole.StoreManager
                    || operator.getStore() == null || target.getStore() == null
                    || !Objects.equals(operator.getStore().getStoreId(), target.getStore().getStoreId())) {
                throw new DefinedException(new DefaultErrorCode("403", "无权管理该员工"));
            }
            return;
        }
        if (operator.getRole() == EmployeeRole.TeamLeader) {
            if (target.getRole() == EmployeeRole.Boss || target.getRole() == EmployeeRole.StoreManager
                    || target.getRole() == EmployeeRole.TeamLeader || operator.getTeam() == null
                    || target.getTeam() == null
                    || !Objects.equals(operator.getTeam().getTeamId(), target.getTeam().getTeamId())) {
                throw new DefinedException(new DefaultErrorCode("403", "无权管理该员工"));
            }
            return;
        }
        throw new DefinedException(new DefaultErrorCode("403", "无权管理员工"));
    }

    private void requireNotLastBoss(Employee employee) {
        if (employee.getRole() != EmployeeRole.Boss) return;
        long bosses = employeeRepository.countByCompany_ComIdAndRoleAndStatus(
                employee.getCompany().getComId(), EmployeeRole.Boss, EmployeeStatus.Active);
        if (bosses <= 1) {
            throw new DefinedException(new DefaultErrorCode("400", "企业唯一负责人不能退出，请先转让负责人"));
        }
    }

    private CompanyJoinLimit joinLimit(Credentials user, String phone) {
        return joinLimitRepository.findByUser_CreId(user.getCreId())
                .orElseGet(() -> joinLimitRepository.findByPhone(phone).orElseGet(() -> {
                    CompanyJoinLimit limit = new CompanyJoinLimit();
                    limit.setUser(user);
                    limit.setPhone(phone);
                    return limit;
                }));
    }

    private void updateJoinLimit(Credentials user, String phone, boolean joining) {
        CompanyJoinLimit limit = joinLimit(user, phone);
        limit.setUser(user);
        limit.setPhone(phone);
        if (joining) {
            long joins = memberLogRepository.countRecentLogs(
                    user.getCreId(), MemberLogAction.Join,
                    LocalDateTime.now().minusDays(JOIN_WINDOW_DAYS).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            limit.setJoinCount180d((int) joins);
            limit.setLastJoinAt(LocalDateTime.now());
            limit.setLastInviteAcceptAt(LocalDateTime.now());
        } else {
            limit.setLastResignAt(LocalDateTime.now());
        }
        joinLimitRepository.save(modelIdentityContext.prepare(limit));
    }

    private void memberLog(Credentials user, com.th4.edge.tenant.user.model.Company company, Employee employee,
                           MemberLogAction action, String before, String after,
                           Employee operator, String reason) {
        CompanyMemberLog log = new CompanyMemberLog();
        log.setUser(user);
        log.setCompany(company);
        log.setEmployee(employee);
        log.setAction(action);
        log.setBeforeSnapshot(before);
        log.setAfterSnapshot(after);
        log.setOperator(operator);
        log.setReason(reason);
        memberLogRepository.save(modelIdentityContext.prepare(log));
    }

    private String snapshot(Employee employee) {
        return "company=" + (employee.getCompany() == null ? null : employee.getCompany().getComId())
                + ",store=" + (employee.getStore() == null ? null : employee.getStore().getStoreId())
                + ",team=" + (employee.getTeam() == null ? null : employee.getTeam().getTeamId())
                + ",role=" + employee.getRole()
                + ",status=" + employee.getStatus();
    }

    private boolean after(Invite invite, LocalDateTime threshold) {
        return invite.getCreateTime() != null && invite.getCreateTime().toLocalDateTime().isAfter(threshold);
    }

    private String inviteExpiredMessage(InviteStatus status) {
        return switch (status) {
            case Expired -> "邀请已过期";
            case Accepted -> "邀请已被使用";
            case Cancelled -> "邀请已取消";
            case Rejected -> "邀请已拒绝";
            default -> "邀请不可用";
        };
    }

    private void blocked(Credentials user, String phone, String reason) {
        userContext.log(user, SecurityAction.LimitTrigger, SecurityResult.Blocked, null, reason, null);
        throw new DefinedException(new DefaultErrorCode("429", reason));
    }
}
