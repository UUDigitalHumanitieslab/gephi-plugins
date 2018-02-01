package org.gephi.scripting.wrappers;

import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.FilterModel;
import org.gephi.filters.api.Query;
import org.gephi.scripting.util.FieldnotesNamespace;
import org.openide.util.Lookup;
import org.python.core.PyObject;
import org.python.core.PySet;

public class FieldnotesFilter {

    /** The namespace in which this object is inserted */
    private final FieldnotesNamespace namespace;
    /** The underlying query object */
    private Query underlyingQuery;

    /**
     * Constructor for the filter wrapper.
     * @param namespace     the namespace in which this object is inserted
     * @param query         the query object that will be wrapped
     */
    public FieldnotesFilter(FieldnotesNamespace namespace, Query query) {
        this.namespace = namespace;
        this.underlyingQuery = query;
        FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
        // API documentation: https://gephi.org/docs/api/org/gephi/filters/api/FilterController.html
        FilterModel filterModel = filterController.getModel();
        Query currentQuery = filterModel.getCurrentQuery();
        String propertyName = currentQuery.getPropertyName(0);
        String propertyValue = currentQuery.getPropertyValue(0).toString();
    }










    /**
     * Retrieves the underlying query object.
     * @return              the underlying query object
     */
    public Query getUnderlyingQuery() {
        return underlyingQuery;
    }

    /**
     * Sets a new query to be wrapped by this wrapper object.
     * @param query         the new underlying query object
     */
    public void setUnderlyingQuery(Query query) {
        this.underlyingQuery = query;
    }

    @Override
    public PyObject __and__(PyObject obj) {
        if (obj instanceof FieldnotesFilter) {
            FilterController filterController = Lookup.getDefault().lookup(FilterController.class);

            IntersectionOperator intersectionOperator = new IntersectionOperator();
            Query andQuery = filterController.createQuery(intersectionOperator);
            FieldnotesFilter otherFilter = (FieldnotesFilter) obj;

            filterController.setSubQuery(andQuery, underlyingQuery);
            filterController.setSubQuery(andQuery, otherFilter.underlyingQuery);

            return new FieldnotesFilter(namespace, andQuery);
        }

        return null;
    }

    @Override
    public PyObject __or__(PyObject obj) {
        if (obj instanceof FieldnotesFilter) {
            FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
            UnionOperator unionOperator = new UnionOperator();
            Query orQuery = filterController.createQuery(unionOperator);
            FieldnotesFilter otherFilter = (FieldnotesFilter) obj;

            filterController.setSubQuery(orQuery, underlyingQuery);
            filterController.setSubQuery(orQuery, otherFilter.underlyingQuery);

            return new FieldnotesFilter(namespace, orQuery);
        }

        return null;
    }

    @Override
    public void __setattr__(String name, PyObject value) {
        FieldnotesGraph graph = (FieldnotesGraph) this.namespace.__finditem__(FieldnotesNamespace.GRAPH_NAME);
        FieldnotesSubGraph subGraph = graph.filter(this);

        PySet nodes = (PySet) subGraph.__findattr_ex__("nodes");
        PySet edges = (PySet) subGraph.__findattr_ex__("edges");

        nodes.__setattr__(name, value);
        edges.__setattr__(name, value);
    }
}
