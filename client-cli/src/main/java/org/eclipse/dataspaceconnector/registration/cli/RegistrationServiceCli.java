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

import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.iam.did.web.resolution.WebDidResolver;
import org.eclipse.dataspaceconnector.registration.client.ApiClient;
import org.eclipse.dataspaceconnector.registration.client.ApiClientFactory;
import org.eclipse.dataspaceconnector.registration.client.api.RegistryApi;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
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

    private static final ObjectMapper MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @CommandLine.Option(names = "-s", required = true, description = "Registration service URL", defaultValue = "http://localhost:8182/authority")
    String service;

    @CommandLine.Option(names = "-did", required = true, description = "DID document URI", defaultValue = "")
    String didURI;

    @CommandLine.Option(names = "-d", required = true, description = "Client DID")
    String clientDid;

    @CommandLine.Option(names = "-k", required = true, description = "File containing the private key in PEM format")
    Path privateKeyFile;

    RegistryApi registryApiClient;

    EnrollmentUrlResolver resolver;

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
        if (init()) // custom initialization to be done before executing any command or subcommand
            return new CommandLine.RunLast().execute(parseResult);
        return 1;
    }

    private boolean init() {
        String privateKeyData;
        try {
            privateKeyData = Files.readString(privateKeyFile);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file " + privateKeyFile, e);
        }

        // temporary to preserve the backwarts compatibility
        if (didURI.isEmpty()) {
            var apiClient = ClientUtils.createApiClient(service, clientDid, privateKeyData);
            this.registryApiClient = new RegistryApi(apiClient);
            return true;
        }
        var webResolver = new WebDidResolver(new OkHttpClient(), MAPPER, new ConsoleMonitor());
        resolver = new EnrollmentUrlResolver(webResolver);
        Optional<String> enrollmentUrl = resolver.resolveUrl(didURI);
        Optional<RegistryApi> registryApi = enrollmentUrl.map((String baseUri) -> ApiClientFactory.createApiClient(baseUri, clientDid, privateKeyData)).map(RegistryApi::new);
        if (registryApi.isEmpty()) {
            return false;
        }
        registryApiClient = registryApi.get();
        return true;

    }
}
