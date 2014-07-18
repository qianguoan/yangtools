/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.impl.codec.xml;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

import javax.activation.UnsupportedDataTypeException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.AttributesContainer;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.*;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XmlDocumentUtils {
    private final static String RPC_REPLY_LOCAL_NAME = "rpc-reply";
    private final static String RPC_REPLY_NAMESPACE = "urn:ietf:params:xml:ns:netconf:base:1.0";
    private final static QName RPC_REPLY_QNAME = QName.create(URI.create(RPC_REPLY_NAMESPACE), null, RPC_REPLY_LOCAL_NAME);

    private static class ElementWithSchemaContext {
        Element element;
        SchemaContext schemaContext;

        ElementWithSchemaContext(final Element element,final SchemaContext schemaContext) {
            this.schemaContext = schemaContext;
            this.element = element;
        }

        Element getElement() {
            return element;
        }

        SchemaContext getSchemaContext() {
            return schemaContext;
        }
    }

    public static final QName OPERATION_ATTRIBUTE_QNAME = QName.create(URI.create("urn:ietf:params:xml:ns:netconf:base:1.0"), null, "operation");
    private static final Logger logger = LoggerFactory.getLogger(XmlDocumentUtils.class);
    private static final XMLOutputFactory FACTORY = XMLOutputFactory.newFactory();

    /**
     * Converts Data DOM structure to XML Document for specified XML Codec Provider and corresponding
     * Data Node Container schema. The CompositeNode data parameter enters as root of Data DOM tree and will
     * be transformed to root in XML Document. Each element of Data DOM tree is compared against specified Data
     * Node Container Schema and transformed accordingly.
     *
     * @param data Data DOM root element
     * @param schema Data Node Container Schema
     * @param codecProvider XML Codec Provider
     * @return new instance of XML Document
     * @throws UnsupportedDataTypeException
     */
    public static Document toDocument(final CompositeNode data, final DataNodeContainer schema, final XmlCodecProvider codecProvider)
            throws UnsupportedDataTypeException {
        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(schema);

        if (!(schema instanceof ContainerSchemaNode || schema instanceof ListSchemaNode)) {
            throw new UnsupportedDataTypeException("Schema can be ContainerSchemaNode or ListSchemaNode. Other types are not supported yet.");
        }

        final DOMResult result = new DOMResult(getDocument());
        try {
            final XMLStreamWriter writer = FACTORY.createXMLStreamWriter(result);
            XmlStreamUtils.create(codecProvider).writeDocument(writer, data, (SchemaNode)schema);
            writer.close();
            return (Document)result.getNode();
        } catch (XMLStreamException e) {
            logger.error("Failed to serialize data {}", data, e);
            return null;
        }
    }

    public static Document getDocument() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = null;
        try {
            DocumentBuilder bob = dbf.newDocumentBuilder();
            doc = bob.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        return doc;
    }

    /**
     * Converts Data DOM structure to XML Document for specified XML Codec Provider. The CompositeNode
     * data parameter enters as root of Data DOM tree and will be transformed to root in XML Document. The child
     * nodes of Data Tree are transformed accordingly.
     *
     * @param data Data DOM root element
     * @param codecProvider XML Codec Provider
     * @return new instance of XML Document
     * @throws UnsupportedDataTypeException
     */
    public static Document toDocument(final CompositeNode data, final XmlCodecProvider codecProvider) {
        final DOMResult result = new DOMResult(getDocument());
        try {
            final XMLStreamWriter writer = FACTORY.createXMLStreamWriter(result);
            XmlStreamUtils.create(codecProvider).writeDocument(writer, data);
            writer.close();
            return (Document)result.getNode();
        } catch (XMLStreamException e) {
            logger.error("Failed to serialize data {}", data, e);
            return null;
        }
    }

    private static final Element createElementFor(final Document doc, final QName qname, final Object obj) {
        final Element ret;
        if (qname.getNamespace() != null) {
            ret = doc.createElementNS(qname.getNamespace().toString(), qname.getLocalName());
        } else {
            ret = doc.createElementNS(null, qname.getLocalName());
        }

        if (obj instanceof AttributesContainer) {
            final Map<QName, String> attrs = ((AttributesContainer)obj).getAttributes();

            if (attrs != null) {
                for (Entry<QName, String> attribute : attrs.entrySet()) {
                    ret.setAttributeNS(attribute.getKey().getNamespace().toString(), attribute.getKey().getLocalName(),
                            attribute.getValue());
                }
            }
        }

        return ret;
    }

    public static Element createElementFor(final Document doc, final Node<?> data) {
        return createElementFor(doc, data.getNodeType(), data);
    }

    public static Element createElementFor(final Document doc, final NormalizedNode<?, ?> data) {
        return createElementFor(doc, data.getNodeType(), data);
    }

    public static Node<?> toDomNode(final Element xmlElement, final Optional<DataSchemaNode> schema,
            final Optional<XmlCodecProvider> codecProvider) {
        if (schema.isPresent()) {
            return toNodeWithSchema(xmlElement, schema.get(), codecProvider.or(XmlUtils.DEFAULT_XML_CODEC_PROVIDER));
        }
        return toDomNode(xmlElement);
    }

    public static QName qNameFromElement(final Element xmlElement) {
        String namespace = xmlElement.getNamespaceURI();
        String localName = xmlElement.getLocalName();
        return QName.create(namespace != null ? URI.create(namespace) : null, null, localName);
    }

    private static Node<?> toNodeWithSchema(final Element xmlElement, final DataSchemaNode schema, final XmlCodecProvider codecProvider,final SchemaContext schemaCtx) {
        checkQName(xmlElement, schema.getQName());
        if (schema instanceof DataNodeContainer) {
            return toCompositeNodeWithSchema(xmlElement, schema.getQName(), (DataNodeContainer) schema, codecProvider,schemaCtx);
        } else if (schema instanceof LeafSchemaNode) {
            return toSimpleNodeWithType(xmlElement, (LeafSchemaNode) schema, codecProvider,schemaCtx);
        } else if (schema instanceof LeafListSchemaNode) {
            return toSimpleNodeWithType(xmlElement, (LeafListSchemaNode) schema, codecProvider,schemaCtx);
        }
        return null;
    }

    private static Node<?> toNodeWithSchema(final Element xmlElement, final DataSchemaNode schema, final XmlCodecProvider codecProvider) {
        return toNodeWithSchema(xmlElement, schema, codecProvider, null);
    }

    protected static Node<?> toSimpleNodeWithType(final Element xmlElement, final LeafSchemaNode schema,
            final XmlCodecProvider codecProvider,final SchemaContext schemaCtx) {
        TypeDefinitionAwareCodec<? extends Object, ? extends TypeDefinition<?>> codec = codecProvider.codecFor(schema.getType());
        String text = xmlElement.getTextContent();
        Object value = null;
        if (codec != null) {
            value = codec.deserialize(text);
        }

        if (schema.getType() instanceof org.opendaylight.yangtools.yang.model.util.InstanceIdentifier) {
            value = InstanceIdentifierForXmlCodec.deserialize(xmlElement,schemaCtx);
        } else if(schema.getType() instanceof IdentityrefTypeDefinition){
            value = InstanceIdentifierForXmlCodec.toIdentity(xmlElement.getTextContent(), xmlElement, schemaCtx);
        }

        if (value == null) {
            value = xmlElement.getTextContent();
        }

        Optional<ModifyAction> modifyAction = getModifyOperationFromAttributes(xmlElement);
        return new SimpleNodeTOImpl<>(schema.getQName(), null, value, modifyAction.orNull());
    }

    private static Node<?> toSimpleNodeWithType(final Element xmlElement, final LeafListSchemaNode schema,
            final XmlCodecProvider codecProvider,final SchemaContext schemaCtx) {
        TypeDefinitionAwareCodec<? extends Object, ? extends TypeDefinition<?>> codec = codecProvider.codecFor(schema.getType());
        String text = xmlElement.getTextContent();
        Object value = null;
        if (codec != null) {
            value = codec.deserialize(text);
        }
        if (schema.getType() instanceof org.opendaylight.yangtools.yang.model.util.InstanceIdentifier) {
            value = InstanceIdentifierForXmlCodec.deserialize(xmlElement,schemaCtx);
        }
        if (value == null) {
            value = xmlElement.getTextContent();
        }

        Optional<ModifyAction> modifyAction = getModifyOperationFromAttributes(xmlElement);
        return new SimpleNodeTOImpl<>(schema.getQName(), null, value, modifyAction.orNull());
    }

    private static Node<?> toCompositeNodeWithSchema(final Element xmlElement, final QName qName, final DataNodeContainer schema,
            final XmlCodecProvider codecProvider,final SchemaContext schemaCtx) {
        List<Node<?>> values = toDomNodes(xmlElement, Optional.fromNullable(schema.getChildNodes()),schemaCtx);
        Optional<ModifyAction> modifyAction = getModifyOperationFromAttributes(xmlElement);
        return ImmutableCompositeNode.create(qName, values, modifyAction.orNull());
    }

    public static Optional<ModifyAction> getModifyOperationFromAttributes(final Element xmlElement) {
        Attr attributeNodeNS = xmlElement.getAttributeNodeNS(OPERATION_ATTRIBUTE_QNAME.getNamespace().toString(), OPERATION_ATTRIBUTE_QNAME.getLocalName());
        if(attributeNodeNS == null) {
            return Optional.absent();
        }

        ModifyAction action = ModifyAction.fromXmlValue(attributeNodeNS.getValue());
        Preconditions.checkArgument(action.isOnElementPermitted(), "Unexpected operation %s on %s", action, xmlElement);

        return Optional.of(action);
    }

    private static void checkQName(final Element xmlElement, final QName qName) {
        checkState(Objects.equal(xmlElement.getNamespaceURI(), qName.getNamespace().toString()));
        checkState(qName.getLocalName().equals(xmlElement.getLocalName()));
    }

    public static final Optional<DataSchemaNode> findFirstSchema(final QName qname, final Iterable<DataSchemaNode> dataSchemaNode) {
        if (dataSchemaNode != null && qname != null) {
            for (DataSchemaNode dsn : dataSchemaNode) {
                if (qname.isEqualWithoutRevision(dsn.getQName())) {
                    return Optional.<DataSchemaNode> of(dsn);
                } else if (dsn instanceof ChoiceNode) {
                    for (ChoiceCaseNode choiceCase : ((ChoiceNode) dsn).getCases()) {
                        Optional<DataSchemaNode> foundDsn = findFirstSchema(qname, choiceCase.getChildNodes());
                        if (foundDsn != null && foundDsn.isPresent()) {
                            return foundDsn;
                        }
                    }
                }
            }
        }
        return Optional.absent();
    }

    public static Node<?> toDomNode(final Document doc) {
        return toDomNode(doc.getDocumentElement());
    }

    private static Node<?> toDomNode(final Element element) {
        QName qname = qNameFromElement(element);

        ImmutableList.Builder<Node<?>> values = ImmutableList.<Node<?>> builder();
        NodeList nodes = element.getChildNodes();
        boolean isSimpleObject = true;
        String value = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Node child = nodes.item(i);
            if (child instanceof Element) {
                isSimpleObject = false;
                values.add(toDomNode((Element) child));
            }
            if (isSimpleObject && child instanceof org.w3c.dom.Text) {
                value = element.getTextContent();
                if (!Strings.isNullOrEmpty(value)) {
                    isSimpleObject = true;
                }
            }
        }
        if (isSimpleObject) {
            return new SimpleNodeTOImpl<>(qname, null, value);
        }
        return ImmutableCompositeNode.create(qname, values.build());
    }

    public static List<Node<?>> toDomNodes(final Element element, final Optional<? extends Iterable<DataSchemaNode>> context, final SchemaContext schemaCtx) {
        return forEachChild(element.getChildNodes(),schemaCtx, new Function<ElementWithSchemaContext, Optional<Node<?>>>() {

            @Override
            public Optional<Node<?>> apply(final ElementWithSchemaContext input) {
                if (context.isPresent()) {
                    QName partialQName = qNameFromElement(input.getElement());
                    Optional<DataSchemaNode> schemaNode = findFirstSchema(partialQName, context.get());
                    if (schemaNode.isPresent()) {
                        return Optional.<Node<?>> fromNullable(
                                toNodeWithSchema(input.getElement(), schemaNode.get(), XmlUtils.DEFAULT_XML_CODEC_PROVIDER, input.getSchemaContext()));
                    }
                }
                return Optional.<Node<?>> fromNullable(toDomNode(input.getElement()));
            }

        });

    }

    public static List<Node<?>> toDomNodes(final Element element, final Optional<? extends Iterable<DataSchemaNode>> context) {
        return toDomNodes(element, context, null);
    }

    /**
     * Converts XML Document containing notification data from Netconf device to
     * Data DOM Nodes. <br>
     * By specification defined in <a
     * href="http://tools.ietf.org/search/rfc6020#section-7.14">RFC 6020</a>
     * there are xml elements containing notifications metadata, like eventTime
     * or root notification element which specifies namespace for which is
     * notification defined in yang model. Those elements MUST be stripped off
     * notifications body. This method returns pure notification body which
     * begins in element which is equal to notifications name defined in
     * corresponding yang model. Rest of notification metadata are obfuscated,
     * thus Data DOM contains only pure notification body.
     *
     * @param document
     *            XML Document containing notification body
     * @param notifications
     *            Notifications Definition Schema
     * @return Data DOM Nodes containing xml notification body definition or
     *         <code>null</code> if there is no NotificationDefinition with
     *         Element with equal notification QName defined in XML Document.
     */
    public static CompositeNode notificationToDomNodes(final Document document,
            final Optional<Set<NotificationDefinition>> notifications, final SchemaContext schemaCtx) {
        if (notifications.isPresent() && (document != null) && (document.getDocumentElement() != null)) {
            final NodeList originChildNodes = document.getDocumentElement().getChildNodes();
            for (int i = 0; i < originChildNodes.getLength(); i++) {
                org.w3c.dom.Node child = originChildNodes.item(i);
                if (child instanceof Element) {
                    final Element childElement = (Element) child;
                    final QName partialQName = qNameFromElement(childElement);
                    final Optional<NotificationDefinition> notificationDef = findNotification(partialQName,
                            notifications.get());
                    if (notificationDef.isPresent()) {
                        final Iterable<DataSchemaNode> dataNodes = notificationDef.get().getChildNodes();
                        final List<Node<?>> domNodes = toDomNodes(childElement,
                                Optional.<Iterable<DataSchemaNode>> fromNullable(dataNodes),schemaCtx);
                        return ImmutableCompositeNode.create(notificationDef.get().getQName(), domNodes);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Transforms XML Document representing Rpc output body into Composite Node structure based on Rpc definition
     * within given Schema Context. The transformation is based on Rpc Definition which is defined in provided Schema Context.
     * If Rpc Definition is missing from given Schema Context the method will return <code>null</code>
     *
     * @param document XML Document containing Output RPC message
     * @param rpcName Rpc QName
     * @param context Schema Context
     * @return Rpc message in Composite Node data structures if Rpc definition is present within provided Schema Context, otherwise
     * returns <code>null</code>
     */
    public static CompositeNode rpcReplyToDomNodes(final Document document, final QName rpcName,
        final SchemaContext context) {
        Preconditions.checkNotNull(document);
        Preconditions.checkNotNull(rpcName);
        Preconditions.checkNotNull(context);

        Optional<RpcDefinition> rpcDefinition = findRpc(rpcName, context);
        if (rpcDefinition.isPresent()) {
             RpcDefinition rpc = rpcDefinition.get();

            final Collection<DataSchemaNode> outputNode = rpc.getOutput().getChildNodes();
            final Element rpcReplyElement = document.getDocumentElement();
            final QName partialQName = qNameFromElement(rpcReplyElement);

            if (RPC_REPLY_QNAME.equals(partialQName)) {
                final List<Node<?>> domNodes = toDomNodes(rpcReplyElement, Optional.fromNullable(outputNode), context);
                List<Node<?>> rpcOutNodes = Collections.<Node<?>>singletonList(ImmutableCompositeNode.create(
                    rpc.getOutput().getQName(), domNodes));
                return ImmutableCompositeNode.create(rpcName, rpcOutNodes);
            }
        }
        return null;
    }

    /**
     * Method searches given schema context for Rpc Definition with given QName.
     * Returns Rpc Definition if is present within given Schema Context, otherwise returns Optional.absent().
     *
     * @param rpc Rpc QName
     * @param context Schema Context
     * @return Rpc Definition if is present within given Schema Context, otherwise returns Optional.absent().
     */
    private static Optional<RpcDefinition> findRpc(QName rpc, SchemaContext context) {
        Preconditions.checkNotNull(rpc);
        Preconditions.checkNotNull(context);
        for (final RpcDefinition rpcDefinition : context.getOperations()) {
            if ((rpcDefinition != null) && rpc.equals(rpcDefinition.getQName())) {
                return Optional.of(rpcDefinition);
            }
        }
        return Optional.absent();
    }

    public static CompositeNode notificationToDomNodes(final Document document,
            final Optional<Set<NotificationDefinition>> notifications) {
        return notificationToDomNodes(document, notifications,null);
    }

    private static Optional<NotificationDefinition> findNotification(final QName notifName,
            final Set<NotificationDefinition> notifications) {
        if ((notifName != null) && (notifications != null)) {
            for (final NotificationDefinition notification : notifications) {
                if ((notification != null) && notifName.isEqualWithoutRevision(notification.getQName())) {
                    return Optional.<NotificationDefinition>fromNullable(notification);
                }
            }
        }
        return Optional.<NotificationDefinition>absent();
    }

    private static final <T> List<T> forEachChild(final NodeList nodes, final SchemaContext schemaContext, final Function<ElementWithSchemaContext, Optional<T>> forBody) {
        final int l = nodes.getLength();
        if (l == 0) {
            return ImmutableList.of();
        }

        final List<T> list = new ArrayList<>(l);
        for (int i = 0; i < l; i++) {
            org.w3c.dom.Node child = nodes.item(i);
            if (child instanceof Element) {
                Optional<T> result = forBody.apply(new ElementWithSchemaContext((Element) child,schemaContext));
                if (result.isPresent()) {
                    list.add(result.get());
                }
            }
        }
        return ImmutableList.copyOf(list);
    }

    public static final XmlCodecProvider defaultValueCodecProvider() {
        return XmlUtils.DEFAULT_XML_CODEC_PROVIDER;
    }
}
