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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.registration.authority.model.Participant;
import org.eclipse.dataspaceconnector.registration.dto.ParticipantDto;
import org.eclipse.dataspaceconnector.spi.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.eclipse.dataspaceconnector.registration.auth.DidJwtAuthenticationFilter.CALLER_DID_HEADER;


/**
 * Registration Service API controller to manage dataspace participants.
 */
@Tag(name = "Registry")
@Produces({"application/json"})
@Consumes({"application/json"})
@Path("/registry")
public class RegistrationApiController {

    /**
     * A IDS URL (this will be removed in https://github.com/agera-edc/MinimumViableDataspace/issues/174)
     */
    private static final String TEMPORARY_IDS_URL_HEADER = "IdsUrl";

    private final RegistrationService service;
    private final DtoTransformerRegistry transformerRegistry;

    /**
     * Constructs an instance of {@link RegistrationApiController}
     *
     * @param service service handling the registration service logic.
     */
    public RegistrationApiController(RegistrationService service, DtoTransformerRegistry transformerRegistry) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
    }

    @GET
    @Path("{did}")
    @Operation(description = "Gets a participant by DID.")
    @ApiResponse(description = "Dataspace participant.")
    public ParticipantDto getParticipant(@PathParam("did") String did) {

        return Optional.of(did)
                .map(s -> service.findByDid(did))
                .map(participant -> transformerRegistry.transform(participant, ParticipantDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(Participant.class, did));
    }

    @Path("/participants")
    @GET
    @Operation(description = "Gets all dataspace participants.")
    @ApiResponse(description = "Dataspace participants.")
    public List<ParticipantDto> listParticipants() {

        return service.listParticipants().stream()
                .map(participant -> transformerRegistry.transform(participant, ParticipantDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(Collectors.toList());
    }

    @Path("/participant")
    @Operation(description = "Asynchronously request to add a dataspace participant.")
    @ApiResponse(responseCode = "204", description = "No content")
    @POST
    public void addParticipant(
            @HeaderParam(TEMPORARY_IDS_URL_HEADER) String idsUrl,
            @Context HttpHeaders headers) {
        var issuer = Objects.requireNonNull(headers.getHeaderString(CALLER_DID_HEADER));

        service.addParticipant(issuer, idsUrl);
    }
}
