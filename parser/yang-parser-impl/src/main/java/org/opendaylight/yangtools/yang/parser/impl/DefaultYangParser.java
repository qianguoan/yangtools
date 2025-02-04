/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.FeatureSet;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceRepresentation;
import org.opendaylight.yangtools.yang.model.repo.api.YangIRSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.api.YinDomSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.api.YinTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.api.YinXmlSchemaSource;
import org.opendaylight.yangtools.yang.parser.api.YangParser;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangStatementStreamSource;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YinStatementStreamSource;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YinTextToDomTransformer;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.spi.source.StatementStreamSource;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor.BuildAction;
import org.xml.sax.SAXException;

final class DefaultYangParser implements YangParser {
    static final @NonNull Collection<Class<? extends SchemaSourceRepresentation>> REPRESENTATIONS =
            ImmutableList.of(YangIRSchemaSource.class, YangTextSchemaSource.class, YinDomSchemaSource.class,
                YinXmlSchemaSource.class, YinTextSchemaSource.class);

    private final BuildAction buildAction;

    DefaultYangParser(final BuildAction buildAction) {
        this.buildAction = requireNonNull(buildAction);
    }

    @Override
    public @NonNull Collection<Class<? extends SchemaSourceRepresentation>> supportedSourceRepresentations() {
        return REPRESENTATIONS;
    }

    @Override
    public @NonNull YangParser addSource(final SchemaSourceRepresentation source) throws IOException,
            YangSyntaxErrorException {
        buildAction.addSource(sourceToStatementStream(source));
        return this;
    }

    @Override
    public @NonNull YangParser addLibSource(final SchemaSourceRepresentation source) throws IOException,
            YangSyntaxErrorException {
        buildAction.addLibSource(sourceToStatementStream(source));
        return this;
    }

    @Override
    public @NonNull YangParser setSupportedFeatures(final FeatureSet supportedFeatures) {
        buildAction.setSupportedFeatures(supportedFeatures);
        return this;
    }

    @Override
    public @NonNull YangParser setModulesWithSupportedDeviations(
            final SetMultimap<QNameModule, QNameModule> modulesDeviatedByModules) {
        buildAction.setModulesWithSupportedDeviations(modulesDeviatedByModules);
        return this;
    }

    @Override
    public @NonNull List<DeclaredStatement<?>> buildDeclaredModel() throws YangParserException {
        try {
            return buildAction.build().getRootStatements();
        } catch (ReactorException e) {
            throw decodeReactorException(e);
        }
    }

    @Override
    public @NonNull EffectiveModelContext buildEffectiveModel() throws YangParserException {
        try {
            return buildAction.buildEffective();
        } catch (ReactorException e) {
            throw decodeReactorException(e);
        }
    }

    static YangParserException decodeReactorException(final ReactorException reported) {
        // FIXME: map exception in some reasonable manner
        return new YangParserException("Failed to assemble sources", reported);
    }

    static StatementStreamSource sourceToStatementStream(final SchemaSourceRepresentation source)
            throws IOException, YangSyntaxErrorException {
        requireNonNull(source);
        if (source instanceof YangIRSchemaSource) {
            return YangStatementStreamSource.create((YangIRSchemaSource) source);
        } else if (source instanceof YangTextSchemaSource) {
            return YangStatementStreamSource.create((YangTextSchemaSource) source);
        } else if (source instanceof YinDomSchemaSource) {
            return YinStatementStreamSource.create((YinDomSchemaSource) source);
        } else if (source instanceof YinTextSchemaSource) {
            try {
                return YinStatementStreamSource.create(YinTextToDomTransformer.transformSource(
                    (YinTextSchemaSource) source));
            } catch (SAXException e) {
                throw new YangSyntaxErrorException(source.getIdentifier(), 0, 0, "Failed to parse XML text", e);
            }
        } else if (source instanceof YinXmlSchemaSource) {
            try {
                return YinStatementStreamSource.create((YinXmlSchemaSource) source);
            } catch (TransformerException e) {
                throw new YangSyntaxErrorException(source.getIdentifier(), 0, 0,
                    "Failed to assemble in-memory representation", e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported source " + source);
        }
    }
}
