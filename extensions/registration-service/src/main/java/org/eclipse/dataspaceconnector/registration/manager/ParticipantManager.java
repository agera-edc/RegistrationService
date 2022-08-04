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

package org.eclipse.dataspaceconnector.registration.manager;

import org.eclipse.dataspaceconnector.common.statemachine.StateMachineManager;
import org.eclipse.dataspaceconnector.common.statemachine.StateProcessorImpl;
import org.eclipse.dataspaceconnector.registration.authority.model.Participant;
import org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus;
import org.eclipse.dataspaceconnector.registration.authority.spi.CredentialsVerifier;
import org.eclipse.dataspaceconnector.registration.store.spi.ParticipantStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.retry.WaitStrategy;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;

import java.util.function.Function;

import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.AUTHORIZED;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.AUTHORIZING;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.ONBOARDING_INITIATED;

/**
 * Manager for participant registration state machine.
 */
public class ParticipantManager {

    private final ParticipantStore participantStore;
    private final CredentialsVerifier credentialsVerifier;
    private final StateMachineManager stateMachineManager;

    public ParticipantManager(Monitor monitor, ParticipantStore participantStore, CredentialsVerifier credentialsVerifier, ExecutorInstrumentation executorInstrumentation) {
        this.participantStore = participantStore;
        this.credentialsVerifier = credentialsVerifier;

        // default wait five seconds
        WaitStrategy waitStrategy = () -> 5000L;

        // define state machine
        stateMachineManager = StateMachineManager.Builder.newInstance("registration-service", monitor, executorInstrumentation, waitStrategy)
                .processor(processParticipantsInState(ONBOARDING_INITIATED, this::processOnboardingInitiated))
                .processor(processParticipantsInState(AUTHORIZING, this::processAuthorizing))
                .processor(processParticipantsInState(AUTHORIZED, this::processAuthorized))
                .build();
    }

    /**
     * Start the participant manager state machine processor thread.
     */
    public void start() {
        stateMachineManager.start();
    }

    /**
     * Stop the participant manager state machine processor thread.
     */
    public void stop() {
        stateMachineManager.stop();
    }

    private Boolean processOnboardingInitiated(Participant participant) {
        participant.transitionAuthorizing();
        participantStore.save(participant);
        return true;
    }

    private Boolean processAuthorizing(Participant participant) {
        var credentialsValid = credentialsVerifier.verifyCredentials(participant.getDid());
        if (credentialsValid.failed()) {
            participant.transitionFailed(); // FIXME update ADR post https://github.com/agera-edc/RegistrationService/pull/19/files
        } else if (credentialsValid.getContent()) {
            participant.transitionAuthorized();
        } else {
            participant.transitionDenied();
        }
        participantStore.save(participant);
        return true;
    }

    private Boolean processAuthorized(Participant participant) {
        var result = Result.success(); // TODO publish VC
        if (result.succeeded()) {
            participant.transitionOnboarded();
        } else {
            participant.transitionFailed();
        }

        participantStore.save(participant);
        return true;
    }

    private StateProcessorImpl<Participant> processParticipantsInState(ParticipantStatus status, Function<Participant, Boolean> function) {
        return new StateProcessorImpl<>(() -> participantStore.listParticipantsWithStatus(status), function);
    }
}
