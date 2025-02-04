/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.impl.schema;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.SystemLeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.data.util.LeafsetEntryInterner;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;

final class InterningLeafSetNodeBuilder<T> extends ImmutableLeafSetNodeBuilder<T> {
    private final LeafsetEntryInterner interner;

    private InterningLeafSetNodeBuilder(final LeafsetEntryInterner interner) {
        this.interner = requireNonNull(interner);
    }

    private InterningLeafSetNodeBuilder(final LeafsetEntryInterner interner, final int sizeHint) {
        super(sizeHint);
        this.interner = requireNonNull(interner);
    }

    private static @Nullable LeafsetEntryInterner getInterner(final @Nullable DataSchemaNode schema) {
        return schema instanceof LeafListSchemaNode leafListSchema ? LeafsetEntryInterner.forSchema(leafListSchema)
                : null;
    }

    static <T> ListNodeBuilder<T, SystemLeafSetNode<T>> create(final @Nullable DataSchemaNode schema) {
        final LeafsetEntryInterner interner = getInterner(schema);
        if (interner != null) {
            return new InterningLeafSetNodeBuilder<>(interner);
        }

        return ImmutableLeafSetNodeBuilder.create();
    }

    static <T> ListNodeBuilder<T, SystemLeafSetNode<T>> create(final @Nullable DataSchemaNode schema,
            final int sizeHint) {
        final LeafsetEntryInterner interner = getInterner(schema);
        if (interner != null) {
            return new InterningLeafSetNodeBuilder<>(interner, sizeHint);
        }

        return ImmutableLeafSetNodeBuilder.create(sizeHint);
    }

    @Override
    public ImmutableLeafSetNodeBuilder<T> withChild(final LeafSetEntryNode<T> child) {
        return super.withChild(interner.intern(child));
    }
}
