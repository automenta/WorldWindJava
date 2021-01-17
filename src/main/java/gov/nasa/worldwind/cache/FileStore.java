/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.cache;

import gov.nasa.worldwind.WWObject;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * @author Tom Gaskins
 * @version $Id: FileStore.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Deprecated public interface FileStore extends WWObject {
    /**
     * Returns the locations that the file store will look for files.
     *
     * @return the list of locations the file store will search when a file is requested.
     */
    List<? extends File> getLocations();

    /**
     * Returns the location that additions to the file store are placed.
     *
     * @return the location at which new entries are placed.
     */
    File getWriteLocation();

    /**
     * Remove a specified read location from the file store. The current write location cannot be removed.
     *
     * @param path the read location to remove.
     * @throws IllegalArgumentException if the specified path is null or identifies the current write location.
     */
    void removeLocation(String path);

    /**
     * Searches the file store for a specified file and returns a reference to it if it is.
     *
     * @param fileName       the file to search for, identified by a path relative to the root of the file store.
     * @param checkClassPath if true, the current classpath is first searched for the file, otherwise the classpath is
     *                       not searched.
     * @return a URL addressing the file if it is found.
     * @throws IllegalArgumentException if the specified path is null.
     */
    URL findFile(String fileName, boolean checkClassPath);

    /**
     * Creates a new, empty file in the file store.
     * <p>
     * If the file store has no write location, the file is not created and null is returned.
     *
     * @param fileName the name of the file to create.
     * @return a reference to the newly created file.
     * @throws IllegalArgumentException if the specified path is null.
     */
    File newFile(String fileName);

    /**
     * Remove an entry from the file store. This method removes files that were added to the file store by {@link
     * #requestFile(String)}. The {@code address} passed to this method must be the same as the address string that was
     * passed to {@code requestFile} when the file was added.
     *
     * @param address file address of the entry to remove. This must be the same string as was passed to {@link
     *                #requestFile(String)}.
     */
    void removeFile(String address);

    /**
     * Removes a file from the file store using the URL to the cached file.
     *
     * @param url a URL, as returned by {@link #findFile(String, boolean)} identifying the file.
     * @throws IllegalArgumentException if the specified URL is null.
     */
    void removeFile(URL url);

    /**
     * Returns an array of strings naming the files discovered directly under a specified file store path name. If the
     * path name is null, files under the store root are searched. This returns null if the path does not exist in the
     * store. Returned names are relative pointers to a file in the store; they are not necessarily a file system path.
     *
     * @param pathName relative path in the file store to search, or null to search the entire file store.
     * @param filter   a file filter.
     * @return an array of file store names. Returns null if the path does not exist in the file store.
     * @throws IllegalArgumentException if the filter is null.
     */
    String[] listFileNames(String pathName, FileStoreFilter filter);

    /**
     * Returns the content type of a cached file.
     *
     * @param address the file's address. If null, null is returned.
     * @return the mime type describing the cached file's contents. Null is returned if the specified address is null.
     */
    String getContentType(String address);

    /**
     * Returns the expiration time of a cached file.
     *
     * @param address the file's address. If null, zero is returned.
     * @return The expiration time of the file, in milliseconds since the Epoch (January 1, 1970, 00:00:00 GMT). Zero
     * indicates that there is no expiration time.
     */
    long getExpirationTime(String address);

    /**
     * Requests a file. If the file exists locally, including as a resource on the classpath, this returns a
     * <code>{@link URL}</code> to the file. Otherwise if the specified address is a URL to a remote location, this
     * initiates a request for the file and returns <code>null</code>. When the request succeeds the file is stored in
     * the local WorldWind cache and subsequent invocations of this method return a URL to the retrieved file.
     *
     * @param address the file address: either a local file, a URL, or a path relative to the root of the file store.
     * @return the file's URL if it exists locally or is a remote file that has been retrieved, otherwise
     * <code>null</code>.
     * @throws IllegalArgumentException if the <code>address</code> is <code>null</code>.
     */
    URL requestFile(String address);

    /**
     * Requests a file and specifies whether to store retrieved files in the cache or in a temporary location. If the
     * file exists locally, including as a resource on the classpath, this returns a <code>{@link URL}</code> to the
     * file. Otherwise if the specified address is a URL to a remote location, this initiates a request for the file and
     * returns <code>null</code>. When the request succeeds the file is stored either in the local WorldWind cache or in
     * a temporary location and subsequent invocations of this method return a URL to the retrieved file.
     * <p>
     * The <code>cacheRemoteFile</code> parameter specifies whether to store a retrieved remote file in the WorldWind
     * cache or in a temporary location. This parameter has no effect if the file exists locally. The temporary location
     * for a retrieved file does not persist between runtime sessions, and subsequent invocations of this method may not
     * return the same temporary location.
     * <p>
     * If a remote file is requested multiple times with different values for <code>cacheRemoteFile</code>, it is
     * undefined whether the retrieved file is stored in the WorldWind cache or in a temporary location.
     *
     * @param address         the file address: either a local file, a URL, or a path relative to the root of the file
     *                        store.
     * @param cacheRemoteFile <code>true</code> to store remote files in the WorldWind cache, or <code>false</code> to
     *                        store remote files in a temporary location. Has no effect if the address is a local file.
     * @return the file's URL if it exists locally or is a remote file that has been retrieved, otherwise
     * <code>null</code>.
     * @throws IllegalArgumentException if the <code>address</code> is <code>null</code>.
     */
    URL requestFile(String address, boolean cacheRemoteFile);
}
