/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client;

/** Supplies a current ToolGate user credential for a run's user. */
public interface UserCredentialProvider {

    /**
     * @throws ToolGateException {@link ToolGateException.Kind#FORBIDDEN} when the backend refuses the user (session ended,
     *                           not a member, not enabled in ToolGate), {@link ToolGateException.Kind#UNAVAILABLE} when it
     *                           cannot be reached
     */
    String credential(UserBinding binding);
}
