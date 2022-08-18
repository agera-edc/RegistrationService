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

package org.eclipse.dataspaceconnector.registration.authority.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.telemetry.TraceCarrier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.AUTHORIZED;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.AUTHORIZING;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.DENIED;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.FAILED;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.ONBOARDED;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.ONBOARDING_INITIATED;

/**
 * Dataspace participant.
 */
@JsonDeserialize(builder = Participant.Builder.class)
public class Participant implements TraceCarrier {

    private String did;
    private ParticipantStatus status = ONBOARDING_INITIATED;
    private Map<String, String> traceContext = new HashMap<>();

    private Participant() {
    }

    public String getDid() {
        return did;
    }

    public ParticipantStatus getStatus() {
        return status;
    }

    @Override
    public Map<String, String> getTraceContext() {
        return unmodifiableMap(traceContext);
    }

    public void transitionAuthorizing() {
        transition(AUTHORIZING, ONBOARDING_INITIATED);
    }

    public void transitionAuthorized() {
        transition(AUTHORIZED, AUTHORIZING);
    }

    public void transitionDenied() {
        transition(DENIED, AUTHORIZING);
    }

    public void transitionOnboarded() {
        transition(ONBOARDED, AUTHORIZED);
    }

    public void transitionFailed() {
        transition(FAILED, AUTHORIZING, AUTHORIZED);
    }

    /**
     * Transition to a given end state from an allowed number of previous states.
     *
     * @param end    The desired state.
     * @param starts The allowed previous states.
     */
    private void transition(ParticipantStatus end, ParticipantStatus... starts) {
        if (Arrays.stream(starts).noneMatch(s -> s == status)) {
            throw new IllegalStateException(format("Cannot transition from state %s to %s", status, end));
        }
        status = end;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final Participant participant;

        private Builder() {
            participant = new Participant();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder did(String did) {
            participant.did = did;
            return this;
        }

        public Builder status(ParticipantStatus status) {
            participant.status = status;
            return this;
        }

        public Builder traceContext(Map<String, String> traceContext) {
            participant.traceContext = unmodifiableMap(traceContext);
            return this;
        }

        public Participant build() {
            Objects.requireNonNull(participant.did, "did");
            Objects.requireNonNull(participant.status, "status");
            return participant;
        }
    }
}
