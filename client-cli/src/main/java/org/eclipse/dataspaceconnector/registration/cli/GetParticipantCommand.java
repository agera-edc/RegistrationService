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

import org.eclipse.dataspaceconnector.registration.client.ApiException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

import static org.eclipse.dataspaceconnector.registration.cli.RegistrationServiceCli.MAPPER;

@Command(name = "get", description = "Get participant by caller did")
class GetParticipantCommand implements Callable<Integer> {

    @ParentCommand
    private ParticipantsCommand command;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        try {
            var out = spec.commandLine().getOut();
            var response = command.cli.registryApiClient.getParticipant();
            MAPPER.writeValue(out, response);
            out.println();
            return 0;
        } catch (ApiException ex) {
            throw new CliException("Error occurred.", ex);
        }
    }
}
