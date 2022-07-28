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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.eclipse.dataspaceconnector.registration.client.ApiException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(name = "status", description = "Get participant status")
class ParticipantStatusCommand implements Callable<Integer> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @ParentCommand
    private ParticipantsCommand command;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        try {
            var out = spec.commandLine().getOut();
            var response = command.cli.registryApiClient.getParticipantStatus();
            MAPPER.writeValue(out, response);
            out.println();
            return 0;
        } catch (ApiException ex) {
            throw new CliException("Participant Not Found.", ex);
        }
    }
}
