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

import org.eclipse.dataspaceconnector.common.statemachine.StateMachine;
import org.eclipse.dataspaceconnector.common.statemachine.StateProcessorImpl;
import org.eclipse.dataspaceconnector.registration.authority.model.Participant;
import org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus;
import org.eclipse.dataspaceconnector.registration.authority.spi.CredentialsVerifier;
import org.eclipse.dataspaceconnector.registration.store.spi.ParticipantStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.retry.WaitStrategy;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.AUTHORIZING;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.ONBOARDING_INITIATED;

/**
 * Registration service for dataspace participants.
 */
public class RegistrationService {

    private final Monitor monitor;
    private final ParticipantStore participantStore;
    private final CredentialsVerifier credentialsVerifier;
    private final StateMachine stateMachine;

    public RegistrationService(Monitor monitor, ParticipantStore participantStore, CredentialsVerifier credentialsVerifier, ExecutorInstrumentation executorInstrumentation) {
        this.monitor = monitor;
        this.participantStore = participantStore;
        this.credentialsVerifier = credentialsVerifier;

        // default wait five seconds
        WaitStrategy waitStrategy = () -> 5000L;
        stateMachine = StateMachine.Builder.newInstance("registration-service", monitor, executorInstrumentation, waitStrategy)
                .processor(processNegotiationsInState(ONBOARDING_INITIATED, this::processOnboardingInitiated))
                .processor(processNegotiationsInState(AUTHORIZING, this::processAuthorizing))
                .build();
    }

    /**
     * Lists all dataspace participants.
     *
     * @return list of dataspace participants.
     */
    public List<Participant> listParticipants() {
        monitor.info("List all participants of the dataspace.");
        return new ArrayList<>(participantStore.listParticipants());
    }

    public void addParticipant(Participant participant) {
        monitor.info("Adding a participant in the dataspace.");
        participantStore.save(participant);
    }

    public void start() {
        stateMachine.start();
    }

    public void stop() {
        stateMachine.stop();
    }

    private Boolean processOnboardingInitiated(Participant participant) {
        participant.transitionAuthorizing();
        participantStore.save(participant);
        return true;
    }

    private Boolean processAuthorizing(Participant participant) {
        var verificationResult = credentialsVerifier.verifyCredentials();
        if (verificationResult) {
            participant.transitionAuthorized();
            participantStore.save(participant);
        } else {
            participant.transitionDenied();
            participantStore.save(participant);
        }
        return true;
    }

    private StateProcessorImpl<Participant> processNegotiationsInState(ParticipantStatus status, Function<Participant, Boolean> function) {
        return new StateProcessorImpl<>(() -> participantStore.listParticipantsWithStatus(status), function);
    }
}
