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

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.registration.DataspacePolicy;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultParticipantVerifierTest {
    static final Faker FAKER = new Faker();

    String participantDid = FAKER.internet().url();
    PolicyEngine policyEngine = mock(PolicyEngine.class);
    Policy policy = mock(Policy.class);
    Policy policyResult = mock(Policy.class);
    DataspacePolicy dataspacePolicy = mock(DataspacePolicy.class);
    DefaultParticipantVerifier service = new DefaultParticipantVerifier(policyEngine, dataspacePolicy);

    @BeforeEach
    void beforeEach() {
        when(dataspacePolicy.get())
                .thenReturn(policy);
        when(policyEngine.evaluate(any(), any(), any()))
                .thenReturn(Result.success(policyResult));
    }

    @Test
    void verifyCredentials_createsMembershipCredential() {
        var result = service.verifyCredentials(participantDid);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isTrue();
    }
}