package com.th4.edge.decoboon.trade.quote;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.context.QuoteCopyContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuoteCopyContextTest {

    private final QuoteRepository quoteRepository = mock(QuoteRepository.class);
    private final QuoteLineRepository lineRepository = mock(QuoteLineRepository.class);
    private final DataScopeContext dataScopeContext = mock(DataScopeContext.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final QuoteCopyContext context = new QuoteCopyContext(
            quoteRepository, lineRepository, dataScopeContext, projectRepository);

    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateNewDraftFromRejectedQuoteAndKeepOriginal() {
        Employee actor = new Employee();
        actor.setEmpId(7L);
        when(dataScopeContext.requireEmployee(actor)).thenReturn(actor);
        when(dataScopeContext.resolve(actor)).thenReturn(new DataScope(DataScopeType.Company, 1L, null, null, 7L));

        Quote source = rejectedQuote(actor);
        QuoteLine sourceLine = new QuoteLine();
        sourceLine.setItemId(8L);
        sourceLine.setName("套餐项目");
        sourceLine.setQuantity(BigDecimal.ONE);
        sourceLine.setPrice(new BigDecimal("1000"));
        sourceLine.setAmount(new BigDecimal("1000"));
        sourceLine.setCost(new BigDecimal("600"));
        when(quoteRepository.findOne(any(Specification.class))).thenReturn(Optional.of(source));
        when(projectRepository.existsByCompany_ComIdAndProperty_PropertyId(1L, 3L)).thenReturn(false);
        when(quoteRepository.existsByProperty_PropertyIdAndStatusIn(any(), any())).thenReturn(false);
        when(quoteRepository.countByCompany_ComIdAndProperty_PropertyIdAndPackageTemplate_TemplateId(1L, 3L, 4L))
                .thenReturn(2L);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> {
            Quote quote = invocation.getArgument(0);
            quote.setQuoteId(99L);
            return quote;
        });
        when(lineRepository.findByQuote(source)).thenReturn(List.of(sourceLine));
        when(lineRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Quote copy = context.rebase(1L, actor);

        assertThat(copy.getQuoteId()).isEqualTo(99L);
        assertThat(copy.getStatus()).isEqualTo(QuoteStatus.Draft);
        assertThat(copy.getVersion()).isEqualTo(3);
        assertThat(source.getStatus()).isEqualTo(QuoteStatus.Rejected);
        ArgumentCaptor<List<QuoteLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(lineRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0)).isNotSameAs(sourceLine);
        assertThat(captor.getValue().get(0).getAmount()).isEqualByComparingTo("1000");
    }

    private Quote rejectedQuote(Employee owner) {
        Company company = new Company();
        company.setComId(1L);
        Customer customer = new Customer();
        customer.setCustId(2L);
        Property property = new Property();
        property.setPropertyId(3L);
        PackageTemplate template = new PackageTemplate();
        template.setTemplateId(4L);
        Quote quote = new Quote();
        quote.setQuoteId(1L);
        quote.setNo("Q-OLD");
        quote.setStatus(QuoteStatus.Rejected);
        quote.setCompany(company);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setPackageTemplate(template);
        quote.setCreatedBy(owner);
        quote.setVersion(2);
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxRate(new BigDecimal("0.05"));
        return quote;
    }
}
