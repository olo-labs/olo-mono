package org.olo.worker.config.loader;

import org.olo.worker.config.exception.WorkerConfigurationException;
import org.olo.worker.config.model.WorkerConfiguration;

/**
 * Loads a {@link WorkerConfiguration} from a {@link org.olo.worker.config.source.ConfigurationSource}.
 */
public interface WorkerConfigurationLoader {

    WorkerConfiguration load() throws WorkerConfigurationException;
}
