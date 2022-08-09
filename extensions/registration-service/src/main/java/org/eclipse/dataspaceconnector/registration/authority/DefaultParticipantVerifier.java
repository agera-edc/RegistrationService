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

package org.eclipse.dataspaceconnector.registration.authority;

import org.eclipse.dataspaceconnector.registration.DataspacePolicy;
import org.eclipse.dataspaceconnector.registration.authority.spi.ParticipantVerifier;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.Collections;
import java.util.Map;

import static org.eclipse.dataspaceconnector.registration.DataspacePolicy.ONBOARDING_SCOPE;

public class DefaultParticipantVerifier implements ParticipantVerifier {
    private final PolicyEngine policyEngine;
    private final DataspacePolicy dataspacePolicy;

    public DefaultParticipantVerifier(PolicyEngine policyEngine, DataspacePolicy dataspacePolicy) {
        this.policyEngine = policyEngine;
        this.dataspacePolicy = dataspacePolicy;
    }

    @Override
    public StatusResult<Boolean> verifyCredentials(String participantDid) {
        var claimsResult = Result.success(Map.<String, Object>of("region", "eu")); // TODO retrieve real credentials

        if (claimsResult.failed()) {
            return StatusResult.failure(ResponseStatus.ERROR_RETRY, claimsResult.getFailureDetail());
        }
        var pa = new ParticipantAgent(claimsResult.getContent(), Collections.emptyMap());

        var evaluationResult = policyEngine.evaluate(ONBOARDING_SCOPE, dataspacePolicy.get(), pa);
        return StatusResult.success(evaluationResult.succeeded());
    }
}
