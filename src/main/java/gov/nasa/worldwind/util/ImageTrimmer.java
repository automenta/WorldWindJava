/*
 * Copyright (C) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.*;

public class ImageTrimmer {
    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            return;
        }

        for (String path : args) {
            try {
                System.out.print("Trimming " + path + " ... ");
                ImageTrimmer.trimImageInPlace(new File(path));
                System.out.print("success");
                System.out.println();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void trimImageInPlace(File file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file);
        BufferedImage trimmedImage = ImageUtil.trimImage(originalImage);
        ImageIO.write(trimmedImage, WWIO.getSuffix(file.getPath()), file);
    }
}
