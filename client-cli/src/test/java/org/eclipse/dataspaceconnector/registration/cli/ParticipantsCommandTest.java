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

package org.eclipse.dataspaceconnector.registration.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.registration.client.api.RegistryApi;
import org.eclipse.dataspaceconnector.registration.client.models.Participant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.registration.cli.TestUtils.createParticipant;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParticipantsCommandTest {

    static final Faker FAKER = new Faker();
    static final ObjectMapper MAPPER = new ObjectMapper();

    Participant participant1 = createParticipant();
    Participant participant2 = createParticipant();
    String serverUrl = FAKER.internet().url();
    String idsUrl = FAKER.internet().url();

    RegistrationServiceCli app = new RegistrationServiceCli();
    CommandLine cmd = new CommandLine(app);
    StringWriter sw = new StringWriter();

    @BeforeEach
    void setUp() {
        app.registryApiClient = mock(RegistryApi.class);
        cmd.setOut(new PrintWriter(sw));
    }

    @Test
    void list() throws Exception {
        var participants = List.of(this.participant1, participant2);
        when(app.registryApiClient.listParticipants())
                .thenReturn(participants);

        var exitCode = executeParticipantsAdd();
        assertThat(exitCode).isEqualTo(0);
        assertThat(serverUrl).isEqualTo(app.service);

        var parsedResult = MAPPER.readValue(sw.toString(), new TypeReference<List<Participant>>() {
        });
        assertThat(parsedResult)
                .usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(participants);
    }

    @Test
    void add() {
        var exitCode = executeParticipantsAdd(idsUrl);

        assertThat(exitCode).isEqualTo(0);
        assertThat(serverUrl).isEqualTo(app.service);
        verify(app.registryApiClient).addParticipant(idsUrl);
    }

    private int executeParticipantsAdd(String idsUrl) {
        return cmd.execute(
                "-d", "did:web:did-server:test-authority",
                "-k", "../rest-client/src/test/resources/private_p256.pem",
                "-s", serverUrl,
                "participants", "add",
                "--ids-url", idsUrl);
    }

    private int executeParticipantsAdd() {
        return cmd.execute(
                "-d", "did:web:did-server:test-authority",
                "-k", "../rest-client/src/test/resources/private_p256.pem",
                "-s", serverUrl,
                "participants", "list");
    }
}