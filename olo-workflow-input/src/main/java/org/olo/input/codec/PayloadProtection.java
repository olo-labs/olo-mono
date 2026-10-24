/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.input.codec;

import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.DefaultDataConverter;

import java.util.List;

/**
 * The Temporal data converter every OLO client and worker uses (M5.2): payloads sealed by {@link OloPayloadCodec} under
 * the key ring in {@value PayloadKeyRing#ENV_FILE}. Without a ring, startup fails, unless
 * {@value #ENV_MODE}={@value #DISABLED_FOR_DEVELOPMENT} is set for a local development stack.
 */
public final class PayloadProtection {

    public static final String ENV_MODE = "OLO_PAYLOAD_PROTECTION";
    public static final String DISABLED_FOR_DEVELOPMENT = "disabled-for-development";

    private PayloadProtection() {
    }

    /** The converter for this process, from the environment. */
    public static DataConverter dataConverter() {
        return dataConverter(PayloadKeyRing.fromEnvironment().orElse(null), System.getenv(ENV_MODE));
    }

    /** The converter for a key file path (blank for none) and a mode, for callers that bind their own settings. */
    public static DataConverter dataConverter(String keysFile, String mode) {
        PayloadKeyRing ring = null;
        if (keysFile != null && !keysFile.isBlank()) {
            try {
                ring = PayloadKeyRing.parse(java.nio.file.Files.readString(java.nio.file.Path.of(keysFile.trim())));
            } catch (java.io.IOException e) {
                throw new IllegalStateException("payload key file cannot be read", e);
            }
        }
        return dataConverter(ring, mode);
    }

    static DataConverter dataConverter(PayloadKeyRing ring, String mode) {
        if (ring != null) {
            return new CodecDataConverter(DefaultDataConverter.newDefaultInstance(), List.of(new OloPayloadCodec(ring)), true);
        }
        if (DISABLED_FOR_DEVELOPMENT.equals(mode)) {
            System.getLogger(PayloadProtection.class.getName()).log(System.Logger.Level.WARNING,
                    "Temporal payload protection is OFF ({0}={1}): workflow history holds cleartext. Development only.",
                    ENV_MODE, mode);
            return DefaultDataConverter.newDefaultInstance();
        }
        throw new IllegalStateException("Temporal payload protection needs " + PayloadKeyRing.ENV_FILE
                + " (a payload key ring); set " + ENV_MODE + "=" + DISABLED_FOR_DEVELOPMENT + " only for local development");
    }
}
