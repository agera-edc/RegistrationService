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
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.dataspaceconnector.iam.did.crypto.credentials.VerifiableCredentialFactory;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.exception.AuthenticationFailedException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.List;
import java.util.Objects;

/**
 * Intercepts all requests sent to this resource and authenticates them using DID Web.
 *
 * The resulting SecurityContext can be injected into REST Controllers as {@code @Context SecurityContext sec} parameter.
 */
public class DidJwtAuthenticationFilter implements ContainerRequestFilter {

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

        String credential = getCredential(headers);
        SignedJWT jwt = parseJsonWebToken(credential);
        String issuer = getIssuerClaim(jwt);
        verifyTokenSignature(jwt, issuer);

        monitor.debug("Valid JWT");

        requestContext.setSecurityContext(new DidSecurityContext(issuer));
    }


    @NotNull
    private SignedJWT parseJsonWebToken(String credential) {
        try {
            return SignedJWT.parse(credential);
        } catch (ParseException e) {
            throw authenticationFailure("Invalid JWT (parse error)", List.of(e.getMessage()));
        }
    }

    private String getIssuerClaim(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet().getIssuer();
        } catch (ParseException e) {
            throw authenticationFailure("Invalid JWT (parse error)", List.of(e.getMessage()));
        }
    }


    private String getCredential(MultivaluedMap<String, String> headers) {
        var authHeader = headers.getFirst("Authorization");
        if (authHeader == null) {
            throw authenticationFailure("Cannot authenticate request", List.of("Missing Authorization header"));
        }
        var separatedAuthHeader = authHeader.split(" ");

        if (separatedAuthHeader.length != 2 || !"Bearer".equals(separatedAuthHeader[0])) {
            throw authenticationFailure("Cannot authenticate request", List.of("Authorization header value is not a valid Bearer token"));
        }

        return separatedAuthHeader[1];
    }

    private void verifyTokenSignature(SignedJWT jwt, String issuer) {
        var publicKey = didPublicKeyResolver.resolvePublicKey(issuer);

        if (publicKey.failed()) {
            throw authenticationFailure("Failed obtaining public key for DID: " + issuer, publicKey.getFailureMessages());
        }

        var verificationResult = VerifiableCredentialFactory.verify(jwt, publicKey.getContent(), audience);
        if (verificationResult.failed()) {
            throw authenticationFailure("Invalid JWT (verification error)", verificationResult.getFailureMessages());
        }
    }

    @NotNull
    private AuthenticationFailedException authenticationFailure(String message, List<String> failureMessages) {
        failureMessages.forEach(m -> monitor.debug(() -> message + ". " + m));
        return new AuthenticationFailedException(message + ". " + String.join(". ", failureMessages));
    }
}
