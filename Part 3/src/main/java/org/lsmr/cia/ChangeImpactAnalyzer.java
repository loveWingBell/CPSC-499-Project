// CPSC 499 - Part 3 - Elda Britu - 30158734

package org.lsmr.cia;

import org.lsmr.pdg.PDGNode;
import org.lsmr.pdg.ProgramDependenceGraph;

/**
 * Core CIA engine.
 *
 * Given a built {@link ProgramDependenceGraph} and a change-point line number,
 * performs a forward BFS over all outgoing PDG edges (both CONTROL and DATA)
 * and returns a {@link CIAResult} containing every reachable node's line number.
 */
public class ChangeImpactAnalyzer {

    private final ProgramDependenceGraph pdg;

    public ChangeImpactAnalyzer(ProgramDependenceGraph pdg) {
        // TODO: validate and store
        this.pdg = pdg;
    }

    /**
     * Runs CIA from the given change-point line number.
     *
     * @param changePointLine the 1-based source line number of the changed statement
     * @return a {@link CIAResult} with all transitively impacted line numbers
     */
    public CIAResult analyze(int changePointLine) {
        // TODO: implement
        //  1. Find the PDGNode whose label starts with "<changePointLine>:"
        //  2. BFS/DFS forward over all outgoing edges (CONTROL + DATA)
        //  3. Collect line numbers of all reachable nodes
        //  4. Return a sorted CIAResult
        return null;
    }

    /**
     * Extracts the line number from a PDG node label of the form "N: statement".
     * Returns -1 if the label does not start with a number.
     */
    static int extractLineNumber(PDGNode node) {
        // TODO: implement
        return -1;
    }
}
