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

import org.eclipse.dataspaceconnector.registration.store.model.Participant;
import org.eclipse.dataspaceconnector.registration.store.model.ParticipantStatus;
import org.eclipse.dataspaceconnector.spi.observe.Observable;

/**
 * Interface implemented by listeners registered to observe participant state changes
 * via {@link Observable#registerListener}.
 * <p>
 * Note that the listener is called before state changes are persisted.
 * Therefore, when using a persistent store implementation, it
 * is guaranteed to be called at least once.
 */
public interface ParticipantListener {

    /**
     * Called after a {@link Participant} has been created, but before the entity is persisted.
     *
     * @param participant the participant that has been created.
     */
    default void onCreation(Participant participant) {
    }

    /**
     * Called after a {@link Participant} has moved to state
     * {@link ParticipantStatus#AUTHORIZED}, but before the change is persisted.
     *
     * @param participant the participant whose state has changed.
     */
    default void preAuthorized(Participant participant) {
    }
}
