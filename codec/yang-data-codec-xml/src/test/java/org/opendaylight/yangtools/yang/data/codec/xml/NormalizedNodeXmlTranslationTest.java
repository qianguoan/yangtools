/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.codec.xml;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opendaylight.yangtools.yang.data.impl.schema.Builders.choiceBuilder;
import static org.opendaylight.yangtools.yang.data.impl.schema.Builders.containerBuilder;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.leafNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndTextQualifier;
import org.custommonkey.xmlunit.IgnoreTextAndAttributeValuesDifferenceListener;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.SystemLeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.SystemMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.builder.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.builder.NormalizedNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack.Inference;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class NormalizedNodeXmlTranslationTest {
    private final EffectiveModelContext schema;

    @Parameterized.Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "/schema/augment_choice_hell.yang", "/schema/augment_choice_hell_ok.xml", augmentChoiceHell() },
                { "/schema/augment_choice_hell.yang", "/schema/augment_choice_hell_ok2.xml", null },
                { "/schema/augment_choice_hell.yang", "/schema/augment_choice_hell_ok3.xml", augmentChoiceHell2() },
                { "/schema/test.yang", "/schema/simple.xml", null },
                { "/schema/test.yang", "/schema/simple2.xml", null },
                // TODO check attributes
                { "/schema/test.yang", "/schema/simple_xml_with_attributes.xml", withAttributes() }
        });
    }

    private static final QNameModule MODULE = QNameModule.create(
        XMLNamespace.of("urn:opendaylight:params:xml:ns:yang:controller:test"), Revision.of("2014-03-13"));

    private static ContainerNode augmentChoiceHell2() {
        final NodeIdentifier container = getNodeIdentifier("container");
        final QName augmentChoice1QName = QName.create(container.getNodeType(), "augment-choice1");
        final QName augmentChoice2QName = QName.create(augmentChoice1QName, "augment-choice2");
        final QName containerQName = QName.create(augmentChoice1QName, "case11-choice-case-container");
        final QName leafQName = QName.create(augmentChoice1QName, "case11-choice-case-leaf");

        final NodeIdentifier augmentChoice1Id = new NodeIdentifier(augmentChoice1QName);
        final NodeIdentifier augmentChoice2Id = new NodeIdentifier(augmentChoice2QName);
        final NodeIdentifier containerId = new NodeIdentifier(containerQName);

        return containerBuilder().withNodeIdentifier(container)
            .withChild(choiceBuilder().withNodeIdentifier(augmentChoice1Id)
                .withChild(choiceBuilder().withNodeIdentifier(augmentChoice2Id)
                    .withChild(containerBuilder().withNodeIdentifier(containerId)
                        .withChild(leafNode(leafQName, "leaf-value"))
                        .build())
                    .build())
                .build())
            .build();
    }

    private static ContainerNode withAttributes() {
        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> b = containerBuilder();
        b.withNodeIdentifier(getNodeIdentifier("container"));

        final CollectionNodeBuilder<MapEntryNode, SystemMapNode> listBuilder =
                Builders.mapBuilder().withNodeIdentifier(getNodeIdentifier("list"));

        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> list1Builder = Builders
                .mapEntryBuilder().withNodeIdentifier(NodeIdentifierWithPredicates.of(
                                getNodeIdentifier("list").getNodeType(),
                                getNodeIdentifier("uint32InList").getNodeType(), Uint32.valueOf(3)));
        final NormalizedNodeBuilder<NodeIdentifier, Object, LeafNode<Object>> uint32InListBuilder = Builders
                .leafBuilder().withNodeIdentifier(getNodeIdentifier("uint32InList"));

        list1Builder.withChild(uint32InListBuilder.withValue(Uint32.valueOf(3)).build());

        listBuilder.withChild(list1Builder.build());
        b.withChild(listBuilder.build());

        final NormalizedNodeBuilder<NodeIdentifier, Object, LeafNode<Object>> booleanBuilder = Builders
                .leafBuilder().withNodeIdentifier(getNodeIdentifier("boolean"));
        booleanBuilder.withValue(Boolean.FALSE);
        b.withChild(booleanBuilder.build());

        final ListNodeBuilder<Object, SystemLeafSetNode<Object>> leafListBuilder = Builders.leafSetBuilder()
                .withNodeIdentifier(getNodeIdentifier("leafList"));

        final NormalizedNodeBuilder<NodeWithValue, Object, LeafSetEntryNode<Object>> leafList1Builder = Builders
                .leafSetEntryBuilder().withNodeIdentifier(
                        new NodeWithValue(getNodeIdentifier("leafList").getNodeType(), "a"));

        leafList1Builder.withValue("a");

        leafListBuilder.withChild(leafList1Builder.build());
        b.withChild(leafListBuilder.build());

        return b.build();
    }

    private static ContainerNode augmentChoiceHell() {

        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> b = containerBuilder();
        b.withNodeIdentifier(getNodeIdentifier("container"));

        b.withChild(choiceBuilder()
                .withNodeIdentifier(getNodeIdentifier("ch2"))
                .withChild(
                        Builders.leafBuilder().withNodeIdentifier(getNodeIdentifier("c2Leaf")).withValue("2").build())
                .withChild(
                        choiceBuilder()
                                .withNodeIdentifier(getNodeIdentifier("c2DeepChoice"))
                                .withChild(
                                        Builders.leafBuilder()
                                                .withNodeIdentifier(getNodeIdentifier("c2DeepChoiceCase1Leaf2"))
                                                .withValue("2").build()).build()).build());

        b.withChild(choiceBuilder()
                .withNodeIdentifier(getNodeIdentifier("ch3"))
                .withChild(
                        Builders.leafBuilder().withNodeIdentifier(getNodeIdentifier("c3Leaf")).withValue("3").build())
                .build());

        b.withChild(Builders.leafBuilder().withNodeIdentifier(getNodeIdentifier("augLeaf")).withValue("augment")
                                .build());

        b.withChild(choiceBuilder()
            .withNodeIdentifier(getNodeIdentifier("ch"))
            .withChild(Builders.leafBuilder().withNodeIdentifier(getNodeIdentifier("c1Leaf")).withValue("1").build())
            .withChild(Builders.leafBuilder().withNodeIdentifier(getNodeIdentifier("c1Leaf_AnotherAugment"))
                .withValue("1").build())
            .withChild(choiceBuilder()
                .withNodeIdentifier(getNodeIdentifier("deepChoice"))
                .withChild(Builders.leafBuilder()
                    .withNodeIdentifier(getNodeIdentifier("deepLeafc1"))
                    .withValue("1").build()).build())
            .build());

        return b.build();
    }

    private static NodeIdentifier getNodeIdentifier(final String localName) {
        return new NodeIdentifier(QName.create(MODULE, localName));
    }

    private final ContainerNode expectedNode;
    private final String xmlPath;

    public NormalizedNodeXmlTranslationTest(final String yangPath, final String xmlPath,
            final ContainerNode expectedNode) {
        schema = YangParserTestUtils.parseYangResource(yangPath);
        this.xmlPath = xmlPath;
        this.expectedNode = expectedNode;
    }

    @Test
    public void testTranslationRepairing() throws Exception {
        testTranslation(TestFactories.REPAIRING_OUTPUT_FACTORY);
    }

    @Test
    public void testTranslation() throws Exception {
        testTranslation(TestFactories.DEFAULT_OUTPUT_FACTORY);
    }

    private void testTranslation(final XMLOutputFactory factory) throws Exception {
        final var resourceAsStream = XmlToNormalizedNodesTest.class.getResourceAsStream(xmlPath);

        final var reader = UntrustedXML.createXMLStreamReader(resourceAsStream);

        final var result = new NormalizationResultHolder();
        final var streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
        final var xmlParser = XmlParserStream.create(streamWriter,
            Inference.ofDataTreePath(schema, QName.create(MODULE, "container")));
        xmlParser.parse(reader);

        final var built = result.getResult().data();
        assertNotNull(built);

        if (expectedNode != null) {
            assertEquals(expectedNode, built);
        }

        final Document document = UntrustedXML.newDocumentBuilder().newDocument();
        final DOMResult domResult = new DOMResult(document);

        final XMLStreamWriter xmlStreamWriter = factory.createXMLStreamWriter(domResult);

        final NormalizedNodeStreamWriter xmlNormalizedNodeStreamWriter = XMLStreamNormalizedNodeStreamWriter
                .create(xmlStreamWriter, schema);

        final NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(
                xmlNormalizedNodeStreamWriter);

        normalizedNodeWriter.write(built);

        final Document doc = loadDocument(xmlPath);

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setNormalize(true);

        final String expectedXml = toString(doc.getDocumentElement());
        final String serializedXml = toString(domResult.getNode());

        final Diff diff = new Diff(expectedXml, serializedXml);
        diff.overrideDifferenceListener(new IgnoreTextAndAttributeValuesDifferenceListener());
        diff.overrideElementQualifier(new ElementNameAndTextQualifier());

        // FIXME the comparison cannot be performed, since the qualifiers supplied by XMlUnit do not work correctly in
        // this case
        // We need to implement custom qualifier so that the element ordering does not mess the DIFF
        // dd.overrideElementQualifier(new MultiLevelElementNameAndTextQualifier(100, true));
        // assertTrue(dd.toString(), dd.similar());

        //new XMLTestCase() {}.assertXMLEqual(diff, true);
    }

    private static Document loadDocument(final String xmlPath) throws IOException, SAXException {
        final InputStream resourceAsStream = NormalizedNodeXmlTranslationTest.class.getResourceAsStream(xmlPath);
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
