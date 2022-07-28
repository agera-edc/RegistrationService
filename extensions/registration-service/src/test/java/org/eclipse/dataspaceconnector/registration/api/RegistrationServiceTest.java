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
import org.eclipse.dataspaceconnector.registration.authority.dto.ParticipantDto;
import org.eclipse.dataspaceconnector.registration.authority.dto.ParticipantStatusDto;
import org.eclipse.dataspaceconnector.registration.store.spi.ParticipantStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.registration.TestUtils.createParticipant;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.ONBOARDING_INITIATED;
import static org.eclipse.dataspaceconnector.spi.result.Result.success;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {
    static final Faker FAKER = new Faker();

    Monitor monitor = mock(Monitor.class);
    ParticipantStore participantStore = mock(ParticipantStore.class);
    DtoTransformerRegistry registry = mock(DtoTransformerRegistry.class);
    RegistrationService service = new RegistrationService(monitor, participantStore, registry);

    Participant.Builder participantBuilder = createParticipant();
    String did = FAKER.internet().url();
    String idsUrl = FAKER.internet().url();

    @Test
    void listParticipants_empty() {
        assertThat(service.listParticipants()).isEmpty();
    }

    @Test
    void listParticipants() {
        var participant = participantBuilder.build();
        var participantDto = participantDto();
        when(participantStore.listParticipants()).thenReturn(List.of(participant));
        when(registry.transform(participant, ParticipantDto.class))
                .thenReturn(success(participantDto));

        var result = service.listParticipants();

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(participantDto);

    }

    @Test
    void addParticipant() {
        service.addParticipant(did, idsUrl);

        var captor = ArgumentCaptor.forClass(Participant.class);
        verify(participantStore).save(captor.capture());
        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(Participant.Builder.newInstance()
                        .did(did)
                        .status(ONBOARDING_INITIATED)
                        .name(did)
                        .url(idsUrl)
                        .supportedProtocol("ids-multipart")
                        .build());
    }

    @Test
    void findByDid_nullResponse() {
        var participant = participantBuilder.build();
        when(participantStore.findByDid(participant.getDid()))
                .thenReturn(Optional.empty());

        assertThat(service.findByDid(participant.getDid())).isNull();
        verify(participantStore).findByDid(participant.getDid());
    }

    private ParticipantDto participantDto() {
        return ParticipantDto.Builder.newInstance()
                .name(FAKER.lorem().characters())
                .url(FAKER.internet().url())
                .did(format("did:web:%s", FAKER.internet().domainName()))
                .status(FAKER.options().option(ParticipantStatusDto.class))
                .supportedProtocol(FAKER.lorem().word())
                .build();

    }
}
