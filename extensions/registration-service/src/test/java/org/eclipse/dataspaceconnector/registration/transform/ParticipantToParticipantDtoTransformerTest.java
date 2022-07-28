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

package org.eclipse.dataspaceconnector.registration.transform;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.eclipse.dataspaceconnector.registration.authority.dto.ParticipantDto;
import org.eclipse.dataspaceconnector.registration.authority.dto.ParticipantStatusDto;
import org.eclipse.dataspaceconnector.registration.authority.model.Participant;
import org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.registration.TestUtils.createParticipant;
import static org.mockito.Mockito.mock;

public class ParticipantToParticipantDtoTransformerTest {

    private final ParticipantToParticipantDtoTransformer transformer = new ParticipantToParticipantDtoTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isEqualTo(Participant.class);
        assertThat(transformer.getOutputType()).isEqualTo(ParticipantDto.class);
    }

    @ParameterizedTest
    @EnumSource(value = ParticipantStatus.class)
    void transform(ParticipantStatus status) {
        var context = mock(TransformerContext.class);

        var participant = createParticipant().status(status).build();
        var participantDto = transformer.transform(participant, context);
        var expectedDtoStatus = modelToDtoStatusMap().get(status);

        assertThat(expectedDtoStatus)
                .withFailMessage(format("Status %s not found in modelToDtoStatusMap", status))
                .isNotNull();
        assertThat(participantDto).isNotNull();
        // ignoring status field as it is mapped to specific status in DTO.
        assertThat(participantDto)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoredFields("status")
                        .build())
                .isEqualTo(participant);
        // comparing dto status
        assertThat(participantDto.getStatus()).isEqualTo(expectedDtoStatus);
    }

    /**
     * Map of ParticipantStatus & ParticipantStatusDto.
     * It describes what should be DTO status in respect of domain model status.
     */
    private Map<ParticipantStatus, ParticipantStatusDto> modelToDtoStatusMap() {
        var map = new HashMap<ParticipantStatus, ParticipantStatusDto>();
        map.put(ParticipantStatus.ONBOARDING_INITIATED, ParticipantStatusDto.AUTHORIZING);
        map.put(ParticipantStatus.AUTHORIZING, ParticipantStatusDto.AUTHORIZING);
        map.put(ParticipantStatus.AUTHORIZED, ParticipantStatusDto.AUTHORIZED);
        map.put(ParticipantStatus.DENIED, ParticipantStatusDto.DENIED);

        return map;
    }
}
