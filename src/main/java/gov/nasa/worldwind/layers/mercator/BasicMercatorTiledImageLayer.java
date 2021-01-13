/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.layers.mercator;

import com.jogamp.opengl.util.texture.TextureData;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.*;
import jcog.Util;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import static java.lang.Math.toRadians;

/**
 * BasicTiledImageLayer modified 2009-02-03 to add support for Mercator projections.
 *
 * @author tag
 * @version $Id: BasicMercatorTiledImageLayer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BasicMercatorTiledImageLayer extends MercatorTiledImageLayer {
    private final Object fileLock = new Object();


    public BasicMercatorTiledImageLayer(LevelSet levelSet) {
        super(levelSet);
    }

    public BasicMercatorTiledImageLayer(AVList params) {
        this(new LevelSet(params));
        this.set(AVKey.CONSTRUCTION_PARAMETERS, params);
    }

    private static TextureData readTexture(InputStream s, boolean useMipMaps) throws IOException {
//        try {
            return OGLUtil.newTextureData(Configuration.getMaxCompatibleGLProfile(), s, useMipMaps);
//        }
//        catch (Exception e) {
//            String msg = Logging.getMessage("layers.TextureLayer.ExceptionAttemptingToReadTextureFile", url.toString());
//            Logging.logger().log(Level.SEVERE, msg, e);
//            return null;
//        }
    }

    private static void addTileToCache(MercatorTextureTile tile) {
        TextureTile.cache.add(tile.key, tile);
    }

    protected static boolean isTileValid(BufferedImage image) {
        //override in subclass to check image tile
        //if false is returned, then tile is marked absent
        return true;
    }

    protected static BufferedImage modifyImage(BufferedImage image) {
        //override in subclass to modify image tile
        return image;
    }

    private static BufferedImage convertBufferToImage(ByteBuffer buffer) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(buffer.array()));
    }

    private static BufferedImage transform(BufferedImage image, MercatorSector sector) {
        int type = image.getType();
        if (type == 0)
            type = BufferedImage.TYPE_INT_RGB;
        final int w = image.getWidth();
        final int h = image.getHeight();
        BufferedImage trans = new BufferedImage(w, h, type);
        double miny = sector.getMinLatPercent();
        double maxy = sector.getMaxLatPercent();
        for (int y = 0; y < h; y++) {
            double sy = 1.0 - y / (double) (h - 1);
            Angle lat = Angle.fromRadians(sy * toRadians(sector.latDelta) + sector.latMin().radians());
            double dy = Util.unitize(1.0 - (MercatorSector.gudermannianInverse(lat) - miny) / (maxy - miny));
            int iy = (int) (dy * (h - 1));

            for (int x = 0; x < w; x++) {
                trans.setRGB(x, y, image.getRGB(x, iy));
            }
        }
        return trans;
    }

    protected void requestTexture(DrawContext dc, MercatorTextureTile tile) {
        Vec4 centroid = tile.getCentroidPoint(dc.getGlobe());
        if (this.referencePoint != null)
            tile.setPriorityDistance(centroid.distanceTo3(this.referencePoint));

        requestQ.add(new RequestTask(tile, this));
    }

    private boolean loadTexture(MercatorTextureTile tile, InputStream in) throws IOException {
        TextureData textureData;

        synchronized (this.fileLock) {
            textureData = BasicMercatorTiledImageLayer.readTexture(in, this.isUseMipMaps());
        }

        if (textureData == null)
            return false;

        tile.setTextureData(textureData);
        if (tile.getLevelNumber() != 0 || !this.isRetainLevelZeroTiles())
            BasicMercatorTiledImageLayer.addTileToCache(tile);

        return true;
    }

    private void saveBuffer(ByteBuffer buffer, File outFile)
        throws IOException {
        synchronized (this.fileLock) // synchronized with read of file in RequestTask.run()
        {
            WWIO.saveBuffer(buffer, outFile);
        }
    }

    private boolean transformAndSave(BufferedImage image, MercatorSector sector,
        File outFile) {
        try {
            image = BasicMercatorTiledImageLayer.transform(image, sector);
            String extension = outFile.getName().substring(
                outFile.getName().lastIndexOf('.') + 1);
            synchronized (this.fileLock) // synchronized with read of file in RequestTask.run()
            {
                return ImageIO.write(image, extension, outFile);
            }
        }
        catch (IOException e) {
            return false;
        }
    }

    private static class RequestTask implements Runnable, Comparable<RequestTask> {
        private final BasicMercatorTiledImageLayer layer;
        private final MercatorTextureTile tile;

        private RequestTask(MercatorTextureTile tile, BasicMercatorTiledImageLayer layer) {
            this.layer = layer;
            this.tile = tile;
        }

        @Deprecated public void run() {
            // TODO: check to ensure load is still needed

//            try {
            final String url;
            try {
                url = tile.getResourceURL().toString();
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            WWIO.get(url, (response)->{
                    if (layer.loadTexture(tile, response.byteStream())) {
                        layer.getLevels().has(tile);
                        layer.firePropertyChange(AVKey.LAYER, null, this);
                    }
                }, e->{
                    layer.getLevels().miss(tile);
//                    Logging.logger().info(Logging.getMessage("http fail", e));
                    return false;
                });
//            }
//            catch (MalformedURLException e) {
//                e.printStackTrace();
//                layer.getLevels().miss(tile);
//                Logging.logger().info("http fail", e);
//            }


        }

            /**
             * @param that the task to compare
             * @return -1 if <code>this</code> less than <code>that</code>, 1 if greater than, 0 if equal
             * @throws IllegalArgumentException if <code>that</code> is null
             */
        public int compareTo(RequestTask that) {
            if (this==that/* || tile.equals(that.tile)*/)
                return 0;
            int c = Double.compare(this.tile.getPriority(), that.tile.getPriority());
            if (c != 0)
                return c;
            return tile.compareTo(that.tile); //Integer.compare(System.identityHashCode(tile), System.identityHashCode(that.tile));
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            final RequestTask that = (RequestTask) o;

            // Don't include layer in comparison so that requests are shared among layers
            return tile.equals(that.tile);
        }

        public int hashCode() {
            return tile.hashCode();
        }

        public String toString() {
            return this.tile.toString();
        }
    }

    private static class DownloadPostProcessor implements RetrievalPostProcessor {
        // TODO: Rewrite this inner class, factoring out the generic parts.
        private final MercatorTextureTile tile;
        private final BasicMercatorTiledImageLayer layer;

        public DownloadPostProcessor(MercatorTextureTile tile, BasicMercatorTiledImageLayer layer) {
            this.tile = tile;
            this.layer = layer;
        }

        public ByteBuffer apply(Retriever retriever) {
            try {
                if (!retriever.getState().equals(Retriever.RETRIEVER_STATE_SUCCESSFUL))
                    throw new IOException("retriever fail: " + retriever.getState());

                if (retriever instanceof HTTPRetriever) {
                    HTTPRetriever htr = (HTTPRetriever) retriever;
                    final int code = htr.getResponseCode();
                    if (code != HttpURLConnection.HTTP_OK)
                        throw new IOException("http fail: " + htr.getUrl().toString());
                }

                return BasicMercatorTiledImageLayer.load(retriever.getBuffer(), retriever.getContentType(), tile, layer);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

    }

    @Nullable
    public static ByteBuffer load(ByteBuffer buffer, String contentType, MercatorTextureTile tile, BasicMercatorTiledImageLayer layer) {
        try {


            final File outFile = layer.getDataFileStore().newFile(tile.getPath());
//                if (outFile == null) return null;
            if (outFile.exists())
                return buffer;

            // TODO: Better, more generic and flexible handling of file-format type
            if (buffer != null) {
                if (contentType == null)
                    throw new IOException("unknown ContentType");

                if (contentType.contains("xml")
                    || contentType.contains("html")
                    || contentType.contains("text")) {
                    layer.getLevels().miss(tile);

                    StringBuilder sb = new StringBuilder(buffer.remaining());
                    while (buffer.hasRemaining()) {
                        sb.append((char) buffer.get());
                    }
                    // TODO: parse out the message if the content is xml or html.
                    throw new IOException(sb.toString());
                } else if (contentType.contains("dds")) {
                    layer.saveBuffer(buffer, outFile);
                } else if (contentType.contains("zip")) {
                    // Assume it's zipped DDS, which the retriever would have unzipped into the buffer.
                    layer.saveBuffer(buffer, outFile);
                }
//                    else if (outFile.getName().endsWith(".dds"))
                else if (contentType.contains("image")) {
                    BufferedImage image = BasicMercatorTiledImageLayer.convertBufferToImage(buffer);

                    image = BasicMercatorTiledImageLayer.modifyImage(image);
                    if (BasicMercatorTiledImageLayer.isTileValid(image)) {
                        if (!layer.transformAndSave(image, tile.mercatorSector, outFile))
                            image = null;
                    }

                }

                //layer.firePropertyChange(AVKey.LAYER, null, this);
                return buffer;
            }
        }
        catch (IOException e) {
            layer.getLevels().miss(tile);
            Logging.logger().log(Level.SEVERE,
                Logging.getMessage("layers.TextureLayer.ExceptionSavingRetrievedTextureFile", tile.getPath()), e);
        }
        return null;
    }

}
//        @Deprecated public void run0() {
//            // TODO: check to ensure load is still needed
//
//            final URL textureURL = this.layer.getDataFileStore()
//                .findFile(tile.getPath(), false);
//            if (textureURL != null && !this.layer.isTextureExpired(tile, textureURL)) {
//                try {
//                    if (this.layer.loadTexture(tile, textureURL.openStream())) {
//                        layer.getLevels().has(tile);
//                        this.layer.firePropertyChange(AVKey.LAYER, null, this);
//                        return;
//                    }
//                } catch (IOException e) {
//                    // Assume that something's wrong with the file and delete it.
//                    this.layer.getDataFileStore().removeFile(textureURL);
//                    layer.getLevels().miss(tile);
//                    String message = Logging.getMessage(
//                        "generic.DeletedCorruptDataFile", textureURL);
//                    Logging.logger().info(message);
//                }
//            }
//
//            this.layer.downloadTexture(this.tile);
//        }
