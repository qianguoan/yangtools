/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.codec.binfmt;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import java.io.DataInput;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Either;
import org.opendaylight.yangtools.util.ImmutableOffsetMapTemplate;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * NormalizedNodeInputStreamReader reads the byte stream and constructs the normalized node including its children
 * nodes. This process goes in recursive manner, where each NodeTypes object signifies the start of the object, except
 * END_NODE. If a node can have children, then that node's end is calculated based on appearance of END_NODE.
 */
abstract class AbstractLithiumDataInput extends AbstractLegacyDataInput {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLithiumDataInput.class);

    private final List<String> codedStringMap = new ArrayList<>();

    private QName lastLeafSetQName;

    AbstractLithiumDataInput(final DataInput input) {
        super(input);
    }

    @Override
    public final void streamNormalizedNode(final NormalizedNodeStreamWriter writer) throws IOException {
        streamNormalizedNode(requireNonNull(writer), input.readByte());
    }

    private void streamNormalizedNode(final NormalizedNodeStreamWriter writer, final byte nodeType) throws IOException {
        switch (nodeType) {
            case LithiumNode.ANY_XML_NODE -> streamAnyxml(writer);
            case LithiumNode.AUGMENTATION_NODE -> streamAugmentation(writer);
            case LithiumNode.CHOICE_NODE -> streamChoice(writer);
            case LithiumNode.CONTAINER_NODE -> streamContainer(writer);
            case LithiumNode.LEAF_NODE -> streamLeaf(writer);
            case LithiumNode.LEAF_SET -> streamLeafSet(writer);
            case LithiumNode.ORDERED_LEAF_SET -> streamOrderedLeafSet(writer);
            case LithiumNode.LEAF_SET_ENTRY_NODE -> streamLeafSetEntry(writer);
            case LithiumNode.MAP_ENTRY_NODE -> streamMapEntry(writer);
            case LithiumNode.MAP_NODE -> streamMap(writer);
            case LithiumNode.ORDERED_MAP_NODE -> streamOrderedMap(writer);
            case LithiumNode.UNKEYED_LIST -> streamUnkeyedList(writer);
            case LithiumNode.UNKEYED_LIST_ITEM -> streamUnkeyedListItem(writer);
            default -> throw new InvalidNormalizedNodeStreamException("Unexpected node " + nodeType);
        }
    }

    private void streamAnyxml(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming anyxml node {}", identifier);

        final DOMSource value = readDOMSource();
        if (writer.startAnyxmlNode(identifier, DOMSource.class)) {
            writer.domSourceValue(value);
            writer.endNode();
        }
    }

    private void streamAugmentation(final NormalizedNodeStreamWriter writer) throws IOException {
        final var augIdentifier = readAugmentationIdentifier();
        LOG.trace("Streaming augmentation node {}", augIdentifier);
        for (byte nodeType = input.readByte(); nodeType != LithiumNode.END_NODE; nodeType = input.readByte()) {
            streamNormalizedNode(writer, nodeType);
        }
    }

    private void streamChoice(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming choice node {}", identifier);
        writer.startChoiceNode(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void streamContainer(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming container node {}", identifier);
        writer.startContainerNode(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void streamLeaf(final NormalizedNodeStreamWriter writer) throws IOException {
        startLeaf(writer);
        endLeaf(writer, readObject());
    }

    // Leaf inside a MapEntryNode, it can potentially be a key leaf, in which case we want to de-duplicate values.
    private void streamLeaf(final NormalizedNodeStreamWriter writer, final NodeIdentifierWithPredicates entryId)
            throws IOException {
        final NodeIdentifier identifier = startLeaf(writer);
        final Object value = readObject();
        final Object entryValue = entryId.getValue(identifier.getNodeType());
        endLeaf(writer, entryValue == null ? value : entryValue);
    }

    private NodeIdentifier startLeaf(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming leaf node {}", identifier);
        writer.startLeafNode(identifier);
        return identifier;
    }

    private static void endLeaf(final NormalizedNodeStreamWriter writer, final Object value) throws IOException {
        writer.scalarValue(value);
        writer.endNode();
    }

    private void streamLeafSet(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming leaf set node {}", identifier);
        writer.startLeafSet(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamLeafSet(writer, identifier);
    }

    private void streamOrderedLeafSet(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming ordered leaf set node {}", identifier);
        writer.startOrderedLeafSet(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamLeafSet(writer, identifier);
    }

    private void commonStreamLeafSet(final NormalizedNodeStreamWriter writer, final NodeIdentifier identifier)
            throws IOException {
        lastLeafSetQName = identifier.getNodeType();
        try {
            commonStreamContainer(writer);
        } finally {
            // Make sure we never leak this
            lastLeafSetQName = null;
        }
    }

    private void streamLeafSetEntry(final NormalizedNodeStreamWriter writer) throws IOException {
        final QName name = lastLeafSetQName != null ? lastLeafSetQName : readQName();
        final Object value = readObject();
        final NodeWithValue<Object> leafIdentifier = new NodeWithValue<>(name, value);
        LOG.trace("Streaming leaf set entry node {}, value {}", leafIdentifier, value);
        writer.startLeafSetEntryNode(leafIdentifier);
        writer.scalarValue(value);
        writer.endNode();
    }

    private void streamMap(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming map node {}", identifier);
        writer.startMapNode(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void streamOrderedMap(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming ordered map node {}", identifier);
        writer.startOrderedMapNode(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void streamMapEntry(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifierWithPredicates entryIdentifier = readNormalizedNodeWithPredicates();
        LOG.trace("Streaming map entry node {}", entryIdentifier);
        writer.startMapEntryNode(entryIdentifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);

        // Same loop as commonStreamContainer(), but ...
        for (byte nodeType = input.readByte(); nodeType != LithiumNode.END_NODE; nodeType = input.readByte()) {
            if (nodeType == LithiumNode.LEAF_NODE) {
                // ... leaf nodes may need de-duplication
                streamLeaf(writer, entryIdentifier);
            } else {
                streamNormalizedNode(writer, nodeType);
            }
        }
        writer.endNode();
    }

    private void streamUnkeyedList(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming unkeyed list node {}", identifier);
        writer.startUnkeyedList(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void streamUnkeyedListItem(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming unkeyed list item node {}", identifier);
        writer.startUnkeyedListItem(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void commonStreamContainer(final NormalizedNodeStreamWriter writer) throws IOException {
        for (byte nodeType = input.readByte(); nodeType != LithiumNode.END_NODE; nodeType = input.readByte()) {
            streamNormalizedNode(writer, nodeType);
        }
        writer.endNode();
    }

    private DOMSource readDOMSource() throws IOException {
        String xml = readObject().toString();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Element node = factory.newDocumentBuilder().parse(
                    new InputSource(new StringReader(xml))).getDocumentElement();
            return new DOMSource(node);
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException("Error parsing XML: " + xml, e);
        }
    }

    final QName defaultReadQName() throws IOException {
        // Read in the same sequence of writing
        String localName = readCodedString();
        String namespace = readCodedString();
        String revision = Strings.emptyToNull(readCodedString());

        return QNameFactory.create(localName, namespace, revision);
    }

    final String readCodedString() throws IOException {
        final byte valueType = input.readByte();
        return switch (valueType) {
            case LithiumTokens.IS_NULL_VALUE -> null;
            case LithiumTokens.IS_CODE_VALUE -> {
                final int code = input.readInt();
                try {
                    yield codedStringMap.get(code);
                } catch (IndexOutOfBoundsException e) {
                    throw new IOException("String code " + code + " was not found", e);
                }
            }
            case LithiumTokens.IS_STRING_VALUE -> {
                final String value = input.readUTF().intern();
                codedStringMap.add(value);
                yield value;
            }
            default -> throw new IOException("Unhandled string value type " + valueType);
        };
    }

    private ImmutableSet<QName> readQNameSet() throws IOException {
        // Read the children count
        final int count = input.readInt();
        final var children = ImmutableSet.<QName>builderWithExpectedSize(count);
        for (int i = 0; i < count; i++) {
            children.add(readQName());
        }
        return children.build();
    }

    abstract @NonNull LegacyAugmentationIdentifier readAugmentationIdentifier() throws IOException;

    abstract @NonNull NodeIdentifier readNodeIdentifier() throws IOException;

    final @NonNull LegacyAugmentationIdentifier defaultReadAugmentationIdentifier() throws IOException {
        return new LegacyAugmentationIdentifier(readQNameSet());
    }

    private @NonNull NodeIdentifierWithPredicates readNormalizedNodeWithPredicates() throws IOException {
        final QName qname = readQName();
        final int count = input.readInt();
        switch (count) {
            case 0:
                return NodeIdentifierWithPredicates.of(qname);
            case 1:
                return NodeIdentifierWithPredicates.of(qname, readQName(), readObject());
            default:
                // ImmutableList is used by ImmutableOffsetMapTemplate for lookups, hence we use that.
                final Builder<QName> keys = ImmutableList.builderWithExpectedSize(count);
                final Object[] values = new Object[count];
                for (int i = 0; i < count; i++) {
                    keys.add(readQName());
                    values[i] = readObject();
                }

                return NodeIdentifierWithPredicates.of(qname, ImmutableOffsetMapTemplate.ordered(keys.build())
                    .instantiateWithValues(values));
        }
    }

    private Object readObject() throws IOException {
        byte objectType = input.readByte();
        return switch (objectType) {
            case LithiumValue.BITS_TYPE -> readObjSet();
            case LithiumValue.BOOL_TYPE -> input.readBoolean();
            case LithiumValue.BYTE_TYPE -> input.readByte();
            case LithiumValue.INT_TYPE -> input.readInt();
            case LithiumValue.LONG_TYPE -> input.readLong();
            case LithiumValue.QNAME_TYPE -> readQName();
            case LithiumValue.SHORT_TYPE -> input.readShort();
            case LithiumValue.STRING_TYPE -> input.readUTF();
            case LithiumValue.STRING_BYTES_TYPE -> readStringBytes();
            case LithiumValue.BIG_DECIMAL_TYPE -> Decimal64.valueOf(input.readUTF());
            case LithiumValue.BIG_INTEGER_TYPE -> new BigInteger(input.readUTF());
            case LithiumValue.BINARY_TYPE -> {
                byte[] bytes = new byte[input.readInt()];
                input.readFully(bytes);
                yield bytes;
            }
            case LithiumValue.YANG_IDENTIFIER_TYPE -> readYangInstanceIdentifierInternal();
            case LithiumValue.EMPTY_TYPE, LithiumValue.NULL_TYPE ->
            // Leaf nodes no longer allow null values and thus we no longer emit null values. Previously, the "empty"
            // yang type was represented as null so we translate an incoming null value to Empty. It was possible for
            // a BI user to set a string leaf to null and we're rolling the dice here but the chances for that are
            // very low. We'd have to know the yang type but, even if we did, we can't let a null value pass upstream
            // so we'd have to drop the leaf which might cause other issues.
                Empty.value();
            default -> null;
        };
    }

    private String readStringBytes() throws IOException {
        byte[] bytes = new byte[input.readInt()];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public final YangInstanceIdentifier readYangInstanceIdentifier() throws IOException {
        return readYangInstanceIdentifierInternal();
    }

    private @NonNull YangInstanceIdentifier readYangInstanceIdentifierInternal() throws IOException {
        int size = input.readInt();
        final Builder<PathArgument> pathArguments = ImmutableList.builderWithExpectedSize(size);
        for (int i = 0; i < size; i++) {
            pathArguments.add(readPathArgument());
        }
        return YangInstanceIdentifier.of(pathArguments.build());
    }

    private Set<String> readObjSet() throws IOException {
        int count = input.readInt();
        Set<String> children = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            children.add(readCodedString());
        }
        return children;
    }

    @Override
    @Deprecated(since = "11.0.0", forRemoval = true)
    public final Either<PathArgument, LegacyPathArgument> readLegacyPathArgument() throws IOException {
        // read Type
        int type = input.readByte();
        return switch (type) {
            case LithiumPathArgument.AUGMENTATION_IDENTIFIER -> Either.ofSecond(readAugmentationIdentifier());
            case LithiumPathArgument.NODE_IDENTIFIER -> Either.ofFirst(readNodeIdentifier());
            case LithiumPathArgument.NODE_IDENTIFIER_WITH_PREDICATES ->
                Either.ofFirst(readNormalizedNodeWithPredicates());
            case LithiumPathArgument.NODE_IDENTIFIER_WITH_VALUE ->
                Either.ofFirst(new NodeWithValue<>(readQName(), readObject()));
            default -> throw new InvalidNormalizedNodeStreamException("Unexpected PathArgument type " + type);
        };
    }
}
