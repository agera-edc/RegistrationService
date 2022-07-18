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

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;
import org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime;
import org.eclipse.dataspaceconnector.iam.did.resolution.DidResolverRegistryImpl;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.iam.did.web.resolution.WebDidResolver;
import org.eclipse.dataspaceconnector.registration.client.ApiClientFactory;
import org.eclipse.dataspaceconnector.registration.client.api.RegistryApi;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "registration-service-cli", mixinStandardHelpOptions = true,
        description = "Client utility for MVD registration service.",
        subcommands = {
                ParticipantsCommand.class
        })
public class RegistrationServiceCli {


    @CommandLine.Option(names = "-s", required = true, description = "Registration service URL", defaultValue = "http://localhost:8181/api")
    String service;

    @CommandLine.Option(names = "-d", required = true, description = "DID document URI", defaultValue = "")
    String didURI;

    RegistryApi registryApiClient;

    DidResolver resolver;

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
        OkHttpClient httpClient = new OkHttpClient();
        resolver = new WebDidResolver(httpClient, new ObjectMapper(), new ConsoleMonitor());
        if (didURI.isEmpty()) return;
        Result<DidDocument> didDocument = resolver.resolve(didURI);
        Optional<String> enrollmentUrl = didDocument.getContent().getService().stream().filter(service -> service.getType().equals("EnrollmentUrl")).map(Service::getServiceEndpoint).findFirst();
        if (enrollmentUrl.isEmpty()) {
            throw new RuntimeException("No enrollment API in the did document.");
        }

        String url = enrollmentUrl.get();
        System.out.println(url);
//        var apiClient = ApiClientFactory.createApiClient(url);
//        registryApiClient = new RegistryApi(apiClient);
    }
}

/*
var resolverRegistry = new DidResolverRegistryImpl();
            Result<DidDocument> didDocument = resolverRegistry.resolve(didURI);
            Optional<String> enrollmentUrl = didDocument.getContent().getService().stream().filter(service -> service.getType().equals("EnrollmentUrl")).map(Service::getServiceEndpoint).findFirst();
            if (enrollmentUrl.isEmpty()) {
                throw new RuntimeException("No enrollment API in the did document.");
            }

            String url = enrollmentUrl.get();
            System.out.println(url);
            var apiClient = ApiClientFactory.createApiClient(url);
            registryApiClient = new RegistryApi(apiClient);
 */
