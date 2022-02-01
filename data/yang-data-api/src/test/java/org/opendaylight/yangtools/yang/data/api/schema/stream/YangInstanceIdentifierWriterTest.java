/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.api.schema.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerLike;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

@ExtendWith(MockitoExtension.class)
public class YangInstanceIdentifierWriterTest {
    @Test
    public void testYangInstanceIdentifierWriter() throws IOException {
        final FormattingNormalizedNodeStreamWriter streamWriter = new FormattingNormalizedNodeStreamWriter();

        final DataNodeContainer root = mock(DataNodeContainer.class);

        final ContainerLike containerSchema1 = mock(ContainerLike.class);
        final ContainerLike containerSchema2 = mock(ContainerLike.class);
        final ContainerLike containerSchema3 = mock(ContainerLike.class);

        doReturn(containerSchema1).when(root).dataChildByName(any());
        doReturn(containerSchema2).when(containerSchema1).dataChildByName(any());
        doReturn(containerSchema3).when(containerSchema2).dataChildByName(any());

        final YangInstanceIdentifier path = YangInstanceIdentifier.builder()
            .node(QName.create("test", "container-1"))
            .node(QName.create("test", "container-2"))
            .node(QName.create("test", "container-3"))
            .build();

        try (var iidWriter = YangInstanceIdentifierWriter.open(streamWriter, root, path)) {
            try (var nnWriter = new NormalizedNodeWriter(streamWriter)) {
                nnWriter.write(mockedPayload());
            }
        }

        assertEquals("(test)container-1(container)\n"
            + "  (test)container-2(container)\n"
            + "    (test)container-3(container)\n"
            + "      (test)payload-container(container)\n"
            + "        (test)payload-leaf(leaf)\n"
            + "          (String)=leaf-value\n"
            + "        (end)\n"
            + "      (end)\n"
            + "    (end)\n"
            + "  (end)\n"
            + "(end)\n", streamWriter.result());
    }

    @Test
    public void testAugmentationIdentifier() throws IOException {
        final FormattingNormalizedNodeStreamWriter streamWriter = new FormattingNormalizedNodeStreamWriter();

        final ContainerLike root = mock(ContainerLike.class);
        final QName augmented = QName.create("augment-namespace", "augmented-container");
        final QName container2Qname = QName.create("augment-namespace", "container-2");

        final ContainerLike containerSchema1 = mock(ContainerLike.class);
        final AugmentationSchemaNode augmentationSchema = mock(AugmentationSchemaNode.class);
        final ContainerLike augmentedContainerSchema = mock(ContainerLike.class);
        final ContainerLike containerSchema2 = mock(ContainerLike.class);

        doReturn(containerSchema1).when(root).dataChildByName(any());

        doReturn(Set.of(augmentationSchema)).when(containerSchema1).getAvailableAugmentations();
        doReturn(augmentedContainerSchema).when(augmentationSchema).dataChildByName(augmented);

        doReturn(Set.of(augmentedContainerSchema)).when(augmentationSchema).getChildNodes();
        doReturn(augmented).when(augmentedContainerSchema).getQName();

        doReturn(containerSchema2).when(augmentedContainerSchema).dataChildByName(any());

        final YangInstanceIdentifier path = YangInstanceIdentifier.builder()
            .node(QName.create("test", "container-1"))
            .node(AugmentationIdentifier.create(Set.of(augmented)))
            .node(augmented)
            .node(container2Qname)
            .build();

        try (var iidWriter = YangInstanceIdentifierWriter.open(streamWriter, root, path)) {
            try (var nnWriter = new NormalizedNodeWriter(streamWriter)) {
                nnWriter.write(mockedPayload());
            }
        }

        assertEquals("(test)container-1(container)\n"
            + "  AugmentationIdentifier{childNames=[(augment-namespace)augmented-container]}(augmentation)\n"
            + "    (augment-namespace)augmented-container(container)\n"
            + "      (augment-namespace)container-2(container)\n"
            + "        (test)payload-container(container)\n"
            + "          (test)payload-leaf(leaf)\n"
            + "            (String)=leaf-value\n"
            + "          (end)\n"
            + "        (end)\n"
            + "      (end)\n"
            + "    (end)\n"
            + "  (end)\n"
            + "(end)\n", streamWriter.result());
    }

    @Test
    public void testMapIdentifier() throws IOException {
        final FormattingNormalizedNodeStreamWriter streamWriter = new FormattingNormalizedNodeStreamWriter();

        final ContainerLike root = mock(ContainerLike.class);
        final ListSchemaNode listSchemaNode = mock(ListSchemaNode.class);

        final MapEntryNode listEntry = mock(MapEntryNode.class);

        final ContainerLike containerSchema1 = mock(ContainerLike.class);
        final QName container1Qname = QName.create("test", "container-1");
        final QName list1KeyQname = QName.create("test", "list-1-key");
        final QName listQname = QName.create("test", "list-1");

        doReturn(listQname).when(listSchemaNode).getQName();
        doReturn(listSchemaNode).when(root).dataChildByName(any());
        doReturn(List.of(list1KeyQname)).when(listSchemaNode).getKeyDefinition();
        doReturn(containerSchema1).when(listSchemaNode).dataChildByName(container1Qname);
        doReturn("test-list-entry").when(listEntry).toString();

        final YangInstanceIdentifier path = YangInstanceIdentifier.builder()
            .node(listQname)
            .nodeWithKey(listQname,list1KeyQname, listEntry)
            .node(container1Qname)
            .build();

        try (var iidWriter = YangInstanceIdentifierWriter.open(streamWriter, root, path)) {
            try (var nnWriter = new NormalizedNodeWriter(streamWriter)) {
                nnWriter.write(mockedPayload());
            }
        }

        assertEquals("(test)list-1(key)\n"
            + "  (test)list-1[{(test)list-1-key=test-list-entry}][](key)\n"
            + "    (test)container-1(container)\n"
            + "      (test)payload-container(container)\n"
            + "        (test)payload-leaf(leaf)\n"
            + "          (String)=leaf-value\n"
            + "        (end)\n"
            + "      (end)\n"
            + "    (end)\n"
            + "  (end)\n"
            + "(end)\n", streamWriter.result());
    }

    @Test
    public void testChoiceIdentifier() throws IOException {
        final FormattingNormalizedNodeStreamWriter streamWriter = new FormattingNormalizedNodeStreamWriter();
        final ContainerLike root = mock(ContainerLike.class);
        final ChoiceSchemaNode choiceSchemaNode = mock(ChoiceSchemaNode.class);
        final CaseSchemaNode caseSchemaNode = mock(CaseSchemaNode.class);
        final ContainerLike caseContainer = mock(ContainerLike.class);

        final QName choiceQname = QName.create("test", "choice-node");
        final QName caseQname = QName.create("test", "container-in-case");

        doReturn(choiceSchemaNode).when(root).dataChildByName(choiceQname);
        doReturn(Set.of(caseSchemaNode)).when(choiceSchemaNode).getCases();
        doCallRealMethod().when(choiceSchemaNode).findDataSchemaChild(any());
        doReturn(Optional.of(caseContainer)).when(caseSchemaNode).findDataChildByName(any());

        final YangInstanceIdentifier path = YangInstanceIdentifier.builder()
            .node(choiceQname)
            .node(caseQname)
            .build();

        try (var iidWriter = YangInstanceIdentifierWriter.open(streamWriter, root, path)) {
            try (var nnWriter = new NormalizedNodeWriter(streamWriter)) {
                nnWriter.write(mockedPayload());
            }
        }

        assertEquals("(test)choice-node(choice)\n"
            + "  (test)container-in-case(container)\n"
            + "    (test)payload-container(container)\n"
            + "      (test)payload-leaf(leaf)\n"
            + "        (String)=leaf-value\n"
            + "      (end)\n"
            + "    (end)\n"
            + "  (end)\n"
            + "(end)\n", streamWriter.result());
    }

    @Test
    public void testLeafSetIdentifier() throws IOException {
        final FormattingNormalizedNodeStreamWriter streamWriter = new FormattingNormalizedNodeStreamWriter();

        final ContainerLike root = mock(ContainerLike.class);
        final LeafListSchemaNode leafSetSchema = mock(LeafListSchemaNode.class);

        doReturn(leafSetSchema).when(root).dataChildByName(any());

        final YangInstanceIdentifier path = YangInstanceIdentifier.builder()
            .node(QName.create("test", "list-list"))
            .build();

        try (var iidWriter = YangInstanceIdentifierWriter.open(streamWriter, root, path)) {
            try (var nnWriter = new NormalizedNodeWriter(streamWriter)) {
                final QName leafQname = QName.create("test", "leaf");

                final LeafSetEntryNode<?> leafNode = mock(LeafSetEntryNode.class);
                doReturn(new NodeWithValue<>(leafQname, "test-value")).when(leafNode).getIdentifier();
                doReturn("test-value").when(leafNode).body();
                nnWriter.write(leafNode);

                final LeafSetEntryNode<?> leafNode2 = mock(LeafSetEntryNode.class);
                doReturn(new NodeWithValue<>(leafQname, "test-value-2")).when(leafNode2).getIdentifier();
                doReturn("test-value-2").when(leafNode2).body();
                nnWriter.write(leafNode2);
            }
        }

        assertEquals("(test)list-list(leaf-list)\n"
            + "  (test)leaf(entry)\n"
            + "    (String)=test-value\n"
            + "  (end)\n"
            + "  (test)leaf(entry)\n"
            + "    (String)=test-value-2\n"
            + "  (end)\n"
            + "(end)\n", streamWriter.result());
    }

    private static NormalizedNode mockedPayload() {
        final ContainerNode containerNode = mock(ContainerNode.class);
        final LeafNode<?> leafNode = mock(LeafNode.class);

        doReturn(new NodeIdentifier(QName.create("test", "payload-container"))).when(containerNode).getIdentifier();
        doReturn(Set.of(leafNode)).when(containerNode).body();
        doReturn(new NodeIdentifier(QName.create("test", "payload-leaf"))).when(leafNode).getIdentifier();
        doReturn("leaf-value").when(leafNode).body();

        return containerNode;
    }
}