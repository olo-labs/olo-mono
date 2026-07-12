/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.spi.runtime;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Helpers for {@link RuntimeCapability} lists in catalog JSON and workflow definitions.
 */
public final class RuntimeCapabilities {

    /** Inherited Studio catalog defaults ({@code defaults.runtime.capabilities}). */
    public static final List<RuntimeCapability> INHERITED_CATALOG_DEFAULTS =
            List.of(RuntimeCapability.DEBUG, RuntimeCapability.REPLAY);

    private RuntimeCapabilities() {
    }

    public static List<String> inheritedCatalogDefaultNames() {
        return toNames(INHERITED_CATALOG_DEFAULTS);
    }

    public static RuntimeCapability parse(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("capability name is required");
        }
        return RuntimeCapability.valueOf(name.trim());
    }

    public static List<RuntimeCapability> parseAll(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        List<RuntimeCapability> parsed = new ArrayList<>(names.size());
        for (String name : names) {
            parsed.add(parse(name));
        }
        return List.copyOf(parsed);
    }

    public static List<String> toNames(List<RuntimeCapability> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            return List.of();
        }
        List<String> names = new ArrayList<>(capabilities.size());
        for (RuntimeCapability capability : capabilities) {
            names.add(capability.name());
        }
        return List.copyOf(names);
    }

    /**
     * Returns capability names to persist on a catalog descriptor, or {@code null} when
     * {@code effective} matches {@link #INHERITED_CATALOG_DEFAULTS}.
     */
    public static List<String> materializeDeviations(List<RuntimeCapability> effective) {
        return materializeDeviations(effective, INHERITED_CATALOG_DEFAULTS);
    }

    public static List<String> materializeDeviations(
            List<RuntimeCapability> effective, List<RuntimeCapability> defaults) {
        List<String> deviationNames = toNames(materializeDeviationsAsEnums(effective, defaults));
        return deviationNames.isEmpty() ? null : deviationNames;
    }

    public static List<String> materializeDeviationsFromNames(List<String> effective) {
        if (effective == null || effective.isEmpty()) {
            return null;
        }
        return materializeDeviations(parseAll(effective));
    }

    public static List<RuntimeCapability> resolveEffective(List<String> declared) {
        return resolveEffective(declared, INHERITED_CATALOG_DEFAULTS);
    }

    public static List<String> resolveEffectiveNames(List<String> declared) {
        return toNames(resolveEffective(declared));
    }

    public static List<RuntimeCapability> resolveEffective(
            List<String> declared, List<RuntimeCapability> defaults) {
        return parseAll(resolveEffectiveNames(declared, defaults));
    }

    public static List<String> resolveEffectiveNames(
            List<String> declared, List<RuntimeCapability> defaults) {
        List<String> defaultNames = toNames(defaults);
        if (declared == null || declared.isEmpty()) {
            return defaultNames;
        }
        Set<String> defaultNameSet = new LinkedHashSet<>(defaultNames);
        Set<String> declaredSet = new LinkedHashSet<>(declared);
        if (declaredSet.stream().noneMatch(defaultNameSet::contains)) {
            return orderedUnion(defaultNames, declared);
        }
        if (defaultNameSet.containsAll(declaredSet) && !declaredSet.equals(defaultNameSet)) {
            return List.copyOf(declared);
        }
        if (declaredSet.containsAll(defaultNameSet)) {
            return List.copyOf(declared);
        }
        List<String> extras = new ArrayList<>();
        for (String capability : declared) {
            if (!defaultNameSet.contains(capability)) {
                extras.add(capability);
            }
        }
        return orderedUnion(defaultNames, extras);
    }

    private static List<RuntimeCapability> materializeDeviationsAsEnums(
            List<RuntimeCapability> effective, List<RuntimeCapability> defaults) {
        if (effective == null || effective.isEmpty()) {
            return List.of();
        }
        Set<RuntimeCapability> effectiveSet = new LinkedHashSet<>(effective);
        Set<RuntimeCapability> defaultSet = new LinkedHashSet<>(defaults);
        if (effectiveSet.equals(defaultSet)) {
            return List.of();
        }
        if (effectiveSet.containsAll(defaultSet)) {
            List<RuntimeCapability> deviations = new ArrayList<>();
            for (RuntimeCapability capability : effective) {
                if (!defaultSet.contains(capability)) {
                    deviations.add(capability);
                }
            }
            return deviations;
        }
        return List.copyOf(effective);
    }

    private static List<String> orderedUnion(List<String> base, List<String> extras) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> merged = new ArrayList<>();
        for (String capability : base) {
            if (seen.add(capability)) {
                merged.add(capability);
            }
        }
        for (String capability : extras) {
            if (seen.add(capability)) {
                merged.add(capability);
            }
        }
        return List.copyOf(merged);
    }
}
