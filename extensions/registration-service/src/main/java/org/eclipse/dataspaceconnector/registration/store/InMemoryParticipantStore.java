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

package org.eclipse.dataspaceconnector.registration.store;

import org.eclipse.dataspaceconnector.registration.authority.model.Participant;
import org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus;
import org.eclipse.dataspaceconnector.registration.store.spi.ParticipantStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory store for dataspace participants.
 */
public class InMemoryParticipantStore implements ParticipantStore {

    private final Map<String, Participant> storage = new ConcurrentHashMap<>();

    @Override
    public List<Participant> listParticipants() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void save(Participant participant) {
        storage.putIfAbsent(participant.getDid(), participant);
    }

    @Override
    public Collection<Participant> listParticipantsWithStatus(ParticipantStatus status) {
        return storage.values().stream().filter(p -> p.getStatus() == status).collect(Collectors.toList());
    }
}
