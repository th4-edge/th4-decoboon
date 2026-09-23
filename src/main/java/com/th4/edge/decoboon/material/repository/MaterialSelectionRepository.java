package com.th4.edge.decoboon.material.repository;

import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 主材选材记录仓储。
 *
 * <p>响应白名单会序列化 customer/property/contract/project/designer 等关联，
 * 因此读取方法统一用 {@link EntityGraph} 预取，保证关闭 OSIV 时也能正确加载。</p>
 */
public interface MaterialSelectionRepository extends JpaRepository<MaterialSelection, Long>,
        JpaSpecificationExecutor<MaterialSelection> {

    @EntityGraph(attributePaths = {"company", "store", "team", "owner", "customer", "property",
            "contract", "project", "sourceContractLine", "designer", "confirmedBy", "attachments"})
    Optional<MaterialSelection> findBySelectionId(Long selectionId);

    @EntityGraph(attributePaths = {"company", "store", "team", "owner", "customer", "property",
            "contract", "project", "sourceContractLine", "designer", "confirmedBy", "attachments"})
    List<MaterialSelection> findByCompany_ComIdAndProperty_PropertyIdOrderByVersionDesc(Long comId, Long propertyId);

    @EntityGraph(attributePaths = {"company", "store", "team", "owner", "customer", "property",
            "contract", "project", "sourceContractLine", "designer", "confirmedBy", "attachments"})
    List<MaterialSelection> findByCompany_ComIdAndProject_ProjectIdOrderByVersionDesc(Long comId, Long projectId);

    @EntityGraph(attributePaths = {"company", "store", "team", "owner", "customer", "property",
            "contract", "project", "sourceContractLine", "designer", "confirmedBy", "attachments"})
    List<MaterialSelection> findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(Long comId,
                                                                                          Collection<Long> propertyIds);

    Optional<MaterialSelection> findFirstByCompany_ComIdAndProperty_PropertyIdOrderByVersionDesc(Long comId, Long propertyId);

    Optional<MaterialSelection> findFirstByCompany_ComIdAndProperty_PropertyIdAndContract_ContractIdOrderByVersionDesc(
            Long comId, Long propertyId, Long contractId);

    Optional<MaterialSelection> findFirstByCompany_ComIdAndProperty_PropertyIdAndContractIsNullOrderByVersionDesc(
            Long comId, Long propertyId);

    @EntityGraph(attributePaths = {"company", "store", "team", "owner", "customer", "property",
            "contract", "project", "sourceContractLine", "designer", "confirmedBy", "attachments"})
    Optional<MaterialSelection> findFirstByCompany_ComIdAndSourceContractLine_LineIdOrderByVersionDesc(
            Long comId, Long contractLineId);

    @EntityGraph(attributePaths = {"company", "store", "team", "owner", "customer", "property",
            "contract", "project", "sourceContractLine", "designer", "confirmedBy", "attachments"})
    List<MaterialSelection> findByCompany_ComIdAndProject_ProjectIdAndSourceContractLineIsNotNullOrderByVersionDesc(
            Long comId, Long projectId);

    boolean existsByCompany_ComIdAndProperty_PropertyIdAndStatus(Long comId, Long propertyId, SelectionStatus status);

    boolean existsByCompany_ComIdAndProject_ProjectIdAndStatus(Long comId, Long projectId, SelectionStatus status);
}
