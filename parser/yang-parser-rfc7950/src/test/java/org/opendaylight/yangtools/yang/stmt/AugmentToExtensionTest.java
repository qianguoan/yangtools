/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.stmt;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.UsesNode;
import org.opendaylight.yangtools.yang.parser.spi.meta.InferenceException;
import org.opendaylight.yangtools.yang.parser.spi.meta.SomeModifiersUnresolvedException;

public class AugmentToExtensionTest {
    @Test
    public void testIncorrectPath() throws Exception {
        final var ex = assertThrows(SomeModifiersUnresolvedException.class,
            () -> TestUtils.loadModules("/augment-to-extension-test/incorrect-path"));
        final var cause = ex.getCause();

        // FIXME: this should not be here
        assertThat(cause, instanceOf(InferenceException.class));
        assertThat(cause.getMessage(), startsWith("Yang model processing phase EFFECTIVE_MODEL failed [at "));

        final var firstCause = cause.getCause();
        assertThat(firstCause, instanceOf(InferenceException.class));
        assertThat(firstCause.getMessage(), startsWith("Augment target "
            + "'Descendant{qnames=[(uri:augment-module?revision=2014-10-07)my-extension-name-a, input]}'"
            + " not found [at "));
    }

    /*
     * FIXME: Figure way to determine use case of tail-f:input without hacks
     */
    @Test
    public void testCorrectPathIntoUnsupportedTarget() throws Exception {
        final Module devicesModule =
            TestUtils.loadModules("/augment-to-extension-test/correct-path-into-unsupported-target")
            .findModules("augment-module").iterator().next();
        final ContainerSchemaNode devicesContainer = (ContainerSchemaNode) devicesModule.getDataChildByName(
            QName.create(devicesModule.getQNameModule(), "my-container"));
        for (final UsesNode usesNode : devicesContainer.getUses()) {
            assertTrue(usesNode.getAugmentations().isEmpty());
        }
    }

    @Test
    public void testCorrectAugment() throws Exception {
        final Module devicesModule = TestUtils.loadModules("/augment-to-extension-test/correct-augment")
            .findModules("augment-module").iterator().next();

        final ContainerSchemaNode devicesContainer = (ContainerSchemaNode) devicesModule.getDataChildByName(QName
                .create(devicesModule.getQNameModule(), "my-container"));
        boolean augmentationIsInContainer = false;
        for (final UsesNode usesNode : devicesContainer.getUses()) {
            for (final AugmentationSchemaNode augmentationSchema : usesNode.getAugmentations()) {
                augmentationIsInContainer = true;
            }
        }

        assertTrue(augmentationIsInContainer);
    }
}
