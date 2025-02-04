/*
 * Copyright (c) 2016 Intel Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.codec.xml;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class UnionXmlCodec<T> implements XmlCodec<T> {
    private static final class Diverse extends UnionXmlCodec<Object> {
        Diverse(final List<XmlCodec<?>> codecs) {
            super(codecs);
        }

        @Override
        public Class<Object> getDataType() {
            return Object.class;
        }
    }

    private static final class SingleType<T> extends UnionXmlCodec<T> {
        private final Class<T> dataClass;

        SingleType(final Class<T> dataClass, final List<XmlCodec<?>> codecs) {
            super(codecs);
            this.dataClass = requireNonNull(dataClass);
        }

        @Override
        public Class<T> getDataType() {
            return dataClass;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(UnionXmlCodec.class);

    private final ImmutableList<XmlCodec<?>> codecs;

    UnionXmlCodec(final List<XmlCodec<?>> codecs) {
        this.codecs = ImmutableList.copyOf(codecs);
    }

    static UnionXmlCodec<?> create(final UnionTypeDefinition type, final List<XmlCodec<?>> codecs) {
        final Iterator<XmlCodec<?>> it = codecs.iterator();
        verify(it.hasNext(), "Union %s has no subtypes", type);

        Class<?> dataClass = it.next().getDataType();
        while (it.hasNext()) {
            final Class<?> next = it.next().getDataType();
            if (!dataClass.equals(next)) {
                LOG.debug("Type {} has diverse data classes: {} and {}", type, dataClass, next);
                return new Diverse(codecs);
            }
        }

        LOG.debug("Type {} has single data class {}", type, dataClass);
        return new SingleType<>(dataClass, codecs);
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    public final T parseValue(final NamespaceContext ctx, final String str) {
        final var suppressed = new ArrayList<RuntimeException>();

        for (XmlCodec<?> codec : codecs) {
            final Object ret;
            try {
                ret = codec.parseValue(ctx, str);
            } catch (RuntimeException e) {
                LOG.debug("Codec {} did not accept input '{}'", codec, str, e);
                suppressed.add(e);
                continue;
            }

            return getDataType().cast(ret);
        }

        final var ex = new IllegalArgumentException("Invalid value \"" + str + "\" for union type.");
        suppressed.forEach(ex::addSuppressed);
        throw ex;
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    public void writeValue(final XMLStreamWriter ctx, final Object value) throws XMLStreamException {
        for (XmlCodec<?> codec : codecs) {
            if (!codec.getDataType().isInstance(value)) {
                LOG.debug("Codec {} cannot accept input {}, skipping it", codec, value);
                continue;
            }

            @SuppressWarnings("unchecked")
            final XmlCodec<Object> objCodec = (XmlCodec<Object>) codec;
            try {
                objCodec.writeValue(ctx, value);
                return;
            } catch (RuntimeException e) {
                LOG.debug("Codec {} failed to serialize {}", codec, value, e);
            }
        }

        throw new IllegalArgumentException("No codec would accept value \"" + value + "\"");
    }
}
