package ec.com.antenasur.util;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.PredicateUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Admindba
 */
public class ReflectionColumnModelBuilder {

    private Class modelClass;
    private Comparator<PropertyDescriptor> propertySortComparator;
    private Predicate propertyFilterPredicate;
    private Set<String> excludedProperties;
    private static Set<String> defaultExcludedProperties = new HashSet<String>(0);

    static {
        defaultExcludedProperties.add("class");
    }

    public ReflectionColumnModelBuilder(Class modelClass) {
        this.modelClass = modelClass;
        this.propertyFilterPredicate = PredicateUtils.truePredicate();
        this.propertySortComparator = new Comparator<PropertyDescriptor>() {
            public int compare(PropertyDescriptor o1, PropertyDescriptor o2) {
                return o1.getName().compareTo(o2.getName());
            }
        };
        this.excludedProperties = new HashSet<String>(0);
    }

    public ReflectionColumnModelBuilder setPropertyFilterPredicate(Predicate p) {
        this.propertyFilterPredicate = p;
        return this;
    }

    public ReflectionColumnModelBuilder setPropertySortComparator(Comparator<PropertyDescriptor> c) {
        this.propertySortComparator = c;
        return this;
    }

    public ReflectionColumnModelBuilder setExcludedProperties(Set<String> p) {
        this.excludedProperties = p;
        return this;
    }

    public ReflectionColumnModelBuilder setExcludedProperties(String... p) {
        this.excludedProperties = new HashSet<String>(0);
        for (String excludedProperty : p) {
            this.excludedProperties.add(excludedProperty);
        }
        return this;
    }

    public List<ModeloColumna> build() {
        List<ModeloColumna> columns = new ArrayList<ModeloColumna>(0);
        List<PropertyDescriptor> propertyDescriptors = new ArrayList<PropertyDescriptor>(Arrays.asList(PropertyUtils.getPropertyDescriptors(modelClass)));
        CollectionUtils.filter(propertyDescriptors, PredicateUtils.andPredicate(propertyFilterPredicate, new Predicate() {
            public boolean evaluate(Object object) {
                PropertyDescriptor propertyDescriptor = (PropertyDescriptor) object;
                return propertyDescriptor.getReadMethod() != null
                        && !defaultExcludedProperties.contains(propertyDescriptor.getName())
                        && !excludedProperties.contains(propertyDescriptor.getName());
            }
        }));

        Collections.sort(propertyDescriptors, propertySortComparator);
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            ModeloColumna columnDescriptor = new ModeloColumna();
            columnDescriptor.setProperty(propertyDescriptor.getName());
            columnDescriptor.setHeader(StringUtils.capitalize(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(propertyDescriptor.getName()), " ")));
            columnDescriptor.setType(propertyDescriptor.getPropertyType());
            columns.add(columnDescriptor);
        }
        return columns;
    }
}
