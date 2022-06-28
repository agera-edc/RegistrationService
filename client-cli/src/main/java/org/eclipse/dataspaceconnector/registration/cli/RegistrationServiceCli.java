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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.registration.client.ApiClientFactory;
import org.eclipse.dataspaceconnector.registration.client.api.RegistryApi;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Command(name = "registration-service-cli", mixinStandardHelpOptions = true,
        description = "Client utility for MVD registration service.",
        subcommands = {
                ParticipantsCommand.class
        })
public class RegistrationServiceCli {
    @CommandLine.Option(names = "-s", required = true, description = "Registration service URL", defaultValue = "http://localhost:8181/api")
    String service;

    @CommandLine.Option(names = "-d", required = true, description = "Client DID")
    String clientDid;

    @CommandLine.Option(names = "-k", required = true, description = "Private key")
    Path privateKey;

    RegistryApi registryApiClient;

    public static void main(String... args) {
        CommandLine commandLine = getCommandLine();
        var exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    public static CommandLine getCommandLine() {
        var command = new RegistrationServiceCli();
        return new CommandLine(command)
                .setExecutionStrategy(command::executionStrategy);
    }

    private int executionStrategy(CommandLine.ParseResult parseResult) {
        init(); // custom initialization to be done before executing any command or subcommand
        return new CommandLine.RunLast().execute(parseResult);
    }

    private void init() {
        EcPrivateKeyWrapper privateKeyWrapper;
        try {
            var ecKey = (ECKey) ECKey.parseFromPEMEncodedObjects(Files.readString(privateKey));
            privateKeyWrapper = new EcPrivateKeyWrapper(ecKey);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
        var apiClient = ApiClientFactory.createApiClient(service, clientDid, privateKeyWrapper);
        registryApiClient = new RegistryApi(apiClient);
    }
}
