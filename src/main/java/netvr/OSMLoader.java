package netvr;

import com.graphhopper.reader.ReaderElement;
import com.graphhopper.reader.osm.*;
import com.graphhopper.reader.osm.pbf.*;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Sector;
import jcog.Str;
import okhttp3.*;

import javax.xml.stream.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.zip.*;

import static com.graphhopper.reader.osm.OSMXMLHelper.*;
import static java.lang.Long.parseLong;

public class OSMLoader {
    static final OkHttpClient http = new OkHttpClient.Builder()
        .cache(new Cache(
            WorldWind.store().newFile("OSM"),
            512 * 1024L * 1024L))
        .build();

    static final int CACHE_STALE_DAYS = 365;
    static final CacheControl cacheControl = new CacheControl.Builder()
        .maxStale(CACHE_STALE_DAYS, TimeUnit.DAYS)
        .build();



    public static void osm(Sector s, Consumer<ReaderElement> each) throws IOException {
        osm("bbox=" + Str.n(s.lonMin, 3) + "," + Str.n(s.latMin, 3) +
            "," + Str.n(s.lonMax, 3) + "," + Str.n(s.latMax, 3), each);
    }

     /** https://overpass-api.de/api/map?bbox=left,bottom,right,top */
     private static void osm(String request, Consumer<ReaderElement> each) throws IOException {
         final String uu = "https://overpass-api.de/api/map?" + request;

         System.out.println(uu);

         read(http.newCall(new Request.Builder()
             .cacheControl(cacheControl)
             .url(uu).build()).execute().body().byteStream(), each);

         //final URL u = new URL(uu);
////        OSMInputFile2 o = new OSMInputFile2(
////            new URL(u)
////        );
//        AbstractRetrievalPostProcessor pp = new AbstractRetrievalPostProcessor() {
//            @Override
//            protected boolean validateResponseCode() {
//                return true;
//            }
//
//            @Override
//            protected ByteBuffer handleContent() throws IOException {
//                saveBuffer();
//
//                return read(retriever.getBuffer(), each);
//            }
//
//            @Override
//            protected File doGetOutputFile() {
//                return WorldWind.store().newFile("OSM/" + request);
//            }
//        };
//        try {
//            ByteBuffer b = WWIO.readFileToBuffer(pp.getOutputFile());
//            if (b!=null) {
//                read(b, each);
//                return;
//            }
//        }
//        catch (IOException e) {
//        }
//        WorldWind.retrieveRemote().run(new URLRetriever(u, pp), 1);
    }

    private static void read(InputStream i, Consumer<ReaderElement> each) throws IOException {
        try (OSMInputFile2 o = new OSMInputFile2(i)) {

            ReaderElement e;
            while ((e = o.getNext()) != null) {
                each.accept(e);
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    public static class OSMInputFile2 implements Sink, OSMInput {
        private final InputStream bis;
        private final Queue<ReaderElement> itemQueue = new ArrayDeque<>();
        Thread pbfReaderThread;
        private boolean eof;
        private XMLStreamReader parser;
        private boolean binary = false;


        private OSMFileHeader fileheader;
        static final XMLInputFactory factory = XMLInputFactory.newInstance();
        static private final int BUFFER_SIZE = 1*1024*1024;

        public OSMInputFile2(InputStream i) throws IOException, XMLStreamException {
            InputStream result;

            BufferedInputStream ips;

            ips = i instanceof BufferedInputStream ? ((BufferedInputStream) i) :
                new BufferedInputStream(i, BUFFER_SIZE);

            ips.mark(10);
            byte[] header = new byte[6];
            if (ips.read(header) < 0) {
                throw new IllegalArgumentException();
            } else if (header[0] == 31 && header[1] == -117) {
                ips.reset();
                result = new GZIPInputStream(ips, BUFFER_SIZE);
            } else if (header[0] != 0 || header[1] != 0 || header[2] != 0 || header[4] != 10 || header[5] != 9 || header[3] != 13 && header[3] != 14) {
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

        //        public com.graphhopper.reader.osm.OSMInputFile setWorkerThreads(int num) {
//            this.workerThreads = num;
//            return this;
//        }

        private void openXMLStream(InputStream in) throws XMLStreamException {
            this.parser = factory.createXMLStreamReader(in, "UTF-8");

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
                    } catch (Exception var6) {
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
                for(; event != 8; event = this.parser.next()) {
                    if (event == 1) {
                        String idStr = this.parser.getAttributeValue(null, "id");
                        if (idStr != null) {
                            String name = this.parser.getLocalName();
                            switch(name.charAt(0)) {
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
                if (!this.binary) this.parser.close();

            } catch (XMLStreamException var5) {
                throw new IOException(var5);
            } finally {
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
//            this.pbfReaderThread = new Thread(reader, "PBF Reader");
//            this.pbfReaderThread.start();
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
//            ReaderElement next;
//            while((next = this.itemQueue.poll()) != null) {
//                process(next);
//            }
//            this.eof = true;
//            return next;
        }
    }

}
