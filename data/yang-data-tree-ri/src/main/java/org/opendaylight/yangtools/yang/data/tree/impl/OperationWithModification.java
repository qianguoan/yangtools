/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.tree.impl;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.impl.node.TreeNode;
import org.opendaylight.yangtools.yang.data.tree.impl.node.Version;

final class OperationWithModification {
    private final ModificationApplyOperation applyOperation;
    private final ModifiedNode modification;

    private OperationWithModification(final ModificationApplyOperation op, final ModifiedNode mod) {
        applyOperation = requireNonNull(op);
        modification = requireNonNull(mod);
    }

    void write(final NormalizedNode value) {
        modification.write(value);
        /**
         * Fast validation of structure, full validation on written data will be run during seal.
         */
        applyOperation.quickVerifyStructure(value);
    }

    void merge(final NormalizedNode data, final Version version) {
        /*
         * A merge operation will end up overwriting parts of the tree, retaining others. We want to
         * make sure we do not validate the complete resulting structure, but rather just what was
         * written. In order to do that, we first pretend the data was written, run verification and
         * then perform the merge -- with the explicit assumption that adding the newly-validated
         * data with the previously-validated data will not result in invalid data.
         *
         * We perform only quick validation here, full validation will be applied as-needed during
         * preparation, as the merge is reconciled with current state.
         */
        applyOperation.quickVerifyStructure(data);
        applyOperation.mergeIntoModifiedNode(modification, data, version);
    }

    void delete() {
        modification.delete();
    }

    /**
     * Read a particular child. If the child has been modified and does not have a stable
     * view, one will we instantiated with specified version.
     */
    Optional<NormalizedNode> read(final PathArgument child, final Version version) {
        final ModifiedNode childNode = modification.childByArg(child);
        if (childNode != null) {
            Optional<? extends TreeNode> snapshot = childNode.getSnapshot();
            if (snapshot == null) {
                // Snapshot is not present, force instantiation
                snapshot = applyOperation.getChildByArg(child).apply(childNode, childNode.getOriginal(), version);
            }

            return snapshot.map(TreeNode::getData);
        }

        Optional<? extends TreeNode> snapshot = modification.getSnapshot();
        if (snapshot == null) {
            snapshot = apply(modification.getOriginal(), version);
        }

        if (snapshot.isPresent()) {
            return snapshot.orElseThrow().findChildByArg(child).map(TreeNode::getData);
        }

        return Optional.empty();
    }

    public ModifiedNode getModification() {
        return modification;
    }

    public ModificationApplyOperation getApplyOperation() {
        return applyOperation;
    }

    public Optional<? extends TreeNode> apply(final Optional<? extends TreeNode> data, final Version version) {
        return applyOperation.apply(modification, data, version);
    }

    public static OperationWithModification from(final ModificationApplyOperation operation,
            final ModifiedNode modification) {
        return new OperationWithModification(operation, modification);
    }
}
