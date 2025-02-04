/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.model.api.stmt;

import org.opendaylight.yangtools.yang.model.api.YangStmtMapping;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.meta.StatementDefinition;

/**
 * Declared representation of a {@code value} statement.
 */
public interface ValueStatement extends DeclaredStatement<Integer> {
    @Override
    default StatementDefinition statementDefinition() {
        return YangStmtMapping.VALUE;
    }

    @Deprecated(since = "10.0.0", forRemoval = true)
    default int getValue() {
        return argument().intValue();
    }
}
