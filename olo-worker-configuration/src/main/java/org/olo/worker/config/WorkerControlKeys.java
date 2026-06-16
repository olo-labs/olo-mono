package org.olo.worker.config;

/**
 * Redis keys shared between studio UI / olo-be and running worker processes.
 */
public final class WorkerControlKeys {

    /** Monotonic token; worker reloads configuration and Temporal queues when this value changes. */
    public static final String REFRESH_REDIS_KEY = "olo:worker:refresh";

    private WorkerControlKeys() {
    }
}
