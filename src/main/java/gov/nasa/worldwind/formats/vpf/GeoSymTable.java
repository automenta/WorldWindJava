/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.avlist.*;

import java.util.*;

/**
 * @author dcollins
 * @version $Id: GeoSymTable.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GeoSymTable {
    private final GeoSymTableHeader header;
    private final Map<Integer, Integer> indexOnId;
    private AVList[] records;

    public GeoSymTable(GeoSymTableHeader header) {
        this.header = header;
        this.indexOnId = new HashMap<>();
    }

    public static void selectMatchingRows(String columnName, Object value, boolean acceptNullValue,
        Iterable<AVList> outRows) {
        Iterator<AVList> iter = outRows.iterator();
        if (!iter.hasNext())
            return;

        AVList record;
        while (iter.hasNext()) {
            record = iter.next();
            if (record == null)
                continue;

            Object o = record.get(columnName);
            if ((o == null && !acceptNullValue) || (o != null && !o.equals(value))) {
                iter.remove();
            }
        }
    }

    public static void selectMatchingStringRows(String columnName, String value, boolean acceptNullValue,
        Iterable<AVList> outRows) {
        Iterator<AVList> iter = outRows.iterator();
        if (!iter.hasNext())
            return;

        AVList record;
        while (iter.hasNext()) {
            record = iter.next();
            if (record == null)
                continue;

            Object o = record.get(columnName);
            if (o == null || o instanceof String) {
                String s = (String) o;
                if (s == null || s.isEmpty()) {
                    if (!acceptNullValue)
                        iter.remove();
                } else {
                    if (!s.equalsIgnoreCase(value))
                        iter.remove();
                }
            }
        }
    }

    public GeoSymTableHeader getHeader() {
        return header;
    }

    public AVList[] getRecords() {
        return this.records;
    }

    public void setRecords(AVList[] records) {
        this.records = records;
        this.buildRecordIndices();
    }

    public AVList getRecord(int id) {
        Integer index = this.indexOnId.get(id);
        return (index != null && index >= 0 && index < this.records.length) ? this.records[index] : null;
    }

    protected void buildRecordIndices() {
        // Build index on record ids.
        this.indexOnId.clear();
        for (int i = 0; i < this.records.length; i++) {
            Integer id = AVListImpl.getIntegerValue(this.records[i], "id");
            if (id != null) {
                this.indexOnId.put(id, i);
            }
        }
    }
}
