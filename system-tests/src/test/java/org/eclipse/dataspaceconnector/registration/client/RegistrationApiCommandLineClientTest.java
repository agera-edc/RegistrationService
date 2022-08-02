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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.registration.cli.RegistrationServiceCli;
import org.eclipse.dataspaceconnector.registration.client.models.Participant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpStatusCode;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.registration.client.TestUtils.DATASPACE_DID_WEB;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

public class RegistrationApiCommandLineClientTest {
    private static final int API_PORT = getFreePort();

    static final ObjectMapper MAPPER = new ObjectMapper();
    static Path privateKeyFile;
    String didWeb = "did:web:host.docker.internal%3A" + API_PORT;
    static ClientAndServer httpSourceClientAndServer;

    @BeforeAll
    static void setUpClass() throws Exception {
        privateKeyFile = Files.createTempFile("test", ".pem");
        privateKeyFile.toFile().deleteOnExit();
        Files.writeString(privateKeyFile, TestKeyData.PRIVATE_KEY_P256);
        httpSourceClientAndServer = startClientAndServer(API_PORT);

    }

    @AfterAll
    public static void tearDown() {
        stopQuietly(httpSourceClientAndServer);
    }

    @Test
    void listParticipants() throws Exception {

        var didDocument = new String(Objects.requireNonNull(RegistrationApiCommandLineClientTest.class.getClassLoader().getResourceAsStream("test-client/did.json")).readAllBytes());

        httpSourceClientAndServer.when(request().withPath("/.well-known/did.json"))
                .respond(response()
                        .withBody(didDocument)
                        .withStatusCode(HttpStatusCode.OK_200.code()));

        CommandLine cmd = RegistrationServiceCli.getCommandLine();

        assertThat(getParticipants(cmd, didWeb)).noneSatisfy(p -> assertThat(p.getDid()).isEqualTo(didWeb));

        var addCmdExitCode = cmd.execute(
                "-c", didWeb,
                "-d", DATASPACE_DID_WEB,
                "-k", privateKeyFile.toString(),
                "--http-scheme",
                "participants", "add");
        assertThat(addCmdExitCode).isEqualTo(0);
        assertThat(getParticipants(cmd, didWeb)).anySatisfy(p -> assertThat(p.getDid()).isEqualTo(didWeb));
    }

    private List<Participant> getParticipants(CommandLine cmd, String didWeb) throws JsonProcessingException {
        var writer = new StringWriter();
        cmd.setOut(new PrintWriter(writer));
        var listCmdExitCode = cmd.execute(
                "-c", didWeb,
                "-d", DATASPACE_DID_WEB,
                "-k", privateKeyFile.toString(),
                "--http-scheme",
                "participants", "list");
        assertThat(listCmdExitCode).isEqualTo(0);

        var output = writer.toString();
        return MAPPER.readValue(output, new TypeReference<>() {
        });
    }
}
