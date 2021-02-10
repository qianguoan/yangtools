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
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.type.Int16TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.RangeConstraint;

final class RestrictedInt16Type extends AbstractRangeRestrictedType<Int16TypeDefinition, Short>
        implements Int16TypeDefinition {
    RestrictedInt16Type(final Int16TypeDefinition baseType, final QName qname,
            final Collection<? extends UnknownSchemaNode> unknownSchemaNodes,
            final @Nullable RangeConstraint<Short> rangeConstraint) {
        super(baseType, qname, unknownSchemaNodes, rangeConstraint);
    }

    @Override
    public int hashCode() {
        return Int16TypeDefinition.hashCode(this);
    }

    @Override
    public boolean equals(final Object obj) {
        return Int16TypeDefinition.equals(this, obj);
    }

    @Override
    public String toString() {
        return Int16TypeDefinition.toString(this);
    }
}
