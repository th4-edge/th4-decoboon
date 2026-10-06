package com.th4.edge.decoboon.property.repository;

import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 房屋仓储。 */
public interface PropertyRepository extends JpaRepository<Property, Long>,
        JpaSpecificationExecutor<Property> {

    Optional<Property> findByPropertyIdAndCompany_ComId(Long propertyId, Long comId);

    boolean existsByCompany_ComIdAndCustomer_CustIdAndAddress(Long comId, Long custId, String address);

    boolean existsByCompany_ComIdAndCustomer_CustId(Long comId, Long custId);

    Optional<Property> findFirstByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(Long comId, Long custId);

    List<Property> findByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(Long comId, Long custId);

    List<Property> findByCompany_ComIdAndCustomer_CustIdIn(Long comId, List<Long> customerIds);

    List<Property> findByCompany_ComIdAndStore_StoreIdOrderByPropertyIdDesc(Long comId, Long storeId);

    List<Property> findByCompany_ComIdAndTeam_TeamIdOrderByPropertyIdDesc(Long comId, Long teamId);

    List<Property> findByCompany_ComIdAndOwner_EmpIdOrderByPropertyIdDesc(Long comId, Long empId);
    List<Property> findByCompany_ComIdAndDesigner_EmpIdOrderByPropertyIdDesc(Long comId, Long empId);
    List<Property> findByCompany_ComIdAndSalesman_EmpIdOrderByPropertyIdDesc(Long comId, Long empId);
}
