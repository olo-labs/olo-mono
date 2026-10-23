/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.olo.toolgate.client.impl.FileAndEnvCredentialResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileAndEnvCredentialResolverTest {

    @TempDir
    Path dir;

    @Test
    void environmentVariableWinsOverFile() throws Exception {
        Files.writeString(dir.resolve("triage-agent.token"), "from-file\n");
        var resolver = new FileAndEnvCredentialResolver(dir, Map.of("OLO_TOOLGATE_TOKEN_TRIAGE_AGENT", "from-env"));

        assertThat(resolver.resolveToken("triage-agent")).isEqualTo("from-env");
    }

    @Test
    void readsTrimmedTokenFileAndPicksUpRotation() throws Exception {
        var resolver = new FileAndEnvCredentialResolver(dir, Map.of());
        Path file = dir.resolve("triage-agent.token");

        Files.writeString(file, "  first \n");
        assertThat(resolver.resolveToken("triage-agent")).isEqualTo("first");

        Files.writeString(file, "second");
        assertThat(resolver.resolveToken("triage-agent")).isEqualTo("second");
    }

    @Test
    void missingTokenNamesBothPlacesToConfigureIt() {
        var resolver = new FileAndEnvCredentialResolver(dir, Map.of());

        assertThatThrownBy(() -> resolver.resolveToken("report.agent"))
                .isInstanceOf(ToolGateException.class)
                .hasMessageContaining("OLO_TOOLGATE_TOKEN_REPORT_AGENT")
                .hasMessageContaining("report.agent.token")
                .satisfies(e -> assertThat(((ToolGateException) e).kind())
                        .isEqualTo(ToolGateException.Kind.CREDENTIAL_MISSING));
    }

    @Test
    void rejectsNamesThatCouldEscapeTheCredentialsFolder() {
        var resolver = new FileAndEnvCredentialResolver(dir, Map.of());

        assertThatThrownBy(() -> resolver.resolveToken("../secret")).isInstanceOf(ToolGateException.class);
        assertThatThrownBy(() -> resolver.resolveToken("a/b")).isInstanceOf(ToolGateException.class);
        assertThatThrownBy(() -> resolver.resolveToken("")).isInstanceOf(ToolGateException.class);
        assertThatThrownBy(() -> resolver.resolveToken(null)).isInstanceOf(ToolGateException.class);
    }

    @Test
    void agentNameRules() {
        assertThat(AgentNames.isValid("log-failure-agent")).isTrue();
        assertThat(AgentNames.isValid("ops.triage_v2")).isTrue();
        assertThat(AgentNames.isValid("-leading-dash")).isFalse();
        assertThat(AgentNames.isValid("a".repeat(129))).isFalse();
        assertThat(AgentNames.tokenEnvironmentVariable("ops.triage-v2")).isEqualTo("OLO_TOOLGATE_TOKEN_OPS_TRIAGE_V2");
    }
}
