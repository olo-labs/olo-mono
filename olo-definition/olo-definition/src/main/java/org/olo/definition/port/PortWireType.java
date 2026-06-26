package org.olo.definition.port;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Closed vocabulary for workflow port wire types ({@code ports.*.type} / {@code acceptType}).
 */
public enum PortWireType {

    ANY("any"),
    MESSAGE("message"),
    /** Tools and hooks offered to a planner/agent host ({@code capabilities} port). */
    CAPABILITIES("capabilities"),
    /** Delegate agent workflows offered to a planner/agent host ({@code agentPlug} port). */
    AGENT_PLUG("agent-plug");

    private final String wireName;

    PortWireType(String wireName) {
        this.wireName = wireName;
    }

    @JsonValue
    public String wireName() {
        return wireName;
    }

    @JsonCreator
    public static PortWireType fromWireName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (PortWireType type : values()) {
            if (type.wireName.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown port wire type: " + raw);
    }
}
