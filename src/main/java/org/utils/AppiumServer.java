package org.utils;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

/**
 * Supplies the Appium server the tests drive, starting one if it has to.
 * <p>
 * Resolution order, so the framework never fights a server you are already running:
 * <ol>
 *     <li>{@code appium.auto.start=false} - the configured URL is used as-is and nothing is
 *         managed here. This is the mode for a remote grid or a server you supervise.</li>
 *     <li>Something is already listening on the configured host and port - that server is used.
 *         Starting a second one on the same port would only fail with "address in use".</li>
 *     <li>Nothing is listening - a local server is started on that port and stopped when the JVM
 *         exits.</li>
 * </ol>
 * One server per run rather than one per test: a server is a long-lived process and starting one
 * takes seconds, where creating a <em>session</em> against a running one takes a moment. The
 * per-test lifecycle belongs to the session, which {@code BaseMobileTest} already owns.
 * <p>
 * Nothing here runs for an API-only suite - the first call arrives from
 * {@link DriverFactory#create(String)}, so a run that never opens a device never starts a server.
 */
@Slf4j
public final class AppiumServer {

    /** How long to wait when probing whether a server is already up. */
    private static final int PROBE_TIMEOUT_MILLIS = 1000;

    /** Only ever set when this class started the server, so teardown stops nothing it borrowed. */
    private static AppiumDriverLocalService managedService;

    private AppiumServer() {
        throw new IllegalStateException("Utility class");
    }

    /** The URL to drive, with a server guaranteed to be answering on it. */
    public static synchronized URL resolveUrl() {
        URL configured = configuredUrl();

        if (!ConfigManager.getBooleanProperty("appium.auto.start")) {
            log.info("appium.auto.start is off - expecting a server at {}", configured);
            return configured;
        }

        if (managedService != null && managedService.isRunning()) {
            return managedService.getUrl();
        }

        if (isListening(configured)) {
            log.info("An Appium server is already running at {} - using it", configured);
            return configured;
        }

        return start(configured);
    }

    /**
     * Stops the server, but only if this class started it.
     * <p>
     * Registered as a shutdown hook rather than wired into a suite listener so it also runs when
     * a run ends abnormally - an orphaned Appium process holding port 4723 would break the next
     * run with an error about the port rather than about the failure that caused it.
     */
    public static synchronized void stop() {
        if (managedService == null) {
            return;
        }
        try {
            managedService.stop();
            log.info("Appium server stopped");
        } catch (Exception e) {
            log.warn("Could not stop the Appium server cleanly.", e);
        } finally {
            managedService = null;
        }
    }

    private static URL start(URL configured) {
        log.info("No Appium server at {} - starting one", configured);

        AppiumServiceBuilder builder = new AppiumServiceBuilder()
                .withIPAddress(configured.getHost())
                .usingPort(configured.getPort())
                // The server's own logging, not the framework's. Warn keeps the wire protocol out
                // of the build output while still surfacing anything that actually went wrong.
                .withArgument(GeneralServerFlag.LOG_LEVEL, "warn")
                // The client's own default is 20 seconds, which this machine loses to: a cold
                // Appium 3 start was measured at 22s here, and the first ever start is slower
                // still because it builds its cache directory. Waiting longer costs nothing when
                // the server is quick - the wait ends as soon as the port answers.
                .withTimeout(startupTimeout());

        try {
            AppiumDriverLocalService service = AppiumDriverLocalService.buildService(builder);
            service.start();

            managedService = service;
            Runtime.getRuntime().addShutdownHook(new Thread(AppiumServer::stop, "appium-server-stop"));

            log.info("Appium server started at {}", service.getUrl());
            return service.getUrl();

        } catch (Exception e) {
            throw new IllegalStateException("❌ CRITICAL: could not start an Appium server on "
                    + configured + ". Appium must be installed and on the PATH ('npm install -g "
                    + "appium'), or Node must be resolvable for the Java client to launch it. To "
                    + "manage the server yourself instead, run 'appium' in a terminal and set "
                    + "appium.auto.start=false. The launcher's own reason follows.", e);
        }
    }

    private static Duration startupTimeout() {
        return Duration.ofSeconds(ConfigManager.getIntProperty("appium.server.startup.timeout"));
    }

    /**
     * Whether anything answers on that host and port.
     * <p>
     * A plain socket connect rather than an HTTP call to {@code /status}: this only needs to know
     * whether the port is taken, and a socket says so in milliseconds without caring what is
     * listening or how it responds.
     */
    private static boolean isListening(URL url) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(url.getHost(), url.getPort()), PROBE_TIMEOUT_MILLIS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static URL configuredUrl() {
        String url = ConfigManager.getProperty("appium.server.url");
        try {
            return URI.create(url).toURL();
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("❌ CRITICAL: 'appium.server.url' is not a usable URL: "
                    + url, e);
        }
    }
}
