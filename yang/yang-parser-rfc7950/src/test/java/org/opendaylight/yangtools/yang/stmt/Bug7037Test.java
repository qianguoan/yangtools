/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.stmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.stmt.UnrecognizedStatement;

public class Bug7037Test {
    private static final String FOO_NS = "foo";
    private static final String BAR_NS = "bar";

    @Test
    public void test() throws Exception {
        final EffectiveModelContext context = StmtTestUtils.parseYangSources("/bugs/bug7037");
        assertNotNull(context);

        final Collection<? extends UnrecognizedStatement> unknownSchemaNodes = context.getModuleStatement(foo("foo"))
            .getDeclared().declaredSubstatements(UnrecognizedStatement.class);
        assertEquals(1, unknownSchemaNodes.size());

        final UnrecognizedStatement first = unknownSchemaNodes.iterator().next();
        final Collection<? extends UnrecognizedStatement> firstUnknownNodes =
            first.declaredSubstatements(UnrecognizedStatement.class);
        assertEquals(1, firstUnknownNodes.size());

        final UnrecognizedStatement barExtCont = firstUnknownNodes.iterator().next();
        assertEquals(bar("container"), barExtCont.statementDefinition().getStatementName());
        assertEquals("bar-ext-con", barExtCont.argument());

        final DataSchemaNode root = context.getDataChildByName(foo("root"));
        assertTrue(root instanceof ContainerSchemaNode);

        final Collection<? extends UnrecognizedStatement> rootUnknownNodes =
            ((ContainerSchemaNode) root).asEffectiveStatement().getDeclared()
            .declaredSubstatements(UnrecognizedStatement.class);
        assertEquals(2, rootUnknownNodes.size());

        final Map<QName, UnrecognizedStatement> rootUnknownNodeMap = rootUnknownNodes.stream()
                .collect(Collectors.toMap(u -> u.statementDefinition().getStatementName(), u -> u));

        final UnrecognizedStatement barExt = rootUnknownNodeMap.get(bar("bar-ext"));
        final Collection<? extends UnrecognizedStatement> barExtUnknownNodes =
            barExt.declaredSubstatements(UnrecognizedStatement.class);
        assertEquals(3, barExtUnknownNodes.size());

        final Iterator<? extends UnrecognizedStatement> iterator = barExtUnknownNodes.iterator();
        UnrecognizedStatement barExtCont2 = null;
        while (iterator.hasNext()) {
            final UnrecognizedStatement next = iterator.next();
            if (bar("container").equals(next.statementDefinition().getStatementName())) {
                barExtCont2 = next;
                break;
            }
        }
        assertNotNull(barExtCont2);
        assertEquals("bar-ext-con-2", barExtCont2.argument());

        final UnrecognizedStatement fooExt = rootUnknownNodeMap.get(foo("foo-ext"));
        final Collection<? extends UnrecognizedStatement> fooUnknownNodes =
            fooExt.declaredSubstatements(UnrecognizedStatement.class);
        assertEquals(1, fooUnknownNodes.size());

        final UnrecognizedStatement fooExtCont = fooUnknownNodes.iterator().next();
        assertEquals(foo("container"), fooExtCont.statementDefinition().getStatementName());
        assertEquals("foo-ext-con", fooExtCont.argument());
    }

    private static QName foo(final String localName) {
        return QName.create(FOO_NS, localName);
    }

    private static QName bar(final String localName) {
        return QName.create(BAR_NS, localName);
    }
}
