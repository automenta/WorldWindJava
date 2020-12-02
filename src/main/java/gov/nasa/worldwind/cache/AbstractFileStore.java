/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.cache;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * @author tag
 * @version $Id: AbstractFileStore.java 2190 2014-08-01 21:54:20Z pabercrombie $
 */
public abstract class AbstractFileStore extends WWObjectImpl implements FileStore {
    public static final String UNIX_CACHE_PATH = "/.cache/";
    // Retrieval could be occurring on several threads when the app adds a read location, so protect the list of read
    // locations from concurrent modification.
    protected final List<StoreLocation> readLocations =
        new CopyOnWriteArrayList<>();
    private final Object fileLock = new Object();
    protected StoreLocation writeLocation = null;

    protected static String buildLocationPath(String property, String append, String wwDir) {
        String path = propertyToPath(property);

        if (append != null && !append.isEmpty())
            path = WWIO.appendPathPart(path, append.trim());

        if (wwDir != null && !wwDir.isEmpty())
            path = WWIO.appendPathPart(path, wwDir.trim());

        return path;
    }

    //**************************************************************//
    //********************  File Store Configuration  **************//
    //**************************************************************//

    protected static String propertyToPath(String propName) {
        if (propName == null || propName.isEmpty())
            return null;

        String prop = System.getProperty(propName);
        if (prop != null)
            return prop;

        if (propName.equalsIgnoreCase("gov.nasa.worldwind.platform.alluser.store"))
            return determineAllUserLocation();

        if (propName.equalsIgnoreCase("gov.nasa.worldwind.platform.user.store"))
            return determineSingleUserLocation();

        return null;
    }

    protected static String determineAllUserLocation() {
        if (Configuration.isMacOS()) {
            return "/Library/Caches";
        }
        else if (Configuration.isWindowsOS()) {
            String path = System.getenv("ALLUSERSPROFILE");
            if (path == null) {
                Logging.logger().severe("generic.AllUsersWindowsProfileNotKnown");
                return null;
            }
            return path + (Configuration.isWindows7OS() ? "" : "\\Application Data");
        }
        else if (Configuration.isLinuxOS() || Configuration.isUnixOS()
            || Configuration.isSolarisOS()) {
            return UNIX_CACHE_PATH;
        }
        else {
            Logging.logger().warning("generic.UnknownOperatingSystem");
            return null;
        }
    }

    protected static String determineSingleUserLocation() {
        String home = getUserHomeDir();
        if (home == null) {
            Logging.logger().warning("generic.UsersHomeDirectoryNotKnown");
            return null;
        }

        String path = null;

        if (Configuration.isMacOS()) {
            path = "/Library/Caches";
        }
        else if (Configuration.isWindowsOS()) {
            // This produces an incorrect path with duplicate parts,
            // like "C:\Users\PatC:\Users\Pat\Application Data".

            path = "\\Application Data";
        }
        else if (Configuration.isLinuxOS() || Configuration.isUnixOS()
            || Configuration.isSolarisOS()) {
            path = UNIX_CACHE_PATH;
        }
        else {
            Logging.logger().fine("generic.UnknownOperatingSystem");
        }

        if (path == null)
            return null;

        return home + path;
    }

    protected static String getUserHomeDir() {
        return System.getProperty("user.home");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected static void markFileUsed(File file) {
        if (file == null)
            return;

        long currentTime = System.currentTimeMillis();

        if (file.canWrite())
            file.setLastModified(currentTime);

        if (file.isDirectory())
            return;

        File parent = file.getParentFile();
        if (parent != null && parent.canWrite())
            parent.setLastModified(currentTime);
    }

    protected static File makeAbsoluteFile(File file, String fileName) {
        return new File(file.getAbsolutePath() + "/" + fileName);
    }

    protected static String makeAbsolutePath(File dir, String fileName) {
        return dir.getAbsolutePath() + "/" + fileName;
    }

    protected static String normalizeFileStoreName(String fileName) {
        // Convert all file separators to forward slashes, and strip any leading or trailing file separators
        // from the path.
        String normalizedName = fileName.replaceAll("\\\\", "/");
        normalizedName = WWIO.stripLeadingSeparator(normalizedName);
        normalizedName = WWIO.stripTrailingSeparator(normalizedName);

        return normalizedName;
    }

    //**************************************************************//
    //********************  File Store Locations  ******************//
    //**************************************************************//

    protected static String storePathForFile(StoreLocation location, File file) {
        String path = file.getPath();

        if (location != null) {
            String locationPath = location.getFile().getPath();
            if (path.startsWith(locationPath))
                path = path.substring(locationPath.length());
        }

        return path;
    }

    protected void initialize(InputStream xmlConfigStream) {
        DocumentBuilderFactory docBuilderFactory =
            DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(xmlConfigStream);

            // The order of the following two calls is important, because building the writable location may entail
            // creating a location that's included in the specified read locations.
            this.buildWritePaths(doc);
            this.buildReadPaths(doc);

            if (this.writeLocation == null) {
                Logging.logger().warning("FileStore.NoWriteLocation");
            }

            if (this.readLocations.isEmpty()) {
                // This should not happen because the writable location is added to the read list, but check nonetheless
                String message = Logging.getMessage("FileStore.NoReadLocations");
                Logging.logger().severe(message);
                throw new IllegalStateException(message);
            }
        }
        catch (ParserConfigurationException | IOException | SAXException e) {
            String message = Logging.getMessage("FileStore.ExceptionReadingConfigurationFile");
            Logging.logger().severe(message);
            throw new IllegalStateException(message, e);
        }
    }

    protected void buildReadPaths(Node dataFileStoreNode) {
        XPathFactory pathFactory = XPathFactory.newInstance();
        XPath pathFinder = pathFactory.newXPath();

        try {
            NodeList locationNodes = (NodeList) pathFinder.evaluate(
                "/dataFileStore/readLocations/location",
                dataFileStoreNode.getFirstChild(),
                XPathConstants.NODESET);
            for (int i = 0; i < locationNodes.getLength(); i++) {
                Node location = locationNodes.item(i);
                String prop = pathFinder.evaluate("@property", location);
                String wwDir = pathFinder.evaluate("@wwDir", location);
                String append = pathFinder.evaluate("@append", location);
                String isInstall = pathFinder.evaluate("@isInstall", location);
                String isMarkWhenUsed = pathFinder.evaluate("@isMarkWhenUsed", location);

                String path = buildLocationPath(prop, append, wwDir);
                if (path == null) {
                    Logging.logger().log(Level.WARNING, "FileStore.LocationInvalid",
                        prop != null ? prop : Logging.getMessage("generic.Unknown"));
                    continue;
                }

                StoreLocation oldStore = this.storeLocationFor(path);
                if (oldStore != null) // filter out duplicates
                    continue;

                // Even paths that don't exist or are otherwise problematic are added to the list because they may
                // become readable during the session. E.g., removable media. So add them to the search list.

                File pathFile = new File(path);
                if (pathFile.exists() && !pathFile.isDirectory()) {
                    Logging.logger().log(Level.WARNING, "FileStore.LocationIsFile", pathFile.getPath());
                }

                boolean pathIsInstall = isInstall != null && (isInstall.contains("t") || isInstall.contains("T"));
                StoreLocation newStore = new StoreLocation(pathFile, pathIsInstall);

                // If the input parameter "markWhenUsed" is null or empty, then the StoreLocation should keep its
                // default value. Otherwise the store location value is set to true when the input parameter contains
                // "t", and is set to false otherwise.
                if (isMarkWhenUsed != null && !isMarkWhenUsed.isEmpty())
                    newStore.setMarkWhenUsed(isMarkWhenUsed.toLowerCase().contains("t"));

                this.readLocations.add(newStore);
            }
        }
        catch (XPathExpressionException e) {
            String message = Logging.getMessage("FileStore.ExceptionReadingConfigurationFile");
            Logging.logger().severe(message);
            throw new IllegalStateException(message, e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected void buildWritePaths(Node dataFileCacheNode) {
        XPathFactory pathFactory = XPathFactory.newInstance();
        XPath pathFinder = pathFactory.newXPath();

        try {
            NodeList locationNodes = (NodeList) pathFinder.evaluate(
                "/dataFileStore/writeLocations/location",
                dataFileCacheNode.getFirstChild(),
                XPathConstants.NODESET);
            for (int i = 0; i < locationNodes.getLength(); i++) {
                Node location = locationNodes.item(i);
                String prop = pathFinder.evaluate("@property", location);
                String wwDir = pathFinder.evaluate("@wwDir", location);
                String append = pathFinder.evaluate("@append", location);
                String create = pathFinder.evaluate("@create", location);

                String path = buildLocationPath(prop, append, wwDir);
                if (path == null) {
                    Logging.logger().log(Level.WARNING, "FileStore.LocationInvalid",
                        prop != null ? prop : Logging.getMessage("generic.Unknown"));
                    continue;
                }

                Logging.logger().log(Level.FINER, "FileStore.AttemptingWriteDir", path);
                File pathFile = new File(path);
                if (!pathFile.exists() && create != null && (create.contains("t") || create.contains("T"))) {
                    Logging.logger().log(Level.FINER, "FileStore.MakingDirsFor", path);
                    pathFile.mkdirs();
                }

                if (pathFile.isDirectory() && pathFile.canWrite() && pathFile.canRead()) {
                    Logging.logger().log(Level.FINER, "FileStore.WriteLocationSuccessful", path);
                    this.writeLocation = new StoreLocation(pathFile);

                    // Remove the writable location from search path if it already exists.
                    StoreLocation oldLocation = this.storeLocationFor(path);
                    if (oldLocation != null)
                        this.readLocations.remove(oldLocation);

                    // Writable location is always first in search path.
                    this.readLocations.add(0, this.writeLocation);

                    break; // only need one
                }
            }
        }
        catch (XPathExpressionException e) {
            String message = Logging.getMessage("FileStore.ExceptionReadingConfigurationFile");
            Logging.logger().severe(message);
            throw new IllegalStateException(message, e);
        }
    }

    public List<? extends File> getLocations() {
        List<File> locations = new ArrayList<>();
        for (StoreLocation location : this.readLocations) {
            locations.add(location.getFile());
        }
        return locations;
    }

    public File getWriteLocation() {
        return (this.writeLocation != null) ? this.writeLocation.getFile() : null;
    }

    public void addLocation(String newPath, boolean isInstall) {
        this.addLocation(this.readLocations.size(), newPath, isInstall);
    }

    //**************************************************************//
    //********************  File Store Contents  *******************//
    //**************************************************************//

    public void addLocation(int index, String newPath, boolean isInstall) {
        if (newPath == null || newPath.isEmpty()) {
            String message = Logging.getMessage("nullValue.FileStorePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (index < 0) {
            String message = Logging.getMessage("generic.InvalidIndex", index);
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        StoreLocation oldLocation = this.storeLocationFor(newPath);
        if (oldLocation != null)
            this.readLocations.remove(oldLocation);

        if (index > 0 && index > this.readLocations.size())
            index = this.readLocations.size();
        File newFile = new File(newPath);
        StoreLocation newLocation = new StoreLocation(newFile, isInstall);
        this.readLocations.add(index, newLocation);
    }

    public void removeLocation(String path) {
        if (path == null || path.isEmpty()) {
            String message = Logging.getMessage("nullValue.FileStorePathIsNull");
            Logging.logger().severe(message);
            // Just warn and return.
            return;
        }

        StoreLocation location = this.storeLocationFor(path);
        if (location == null) // Path is not part of this FileStore.
            return;

        if (location.equals(this.writeLocation)) {
            String message = Logging.getMessage("FileStore.CannotRemoveWriteLocation", path);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.readLocations.remove(location);
    }

    public boolean isInstallLocation(String path) {
        if (path == null || path.isEmpty()) {
            String message = Logging.getMessage("nullValue.FileStorePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        StoreLocation location = this.storeLocationFor(path);
        return location != null && location.isInstall();
    }

    protected StoreLocation storeLocationFor(String path) {
        File file = new File(path);

        for (StoreLocation location : this.readLocations) {
            if (file.equals(location.getFile()))
                return location;
        }

        return null;
    }

    public boolean containsFile(String fileName) {
        if (fileName == null)
            return false;

        for (StoreLocation location : this.readLocations) {
            File dir = location.getFile();
            File file;

            if (fileName.startsWith(dir.getAbsolutePath()))
                file = new File(fileName);
            else
                file = makeAbsoluteFile(dir, fileName);

            if (file.exists())
                return true;
        }

        return false;
    }

    /**
     * @param fileName       the name of the file to find
     * @param checkClassPath if <code>true</code>, the class path is first searched for the file, otherwise the class
     *                       path is not searched unless it's one of the explicit paths in the cache search directories
     * @return a handle to the requested file if it exists in the cache, otherwise null
     * @throws IllegalArgumentException if <code>fileName</code> is null
     */
    public URL findFile(String fileName, boolean checkClassPath) {
        if (fileName == null) {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (checkClassPath) {
            URL url = this.getClass().getClassLoader().getResource(fileName);
            if (url != null)
                return url;

            // Check for a thread context class loader. This allows the file store to find resources in a case
            // in which different parts of the application are handled by different class loaders.
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            if (tccl != null) {
                url = tccl.getResource(fileName);
                if (url != null)
                    return url;
            }
        }

        for (StoreLocation location : this.readLocations) {
            File dir = location.getFile();
            if (!dir.exists())
                continue;

            File file = new File(makeAbsolutePath(dir, fileName));
            if (file.exists()) {
                try {
                    if (location.isMarkWhenUsed())
                        markFileUsed(file);
                    else
                        markFileUsed(file.getParentFile());

                    return file.toURI().toURL();
                }
                catch (MalformedURLException e) {
                    Logging.logger().log(Level.SEVERE,
                        Logging.getMessage("FileStore.ExceptionCreatingURLForFile", file.getPath()), e);
                }
            }
        }

        return null;
    }

    /**
     * @param fileName the name to give the newly created file
     * @return a handle to the newly created file if it could be created and added to the file store, otherwise null
     * @throws IllegalArgumentException if <code>fileName</code> is null
     */
    public File newFile(String fileName) {
        if (fileName == null) {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.writeLocation != null) {
            String fullPath = makeAbsolutePath(this.writeLocation.getFile(), fileName);
            File file = new File(fullPath);
            boolean canCreateFile = false;

            // This block of code must be synchronized for proper operation. A thread may check that
            // file.getParentFile() does not exist, and become immediately suspended. A second thread may then create
            // the parent and ancestor directories. When the first thread wakes up, file.getParentFile().mkdirs()
            // fails, resulting in an erroneous log message: The log reports that the file cannot be created.
            synchronized (this.fileLock) {
                if (file.getParentFile().exists())
                    canCreateFile = true;
                else if (file.getParentFile().mkdirs())
                    canCreateFile = true;
            }

            if (canCreateFile)
                return file;
            else {
                String msg = Logging.getMessage("generic.CannotCreateFile", fullPath);
                Logging.logger().severe(msg);
            }
        }

        return null;
    }

    /**
     * @param url the "file:" URL of the file to remove from the file store. Only files in the writable WorldWind disk
     *            cache or temp file directory are removed by this method.
     * @throws IllegalArgumentException if <code>url</code> is null
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void removeFile(URL url) {
        if (url == null) {
            String msg = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        try {
            File file = new File(url.toURI());

            // This block of code must be synchronized for proper operation. A thread may check that the file exists,
            // and become immediately suspended. A second thread may then delete that file. When the first thread
            // wakes up, file.delete() fails.
            synchronized (this.fileLock) {
                if (file.exists()) {
                    // Don't remove files outside the cache or temp directory.
                    String parent = file.getParent();
                    if (!(parent.startsWith(this.getWriteLocation().getPath())
                        || parent.startsWith(Configuration.getSystemTempDirectory())))
                        return;

                    file.delete();
                }
            }
        }
        catch (URISyntaxException e) {
            Logging.logger().log(Level.SEVERE, Logging.getMessage("FileStore.ExceptionRemovingFile", url.toString()),
                e);
        }
    }

    public String[] listFileNames(String pathName, FileStoreFilter filter) {
        if (filter == null) {
            String msg = Logging.getMessage("nullValue.FilterIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Do not recurse.
        return this.doListFileNames(pathName, filter, false, false);
    }

    //**************************************************************//
    //********************  File Store Content Discovery  **********//
    //**************************************************************//

    public String[] listAllFileNames(String pathName, FileStoreFilter filter) {
        if (filter == null) {
            String msg = Logging.getMessage("nullValue.FilterIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Recurse, and continue to search each branch after a match is found.
        return this.doListFileNames(pathName, filter, true, false);
    }

    public String[] listTopFileNames(String pathName, FileStoreFilter filter) {
        if (filter == null) {
            String msg = Logging.getMessage("nullValue.FilterIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Recurse, but stop searching a branch after a match is found.
        return this.doListFileNames(pathName, filter, true, true);
    }

    protected String[] doListFileNames(String pathName, FileStoreFilter filter, boolean recurse,
        boolean exitBranchOnFirstMatch) {
        ArrayList<String> nameList = null;

        for (StoreLocation location : this.readLocations) {
            // If the path name is null, then just search from the root of each location. Otherwise search from the
            // named cache path.
            File dir = location.getFile();
            if (pathName != null)
                dir = new File(makeAbsolutePath(dir, pathName));

            // Either the location does not exists, or the speciifed path does not exist under that location. In either
            // case we skip searching this location.
            if (!dir.exists())
                continue;

            // Lazily initialize the list of file names. If no location contains the specified path, then the list is
            // not created, and this method will return null.
            if (nameList == null)
                nameList = new ArrayList<>();

            this.doListFileNames(location, dir, filter, recurse, exitBranchOnFirstMatch, nameList);
        }

        if (nameList == null)
            return null;

        String[] names = new String[nameList.size()];
        nameList.toArray(names);
        return names;
    }

    protected void doListFileNames(StoreLocation location, File dir, FileStoreFilter filter,
        boolean recurse, boolean exitBranchOnFirstMatch, Collection<String> names) {
        Collection<File> subDirs = new ArrayList<>();

        // Search the children of the specified directory. If the child is a directory, append it to the list of sub
        // directories to search later. Otherwise, try to list the file as a match. If the file is a match and
        // exitBranchOnFirstMatch is true, then exit this branch without considering any other files. This has the
        // effect of choosing files closest to the search root.
        for (File childFile : dir.listFiles()) {
            if (childFile == null)
                continue;

            if (childFile.isDirectory()) {
                subDirs.add(childFile);
            }else {
                if (this.listFile(location, childFile, filter, names) && exitBranchOnFirstMatch)
                    return;
            }
        }

        if (!recurse)
            return;

        // Recursively search each sub-directory. If exitBranchOnFirstMatch is true, then we did not find a match under
        // this directory.
        for (File childDir : subDirs) {
            this.doListFileNames(location, childDir, filter, true, exitBranchOnFirstMatch, names);
        }
    }

    protected boolean listFile(StoreLocation location, File file, FileStoreFilter filter,
        Collection<String> names) {
        String fileName = storePathForFile(location, file);
        if (fileName == null)
            return false;

        return this.listFileName(location, normalizeFileStoreName(fileName), filter, names);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected boolean listFileName(StoreLocation location, String fileName, FileStoreFilter filter,
        Collection<String> names) {
        if (!filter.accept(this, fileName))
            return false;

        names.add(fileName);
        return true;
    }

    protected static class StoreLocation extends AVListImpl {
        protected boolean markWhenUsed = false;

        public StoreLocation(File file, boolean isInstall) {
            this.set(AVKey.FILE_STORE_LOCATION, file);
            this.set(AVKey.INSTALLED, isInstall);
        }

        public StoreLocation(File file) {
            this(file, false);
        }

        public File getFile() {
            Object o = this.get(AVKey.FILE_STORE_LOCATION);
            return (o instanceof File) ? (File) o : null;
        }

        public void setFile(File file) {
            this.set(AVKey.FILE_STORE_LOCATION, file);
        }

        public boolean isInstall() {
            Object o = this.get(AVKey.INSTALLED);
            return (o instanceof Boolean) ? (Boolean) o : false;
        }

        public void setInstall(boolean isInstall) {
            this.set(AVKey.INSTALLED, isInstall);
        }

        public boolean isMarkWhenUsed() {
            return markWhenUsed;
        }

        public void setMarkWhenUsed(boolean markWhenUsed) {
            this.markWhenUsed = markWhenUsed;
        }
    }
}
