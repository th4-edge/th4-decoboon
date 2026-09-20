package com.th4.edge.decoboon.catalog.service.categories.tree.support;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.model.CatalogTree;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 材料库分类、品牌、规格树实现。 */
@Note("家装材料库分类树")
@Properties(allow = {
        @AllowProperties(model = CatalogTree.class,
                properties = {"type", "categories", "brands", "specs", "total"})
})
@Service("catalog/item/tree")
@Scope(ScopeTypes.PROTOTYPE)
public class Tree implements com.th4.edge.decoboon.catalog.service.categories.tree.Tree {

    @AutoInject
    @NotNull
    private ItemType type;

    @User
    private Employee employee;

    private final CatalogItemRepository repository;
    private final CatalogContext context;

    public Tree(CatalogItemRepository repository, CatalogContext context) {
        this.repository = repository;
        this.context = context;
    }

    @Override
    public CatalogTree call(CatalogItem request) {
        Employee current = context.requireEmployee(employee);
        Long comId = current.getCompany().getComId();
        List<Object[]> facets = repository.findFacetsByCompanyAndType(comId, type);
        Set<String> categories = new LinkedHashSet<>();
        Map<String, List<String>> brands = new LinkedHashMap<>();
        Map<String, List<String>> specs = new LinkedHashMap<>();
        Map<String, Map<String, List<String>>> seriesByBrand = new LinkedHashMap<>();
        Map<String, Map<String, Map<String, List<String>>>> specsBySeries = new LinkedHashMap<>();
        for (Object[] facet : facets) {
            String category = text((String) facet[0]);
            String brand = text((String) facet[1]);
            String series = text((String) facet[2]);
            String spec = text((String) facet[3]);
            categories.add(category);
            brands.computeIfAbsent(category, ignored -> new ArrayList<>());
            specs.computeIfAbsent(category, ignored -> new ArrayList<>());
            add(brands.get(category), brand);
            add(specs.get(category), spec);
            Map<String, List<String>> brandSeries = seriesByBrand.computeIfAbsent(category, ignored -> new LinkedHashMap<>());
            add(brandSeries.computeIfAbsent(brand, ignored -> new ArrayList<>()), series);
            Map<String, Map<String, List<String>>> brandSpecs = specsBySeries
                    .computeIfAbsent(category, ignored -> new LinkedHashMap<>());
            Map<String, List<String>> seriesSpecs = brandSpecs
                    .computeIfAbsent(brand, ignored -> new LinkedHashMap<>());
            List<String> specValues = seriesSpecs
                    .computeIfAbsent(series, ignored -> new ArrayList<>());
            add(specValues, spec);
        }
        CatalogTree tree = new CatalogTree();
        tree.setType(type);
        tree.setCategories(new ArrayList<>(categories));
        tree.setBrands(brands);
        tree.setSpecs(specs);
        tree.setSeriesByBrand(seriesByBrand);
        tree.setSpecsBySeries(specsBySeries);
        tree.setTotal(repository.countByCompany_ComIdAndTypeAndActiveTrue(comId, type));
        return tree;
    }

    private void add(List<String> values, String value) {
        if (value != null && !value.isBlank() && !values.contains(value)) {
            values.add(value);
        }
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "其他" : value;
    }
}
