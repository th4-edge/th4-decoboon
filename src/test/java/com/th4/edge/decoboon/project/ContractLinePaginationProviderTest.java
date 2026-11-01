package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.catalog.context.CatalogCostContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.service.contractlines.page.support.Page;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** DQ-X8：合同基线行必须由数据库真实分页，并有稳定排序和角色成本裁剪。 */
class ContractLinePaginationProviderTest {

    private final ContractLineRepository lineRepository = mock(ContractLineRepository.class);
    private final ProjectContractRepository contractRepository = mock(ProjectContractRepository.class);
    private final ProjectAccessContext accessContext = mock(ProjectAccessContext.class);
    private final CatalogCostContext costContext = mock(CatalogCostContext.class);

    private Employee employee;
    private ProjectContract contract;
    private Page provider;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setEmpId(1001L);
        employee.setRole(EmployeeRole.Designer);

        contract = new ProjectContract();
        contract.setContractId(2001L);

        Specification<ProjectContract> visible = (root, query, builder) -> builder.conjunction();
        when(accessContext.requireEmployee(employee)).thenReturn(employee);
        when(accessContext.visibleContract(employee)).thenReturn(visible);
        when(contractRepository.findOne(any(Specification.class))).thenReturn(Optional.of(contract));
        when(costContext.canViewCost(employee)).thenReturn(true);
        when(lineRepository.findByContractOrderBySortAsc(eq(contract), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(1), 0));

        provider = new Page(contractRepository, lineRepository, accessContext, costContext);
        ReflectionTestUtils.setField(provider, "employee", employee);
        ReflectionTestUtils.setField(provider, "contractId", contract.getContractId());
    }

    @Test
    void omittedPageParametersDefaultToFirstPageWithTwentyRows() {
        provider.call(new ContractLine());

        Pageable request = capturePageable();
        assertThat(request.getPageNumber()).isZero();
        assertThat(request.getPageSize()).isEqualTo(20);
        assertThat(request.getSort().getOrderFor("sort")).isNotNull();
        assertThat(request.getSort().getOrderFor("lineId")).isNotNull();
        verify(lineRepository, never()).findByContractOrderBySortAsc(any(ProjectContract.class));
    }

    @Test
    void requestedLargePageIsCappedAtFiftyRows() {
        ReflectionTestUtils.setField(provider, "current", 2);
        ReflectionTestUtils.setField(provider, "size", 500);

        provider.call(new ContractLine());

        Pageable request = capturePageable();
        assertThat(request.getPageNumber()).isEqualTo(1);
        assertThat(request.getPageSize()).isEqualTo(50);
        assertThat(request.getOffset()).isEqualTo(50);
    }

    @Test
    void nextPageUsesStableSortAndNonOverlappingWindow() {
        ReflectionTestUtils.setField(provider, "current", 1);
        ReflectionTestUtils.setField(provider, "size", 20);
        provider.call(new ContractLine());

        ReflectionTestUtils.setField(provider, "current", 2);
        provider.call(new ContractLine());

        ArgumentCaptor<Pageable> requests = ArgumentCaptor.forClass(Pageable.class);
        verify(lineRepository, org.mockito.Mockito.times(2))
                .findByContractOrderBySortAsc(eq(contract), requests.capture());
        assertThat(requests.getAllValues())
                .extracting(Pageable::getOffset)
                .containsExactly(0L, 20L);
        assertThat(requests.getAllValues()).allSatisfy(request -> {
            assertThat(request.getSort().getOrderFor("sort")).isNotNull();
            assertThat(request.getSort().getOrderFor("lineId")).isNotNull();
        });
        verify(lineRepository, never()).findByContractOrderBySortAsc(any(ProjectContract.class));
    }

    @Test
    void designerResponseMustNotExposeContractCost() {
        ContractLine line = new ContractLine();
        line.setLineId(3001L);
        line.setCost(new BigDecimal("88.00"));
        when(costContext.canViewCost(employee)).thenReturn(false);
        when(lineRepository.findByContractOrderBySortAsc(eq(contract), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(line), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        var result = provider.call(new ContractLine());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCost()).isNull();
        verify(lineRepository, never()).findByContractOrderBySortAsc(any(ProjectContract.class));
    }

    private Pageable capturePageable() {
        ArgumentCaptor<Pageable> request = ArgumentCaptor.forClass(Pageable.class);
        verify(lineRepository).findByContractOrderBySortAsc(eq(contract), request.capture());
        return request.getValue();
    }
}
