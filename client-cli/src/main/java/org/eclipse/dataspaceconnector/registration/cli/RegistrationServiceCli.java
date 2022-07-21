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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.did.web.resolution.WebDidResolver;
import org.eclipse.dataspaceconnector.registration.client.api.RegistryApi;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;

@Command(name = "registration-service-cli", mixinStandardHelpOptions = true,
        description = "Client utility for MVD registration service.",
        subcommands = {
                ParticipantsCommand.class
        })
public class RegistrationServiceCli {

    private static final ObjectMapper MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static final String USE_HTTPS_SCHEME = "did.web.use.https";

    @Deprecated
    @CommandLine.Option(names = "-s", description = "Registration service URL. Deprecated. Use -dd instead.", defaultValue = "http://localhost:8182/authority")
    String service;

    @CommandLine.Option(names = "-dd", required = false, description = "Dataspace Authority DID.", defaultValue = "")
    String dataspaceDid;

    @CommandLine.Option(names = "-cd", required = true, description = "Client DID.")
    String clientDid;

    @CommandLine.Option(names = "-k", required = true, description = "File containing the private key in PEM format")
    Path privateKeyFile;

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
        String privateKeyData;
        try {
            privateKeyData = Files.readString(privateKeyFile);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file " + privateKeyFile, e);
        }

        // temporary to preserve the backwards compatibility
        if (dataspaceDid.isEmpty()) {
            var apiClient = ClientUtils.createApiClient(service, clientDid, privateKeyData);
            this.registryApiClient = new RegistryApi(apiClient);
            return;
        }
        var didWebResolver = new WebDidResolver(new OkHttpClient(), useHttpsScheme(), MAPPER, new ConsoleMonitor());
        var urlResolver = new RegistrationUrlResolver(didWebResolver);
        String registrationUrl = urlResolver.resolveUrl(dataspaceDid).orElseThrow(() -> new RuntimeException("Error resolving the registration url."));

        registryApiClient = new RegistryApi(ClientUtils.createApiClient(registrationUrl, clientDid, privateKeyData));
    }

    private boolean useHttpsScheme() {
        return propOrEnv(USE_HTTPS_SCHEME, "true").equals("true");
    }
}
