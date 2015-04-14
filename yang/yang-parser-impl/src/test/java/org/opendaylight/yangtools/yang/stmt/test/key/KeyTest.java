/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yangtools.yang.stmt.test.key;

import org.junit.Test;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.spi.source.SourceException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor.BuildAction;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.EffectiveModelContext;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;

import static org.junit.Assert.*;

public class KeyTest {

    private static final TestKeySource KEY_SIMPLE = new TestKeySource("root", "key");
    private static final TestKeySource KEY_COMP = new TestKeySource("root", "key1 key2 key3");
    private static final TestKeySource KEY_COMP_DUPLICATE = new TestKeySource("root", "key1 key1 key2");

    @Test
    public void keySimpleTest() throws SourceException, ReactorException {

        BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        addSources(reactor, KEY_SIMPLE);

        EffectiveModelContext result = reactor.build();
        assertNotNull(result);
    }

    @Test
    public void keyCompositeTest() throws SourceException, ReactorException {

        BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        addSources(reactor, KEY_COMP);

        EffectiveModelContext result = reactor.build();
        assertNotNull(result);
    }

    @Test
    public void keyCompositeInvalid() throws SourceException, ReactorException {

        BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        addSources(reactor, KEY_COMP_DUPLICATE);

        try {
            reactor.build();
            fail("reactor.process should fail due to duplicate name in key");
        } catch (Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }

    private void addSources(BuildAction reactor, TestKeySource... sources) {
        for (TestKeySource source : sources) {
            reactor.addSource(source);
        }
    }

}
