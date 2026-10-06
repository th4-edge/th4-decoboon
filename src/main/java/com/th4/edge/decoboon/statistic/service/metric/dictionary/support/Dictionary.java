package com.th4.edge.decoboon.statistic.service.metric.dictionary.support;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.statistic.context.MetricDictionaryContext;
import com.th4.edge.decoboon.statistic.model.MetricDefinition;
import com.th4.edge.decoboon.statistic.model.MetricDictionaryQuery;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

/** 对外指标口径字典实现。 */
@Note("指标口径字典")
@Properties(allow = @AllowProperties(model = MetricDefinition.class,
        properties = {"key", "name", "userLabel", "category", "definition", "formula", "source", "timeScope",
                "dataScope", "includes", "excludes", "missingValue", "example", "updatedAt", "status"}))
@Service("statistic/metric/dictionary")
@Scope(ScopeTypes.PROTOTYPE)
public class Dictionary implements com.th4.edge.decoboon.statistic.service.metric.dictionary.Dictionary {

    @User
    private Employee employee;
    private final MetricDictionaryContext context;
    private final FinanceAccessContext accessContext;

    public Dictionary(MetricDictionaryContext context, FinanceAccessContext accessContext) {
        this.context = context;
        this.accessContext = accessContext;
    }

    @Override
    public List<MetricDefinition> call(MetricDictionaryQuery request) {
        accessContext.requireReader(employee);
        return context.filter(request);
    }
}
