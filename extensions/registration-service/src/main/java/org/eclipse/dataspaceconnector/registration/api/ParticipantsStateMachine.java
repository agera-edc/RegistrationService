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

import java.util.function.Function;

import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.AUTHORIZING;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.ONBOARDING_INITIATED;

public class ParticipantsStateMachine {

    private final ParticipantStore store;
    private final CredentialsVerifier credentialsVerifier;
    private final StateMachine stateMachine;

    public ParticipantsStateMachine(ParticipantStore service, CredentialsVerifier credentialsVerifier,
                                    ExecutorInstrumentation executorInstrumentation, Monitor monitor) {
        this.store = service;
        this.credentialsVerifier = credentialsVerifier;
        WaitStrategy waitStrategy = () -> 5000L;
        stateMachine = StateMachine.Builder.newInstance("registration-service", monitor, executorInstrumentation, waitStrategy)
                .processor(processNegotiationsInState(ONBOARDING_INITIATED, this::processOnboardingInitiated))
                .processor(processNegotiationsInState(AUTHORIZING, this::processAuthorizing))
                .build();
    }

    private Boolean processOnboardingInitiated(Participant participant) {
        participant.transitionAuthorizing();
        store.save(participant);
        return true;
    }

    private Boolean processAuthorizing(Participant participant) {
        var verificationResult = credentialsVerifier.verifyCredentials();
        if (verificationResult) {
            participant.transitionAuthorized();
        } else {
            participant.transitionDenied();
        }
        store.save(participant);
        return true;
    }


    public void start() {
        stateMachine.start();
    }

    public void stop() {
        stateMachine.stop();
    }

    private StateProcessorImpl<Participant> processNegotiationsInState(ParticipantStatus status, Function<Participant, Boolean> function) {
        return new StateProcessorImpl<>(() -> store.listParticipantsWithStatus(status), function);
    }
}
