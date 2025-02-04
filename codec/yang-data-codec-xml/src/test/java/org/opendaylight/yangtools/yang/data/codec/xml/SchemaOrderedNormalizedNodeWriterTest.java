/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.codec.xml;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UserMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.SchemaOrderedNormalizedNodeWriter;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class SchemaOrderedNormalizedNodeWriterTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return TestFactories.junitParameters();
    }

    private static final String EXPECTED_1 = ""
        + "<root xmlns=\"foo\">\n"
        + "    <policy>\n"
        + "        <name>policy1</name>\n"
        + "        <rule>\n"
        + "            <name>rule1</name>\n"
        + "        </rule>\n"
        + "        <rule>\n"
        + "            <name>rule2</name>\n"
        + "        </rule>\n"
        + "        <rule>\n"
        + "            <name>rule3</name>\n"
        + "        </rule>\n"
        + "        <rule>\n"
        + "            <name>rule4</name>\n"
        + "        </rule>\n"
        + "    </policy>\n"
        + "    <policy>\n"
        + "        <name>policy2</name>\n"
        + "    </policy>\n"
        + "</root>\n";


    private static final String EXPECTED_2 = ""
        + "<root xmlns=\"order\">\n"
        + "    <id>id1</id>\n"
        + "    <cont>\n"
        + "        <content>content1</content>\n"
        + "    </cont>\n"
        + "</root>";

    private static final String FOO_NAMESPACE = "foo";
    private static final String RULE_NODE = "rule";
    private static final String NAME_NODE = "name";
    private static final String POLICY_NODE = "policy";
    private static final String ORDER_NAMESPACE = "order";

    private final XMLOutputFactory factory;

    public SchemaOrderedNormalizedNodeWriterTest(final String factoryMode, final XMLOutputFactory factory) {
        this.factory = factory;
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Test
    public void testWrite() throws XMLStreamException, IOException, SAXException {
        final StringWriter stringWriter = new StringWriter();
        final XMLStreamWriter xmlStreamWriter = factory.createXMLStreamWriter(stringWriter);

        EffectiveModelContext schemaContext = getSchemaContext("""
            module foo {
              namespace "foo";
              prefix "foo";
              revision "2016-02-17";

              container root {
                list policy {
                  key name;
                  leaf name {
                    type string;
                  }
                  list rule {
                    key name;
                    leaf name {
                      type string;
                    }
                  }
                }
              }
            }""");
        NormalizedNodeStreamWriter writer = XMLStreamNormalizedNodeStreamWriter.create(xmlStreamWriter, schemaContext);

        try (SchemaOrderedNormalizedNodeWriter nnw = new SchemaOrderedNormalizedNodeWriter(writer, schemaContext)) {

            List<MapEntryNode> rule1Names = new ArrayList<>();
            rule1Names.add(ImmutableNodes.mapEntry(createQName(FOO_NAMESPACE, RULE_NODE),
                createQName(FOO_NAMESPACE, NAME_NODE), "rule1"));
            rule1Names.add(ImmutableNodes.mapEntry(createQName(FOO_NAMESPACE, RULE_NODE),
                createQName(FOO_NAMESPACE, NAME_NODE), "rule2"));

            List<MapEntryNode> rule2Names = new ArrayList<>();
            rule1Names.add(ImmutableNodes.mapEntry(createQName(FOO_NAMESPACE, RULE_NODE),
                createQName(FOO_NAMESPACE, NAME_NODE), "rule3"));
            rule1Names.add(ImmutableNodes.mapEntry(createQName(FOO_NAMESPACE, RULE_NODE),
                createQName(FOO_NAMESPACE, NAME_NODE), "rule4"));

            UserMapNode rules1 = Builders.orderedMapBuilder()
                    .withNodeIdentifier(getNodeIdentifier(FOO_NAMESPACE, RULE_NODE))
                    .withValue(rule1Names)
                    .build();
            UserMapNode rules2 = Builders.orderedMapBuilder()
                    .withNodeIdentifier(getNodeIdentifier(FOO_NAMESPACE, RULE_NODE))
                    .withValue(rule2Names)
                    .build();

            List<MapEntryNode> policyNodes = new ArrayList<>();


            final MapEntryNode pn1 = ImmutableNodes
                    .mapEntryBuilder(createQName(FOO_NAMESPACE, POLICY_NODE),
                        createQName(FOO_NAMESPACE, NAME_NODE), "policy1")
                    .withChild(rules1)
                    .build();
            final MapEntryNode pn2 = ImmutableNodes
                    .mapEntryBuilder(createQName(FOO_NAMESPACE, POLICY_NODE),
                        createQName(FOO_NAMESPACE, NAME_NODE), "policy2")
                    .withChild(rules2)
                    .build();
            policyNodes.add(pn1);
            policyNodes.add(pn2);

            UserMapNode policy = Builders.orderedMapBuilder()
                    .withNodeIdentifier(getNodeIdentifier(FOO_NAMESPACE, POLICY_NODE))
                    .withValue(policyNodes)
                    .build();
            ContainerNode root = Builders.containerBuilder()
                    .withNodeIdentifier(getNodeIdentifier(FOO_NAMESPACE, "root"))
                    .withChild(policy).build();
            nnw.write(root);
        }

        XMLAssert.assertXMLIdentical(new Diff(EXPECTED_1, stringWriter.toString()), true);
    }

    @Test
    public void testWriteOrder() throws XMLStreamException, IOException, SAXException {
        final StringWriter stringWriter = new StringWriter();
        final XMLStreamWriter xmlStreamWriter = factory.createXMLStreamWriter(stringWriter);
        EffectiveModelContext schemaContext = getSchemaContext("""
            module order {
              namespace "order";
              prefix "order";
              revision "2016-02-17";

              container root {
                leaf id {
                  type string;
                }
                container cont {
                  leaf content {
                    type string;
                  }
                }
              }
            }""");
        NormalizedNodeStreamWriter writer = XMLStreamNormalizedNodeStreamWriter.create(xmlStreamWriter, schemaContext);

        try (NormalizedNodeWriter nnw = new SchemaOrderedNormalizedNodeWriter(writer, schemaContext)) {

            ContainerNode cont = Builders.containerBuilder()
                    .withNodeIdentifier(getNodeIdentifier(ORDER_NAMESPACE, "cont"))
                    .withChild(ImmutableNodes.leafNode(createQName(ORDER_NAMESPACE, "content"), "content1"))
                    .build();

            ContainerNode root = Builders.containerBuilder()
                    .withNodeIdentifier(getNodeIdentifier(ORDER_NAMESPACE, "root"))
                    .withChild(cont)
                    .withChild(ImmutableNodes.leafNode(createQName(ORDER_NAMESPACE, "id"), "id1"))
                    .build();

            nnw.write(root);
        }

        XMLAssert.assertXMLIdentical(new Diff(EXPECTED_2, stringWriter.toString()), true);
    }

    private static EffectiveModelContext getSchemaContext(final String literalYang) {
        return YangParserTestUtils.parseYang(literalYang);
    }

    private static YangInstanceIdentifier.NodeIdentifier getNodeIdentifier(final String ns, final String name) {
        return YangInstanceIdentifier.NodeIdentifier.create(createQName(ns, name));
    }

    private static QName createQName(final String ns, final String name) {
        return QName.create(ns, "2016-02-17", name);
    }

}
