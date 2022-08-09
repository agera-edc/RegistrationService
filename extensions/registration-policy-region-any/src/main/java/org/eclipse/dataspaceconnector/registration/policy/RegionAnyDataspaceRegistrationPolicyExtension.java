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

package org.eclipse.dataspaceconnector.registration.policy;

import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.registration.DataspaceRegistrationPolicy;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.policy.RuleBindingRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;

import static org.eclipse.dataspaceconnector.registration.DataspaceRegistrationPolicy.PARTICIPANT_REGISTRATION_SCOPE;

/**
 * EDC extension to create a policy that accepts participants with any (non-empty) region credential.
 */
public class RegionAnyDataspaceRegistrationPolicyExtension implements ServiceExtension {
    private static final String REGION = "region";
    public static final String RULE_TYPE_REGION = REGION;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    @Inject
    private PolicyEngine policyEngine;

    /**
     * Performs the plumbing for registering a {@link Policy} and an evaluation function that checks that "region EQ
     * eu". The rule type {@link RegionAnyDataspaceRegistrationPolicyExtension#RULE_TYPE_REGION} is bound to the scope
     * {@link DataspaceRegistrationPolicy#PARTICIPANT_REGISTRATION_SCOPE}.
     */
    @Provider
    public DataspaceRegistrationPolicy createDataspaceRegistrationPolicy() {

        var regionConstraint = AtomicConstraint.Builder.newInstance().leftExpression(new LiteralExpression(REGION))
                .operator(Operator.NEQ)
                .rightExpression(new LiteralExpression("")).build();
        var regionPermission = Permission.Builder.newInstance().constraint(regionConstraint).build();
        var p = Policy.Builder.newInstance()
                .permission(regionPermission).build();
        ruleBindingRegistry.bind(RULE_TYPE_REGION, PARTICIPANT_REGISTRATION_SCOPE);
        policyEngine.registerFunction(PARTICIPANT_REGISTRATION_SCOPE, Permission.class, RULE_TYPE_REGION, (operator, rightValue, rule, context) -> Operator.NEQ == operator && "".equalsIgnoreCase(rightValue.toString()));
        return new DataspaceRegistrationPolicy(p);
    }
}
