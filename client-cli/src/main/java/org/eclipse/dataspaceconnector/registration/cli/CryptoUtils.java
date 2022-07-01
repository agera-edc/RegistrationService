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
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;

public class CryptoUtils {

    public static PrivateKeyWrapper parseFromPemEncodedObjects(String pemEncodedObjects) {
        try {
            var jwk = JWK.parseFromPEMEncodedObjects(pemEncodedObjects);
            if (!jwk.isPrivate()) {
                throw new IllegalArgumentException("Missing private key");
            }
            if (jwk.getKeyType() == KeyType.EC) {
                return new EcPrivateKeyWrapper(jwk.toECKey());
            }
            throw new IllegalArgumentException("Unsupported key type: " + jwk.getKeyType());
        } catch (JOSEException e) {
            throw new IllegalArgumentException("Key parsing failed: " + e);
        }
    }
}
