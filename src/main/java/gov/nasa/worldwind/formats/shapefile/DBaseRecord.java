/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.shapefile;

import gov.nasa.worldwind.avlist.KVMap;
import gov.nasa.worldwind.util.Logging;

import java.nio.*;
import java.text.*;
import java.util.logging.Level;

/**
 * @author Patrick Murris
 * @version $Id: DBaseRecord.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class DBaseRecord extends KVMap {
    private static final DateFormat dateformat = new SimpleDateFormat("yyyyMMdd");
    private boolean deleted;
    private int recordNumber;

    public DBaseRecord(DBaseFile dbaseFile, ByteBuffer buffer, int recordNumber) {
        if (dbaseFile == null) {
            String message = Logging.getMessage("nullValue.DBaseFileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (buffer == null) {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.readFromBuffer(dbaseFile, buffer, recordNumber);
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public int getRecordNumber() {
        return this.recordNumber;
    }

    @SuppressWarnings("StringEquality")
    protected void readFromBuffer(DBaseFile dbaseFile, ByteBuffer buffer, int recordNumber) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Set parent DBaseFile and record number.
        this.recordNumber = recordNumber;

        // Read deleted record flag.
        byte b = buffer.get();
        this.deleted = (b == 0x2A);

        // Create a buffer to hold the field values.
        int maxFieldLength = 0;
        for (DBaseField field : dbaseFile.getFields()) {
            if (maxFieldLength < field.getLength())
                maxFieldLength = field.getLength();
        }

        // Read field values.
        DBaseField[] fields = dbaseFile.getFields();
        byte[] bytes = new byte[maxFieldLength];

        for (DBaseField field : fields) {
            int numRead = DBaseFile.readZeroTerminatedString(buffer, bytes, field.getLength());

            // Add a null entry for this field if the field's value is null or the empty string. This enables
            // applications to treat the DBaseRecord a standard AVList without any knowledge of the DBase file's field
            // keys. Specifically, DBaseRecord.hasKey() returns true for all fields.
            if (DBaseFile.isStringEmpty(bytes, numRead)) {
                this.set(field.getName(), "");
                continue;
            }

            String value = DBaseFile.decodeString(bytes, numRead).trim();

            try {
                final String fieldType = field.getType();
                if (fieldType == DBaseField.TYPE_BOOLEAN) {
                    this.set(field.getName(), value.equalsIgnoreCase("T") || value.equalsIgnoreCase("Y"));
                } else if (fieldType == DBaseField.TYPE_CHAR) {
                    this.set(field.getName(), value);
                } else if (fieldType == DBaseField.TYPE_DATE) {
                    this.set(field.getName(), DBaseRecord.dateformat.parse(value));
                } else if (fieldType == DBaseField.TYPE_NUMBER) {
                    // Parse the field value as a decimal number. Double.parseDouble ignores any leading or trailing
                    // whitespace.
                    if (field.getDecimals() > 0)
                        this.set(field.getName(), Double.valueOf(value));
                    else
                        this.set(field.getName(), Long.valueOf(value));
                }
            }
            catch (Exception e) {
                // Log warning but keep reading.
                Logging.logger().log(Level.WARNING, Logging.getMessage("SHP.FieldParsingError", field, value), e);
            }
        }
    }
}