/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.model.spi.type;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

abstract class AbstractTypeDefinition<T extends TypeDefinition<T>> implements Immutable, TypeDefinition<T> {
    private final @NonNull ImmutableList<UnknownSchemaNode> unknownSchemaNodes;
    private final @NonNull QName qname;

    AbstractTypeDefinition(final QName qname, final Collection<? extends UnknownSchemaNode> unknownSchemaNodes) {
        this.qname = requireNonNull(qname);
        this.unknownSchemaNodes = ImmutableList.copyOf(unknownSchemaNodes);
    }

    @Override
    public final QName getQName() {
        return qname;
    }

    @Override
    public final Collection<? extends UnknownSchemaNode> getUnknownSchemaNodes() {
        return unknownSchemaNodes;
    }

    @Override
    public abstract String toString();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);
}
