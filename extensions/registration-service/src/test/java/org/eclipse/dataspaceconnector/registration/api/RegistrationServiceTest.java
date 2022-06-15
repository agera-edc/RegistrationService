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

package org.eclipse.dataspaceconnector.registration.api;

import org.eclipse.dataspaceconnector.registration.authority.model.Participant;
import org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus;
import org.eclipse.dataspaceconnector.registration.authority.spi.CredentialsVerifier;
import org.eclipse.dataspaceconnector.registration.store.spi.ParticipantStore;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.registration.TestUtils.createParticipant;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.AUTHORIZED;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.AUTHORIZING;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.DENIED;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.ONBOARDING_INITIATED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {

    Monitor monitor = mock(Monitor.class);
    ParticipantStore participantStore = mock(ParticipantStore.class);
    CredentialsVerifier credentialsVerifier = mock(CredentialsVerifier.class);
    RegistrationService service = new RegistrationService(monitor, participantStore, credentialsVerifier, ExecutorInstrumentation.noop());
    Participant.Builder participantBuilder = createParticipant();
    ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);

    @Test
    void listParticipants_empty() {
        assertThat(service.listParticipants()).isEmpty();
    }

    @Test
    void listParticipants() {
        var participant = participantBuilder.build();
        when(participantStore.listParticipants()).thenReturn(List.of(participant));
        assertThat(service.listParticipants()).containsExactly(participant);
    }

    @Test
    void addParticipant() {
        var participant = participantBuilder.build();
        service.addParticipant(participant);
        verify(participantStore).save(participant);
    }

    @Test
    void advancesStateFromOnboardingInitiatedToAuthorizing() throws Exception {
        advancesState(ONBOARDING_INITIATED, AUTHORIZING);
    }

    @Test
    void advancesStateFromAuthorizingToAuthorized() throws Exception {
        when(credentialsVerifier.verifyCredentials()).thenReturn(true);
        advancesState(AUTHORIZING, AUTHORIZED);
    }

    @Test
    void advancesStateFromAuthorizingToDenied() throws Exception {
        when(credentialsVerifier.verifyCredentials()).thenReturn(false);
        advancesState(AUTHORIZING, DENIED);
    }

    @SuppressWarnings("unchecked")
    private void advancesState(ParticipantStatus startState, ParticipantStatus endState) throws Exception {
        var participant = participantBuilder.status(startState).build();
        when(participantStore.listParticipantsWithStatus(eq(startState)))
                .thenReturn(List.of(participant), List.of());
        var latch = new CountDownLatch(1);
        service.registerListener(new LatchListener(latch, endState));

        service.start();
        assertThat(latch.await(30, SECONDS)).isTrue();

        verify(participantStore).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(endState);
        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(ParticipantStatus.class)
                .isEqualTo(participant);
    }
}