/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.codec.xml;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack.Inference;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class DOMSourceXMLStreamReaderTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return TestFactories.junitParameters();
    }

    private static EffectiveModelContext SCHEMA_CONTEXT;

    private final XMLOutputFactory factory;

    public DOMSourceXMLStreamReaderTest(final String factoryMode, final XMLOutputFactory factory) {
        this.factory = factory;
    }

    @BeforeClass
    public static void beforeClass() {
        SCHEMA_CONTEXT = YangParserTestUtils.parseYang("""
            module bar {
              namespace bar-ns;
              prefix bar;

              import foo {
                prefix foo;
              }

              augment "/foo:top-cont/foo:keyed-list" {
                leaf iid-leaf {
                  type instance-identifier;
                }
              }
            }""", """
            module baz {
              namespace baz-ns;
              prefix baz;

              container top-cont {
                /*list keyed-list {
                  key key-leaf;
                  leaf key-leaf {
                    type int32;
                  }
                }*/
              }
            }""", """
            module foo {
              namespace foo-ns;
              prefix foo;

              import rab {
                prefix rab;
              }

              container top-cont {
                list keyed-list {
                  key key-leaf;
                  leaf key-leaf {
                    type int32;
                  }
                  leaf idref-leaf {
                    type identityref {
                      base rab:base-id;
                    }
                  }
                  leaf ordinary-leaf {
                    type int32;
                  }
                }
              }
            }""", """
            module rab {
              namespace rab-ns;
              prefix rab;

              import baz {
                prefix baz;
              }

              augment "/baz:top-cont" {
                list keyed-list {
                  key key-leaf;
                  leaf key-leaf {
                    type int32;
                  }
                }
              }

              identity base-id;

              identity id-foo {
                base base-id;
              }
            }""", """
            module zab {
              namespace zab-ns;
              prefix zab;

              import foo {
                prefix foo;
              }

              augment "/foo:top-cont/foo:keyed-list/" {
                /*leaf ordinary-leaf {
                  type int32;
                }*/
                anyxml aug-anyxml;
              }
            }""");
    }

    @AfterClass
    public static void afterClass() {
        SCHEMA_CONTEXT = null;
    }

    @Test
    public void test() throws Exception {

        // deserialization
        final Document doc = loadDocument("/dom-reader-test/foo.xml");
        final var inputXml = new DOMSource(doc.getDocumentElement());
        var domXMLReader = new DOMSourceXMLStreamReader(inputXml);

        final var result = new NormalizationResultHolder();
        final var streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
        final var xmlParser = XmlParserStream.create(streamWriter,
            Inference.ofDataTreePath(SCHEMA_CONTEXT, QName.create("foo-ns", "top-cont")));
        xmlParser.parse(domXMLReader);
        final var transformedInput = result.getResult().data();
        assertNotNull(transformedInput);

        // serialization
        //final StringWriter writer = new StringWriter();
        final var domResult = new DOMResult(UntrustedXML.newDocumentBuilder().newDocument());
        final var xmlStreamWriter = factory.createXMLStreamWriter(domResult);

        final var xmlNormalizedNodeStreamWriter = XMLStreamNormalizedNodeStreamWriter.create(
                xmlStreamWriter, SCHEMA_CONTEXT);

        final var normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(xmlNormalizedNodeStreamWriter);
        normalizedNodeWriter.write(transformedInput);

        //final String serializedXml = writer.toString();
        final String serializedXml = toString(domResult.getNode());
        assertFalse(serializedXml.isEmpty());
    }

    private static Document loadDocument(final String xmlPath) throws IOException, SAXException {
        final InputStream resourceAsStream = NormalizedNodesToXmlTest.class.getResourceAsStream(xmlPath);
        return requireNonNull(readXmlToDocument(resourceAsStream));
    }

    private static Document readXmlToDocument(final InputStream xmlContent) throws IOException, SAXException {
        final Document doc = UntrustedXML.newDocumentBuilder().parse(xmlContent);
        doc.getDocumentElement().normalize();
        return doc;
    }

    private static String toString(final Node xml) {
        try {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            final StreamResult result = new StreamResult(new StringWriter());
            final DOMSource source = new DOMSource(xml);
            transformer.transform(source, result);

            return result.getWriter().toString();
        } catch (IllegalArgumentException | TransformerFactoryConfigurationError | TransformerException e) {
            throw new RuntimeException("Unable to serialize xml element " + xml, e);
        }
    }
}
