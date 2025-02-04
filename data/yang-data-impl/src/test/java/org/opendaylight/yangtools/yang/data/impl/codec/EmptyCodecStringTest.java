/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.impl.codec;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.api.codec.EmptyCodec;
import org.opendaylight.yangtools.yang.model.ri.type.BaseTypes;

/**
 * Unit tests for EmptyCodecString.
 *
 * @author Thomas Pantelis
 */
public class EmptyCodecStringTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testSerialize() {
        EmptyCodec<String> codec = TypeDefinitionAwareCodecTestHelper.getCodec(BaseTypes.emptyType(), EmptyCodec.class);

        assertEquals("serialize", "", codec.serialize(Empty.value()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeserialize() {
        EmptyCodec<String> codec = TypeDefinitionAwareCodecTestHelper.getCodec(BaseTypes.emptyType(), EmptyCodec.class);

        assertEquals("deserialize", Empty.value(), codec.deserialize(""));

        TypeDefinitionAwareCodecTestHelper.deserializeWithExpectedIllegalArgEx(codec, "foo");
    }
}
