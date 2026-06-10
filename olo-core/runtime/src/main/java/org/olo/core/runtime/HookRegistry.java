package org.olo.core.runtime;

import org.olo.core.hook.CoreHooks;
import org.olo.spi.hook.Hook;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of {@link Hook} implementations keyed by {@link Hook#implementationId()}.
 */
public final class HookRegistry {

    private final Map<String, Hook> byId = new LinkedHashMap<>();

    public void register(Hook hook) {
        if (hook == null) {
            throw new IllegalArgumentException("hook must not be null");
        }
        byId.put(hook.implementationId(), hook);
    }

    public void registerAll(Collection<Hook> hooks) {
        if (hooks != null) {
            hooks.forEach(this::register);
        }
    }

    public Optional<Hook> find(String implementationId) {
        if (implementationId == null || implementationId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(implementationId));
    }

    public Map<String, Hook> snapshot() {
        return Map.copyOf(byId);
    }

    public static HookRegistry withDefaults() {
        HookRegistry registry = new HookRegistry();
        registry.registerAll(CoreHooks.all());
        return registry;
    }
}
