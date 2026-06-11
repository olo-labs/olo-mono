package org.olo.spi.runtime;

/**
 * Canonical runtime capability contract shared by Studio catalog, workflow definitions, workers,
 * replay engines, and SDKs.
 * <p>
 * Serialized in JSON as uppercase names (e.g. {@code "DEBUG"}, {@code "REPLAY"}) on
 * {@code runtime.capabilities}.
 */
public enum RuntimeCapability {

    /** Interactive debugger / step-through. */
    DEBUG,

    /** Deterministic or workflow-level replay. */
    REPLAY,

    /** Checkpointing and restore (time-travel foundation). */
    CHECKPOINT,

    /** Orchestrator timeout policies. */
    TIMEOUT,

    /** Failed step may be retried. */
    RETRY,

    /** Activity heartbeat expectation. */
    HEARTBEAT,

    /** Async completion via signal or callback. */
    ASYNC_COMPLETION,

    /** Time-travel across workflow history (future). */
    TIME_TRAVEL,

    /** Streaming input/output during execution (future). */
    STREAMING,

    /** Human-in-the-loop gate (future). */
    HUMAN_IN_LOOP
}
