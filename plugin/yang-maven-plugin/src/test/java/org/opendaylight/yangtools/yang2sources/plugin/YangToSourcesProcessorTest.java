/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang2sources.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.google.common.collect.ImmutableTable;
import java.io.File;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.yangtools.yang.model.api.Module;

@ExtendWith(MockitoExtension.class)
class YangToSourcesProcessorTest extends AbstractCodeGeneratorTest {
    private final File file = new File(getClass().getResource("/yang").getFile());

    @Test
    void basicTest() {
        assertMojoExecution(
            new YangToSourcesProcessor(file, List.of(), List.of(new FileGeneratorArg("mockGenerator")), project, true,
                YangProvider.getInstance()),
            mock -> {
                doAnswer(invocation -> {
                    final Set<Module> localModules = invocation.getArgument(1);
                    assertEquals(2, localModules.size());
                    return ImmutableTable.of();
                }).when(mock).generateFiles(any(), any(), any());
            },
            mock -> {
                // No-op
            });
    }

    @Test
    void excludeFilesTest() throws Exception {
        final var excludedYang = new File(getClass().getResource("/yang/excluded-file.yang").getFile());

        assertMojoExecution(
            new YangToSourcesProcessor(file, List.of(excludedYang), List.of(new FileGeneratorArg("mockGenerator")),
                project, true, YangProvider.getInstance()),
            mock -> {
                doAnswer(invocation -> {
                    final Set<Module> localModules = invocation.getArgument(1);
                    assertEquals(1, localModules.size());
                    assertEquals("mock", localModules.iterator().next().getName());
                    return ImmutableTable.of();
                }).when(mock).generateFiles(any(), any(), any());
            }, mock -> {
                // No-op
            });
    }
}
