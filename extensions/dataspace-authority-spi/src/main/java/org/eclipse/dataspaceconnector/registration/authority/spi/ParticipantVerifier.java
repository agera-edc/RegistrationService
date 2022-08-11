/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.registration.authority.spi;

import org.eclipse.dataspaceconnector.spi.response.StatusResult;

/**
 * Verifier that fetches and evaluate verifiable credentials to
 * authorize dataspace membership.
 */
public interface ParticipantVerifier {
    /**
     * Apply dataspace membership policy to evaluate whether a participant may be onboarded.
     *
     * @param did The putative participant's DID URL. Used to resolve participant Identity Hub and retrieve Verifiable Credentials.
     * @return a successful {@link StatusResult} if the policy could be evaluated.
     * The payload then contains the policy evaluation result.
     * A failure result indicates that the policy could not be evaluated,
     * for example, because Verifiable Credentials could not be retrieved from the Identity Hub.
     */
    StatusResult<Boolean> applyOnboardingPolicy(String did);
}
