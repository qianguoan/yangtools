/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.codec.binfmt;

import com.google.common.base.Strings;
import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;

/**
 * Lithium (or Oxygen really) specialization of AbstractLithiumDataInput.
 */
final class LithiumNormalizedNodeInputStreamReader extends AbstractLithiumDataInput {
    LithiumNormalizedNodeInputStreamReader(final DataInput input) {
        super(input);
    }

    @Override
    @Deprecated(since = "11.0.0", forRemoval = true)
    public NormalizedNodeStreamVersion getVersion() {
        return NormalizedNodeStreamVersion.LITHIUM;
    }

    @Override
    public QName readQName() throws IOException {
        // Read in the same sequence of writing
        String localName = readCodedString();
        String namespace = readCodedString();
        String revision = Strings.emptyToNull(readCodedString());

        return QNameFactory.create(localName, namespace, revision);
    }

    @Override
    LegacyAugmentationIdentifier readAugmentationIdentifier() throws IOException {
        return defaultReadAugmentationIdentifier();
    }

    @Override
    NodeIdentifier readNodeIdentifier() throws IOException {
        return new NodeIdentifier(readQName());
    }
}
