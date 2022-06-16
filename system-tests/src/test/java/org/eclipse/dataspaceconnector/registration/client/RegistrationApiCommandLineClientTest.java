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

package org.eclipse.dataspaceconnector.registration.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.registration.cli.RegistrationServiceCli;
import org.eclipse.dataspaceconnector.registration.client.models.Participant;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.registration.client.IntegrationTestUtils.createParticipant;


@IntegrationTest
public class RegistrationApiCommandLineClientTest {
    static final String API_URL = "http://localhost:8181/api";

    //    ApiClient apiClient = ApiClientFactory.createApiClient(API_URL);
//    RegistryApi api = new RegistryApi(apiClient);
    Participant participant = createParticipant();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Disabled
    void listParticipants() throws Exception {
        CommandLine cmd = RegistrationServiceCli.getCommandLine();

        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("participants", "list");
        assertThat(exitCode).isEqualTo(0);

        String s = sw.toString();
        var participants = MAPPER.readValue(s, new TypeReference<List<Participant>>() {
        });
        assertThat(participants).hasSize(3);
        assertThat(participants).extracting(Participant::getName).contains("consumer-eu");
    }

    @Test
    void addParticipants() throws Exception {
        CommandLine cmd = RegistrationServiceCli.getCommandLine();

        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        var request = MAPPER.writeValueAsString(participant);

        int exitCode = cmd.execute("participants", "add", "--request=" + request);

        assertThat(exitCode).isEqualTo(0);

        String s = sw.toString();
        System.out.println(s);
    }
}
