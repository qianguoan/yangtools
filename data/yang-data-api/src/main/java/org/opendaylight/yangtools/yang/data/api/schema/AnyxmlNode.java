/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.api.schema;

import com.google.common.annotations.Beta;

/**
 * A NormalizedNode holding the contents of an {@code anyxml} node in some object model. This interface is a common
 * capture for all object model specializations.
 *
 * @param <V> Value type, uniquely identifying the object model used for values
 */
@Beta
public non-sealed interface AnyxmlNode<V> extends ForeignDataNode<V> {
    @Override
    @SuppressWarnings("rawtypes")
    default Class<AnyxmlNode> contract() {
        return AnyxmlNode.class;
    }
}
