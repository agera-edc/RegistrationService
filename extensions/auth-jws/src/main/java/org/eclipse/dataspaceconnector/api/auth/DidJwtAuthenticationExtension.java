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

package org.eclipse.dataspaceconnector.api.auth;

import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Objects;

import static java.lang.String.format;

public class DidJwtAuthenticationExtension implements ServiceExtension {

    @EdcSetting
    private static final String JWT_AUDIENCE_SETTING = "jwt.audience";

    @Inject
    private DidPublicKeyResolver didPublicKeyResolver;

    @Inject
    private Monitor monitor;

    @Provider
    public AuthenticationService authenticationService(ServiceExtensionContext context) {
        var didUrl = Objects.requireNonNull(context.getSetting(JWT_AUDIENCE_SETTING, null),
                () -> format("Missing setting %s", JWT_AUDIENCE_SETTING));
        return new DidJwtAuthenticationService(monitor, didPublicKeyResolver, didUrl);
    }
}
