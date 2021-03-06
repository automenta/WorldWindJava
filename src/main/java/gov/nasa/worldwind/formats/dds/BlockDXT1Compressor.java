/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.dds;

import gov.nasa.worldwind.util.Logging;

import static gov.nasa.worldwind.util.WWMath.sqr;

/**
 * Compressor for DXT1 color blocks. This class is not thread safe. Unsynchronized access will result in unpredictable
 * behavior. Access to methods of this class must be synchronized by the caller.
 * <p>
 * Documentation on the DXT1 format is available at http://msdn.microsoft.com/en-us/library/bb694531.aspx under the name
 * "BC1".
 *
 * @author dcollins
 * @version $Id: BlockDXT1Compressor.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BlockDXT1Compressor {
    // Implementation based on the paper "Real-Time DXT Compression" by J.M.P van Waveren
    // http://www.intel.com/cd/ids/developer/asmo-na/eng/324337.htm
    // and on the NVidia Texture Tools
    // http://code.google.com/p/nvidia-texture-tools/

    protected final Color32 minColor;
    protected final Color32 maxColor;
    protected final Color32[] palette;

    /**
     * Creates a new DXT1 block compressor.
     */
    public BlockDXT1Compressor() {
        this.minColor = new Color32();
        this.maxColor = new Color32();
        this.palette = new Color32[4];

        for (int i = 0; i < 4; i++) {
            this.palette[i] = new Color32();
        }
    }

    protected static void computeColorPalette3(int color0, int color1, Color32[] palette) {
        // Assign 16 bit 565 values to the color palette. We want to find the closest match to the hardware computed
        // colors, and the hardware will be computing the colors using 16 bit 565 values. The second color is 1/2 on
        // the line between max and min. The third color is considered to be transparent black. Computations of the
        // second colors is based on the current hardware algorithms, and the Direct3D SDK documentation at
        // http://msdn.microsoft.com/en-us/library/bb694531(VS.85).aspx

        BlockDXT1Compressor.short565ToColor32(color0, palette[0]);
        BlockDXT1Compressor.short565ToColor32(color1, palette[1]);

        palette[2].a = 255;
        palette[2].r = (palette[0].r + palette[1].r) / 2;
        palette[2].g = (palette[0].g + palette[1].g) / 2;
        palette[2].b = (palette[0].b + palette[1].b) / 2;

        // Set all components to 0 to match DXT specs.
        palette[3].a = 0;
        palette[3].r = 0;
        palette[3].g = 0;
        palette[3].b = 0;
    }

    protected static void computeColorPalette4(int color0, int color1, Color32[] palette) {
        // Assign 16 bit 565 values to the color palette. We want to find the closest match to the hardware computed
        // colors, and the hardware will be computing the colors using 16 bit 565 values. The second color is 1/3 on
        // the line between max and min. The third color is 2/3 on the line between max and min. Computations of the
        // second and third colors are based on the current hardware algorithms, and the Direct3D SDK documentation at
        // http://msdn.microsoft.com/en-us/library/bb694531(VS.85).aspx

        BlockDXT1Compressor.short565ToColor32(color0, palette[0]);
        BlockDXT1Compressor.short565ToColor32(color1, palette[1]);

        palette[2].a = 255;
        palette[2].r = (2 * palette[0].r + palette[1].r) / 3;
        palette[2].g = (2 * palette[0].g + palette[1].g) / 3;
        palette[2].b = (2 * palette[0].b + palette[1].b) / 3;

        palette[2].a = 255;
        palette[3].r = (palette[0].r + 2 * palette[1].r) / 3;
        palette[3].g = (palette[0].g + 2 * palette[1].g) / 3;
        palette[3].b = (palette[0].b + 2 * palette[1].b) / 3;
    }

    protected static long computePaletteIndices3(ColorBlock4x4 block, DXTCompressionAttributes attributes,
        Color32[] palette) {
        // This implementation is based on code available in the nvidia-texture-tools project:
        // http://code.google.com/p/nvidia-texture-tools/
        //
        // If the pixel alpha is below the specified threshold, we return index 3. In a three color DXT1 palette,
        // index 3 is interpreted as transparent black. Otherwise, we compare the sums of absolute differences, and
        // choose the nearest color index.

        int alphaThreshold = attributes.getDXT1AlphaThreshold();

        long mask = 0L;
        long index;

        for (int i = 0; i < 16; i++) {
            int d0 = BlockDXT1Compressor.colorDistanceSquared(palette[0], block.color[i]);
            int d1 = BlockDXT1Compressor.colorDistanceSquared(palette[1], block.color[i]);
            int d2 = BlockDXT1Compressor.colorDistanceSquared(palette[2], block.color[i]);

            // TODO: implement bit twiddle as in computePaletteIndex4 to avoid conditional branching

            if (block.color[i].a < alphaThreshold) {
                index = 3;
            } else if (d0 < d1 && d0 < d2) {
                index = 0;
            } else if (d1 < d2) {
                index = 1;
            } else {
                index = 2;
            }

            mask |= (index << (i << 1));
        }

        return mask;
    }

    //**************************************************************//
    //********************  Color Block Palette Assembly  **********//
    //**************************************************************//

    protected static long computePaletteIndices4(ColorBlock4x4 block, Color32[] palette) {
        // This implementation is based on the paper by J.M.P. van Waveren:
        // http://cache-www.intel.com/cd/00/00/32/43/324337_324337.pdf
        //
        // We compare the sums of absolute differences, and choose the nearest color index. We avoid conditional
        // branching to determine the nearest index, which would suffer from a lot of branch mispredition. Instead,
        // we compute each distance and derive a 2-bit binary index directly from the results of the distance
        // comparisons.

        long mask = 0L;
        long index;

        final Color32[] blockColor = block.color;

        for (int i = 0; i < 16; i++) {
            int d0 = BlockDXT1Compressor.colorDistanceSquared(palette[0], blockColor[i]);
            int d1 = BlockDXT1Compressor.colorDistanceSquared(palette[1], blockColor[i]);
            int d2 = BlockDXT1Compressor.colorDistanceSquared(palette[2], blockColor[i]);
            int d3 = BlockDXT1Compressor.colorDistanceSquared(palette[3], blockColor[i]);

            int b0 = BlockDXT1Compressor.greaterThan(d0, d3);
            int b1 = BlockDXT1Compressor.greaterThan(d1, d2);
            int b2 = BlockDXT1Compressor.greaterThan(d0, d2);
            int b3 = BlockDXT1Compressor.greaterThan(d1, d3);
            int b4 = BlockDXT1Compressor.greaterThan(d2, d3);

            int x0 = b1 & b2;
            int x1 = b0 & b3;
            int x2 = b0 & b4;

            index = (x2 | ((x0 | x1) << 1));

            mask |= (index << (i << 1));
        }

        return mask;
    }

    protected static void findMinMaxColorsBox(ColorBlock4x4 block, Color32 minColor, Color32 maxColor) {
        minColor.r = minColor.g = minColor.b = 255;
        maxColor.r = maxColor.g = maxColor.b = 0;

        final Color32[] blockColor = block.color;
        for (int i = 0; i < 16; i++) {
            BlockDXT1Compressor.minColorComponents(minColor, blockColor[i], minColor);
            BlockDXT1Compressor.maxColorComponents(maxColor, blockColor[i], maxColor);
        }
    }

    protected static void selectDiagonal(ColorBlock4x4 block, Color32 minColor, Color32 maxColor) {
        int centerR = (minColor.r + maxColor.r) / 2;
        int centerG = (minColor.g + maxColor.g) / 2;
        int centerB = (minColor.b + maxColor.b) / 2;

        int cvx = 0;
        int cvy = 0;
        for (int i = 0; i < 16; i++) {
            int tx = block.color[i].r - centerR;
            int ty = block.color[i].g - centerG;
            int tz = block.color[i].b - centerB;

            cvx += tx * tz;
            cvy += ty * tz;
        }

        int x0 = minColor.r;
        int y0 = minColor.g;
        int x1 = maxColor.r;
        int y1 = maxColor.g;

        if (cvx < 0) {
            int tmp = x0;
            x0 = x1;
            x1 = tmp;
        }

        if (cvy < 0) {
            int tmp = y0;
            y0 = y1;
            y1 = tmp;
        }

        minColor.r = x0;
        minColor.g = y0;
        maxColor.r = x1;
        maxColor.g = y1;
    }

    protected static void insetBox(Color32 minColor, Color32 maxColor) {
        int insetR = (maxColor.r - minColor.r) >> 4;
        int insetG = (maxColor.g - minColor.g) >> 4;
        int insetB = (maxColor.b - minColor.b) >> 4;

        minColor.r = Math.min(minColor.r + insetR, 255);
        minColor.g = Math.min(minColor.g + insetG, 255);
        minColor.b = Math.min(minColor.b + insetB, 255);

        maxColor.r = (maxColor.r > insetR) ? (maxColor.r - insetR) : 0;
        maxColor.g = (maxColor.g > insetG) ? (maxColor.g - insetG) : 0;
        maxColor.b = (maxColor.b > insetB) ? (maxColor.b - insetB) : 0;
    }

    //**************************************************************//
    //********************  Color Block Box Fitting  ***************//
    //**************************************************************//

    protected static void findMinMaxColorsEuclideanDistance(ColorBlock4x4 block, Color32 minColor, Color32 maxColor) {
        double maxDistance = 0;
        int minIndex = 0;
        int maxIndex = 0;

        final Color32[] color = block.color;
        for (int i = 0; i < 15; i++) {
            final Color32 bci = color[i];
            for (int j = i + 1; j < 16; j++) {
                double d = BlockDXT1Compressor.colorDistanceSquared(bci, color[j]);
                if (d > maxDistance) {
                    minIndex = i;
                    maxIndex = j;
                    maxDistance = d;
                }
            }
        }

        BlockDXT1Compressor.copyColorComponents(color[minIndex], minColor);
        BlockDXT1Compressor.copyColorComponents(color[maxIndex], maxColor);
    }

    protected static void findMinMaxColorsLuminanceDistance(ColorBlock4x4 block, Color32 minColor, Color32 maxColor) {
        int minLuminance = Integer.MAX_VALUE;
        int maxLuminance = -1;
        int minIndex = 0;
        int maxIndex = 0;

        for (int i = 0; i < 16; i++) {
            int luminance = BlockDXT1Compressor.colorLuminance(block.color[i]);
            if (luminance < minLuminance) {
                minIndex = i;
                minLuminance = luminance;
            }
            if (luminance > maxLuminance) {
                maxIndex = i;
                maxLuminance = luminance;
            }
        }

        BlockDXT1Compressor.copyColorComponents(block.color[minIndex], minColor);
        BlockDXT1Compressor.copyColorComponents(block.color[maxIndex], maxColor);
    }

    protected static int short565FromColor32(Color32 color) {
        // Quantize a 32 bit RGB color to a 16 bit 565 RGB color. Taken from an algorithm shared on the Molly Rocket
        // forum by member "ryg":
        // https://mollyrocket.com/forums/viewtopic.php?t=392

        return (BlockDXT1Compressor.mul8bit(color.r, 31) << 11) + (BlockDXT1Compressor.mul8bit(color.g, 63) << 5) + (BlockDXT1Compressor.mul8bit(color.b, 31));
    }

    //**************************************************************//
    //********************  Color Block Euclidean Distance  ********//
    //**************************************************************//

    protected static void short565ToColor32(int color16, Color32 color) {
        // Dequantize a 16 bit 565 RGB color to a 32 bit RGB color. Taken from an algorithm shared on the Molly Rocket
        // forum by member "ryg":
        // https://mollyrocket.com/forums/viewtopic.php?t=392

        int r = (color16 & 0xf800) >> 11;
        int g = (color16 & 0x07e0) >> 5;
        int b = (color16 & 0x001f);

        color.a = 255;
        color.r = (r << 3) | (r >> 2);
        color.g = (g << 2) | (g >> 4);
        color.b = (b << 3) | (b >> 2);
    }

    //**************************************************************//
    //********************  Color Block Luminance Distance  ********//
    //**************************************************************//

    private static int mul8bit(int a, int b) {
        int t = a * b + 128;
        return (t + (t >> 8)) >> 8;
    }

    //**************************************************************//
    //********************  Color Arithmetic  **********************//
    //**************************************************************//

    protected static int colorLuminance(Color32 c) {
        return c.r + c.g + 2 * c.b;
    }

    protected static int colorDistanceSquared(Color32 c1, Color32 c2) {
        return (int) (sqr(c1.r - c2.r)
            + sqr(c1.g - c2.g)
            + sqr(c1.b - c2.b));
    }

    protected static void maxColorComponents(Color32 c1, Color32 c2, Color32 max) {
        max.a = Math.max(c1.a, c2.a);
        max.r = Math.max(c1.r, c2.r);
        max.g = Math.max(c1.g, c2.g);
        max.b = Math.max(c1.b, c2.b);
    }

    protected static void minColorComponents(Color32 c1, Color32 c2, Color32 min) {
        min.a = Math.min(c1.a, c2.a);
        min.r = Math.min(c1.r, c2.r);
        min.g = Math.min(c1.g, c2.g);
        min.b = Math.min(c1.b, c2.b);
    }

    protected static void copyColorComponents(Color32 src, Color32 dest) {
        dest.a = src.a;
        dest.r = src.r;
        dest.g = src.g;
        dest.b = src.b;
    }

    protected static int greaterThan(int a, int b) {
        // Exploit the properties of Java's two's complement integer to quickly return a binary value representing
        // whether or not a is greater than b. If a is greater than b, than b-a will be a negative value, and the
        // 32nd bit will be a one. Otherwise, b-a will be a positive value or zero, and the 32nd bit will be a zero.
        // Therefore we need only return the 32nd bit of the value b-a.
        //
        // Note: a two's complement integer has only a single representation for zero, therefore we need not consider
        // the case of negative zero.
        //
        // Note: We use Java's "unsigned" right shift operator here, which places zeroes in the rightmost incoming bits.
        // This makes it unnecessary to mask off bits 2-32, since they will all be zero.

        return (b - a) >>> 31;
    }

    protected static void chooseMinMaxColors(ColorBlock4x4 block, DXTCompressionAttributes attributes,
        Color32 minColor, Color32 maxColor) {
        //noinspection StringEquality
        if (attributes.getColorBlockCompressionType() == DXTCompressionAttributes.COLOR_BLOCK_COMPRESSION_BBOX) {
            BlockDXT1Compressor.findMinMaxColorsBox(block, minColor, maxColor);
            BlockDXT1Compressor.selectDiagonal(block, minColor, maxColor);
            BlockDXT1Compressor.insetBox(minColor, maxColor);
        } else //noinspection StringEquality
            if (attributes.getColorBlockCompressionType()
                == DXTCompressionAttributes.COLOR_BLOCK_COMPRESSION_EUCLIDEAN_DISTANCE) {
                BlockDXT1Compressor.findMinMaxColorsEuclideanDistance(block, minColor, maxColor);
            } else //noinspection StringEquality
                if (attributes.getColorBlockCompressionType()
                    == DXTCompressionAttributes.COLOR_BLOCK_COMPRESSION_LUMINANCE_DISTANCE) {
                    // Default to using euclidean distance to compute the min and max palette colors.
                    BlockDXT1Compressor.findMinMaxColorsLuminanceDistance(block, minColor, maxColor);
                }
    }

    /**
     * Compress the 4x4 color block into a DXT1 block using four colors. This method ignores transparency and guarantees
     * that the DXT1 block will use four colors.
     * <p>
     * Access to this method must be synchronized by the caller. This method is frequently invoked by the DXT
     * compressor, so in order to reduce garbage each instance of this class has unsynchronized properties that are
     * reused during each call.
     *
     * @param colorBlock the 4x4 color block to compress.
     * @param attributes attributes that will control the compression.
     * @param dxtBlock   the DXT1 block that will receive the compressed data.
     * @throws IllegalArgumentException if either <code>colorBlock</code> or <code>dxtBlock</code> are null.
     */
    public void compressBlockDXT1(ColorBlock4x4 colorBlock, DXTCompressionAttributes attributes, BlockDXT1 dxtBlock) {

        BlockDXT1Compressor.chooseMinMaxColors(colorBlock, attributes, this.minColor, this.maxColor);
        int color0 = BlockDXT1Compressor.short565FromColor32(this.maxColor);
        int color1 = BlockDXT1Compressor.short565FromColor32(this.minColor);

        if (color0 < color1) {
            int tmp = color0;
            color0 = color1;
            color1 = tmp;
        }

        // To get a four color palette with no alpha, the first color must be greater than the second color.
        BlockDXT1Compressor.computeColorPalette4(color0, color1, this.palette);

        dxtBlock.color0 = color0;
        dxtBlock.color1 = color1;
        dxtBlock.colorIndexMask = BlockDXT1Compressor.computePaletteIndices4(colorBlock, this.palette);
    }

    /**
     * Compress the 4x4 color block into a DXT1 block with three colors. This method will consider a color transparent
     * if its alpha value is less than <code>alphaThreshold</code>.
     * <p>
     * This method is frequently invoked by the DXT compressor. In order to reduce garbage each instance of this class
     * has buffer that is used during each call. Access to this method must be synchronized by the caller.
     *
     * @param colorBlock the 4x4 color block to compress.
     * @param attributes attributes that will control the compression.
     * @param dxtBlock   the DXT1 block that will receive the compressed data.
     * @throws IllegalArgumentException if either <code>colorBlock</code> or <code>dxtBlock</code> are null.
     */
    public void compressBlockDXT1a(ColorBlock4x4 colorBlock, DXTCompressionAttributes attributes, BlockDXT1 dxtBlock) {
        if (colorBlock == null) {
            String message = Logging.getMessage("nullValue.ColorBlockIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (attributes == null) {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dxtBlock == null) {
            String message = Logging.getMessage("nullValue.DXTBlockIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        BlockDXT1Compressor.chooseMinMaxColors(colorBlock, attributes, this.minColor, this.maxColor);
        int color0 = BlockDXT1Compressor.short565FromColor32(this.maxColor);
        int color1 = BlockDXT1Compressor.short565FromColor32(this.minColor);

        if (color0 < color1) {
            int tmp = color0;
            color0 = color1;
            color1 = tmp;
        }

        // To get a three color palette with alpha, the first color must be less than the second color.
        BlockDXT1Compressor.computeColorPalette3(color1, color0, this.palette);

        dxtBlock.color0 = color1;
        dxtBlock.color1 = color0;
        dxtBlock.colorIndexMask = BlockDXT1Compressor.computePaletteIndices3(colorBlock, attributes, this.palette);
    }
}
