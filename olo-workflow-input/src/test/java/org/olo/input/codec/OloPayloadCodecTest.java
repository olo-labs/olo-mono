/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.input.codec;

import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.payload.codec.PayloadCodecException;
import io.temporal.payload.context.WorkflowSerializationContext;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** M5.2 (D7): Temporal payloads are sealed per tenant; history holds no cleartext. */
class OloPayloadCodecTest {

    private static byte[] key() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return key;
    }

    private final byte[] t1 = key(), t2 = key(), platform = key();
    private final PayloadKeyRing ring = new PayloadKeyRing(Map.of("t1-a", t1, "t2-a", t2, "platform-1", platform),
            Map.of("t1", "t1-a", "t2", "t2-a"), "platform-1");
    private final OloPayloadCodec codec = new OloPayloadCodec(ring);

    private OloPayloadCodec in(String workflowId) {
        return (OloPayloadCodec) codec.withContext(new WorkflowSerializationContext("default", workflowId));
    }

    private static Payload plain(String text) {
        return Payload.newBuilder().putMetadata("encoding", com.google.protobuf.ByteString.copyFromUtf8("json/plain"))
                .setData(com.google.protobuf.ByteString.copyFromUtf8(text)).build();
    }

    @Test
    void payloadsAreSealedWithTheRunsTenantKeyAndOpenOnlyThere() {
        var run = in(TenantWorkflowIds.run("t1", "run-9"));
        Payload sealed = run.encode(List.of(plain("{\"prompt\":\"my salary is 100k\"}"))).get(0);

        assertThat(sealed.getMetadataMap().get("encoding").toStringUtf8()).isEqualTo(OloPayloadCodec.ENCODING);
        assertThat(sealed.getMetadataMap().get("olo-kid").toStringUtf8()).isEqualTo("t1-a");
        assertThat(sealed.getData().toString(StandardCharsets.UTF_8)).doesNotContain("salary");
        assertThat(run.decode(List.of(sealed)).get(0)).isEqualTo(plain("{\"prompt\":\"my salary is 100k\"}"));
        // An activity of the same run (same workflow id) reads it; another tenant's workflow never does.
        assertThat(in(TenantWorkflowIds.run("t1", "run-9")).decode(List.of(sealed))).hasSize(1);
        assertThatThrownBy(() -> in(TenantWorkflowIds.run("t2", "run-3")).decode(List.of(sealed)))
                .isInstanceOf(PayloadCodecException.class);
    }

    @Test
    void tamperingAndRelabellingAreDetected() {
        Payload sealed = in(TenantWorkflowIds.run("t1", "r")).encode(List.of(plain("x"))).get(0);
        byte[] data = sealed.getData().toByteArray();
        data[data.length - 1] ^= 1;
        Payload tampered = sealed.toBuilder().setData(com.google.protobuf.ByteString.copyFrom(data)).build();
        assertThatThrownBy(() -> codec.decode(List.of(tampered))).isInstanceOf(PayloadCodecException.class);
        Payload relabelled = sealed.toBuilder().putMetadata("olo-tenant", com.google.protobuf.ByteString.copyFromUtf8("t2")).build();
        assertThatThrownBy(() -> codec.decode(List.of(relabelled))).isInstanceOf(PayloadCodecException.class);
    }

    @Test
    void rotationKeepsOldPayloadsReadable() throws Exception {
        Payload old = in(TenantWorkflowIds.run("t1", "r")).encode(List.of(plain("before"))).get(0);
        String file = "{\"keys\":{\"t1-a\":\"" + Base64.getEncoder().encodeToString(t1) + "\",\"t1-b\":\""
                + Base64.getEncoder().encodeToString(key()) + "\",\"platform-1\":\"" + Base64.getEncoder().encodeToString(platform)
                + "\"},\"tenants\":{\"t1\":\"t1-b\"},\"platform\":\"platform-1\"}";
        var rotated = (OloPayloadCodec) new OloPayloadCodec(PayloadKeyRing.parse(file))
                .withContext(new WorkflowSerializationContext("default", TenantWorkflowIds.run("t1", "r")));
        assertThat(rotated.encode(List.of(plain("after"))).get(0).getMetadataMap().get("olo-kid").toStringUtf8()).isEqualTo("t1-b");
        assertThat(rotated.decode(List.of(old)).get(0)).isEqualTo(plain("before"));
    }

    @Test
    void foreignPayloadsPassThroughAndUntenantedOnesUseThePlatformKey() {
        assertThat(codec.decode(List.of(plain("system")))).containsExactly(plain("system"));
        Payload sealed = in("some-other-workflow").encode(List.of(plain("y"))).get(0);
        assertThat(sealed.getMetadataMap().get("olo-kid").toStringUtf8()).isEqualTo("platform-1");
    }

    @Test
    void theTemporalDataConverterRoundTripsThroughTheCodec() {
        var converter = new CodecDataConverter(DefaultDataConverter.newDefaultInstance(), List.of(codec));
        var context = new WorkflowSerializationContext("default", TenantWorkflowIds.run("t1", "r"));
        var scoped = converter.withContext(context);
        Optional<io.temporal.api.common.v1.Payloads> payloads = scoped.toPayloads(Map.of("arguments", "card 4111"));
        assertThat(payloads.orElseThrow().toString()).doesNotContain("4111");
        assertThat(scoped.fromPayloads(0, payloads, Map.class, Map.class)).isEqualTo(Map.of("arguments", "card 4111"));
    }

    @Test
    void protectionIsRequiredUnlessExplicitlyDisabledForDevelopment() {
        assertThat(PayloadProtection.dataConverter(ring, null)).isInstanceOf(CodecDataConverter.class);
        assertThatThrownBy(() -> PayloadProtection.dataConverter((PayloadKeyRing) null, null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> PayloadProtection.dataConverter((PayloadKeyRing) null, "off")).isInstanceOf(IllegalStateException.class);
        assertThat(PayloadProtection.dataConverter((PayloadKeyRing) null, PayloadProtection.DISABLED_FOR_DEVELOPMENT))
                .isNotInstanceOf(CodecDataConverter.class);
    }

    @Test
    void workflowIdsCarryTheTenant() {
        assertThat(TenantWorkflowIds.run("t1", "abc")).isEqualTo("run-t1~abc");
        assertThat(TenantWorkflowIds.tenantOf("run-t1~abc")).isEqualTo("t1");
        assertThat(TenantWorkflowIds.tenantOf(TenantWorkflowIds.child("t1", "child-1"))).isEqualTo("t1");
        assertThat(TenantWorkflowIds.tenantOf("run-abc")).isNull();
        assertThatThrownBy(() -> TenantWorkflowIds.run("t~1", "abc")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void prefixedTenantNamesRoundTripForRootChildAndGrandchildWorkflows() {
        for (String tenant : List.of("t1", "run-t1", "child-t1", "run-child-t1")) {
            assertThat(TenantWorkflowIds.tenantOf(TenantWorkflowIds.run(tenant, "root"))).isEqualTo(tenant);
            String child = TenantWorkflowIds.child(tenant, "child-1");
            assertThat(TenantWorkflowIds.tenantOf(child)).isEqualTo(tenant);
            assertThat(TenantWorkflowIds.tenantOf(TenantWorkflowIds.child(TenantWorkflowIds.tenantOf(child), "grandchild")))
                    .isEqualTo(tenant);
        }
    }

    @Test
    void tenantPayloadsCannotDowngradeToCleartextOrASharedPlatformKey() {
        assertThatThrownBy(() -> in(TenantWorkflowIds.run("unknown-tenant", "r")).encode(List.of(plain("secret"))))
                .isInstanceOf(PayloadCodecException.class);
        assertThatThrownBy(() -> in(TenantWorkflowIds.run("t1", "r")).decode(List.of(plain("secret"))))
                .isInstanceOf(PayloadCodecException.class);
    }

    @Test
    void exceptionMessagesAreEncryptedAlongWithPayloads() {
        var converter = PayloadProtection.dataConverter(ring, null)
                .withContext(new WorkflowSerializationContext("default", TenantWorkflowIds.run("t1", "r")));
        var failure = converter.exceptionToFailure(new IllegalStateException("private account 123456"));
        assertThat(failure.toString()).doesNotContain("private account 123456");
        assertThat(converter.failureToException(failure).getMessage()).contains("private account 123456");
    }
}
