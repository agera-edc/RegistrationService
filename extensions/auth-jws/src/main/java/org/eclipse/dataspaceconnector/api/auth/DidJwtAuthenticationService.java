/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.api.auth;


import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.crypto.credentials.VerifiableCredentialFactory;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DidJwtAuthenticationService implements AuthenticationService {

    private final Monitor monitor;
    private final DidPublicKeyResolver didPublicKeyResolver;
    private final String audience;

    public DidJwtAuthenticationService(Monitor monitor, DidPublicKeyResolver didPublicKeyResolver, String audience) {
        this.monitor = monitor;
        this.didPublicKeyResolver = didPublicKeyResolver;
        this.audience = audience;
    }

    /**
     * Validates if the request is authenticated
     *
     * @param headers The headers, that contains the credential to be used, in this case the Basic-Auth credentials.
     * @return True if the credentials are correct.
     */
    @Override
    public boolean isAuthenticated(Map<String, List<String>> headers) {
        Objects.requireNonNull(headers, "headers");

        return headers.keySet().stream()
                .filter(k -> k.equalsIgnoreCase("Authorization"))
                .map(headers::get)
                .filter(list -> !list.isEmpty())
                .anyMatch(list -> list.stream()
                        .anyMatch(this::checkBasicAuthValid));
    }

    private boolean checkBasicAuthValid(String authHeader) {
        var separatedAuthHeader = authHeader.split(" ");

        if (separatedAuthHeader.length != 2 || !"Bearer".equals(separatedAuthHeader[0])) {
            monitor.debug("Authorization header value is not a valid Bearer token");
            return false;
        }

        var credential = separatedAuthHeader[1];

        SignedJWT jwt;
        String issuer;
        try {
            jwt = SignedJWT.parse(credential);
            issuer = jwt.getJWTClaimsSet().getIssuer();
        } catch (ParseException e) {
            monitor.debug("Invalid JWT (parse error)");
            return false;
        }

        var publicKey = didPublicKeyResolver.resolvePublicKey(issuer);

        if (publicKey.failed()) {
            publicKey.getFailureMessages().forEach(monitor::debug);
            return false;
        }

        var verified = VerifiableCredentialFactory.verify(jwt, publicKey.getContent(), audience);
        if (!verified) {
            monitor.debug("Invalid JWT (verification error)");
            return false;
        }

        monitor.debug("Valid JWT");
        return true;
    }
}
