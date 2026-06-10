/**
 * OLO worker configuration module: the single entry point for all worker deployment settings.
 *
 * <p>Worker processes must obtain configuration through {@link org.olo.worker.config.WorkerConfigurationProvider}
 * (or {@link org.olo.worker.config.WorkerSettings}) so the storage medium can change later without
 * modifying worker code.
 *
 * <ul>
 *   <li>{@link org.olo.worker.config.WorkerConfigurationProvider} – {@link org.olo.worker.config.WorkerConfigurationProvider#load()} / {@link org.olo.worker.config.WorkerConfigurationProvider#load(boolean)}</li>
 *   <li>{@link org.olo.worker.config.WorkerSettings} – typed accessors for common settings</li>
 *   <li>{@link org.olo.worker.config.model} – configuration document types</li>
 *   <li>{@link org.olo.worker.config.source} – pluggable storage backends</li>
 *   <li>{@link org.olo.worker.config.bootstrap} – bootstrap env vars for locating storage only</li>
 * </ul>
 */
package org.olo.worker.config;
