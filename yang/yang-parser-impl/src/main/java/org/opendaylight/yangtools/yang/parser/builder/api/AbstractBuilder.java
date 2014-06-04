/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.builder.api;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.parser.builder.impl.UnknownSchemaNodeBuilder;

import com.google.common.base.Preconditions;

/**
 * Base helper implementation of Builders for Yang Model elements.
 *
 */
public abstract class AbstractBuilder implements Builder {
    private String moduleName;
    private final int line;
    private Builder parentBuilder;

    protected final List<UnknownSchemaNode> unknownNodes = new ArrayList<>();
    protected final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    protected AbstractBuilder(final String moduleName, final int line) {
        this.moduleName = Preconditions.checkNotNull(moduleName,"moduleName must not be null");
        this.line = line;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    @Deprecated
    public void setModuleName(final String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public Builder getParent() {
        return parentBuilder;
    }

    @Override
    public void setParent(final Builder parentBuilder) {
        this.parentBuilder = parentBuilder;
    }

    @Override
    public List<UnknownSchemaNodeBuilder> getUnknownNodes() {
        return addedUnknownNodes;
    }

    @Override
    public void addUnknownNodeBuilder(UnknownSchemaNodeBuilder unknownNode) {
        addedUnknownNodes.add(unknownNode);
    }

}
