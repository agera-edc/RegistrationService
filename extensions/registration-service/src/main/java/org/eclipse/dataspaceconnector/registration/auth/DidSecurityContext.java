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

package org.eclipse.dataspaceconnector.registration.auth;

import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;

class DidSecurityContext implements SecurityContext {
    private static final String DID_SCHEME = "DID";

    private final String issuer;

    DidSecurityContext(String issuer) {
        this.issuer = issuer;
    }

    @Override
    public Principal getUserPrincipal() {
        return new DidPrincipal(issuer);
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String getAuthenticationScheme() {
        return DID_SCHEME;
    }
}
