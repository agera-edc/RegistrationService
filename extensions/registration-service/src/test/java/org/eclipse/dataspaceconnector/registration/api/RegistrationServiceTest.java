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

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.registration.authority.model.Participant;
import org.eclipse.dataspaceconnector.registration.model.ParticipantDto;
import org.eclipse.dataspaceconnector.registration.store.spi.ParticipantStore;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.registration.TestUtils.createParticipantDto;
import static org.eclipse.dataspaceconnector.registration.authority.TestUtils.createParticipant;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.ONBOARDING_INITIATED;
import static org.eclipse.dataspaceconnector.spi.result.Result.failure;
import static org.eclipse.dataspaceconnector.spi.result.Result.success;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {
    static final Faker FAKER = new Faker();

    Monitor monitor = mock(Monitor.class);
    ParticipantStore participantStore = mock(ParticipantStore.class);
    DtoTransformerRegistry dtoTransformerRegistry = mock(DtoTransformerRegistry.class);
    Telemetry telemetryMock = mock(Telemetry.class);
    RegistrationService service = new RegistrationService(monitor, participantStore, dtoTransformerRegistry, telemetryMock);


    Participant.Builder participantBuilder = createParticipant();
    ParticipantDto.Builder participantDtoBuilder = createParticipantDto();
    String did = FAKER.internet().url();

    @Test
    void listParticipants_empty() {
        when(participantStore.listParticipants()).thenReturn(List.of());

        assertThat(service.listParticipants()).isEmpty();
        verify(participantStore).listParticipants();
        verifyNoInteractions(dtoTransformerRegistry);
    }

    @Test
    void listParticipants() {
        var participant = participantBuilder.build();
        var participantDto = participantDtoBuilder.build();
        when(participantStore.listParticipants()).thenReturn(List.of(participant));
        when(dtoTransformerRegistry.transform(participant, ParticipantDto.class))
                .thenReturn(success(participantDto));

        var result = service.listParticipants();

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(participantDto);
        verify(participantStore).listParticipants();
        verify(dtoTransformerRegistry).transform(participant, ParticipantDto.class);
    }

    @Test
    void listParticipants_verifyResultFilter() {
        var participant1 = participantBuilder.build();
        var participant2 = createParticipant().build();
        var participantDto1 = participantDtoBuilder.build();

        when(participantStore.listParticipants()).thenReturn(List.of(participant1, participant2));
        // Transform for participant1 returns success.
        when(dtoTransformerRegistry.transform(participant1, ParticipantDto.class))
                .thenReturn(success(participantDto1));
        // Transform for participant2 returns failure.
        when(dtoTransformerRegistry.transform(participant2, ParticipantDto.class))
                .thenReturn(failure("dummy-failure-from-test"));

        var result = service.listParticipants();

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(participantDto1);
        verify(participantStore).listParticipants();
        verify(dtoTransformerRegistry).transform(participant1, ParticipantDto.class);
        verify(dtoTransformerRegistry).transform(participant2, ParticipantDto.class);
    }

    @Test
    void addParticipant() {
        var traceContext = getTraceContext();
        when(telemetryMock.getCurrentTraceContext()).thenReturn(traceContext);
        service.addParticipant(did);

        var captor = ArgumentCaptor.forClass(Participant.class);
        verify(participantStore).save(captor.capture());
        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(Participant.Builder.newInstance()
                        .did(did)
                        .status(ONBOARDING_INITIATED)
                        .traceContext(traceContext)
                        .build());
    }

    @Test
    void findByDid() {
        var participant = participantBuilder.build();
        var participantDto = participantDtoBuilder.build();
        when(participantStore.findByDid(participant.getDid()))
                .thenReturn(participant);
        when(dtoTransformerRegistry.transform(participant, ParticipantDto.class))
                .thenReturn(success(participantDto));

        assertThat(service.findByDid(participant.getDid())).isEqualTo(participantDto);

        verify(participantStore).findByDid(participant.getDid());
        verify(dtoTransformerRegistry).transform(participant, ParticipantDto.class);
    }

    @Test
    void findByDid_dtoTransformerFailure() {
        var participant = participantBuilder.build();
        when(participantStore.findByDid(participant.getDid()))
                .thenReturn(participant);
        when(dtoTransformerRegistry.transform(participant, ParticipantDto.class))
                .thenReturn(failure("dummy-failure-from-test"));

        assertThatThrownBy(() ->
                service.findByDid(participant.getDid())
        ).isInstanceOf(EdcException.class);

        verify(participantStore).findByDid(participant.getDid());
        verify(dtoTransformerRegistry).transform(participant, ParticipantDto.class);
    }

    @Test
    void findByDid_notFound() {
        var participant = participantBuilder.build();
        when(participantStore.findByDid(participant.getDid()))
                .thenReturn(null);

        assertThatThrownBy(() ->
                service.findByDid(participant.getDid())
        ).isInstanceOf(ObjectNotFoundException.class);

        verify(participantStore).findByDid(participant.getDid());
        verifyNoInteractions(dtoTransformerRegistry);
    }

    @NotNull
    private Map<String, String> getTraceContext() {
        return Map.of(FAKER.lorem().word(), FAKER.lorem().word(), FAKER.lorem().word(), FAKER.lorem().word());
    }

}
