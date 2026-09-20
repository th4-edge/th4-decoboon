package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.configure.TagCategory;
import com.th4.edge.decoboon.customer.configure.TagScope;
import com.th4.edge.decoboon.customer.configure.TagStatus;
import com.th4.edge.decoboon.customer.model.CustomerTag;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 客户标签字典仓储。 */
public interface CustomerTagRepository extends JpaRepository<CustomerTag, Long> {

    List<CustomerTag> findByCompany_ComIdOrderBySortAscTagIdAsc(Long comId);

    List<CustomerTag> findByCompany_ComIdAndStatusOrderBySortAscTagIdAsc(Long comId, TagStatus status);

    List<CustomerTag> findByCompany_ComIdAndCategoryAndStatusOrderBySortAscTagIdAsc(Long comId, TagCategory category, TagStatus status);

    Optional<CustomerTag> findByCompany_ComIdAndName(Long comId, String name);

    Optional<CustomerTag> findByCompany_ComIdAndTagId(Long comId, Long tagId);

    List<CustomerTag> findByCompany_ComIdAndScope(Long comId, TagScope scope);
}
