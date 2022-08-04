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

import com.github.javafaker.Faker;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClient;
import org.eclipse.dataspaceconnector.identityhub.credentials.VerifiableCredentialsJwtService;
import org.eclipse.dataspaceconnector.identityhub.credentials.model.VerifiableCredential;
import org.eclipse.dataspaceconnector.registration.authority.model.Participant;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.dataspaceconnector.registration.TestUtils.createParticipant;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerifiableCredentialServiceImplTest {
    static final Faker FAKER = new Faker();
    static final String IDENTITY_HUB_TYPE = "IdentityHub";

    Monitor monitor = mock(Monitor.class);
    VerifiableCredentialsJwtService jwtService = mock(VerifiableCredentialsJwtService.class);
    PrivateKeyWrapper privateKeyWrapper = mock(PrivateKeyWrapper.class);
    String dataspaceDid = FAKER.internet().url();
    String participantDid = FAKER.internet().url();
    DidResolverRegistry resolverRegistry = mock(DidResolverRegistry.class);
    IdentityHubClient identityHubClient = mock(IdentityHubClient.class);
    SignedJWT jwt = mock(SignedJWT.class);
    String identityHubUrl = FAKER.internet().url();
    String failure = FAKER.lorem().sentence();
    VerifiableCredentialServiceImpl service = new VerifiableCredentialServiceImpl(monitor, jwtService, privateKeyWrapper, dataspaceDid, resolverRegistry, identityHubClient);
    Participant.Builder participantBuilder = createParticipant().did(participantDid);
    ArgumentCaptor<VerifiableCredential> vc = ArgumentCaptor.forClass(VerifiableCredential.class);

    @BeforeEach
    void beforeEach() throws Exception {
        when(resolverRegistry.resolve(participantDid))
                .thenReturn(Result.success(DidDocument.Builder.newInstance()
                        .service(List.of(new Service(FAKER.lorem().word(), IDENTITY_HUB_TYPE, identityHubUrl)))
                        .build()));
        when(jwtService.buildSignedJwt(any(), eq(dataspaceDid), eq(participantDid), eq(privateKeyWrapper)))
                .thenReturn(jwt);
        when(identityHubClient.addVerifiableCredential(identityHubUrl, jwt))
                .thenReturn(StatusResult.success());
    }

    @Test
    void publishVerifiableCredential_createsMembershipCredential() throws Exception {
        service.publishVerifiableCredential(participantBuilder.build());

        verify(jwtService).buildSignedJwt(vc.capture(), eq(dataspaceDid), eq(participantDid), eq(privateKeyWrapper));
        assertThat(vc.getValue().getId()).satisfies(i -> assertThat(UUID.fromString(i)).isNotNull());
        assertThat(vc.getValue().getCredentialSubject()).isEqualTo(Map.of("memberOfDataspace", dataspaceDid));
    }

    @Test
    void publishVerifiableCredential_pushesCredential() {
        service.publishVerifiableCredential(participantBuilder.build());

        verify(identityHubClient).addVerifiableCredential(identityHubUrl, jwt);
    }

    @Test
    void publishVerifiableCredential_whenDidNotResolved_throws() {
        when(resolverRegistry.resolve(participantDid))
                .thenReturn(Result.failure(failure));

        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() -> service.publishVerifiableCredential(participantBuilder.build()))
                .withMessage(format("Failed to resolve DID %s. %s", participantDid, failure));
    }

    @Test
    void publishVerifiableCredential_whenDidDocumentDoesNotContainHubUrl_throws() {
        when(resolverRegistry.resolve(participantDid))
                .thenReturn(Result.success(DidDocument.Builder.newInstance()
                        .service(List.of(new Service(FAKER.lorem().word(), FAKER.lorem().word(), identityHubUrl)))
                        .build()));

        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() -> service.publishVerifiableCredential(participantBuilder.build()))
                .withMessage(format("Failed to resolve Identity Hub URL from DID document for %s", participantDid));
    }

    @Test
    void publishVerifiableCredential_whenJwtCannotBeSigned_throws() throws Exception {
        when(jwtService.buildSignedJwt(any(), eq(dataspaceDid), eq(participantDid), eq(privateKeyWrapper)))
                .thenThrow(new JOSEException(failure));

        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() -> service.publishVerifiableCredential(participantBuilder.build()))
                .withMessage(format("%s: %s", JOSEException.class.getCanonicalName(), failure));
    }

    @Test
    void publishVerifiableCredential_whenPushToIdentityHubFails_throws() {
        when(identityHubClient.addVerifiableCredential(identityHubUrl, jwt))
                .thenReturn(StatusResult.failure(FAKER.options().option(ResponseStatus.class), failure));

        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() -> service.publishVerifiableCredential(participantBuilder.build()))
                .withMessage(format("Failed to send VC. %s", failure));
    }
}