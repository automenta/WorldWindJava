/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.KVMap;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Basic implementation of NetworkStatus.
 *
 * @author tag
 * @version $Id: BasicNetworkStatus.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BasicNetworkStatus extends KVMap implements NetworkStatus {
    protected static final long DEFAULT_TRY_AGAIN_INTERVAL_MS = 2 * 60_000L;
    protected static final int DEFAULT_ATTEMPT_LIMIT = 8; // number of unavailable events to declare host unavailable
    protected static final long NETWORK_STATUS_REPORT_INTERVAL_MS = (long) 120.000e3;
    protected static final String[] DEFAULT_NETWORK_TEST_SITES = {
        "cloudflare.com", "archive.org", "w3c.org", "wikipedia.org", "github.com"
        //"www.nasa.gov", "worldwind.arc.nasa.gov", "google.com", "microsoft.com", "yahoo.com"
    };
    // Fields for determining and remembering overall network status.
    protected final ConcurrentHashMap<String, HostInfo> hostMap = new ConcurrentHashMap<>();
    protected final AtomicLong lastUnavailableLogTime = new AtomicLong(System.currentTimeMillis());
    protected final AtomicLong lastAvailableLogTime = new AtomicLong(System.currentTimeMillis() + 1);
    protected final AtomicLong lastNetworkCheckTime = new AtomicLong(System.currentTimeMillis());
    protected final AtomicLong lastNetworkStatusReportTime = new AtomicLong(0);
    protected final AtomicBoolean lastNetworkUnavailableResult = new AtomicBoolean(false);
    // Values exposed to the application.
    private final CopyOnWriteArrayList<String> networkTestSites = new CopyOnWriteArrayList<>();
    private final AtomicLong tryAgainInterval = new AtomicLong(BasicNetworkStatus.DEFAULT_TRY_AGAIN_INTERVAL_MS);
    private final AtomicInteger attemptLimit = new AtomicInteger(BasicNetworkStatus.DEFAULT_ATTEMPT_LIMIT);
    private boolean offlineMode;

    public BasicNetworkStatus() {
        String oms = Configuration.getStringValue(Keys.OFFLINE_MODE, "false");
        this.offlineMode = !oms.isEmpty() && oms.charAt(0) == 't' || !oms.isEmpty() && oms.charAt(0) == 'T';

        this.establishNetworkTestSites();
    }

    /**
     * Determine if a host is reachable by attempting to resolve the host name, and then attempting to open a connection
     * using either https or http.
     *
     * @param hostName Name of the host to connect to.
     * @return {@code true} if a the host is reachable, {@code false} if the host name cannot be resolved, or if opening
     * a connection to the host fails.
     */
    protected static boolean isHostReachable(String hostName) {
        try {
            // Assume host is unreachable if we can't get its dns entry without getting an exception
            //noinspection ResultOfMethodCallIgnored
            InetAddress.getByName(hostName);
        }
        catch (UnknownHostException e) {
            String message = Logging.getMessage("NetworkStatus.UnreachableTestHost", hostName);
            Logging.logger().fine(message);
            return false;
        }
        catch (RuntimeException e) {
            String message = Logging.getMessage("NetworkStatus.ExceptionTestingHost", hostName);
            Logging.logger().info(message);
            return false;
        }

        // Was able to get internet address, but host still might not be reachable because the address might have been
        // cached earlier when it was available. So need to try something else.

        URLConnection connection = null;
        try {
            final String[] protocols = {"https://", "http://"};
            for (String protocol : protocols) {
                URL url = new URL(protocol + hostName);

                Proxy proxy = WWIO.configureProxy();
                connection = proxy != null ? url.openConnection(proxy) : url.openConnection();

                connection.setConnectTimeout(2000);
                connection.setReadTimeout(2000);
                String ct = connection.getContentType();
                if (ct != null)
                    return true;
            }
        } catch (IOException e) {
            String message = Logging.getMessage("NetworkStatus.ExceptionTestingHost", hostName);
            Logging.logger().info(message);
        } finally {
            if (connection instanceof HttpURLConnection)
                ((HttpURLConnection) connection).disconnect();
        }

        return false;
    }

    /**
     * Determines and stores the network sites to test for public network connectivity. The sites are drawn from the
     * JVM's gov.nasa.worldwind.avkey.NetworkStatusTestSites property ({@link Keys#NETWORK_STATUS_TEST_SITES}). If that
     * property is not defined, the sites are drawn from the same property in the WorldWind or application configuration
     * file. If the sites are not specified there, the set of sites specified in {@link #DEFAULT_NETWORK_TEST_SITES} are
     * used. To indicate an empty list in the JVM property or configuration file property, specify an empty site list,
     * "".
     */
    protected void establishNetworkTestSites() {
        String testSites = System.getProperty(Keys.NETWORK_STATUS_TEST_SITES);

        if (testSites == null)
            testSites = Configuration.getStringValue(Keys.NETWORK_STATUS_TEST_SITES);

        if (testSites == null) {
            this.networkTestSites.addAll(Arrays.asList(BasicNetworkStatus.DEFAULT_NETWORK_TEST_SITES));
        } else {
            String[] sites = testSites.split(",");
            List<String> actualSites = new ArrayList<>(sites.length);

            for (String s : sites) {
                String site = WWUtil.removeWhiteSpace(s);
                if (!WWUtil.isEmpty(site))
                    actualSites.add(site);
            }

            this.setNetworkTestSites(actualSites);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isOfflineMode() {
        return offlineMode;
    }

    /**
     * {@inheritDoc}
     */
    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
    }

    /**
     * {@inheritDoc}
     */
    public int getAttemptLimit() {
        return this.attemptLimit.get();
    }

    /**
     * {@inheritDoc}
     */
    public void setAttemptLimit(int limit) {
        if (limit < 1) {
            String message = Logging.getMessage("NetworkStatus.InvalidAttemptLimit");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.attemptLimit.set(limit);
    }

    /**
     * {@inheritDoc}
     */
    public long getTryAgainInterval() {
        return this.tryAgainInterval.get();
    }

    /**
     * {@inheritDoc}
     */
    public void setTryAgainInterval(long interval) {
        if (interval < 0) {
            String message = Logging.getMessage("NetworkStatus.InvalidTryAgainInterval");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.tryAgainInterval.set(interval);
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getNetworkTestSites() {
        return new ArrayList<>(networkTestSites);
    }

    /**
     * {@inheritDoc}
     */
    public void setNetworkTestSites(List<String> networkTestSites) {
        this.networkTestSites.clear();

        if (networkTestSites != null)
            this.networkTestSites.addAll(networkTestSites);
    }

    /**
     * {@inheritDoc}
     */
    public void logUnavailableHost(URL url) {
        if (this.offlineMode)
            return;

        final long now = System.currentTimeMillis();
        this.lastUnavailableLogTime.set(now);

        hostMap.compute(url.getHost(), (hn, hi)-> {


            if (hi != null) {
                hi.lastLogTime.set(now);
                if (!hi.isUnavailable())
                    hi.logCount.incrementAndGet();
            } else {
                hi = new HostInfo(this.attemptLimit.get(), this.tryAgainInterval.get());
                hi.logCount.set(1);
            }
            return hi;
        });

    }

    /**
     * {@inheritDoc}
     */
    public void logAvailableHost(URL url) {
        if (this.offlineMode)
            return;

        String hostName = url.getHost();
        HostInfo hi = this.hostMap.remove(hostName);

        this.lastAvailableLogTime.set(System.currentTimeMillis());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isHostUnavailable(URL url) {
        if (this.offlineMode)
            return true;

        String hostName = url.getHost();
        HostInfo hi = this.hostMap.get(hostName);
        if (hi == null)
            return false;

        if (hi.isTimeToTryAgain()) {
            hi.logCount.set(0); // info removed from table in logAvailableHost
            return false;
        }

        return hi.isUnavailable();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNetworkUnavailable() {
        return this.offlineMode || this.isNetworkUnavailable(10000L);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean isNetworkUnavailable(long checkInterval) {
        if (this.offlineMode)
            return true;

        // If there's been success since failure, network assumed to be reachable.
        if (this.lastAvailableLogTime.get() > this.lastUnavailableLogTime.get()) {
            this.lastNetworkUnavailableResult.set(false);
            return this.lastNetworkUnavailableResult.get();
        }

        long now = System.currentTimeMillis();

        // If there's been success recently, network assumed to be reachable.
        if (!this.lastNetworkUnavailableResult.get() && now - this.lastAvailableLogTime.get() < checkInterval) {
            return this.lastNetworkUnavailableResult.get();
        }

        // If query comes too soon after an earlier one that addressed the network, return the earlier result.
        if (now - this.lastNetworkCheckTime.get() < checkInterval) {
            return this.lastNetworkUnavailableResult.get();
        }

        this.lastNetworkCheckTime.set(now);

        if (!this.isWorldWindServerUnavailable()) {
            this.lastNetworkUnavailableResult.set(false); // network not unreachable
            return this.lastNetworkUnavailableResult.get();
        }

        for (String testHost : networkTestSites) {
            if (BasicNetworkStatus.isHostReachable(testHost)) {
                {
                    this.lastNetworkUnavailableResult.set(false); // network not unreachable
                    return this.lastNetworkUnavailableResult.get();
                }
            }
        }

        if (now - this.lastNetworkStatusReportTime.get() > BasicNetworkStatus.NETWORK_STATUS_REPORT_INTERVAL_MS) {
            this.lastNetworkStatusReportTime.set(now);
            String message = Logging.getMessage("NetworkStatus.NetworkUnreachable");
            Logging.logger().info(message);
        }

        this.lastNetworkUnavailableResult.set(true); // if no successful contact then network is unreachable
        return this.lastNetworkUnavailableResult.get();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isWorldWindServerUnavailable() {
        return this.offlineMode || !BasicNetworkStatus.isHostReachable("worldwind.arc.nasa.gov");
    }

    protected static class HostInfo {
        protected final long tryAgainInterval;
        protected final int attemptLimit;
        protected final AtomicInteger logCount = new AtomicInteger();
        protected final AtomicLong lastLogTime = new AtomicLong();

        protected HostInfo(int attemptLimit, long tryAgainInterval) {
            this.lastLogTime.set(System.currentTimeMillis());
            this.logCount.set(1);
            this.tryAgainInterval = tryAgainInterval;
            this.attemptLimit = attemptLimit;
        }

        protected boolean isUnavailable() {
            return this.logCount.get() >= this.attemptLimit;
        }

        protected boolean isTimeToTryAgain() {
            return System.currentTimeMillis() - this.lastLogTime.get() >= this.tryAgainInterval;
        }
    }
}