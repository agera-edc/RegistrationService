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

import java.nio.file.Path;

public class TestUtils {
    private TestUtils() {
    }

    static final Path PRIVATE_KEY_FILE = Path.of("../rest-client/src/test/resources/private_p256.pem");

    static final String DID_WEB = "did:web:did-server:test-authority";
}
