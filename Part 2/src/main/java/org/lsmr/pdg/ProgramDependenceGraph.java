// CPSC 499 Assignment 4 - Elda Britu - 30158734 - March 28, 2025

package org.lsmr.pdg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Represents a Program Dependence Graph (PDG) for a single method.
// Contains both control-dependence edges and data-dependence edges.

public class ProgramDependenceGraph {

    private final String name;
    private final Set<PDGNode> nodes;
    private final List<PDGEdge> edges;

    public ProgramDependenceGraph(String name) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("name must not be null or empty");
        this.name  = name;
        this.nodes = new LinkedHashSet<>();
        this.edges = new ArrayList<>();
    }

    // Mutation API - only PDGBuilder should call these) ----------------

    void addNode(PDGNode node) {
        if (node == null)
            throw new IllegalArgumentException("node must not be null");
        nodes.add(node);
    }

    void addEdge(PDGEdge edge) {
        if (edge == null)
            throw new IllegalArgumentException("edge must not be null");
        nodes.add(edge.getSource());
        nodes.add(edge.getTarget());
        edges.add(edge);
    }

    // Query API -----------------------------------------------------------

    // Returns the name of the method this PDG represents
    public String getName() { return name; }

    // Returns an unmodifiable view of all nodes in this PDG.
    public Set<PDGNode> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    // Returns an unmodifiable view of all edges in this PDG.
    public List<PDGEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    // Returns all edges whose type matches {@code type}.
    public List<PDGEdge> getEdgesOfType(PDGEdge.PDGEdgeType type) {
        if (type == null)
            throw new IllegalArgumentException("type must not be null");
        List<PDGEdge> result = new ArrayList<>();
        for (PDGEdge e : edges)
            if (e.getType() == type)
                result.add(e);
        return result;
    }

    // Returns all control-dependence edges whose source is {@code node}.
    public List<PDGEdge> getControlEdgesFrom(PDGNode node) {
        if (node == null)
            throw new IllegalArgumentException("node must not be null");
        List<PDGEdge> result = new ArrayList<>();
        for (PDGEdge e : edges)
            if (e.getType() == PDGEdge.PDGEdgeType.CONTROL && e.getSource().equals(node))
                result.add(e);
        return result;
    }

    // Returns all data-dependence edges whose source is {@code node}.
    public List<PDGEdge> getDataEdgesFrom(PDGNode node) {
        if (node == null)
            throw new IllegalArgumentException("node must not be null");
        List<PDGEdge> result = new ArrayList<>();
        for (PDGEdge e : edges)
            if (e.getType() == PDGEdge.PDGEdgeType.DATA && e.getSource().equals(node))
                result.add(e);
        return result;
    }

    // Returns all edges (of any type) that target {@code node}.
    public List<PDGEdge> getEdgesTo(PDGNode node) {
        if (node == null)
            throw new IllegalArgumentException("node must not be null");
        List<PDGEdge> result = new ArrayList<>();
        for (PDGEdge e : edges)
            if (e.getTarget().equals(node))
                result.add(e);
        return result;
    }

    // Looks up the {@link PDGNode} whose underlying CFG node carries the given label.
    public PDGNode getNodeByLabel(String label) {
        if (label == null) return null;
        for (PDGNode n : nodes)
            if (n.getLabel().equals(label))
                return n;
        return null;
    }

    // Object overrides
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PDG[").append(name).append("]: nodes=").append(nodes.size())
          .append(", edges=").append(edges.size()).append("\n");
        for (PDGEdge e : edges)
            sb.append("  ").append(e).append("\n");
        return sb.toString();
    }
}