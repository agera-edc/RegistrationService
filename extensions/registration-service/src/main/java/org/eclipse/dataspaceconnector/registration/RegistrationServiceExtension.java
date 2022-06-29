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

package org.eclipse.dataspaceconnector.registration;

import org.eclipse.dataspaceconnector.registration.api.RegistrationApiController;
import org.eclipse.dataspaceconnector.registration.api.RegistrationService;
import org.eclipse.dataspaceconnector.registration.authority.DummyCredentialsVerifier;
import org.eclipse.dataspaceconnector.registration.authority.spi.CredentialsVerifier;
import org.eclipse.dataspaceconnector.registration.manager.ParticipantManager;
import org.eclipse.dataspaceconnector.registration.store.InMemoryParticipantStore;
import org.eclipse.dataspaceconnector.registration.store.spi.ParticipantStore;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * EDC extension to boot the services used by the Registration Service.
 */
public class RegistrationServiceExtension implements ServiceExtension {

    public static final String CONTEXT_ALIAS = "authority";

    @Inject
    private Monitor monitor;

    @Inject
    private WebService webService;

    @Inject
    private ParticipantStore participantStore;

    @Inject
    private CredentialsVerifier credentialsVerifier;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    private ParticipantManager participantManager;

    @Override
    public void initialize(ServiceExtensionContext context) {

        participantManager = new ParticipantManager(monitor, participantStore, credentialsVerifier, executorInstrumentation);

        var registrationService = new RegistrationService(monitor, participantStore);
        webService.registerResource(CONTEXT_ALIAS, new RegistrationApiController(registrationService));
    }

    @Override
    public void start() {
        participantManager.start();
    }

    @Override
    public void shutdown() {
        participantManager.stop();
    }

    @Provider(isDefault = true)
    public ParticipantStore participantStore() {
        return new InMemoryParticipantStore();
    }

    @Provider(isDefault = true)
    public CredentialsVerifier credentialsVerifier() {
        return new DummyCredentialsVerifier();
    }
}
