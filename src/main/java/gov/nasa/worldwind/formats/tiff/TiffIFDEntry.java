/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.tiff;

import gov.nasa.worldwind.util.Logging;

import java.nio.*;

/**
 * A bag for holding individual entries from a Tiff ImageFileDirectory.
 *
 * @author brownrigg
 * @version $Id: TiffIFDEntry.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class TiffIFDEntry implements Comparable<TiffIFDEntry> {
    // package visibility is intended...
    final int tag;
    final int type;
    final long count;
    final long valOffset;
    private ByteBuffer data = null;

    public TiffIFDEntry(int tag, int type, long count, long valOffset) throws IllegalArgumentException {
        this(tag, type, count, valOffset, null);
    }

    public TiffIFDEntry(int tag, int type, long value) throws IllegalArgumentException {
        this(tag, type, 1, value, null);
    }

    public TiffIFDEntry(int tag, int type, long count, long valOffset, ByteBuffer data)
        throws IllegalArgumentException {
        this.tag = tag;
        this.type = type;
        this.count = count;
        this.valOffset = valOffset;
        this.data = data;
    }

    public long asLong() throws IllegalStateException {
        if (this.type != Tiff.Type.SHORT && this.type != Tiff.Type.LONG)
            throw new IllegalStateException("Attempt to access Tiff IFD-entry as int: tag/type="
                + Long.toHexString(tag) + "/" + type);

        if (this.type == Tiff.Type.SHORT && this.count == 1)
            return 0xFFFFL & (valOffset >> 16);
        else
            return valOffset;
    }

    public Double getAsDouble() {
        Double value = null;

        switch (this.type) {
            case Tiff.Type.SHORT, Tiff.Type.SSHORT -> value = (double) this.asShort();
            case Tiff.Type.LONG, Tiff.Type.SLONG -> value = (double) this.asLong();
            case Tiff.Type.FLOAT -> {
                float[] values = this.getFloats();
                if (null != values)
                    value = (double) values[0];
            }
            case Tiff.Type.DOUBLE -> {
                double[] values = this.getDoubles();
                if (null != values)
                    value = values[0];
            }
        }

        return value;
    }

    public int asShort() throws IllegalStateException {
        if (this.type != Tiff.Type.SHORT) {
            String message = Logging.getMessage("GeotiffReader.InvalidType", "short", this.tag, this.type);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.count > 0) {
            if (this.count == 1)
                return 0xFFFF & (int) (valOffset >> 16L);
            else {
                int[] values = this.getShortsAsInts();
                if (null != values && values.length > 0)
                    return values[0];
            }
        }

        String message = Logging.getMessage("generic.indexOutOfRange", this.count);
        Logging.logger().severe(message);
        throw new IllegalArgumentException(message);
    }

    /*
     * Reads and returns an array of doubles from the file.
     *
     */

    public int asShort(int index) throws IllegalStateException {
        if (this.type != Tiff.Type.SHORT) {
            String message = Logging.getMessage("GeotiffReader.InvalidType", "short", this.tag, this.type);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.count > index) {
            int[] values = this.getShortsAsInts();
            if (null != values && values.length > index)
                return values[index];
        }

        String message = Logging.getMessage("generic.indexOutOfRange", this.count);
        Logging.logger().severe(message);
        throw new IllegalArgumentException(message);
    }

    public int[] getShortsAsInts() {
        if (this.type != Tiff.Type.SHORT) {
            String message = Logging.getMessage("GeotiffReader.InvalidType", "short", this.tag, this.type);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.count == 1)
            return new int[] {this.asShort()};

        if (this.count > 0 && null != this.data) {
            int[] array = new int[(int) this.count];
            this.data.rewind();
            ShortBuffer sb = this.data.asShortBuffer();
            int i = 0;
            while (sb.hasRemaining()) {
                array[i++] = 0xFFFF & sb.get();
            }
            return array;
        }

        String message = Logging.getMessage("generic.indexOutOfRange", this.count);
        Logging.logger().severe(message);
        throw new IllegalArgumentException(message);
    }

    public long[] getAsLongs() {
        if (this.type != Tiff.Type.SHORT && this.type != Tiff.Type.LONG) {
            String message = Logging.getMessage("GeotiffReader.InvalidType", "long", this.tag, this.type);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.count == 1) {
            return new long[] {this.asLong()};
        }
        else if (this.count > 1 && null != this.data) {
            long[] array = new long[(int) this.count];

            if (this.type == Tiff.Type.SHORT) {
                ShortBuffer sb = this.data.rewind().asShortBuffer();
                this.data.rewind();
                int i = 0;
                while (sb.hasRemaining()) {
                    array[i++] = 0xFFFFL & sb.get();
                }
            }
            else if (this.type == Tiff.Type.LONG) {
                IntBuffer sb = this.data.rewind().asIntBuffer();
                this.data.rewind();
                int i = 0;
                while (sb.hasRemaining()) {
                    array[i++] = 0xFFFFFFFFL & sb.get();
                }
            }
            return array;
        }

        return null;
    }

//  1 = BYTE 8-bit unsigned integer.
//  2 = ASCII 8-bit byte that contains a 7-bit ASCII code; the last byte must be NUL (binary zero).
//  3 = SHORT 16-bit (2-byte) unsigned integer.
//  4 = LONG 32-bit (4-byte) unsigned integer.

    public short[] getShorts() {
        if (this.type != Tiff.Type.SHORT) {
            String message = Logging.getMessage("GeotiffReader.InvalidType", "short", this.tag, this.type);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.count == 1)
            return new short[] {(short) this.asShort()};

        if (this.count > 0 && null != this.data) {
            ShortBuffer sb = this.data.rewind().asShortBuffer();
            this.data.rewind();
            short[] array = new short[(int) this.count];
            int i = 0;
            while (sb.hasRemaining()) {
                array[i++] = sb.get();
            }
            return array;
        }

        return null;
    }

    public double[] getDoubles() {
        if (this.type != Tiff.Type.DOUBLE) {
            String message = Logging.getMessage("GeotiffReader.InvalidType", "double", this.tag, this.type);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.count == 0 || null == this.data)
            return null;

        DoubleBuffer db = this.data.rewind().asDoubleBuffer();
        this.data.rewind();

        int size = Math.max(db.limit(), (int) this.count);
        double[] array = new double[size];
        int i = 0;
        while (db.hasRemaining()) {
            array[i++] = db.get();
        }
        return array;
    }

    public float[] getFloats() {
        if (this.type != Tiff.Type.FLOAT) {
            String message = Logging.getMessage("GeotiffReader.InvalidType", "float", this.tag, this.type);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.count == 0)
            return null;

        if (this.count == 1) {
            int num = (int) (0xFFFFFFFFL & this.valOffset);
            return new float[] {Float.intBitsToFloat(num)};
        }

        if (null == this.data)
            return null;

        FloatBuffer db = this.data.rewind().asFloatBuffer();
        this.data.rewind();

        int size = Math.max(db.limit(), (int) this.count);
        float[] array = new float[size];
        int i = 0;
        while (db.hasRemaining()) {
            array[i++] = db.get();
        }
        return array;
    }

    public String asString() {
        if (this.type != Tiff.Type.ASCII) {
            String message = Logging.getMessage("GeotiffReader.InvalidType", "ascii", this.tag, this.type);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.count != 1 || null == this.data)
            return null;

        CharBuffer cbuf = this.data.rewind().asCharBuffer();
        return cbuf.toString();
    }

    public long asOffset() {
        return valOffset;
    }

    public int compareTo(TiffIFDEntry o) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this == o)
            return EQUAL;

        if (o != null) {
            return Integer.compare(this.tag, o.tag);
        }

        return AFTER;
    }
}
