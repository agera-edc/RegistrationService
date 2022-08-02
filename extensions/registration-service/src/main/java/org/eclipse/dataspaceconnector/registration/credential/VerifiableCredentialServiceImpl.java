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

package org.eclipse.dataspaceconnector.registration.credential;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClient;
import org.eclipse.dataspaceconnector.identityhub.credentials.VerifiableCredentialsJwtService;
import org.eclipse.dataspaceconnector.identityhub.credentials.model.VerifiableCredential;
import org.eclipse.dataspaceconnector.registration.authority.model.Participant;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.Map;
import java.util.UUID;

public class VerifiableCredentialServiceImpl implements VerifiableCredentialService {
    private static final String IDENTITY_HUB_SERVICE_TYPE = "IdentityHub";

    private final Monitor monitor;
    private final VerifiableCredentialsJwtService jwtService;
    private final PrivateKeyWrapper privateKeyWrapper;
    private final String issuer;
    private final DidResolverRegistry resolverRegistry;
    private final IdentityHubClient identityHubClient;

    public VerifiableCredentialServiceImpl(Monitor monitor, VerifiableCredentialsJwtService jwtService, PrivateKeyWrapper privateKeyWrapper, String issuer, DidResolverRegistry resolverRegistry, IdentityHubClient identityHubClient) {
        this.monitor = monitor;
        this.jwtService = jwtService;
        this.privateKeyWrapper = privateKeyWrapper;
        this.issuer = issuer;
        this.resolverRegistry = resolverRegistry;
        this.identityHubClient = identityHubClient;
    }

    @Override
    public void generateVerifiableCredential(Participant participant) {
        VerifiableCredential vc = VerifiableCredential.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .credentialSubject(Map.of("memberOfDataspace", "true"))
                .build();

        var subject = participant.getDid();
        SignedJWT jwt;
        try {
            jwt = jwtService.buildSignedJwt(vc, issuer, subject, privateKeyWrapper);
        } catch (Exception e) {
            throw new EdcException(e);
        }
        monitor.info("Created dataspace membership VC for " + subject);

        var did = participant.getDid();
        var didDocument = resolverRegistry.resolve(did);
        if (didDocument.failed()) {
            throw new EdcException("Failed to resolve DID " + did);
        }
        var hubBaseUrl = getIdentityHubBaseUrl(didDocument.getContent());
        if (hubBaseUrl.failed()) {
            throw new EdcException("Failed to resolve IH URL from DID document for " + did);
        }

        var addVcResult = identityHubClient.addVerifiableCredential(hubBaseUrl.getContent(), jwt);
        if (addVcResult.failed()) {
            throw new EdcException("Failed to resolve DID");
        }
    }

    private Result<String> getIdentityHubBaseUrl(DidDocument didDocument) {
        var hubBaseUrl = didDocument
                .getService()
                .stream()
                .filter(s -> s.getType().equals(IDENTITY_HUB_SERVICE_TYPE))
                .findFirst();

        return hubBaseUrl.map(u -> Result.success(u.getServiceEndpoint()))
                .orElse(Result.failure("Failed getting Identity Hub URL"));
    }
}
