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

package org.eclipse.dataspaceconnector.registration.client;

import org.eclipse.dataspaceconnector.iam.did.crypto.credentials.VerifiableCredentialFactory;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;

import java.net.http.HttpRequest;
import java.time.Clock;
import java.util.function.Consumer;

public class JsonWebSignatureHeaderInterceptor implements Consumer<HttpRequest.Builder> {

    private final String issuer;
    private final String audience;
    private final PrivateKeyWrapper privateKey;

    public JsonWebSignatureHeaderInterceptor(String issuer, String audience, PrivateKeyWrapper privateKey) {
        this.issuer = issuer;
        this.audience = audience;
        this.privateKey = privateKey;
    }

    @Override
    public void accept(HttpRequest.Builder b) {
        var token = VerifiableCredentialFactory.create(
                privateKey,
                issuer,
                audience,
                Clock.systemUTC()).serialize();
        b.header("Authorization", String.format("Bearer %s", token));
    }
}