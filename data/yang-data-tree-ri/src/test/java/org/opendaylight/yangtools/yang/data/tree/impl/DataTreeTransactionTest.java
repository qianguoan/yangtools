/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.tree.impl;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;

public class DataTreeTransactionTest extends AbstractTestModelTest {
    private DataTree tree;

    @Before
    public void setUp() {
        tree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_OPERATIONAL, SCHEMA_CONTEXT);
    }

    @Test
    public void testSealedValidate() throws DataValidationFailedException {
        final DataTreeModification mod = tree.takeSnapshot().newModification();
        mod.ready();
        tree.validate(mod);
    }

    @Test
    public void testSealedPrepare() throws DataValidationFailedException {
        final DataTreeModification mod = tree.takeSnapshot().newModification();
        mod.ready();
        tree.prepare(mod);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsealedValidate() throws DataValidationFailedException {
        final DataTreeModification mod = tree.takeSnapshot().newModification();
        tree.validate(mod);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsealedPrepare() throws DataValidationFailedException {
        final DataTreeModification mod = tree.takeSnapshot().newModification();
        tree.prepare(mod);
    }
}
