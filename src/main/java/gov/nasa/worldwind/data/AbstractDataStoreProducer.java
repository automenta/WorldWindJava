/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.util.Logging;

import java.util.*;

/**
 * @author dcollins
 * @version $Id: AbstractDataStoreProducer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public abstract class AbstractDataStoreProducer extends WWObjectImpl implements DataStoreProducer {
    private final List<SourceInfo> dataSourceList =
        Collections.synchronizedList(new ArrayList<>());
    private final List<Object> productionResults = new ArrayList<>();
    protected KV productionParams;
    private KV params;
    private boolean isStopped;

    public AbstractDataStoreProducer() {
    }

    public KV getProductionParameters() {
        return this.productionParams;
    }

    public KV getStoreParameters() {
        return this.params;
    }

    public void setStoreParameters(KV parameters) {
        if (parameters == null) {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String message = this.validateProductionParameters(parameters);
        if (message != null) {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.params = parameters;
    }

    public Iterable<Object> getDataSources() {
        Collection<Object> list = new ArrayList<>(dataSourceList.size());

        for (SourceInfo info : this.dataSourceList) {
            list.add(info.source);
        }

        return list;
    }

    public boolean acceptsDataSource(Object source, KV params) {
        if (source == null || this.isStopped())
            return false;

        String message = this.validateDataSource(source, params);
        //noinspection RedundantIfStatement
        if (message != null) {
            // TODO garakl Do we want to log these files which we do not have readers for?
            return false;
        }

        return true;
    }

    public boolean containsDataSource(Object source) {
        for (SourceInfo info : this.dataSourceList) {
            if (Objects.equals(info.source, source))
                return true;
        }

        return false;
    }

    public void offerDataSource(Object source, KV params) {
        if (source == null) {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        params = (null == params) ? new KVMap() : params.copy();
        String message = this.validateDataSource(source, params);
        if (message != null) {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.dataSourceList.add(new SourceInfo(source, params));
    }

    public void offerAllDataSources(Iterable<?> sources) {
        if (sources == null) {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (Object source : sources) {
            this.offerDataSource(source, null);
        }
    }

    public void removeDataSource(Object source) {
        if (source == null) {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().warning(message);
            return; // Warn but don't throw an exception.
        }

        Iterator<SourceInfo> iter = this.dataSourceList.iterator();
        if (!iter.hasNext())
            return;

        for (SourceInfo info = iter.next(); iter.hasNext(); info = iter.next()) {
            if (info.source != null && info.source.equals(source))
                iter.remove();
        }
    }

    public void removeAllDataSources() {
        this.dataSourceList.clear();
    }

    public void startProduction() {
        if (this.isStopped()) {
            String message = Logging.getMessage("DataStoreProducer.Stopped");
            Logging.logger().warning(message);
            return;
        }

        String message = this.validateProductionParameters(this.params);
        if (message != null) {
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        this.doStartProduction(this.params);
    }

    public synchronized void stopProduction() {
        this.isStopped = true;
    }

    protected synchronized boolean isStopped() {
        return this.isStopped;
    }

    public Iterable<?> getProductionResults() {
        return Collections.unmodifiableList(this.productionResults);
    }

    public void removeProductionState() {
        // Left as an optional operation for subclasses to define.
    }

    protected List<SourceInfo> getDataSourceList() {
        return this.dataSourceList;
    }

    protected List<Object> getProductionResultsList() {
        return this.productionResults;
    }

    protected abstract void doStartProduction(KV parameters);

    protected abstract String validateProductionParameters(KV parameters);

    protected abstract String validateDataSource(Object source, KV params);

    public static class SourceInfo extends KVMap {
        public final Object source;

        public SourceInfo(Object source, KV params) {
            this.source = source;
            if (null != params)
                this.setValues(params);
        }
    }
}