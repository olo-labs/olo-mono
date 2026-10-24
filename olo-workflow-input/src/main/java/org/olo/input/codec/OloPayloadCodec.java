/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.input.codec;

import com.google.protobuf.ByteString;
import io.temporal.api.common.v1.Payload;
import io.temporal.payload.codec.PayloadCodec;
import io.temporal.payload.codec.PayloadCodecException;
import io.temporal.payload.context.HasWorkflowSerializationContext;
import io.temporal.payload.context.SerializationContext;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Seals every Temporal payload with AES-256-GCM under the run's tenant key (D7, M5.2), so workflow history, the Temporal
 * UI and CLI hold no cleartext prompts, arguments, results or retrieved content.
 *
 * <ul>
 *   <li>The tenant comes from the workflow id ({@link TenantWorkflowIds}); payloads outside a tenant workflow use the
 *       ring's platform key.</li>
 *   <li>Each payload records its key id and tenant. Both are authenticated, so a payload cannot be moved into another
 *       tenant's workflow or decrypted under another key.</li>
 *   <li>Rotation adds a key and points the tenant at it; older payloads still decode by their key id.</li>
 *   <li>Payloads this codec did not produce pass through decode unchanged. Encode always seals.</li>
 * </ul>
 * Configure the same ring on every Temporal client and worker that reads or writes OLO payloads.
 */
public final class OloPayloadCodec implements PayloadCodec {

    /** {@code encoding} metadata of a sealed payload. */
    public static final String ENCODING = "binary/olo-aes256gcm";
    static final String METADATA_ENCODING = "encoding";
    static final String METADATA_KEY_ID = "olo-kid";
    static final String METADATA_TENANT = "olo-tenant";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String NO_TENANT = "-";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PayloadKeyRing keys;
    private final String tenant;

    public OloPayloadCodec(PayloadKeyRing keys) {
        this(keys, null);
    }

    private OloPayloadCodec(PayloadKeyRing keys, String tenant) {
        this.keys = Objects.requireNonNull(keys, "keys");
        this.tenant = tenant;
    }

    @Override
    public PayloadCodec withContext(SerializationContext context) {
        String workflowId = context instanceof HasWorkflowSerializationContext workflow ? workflow.getWorkflowId() : null;
        return new OloPayloadCodec(keys, TenantWorkflowIds.tenantOf(workflowId));
    }

    @Override
    public List<Payload> encode(List<Payload> payloads) {
        List<Payload> out = new ArrayList<>(payloads.size());
        for (Payload payload : payloads) {
            out.add(seal(payload));
        }
        return out;
    }

    @Override
    public List<Payload> decode(List<Payload> payloads) {
        List<Payload> out = new ArrayList<>(payloads.size());
        for (Payload payload : payloads) {
            out.add(open(payload));
        }
        return out;
    }

    private Payload seal(Payload payload) {
        String keyId = keys.activeKeyId(tenant);
        String scope = tenant == null ? NO_TENANT : tenant;
        byte[] nonce = new byte[NONCE_BYTES];
        RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keys.key(keyId), "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad(keyId, scope));
            byte[] sealed = cipher.doFinal(payload.toByteArray());
            byte[] data = new byte[NONCE_BYTES + sealed.length];
            System.arraycopy(nonce, 0, data, 0, NONCE_BYTES);
            System.arraycopy(sealed, 0, data, NONCE_BYTES, sealed.length);
            return Payload.newBuilder()
                    .putMetadata(METADATA_ENCODING, ByteString.copyFromUtf8(ENCODING))
                    .putMetadata(METADATA_KEY_ID, ByteString.copyFromUtf8(keyId))
                    .putMetadata(METADATA_TENANT, ByteString.copyFromUtf8(scope))
                    .setData(ByteString.copyFrom(data))
                    .build();
        } catch (GeneralSecurityException e) {
            throw new PayloadCodecException(e);
        }
    }

    private Payload open(Payload payload) {
        ByteString encoding = payload.getMetadataMap().get(METADATA_ENCODING);
        if (encoding == null || !ENCODING.equals(encoding.toStringUtf8())) {
            return payload;
        }
        String keyId = text(payload, METADATA_KEY_ID);
        String scope = text(payload, METADATA_TENANT);
        // In a tenant's workflow only that tenant's payloads open; elsewhere the sealed tenant is still authenticated.
        if (tenant != null && !tenant.equals(scope)) {
            throw new PayloadCodecException(new GeneralSecurityException("payload belongs to another tenant"));
        }
        byte[] data = payload.getData().toByteArray();
        if (data.length <= NONCE_BYTES) {
            throw new PayloadCodecException(new GeneralSecurityException("sealed payload is truncated"));
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keys.key(keyId), "AES"),
                    new GCMParameterSpec(TAG_BITS, data, 0, NONCE_BYTES));
            cipher.updateAAD(aad(keyId, scope));
            return Payload.parseFrom(cipher.doFinal(data, NONCE_BYTES, data.length - NONCE_BYTES));
        } catch (GeneralSecurityException | com.google.protobuf.InvalidProtocolBufferException e) {
            throw new PayloadCodecException(e);
        }
    }

    private static String text(Payload payload, String key) {
        ByteString value = payload.getMetadataMap().get(key);
        if (value == null || value.isEmpty()) {
            throw new PayloadCodecException(new GeneralSecurityException("sealed payload has no " + key));
        }
        return value.toStringUtf8();
    }

    private static byte[] aad(String keyId, String scope) {
        return ("olo-payload-v1\n" + keyId + "\n" + scope).getBytes(StandardCharsets.UTF_8);
    }
}
