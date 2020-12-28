package netvr;

import com.graphhopper.reader.ReaderElement;
import com.graphhopper.reader.osm.*;
import com.graphhopper.reader.osm.pbf.*;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.WWIO;
import jcog.Str;

import javax.xml.stream.*;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.*;

import static com.graphhopper.reader.osm.OSMXMLHelper.*;
import static java.lang.Long.parseLong;

public class OSMLoader {

    static final String[] osmHost = {
        "overpass-api.de",
//        "overpass.openstreetmap.ru",
//        "overpass.kumi.systems",
//        "overpass.nchc.org.tw"
    };

    public static void osm(Sector s, Consumer<ReaderElement> each) throws IOException {
        OSMLoader.osm("bbox=" + Str.n(s.lonMin, 3) + ',' + Str.n(s.latMin, 3) +
            ',' + Str.n(s.lonMax, 3) + ',' + Str.n(s.latMax, 3), each);
    }

    /**
     * https://overpass-api.de/api/map?bbox=left,bottom,right,top
     */
    private static void osm(String request, Consumer<ReaderElement> each) throws IOException {
        String host = OSMLoader.osmHost[0];

        final String uu = "https://" + host + "/api/map?" + request;


        WWIO.get(uu, (b)->OSMLoader.read(b.byteStream(), each));
//        try {
//            final Response r = WWIO.http.newCall(new Request.Builder()
//                .cacheControl(OSMLoader.cacheControl)
//                .url(uu).build()).execute();
//            final Response rn = r.networkResponse();
//            if (rn == null || rn.isSuccessful())
//                OSMLoader.read(r.body().byteStream(), each);
//            else
//                throw new IOException("unsuccessful: " + uu);
//        } catch (Exception e) {
//            throw new IOException("fail to load: " + uu);
//        }
    }

    private static void read(InputStream i, Consumer<ReaderElement> each) throws IOException {
        try (OSMInputFile2 o = new OSMInputFile2(i)) {

            ReaderElement e;
            while ((e = o.getNext()) != null) {
                each.accept(e);
            }
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static class OSMInputFile2 implements Sink, OSMInput {
        static final XMLInputFactory factory = XMLInputFactory.newInstance();
        static private final int BUFFER_SIZE = 1 * 1024 * 1024;
        private final InputStream bis;
        private final Queue<ReaderElement> itemQueue = new ArrayDeque<>();
        Thread pbfReaderThread;
        private boolean eof;
        private XMLStreamReader parser;
        private boolean binary;
        private OSMFileHeader fileheader;

        public OSMInputFile2(InputStream i) throws IOException, XMLStreamException, IllegalArgumentException {
            InputStream result;

            BufferedInputStream ips;

            ips = i instanceof BufferedInputStream ? ((BufferedInputStream) i) :
                new BufferedInputStream(i, OSMInputFile2.BUFFER_SIZE);

            ips.mark(10);
            byte[] header = new byte[6];
            if (ips.read(header) < 0) {
                throw new IllegalArgumentException();
            } else if (header[0] == 31 && header[1] == -117) {
                ips.reset();
                result = new GZIPInputStream(ips, OSMInputFile2.BUFFER_SIZE);
            } else if (header[0] != 0 || header[1] != 0 || header[2] != 0 || header[4] != 10 || header[5] != 9
                || header[3] != 13 && header[3] != 14) {
                if (header[0] == 80 && header[1] == 75) {
                    ips.reset();
                    ZipInputStream zip = new ZipInputStream(ips);
                    zip.getNextEntry();
                    result = zip;
//                } else if (!name.endsWith(".osm") && !name.endsWith(".xml")) {
//                    if (!name.endsWith(".bz2") && !name.endsWith(".bzip2")) {
//                        throw new IllegalArgumentException("Input file is not of valid type " + file.getPath());
//                    } else {
//                        String clName = "org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream";
//
//                        try {
//                            Class clazz = Class.forName(clName);
//                            ips.reset();
//                            Constructor<InputStream> ctor = clazz.getConstructor(InputStream.class, Boolean.TYPE);
//                            return (InputStream)ctor.newInstance(ips, true);
//                        } catch (Exception var8) {
//                            throw new IllegalArgumentException("Cannot instantiate " + clName, var8);
//                        }
//                    }
                } else {
                    ips.reset();
                    result = ips;
                }
            } else {
                ips.reset();
                this.binary = true;
                result = ips;
            }
            this.bis = result;
            if (this.binary) {
                this.openPBFReader(this.bis);
            } else {
                this.openXMLStream(this.bis);
            }
        }

        private void openXMLStream(InputStream in) throws XMLStreamException {
            this.parser = OSMInputFile2.factory.createXMLStreamReader(in, "UTF-8");

            int event = this.parser.next();
            if (event == 1 && this.parser.getLocalName().equalsIgnoreCase("osm")) {
                String timestamp = this.parser.getAttributeValue(null, "osmosis_replication_timestamp");
                if (timestamp == null) {
                    timestamp = this.parser.getAttributeValue(null, "timestamp");
                }

                if (timestamp != null) {
                    try {
                        this.fileheader = new OSMFileHeader();
                        this.fileheader.setTag("timestamp", timestamp);
                    }
                    catch (RuntimeException var6) {
                    }
                }

                this.eof = false;
            } else {
                throw new IllegalArgumentException("File is not a valid OSM stream");
            }
        }

        public ReaderElement getNext() throws XMLStreamException {
            if (this.eof) {
                throw new IllegalStateException("EOF reached");
            }

            ReaderElement item = this.binary ? this.getNextPBF() : this.getNextXML();
            if (item != null) {
                return item;
            } else {
                this.eof = true;
                return null;
            }
        }

        private ReaderElement getNextXML() throws XMLStreamException {
            int event = this.parser.next();
            if (this.fileheader != null) {
                ReaderElement copyfileheader = this.fileheader;
                this.fileheader = null;
                return copyfileheader;
            } else {
                for (; event != 8; event = this.parser.next()) {
                    if (event == 1) {
                        String idStr = this.parser.getAttributeValue(null, "id");
                        if (idStr != null) {
                            String name = this.parser.getLocalName();
                            switch (name.charAt(0)) {
                                case 'n':
                                    if ("node".equals(name))
                                        return createNode(parseLong(idStr), this.parser);
                                    break;
                                case 'r':
                                    return createRelation(parseLong(idStr), this.parser);
                                case 'w':
                                    return createWay(parseLong(idStr), this.parser);
                            }
                        }
                    }
                }

                this.parser.close();
                return null;
            }
        }

        public void close() throws IOException {
            try {
                if (!this.binary)
                    this.parser.close();
            }
            catch (XMLStreamException var5) {
                throw new IOException(var5);
            }
            finally {
                this.eof = true;
                this.bis.close();
                if (this.pbfReaderThread != null && this.pbfReaderThread.isAlive()) {
                    this.pbfReaderThread.interrupt();
                }
            }
        }

        private void openPBFReader(InputStream stream) {
            PbfReader reader = new PbfReader(stream, this, 1);
            reader.run();
        }

        public void process(ReaderElement item) {
            this.itemQueue.add(item);
        }

        @Override
        public void complete() {

        }

        public int getUnprocessedElements() {
            return this.itemQueue.size();
        }

        private ReaderElement getNextPBF() {
            return itemQueue.poll();
        }
    }
}
