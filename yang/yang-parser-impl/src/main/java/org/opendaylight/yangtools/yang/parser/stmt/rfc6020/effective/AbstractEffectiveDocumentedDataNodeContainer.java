/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.stmt.rfc6020.effective;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;

import org.opendaylight.yangtools.yang.parser.spi.meta.StmtContext;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.UsesNode;

public abstract class AbstractEffectiveDocumentedDataNodeContainer<A, D extends DeclaredStatement<A>>
        extends AbstractEffectiveDocumentedNode<A, D> implements
        DataNodeContainer {

    private final Map<QName, DataSchemaNode> childNodes;
    private final Set<GroupingDefinition> groupings;
    private final Set<UsesNode> uses;
    private final Set<TypeDefinition<?>> typeDefinitions;
    private final Set<DataSchemaNode> publicChildNodes;

    protected AbstractEffectiveDocumentedDataNodeContainer(
            final StmtContext<A, D, ?> ctx) {
        super(ctx);

        Collection<? extends EffectiveStatement<?, ?>> effectiveSubstatements = effectiveSubstatements();

        childNodes = new HashMap<QName, DataSchemaNode>();
        groupings = new HashSet<GroupingDefinition>();
        uses = new HashSet<UsesNode>();
        typeDefinitions = new HashSet<TypeDefinition<?>>();
        publicChildNodes = new HashSet<DataSchemaNode>();

        for (EffectiveStatement<?, ?> effectiveStatement : effectiveSubstatements) {
            if (effectiveStatement instanceof DataSchemaNode) {
                DataSchemaNode dataSchemaNode = (DataSchemaNode) effectiveStatement;

                childNodes.put(dataSchemaNode.getQName(), dataSchemaNode);
                publicChildNodes.add(dataSchemaNode);
            }
            if (effectiveStatement instanceof UsesNode) {
                UsesNode usesNode = (UsesNode) effectiveStatement;
                uses.add(usesNode);
            }
            if (effectiveStatement instanceof TypeDefinition) {
                TypeDefinition<?> typeDef = (TypeDefinition<?>) effectiveStatement;
                typeDefinitions.add(typeDef);
            }
            if (effectiveStatement instanceof GroupingDefinition) {
                GroupingDefinition grp = (GroupingDefinition) effectiveStatement;
                groupings.add(grp);
            }
        }
    }

    @Override
    public final Set<TypeDefinition<?>> getTypeDefinitions() {
        return typeDefinitions;
    }

    @Override
    public final Set<DataSchemaNode> getChildNodes() {
        return publicChildNodes;
    }

    @Override
    public final Set<GroupingDefinition> getGroupings() {
        return groupings;
    }

    @Override
    public final DataSchemaNode getDataChildByName(final QName name) {
        // Child nodes are keyed by their container name, so we can do a direct
        // lookup
        return childNodes.get(name);
    }

    @Override
    public final DataSchemaNode getDataChildByName(final String name) {
        for (DataSchemaNode node : childNodes.values()) {
            if (node.getQName().getLocalName().equals(name)) {
                return node;
            }
        }
        return null;
    }

    @Override
    public Set<UsesNode> getUses() {
        return uses;
    }

}
