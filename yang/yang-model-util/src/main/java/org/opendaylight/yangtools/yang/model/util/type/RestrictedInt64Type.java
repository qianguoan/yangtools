/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.model.util.type;

import java.util.Collection;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.type.Int64TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.RangeConstraint;

final class RestrictedInt64Type extends AbstractRangeRestrictedType<Int64TypeDefinition, Long>
        implements Int64TypeDefinition {
    RestrictedInt64Type(final Int64TypeDefinition baseType, final SchemaPath path,
        final Collection<UnknownSchemaNode> unknownSchemaNodes, final @Nullable RangeConstraint<Long> rangeConstraint) {
        super(baseType, path, unknownSchemaNodes, rangeConstraint);
    }

    @Override
    public int hashCode() {
        return TypeDefinitions.hashCode(this);
    }

    @Override
    public boolean equals(final Object obj) {
        return TypeDefinitions.equals(this, obj);
    }

    @Override
    public String toString() {
        return TypeDefinitions.toString(this);
    }
}
