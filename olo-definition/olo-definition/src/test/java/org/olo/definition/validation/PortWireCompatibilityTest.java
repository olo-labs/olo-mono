package org.olo.definition.validation;

import org.junit.jupiter.api.Test;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.port.PortDirection;
import org.olo.definition.port.PortWireCompatibility;
import org.olo.definition.port.PortWireType;

import static org.assertj.core.api.Assertions.assertThat;

class PortWireCompatibilityTest {

    @Test
    void messagePortsRequireMatchingWireType() {
        PortDefinition output = PortDefinition.builder()
                .id("out")
                .label("message out")
                .schema(PortWireType.MESSAGE.wireName())
                .type(PortWireType.MESSAGE.wireName())
                .direction(PortDirection.OUTPUT)
                .build();
        PortDefinition input = PortDefinition.builder()
                .id("in")
                .label("message in")
                .schema(PortWireType.MESSAGE.wireName())
                .type(PortWireType.MESSAGE.wireName())
                .acceptType(PortWireType.MESSAGE.wireName())
                .direction(PortDirection.INPUT)
                .build();

        assertThat(PortWireCompatibility.compatible(output, input)).isTrue();
    }

    @Test
    void capabilitiesPortsRequireMatchingWireType() {
        PortDefinition output = PortDefinition.builder()
                .id("capabilities")
                .label("capabilities")
                .schema(PortWireType.CAPABILITIES.wireName())
                .type(PortWireType.CAPABILITIES.wireName())
                .direction(PortDirection.OUTPUT)
                .build();
        PortDefinition input = PortDefinition.builder()
                .id("capabilities")
                .label("capabilities")
                .schema(PortWireType.CAPABILITIES.wireName())
                .type(PortWireType.CAPABILITIES.wireName())
                .acceptType(PortWireType.CAPABILITIES.wireName())
                .direction(PortDirection.INPUT)
                .build();

        assertThat(PortWireCompatibility.compatible(output, input)).isTrue();
    }

    @Test
    void agentPlugPortsRequireMatchingWireType() {
        PortDefinition output = PortDefinition.builder()
                .id("agentPlug")
                .label("agent plug")
                .schema(PortWireType.AGENT_PLUG.wireName())
                .type(PortWireType.AGENT_PLUG.wireName())
                .direction(PortDirection.OUTPUT)
                .build();
        PortDefinition input = PortDefinition.builder()
                .id("agentPlug")
                .label("agent plug")
                .schema(PortWireType.AGENT_PLUG.wireName())
                .type(PortWireType.AGENT_PLUG.wireName())
                .acceptType(PortWireType.AGENT_PLUG.wireName())
                .direction(PortDirection.INPUT)
                .build();

        assertThat(PortWireCompatibility.compatible(output, input)).isTrue();
        assertThat(PortWireCompatibility.catalogWireTypes())
                .contains(PortWireType.CAPABILITIES.wireName(), PortWireType.AGENT_PLUG.wireName());
    }

    @Test
    void messageOutputDoesNotConnectToAnyInput() {
        PortDefinition input = PortDefinition.builder()
                .id("in")
                .label("in")
                .schema(PortWireType.ANY.wireName())
                .type(PortWireType.ANY.wireName())
                .acceptType(PortWireType.ANY.wireName())
                .direction(PortDirection.INPUT)
                .build();

        assertThat(PortWireCompatibility.compatible(
                        PortDefinition.builder()
                                .id("out")
                                .label("out")
                                .schema(PortWireType.MESSAGE.wireName())
                                .type(PortWireType.MESSAGE.wireName())
                                .direction(PortDirection.OUTPUT)
                                .build(),
                        input))
                .isFalse();
    }
}
