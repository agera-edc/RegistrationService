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

import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.registration.authority.dto.ParticipantDto;
import org.eclipse.dataspaceconnector.registration.authority.dto.ParticipantStatusDto;
import org.eclipse.dataspaceconnector.registration.authority.model.Participant;
import org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;

public class ParticipantToParticipantDtoTransformer implements DtoTransformer<Participant, ParticipantDto> {

    @Override
    public Class<Participant> getInputType() {
        return Participant.class;
    }

    @Override
    public Class<ParticipantDto> getOutputType() {
        return ParticipantDto.class;
    }

    @Override
    public @Nullable ParticipantDto transform(@Nullable Participant participant, @NotNull TransformerContext context) {
        return ParticipantDto.Builder.newInstance()
                .did(participant.getDid())
                .name(participant.getName())
                .url(participant.getUrl())
                .supportedProtocols(participant.getSupportedProtocols())
                .status(mapToDtoStatus(participant.getStatus()))
                .build();
    }

    /**
     * Map domain model ParticipantStatus to DTO. This mapping is done to prevent leak of internal status.
     * DTO only exposes {@link ParticipantStatusDto#AUTHORIZING}, {@link ParticipantStatusDto#AUTHORIZED}
     * and {@link ParticipantStatusDto#DENIED} statues.
     *
     * @param status {@link ParticipantStatus}
     * @return {@link ParticipantStatusDto}
     */
    private ParticipantStatusDto mapToDtoStatus(ParticipantStatus status) {
        switch (status) {
            case ONBOARDING_INITIATED:
            case AUTHORIZING:
                return ParticipantStatusDto.AUTHORIZING;
            case AUTHORIZED:
                return ParticipantStatusDto.AUTHORIZED;
            case DENIED:
                return ParticipantStatusDto.DENIED;
            default:
                throw new EdcException(format("Unknown ParticipantStatus value: %s", status));
        }
    }
}
