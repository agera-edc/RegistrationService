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

package org.eclipse.dataspaceconnector.registration.cli;

import com.github.javafaker.Faker;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.crypto.credentials.VerifiableCredentialFactory;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPublicKeyWrapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonWebSignatureHeaderInterceptorTest {

    static final Faker FAKER = new Faker();

    @Test
    void accept() throws Exception {
        var cli = new RegistrationServiceCli();
        cli.privateKey = Path.of("src/test/resources/private_p256.pem");
        var publicKey = Files.readString(Path.of("src/test/resources/public_p256.pem"));
        String clientDid = FAKER.lorem().sentence();
        cli.clientDid = clientDid;
        String targetUrl = randomUrl();
        cli.service = targetUrl;
        var interceptor = new JsonWebSignatureHeaderInterceptor(cli);

        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(randomUrl()));
        interceptor.accept(builder);
        var httpHeaders = builder.build().headers();
        assertThat(httpHeaders.map())
                .containsOnlyKeys("Authorization");
        List<String> authorizationHeaders = httpHeaders.allValues("Authorization");
        assertThat(authorizationHeaders).hasSize(1);
        var authorizationHeader = authorizationHeaders.get(0);
        var authHeaderParts = authorizationHeader.split(" ", 2);
        assertThat(authHeaderParts[0]).isEqualTo("Bearer");
        var jwt = SignedJWT.parse(authHeaderParts[1]);
        var key = new EcPublicKeyWrapper((ECKey) ECKey.parseFromPEMEncodedObjects(publicKey));
        var verified = VerifiableCredentialFactory.verify(jwt, key, targetUrl);
        assertThat(verified).isTrue();
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo(clientDid);
    }

    static String randomUrl() {
        return "http://" + FAKER.internet().url();
    }
}