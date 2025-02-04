/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.tree.impl;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public final class TestModel {

    public static final QName TEST_QNAME = QName.create(
            "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test", "2014-03-13", "test");
    public static final QName OUTER_LIST_QNAME = QName.create(TEST_QNAME, "outer-list");
    public static final QName INNER_LIST_QNAME = QName.create(TEST_QNAME, "inner-list");
    public static final QName OUTER_CHOICE_QNAME = QName.create(TEST_QNAME, "outer-choice");
    public static final QName INNER_CONTAINER_QNAME = QName.create(TEST_QNAME, "inner-container");
    public static final QName ID_QNAME = QName.create(TEST_QNAME, "id");
    public static final QName NAME_QNAME = QName.create(TEST_QNAME, "name");
    public static final QName VALUE_QNAME = QName.create(TEST_QNAME, "value");

    public static final QName NON_PRESENCE_QNAME = QName.create(TEST_QNAME, "non-presence");
    public static final QName DEEP_CHOICE_QNAME = QName.create(TEST_QNAME, "deep-choice");
    public static final QName A_LIST_QNAME = QName.create(TEST_QNAME, "a-list");
    public static final QName A_NAME_QNAME = QName.create(TEST_QNAME, "a-name");

    public static final YangInstanceIdentifier TEST_PATH = YangInstanceIdentifier.of(TEST_QNAME);
    public static final YangInstanceIdentifier OUTER_LIST_PATH = YangInstanceIdentifier.builder(TEST_PATH)
            .node(OUTER_LIST_QNAME).build();
    public static final YangInstanceIdentifier INNER_CONTAINER_PATH = TEST_PATH.node(INNER_CONTAINER_QNAME);
    public static final YangInstanceIdentifier VALUE_PATH = YangInstanceIdentifier.of(VALUE_QNAME);
    public static final YangInstanceIdentifier INNER_VALUE_PATH = INNER_CONTAINER_PATH.node(VALUE_QNAME);
    public static final YangInstanceIdentifier NON_PRESENCE_PATH = YangInstanceIdentifier.of(NON_PRESENCE_QNAME);
    public static final YangInstanceIdentifier DEEP_CHOICE_PATH = NON_PRESENCE_PATH.node(DEEP_CHOICE_QNAME);
    public static final YangInstanceIdentifier NAME_PATH = NON_PRESENCE_PATH.node(NAME_QNAME);

    public static final QName TWO_QNAME = QName.create(TEST_QNAME, "two");
    public static final QName THREE_QNAME = QName.create(TEST_QNAME, "three");

    private TestModel() {
        throw new UnsupportedOperationException();
    }

    // FIXME: inline into single caller
    public static EffectiveModelContext createTestContext() {
        return createTestContext("/odl-datastore-test.yang");
    }

    // FIXME: remove this method
    public static EffectiveModelContext createTestContext(final String resourcePath) {
        return YangParserTestUtils.parseYangResources(TestModel.class, resourcePath);
    }
}
