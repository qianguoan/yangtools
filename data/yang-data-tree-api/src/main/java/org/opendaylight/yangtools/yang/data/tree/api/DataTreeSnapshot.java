/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.tree.api;

import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContextProvider;

/**
 * Read-only snapshot of a {@link DataTree}. The snapshot is stable and isolated, e.g. data tree changes occurring after
 * the snapshot has been taken are not visible through the snapshot.
 */
public interface DataTreeSnapshot extends EffectiveModelContextProvider {
    /**
     * Read a particular node from the snapshot.
     *
     * @param path Path of the node
     * @return Optional result encapsulating the presence and value of the node
     */
    Optional<NormalizedNode> readNode(YangInstanceIdentifier path);

    /**
     * Create a new data tree modification based on this snapshot, using the
     * specified data application strategy.
     *
     * @return A new data tree modification
     */
    @NonNull DataTreeModification newModification();
}
