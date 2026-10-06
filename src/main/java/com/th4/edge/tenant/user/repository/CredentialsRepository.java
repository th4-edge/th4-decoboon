package com.th4.edge.tenant.user.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.model.Credentials;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 登录凭证仓储。
 *
 * <p>非加锁读取；按凭证 ID（{@code credential:{creId}}）与按手机号（{@code credential-phone:{phone}}）的
 * 并发串行由 {@code @DistributedLocked} 承担，替代原行级悲观锁。</p>
 */
public interface CredentialsRepository extends JpaRepository<Credentials, Long> {

    Credentials findByTypeAndIdKey(CredType type, String idKey);

    Credentials findByTypeAndPhone(CredType type, String phone);

    Credentials findFirstByPhoneOrderByCreIdDesc(String phone);

    /** 按手机号读取凭证（原 findByPhone 语义，含手机号唯一命中约束）。 */
    Optional<Credentials> findByPhone(String phone);

    @EntityGraph(attributePaths = {"employee", "employee.store", "employee.team", "employee.company"})
    Credentials findFirstByEmployee_EmpIdOrderByCreIdDesc(Long empId);

    Credentials findFirstByEmployee_EmpIdAndTypeOrderByCreIdDesc(Long empId, CredType type);

    List<Credentials> findByEmployee_EmpId(Long empId);

    List<Credentials> findByEmployee_EmpIdIn(Collection<Long> empIds);

    @EntityGraph(attributePaths = {"employee", "employee.store", "employee.team", "employee.company"})
    Optional<Credentials> findByCreId(Long creId);
}
