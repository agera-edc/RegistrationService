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

package org.eclipse.dataspaceconnector.registration.authority;

import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClient;
import org.eclipse.dataspaceconnector.registration.authority.spi.CredentialsVerifier;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;

import static org.eclipse.dataspaceconnector.registration.utils.DidDocumentUtils.getIdentityHubBaseUrl;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Implementation of {@link CredentialsVerifier} that only retrieves verifiable credentials,
 * but performs no action on them. It authorizes any participant to onboard to the dataspace,
 * as long as its Identity Hub can be accessed.
 * <p>
 * This is meant as a starting point for implementing custom dataspace onboarding logic.
 */
public class DummyCredentialsVerifier implements CredentialsVerifier {

    private final IdentityHubClient identityHubClient;
    private final Monitor monitor;
    private final DidResolverRegistry resolverRegistry;

    public DummyCredentialsVerifier(Monitor monitor, DidResolverRegistry resolverRegistry, IdentityHubClient identityHubClient) {
        this.monitor = monitor;
        this.resolverRegistry = resolverRegistry;
        this.identityHubClient = identityHubClient;
    }

    @Override
    public StatusResult<Boolean> verifyCredentials(String did) {
        monitor.info("Get credentials VC for " + did);

        var didDocument = resolverRegistry.resolve(did);
        if (didDocument.failed()) {
            return StatusResult.failure(ERROR_RETRY, "Failed to resolve DID " + did + ". " + didDocument.getFailureDetail());
        }
        var hubBaseUrl = getIdentityHubBaseUrl(didDocument.getContent());
        if (hubBaseUrl.failed()) {
            return StatusResult.failure(FATAL_ERROR, "Failed to resolve Identity Hub URL from DID document for " + did);
        }

        var vcResult = identityHubClient.getVerifiableCredentials(hubBaseUrl.getContent());
        if (vcResult.failed()) {
            return StatusResult.failure(ERROR_RETRY, "Failed to retrieve VCs. " + vcResult.getFailureDetail());
        }

        monitor.info("Retrieved VCs for " + did);
        return StatusResult.success(true);
    }
}
