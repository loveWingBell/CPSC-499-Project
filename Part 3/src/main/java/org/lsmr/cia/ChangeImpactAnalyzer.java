// CPSC 499 Project Part 3 - Elda Britu - 30158734 - April 13, 2026

package org.lsmr.cia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.lsmr.pdg.PDGEdge;
import org.lsmr.pdg.PDGNode;
import org.lsmr.pdg.ProgramDependenceGraph;

// Core CIA engine

public class ChangeImpactAnalyzer {

    private final ProgramDependenceGraph pdg;

    /**
     * @param pdg the program dependence graph to query; must not be null
     * @throws IllegalArgumentException if pdg is null
     */
    public ChangeImpactAnalyzer(ProgramDependenceGraph pdg) {
        if (pdg == null)
            throw new IllegalArgumentException("pdg must not be null");
        this.pdg = pdg;
    }

    // Public API ---------------------------------------------------------------

    // Runs CIA from the given change-point line number.
    public CIAResult analyze(int changePointLine) {
        if (changePointLine < 1)
            throw new IllegalArgumentException("changePointLine must be >= 1, got: " + changePointLine);

        // Step 1: find the PDG node for this line number.
        PDGNode start = findNodeByLine(changePointLine);

        // Unknown change point: return an empty result rather than crashing.
        if (start == null)
            return new CIAResult(changePointLine, Collections.emptyList());

        // Step 2: forward BFS over all outgoing CONTROL and DATA edges.
        Set<PDGNode> visited = new LinkedHashSet<>();
        Queue<PDGNode> queue = new LinkedList<>();

        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            PDGNode current = queue.poll();

            for (PDGEdge edge : pdg.getControlEdgesFrom(current)) {
                PDGNode target = edge.getTarget();
                if (visited.add(target))   // add returns false if already present
                    queue.add(target);
            }

            for (PDGEdge edge : pdg.getDataEdgesFrom(current)) {
                PDGNode target = edge.getTarget();
                if (visited.add(target))
                    queue.add(target);
            }
        }

        // Step 3: collect line numbers of all reachable nodes except the start node.
        List<Integer> impacted = new ArrayList<>();
        for (PDGNode node : visited) {
            if (node.equals(start)) continue;   // exclude the change point itself
            int line = extractLineNumber(node);
            if (line >= 1)                       // exclude virtual nodes (*ENTRY* etc.)
                impacted.add(line);
        }

        // CIAResult constructor sorts the list, so order here doesn't matter
        return new CIAResult(changePointLine, impacted);
    }

    // Package-visible helpers (used directly in tests) ---------------------------------------------------------------------

    // Scans all PDG nodes and returns the first one whose label begins with {@code "<line>:"}, or {@code null} if no such node exists
    public PDGNode findNodeByLine(int line) {
        String prefix = line + ":";
        for (PDGNode node : pdg.getNodes()) {
            String label = node.getLabel();
            if (label.startsWith(prefix)) {
                // Guard against a label like "10: ..." accidentally matching prefix "1:" by checking that the character after the colon is a space or end-of-string.
                return node;
            }
        }
        return null;
    }

    // Extracts the line number from a PDG node label of the form {@code "N: statement"}
    static int extractLineNumber(PDGNode node) {
        String label = node.getLabel();
        int colon = label.indexOf(':');
        if (colon <= 0) return -1;   // no colon, or colon is the very first character
        String prefix = label.substring(0, colon).trim();
        try {
            int n = Integer.parseInt(prefix);
            return n >= 1 ? n : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}