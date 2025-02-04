/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.codec.xml;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.junit.Test;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.opendaylight.yangtools.yang.data.api.schema.AnydataNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack.Inference;
import org.xml.sax.SAXException;

public class AnydataParseTest extends AbstractAnydataTest {
    @Test
    public void testAnydata() throws XMLStreamException, IOException, URISyntaxException, SAXException {
        final XMLStreamReader reader = UntrustedXML.createXMLStreamReader(
            toInputStream("<foo xmlns=\"test-anydata\"><bar/></foo>"));

        final var result = new NormalizationResultHolder();
        final var streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
        final var xmlParser = XmlParserStream.create(streamWriter, Inference.ofDataTreePath(SCHEMA_CONTEXT, FOO_QNAME),
            true);
        xmlParser.parse(reader);

        assertThat(result.getResult().data(), instanceOf(AnydataNode.class));
    }
}
