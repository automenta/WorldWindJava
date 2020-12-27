/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.tiff;

import gov.nasa.worldwind.util.Logging;

import java.nio.*;
import java.nio.channels.SeekableByteChannel;

/**
 * @author Lado Garakanidze
 * @version $Id: TIFFIFDFactory.java 1171 2013-02-11 21:45:02Z dcollins $
 */
class TIFFIFDFactory {
    public static final int MASK_USHORT = 0xFFFF;
    public static final long MASK_UINT = 0xFFFFFFFFL;

    private TIFFIFDFactory() {

    }

    public static TiffIFDEntry create(SeekableByteChannel fc, ByteOrder tiffFileOrder) {
        if (null == fc)
            return null;

        long savedPosition = 0;

        ByteBuffer header = ByteBuffer.wrap(new byte[12]).order(tiffFileOrder);

        try {
            fc.read(header);
            header.flip();

            int tag = TIFFIFDFactory.getUnsignedShort(header);
            int type = TIFFIFDFactory.getUnsignedShort(header);
            long count = TIFFIFDFactory.getUnsignedInt(header);

            // To save time and space the Value Offset contains the Value instead of pointing to
            // the Value if and only if the Value fits into 4 bytes. If the Value is shorter than 4 bytes,
            // it is left-justified within the 4-byte Value Offset, i.e., stored in the lowernumbered bytes.
            // Whether the Value fits within 4 bytes is determined by the Type and Count of the field.

            if (type == Tiff.Type.SHORT && count == 1) {
                // these get packed left-justified in the bytes...
                int upper = TIFFIFDFactory.getUnsignedShort(header);
                int lower = TIFFIFDFactory.getUnsignedShort(header);
                long value = (TIFFIFDFactory.MASK_USHORT & upper) << 16 | (TIFFIFDFactory.MASK_USHORT & lower);

                return new TiffIFDEntry(tag, type, value);
            } else if (count == 1 && (type == Tiff.Type.LONG || type == Tiff.Type.FLOAT)) {
                long value = header.getInt();
                return new TiffIFDEntry(tag, type, value);
            } else {
                long offset = TIFFIFDFactory.getUnsignedInt(header);
                int size = TIFFIFDFactory.MASK_USHORT & (int) TIFFIFDFactory.calcSize(type, count);

                if (size > 0L) {
                    ByteBuffer data = ByteBuffer.allocateDirect(size).order(tiffFileOrder);
                    savedPosition = fc.position();
                    fc.position(offset);
                    fc.read(data);
                    data.flip();

                    fc.position(savedPosition);
                    savedPosition = 0;

                    return new TiffIFDEntry(tag, type, count, offset, data);
                } else
                    return new TiffIFDEntry(tag, type, count, offset);
            }
        }
        catch (Exception e) {
            Logging.logger().finest(e.getMessage());
        }
        finally {
            if (savedPosition != 0) {
                try {
                    fc.position(savedPosition);
                }
                catch (Exception e2) {
                    Logging.logger().finest(e2.getMessage());
                }
            }
        }

        return null;
    }

    private static long calcSize(int type, long count) {
        return switch (type) {
            case Tiff.Type.BYTE, Tiff.Type.SBYTE, Tiff.Type.ASCII -> count;
            case Tiff.Type.SHORT, Tiff.Type.SSHORT -> count * 2L;
            case Tiff.Type.LONG, Tiff.Type.SLONG, Tiff.Type.FLOAT -> count * 4L;
            case Tiff.Type.DOUBLE, Tiff.Type.RATIONAL, Tiff.Type.SRATIONAL -> count * 8L;
            default -> 0;
        };
    }

    private static int getUnsignedShort(ByteBuffer bb) {
        return TIFFIFDFactory.MASK_USHORT & bb.getShort();
    }

    private static long getUnsignedInt(ByteBuffer bb) {
        return TIFFIFDFactory.MASK_UINT & bb.getInt();
    }
}
