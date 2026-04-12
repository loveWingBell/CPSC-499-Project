// CPSC 499 - Part 3 - Elda Britu - 30158734

package org.lsmr.cia;

import org.junit.jupiter.api.Test;
import org.lsmr.cfg.ControlFlowGraph;
import org.lsmr.cfg.Edge.EdgeLabel;
import org.lsmr.cfg.Node;
import org.lsmr.pdg.PDGBuilder;
import org.lsmr.pdg.ProgramDependenceGraph;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for {@link ChangeImpactAnalyzer}.
 *
 * Uses hand-crafted CFGs (same style as PDGBuilderTest) to verify correctness.
 *
 * Sections:
 *   1. Unit tests for helper methods (e.g. extractLineNumber)
 *   2. Edge cases (unknown change point, single-node graph, no outgoing edges)
 *   3. Integration tests: sequential chains, if/else, while loop, nested control
 */
public class ChangeImpactAnalyzerTest {

    // =========================================================================
    // 1. Helper method tests
    // =========================================================================

    @Test
    void testExtractLineNumber_normalLabel() {
        // TODO: assert extractLineNumber returns 3 for a node labelled "3: int x = 0;"
    }

    @Test
    void testExtractLineNumber_entryNode() {
        // TODO: assert extractLineNumber returns -1 for "*ENTRY*"
    }

    // =========================================================================
    // 2. Edge cases
    // =========================================================================

    @Test
    void testAnalyze_unknownChangePoint_returnsEmptyImpact() {
        // TODO: build a simple CFG, run CIA with a line number that doesn't exist,
        //       assert impactedLines is empty (or result is null/empty)
    }

    @Test
    void testAnalyze_leafNode_returnsEmptyImpact() {
        // TODO: change point is a node with no outgoing PDG edges
    }

    // =========================================================================
    // 3. Integration tests
    // =========================================================================

    @Test
    void testAnalyze_sequentialDataChain() {
        // TODO: CFG: entry -> (1: int x = 0) -> (2: int y = x) -> (3: return y) -> exit
        //       CIA from line 1 should impact lines 2 and 3 via data edges
    }

    @Test
    void testAnalyze_ifBranch_controlDependence() {
        // TODO: CFG with an if-predicate controlling two branches
        //       CIA from the predicate line should impact both branch lines
    }

    @Test
    void testAnalyze_whileLoop_loopCarriedDependence() {
        // TODO: CFG with a while loop where the loop variable is updated in the body
        //       CIA from the update line should impact the loop condition and body
    }

    @Test
    void testAnalyze_changeMidChain_doesNotImpactPredecessors() {
        // TODO: CIA is forward-only; changing line 2 in a chain 1->2->3
        //       should impact line 3, but NOT line 1
    }
}
