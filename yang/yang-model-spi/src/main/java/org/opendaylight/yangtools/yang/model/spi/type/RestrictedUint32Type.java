/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.model.spi.type;

import java.util.Collection;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.type.RangeConstraint;
import org.opendaylight.yangtools.yang.model.api.type.Uint32TypeDefinition;

final class RestrictedUint32Type extends AbstractRangeRestrictedType<Uint32TypeDefinition, Uint32>
        implements Uint32TypeDefinition {
    RestrictedUint32Type(final Uint32TypeDefinition baseType, final QName qname,
            final Collection<? extends UnknownSchemaNode> unknownSchemaNodes,
            final @Nullable RangeConstraint<Uint32> rangeConstraint) {
        super(baseType, qname, unknownSchemaNodes, rangeConstraint);
    }

    @Override
    public int hashCode() {
        return Uint32TypeDefinition.hashCode(this);
    }

    @Override
    public boolean equals(final Object obj) {
        return Uint32TypeDefinition.equals(this, obj);
    }

    @Override
    public String toString() {
        return Uint32TypeDefinition.toString(this);
    }
}
