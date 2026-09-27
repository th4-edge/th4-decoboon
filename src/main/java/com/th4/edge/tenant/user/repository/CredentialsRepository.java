package com.th4.edge.tenant.user.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import com.th4.edge.tenant.user.configure.CredType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import com.th4.edge.tenant.user.model.Credentials;

import java.util.Collection;
import java.util.List;

/**
 * 登录凭证仓储。
 */
public interface CredentialsRepository extends JpaRepository<Credentials, Long> {

    Credentials findByTypeAndIdKey(CredType type, String idKey);

    Credentials findByTypeAndPhone(CredType type, String phone);

    Credentials findFirstByPhoneOrderByCreIdDesc(String phone);

    @EntityGraph(attributePaths = {"employee", "employee.store", "employee.team", "employee.company"})
    Credentials findFirstByEmployee_EmpIdOrderByCreIdDesc(Long empId);

    Credentials findFirstByEmployee_EmpIdAndTypeOrderByCreIdDesc(Long empId, CredType type);

    List<Credentials> findByEmployee_EmpId(Long empId);

    List<Credentials> findByEmployee_EmpIdIn(Collection<Long> empIds);

    @EntityGraph(attributePaths = {"employee", "employee.store", "employee.team", "employee.company"})
    java.util.Optional<Credentials> findByCreId(Long creId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<Credentials> findLockedByCreId(Long creId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<Credentials> findLockedByPhone(String phone);
}
