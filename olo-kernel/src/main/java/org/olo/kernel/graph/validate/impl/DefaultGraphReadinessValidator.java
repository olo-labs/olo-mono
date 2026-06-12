package org.olo.kernel.graph.validate.impl;

import org.olo.kernel.graph.index.GraphIndex;
import org.olo.kernel.graph.start.StartNodeResolver;
import org.olo.kernel.graph.validate.GraphReadinessValidator;

import java.util.Objects;

public final class DefaultGraphReadinessValidator implements GraphReadinessValidator {

    private final StartNodeResolver startNodeResolver;

    public DefaultGraphReadinessValidator(StartNodeResolver startNodeResolver) {
        this.startNodeResolver = Objects.requireNonNull(startNodeResolver, "startNodeResolver");
    }

    @Override
    public boolean isReady(GraphIndex index) {
        Objects.requireNonNull(index, "index");
        return !index.nodes().isEmpty() && startNodeResolver.resolve(index).isPresent();
    }
}
