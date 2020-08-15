/*
 * Copyright (C) ThermoFisher Scientific Inc.- All Rights Reserved
 * Unauthorized use or copying of this file, via any medium is strictly prohibited and will be subject to legal action.
 * Proprietary and confidential
 */

package jetty;

import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class ThreadPoolMonitorThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolMonitorThread.class);

    private final QueuedThreadPool queuedThreadPool;
    private volatile boolean shutdown;

    ThreadPoolMonitorThread(QueuedThreadPool threadPool) {
        this.queuedThreadPool = threadPool;
        queuedThreadPool.setDetailedDump(true);
    }

    @Override
    public void run() {
        try {
            while (!shutdown) {
                synchronized (this) {
                    wait(60000);
                    StringBuilder b = new StringBuilder();
                    try {
                        queuedThreadPool.dump(b, "  ");
                        logger.info("Stats {}", b.toString());
                    } catch (IOException e) {
                        logger.warn("error dumping stats", e);
                    }

                }
            }
        } catch (InterruptedException ex) {
            // terminate
        }
    }

    public void shutdown() {
        shutdown = true;
        synchronized (this) {
            notifyAll();
        }
    }

}
