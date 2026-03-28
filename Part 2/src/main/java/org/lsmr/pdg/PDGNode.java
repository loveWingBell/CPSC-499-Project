// CPSC 499 Assignment 4 - Elda Britu - 30158734 - March 28, 2025

package org.lsmr.pdg;

import org.lsmr.cfg.Node;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// A node in a Program Dependence Graph. Wraps a CFG {@link Node} and additionally stores:
//  - the set of variables defined (written) by the statement
//  - the set of variables <em>used</em> (read) by the statement

public class PDGNode {

    private final Node cfgNode;
    private final Set<String> defs;
    private final Set<String> uses;

    /**
     * Constructs a PDGNode wrapping the given CFG node.
     *
     * @param cfgNode the CFG node this PDG node corresponds to; must not be null
     * @param defs    the set of variable names defined (written) at this node
     * @param uses    the set of variable names used (read) at this node
     */
    public PDGNode(Node cfgNode, Set<String> defs, Set<String> uses) {
        if (cfgNode == null)
            throw new IllegalArgumentException("cfgNode must not be null");
        this.cfgNode = cfgNode;
        this.defs    = Collections.unmodifiableSet(new HashSet<>(defs));
        this.uses    = Collections.unmodifiableSet(new HashSet<>(uses));
    }

    // Returns the underlying CFG node. Never null.
    public Node getCfgNode() { return cfgNode; }

    // Returns the statement label carried by the wrapped CFG node (ex. {@code "3: int x = 0;"})
    public String getLabel() { return cfgNode.label(); }

    // Returns an unmodifiable view of variables defined at this node.
    public Set<String> getDefs() { return defs; }

    // Returns an unmodifiable view of variables used at this node.
    public Set<String> getUses() { return uses; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PDGNode)) return false;
        return cfgNode.equals(((PDGNode) obj).cfgNode);
    }

    @Override
    public int hashCode() { return cfgNode.hashCode(); }

    @Override
    public String toString() { return "PDGNode(" + cfgNode.label() + ")"; }
}