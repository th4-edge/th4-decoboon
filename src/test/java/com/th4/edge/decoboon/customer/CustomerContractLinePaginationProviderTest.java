package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.context.CustomerContractContext;
import com.th4.edge.decoboon.customer.service.contractlines.page.support.Page;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ContractWorkbench;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.tenant.user.model.Credentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** DQ-X9：业主合同行使用数据库分页，且业主 workbench 返回全量合同行摘要。 */
class CustomerContractLinePaginationProviderTest {

    private final CustomerAccessContext accessContext = mock(CustomerAccessContext.class);
    private final CustomerContractContext contractContext = mock(CustomerContractContext.class);
    private final ContractLineRepository lineRepository = mock(ContractLineRepository.class);
    private final Credentials credential = new Credentials();
    private final ProjectContract contract = new ProjectContract();
    private Page provider;

    @BeforeEach
    void setUp() {
        contract.setContractId(2001L);
        when(accessContext.requireCredential(credential)).thenReturn(credential);
        when(contractContext.requireContract(credential, null, contract.getContractId())).thenReturn(contract);
        when(lineRepository.findByContractOrderBySortAsc(eq(contract), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(1), 0));

        provider = new Page(accessContext, contractContext, lineRepository);
        ReflectionTestUtils.setField(provider, "principal", credential);
        ReflectionTestUtils.setField(provider, "contractId", contract.getContractId());
    }

    @Test
    void defaultPageMustUseTwentyRowsAndStableOrder() {
        provider.call(new ContractLine());

        Pageable pageable = capturePageable();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("sort")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("lineId")).isNotNull();
        verify(lineRepository, never()).findByContractOrderBySortAsc(any(ProjectContract.class));
    }

    @Test
    void requestedLargePageMustBeCappedAtFiftyRows() {
        ReflectionTestUtils.setField(provider, "current", 2);
        ReflectionTestUtils.setField(provider, "size", 500);

        provider.call(new ContractLine());

        Pageable pageable = capturePageable();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(50);
        assertThat(pageable.getOffset()).isEqualTo(50L);
        verify(lineRepository, never()).findByContractOrderBySortAsc(any(ProjectContract.class));
    }

    @Test
    void adjacentPagesMustUseNonOverlappingWindow() {
        ReflectionTestUtils.setField(provider, "current", 1);
        ReflectionTestUtils.setField(provider, "size", 20);
        provider.call(new ContractLine());
        ReflectionTestUtils.setField(provider, "current", 2);
        provider.call(new ContractLine());

        ArgumentCaptor<Pageable> requests = ArgumentCaptor.forClass(Pageable.class);
        verify(lineRepository, org.mockito.Mockito.times(2))
                .findByContractOrderBySortAsc(eq(contract), requests.capture());
        assertThat(requests.getAllValues()).extracting(Pageable::getOffset).containsExactly(0L, 20L);
    }

    @Test
    void ownerLineWhitelistMustExcludeCostAndWorkbenchMustExposeLineSummary() {
        assertThat(allowed(Page.class, ContractLine.class)).doesNotContain("cost");
        assertThat(allowed(
                com.th4.edge.decoboon.customer.service.contract.workbench.support.Workbench.class,
                ContractWorkbench.class))
                .contains("lineCount", "lineAmount");
    }

    private Pageable capturePageable() {
        ArgumentCaptor<Pageable> request = ArgumentCaptor.forClass(Pageable.class);
        verify(lineRepository).findByContractOrderBySortAsc(eq(contract), request.capture());
        return request.getValue();
    }

    private List<String> allowed(Class<?> providerClass, Class<?> model) {
        Properties properties = providerClass.getAnnotation(Properties.class);
        assertThat(properties).as(providerClass.getSimpleName()).isNotNull();
        return Arrays.stream(properties.allow())
                .filter(allow -> allow.model().equals(model))
                .flatMap(allow -> Arrays.stream(allow.properties()))
                .toList();
    }
}
