// CPSC 499 Assignment 4 - Elda Britu - 30158734 - March 28, 2025

package org.lsmr.pdg;

// An edge in a Program Dependence Graph.
// Can represent either a control dependence or a data dependence.

public class PDGEdge {

    public enum PDGEdgeType {
        // Edge from a predicate node to a statement it controls.
        CONTROL,
        // Edge from a definition to a use of the same variable.
        DATA
    }

    private final PDGNode source;
    private final PDGNode target;
    private final PDGEdgeType type;
    // Non-null only for DATA edges
    private final String variable;

    public PDGEdge(PDGNode source, PDGNode target) {
        this(source, target, PDGEdgeType.CONTROL, null);
    }

    public PDGEdge(PDGNode source, PDGNode target, String variable) {
        this(source, target, PDGEdgeType.DATA, variable);
    }

    public PDGEdge(PDGNode source, PDGNode target, PDGEdgeType type, String variable) {
        if (source == null) throw new IllegalArgumentException("source must not be null");
        if (target == null) throw new IllegalArgumentException("target must not be null");
        if (type   == null) throw new IllegalArgumentException("type must not be null");
        if (type == PDGEdgeType.DATA && variable == null)
            throw new IllegalArgumentException("variable must not be null for DATA edges");

        this.source   = source;
        this.target   = target;
        this.type     = type;
        this.variable = variable;
    }

    // Returns the source (defining / predicate) node. Never null.
    public PDGNode getSource() { return source; }

    // Returns the target (controlled / using) node. Never null.
    public PDGNode getTarget() { return target; }

    // Returns the type of this edge (CONTROL or DATA). Never null.
    public PDGEdgeType getType() { return type; }

    // Returns the variable name that flows along a DATA edge.
    public String getVariable() { return variable; }

    // Object overrides
    // Two PDGEdges are equal when their source, target, type, and variable are all equal.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PDGEdge)) return false;
        PDGEdge other = (PDGEdge) obj;
        if (!source.equals(other.source)) return false;
        if (!target.equals(other.target)) return false;
        if (type != other.type)           return false;
        if (variable == null)
            return other.variable == null;
        return variable.equals(other.variable);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + source.hashCode();
        result = 31 * result + target.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (variable != null ? variable.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (type == PDGEdgeType.DATA)
            return "PDGEdge[DATA:" + variable + " (" + source.getLabel() + " -> " + target.getLabel() + ")]";
        return "PDGEdge[CONTROL (" + source.getLabel() + " -> " + target.getLabel() + ")]";
    }
}