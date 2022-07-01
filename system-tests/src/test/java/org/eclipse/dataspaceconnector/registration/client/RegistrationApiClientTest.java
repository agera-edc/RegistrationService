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

import com.github.javafaker.Faker;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.registration.client.api.RegistryApi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.registration.client.TestUtils.DID_WEB;
import static org.eclipse.dataspaceconnector.registration.client.TestUtils.PRIVATE_KEY_FILE;

@IntegrationTest
public class RegistrationApiClientTest {
    static final String API_URL = "http://localhost:8182/authority";
    static final Faker FAKER = new Faker();
    static RegistryApi api;

    String participantUrl = FAKER.internet().url();

    @BeforeAll
    static void setUpClass() throws Exception {
        var ecKey = JWK.parseFromPEMEncodedObjects(Files.readString(PRIVATE_KEY_FILE)).toECKey();
        var privateKeyWrapper = new EcPrivateKeyWrapper(ecKey);

        var apiClient = ApiClientFactory.createApiClient(API_URL, DID_WEB, privateKeyWrapper);
        api = new RegistryApi(apiClient);
    }

    @Test
    void listParticipants() {

        assertThat(api.listParticipants())
                .noneSatisfy(p -> assertThat(p.getUrl()).isEqualTo(participantUrl));

        api.addParticipant(participantUrl);

        assertThat(api.listParticipants())
                .anySatisfy(p -> assertThat(p.getUrl()).isEqualTo(participantUrl));
    }
}
