/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind;


import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.globes.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.video.awt.WorldWindowGLCanvas;
import jcog.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.cache.*;
import org.w3c.dom.*;

import javax.xml.xpath.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * This class manages the initial WorldWind configuration. It reads WorldWind configuration files and registers their
 * contents. Configurations files contain the names of classes to create at run-time, the initial model definition,
 * including the globe, elevation model and layers, and various control quantities such as cache sizes and data
 * retrieval timeouts.
 * <p>
 * The Configuration class is a singleton, but its instance is not exposed publicly. It is addressed only via static
 * methods of the class. It is constructed upon first use of any of its static methods.
 * <p>
 * When the Configuration class is first instantiated it reads the XML document <code>config/worldwind.xml</code> and
 * registers all the information there. The information can subsequently be retrieved via the class' various
 * <code>getValue</code> methods. Many WorldWind start-up objects query this information to determine the classes to
 * create. For example, the first WorldWind object created by an application is typically a {@link WorldWindowGLCanvas}.
 * During construction that class causes WorldWind's internal classes to be constructed, using the names of those
 * classes drawn from the Configuration singleton, this class.
 * <p>
 * The default WorldWind configuration document is <code>config/worldwind.xml</code>. This can be changed by setting the
 * Java property <code>gov.nasa.worldwind.config.file</code> to a different file name or a valid URL prior to creating
 * any WorldWind object or invoking any static methods of WorldWind classes, including the Configuration class. When an
 * application specifies a different configuration location it typically does so in its main method prior to using
 * WorldWind. If a file is specified its location must be on the classpath. (The contents of application and WorldWind
 * jar files are typically on the classpath, in which case the configuration file may be in the jar file.)
 * <p>
 * Additionally, an application may set another Java property, <code>gov.nasa.worldwind.app.config.document</code>, to a
 * file name or URL whose contents contain configuration values to override those of the primary configuration document.
 * WorldWind overrides only those values in this application document, it leaves all others to the value specified in
 * the primary document. Applications usually specify an override document in order to specify the initial layers in the
 * model.
 * <p>
 * See <code>config/worldwind.xml</code> for documentation on setting configuration values.
 * <p>
 * Configuration values can also be set programatically via {@link Configuration#setValue(String, Object)}, but they are
 * not retroactive so affect only Configuration queries made subsequent to setting the value.
 * <p>
 * <em>Note:</em> Prior to September of 2009, configuration properties were read from the file
 * <code>config/worldwind.properties</code>. An alternate file could be specified via the
 * <code>gov.nasa.worldwind.config.file</code> Java property. These mechanisms remain available but are deprecated.
 * WorldWind no longer contains a <code>worldwind.properties</code> file. If <code>worldwind.properties</code> or its
 * replacement as specified through the Java property exists at run-time and can be found via the classpath,
 * configuration values specified by that mechanism are given precedence over values specified by the new mechanism.
 *
 * @author Tom Gaskins
 * @version $Id: Configuration.java 1739 2013-12-04 03:38:19Z dcollins $
 */
public class Configuration // Singleton
{
    public static final String DEFAULT_LOGGER_NAME = "gov.nasa.worldwind";

    private static final String CONFIG_PROPERTIES_FILE_NAME = "config/worldwind.properties";
    private static final String CONFIG_FILE_PROPERTY_KEY = "gov.nasa.worldwind.config.file";

    private static final String CONFIG_WW_DOCUMENT_KEY = "gov.nasa.worldwind.config.document";
    private static final String CONFIG_WW_DOCUMENT_NAME =
        "config/worldwind.xml";

    private static final String CONFIG_APP_DOCUMENT_KEY = "gov.nasa.worldwind.app.config.document";

    static final Properties properties = Configuration.initializeDefaults();
    private static final ArrayList<Document> configDocs = new ArrayList<>();

    public static final FileStore data;

    public static final Globe globe;

    public static final HttpClient http;


    public static final String userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:68.0) Gecko/20100101 Firefox/68.0";

    static private final int CACHE_STALE_DAYS = 365;
//    static private final long DISK_CACHE_MB = 1024;

//    public static final HttpCacheContext httpCache;

    public static final File httpCacheDir;

    /**
     * Private constructor invoked only internally.
     */
    static {

        // Load the app's configuration if there is one
        try {
            String appConfigLocation = System.getProperty(Configuration.CONFIG_APP_DOCUMENT_KEY);
            if (appConfigLocation != null)
                Configuration.loadConfigDoc(System.getProperty(Configuration.CONFIG_APP_DOCUMENT_KEY)); // Load app's config first
        }
        catch (RuntimeException e) {
            Logging.logger(Configuration.DEFAULT_LOGGER_NAME).log(Level.WARNING, "Configuration.ConfigNotFound",
                System.getProperty(Configuration.CONFIG_APP_DOCUMENT_KEY));
            // Don't stop if the app config file can't be found or parsed
        }

        try {
            // Load the default configuration
            Configuration.loadConfigDoc(System.getProperty(Configuration.CONFIG_WW_DOCUMENT_KEY, Configuration.CONFIG_WW_DOCUMENT_NAME));

            // Load config properties, ensuring that the app's config takes precedence over wwj's
            for (int i = Configuration.configDocs.size() - 1; i >= 0; i--) {
                Configuration.loadConfigProperties(Configuration.configDocs.get(i));
            }
        }
        catch (RuntimeException e) {
            Logging.logger(Configuration.DEFAULT_LOGGER_NAME).log(Level.WARNING, "Configuration.ConfigNotFound",
                System.getProperty(Configuration.CONFIG_WW_DOCUMENT_KEY));
        }

        data = (FileStore) WorldWind.createConfigurationComponent(Keys.DATA_FILE_STORE_CLASS_NAME);

        httpCacheDir = data.newFile("");

        CacheConfig cacheConfig = CacheConfig.custom()
            .setMaxCacheEntries(32 * 1024)
            .setMaxObjectSize(128 * 1024 * 1024)
            .setHeuristicCachingEnabled(true)
            .setHeuristicDefaultLifetime(TimeUnit.DAYS.toSeconds(CACHE_STALE_DAYS))
            .setHeuristicCoefficient(1)
            .setAllow303Caching(true)
            .setAsynchronousWorkersCore(Runtime.getRuntime().availableProcessors())
            .setAsynchronousWorkersMax(Runtime.getRuntime().availableProcessors())
            .setSharedCache(true)
            .setWeakETagOnPutDeleteAllowed(true)
            .build();
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(60000)
            .setSocketTimeout(60000)
            .build();
        http = CachingHttpClients.custom()
            .setResourceFactory(new HeapResourceFactory())
//            .setResourceFactory(new ResourceFactory() {
//
//                private final File cacheDir = httpCacheDir;
//
//
//                private File generateUniqueCacheFile(final String requestId) {
////                    final StringBuilder buffer = new StringBuilder();
////                    this.idgen.generate(buffer);
////                    buffer.append('.');
////                    final int len = Math.min(requestId.length(), 100);
////                    for (int i = 0; i < len; i++) {
////                        final char ch = requestId.charAt(i);
////                        if (Character.isLetterOrDigit(ch) || ch == '.') {
////                            buffer.append(ch);
////                        } else {
////                            buffer.append('-');
////                        }
////                    }
//                    byte[] r = CompressionUtil.gzipCompress(requestId.getBytes());
//                    String R = Base122.encode(r);
//                    if (R.length() < requestId.length()) {
//                        Util.nop();
//                    } else
//                        R = requestId;
//                    //R = FilenameUtils.normalize(R);
//                    R = URLEncoder.encode(R);
//
////                    String[] rr = requestId.split("/");
//
//                    return new File(this.cacheDir, R);
//                }
//
//                @Override
//                public Resource generate(
//                    final String requestId,
//                    final InputStream inStream,
//                    final InputLimit limit) throws IOException {
//
//                    final File file = generateUniqueCacheFile(requestId);
//
//                    IOUtil.copyStream2File(inStream, file, inStream.available());
//                    //IOUtil.copyStream2File(inStream, file, Util.longToInt(limit.getValue()));
//
//                    return new FileResource(file);
//                }
//
//                @Override
//                public Resource copy(final String requestId, final Resource resource) throws IOException {
//
//                    final File f = generateUniqueCacheFile(requestId);
//
//                    try (InputStream i = resource.getInputStream()) {
//                        Files.copy(i, f.toPath());
//                    }
//
//                    return new FileResource(f);
//                }
//            })
            .setHttpCacheStorage(new HttpCacheStorage() {
                final DefaultHttpCacheEntrySerializer io
                    = new DefaultHttpCacheEntrySerializer();

                @Override
                public void putEntry(String key, HttpCacheEntry entry) throws IOException {
                    key = key(key);

                    final long size = entry.getResource().length();
                    byte[] y;
                    try (ByteArrayOutputStream o = new ByteArrayOutputStream(Util.longToInt(size + 4096))) {
                        io.writeTo(entry, o);
                        y = o.toByteArray();
                    }

                    User.the().put(key, y /* BAD HACK */);
                }

                @Deprecated private static String key(String key) {
                    if (key.startsWith("{")) {
                        //HACK remove the "Vary" stuff
                        key = key.substring(key.indexOf('}')+1);
                    }
                    return key;
                }

                @Override
                public HttpCacheEntry getEntry(String key) throws IOException {
                    byte[] b = User.the().get(key(key));

                    return b == null ?
                        null :
                        io.readFrom(new ByteArrayInputStream(b));
                }

                @Override
                public void removeEntry(String key) {
                    //TODO
                }

                @Override
                public void updateEntry(String key, HttpCacheUpdateCallback callback) throws IOException {
                    HttpCacheEntry e = getEntry(key);
                    HttpCacheEntry f = callback.update(e);
                    if (e==f)
                        return;
                    if (f!=null) {
                        if (e!=null && equals(e, f))
                            return;

                        putEntry(key, f);
                    } else
                        removeEntry(key);
                }

                private boolean equals(HttpCacheEntry x, HttpCacheEntry y) {
                    //return x.toString().equals(y.toString());
                    return Arrays.equals(x.getAllHeaders(), y.getAllHeaders());
                }
            })
            .setCacheConfig(cacheConfig)
            .setCacheDir(httpCacheDir)
            .setConnectionManagerShared(true)
            .setUserAgent(Configuration.userAgent)
            .setDefaultRequestConfig(requestConfig)
            //.disableCookieManagement()
//            .disableAuthCaching()
            .build();
//        httpCache = HttpCacheContext.create();

//        Configuration.http = new OkHttpClient.Builder()
//            .dispatcher(new Dispatcher(ForkJoinPool.commonPool()))
//            .cache(new Cache(
//                data.newFile(""),
//                DISK_CACHE_MB * 1024L * 1024L))
//            .connectTimeout(1, TimeUnit.MINUTES)
//            .readTimeout(1, TimeUnit.MINUTES)
//            .callTimeout(1, TimeUnit.MINUTES)
//            .build();
//        cacheControl = new CacheControl.Builder()
//            .maxStale(CACHE_STALE_DAYS, TimeUnit.DAYS)
//            .build();

        // To support old-style configuration, read an existing config properties file and give the properties
        // specified there precedence.
        Configuration.initializeCustom();

        globe = new Earth();
    }

    /**
     * Return as a string the value associated with a specified key.
     *
     * @param key          the key for the desired value.
     * @param defaultValue the value to return if the key does not exist.
     * @return the value associated with the key, or the specified default value if the key does not exist.
     */
    public static String getStringValue(String key, String defaultValue) {
        String v = Configuration.getStringValue(key);
        return v != null ? v : defaultValue;
    }

    /**
     * Return as a string the value associated with a specified key.
     *
     * @param key the key for the desired value.
     * @return the value associated with the key, or null if the key does not exist.
     */
    public static String getStringValue(String key) {
        Object o = Configuration.properties.getProperty(key);
        return o != null ? o.toString() : null;
    }

    /**
     * Return as an Integer the value associated with a specified key.
     *
     * @param key          the key for the desired value.
     * @param defaultValue the value to return if the key does not exist.
     * @return the value associated with the key, or the specified default value if the key does not exist or is not an
     * Integer or string representation of an Integer.
     */
    public static Integer getIntegerValue(String key, Integer defaultValue) {
        Integer v = Configuration.getIntegerValue(key);
        return v != null ? v : defaultValue;
    }

    /**
     * Return as an Integer the value associated with a specified key.
     *
     * @param key the key for the desired value.
     * @return the value associated with the key, or null if the key does not exist or is not an Integer or string
     * representation of an Integer.
     */
    public static Integer getIntegerValue(String key) {
        String v = Configuration.getStringValue(key);
        if (v == null)
            return null;

        try {
            return Integer.parseInt(v);
        }
        catch (NumberFormatException e) {
            Logging.logger().log(Level.SEVERE, "Configuration.ConversionError", v);
            return null;
        }
    }

    /**
     * Return as an Long the value associated with a specified key.
     *
     * @param key          the key for the desired value.
     * @param defaultValue the value to return if the key does not exist.
     * @return the value associated with the key, or the specified default value if the key does not exist or is not a
     * Long or string representation of a Long.
     */
    public static Long getLongValue(String key, Long defaultValue) {
        Long v = Configuration.getLongValue(key);
        return v != null ? v : defaultValue;
    }

    /**
     * Return as an Long the value associated with a specified key.
     *
     * @param key the key for the desired value.
     * @return the value associated with the key, or null if the key does not exist or is not a Long or string
     * representation of a Long.
     */
    public static Long getLongValue(String key) {
        String v = Configuration.getStringValue(key);
        if (v == null)
            return null;

        try {
            return Long.parseLong(v);
        }
        catch (NumberFormatException e) {
            Logging.logger().log(Level.SEVERE, "Configuration.ConversionError", v);
            return null;
        }
    }

    /**
     * Return as an Double the value associated with a specified key.
     *
     * @param key          the key for the desired value.
     * @param defaultValue the value to return if the key does not exist.
     * @return the value associated with the key, or the specified default value if the key does not exist or is not an
     * Double or string representation of an Double.
     */
    public static Double getDoubleValue(String key, Double defaultValue) {
        Double v = Configuration.getDoubleValue(key);
        return v != null ? v : defaultValue;
    }

    /**
     * Return as an Double the value associated with a specified key.
     *
     * @param key the key for the desired value.
     * @return the value associated with the key, or null if the key does not exist or is not an Double or string
     * representation of an Double.
     */
    public static Double getDoubleValue(String key) {
        String v = Configuration.getStringValue(key);
        if (v == null)
            return null;

        try {
            return Double.parseDouble(v);
        }
        catch (NumberFormatException e) {
            Logging.logger().log(Level.SEVERE, "Configuration.ConversionError", v);
            return null;
        }
    }

    /**
     * Return as a Boolean the value associated with a specified key.
     * <p>
     * Valid values for true are '1' or anything that starts with 't' or 'T'. ie. 'true', 'True', 't' Valid values for
     * false are '0' or anything that starts with 'f' or 'F'. ie. 'false', 'False', 'f'
     *
     * @param key          the key for the desired value.
     * @param defaultValue the value to return if the key does not exist.
     * @return the value associated with the key, or the specified default value if the key does not exist or is not a
     * Boolean or string representation of an Boolean.
     */
    public static Boolean getBooleanValue(String key, Boolean defaultValue) {
        Boolean v = Configuration.getBooleanValue(key);
        return v != null ? v : defaultValue;
    }

    /**
     * Return as a Boolean the value associated with a specified key.
     * <p>
     * Valid values for true are '1' or anything that starts with 't' or 'T'. ie. 'true', 'True', 't' Valid values for
     * false are '0' or anything that starts with 'f' or 'F'. ie. 'false', 'False', 'f'
     *
     * @param key the key for the desired value.
     * @return the value associated with the key, or null if the key does not exist or is not a Boolean or string
     * representation of an Boolean.
     */
    public static Boolean getBooleanValue(String key) {
        String v = Configuration.getStringValue(key);
        if (v == null)
            return null;

        final String V = v.trim();
        final boolean vEmpty = V.isEmpty();
        if (!vEmpty) {
            final char vFirstchar = V.toUpperCase().charAt(0);
            if (vFirstchar == 'T' || V.equals("1")) {
                return true;
            } else if (vFirstchar == 'F' || V.equals("0")) {
                return false;
            }
        }

        Logging.logger().log(Level.SEVERE, "Configuration.ConversionError", v);
        return null;
    }

    /**
     * Determines whether a key exists in the configuration.
     *
     * @param key the key of interest.
     * @return true if the key exists, otherwise false.
     */
    public static boolean hasKey(String key) {
        return Configuration.properties.contains(key);
    }

    /**
     * Removes a key and its value from the configuration if the configuration contains the key.
     *
     * @param key the key of interest.
     */
    public static void removeKey(String key) {
        Configuration.properties.remove(key);
    }

    /**
     * Adds a key and value to the configuration, or changes the value associated with the key if the key is already in
     * the configuration.
     *
     * @param key   the key to set.
     * @param value the value to associate with the key.
     */
    public static void setValue(String key, Object value) {
        Configuration.properties.put(key, value.toString());
    }

    /**
     * Returns the path to the application user's home directory.
     *
     * @return the absolute path to the application user's home directory.
     */
    public static String getUserHomeDirectory() {
        String dir = System.getProperty("user.home");
        return (dir != null) ? dir : ".";
    }

    /**
     * Returns the path to the operating system's temp directory.
     *
     * @return the absolute path to the operating system's temporary directory.
     */
    public static String getSystemTempDirectory() {
        String dir = System.getProperty("java.io.tmpdir");
        return (dir != null) ? dir : ".";
    }

    /**
     * Returns the path to the current user's application data directory. The path returned depends on the operating
     * system on which the Java Virtual Machine is running. The following table provides the path for all supported
     * operating systems:
     * <table><caption style="font-weight: bold;">Mapping</caption>
     * <tr><th>Operating System</th><th>Path</th></tr> <tr><td>Mac OS X</td><td>~/Library/Application
     * Support</td></tr> <tr><td>Windows</td><td>~\\Application Data</td></tr> <tr><td>Linux, Unix,
     * Solaris</td><td>~/</td></tr> </table>
     *
     * @return the absolute path to the current user's application data directory.
     */
    public static String getCurrentUserAppDataDirectory() {
        if (Configuration.isMacOS()) {
            // Return a path that Mac OS X has designated for app-specific data and support files. See the following URL
            // for details:
            // http://developer.apple.com/library/mac/#documentation/FileManagement/Conceptual/FileSystemProgrammingGUide/MacOSXDirectories/MacOSXDirectories.html#//apple_ref/doc/uid/TP40010672-CH10-SW1
            return Configuration.getUserHomeDirectory() + "/Library/Application Support";
        } else if (Configuration.isWindowsOS()) {
            return Configuration.getUserHomeDirectory() + "\\Application Data";
        } else if (Configuration.isLinuxOS() || Configuration.isUnixOS() || Configuration.isSolarisOS()) {
            return Configuration.getUserHomeDirectory();
        } else {
            String msg = Logging.getMessage("generic.UnknownOperatingSystem");
            Logging.logger().fine(msg);
            return null;
        }
    }

    /**
     * Determines whether the operating system is a Mac operating system.
     *
     * @return true if the operating system is a Mac operating system, otherwise false.
     */
    @Deprecated
    public static boolean isMacOS() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("mac");
    }

    // OS, user, and run-time specific system properties. //

    /**
     * Determines whether the operating system is Windows operating system.
     *
     * @return true if the operating system is a Windows operating system, otherwise false.
     */
    public static boolean isWindowsOS() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("windows");
    }

    /**
     * Determines whether the operating system is Windows XP operating system.
     *
     * @return true if the operating system is a Windows XP operating system, otherwise false.
     */
    public static boolean isWindowsXPOS() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("windows") && osName.contains("xp");
    }

    /**
     * Determines whether the operating system is Windows Vista operating system.
     *
     * @return true if the operating system is a Windows Vista operating system, otherwise false.
     */
    public static boolean isWindowsVistaOS() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("windows") && osName.contains("vista");
    }

    /**
     * Determines whether the operating system is Windows 7 operating system.
     *
     * @return true if the operating system is a Windows Vista operating system, otherwise false.
     */
    public static boolean isWindows7OS() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("windows") && osName.contains("7");
    }

    /**
     * Determines whether the operating system is Linux operating system.
     *
     * @return true if the operating system is a Linux operating system, otherwise false.
     */
    public static boolean isLinuxOS() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("linux");
    }

    /**
     * Determines whether the operating system is Unix operating system.
     *
     * @return true if the operating system is a Unix operating system, otherwise false.
     */
    public static boolean isUnixOS() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("unix");
    }

    /**
     * Determines whether the operating system is Solaris operating system.
     *
     * @return true if the operating system is a Solaris operating system, otherwise false.
     */
    public static boolean isSolarisOS() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("solaris");
    }

    /**
     * Returns the version of the Java virtual machine.
     *
     * @return the Java virtual machine version.
     */
    public static float getJavaVersion() {
        float ver = 0.0f;
        String s = System.getProperty("java.specification.version");
        if (null == s || s.isEmpty())
            s = System.getProperty("java.version");
        try {
            ver = Float.parseFloat(s.trim());
        }
        catch (NumberFormatException ignore) {
        }
        return ver;
    }

    /**
     * Returns a specified element of an XML configuration document.
     *
     * @param xpathExpression an XPath expression identifying the element of interest.
     * @return the element of interest if the XPath expression is valid and the element exists, otherwise null.
     * @throws NullPointerException if the XPath expression is null.
     */
    public static Element getElement(String xpathExpression) {
        XPath xpath = WWXML.makeXPath();

        for (Document doc : Configuration.configDocs) {
            try {
                Node node = (Node) xpath.evaluate(xpathExpression, doc.getDocumentElement(), XPathConstants.NODE);
                if (node != null)
                    return (Element) node;
            }
            catch (XPathExpressionException e) {
                return null;
            }
        }

        return null;
    }

    private static Properties initializeDefaults() {
        Properties defaults = new Properties();
        TimeZone tz = Calendar.getInstance().getTimeZone();
        if (tz != null)
            defaults.setProperty(Keys.INITIAL_LONGITUDE,
                Double.toString(
                    new Angle(180.0 * tz.getOffset(System.currentTimeMillis()) / (12.0 * 3.6e6)).degrees));
        return defaults;
    }

    private static void loadConfigDoc(String configLocation) {
        if (!WWUtil.isEmpty(configLocation)) {
            Document doc = WWXML.openDocument(configLocation);
            if (doc != null) {
                Configuration.configDocs.add(doc);
            }
        }
    }

    private static void insertConfigDoc(String configLocation) {
        if (!WWUtil.isEmpty(configLocation)) {
            Document doc = WWXML.openDocument(configLocation);
            if (doc != null) {
                configDocs.add(0, doc);
                loadConfigProperties(doc);
            }
        }
    }

    private static void loadConfigProperties(Document doc) {
        try {
            XPath xpath = WWXML.makeXPath();

            NodeList nodes = (NodeList) xpath.evaluate("/WorldWindConfiguration/Property", doc, XPathConstants.NODESET);
            if (nodes == null || nodes.getLength() == 0)
                return;

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String prop = xpath.evaluate("@name", node);
                String value = xpath.evaluate("@value", node);
                if (WWUtil.isEmpty(prop))// || WWUtil.isEmpty(value))
                    continue;

                Configuration.properties.setProperty(prop, value);
            }
        }
        catch (XPathExpressionException e) {
            Logging.logger(Configuration.DEFAULT_LOGGER_NAME).log(Level.WARNING, "XML.ParserConfigurationException");
        }
    }

    private static void initializeCustom() {
        // IMPORTANT NOTE: Always use the single argument version of Logging.logger in this method because the non-arg
        // method assumes an instance of Configuration already exists.

        String configFileName = System.getProperty(Configuration.CONFIG_FILE_PROPERTY_KEY, Configuration.CONFIG_PROPERTIES_FILE_NAME);
        try {
            InputStream propsStream = null;
            File file = new File(configFileName);
            if (file.exists()) {
                try {
                    propsStream = new FileInputStream(file);
                }
                catch (FileNotFoundException e) {
                    Logging.logger(Configuration.DEFAULT_LOGGER_NAME).log(Level.FINEST, "Configuration.LocalConfigFileNotFound",
                        configFileName);
                }
            }

            if (propsStream == null) {
                propsStream = Configuration.class.getResourceAsStream('/' + configFileName);
            }

            if (propsStream != null)
                Configuration.properties.load(propsStream);
        }
        // Use a named logger in all the catch statements below to prevent Logger from calling back into
        // Configuration when this Configuration instance is not yet fully instantiated.
        catch (IOException e) {
            Logging.logger(Configuration.DEFAULT_LOGGER_NAME).log(Level.SEVERE, "Configuration.ExceptionReadingPropsFile", e);
        }
    }

}