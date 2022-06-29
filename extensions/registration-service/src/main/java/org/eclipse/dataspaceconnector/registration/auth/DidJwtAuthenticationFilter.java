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

package org.eclipse.dataspaceconnector.registration.auth;


import com.nimbusds.jwt.SignedJWT;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import org.eclipse.dataspaceconnector.iam.did.crypto.credentials.VerifiableCredentialFactory;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.exception.AuthenticationFailedException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.text.ParseException;
import java.util.Objects;

/**
 * Intercepts all requests sent to this resource and authenticates them using DID Web.
 *
 * The resolved DID URL is injected as HTTP Header. The name of the header is defined by {@link #CALLER_DID_HEADER}.
 */

public class DidJwtAuthenticationFilter implements ContainerRequestFilter {
    public static final String CALLER_DID_HEADER = "CallerDid";

    private final Monitor monitor;
    private final DidPublicKeyResolver didPublicKeyResolver;
    private final String audience;

    public DidJwtAuthenticationFilter(Monitor monitor, DidPublicKeyResolver didPublicKeyResolver, String audience) {
        this.monitor = monitor;
        this.didPublicKeyResolver = didPublicKeyResolver;
        this.audience = audience;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var headers = requestContext.getHeaders();
        Objects.requireNonNull(headers, "headers");

        var authHeader = headers.getFirst("Authorization");
        var separatedAuthHeader = authHeader.split(" ");

        if (separatedAuthHeader.length != 2 || !"Bearer".equals(separatedAuthHeader[0])) {
            monitor.debug("Authorization header value is not a valid Bearer token");
            throw new AuthenticationFailedException();
        }

        var credential = separatedAuthHeader[1];

        SignedJWT jwt;
        String issuer;
        try {
            jwt = SignedJWT.parse(credential);
            issuer = jwt.getJWTClaimsSet().getIssuer();
        } catch (ParseException e) {
            monitor.debug("Invalid JWT (parse error)");
            throw new AuthenticationFailedException();
        }

        var publicKey = didPublicKeyResolver.resolvePublicKey(issuer);

        if (publicKey.failed()) {
            publicKey.getFailureMessages().forEach(message -> monitor.debug(() -> "Failed obtaining public key for DID: " + issuer + ". " + message));
            throw new AuthenticationFailedException();
        }

        var verificationResult = VerifiableCredentialFactory.verify(jwt, publicKey.getContent(), audience);
        if (verificationResult.failed()) {
            publicKey.getFailureMessages().forEach(message -> monitor.debug(() -> "Invalid JWT (verification error). " + message));
            throw new AuthenticationFailedException();
        }

        monitor.debug("Valid JWT");

        headers.putSingle(CALLER_DID_HEADER, issuer);
    }
}
