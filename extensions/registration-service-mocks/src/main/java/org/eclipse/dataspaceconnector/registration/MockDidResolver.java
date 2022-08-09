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

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

/**
 * internal mock did resolver for the "mock" method. Returns an empty {@link DidDocument} for every did in the format
 * "did:mock...."
 */
class MockDidResolver implements DidResolver {
    @Override
    public @NotNull String getMethod() {
        return "mock";
    }

    @Override
    public @NotNull Result<DidDocument> resolve(String didKey) {
        return Result.success(DidDocument.Builder.newInstance()
                .build());
    }
}
