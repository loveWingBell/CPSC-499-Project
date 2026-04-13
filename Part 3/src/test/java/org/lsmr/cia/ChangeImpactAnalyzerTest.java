// CPSC 499 Project Part 3 - Elda Britu - 30158734 - April 13, 2026

package org.lsmr.cia;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.lsmr.cfg.ControlFlowGraph;
import org.lsmr.cfg.Edge.EdgeLabel;
import org.lsmr.cfg.Node;
import org.lsmr.pdg.PDGBuilder;
import org.lsmr.pdg.ProgramDependenceGraph;

/**
 * JUnit 5 test suite for {@link ChangeImpactAnalyzer}.
 *
 * Uses hand-crafted CFGs in the same style as PDGBuilderTest.
 * Each CFG is built manually and fed through PDGBuilder before analysis,
 * so these tests cover the full stack: CFG → PDG → CIA.
 *
 * Sections:
 *   1. Unit tests for extractLineNumber (static helper)
 *   2. Unit tests for findNodeByLine
 *   3. Constructor validation
 *   4. analyze() — edge cases and boundary conditions
 *   5. Integration tests: sequential, if/else, while loop, independent variables
 *   6. Output format tests (CIAResult contract)
 */
public class ChangeImpactAnalyzerTest {

    // =========================================================================
    // 1. extractLineNumber
    // =========================================================================

    @Test
    void testExtractLineNumber_singleDigit() {
        ControlFlowGraph cfg = new ControlFlowGraph("t");
        Node n = cfg.buildNode("3: int x = 0;");
        assertEquals(3, ChangeImpactAnalyzer.extractLineNumber(
                new org.lsmr.pdg.PDGNode(n, java.util.Set.of(), java.util.Set.of())));
    }

    @Test
    void testExtractLineNumber_multiDigit() {
        ControlFlowGraph cfg = new ControlFlowGraph("t");
        Node n = cfg.buildNode("12: x = y + 1;");
        assertEquals(12, ChangeImpactAnalyzer.extractLineNumber(
                new org.lsmr.pdg.PDGNode(n, java.util.Set.of(), java.util.Set.of())));
    }

    @Test
    void testExtractLineNumber_entryNode() {
        ControlFlowGraph cfg = new ControlFlowGraph("t");
        // *ENTRY* has no numeric prefix
        assertEquals(-1, ChangeImpactAnalyzer.extractLineNumber(
                new org.lsmr.pdg.PDGNode(cfg.entry, java.util.Set.of(), java.util.Set.of())));
    }

    @Test
    void testExtractLineNumber_exitNode() {
        ControlFlowGraph cfg = new ControlFlowGraph("t");
        assertEquals(-1, ChangeImpactAnalyzer.extractLineNumber(
                new org.lsmr.pdg.PDGNode(cfg.normalExit, java.util.Set.of(), java.util.Set.of())));
    }

    @Test
    void testExtractLineNumber_thrownNode() {
        ControlFlowGraph cfg = new ControlFlowGraph("t");
        assertEquals(-1, ChangeImpactAnalyzer.extractLineNumber(
                new org.lsmr.pdg.PDGNode(cfg.abruptExit, java.util.Set.of(), java.util.Set.of())));
    }

    @Test
    void testExtractLineNumber_lineOne_boundary() {
        ControlFlowGraph cfg = new ControlFlowGraph("t");
        Node n = cfg.buildNode("1: return 0;");
        assertEquals(1, ChangeImpactAnalyzer.extractLineNumber(
                new org.lsmr.pdg.PDGNode(n, java.util.Set.of(), java.util.Set.of())));
    }

    // =========================================================================
    // 2. findNodeByLine
    // =========================================================================

    @Test
    void testFindNodeByLine_findsCorrectNode() {
        ControlFlowGraph cfg = new ControlFlowGraph("find");
        Node n1 = cfg.buildNode("1: int x = 0;");
        Node n2 = cfg.buildNode("2: return x;");
        cfg.buildEdge(cfg.entry, n1, EdgeLabel.BLANK);
        cfg.buildEdge(n1, n2, EdgeLabel.BLANK);
        cfg.buildEdge(n2, cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        ChangeImpactAnalyzer cia = new ChangeImpactAnalyzer(pdg);

        assertNotNull(cia.findNodeByLine(1), "Should find the node for line 1");
        assertNotNull(cia.findNodeByLine(2), "Should find the node for line 2");
        assertEquals("1: int x = 0;", cia.findNodeByLine(1).getLabel());
    }

    @Test
    void testFindNodeByLine_returnsNullForMissingLine() {
        ControlFlowGraph cfg = new ControlFlowGraph("findMissing");
        Node n = cfg.buildNode("1: return 0;");
        cfg.buildEdge(cfg.entry, n, EdgeLabel.BLANK);
        cfg.buildEdge(n, cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        ChangeImpactAnalyzer cia = new ChangeImpactAnalyzer(pdg);

        assertNull(cia.findNodeByLine(99), "Line 99 does not exist; must return null");
    }

    @Test
    void testFindNodeByLine_doesNotConfusePrefixes() {
        // "1:" must not match the node labelled "10: ..."
        ControlFlowGraph cfg = new ControlFlowGraph("prefixDisambig");
        Node n10 = cfg.buildNode("10: x = y;");
        cfg.buildEdge(cfg.entry, n10, EdgeLabel.BLANK);
        cfg.buildEdge(n10, cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        ChangeImpactAnalyzer cia = new ChangeImpactAnalyzer(pdg);

        assertNull(cia.findNodeByLine(1),
                "Searching for line 1 must not return the node labelled '10: ...'");
        assertNotNull(cia.findNodeByLine(10),
                "Searching for line 10 must find the '10: ...' node");
    }

    // =========================================================================
    // 3. Constructor validation
    // =========================================================================

    @Test
    void testConstructor_nullPdg_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new ChangeImpactAnalyzer(null),
                "Constructor must reject a null PDG");
    }

    // =========================================================================
    // 4. analyze() — edge cases
    // =========================================================================

    @Test
    void testAnalyze_invalidLineNumber_throws() {
        ControlFlowGraph cfg = new ControlFlowGraph("inv");
        cfg.buildEdge(cfg.entry, cfg.normalExit, EdgeLabel.BLANK);
        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        ChangeImpactAnalyzer cia = new ChangeImpactAnalyzer(pdg);

        assertThrows(IllegalArgumentException.class,
                () -> cia.analyze(0),
                "Line number 0 is invalid; must throw");
        assertThrows(IllegalArgumentException.class,
                () -> cia.analyze(-5),
                "Negative line number is invalid; must throw");
    }

    @Test
    void testAnalyze_unknownChangePoint_returnsEmptyResult() {
        // The PDG has only line 1; asking for line 99 should give an empty result,
        // not null and not an exception.
        ControlFlowGraph cfg = new ControlFlowGraph("unknown");
        Node n = cfg.buildNode("1: return 0;");
        cfg.buildEdge(cfg.entry, n, EdgeLabel.BLANK);
        cfg.buildEdge(n, cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(99);

        assertNotNull(result, "analyze must never return null");
        assertTrue(result.isEmpty(), "Impact must be empty for an unknown change point");
        assertEquals(99, result.getChangePoint(),
                "changePoint in result must match the requested line");
    }

    @Test
    void testAnalyze_emptyMethod_returnsEmptyResult() {
        // CFG with no real statements at all (ENTRY → EXIT).
        ControlFlowGraph cfg = new ControlFlowGraph("emptyMethod");
        cfg.buildEdge(cfg.entry, cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(1);

        assertNotNull(result);
        assertTrue(result.isEmpty(),
                "A body-less method has no impacted lines for any change point");
    }

    @Test
    void testAnalyze_singleStatement_noImpact() {
        // CFG: ENTRY → "1: return 0;" → EXIT.
        // The return statement has no outgoing PDG edges, so its impact set is empty.
        ControlFlowGraph cfg = new ControlFlowGraph("single");
        Node n = cfg.buildNode("1: return 0;");
        cfg.buildEdge(cfg.entry, n, EdgeLabel.BLANK);
        cfg.buildEdge(n, cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(1);

        assertNotNull(result);
        assertTrue(result.isEmpty(),
                "A lone return statement can impact nothing else");
    }

    @Test
    void testAnalyze_changePointExcludedFromImpact() {
        // The change-point line itself must NEVER appear in impactedLines,
        // even in a loop where the node has a self-referential data edge.
        // CFG: ENTRY → "1: int i = 0;" → "2: while (i < n)" →TRUE→ "3: i++;" → back
        //                                                    →FALSE→ "4: return i;" → EXIT
        // Line 3 (i++) has a self-data-edge (i defined and used at the same node through the
        // loop back-edge). CIA from line 3 must include 2 and 4 but NOT 3.
        ControlFlowGraph cfg = new ControlFlowGraph("selfRef");
        Node init  = cfg.buildNode("1: int i = 0;");
        Node cond  = cfg.buildNode("2: while (i < n)");
        Node body  = cfg.buildNode("3: i++;");
        Node ret   = cfg.buildNode("4: return i;");

        cfg.buildEdge(cfg.entry, init,  EdgeLabel.BLANK);
        cfg.buildEdge(init,      cond,  EdgeLabel.BLANK);
        cfg.buildEdge(cond,      body,  EdgeLabel.TRUE);
        cfg.buildEdge(cond,      ret,   EdgeLabel.FALSE);
        cfg.buildEdge(body,      cond,  EdgeLabel.BLANK);
        cfg.buildEdge(ret,       cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(3);

        assertFalse(result.getImpactedLines().contains(3),
                "The change-point line must never appear in impactedLines");
    }

    // =========================================================================
    // 5. Integration tests
    // =========================================================================

    // -------------------------------------------------------------------------
    // 5a. Sequential data chain
    //
    //   CFG: ENTRY → "1: int x = 0;" → "2: int y = x + 1;" → "3: return y;" → EXIT
    //
    //   PDG data edges: node1 →(x)→ node2, node2 →(y)→ node3
    //   No control edges (straight-line code).
    // -------------------------------------------------------------------------

    @Test
    void testAnalyze_sequential_fromFirstNode_impactsAll() {
        ProgramDependenceGraph pdg = buildSequentialChain();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(1);

        assertEquals(List.of(2, 3), result.getImpactedLines(),
                "Changing line 1 (def of x) must transitively impact lines 2 and 3");
    }

    @Test
    void testAnalyze_sequential_fromMiddleNode_impactsOnlyDownstream() {
        ProgramDependenceGraph pdg = buildSequentialChain();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(2);

        assertEquals(List.of(3), result.getImpactedLines(),
                "Changing line 2 must impact only line 3, not line 1 (CIA is forward-only)");
        assertFalse(result.getImpactedLines().contains(1),
                "CIA must never travel backwards along data edges");
    }

    @Test
    void testAnalyze_sequential_fromLeaf_emptyImpact() {
        ProgramDependenceGraph pdg = buildSequentialChain();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(3);

        assertTrue(result.isEmpty(),
                "The return statement at the end of the chain impacts nothing");
    }

    // -------------------------------------------------------------------------
    // 5b. If/else
    //
    //   CFG: ENTRY → "1: if (flag)" →TRUE→ "2: x = 1;" ──┐
    //                               →FALSE→"3: x = 2;" ──┤→ "4: return x;" → EXIT
    //
    //   PDG control edges: node1→node2, node1→node3
    //   PDG data edges:    node2→node4(x), node3→node4(x)
    // -------------------------------------------------------------------------

    @Test
    void testAnalyze_ifElse_predicateImpactsBothBranchesAndJoin() {
        ProgramDependenceGraph pdg = buildIfElse();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(1);

        List<Integer> impact = result.getImpactedLines();
        assertTrue(impact.contains(2), "Predicate controls the TRUE branch");
        assertTrue(impact.contains(3), "Predicate controls the FALSE branch");
        assertTrue(impact.contains(4), "Both branches feed the return via data");
        assertEquals(3, impact.size(), "Expected exactly lines 2, 3, 4");
    }

    @Test
    void testAnalyze_ifElse_trueBranch_impactsOnlyJoin() {
        ProgramDependenceGraph pdg = buildIfElse();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(2);

        assertEquals(List.of(4), result.getImpactedLines(),
                "Changing the TRUE branch body should impact only the join (return), not the FALSE branch");
    }

    @Test
    void testAnalyze_ifElse_falseBranch_impactsOnlyJoin() {
        ProgramDependenceGraph pdg = buildIfElse();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(3);

        assertEquals(List.of(4), result.getImpactedLines(),
                "Changing the FALSE branch body should impact only the join, not the TRUE branch");
    }

    @Test
    void testAnalyze_ifElse_joinNode_emptyImpact() {
        ProgramDependenceGraph pdg = buildIfElse();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(4);

        assertTrue(result.isEmpty(),
                "The return node at the join has no outgoing PDG edges");
    }

    // -------------------------------------------------------------------------
    // 5c. While loop
    //
    //   CFG: ENTRY → "1: int i = 0;" → "2: while (i < n)" →TRUE→ "3: i++;" → back to 2
    //                                                      →FALSE→ "4: return i;" → EXIT
    //
    //   PDG control edge: node2→node3
    //   PDG data edges:   node1→{node2,node3,node4}(i)
    //                     node3→{node2,node4}(i)   [node3→node3 self-loop also exists but
    //                                                is filtered from the result]
    // -------------------------------------------------------------------------

    @Test
    void testAnalyze_whileLoop_initImpactsAll() {
        ProgramDependenceGraph pdg = buildWhileLoop();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(1);

        List<Integer> impact = result.getImpactedLines();
        assertTrue(impact.contains(2), "i flows into the loop condition");
        assertTrue(impact.contains(3), "i flows into the loop body");
        assertTrue(impact.contains(4), "i flows into the return");
        assertEquals(3, impact.size(), "Expected exactly lines 2, 3, 4");
    }

    @Test
    void testAnalyze_whileLoop_incrementImpactsConditionAndReturn() {
        // i++ (line 3) redefines i, which feeds back to the condition (line 2) and
        // eventually to the return (line 4). It does NOT re-impact itself (line 3).
        ProgramDependenceGraph pdg = buildWhileLoop();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(3);

        List<Integer> impact = result.getImpactedLines();
        assertTrue(impact.contains(2), "i++ redefines i, which feeds the loop condition");
        assertTrue(impact.contains(4), "i++ redefines i, which is returned");
        assertFalse(impact.contains(3), "The change point must not appear in impactedLines");
        assertEquals(2, impact.size(), "Expected exactly lines 2 and 4");
    }

    @Test
    void testAnalyze_whileLoop_conditionImpactsBody() {
        // Changing the condition (line 2) controls the body (line 3).
        // It also transitively reaches line 4 (via data from node3 and via the BFS
        // discovering node4 through node3's data edges).
        ProgramDependenceGraph pdg = buildWhileLoop();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(2);

        List<Integer> impact = result.getImpactedLines();
        assertTrue(impact.contains(3),
                "The loop condition controls the body via a CONTROL edge");
        assertTrue(impact.contains(4),
                "The body defines i which data-flows to the return");
        assertFalse(impact.contains(2), "The change point must not appear in impactedLines");
    }

    // -------------------------------------------------------------------------
    // 5d. Independent variables
    //
    //   CFG: ENTRY → "1: int x = 0;" → "2: int y = 5;"
    //              → "3: int a = x + 1;" → "4: return y + a;" → EXIT
    //
    //   Data edges: node1→node3(x), node2→node4(y), node3→node4(a)
    //   Changing x should NOT propagate to node2 (y is unrelated).
    //   Changing y should NOT propagate to node3 (a depends only on x).
    // -------------------------------------------------------------------------

    @Test
    void testAnalyze_independentVars_xChangeDoesNotImpactY() {
        ProgramDependenceGraph pdg = buildIndependentVariables();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(1);

        List<Integer> impact = result.getImpactedLines();
        // x flows: node1 →(x)→ node3 →(a)→ node4
        assertTrue(impact.contains(3), "x flows into a = x + 1");
        assertTrue(impact.contains(4), "a flows into the return");
        assertFalse(impact.contains(2),
                "y is defined independently of x; line 2 must NOT be impacted");
        assertEquals(2, impact.size());
    }

    @Test
    void testAnalyze_independentVars_yChangeDoesNotImpactX() {
        ProgramDependenceGraph pdg = buildIndependentVariables();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(2);

        List<Integer> impact = result.getImpactedLines();
        // y flows: node2 →(y)→ node4
        assertTrue(impact.contains(4), "y flows into the return");
        assertFalse(impact.contains(3),
                "a = x + 1 depends only on x, not y; line 3 must NOT be impacted");
        assertEquals(1, impact.size());
    }

    // =========================================================================
    // 6. Output format / CIAResult contract
    // =========================================================================

    @Test
    void testAnalyze_resultIsSorted() {
        // Build a CFG where BFS could discover impacted nodes in non-sorted order,
        // and verify the result list is always ascending.
        // Use the if/else graph: CIA from line 1 discovers nodes in order 2, 3, 4
        // but we verify the list is sorted regardless.
        ProgramDependenceGraph pdg = buildIfElse();
        List<Integer> impact = new ChangeImpactAnalyzer(pdg).analyze(1).getImpactedLines();

        for (int i = 0; i < impact.size() - 1; i++)
            assertTrue(impact.get(i) < impact.get(i + 1),
                    "impactedLines must be in strictly ascending order");
    }

    @Test
    void testAnalyze_changePointStoredInResult() {
        ProgramDependenceGraph pdg = buildSequentialChain();
        CIAResult result = new ChangeImpactAnalyzer(pdg).analyze(2);
        assertEquals(2, result.getChangePoint(),
                "CIAResult must record the original change-point line number");
    }

    @Test
    void testAnalyze_toJsonFormat() {
        ProgramDependenceGraph pdg = buildSequentialChain();
        String json = new ChangeImpactAnalyzer(pdg).analyze(1).toJson();

        assertTrue(json.startsWith("{"), "JSON must start with '{'");
        assertTrue(json.contains("\"changePoint\":1"), "JSON must contain changePoint field");
        assertTrue(json.contains("\"impactedLines\":[2,3]"), "JSON must contain sorted impactedLines");
        assertEquals("{\"changePoint\":1,\"impactedLines\":[2,3]}", json);
    }

    @Test
    void testAnalyze_toJson_emptyImpact() {
        ProgramDependenceGraph pdg = buildSequentialChain();
        String json = new ChangeImpactAnalyzer(pdg).analyze(3).toJson();
        assertEquals("{\"changePoint\":3,\"impactedLines\":[]}", json);
    }

    // =========================================================================
    // CFG / PDG factory helpers
    // =========================================================================

    /**
     * Sequential data chain:
     *   ENTRY → "1: int x = 0;" → "2: int y = x + 1;" → "3: return y;" → EXIT
     *
     * Expected PDG data edges: node1→node2 (x), node2→node3 (y). No control edges.
     */
    private static ProgramDependenceGraph buildSequentialChain() {
        ControlFlowGraph cfg = new ControlFlowGraph("seq");
        Node n1 = cfg.buildNode("1: int x = 0;");
        Node n2 = cfg.buildNode("2: int y = x + 1;");
        Node n3 = cfg.buildNode("3: return y;");

        cfg.buildEdge(cfg.entry, n1, EdgeLabel.BLANK);
        cfg.buildEdge(n1,        n2, EdgeLabel.BLANK);
        cfg.buildEdge(n2,        n3, EdgeLabel.BLANK);
        cfg.buildEdge(n3,        cfg.normalExit, EdgeLabel.BLANK);

        return new PDGBuilder(cfg).build();
    }

    /**
     * If/else:
     *   ENTRY → "1: if (flag)" →TRUE→ "2: x = 1;" ──┐
     *                          →FALSE→"3: x = 2;" ──┴→ "4: return x;" → EXIT
     *
     * Expected PDG: CONTROL node1→{node2,node3}; DATA node2→node4(x), node3→node4(x).
     */
    private static ProgramDependenceGraph buildIfElse() {
        ControlFlowGraph cfg = new ControlFlowGraph("ifelse");
        Node n1 = cfg.buildNode("1: if (flag)");
        Node n2 = cfg.buildNode("2: x = 1;");
        Node n3 = cfg.buildNode("3: x = 2;");
        Node n4 = cfg.buildNode("4: return x;");

        cfg.buildEdge(cfg.entry, n1, EdgeLabel.BLANK);
        cfg.buildEdge(n1,        n2, EdgeLabel.TRUE);
        cfg.buildEdge(n1,        n3, EdgeLabel.FALSE);
        cfg.buildEdge(n2,        n4, EdgeLabel.BLANK);
        cfg.buildEdge(n3,        n4, EdgeLabel.BLANK);
        cfg.buildEdge(n4,        cfg.normalExit, EdgeLabel.BLANK);

        return new PDGBuilder(cfg).build();
    }

    /**
     * While loop:
     *   ENTRY → "1: int i = 0;" → "2: while (i < n)" →TRUE→ "3: i++;" → back to 2
     *                                                 →FALSE→ "4: return i;" → EXIT
     *
     * Expected PDG: CONTROL node2→node3;
     *               DATA node1→{node2,node3,node4}(i), node3→{node2,node4}(i).
     */
    private static ProgramDependenceGraph buildWhileLoop() {
        ControlFlowGraph cfg = new ControlFlowGraph("while");
        Node n1 = cfg.buildNode("1: int i = 0;");
        Node n2 = cfg.buildNode("2: while (i < n)");
        Node n3 = cfg.buildNode("3: i++;");
        Node n4 = cfg.buildNode("4: return i;");

        cfg.buildEdge(cfg.entry, n1, EdgeLabel.BLANK);
        cfg.buildEdge(n1,        n2, EdgeLabel.BLANK);
        cfg.buildEdge(n2,        n3, EdgeLabel.TRUE);
        cfg.buildEdge(n2,        n4, EdgeLabel.FALSE);
        cfg.buildEdge(n3,        n2, EdgeLabel.BLANK);   // back edge
        cfg.buildEdge(n4,        cfg.normalExit, EdgeLabel.BLANK);

        return new PDGBuilder(cfg).build();
    }

    /**
     * Two independent variables:
     *   ENTRY → "1: int x = 0;" → "2: int y = 5;"
     *         → "3: int a = x + 1;" → "4: return y + a;" → EXIT
     *
     * Expected PDG data edges: node1→node3(x), node2→node4(y), node3→node4(a).
     * x and y have entirely separate dependence chains.
     */
    private static ProgramDependenceGraph buildIndependentVariables() {
        ControlFlowGraph cfg = new ControlFlowGraph("indep");
        Node n1 = cfg.buildNode("1: int x = 0;");
        Node n2 = cfg.buildNode("2: int y = 5;");
        Node n3 = cfg.buildNode("3: int a = x + 1;");
        Node n4 = cfg.buildNode("4: return y + a;");

        cfg.buildEdge(cfg.entry, n1, EdgeLabel.BLANK);
        cfg.buildEdge(n1,        n2, EdgeLabel.BLANK);
        cfg.buildEdge(n2,        n3, EdgeLabel.BLANK);
        cfg.buildEdge(n3,        n4, EdgeLabel.BLANK);
        cfg.buildEdge(n4,        cfg.normalExit, EdgeLabel.BLANK);

        return new PDGBuilder(cfg).build();
    }
}