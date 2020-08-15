package jetty;

import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * Launches a Jetty server.
 */
public abstract class JettyLauncher {

    private static class PrefixedNCSARequestLog extends NCSARequestLog {
        private static final String PREFIX = "REQUEST_LOG: ";

        @Override
        public void write(String requestEntry) throws IOException {
            super.write(PREFIX + requestEntry);
        }
    }

    private static class Config {
        private String contextPath;
        private String servletPath = "/*";
        private GuiceApplication application;
        private int port = 8080;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyLauncher.class);
    private static final Config CONFIG = new Config();

    /**
     * Set the context path for this micro-service Jetty server.  This is equivalent to the WAR file path in traditional
     * servlet containers.
     * @param contextPath The context path.
     */
    protected static void contextPath(String contextPath) {
        CONFIG.contextPath = contextPath;
    }

    /**
     * Set the servlet path pattern for this micro-service Jetty server.  This is equivalent to the servlet mapping path
     * in web.xml.
     * @param servletPath The servlet path pattern.
     */
    protected static void servletPath(String servletPath) {
        CONFIG.servletPath = servletPath;
    }

    /**
     * Set the JAXRS Guice-based {@link javax.ws.rs.core.Application} for this micro-service Jetty server.
     * @param application The application instance.
     */
    protected static void application(GuiceApplication application) {
        CONFIG.application = application;
    }

    protected static void port(int port) {
        CONFIG.port = port;
    }

    /**
     * Start the Jetty server that serves this micro-service.  Override server settings via System properties
     * settings or environmental variables:
     *
     * <table summary="Settings" border="1" cellpadding="0" cellspacing="0">
     *     <tr>
     *         <td>Property</td><td>Default Setting</td>
     *     </tr>
     *     <tr>
     *         <td>JETTY_THREAD_POOL_MIN_THREADS</td><td>10</td>
     *     </tr>
     *     <tr>
     *         <td>JETTY_THREAD_POOL_MAX_THREADS</td><td>1000</td>
     *     </tr>
     *     <tr>
     *         <td>JETTY_THREAD_POOL_IDLE_TIMEOUT</td><td>30000</td>
     *     </tr>
     *     <tr>
     *         <td>JETTY_CONNECTOR_IDLE_TIMEOUT</td><td>20000</td>
     *     </tr>
     *     <tr>
     *         <td>JETTY_THREAD_POOL_MONITOR</td><td>true</td>
     *     </tr>
     *     <tr>
     *         <td>JETTY_LOG_REQUESTS</td><td>true</td>
     *     </tr>
     * </table>
     */
    protected static void serve() {
        int minThreads = Props.getInteger("JETTY_THREAD_POOL_MIN_THREADS", 10);
        int maxThreads = Props.getInteger("JETTY_THREAD_POOL_MAX_THREADS", 1000);
        int idleTimeout = Props.getInteger("JETTY_THREAD_POOL_IDLE_TIMEOUT", 30000);
        int connectorIdleTimeout = Props.getInteger("JETTY_CONNECTOR_IDLE_TIMEOUT", 20000);
        boolean monitorThreads = Props.getBoolean("JETTY_THREAD_POOL_MONITOR", true);
        boolean logRequests = Props.getBoolean("JETTY_LOG_REQUESTS", true);

        QueuedThreadPool threadPool = threadPool(minThreads, maxThreads, idleTimeout, monitorThreads);

        HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(servletContextHandler());

        if (logRequests) {
            handlers.addHandler(requestLogHandler());
        }

        Server server = server(CONFIG.port, connectorIdleTimeout, threadPool);
        server.setHandler(handlers);

        try {
            server.start();
        } catch (Exception e) {
            LOGGER.error("FATAL: Unable to start Jetty server", e);
            System.exit(1);
        }

        // The use of server.join() the will make the current thread join and
        // wait until the server is done executing.
        // See: http://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html#join()
        try {
            server.join();
        } catch (InterruptedException e) {
            LOGGER.error("ERROR: Unable to join Jetty server thread. Exiting.", e);
            System.exit(1);
        }
    }

    private static RequestLogHandler requestLogHandler() {
        NCSARequestLog requestLog = new PrefixedNCSARequestLog();
        requestLog.setExtended(true);
        requestLog.setLogCookies(false);
        requestLog.setLogTimeZone("GMT");

        RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(requestLog);
        return requestLogHandler;
    }

    private static ServletContextHandler servletContextHandler() {
        requireNonNull(CONFIG.application, "Application may not be null");
        requireNonNull(CONFIG.contextPath, "Context path may not be null");
        requireNonNull(CONFIG.servletPath, "Servlet path may not be null");
        if (!CONFIG.contextPath.startsWith("/")) {
            throw new IllegalArgumentException("Context path must begin with slash (/)");
        }

        ServletHolder holder = new ServletHolder(new CXFNonSpringJaxrsServlet(CONFIG.application));
        ServletContextHandler rootContext = new ServletContextHandler();
        rootContext.setContextPath(CONFIG.contextPath);
        rootContext.addServlet(holder, CONFIG.servletPath);
        return rootContext;
    }

    private static Server server(int port, int connectorIdleTimeout, QueuedThreadPool threadPool) {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(8443);
        httpConfig.setOutputBufferSize(32768);
        httpConfig.setRequestHeaderSize(8192);
        httpConfig.setResponseHeaderSize(8192);
        httpConfig.setSendServerVersion(true);
        httpConfig.setSendDateHeader(false);

        Server server = new Server(threadPool);
        server.setDumpAfterStart(false);
        server.setDumpBeforeStop(false);
        server.setStopAtShutdown(true);

        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        connector.setPort(port);
        connector.setIdleTimeout(connectorIdleTimeout);
        server.addConnector(connector);

        return server;
    }

    private static QueuedThreadPool threadPool(int minThreads, int maxThreads, int idleTimeout, boolean monitorThreads) {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(minThreads);
        threadPool.setMaxThreads(maxThreads);
        threadPool.setIdleTimeout(idleTimeout);

        if (monitorThreads) {
            ThreadPoolMonitorThread monitorThread = new ThreadPoolMonitorThread(threadPool);
            monitorThread.start();
        }
        return threadPool;
    }

}
