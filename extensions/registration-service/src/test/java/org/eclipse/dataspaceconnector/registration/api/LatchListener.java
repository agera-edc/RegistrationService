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

package org.eclipse.dataspaceconnector.registration.api;

import org.eclipse.dataspaceconnector.registration.authority.model.Participant;
import org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus;

import java.util.concurrent.CountDownLatch;

import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.AUTHORIZED;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.AUTHORIZING;
import static org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus.DENIED;

class LatchListener implements ParticipantListener {
    private final CountDownLatch latch;
    private final ParticipantStatus status;

    LatchListener(CountDownLatch latch, ParticipantStatus status) {
        this.latch = latch;
        this.status = status;
    }

    @Override
    public void preAuthorizing(Participant participant) {
        countDownLatchIfExpectedStatusIs(AUTHORIZING);
    }

    @Override
    public void preAuthorized(Participant participant) {
        countDownLatchIfExpectedStatusIs(AUTHORIZED);
    }

    @Override
    public void preDenied(Participant participant) {
        countDownLatchIfExpectedStatusIs(DENIED);
    }

    private void countDownLatchIfExpectedStatusIs(ParticipantStatus status) {
        if (status == this.status) {
            latch.countDown();
        }
    }
}
