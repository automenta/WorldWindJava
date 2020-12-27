/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.tiff;

/**
 * @author Lado Garakanidze
 * @version $Id: Tiff.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface Tiff {
    int Undefined = 0;

    interface Type {
        int BYTE = 1;
        int ASCII = 2;
        int SHORT = 3;
        int LONG = 4;
        int RATIONAL = 5;
        int SBYTE = 6;
        int UNDEFINED = 7;
        int SSHORT = 8;
        int SLONG = 9;
        int SRATIONAL = 10;
        int FLOAT = 11;
        int DOUBLE = 12;
    }

    interface Tag {
        // Baseline Tiff 6.0 tags...
        int IMAGE_WIDTH = 256;
        int IMAGE_LENGTH = 257;
        int BITS_PER_SAMPLE = 258;
        int COMPRESSION = 259;
        int PHOTO_INTERPRETATION = 262;

        int DOCUMENT_NAME = 269;
        int IMAGE_DESCRIPTION = 270;
        int DEVICE_MAKE = 271; // manufacturer of the scanner or video digitizer
        int DEVICE_MODEL = 272; // model name/number of the scanner or video digitizer
        int STRIP_OFFSETS = 273;
        int ORIENTATION = 274;

        int SAMPLES_PER_PIXEL = 277;
        int ROWS_PER_STRIP = 278;
        int STRIP_BYTE_COUNTS = 279;
        int MIN_SAMPLE_VALUE = 280;
        int MAX_SAMPLE_VALUE = 281;
        int X_RESOLUTION = 282;
        int Y_RESOLUTION = 283;
        int PLANAR_CONFIGURATION = 284;
        int RESOLUTION_UNIT = 296;

        int SOFTWARE_VERSION = 305; // Name and release # of the software that created the image
        int DATE_TIME = 306; // uses format "YYYY:MM:DD HH:MM:SS"
        int ARTIST = 315;
        int COPYRIGHT = 315; // same as ARTIST

        int TIFF_PREDICTOR = 317;
        int COLORMAP = 320;
        int TILE_WIDTH = 322;
        int TILE_LENGTH = 323;
        int TILE_OFFSETS = 324;
        int TILE_COUNTS = 325;

        // Tiff extensions...
        int SAMPLE_FORMAT = 339;  // SHORT array of samplesPerPixel size
    }

    // The orientation of the image with respect to the rows and columns.
    interface Orientation {
        // 1 = The 0th row represents the visual top of the image,
        // and the 0th column represents the visual left-hand side.
        int Row0_IS_TOP__Col0_IS_LHS = 1;

        //2 = The 0th Row represents the visual top of the image,
        // and the 0th column represents the visual right-hand side.
        int Row0_IS_TOP__Col0_IS_RHS = 2;

        //3 = The 0th row represents the visual bottom of the image,
        // and the 0th column represents the visual right-hand side.
        int Row0_IS_BOTTOM__Col0_IS_RHS = 3;

        //4 = The 0th row represents the visual bottom of the image,
        // and the 0th column represents the visual left-hand side.
        int Row0_IS_BOTTOM__Col0_IS_LHS = 4;

        //5 = The 0th row represents the visual left-hand side of the image,
        // and the 0th column represents the visual top.
        int Row0_IS_LHS__Col0_IS_TOP = 5;

        //6 = The 0th row represents the visual right-hand side of the image,
        // and the 0th column represents the visual top.
        int Row0_IS_RHS__Col0_IS_TOP = 6;

        //7 = The 0th row represents the visual right-hand side of the image,
        // and the 0th column represents the visual bottom.
        int Row0_IS_RHS__Col0_IS_BOTTOM = 7;

        int DEFAULT = Orientation.Row0_IS_TOP__Col0_IS_LHS;
    }

    interface BitsPerSample {
        int MONOCHROME_BYTE = 8;
        int MONOCHROME_UINT8 = 8;
        int MONOCHROME_UINT16 = 16;
        int ELEVATIONS_INT16 = 16;
        int ELEVATIONS_FLOAT32 = 32;
        int RGB = 24;
        int YCbCr = 24;
        int CMYK = 32;
    }

    interface SamplesPerPixel {
        int MONOCHROME = 1;
        int RGB = 3;
        int RGBA = 4;
        int YCbCr = 3;
        int CMYK = 4;
    }

    // The color space of the image data
    interface Photometric {
        int Undefined = -1;

        // 0 = WhiteIsZero
        // For bilevel and grayscale images: 0 is imaged as white.
        // 2**BitsPerSample-1 is imaged as black.
        // This is the normal value for Compression=2
        int Grayscale_WhiteIsZero = 0;

        // 1 = BlackIsZero
        // For bilevel and grayscale images: 0 is imaged as black.
        // 2**BitsPerSample-1 is imaged as white.
        // If this value is specified for Compression=2, the image should display and print reversed.
        int Grayscale_BlackIsZero = 1;

        // 2 = RGB
        // The RGB value of (0,0,0) represents black, (255,255,255) represents white,
        // assuming 8-bit components.
        // Note! For PlanarConfiguration=1, the components are stored in the indicated order:
        // first Red, then Green, then Blue.
        // For PlanarConfiguration = 2, the StripOffsets for the component planes are stored
        // in the indicated order: first the Red component plane StripOffsets,
        // then the Green plane StripOffsets, then the Blue plane StripOffsets.
        int Color_RGB = 2;

        // 3 = Palette color
        // In this model, a color is described with a single component.
        // The value of the component is used as an index into the red, green and blue curves in
        // the ColorMap field to retrieve an RGB triplet that defines the color.
        //
        // Note!!
        // When PhotometricInterpretation=3 is used, ColorMap must be present and SamplesPerPixel must be 1.
        int Color_Palette = 3;

        // 4 = Transparency Mask.
        // This means that the image is used to define an irregularly shaped region of another
        // image in the same TIFF file.
        //
        // SamplesPerPixel and BitsPerSample must be 1.
        //
        // PackBits compression is recommended.
        // The 1-bits define the interior of the region; the 0-bits define the exterior of the region.
        //
        // A reader application can use the mask to determine which parts of the image to
        // display. Main image pixels that correspond to 1-bits in the transparency mask are
        // imaged to the screen or printer, but main image pixels that correspond to 0-bits in
        // the mask are not displayed or printed.
        // The image mask is typically at a higher resolution than the main image, if the
        // main image is grayscale or color so that the edges can be sharp.
        int Transparency_Mask = 4;

        int CMYK = 5;

        int YCbCr = 6;

        // There is no default for PhotometricInterpretation, and it is required.
    }

    interface Compression {
        int NONE = 1;
        int LZW = 5;
        int JPEG = 6;
        int PACKBITS = 32773;
    }

    interface PlanarConfiguration {
        // CHUNKY
        // The component values for each pixel are stored contiguously.
        // The order of the components within the pixel is specified by PhotometricInterpretation.
        // For example, for RGB data, the data is stored as RGBRGBRGB...
        int CHUNKY = 1;

        // PLANAR
        // The components are stored in separate component planes.
        // The values in StripOffsets and StripByteCounts are then arranged as
        // a 2-dimensional array, with SamplesPerPixel rows and StripsPerImage columns.
        // (All of the columns for row 0 are stored first, followed by the columns of row 1, and so on.)
        //
        // PhotometricInterpretation describes the type of data stored in each component plane.
        // For example, RGB data is stored with the Red components in one component plane,
        // the Green in another, and the Blue in another.
        //
        // Note!
        // If SamplesPerPixel is 1, PlanarConfiguration is irrelevant, and need not be included.
        int PLANAR = 2;

        int DEFAULT = PlanarConfiguration.CHUNKY;
    }

    interface ResolutionUnit {
        int NONE = 1;
        int INCH = 2;
        int CENTIMETER = 3;
    }

    interface SampleFormat {
        int UNSIGNED = 1;
        int SIGNED = 2;
        int IEEEFLOAT = 3;
        int UNDEFINED = 4;
    }
}
