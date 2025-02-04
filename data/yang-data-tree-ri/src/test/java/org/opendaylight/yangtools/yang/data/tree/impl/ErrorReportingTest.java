/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.tree.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.ConflictingModificationAppliedException;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.api.ModifiedNodeDoesNotExistException;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;

public class ErrorReportingTest extends AbstractTestModelTest {
    private DataTree tree;

    @Before
    public void setup() {
        tree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_OPERATIONAL, SCHEMA_CONTEXT);
    }

    @Test
    public void writeWithoutParentExisting() {
        DataTreeModification modification = tree.takeSnapshot().newModification();
        // We write node without creating parent
        modification.write(TestModel.OUTER_LIST_PATH, ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME)
            .build());
        modification.ready();
        try {
            tree.validate(modification);
            fail("ModifiedNodeDoesNotExistException should be raised");
        } catch (ModifiedNodeDoesNotExistException e) {
            assertEquals(TestModel.TEST_PATH, e.getPath());
        } catch (DataValidationFailedException e) {
            fail("ModifiedNodeDoesNotExistException expected");
        }
    }

    @Test
    public void parentConcurrentlyDeletedExisting() throws DataValidationFailedException {
        DataTreeModification initial = tree.takeSnapshot().newModification();
        // We write node without creating parent
        initial.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
        initial.ready();
        // We commit transaction
        tree.commit(tree.prepare(initial));

        final DataTreeModification writeTx = tree.takeSnapshot().newModification();
        final DataTreeModification deleteTx = tree.takeSnapshot().newModification();
        deleteTx.delete(TestModel.TEST_PATH);
        deleteTx.ready();
        // We commit delete modification
        tree.commit(tree.prepare(deleteTx));

        writeTx.write(TestModel.OUTER_LIST_PATH, ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());
        writeTx.ready();
        try {
            tree.validate(writeTx);
            fail("ConflictingModificationAppliedException should be raised");
        } catch (ConflictingModificationAppliedException e) {
            assertEquals(TestModel.TEST_PATH, e.getPath());
        } catch (DataValidationFailedException e) {
            fail("ConflictingModificationAppliedException expected");
        }
    }
}
