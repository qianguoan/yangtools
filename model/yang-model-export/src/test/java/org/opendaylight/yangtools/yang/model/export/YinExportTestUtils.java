/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.model.export;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.opendaylight.yangtools.yang.common.YangConstants;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public final class YinExportTestUtils {
    private YinExportTestUtils() {
        // Hidden on purpose
    }

    public static Document loadDocument(final String prefix, final Module module) throws IOException, SAXException {
        final var rev = module.getRevision();
        final var fileName = rev.isEmpty() ? module.getName() : module.getName() + '@' + rev.orElseThrow().toString();
        return loadDocument(prefix + '/' + fileName + YangConstants.RFC6020_YIN_FILE_EXTENSION);
    }

    public static Document loadDocument(final String xmlPath) throws IOException, SAXException {
        return requireNonNull(readXmlToDocument(SchemaContextEmitterTest.class.getResourceAsStream(xmlPath)));
    }

    static Document readXmlToDocument(final InputStream xmlContent) throws IOException, SAXException {
        final var doc = UntrustedXML.newDocumentBuilder().parse(xmlContent);
        doc.getDocumentElement().normalize();
        return doc;
    }

    public static String toString(final Node xml) {
        try {
            final var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            final var result = new StreamResult(new StringWriter());
            final var source = new DOMSource(xml);
            transformer.transform(source, result);

            return result.getWriter().toString();
        } catch (TransformerFactoryConfigurationError | TransformerException e) {
            throw new IllegalStateException("Unable to serialize xml element " + xml, e);
        }
    }
}
