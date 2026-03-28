// CPSC 499 Assignment 4 - Elda Britu - 30158734 - March 28, 2025

package org.lsmr.pdg;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

/**
 * JUnit 5 test suite for {@link PDGBuilder}.
 *
 * Organised in sections:
 *  1. Unit tests for static helper methods (no CFG required).
 *  2. Constructor / validation tests for PDGNode, PDGEdge, ProgramDependenceGraph, PDGBuilder.
 *  3. DominatorTree API tests.
 *  4. ProgramDependenceGraph query API tests.
 *  5. Integration tests: build small CFGs manually, run PDGBuilder,
 *     assert expected control- and data-dependence edges.
 */
public class PDGBuilderTest {

    // =========================================================================
    // 1. Unit tests for static helper methods
    // =========================================================================

    // --- stripPrefix ---

    @Test
    void testStripPrefix_normalLabel() {
        assertEquals("int x = 0;", PDGBuilder.stripPrefix("3: int x = 0;"));
    }

    @Test
    void testStripPrefix_noColonReturnsFullLabel() {
        assertEquals("*ENTRY*", PDGBuilder.stripPrefix("*ENTRY*"));
    }

    @Test
    void testStripPrefix_emptyBodyAfterColon() {
        assertEquals("", PDGBuilder.stripPrefix("1:"));
    }

    @Test
    void testStripPrefix_whitespaceIsTrimmed() {
        assertEquals("return x;", PDGBuilder.stripPrefix("7:   return x;"));
    }

    // --- isVirtualExitNode ---

    @Test
    void testIsVirtualExitNode_normalExit() {
        ControlFlowGraph cfg = new ControlFlowGraph("test");
        assertTrue(PDGBuilder.isVirtualExitNode(cfg.normalExit),
                "*EXIT* should be recognised as a virtual exit node");
    }

    @Test
    void testIsVirtualExitNode_abruptExit() {
        ControlFlowGraph cfg = new ControlFlowGraph("test");
        assertTrue(PDGBuilder.isVirtualExitNode(cfg.abruptExit),
                "*THROWN* should be recognised as a virtual exit node");
    }

    @Test
    void testIsVirtualExitNode_entry() {
        ControlFlowGraph cfg = new ControlFlowGraph("test");
        assertFalse(PDGBuilder.isVirtualExitNode(cfg.entry),
                "*ENTRY* is not a virtual exit node");
    }

    @Test
    void testIsVirtualExitNode_regularNode() {
        ControlFlowGraph cfg = new ControlFlowGraph("test");
        Node n = cfg.buildNode("1: int x = 0;");
        assertFalse(PDGBuilder.isVirtualExitNode(n),
                "A regular statement node should not be a virtual exit node");
    }

    // --- extractDefs ---

    @Test
    void testExtractDefs_typeDeclaration_intVariable() {
        Set<String> defs = PDGBuilder.extractDefs("1: int x = 0;");
        assertTrue(defs.contains("x"), "Variable 'x' should be in defs");
        assertFalse(defs.contains("int"), "Keyword 'int' must not appear in defs");
    }

    @Test
    void testExtractDefs_typeDeclaration_objectVariable() {
        Set<String> defs = PDGBuilder.extractDefs("2: String name = null;");
        assertTrue(defs.contains("name"), "'name' should be defined by the type declaration");
    }

    @Test
    void testExtractDefs_simpleAssignment() {
        Set<String> defs = PDGBuilder.extractDefs("3: x = x + 1;");
        assertTrue(defs.contains("x"), "'x' is defined by the assignment");
    }

    @Test
    void testExtractDefs_compoundAssignment_defIsLHSOnly() {
        Set<String> defs = PDGBuilder.extractDefs("4: sum += value;");
        assertTrue(defs.contains("sum"), "'sum' is the LHS of '+='");
        assertFalse(defs.contains("value"), "'value' is only read here");
    }

    @Test
    void testExtractDefs_postIncrement() {
        Set<String> defs = PDGBuilder.extractDefs("5: i++;");
        assertTrue(defs.contains("i"), "Post-increment defines 'i'");
    }

    @Test
    void testExtractDefs_postDecrement() {
        Set<String> defs = PDGBuilder.extractDefs("6: count--;");
        assertTrue(defs.contains("count"), "Post-decrement defines 'count'");
    }

    @Test
    void testExtractDefs_preIncrement() {
        Set<String> defs = PDGBuilder.extractDefs("7: ++i;");
        assertTrue(defs.contains("i"), "Pre-increment defines 'i'");
    }

    @Test
    void testExtractDefs_preDecrement() {
        Set<String> defs = PDGBuilder.extractDefs("8: --k;");
        assertTrue(defs.contains("k"), "Pre-decrement defines 'k'");
    }

    @Test
    void testExtractDefs_returnStatement_empty() {
        Set<String> defs = PDGBuilder.extractDefs("9: return x;");
        assertTrue(defs.isEmpty(), "A return statement defines no variables");
    }

    @Test
    void testExtractDefs_ifCondition_empty() {
        Set<String> defs = PDGBuilder.extractDefs("10: if (x > 0)");
        assertTrue(defs.isEmpty(), "A bare condition defines no variables");
    }

    @Test
    void testExtractDefs_whileCondition_empty() {
        Set<String> defs = PDGBuilder.extractDefs("11: while (i < 10)");
        assertTrue(defs.isEmpty(), "A while condition defines no variables");
    }

    @Test
    void testExtractDefs_subtractAssignment() {
        Set<String> defs = PDGBuilder.extractDefs("12: total -= discount;");
        assertTrue(defs.contains("total"), "'-=' defines 'total'");
        assertFalse(defs.contains("discount"), "'discount' is only read");
    }

    @Test
    void testExtractDefs_multipleDeclarators() {
        // int x = 0, y = 1  — both x and y are defined
        Set<String> defs = PDGBuilder.extractDefs("13: int x = 0 , y = 1;");
        assertTrue(defs.contains("x"), "'x' should be defined");
        assertTrue(defs.contains("y"), "'y' should be defined");
    }

    // --- extractUses ---

    @Test
    void testExtractUses_typeDeclarationWithRHS() {
        Set<String> uses = PDGBuilder.extractUses("2: int y = x + 1;");
        assertTrue(uses.contains("x"), "'x' is read on the RHS");
        assertFalse(uses.contains("int"), "Keyword 'int' must be filtered");
    }

    @Test
    void testExtractUses_returnStatement() {
        Set<String> uses = PDGBuilder.extractUses("3: return result;");
        assertTrue(uses.contains("result"), "'result' is read in the return");
        assertFalse(uses.contains("return"), "Keyword 'return' must be filtered");
    }

    @Test
    void testExtractUses_whileCondition() {
        Set<String> uses = PDGBuilder.extractUses("4: while (i < 10)");
        assertTrue(uses.contains("i"), "Loop variable 'i' should be in uses");
        assertFalse(uses.contains("while"), "Keyword 'while' must be filtered");
    }

    @Test
    void testExtractUses_ifCondition() {
        Set<String> uses = PDGBuilder.extractUses("5: if (flag)");
        assertTrue(uses.contains("flag"), "'flag' is read in the condition");
        assertFalse(uses.contains("if"), "Keyword 'if' must be filtered");
    }

    @Test
    void testExtractUses_numericLiteralOnly_noIdentifiers() {
        Set<String> uses = PDGBuilder.extractUses("6: return 42;");
        assertFalse(uses.contains("return"), "Keyword 'return' must be filtered");
    }

    @Test
    void testExtractUses_multipleVariables() {
        Set<String> uses = PDGBuilder.extractUses("7: int c = a + b;");
        assertTrue(uses.contains("a"), "'a' is used");
        assertTrue(uses.contains("b"), "'b' is used");
    }

    // =========================================================================
    // 2. Constructor / validation tests
    // =========================================================================

    // --- PDGNode ---

    @Test
    void testPDGNode_nullCfgNodeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new PDGNode(null, new HashSet<>(), new HashSet<>()),
                "PDGNode must reject a null cfgNode");
    }

    @Test
    void testPDGNode_equalsAndHashCode() {
        ControlFlowGraph cfg = new ControlFlowGraph("eq");
        Node n = cfg.buildNode("1: int x = 0;");
        PDGNode a = new PDGNode(n, new HashSet<>(), new HashSet<>());
        PDGNode b = new PDGNode(n, new HashSet<>(), new HashSet<>());
        assertEquals(a, b, "Two PDGNodes wrapping the same CFG node must be equal");
        assertEquals(a.hashCode(), b.hashCode(), "Equal nodes must have equal hash codes");
    }

    @Test
    void testPDGNode_notEqualToDifferentNode() {
        ControlFlowGraph cfg = new ControlFlowGraph("neq");
        Node n1 = cfg.buildNode("1: int x = 0;");
        Node n2 = cfg.buildNode("2: int y = 0;");
        PDGNode a = new PDGNode(n1, new HashSet<>(), new HashSet<>());
        PDGNode b = new PDGNode(n2, new HashSet<>(), new HashSet<>());
        assertFalse(a.equals(b), "PDGNodes wrapping different CFG nodes must not be equal");
    }

    @Test
    void testPDGNode_notEqualToNonPDGNode() {
        ControlFlowGraph cfg = new ControlFlowGraph("type");
        Node n = cfg.buildNode("1: x = 1;");
        PDGNode a = new PDGNode(n, new HashSet<>(), new HashSet<>());
        assertFalse(a.equals("not a PDGNode"), "PDGNode must not equal a non-PDGNode");
    }

    @Test
    void testPDGNode_gettersReturnCorrectValues() {
        ControlFlowGraph cfg = new ControlFlowGraph("getters");
        Node n = cfg.buildNode("1: int x = 0;");
        Set<String> defs = new HashSet<>();
        defs.add("x");
        Set<String> uses = new HashSet<>();
        uses.add("x");
        PDGNode node = new PDGNode(n, defs, uses);
        assertEquals(n, node.getCfgNode());
        assertEquals("1: int x = 0;", node.getLabel());
        assertTrue(node.getDefs().contains("x"));
        assertTrue(node.getUses().contains("x"));
    }

    @Test
    void testPDGNode_defsAreImmutable() {
        ControlFlowGraph cfg = new ControlFlowGraph("immutable");
        Node n = cfg.buildNode("1: x = 1;");
        PDGNode node = new PDGNode(n, new HashSet<>(), new HashSet<>());
        assertThrows(UnsupportedOperationException.class,
                () -> node.getDefs().add("z"),
                "Defs set must be unmodifiable");
    }

    @Test
    void testPDGNode_usesAreImmutable() {
        ControlFlowGraph cfg = new ControlFlowGraph("immutable2");
        Node n = cfg.buildNode("1: x = 1;");
        PDGNode node = new PDGNode(n, new HashSet<>(), new HashSet<>());
        assertThrows(UnsupportedOperationException.class,
                () -> node.getUses().add("z"),
                "Uses set must be unmodifiable");
    }

    // --- PDGEdge ---

    @Test
    void testPDGEdge_nullSourceThrows() {
        ControlFlowGraph cfg = new ControlFlowGraph("e");
        PDGNode tgt = new PDGNode(cfg.buildNode("1: x = 1;"), new HashSet<>(), new HashSet<>());
        assertThrows(IllegalArgumentException.class,
                () -> new PDGEdge(null, tgt),
                "PDGEdge must reject null source");
    }

    @Test
    void testPDGEdge_nullTargetThrows() {
        ControlFlowGraph cfg = new ControlFlowGraph("e2");
        PDGNode src = new PDGNode(cfg.buildNode("1: x = 1;"), new HashSet<>(), new HashSet<>());
        assertThrows(IllegalArgumentException.class,
                () -> new PDGEdge(src, null),
                "PDGEdge must reject null target");
    }

    @Test
    void testPDGEdge_nullTypeThrows() {
        ControlFlowGraph cfg = new ControlFlowGraph("e3");
        PDGNode src = new PDGNode(cfg.buildNode("1: x = 1;"), new HashSet<>(), new HashSet<>());
        PDGNode tgt = new PDGNode(cfg.buildNode("2: y = 2;"), new HashSet<>(), new HashSet<>());
        assertThrows(IllegalArgumentException.class,
                () -> new PDGEdge(src, tgt, null, null),
                "PDGEdge must reject null type");
    }

    @Test
    void testPDGEdge_dataEdgeNullVariableThrows() {
        ControlFlowGraph cfg = new ControlFlowGraph("e4");
        PDGNode src = new PDGNode(cfg.buildNode("1: x = 1;"), new HashSet<>(), new HashSet<>());
        PDGNode tgt = new PDGNode(cfg.buildNode("2: y = x;"), new HashSet<>(), new HashSet<>());
        assertThrows(IllegalArgumentException.class,
                () -> new PDGEdge(src, tgt, PDGEdge.PDGEdgeType.DATA, null),
                "DATA edge must reject null variable");
    }

    @Test
    void testPDGEdge_controlEdge_variableIsNull() {
        ControlFlowGraph cfg = new ControlFlowGraph("e5");
        PDGNode src = new PDGNode(cfg.buildNode("1: if (x)"), new HashSet<>(), new HashSet<>());
        PDGNode tgt = new PDGNode(cfg.buildNode("2: x = 1;"), new HashSet<>(), new HashSet<>());
        PDGEdge edge = new PDGEdge(src, tgt);
        assertEquals(PDGEdge.PDGEdgeType.CONTROL, edge.getType());
        assertNull(edge.getVariable(), "CONTROL edge variable must be null");
    }

    @Test
    void testPDGEdge_dataEdge_getters() {
        ControlFlowGraph cfg = new ControlFlowGraph("e6");
        PDGNode src = new PDGNode(cfg.buildNode("1: int x = 0;"), new HashSet<>(), new HashSet<>());
        PDGNode tgt = new PDGNode(cfg.buildNode("2: return x;"), new HashSet<>(), new HashSet<>());
        PDGEdge edge = new PDGEdge(src, tgt, "x");
        assertEquals(PDGEdge.PDGEdgeType.DATA, edge.getType());
        assertEquals("x", edge.getVariable());
        assertEquals(src, edge.getSource());
        assertEquals(tgt, edge.getTarget());
    }

    @Test
    void testPDGEdge_equalityAndHashCode() {
        ControlFlowGraph cfg = new ControlFlowGraph("eq2");
        PDGNode src = new PDGNode(cfg.buildNode("1: int x = 0;"), new HashSet<>(), new HashSet<>());
        PDGNode tgt = new PDGNode(cfg.buildNode("2: return x;"), new HashSet<>(), new HashSet<>());
        PDGEdge e1 = new PDGEdge(src, tgt, "x");
        PDGEdge e2 = new PDGEdge(src, tgt, "x");
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    void testPDGEdge_controlAndDataNotEqual() {
        ControlFlowGraph cfg = new ControlFlowGraph("neq2");
        PDGNode src = new PDGNode(cfg.buildNode("1: if (x)"), new HashSet<>(), new HashSet<>());
        PDGNode tgt = new PDGNode(cfg.buildNode("2: x = 1;"), new HashSet<>(), new HashSet<>());
        PDGEdge ctrl = new PDGEdge(src, tgt);
        PDGEdge data = new PDGEdge(src, tgt, "x");
        assertFalse(ctrl.equals(data),
                "CONTROL and DATA edges between same nodes must not be equal");
    }

    @Test
    void testPDGEdge_differentVariablesNotEqual() {
        ControlFlowGraph cfg = new ControlFlowGraph("neq3");
        PDGNode src = new PDGNode(cfg.buildNode("1: int x = 0;"), new HashSet<>(), new HashSet<>());
        PDGNode tgt = new PDGNode(cfg.buildNode("2: int y = 0;"), new HashSet<>(), new HashSet<>());
        PDGEdge e1 = new PDGEdge(src, tgt, "x");
        PDGEdge e2 = new PDGEdge(src, tgt, "y");
        assertFalse(e1.equals(e2), "DATA edges for different variables must not be equal");
    }

    // --- ProgramDependenceGraph ---

    @Test
    void testPDG_nullNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProgramDependenceGraph(null),
                "PDG must reject null name");
    }

    @Test
    void testPDG_emptyNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProgramDependenceGraph(""),
                "PDG must reject empty name");
    }

    @Test
    void testPDG_addNullNodeThrows() {
        ProgramDependenceGraph pdg = new ProgramDependenceGraph("test");
        assertThrows(IllegalArgumentException.class,
                () -> pdg.addNode(null),
                "addNode must reject null");
    }

    @Test
    void testPDG_addNullEdgeThrows() {
        ProgramDependenceGraph pdg = new ProgramDependenceGraph("test");
        assertThrows(IllegalArgumentException.class,
                () -> pdg.addEdge(null),
                "addEdge must reject null");
    }

    @Test
    void testPDG_getEdgesOfType_nullThrows() {
        ProgramDependenceGraph pdg = new ProgramDependenceGraph("test");
        assertThrows(IllegalArgumentException.class,
                () -> pdg.getEdgesOfType(null),
                "getEdgesOfType must reject null type");
    }

    @Test
    void testPDG_getControlEdgesFrom_nullThrows() {
        ProgramDependenceGraph pdg = new ProgramDependenceGraph("test");
        assertThrows(IllegalArgumentException.class,
                () -> pdg.getControlEdgesFrom(null),
                "getControlEdgesFrom must reject null node");
    }

    @Test
    void testPDG_getDataEdgesFrom_nullThrows() {
        ProgramDependenceGraph pdg = new ProgramDependenceGraph("test");
        assertThrows(IllegalArgumentException.class,
                () -> pdg.getDataEdgesFrom(null),
                "getDataEdgesFrom must reject null node");
    }

    @Test
    void testPDG_getEdgesTo_nullThrows() {
        ProgramDependenceGraph pdg = new ProgramDependenceGraph("test");
        assertThrows(IllegalArgumentException.class,
                () -> pdg.getEdgesTo(null),
                "getEdgesTo must reject null node");
    }

    @Test
    void testPDG_getNodeByLabel_nullReturnsNull() {
        ProgramDependenceGraph pdg = new ProgramDependenceGraph("test");
        assertNull(pdg.getNodeByLabel(null),
                "getNodeByLabel(null) must return null");
    }

    @Test
    void testPDG_getNodeByLabel_missingLabelReturnsNull() {
        ProgramDependenceGraph pdg = new ProgramDependenceGraph("test");
        assertNull(pdg.getNodeByLabel("nonexistent"),
                "getNodeByLabel for unknown label must return null");
    }

    @Test
    void testPDG_getNodeByLabel_findsNode() {
        ControlFlowGraph cfg = new ControlFlowGraph("lookup");
        Node n = cfg.buildNode("1: int x = 0;");
        cfg.buildEdge(cfg.entry, n,             EdgeLabel.BLANK);
        cfg.buildEdge(n,         cfg.normalExit, EdgeLabel.BLANK);
        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        assertNotNull(pdg.getNodeByLabel("1: int x = 0;"),
                "getNodeByLabel must find an existing node");
    }

    @Test
    void testPDG_nodesAndEdgesAreUnmodifiable() {
        ControlFlowGraph cfg = new ControlFlowGraph("immut");
        cfg.buildEdge(cfg.entry, cfg.normalExit, EdgeLabel.BLANK);
        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        assertThrows(UnsupportedOperationException.class,
                () -> pdg.getEdges().add(null),
                "getEdges() must return an unmodifiable list");
        assertThrows(UnsupportedOperationException.class,
                () -> pdg.getNodes().add(null),
                "getNodes() must return an unmodifiable set");
    }

    // --- PDGBuilder constructor ---

    @Test
    void testConstructor_nullCfgThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new PDGBuilder(null),
                "PDGBuilder must reject a null CFG");
    }

    @Test
    void testBuild_pdgNameMatchesCfgName() {
        ControlFlowGraph cfg = new ControlFlowGraph("myMethod");
        cfg.buildEdge(cfg.entry, cfg.normalExit, EdgeLabel.BLANK);
        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        assertEquals("myMethod", pdg.getName());
    }

    // --- DominatorTree ---

    @Test
    void testDominatorTree_nullCfgThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new DominatorTree(null),
                "DominatorTree must reject a null CFG");
    }

    // =========================================================================
    // 3. DominatorTree API tests
    // =========================================================================

    /**
     * Straight-line: ENTRY → stmt → EXIT.
     * ipostdom(stmt) = normalExit (the only post-dominator between stmt and root).
     * isAncestorOrEqual(null, stmt) must be true (virtual root is ancestor of all).
     * isAncestorOrEqual(stmt, stmt) must be true (a node is an ancestor of itself).
     */
    @Test
    void testDominatorTree_straightLine_ipostdom() {
        ControlFlowGraph cfg = new ControlFlowGraph("dtStraight");
        Node stmt = cfg.buildNode("1: int x = 0;");
        cfg.buildEdge(cfg.entry, stmt,           EdgeLabel.BLANK);
        cfg.buildEdge(stmt,      cfg.normalExit, EdgeLabel.BLANK);

        DominatorTree dt = new DominatorTree(cfg);

        // virtual root (null) is ancestor of stmt
        assertTrue(dt.isAncestorOrEqual(null, stmt),
                "Virtual super-exit must be ancestor of every node");

        // stmt is ancestor-or-equal of itself
        assertTrue(dt.isAncestorOrEqual(stmt, stmt),
                "A node must be an ancestor-or-equal of itself");

        // entry is NOT an ancestor of stmt (entry is not a post-dominator of stmt)
        assertFalse(dt.isAncestorOrEqual(cfg.entry, stmt),
                "ENTRY is not a post-dominator of stmt");

        // The ipostdom map contains stmt
        assertNotNull(dt.getIpostdomMap(),
                "getIpostdomMap must return a non-null map");
        assertTrue(dt.getIpostdomMap().containsKey(stmt),
                "stmt must appear in the ipostdom map");
    }

    /**
     * If-else: stmt is NOT a post-dominator of node1 (the if predicate) because
     * node1's ipostdom is the join node.
     */
    @Test
    void testDominatorTree_ifElse_ipostdomIsJoin() {
        ControlFlowGraph cfg = new ControlFlowGraph("dtIfElse");
        Node node1 = cfg.buildNode("1: if (flag)");
        Node node2 = cfg.buildNode("2: x = 1;");
        Node node3 = cfg.buildNode("3: x = 2;");
        Node node4 = cfg.buildNode("4: return x;");

        cfg.buildEdge(cfg.entry, node1,          EdgeLabel.BLANK);
        cfg.buildEdge(node1,     node2,          EdgeLabel.TRUE);
        cfg.buildEdge(node1,     node3,          EdgeLabel.FALSE);
        cfg.buildEdge(node2,     node4,          EdgeLabel.BLANK);
        cfg.buildEdge(node3,     node4,          EdgeLabel.BLANK);
        cfg.buildEdge(node4,     cfg.normalExit, EdgeLabel.BLANK);

        DominatorTree dt = new DominatorTree(cfg);

        // node4 (join) post-dominates node1 (the if predicate)
        assertTrue(dt.isAncestorOrEqual(node4, node1),
                "Join node4 must post-dominate the if predicate node1");

        // node2 does NOT post-dominate node1 (node2 is only on the true branch)
        assertFalse(dt.isAncestorOrEqual(node2, node1),
                "node2 must NOT post-dominate node1");
    }

    /**
     * isAncestorOrEqual(null, X) must be true for any node X.
     */
    @Test
    void testDominatorTree_virtualRootIsAncestorOfAll() {
        ControlFlowGraph cfg = new ControlFlowGraph("dtRoot");
        Node n = cfg.buildNode("1: return 0;");
        cfg.buildEdge(cfg.entry, n,             EdgeLabel.BLANK);
        cfg.buildEdge(n,         cfg.normalExit, EdgeLabel.BLANK);

        DominatorTree dt = new DominatorTree(cfg);
        assertTrue(dt.isAncestorOrEqual(null, n));
        assertTrue(dt.isAncestorOrEqual(null, cfg.entry));
    }

    // =========================================================================
    // 4. ProgramDependenceGraph query API tests
    // =========================================================================

    @Test
    void testPDG_getControlEdgesFrom_returnsOnlyControlEdges() {
        ControlFlowGraph cfg = new ControlFlowGraph("qapi");
        Node pred = cfg.buildNode("1: if (x)");
        Node body = cfg.buildNode("2: x = 1;");
        Node join = cfg.buildNode("3: return x;");

        cfg.buildEdge(cfg.entry, pred,           EdgeLabel.BLANK);
        cfg.buildEdge(pred,      body,           EdgeLabel.TRUE);
        cfg.buildEdge(pred,      join,           EdgeLabel.FALSE);
        cfg.buildEdge(body,      join,           EdgeLabel.BLANK);
        cfg.buildEdge(join,      cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        PDGNode pdgPred = pdg.getNodeByLabel("1: if (x)");
        assertNotNull(pdgPred);

        List<PDGEdge> ctrlFrom = pdg.getControlEdgesFrom(pdgPred);
        assertTrue(ctrlFrom.stream().allMatch(
                e -> e.getType() == PDGEdge.PDGEdgeType.CONTROL),
                "getControlEdgesFrom must return only CONTROL edges");
    }

    @Test
    void testPDG_getDataEdgesFrom_returnsOnlyDataEdges() {
        ControlFlowGraph cfg = new ControlFlowGraph("qapi2");
        Node def = cfg.buildNode("1: int x = 0;");
        Node use = cfg.buildNode("2: return x;");

        cfg.buildEdge(cfg.entry, def,            EdgeLabel.BLANK);
        cfg.buildEdge(def,       use,            EdgeLabel.BLANK);
        cfg.buildEdge(use,       cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        PDGNode pdgDef = pdg.getNodeByLabel("1: int x = 0;");
        assertNotNull(pdgDef);

        List<PDGEdge> dataFrom = pdg.getDataEdgesFrom(pdgDef);
        assertTrue(dataFrom.stream().allMatch(
                e -> e.getType() == PDGEdge.PDGEdgeType.DATA),
                "getDataEdgesFrom must return only DATA edges");
        assertFalse(dataFrom.isEmpty(), "DATA edge from def node must exist");
    }

    @Test
    void testPDG_getEdgesTo_findsIncomingEdges() {
        ControlFlowGraph cfg = new ControlFlowGraph("qapi3");
        Node def = cfg.buildNode("1: int x = 0;");
        Node use = cfg.buildNode("2: return x;");

        cfg.buildEdge(cfg.entry, def,            EdgeLabel.BLANK);
        cfg.buildEdge(def,       use,            EdgeLabel.BLANK);
        cfg.buildEdge(use,       cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        PDGNode pdgUse = pdg.getNodeByLabel("2: return x;");
        assertNotNull(pdgUse);

        List<PDGEdge> toUse = pdg.getEdgesTo(pdgUse);
        assertFalse(toUse.isEmpty(),
                "There must be at least one edge pointing to the use node");
        assertTrue(toUse.stream().allMatch(e -> e.getTarget().equals(pdgUse)),
                "All edges returned by getEdgesTo must target the queried node");
    }

    // =========================================================================
    // 5. Integration tests
    // =========================================================================

    // -------------------------------------------------------------------------
    // 5a. Minimal / structural checks
    // -------------------------------------------------------------------------

    /**
     * Minimal CFG: ENTRY → EXIT (no real statements).
     * PDG should contain exactly one node (*ENTRY*) and no edges.
     */
    @Test
    void testBuild_emptyCfg_singleNodeNoEdges() {
        ControlFlowGraph cfg = new ControlFlowGraph("empty");
        cfg.buildEdge(cfg.entry, cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();

        assertEquals(1, pdg.getNodes().size(),
                "Only *ENTRY* should be in the PDG");
        assertTrue(pdg.getEdges().isEmpty(),
                "No edges expected for a body-less method");
    }

    /**
     * The PDG must exclude *EXIT* and *THROWN* but include *ENTRY*.
     */
    @Test
    void testBuild_virtualExitNodesExcluded() {
        ControlFlowGraph cfg = new ControlFlowGraph("exclusionCheck");
        Node stmt = cfg.buildNode("1: int x = 0;");
        cfg.buildEdge(cfg.entry, stmt,           EdgeLabel.BLANK);
        cfg.buildEdge(stmt,      cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();

        Set<String> labels = pdg.getNodes().stream()
                .map(PDGNode::getLabel)
                .collect(Collectors.toSet());

        assertTrue(labels.contains("*ENTRY*"),  "*ENTRY* must be in the PDG");
        assertFalse(labels.contains("*EXIT*"),   "*EXIT* must NOT be in the PDG");
        assertFalse(labels.contains("*THROWN*"), "*THROWN* must NOT be in the PDG");
    }

    // -------------------------------------------------------------------------
    // 5b. Straight-line (sequential) code — data dependences only
    // -------------------------------------------------------------------------

    /**
     * CFG: ENTRY → "1: int x = 0;" → "2: int y = x + 1;" → EXIT
     *
     * Expected:
     *   - 3 PDG nodes (*ENTRY*, node1, node2)
     *   - 0 CONTROL edges  (no branching)
     *   - 1 DATA edge: node1 → node2 for "x"
     */
    @Test
    void testBuild_straightLine_dataEdgeForX() {
        ControlFlowGraph cfg = new ControlFlowGraph("seq");
        Node node1 = cfg.buildNode("1: int x = 0;");
        Node node2 = cfg.buildNode("2: int y = x + 1;");

        cfg.buildEdge(cfg.entry, node1,          EdgeLabel.BLANK);
        cfg.buildEdge(node1,     node2,          EdgeLabel.BLANK);
        cfg.buildEdge(node2,     cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();

        assertEquals(3, pdg.getNodes().size(),
                "PDG should have exactly *ENTRY*, node1, node2");

        assertTrue(pdg.getEdgesOfType(PDGEdge.PDGEdgeType.CONTROL).isEmpty(),
                "No control edges in straight-line code");

        assertTrue(hasDataEdge(pdg, "1: int x = 0;", "2: int y = x + 1;", "x"),
                "DATA edge expected: node1 → node2 for 'x'");
    }

    /**
     * CFG: ENTRY → "1: int a = 5;" → "2: int b = 10;" → "3: int c = a + b;" → EXIT
     *
     * Expected DATA edges:
     *   - node1 → node3 for "a"
     *   - node2 → node3 for "b"
     */
    @Test
    void testBuild_straightLine_multipleDataDeps() {
        ControlFlowGraph cfg = new ControlFlowGraph("multiDep");
        Node node1 = cfg.buildNode("1: int a = 5;");
        Node node2 = cfg.buildNode("2: int b = 10;");
        Node node3 = cfg.buildNode("3: int c = a + b;");

        cfg.buildEdge(cfg.entry, node1,          EdgeLabel.BLANK);
        cfg.buildEdge(node1,     node2,          EdgeLabel.BLANK);
        cfg.buildEdge(node2,     node3,          EdgeLabel.BLANK);
        cfg.buildEdge(node3,     cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();

        assertTrue(hasDataEdge(pdg, "1: int a = 5;",  "3: int c = a + b;", "a"),
                "DATA edge expected for 'a': node1 → node3");
        assertTrue(hasDataEdge(pdg, "2: int b = 10;", "3: int c = a + b;", "b"),
                "DATA edge expected for 'b': node2 → node3");
    }

    /**
     * Re-definition kills earlier definition (reaching-definitions KILL).
     *
     * CFG: ENTRY → "1: int x = 1;" → "2: x = 2;" → "3: return x;" → EXIT
     *
     * Expected:
     *   - DATA(x): node2 → node3   (node2's def reaches node3)
     *   - NO DATA(x): node1 → node3 (node1's def is killed by node2)
     */
    @Test
    void testBuild_reDefinitionKillsPreviousDef() {
        ControlFlowGraph cfg = new ControlFlowGraph("reDef");
        Node node1 = cfg.buildNode("1: int x = 1;");
        Node node2 = cfg.buildNode("2: x = 2;");
        Node node3 = cfg.buildNode("3: return x;");

        cfg.buildEdge(cfg.entry, node1,          EdgeLabel.BLANK);
        cfg.buildEdge(node1,     node2,          EdgeLabel.BLANK);
        cfg.buildEdge(node2,     node3,          EdgeLabel.BLANK);
        cfg.buildEdge(node3,     cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();

        assertTrue(hasDataEdge(pdg, "2: x = 2;",    "3: return x;", "x"),
                "node2's def of 'x' should reach node3");
        assertFalse(hasDataEdge(pdg, "1: int x = 1;", "3: return x;", "x"),
                "node1's def of 'x' is killed by node2 and must NOT reach node3");
    }

    // -------------------------------------------------------------------------
    // 5c. If-statement — control dependences
    // -------------------------------------------------------------------------

    /**
     * If-statement (no else):
     *
     *   ENTRY → "1: if (flag)" --TRUE→  "2: x = 1;" --BLANK→ "3: return x;" → EXIT
     *                           --FALSE→ "3: return x;"
     *
     * Expected:
     *   - CONTROL: node1 → node2      (then-body depends on predicate)
     *   - NO CONTROL: node1 → node3   (join point, on both branches)
     *   - DATA(x): node2 → node3
     */
    @Test
    void testBuild_ifNoElse_controlAndDataEdges() {
        ControlFlowGraph cfg = new ControlFlowGraph("ifNoElse");
        Node node1 = cfg.buildNode("1: if (flag)");
        Node node2 = cfg.buildNode("2: x = 1;");
        Node node3 = cfg.buildNode("3: return x;");

        cfg.buildEdge(cfg.entry, node1,          EdgeLabel.BLANK);
        cfg.buildEdge(node1,     node2,          EdgeLabel.TRUE);
        cfg.buildEdge(node1,     node3,          EdgeLabel.FALSE);
        cfg.buildEdge(node2,     node3,          EdgeLabel.BLANK);
        cfg.buildEdge(node3,     cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();

        assertTrue(hasControlEdge(pdg, "1: if (flag)", "2: x = 1;"),
                "CONTROL: if-node → then-body");
        assertFalse(hasControlEdge(pdg, "1: if (flag)", "3: return x;"),
                "Join node must NOT be control-dependent on the if");

        assertTrue(hasDataEdge(pdg, "2: x = 1;", "3: return x;", "x"),
                "DATA(x): node2 → node3");
    }

    /**
     * If-else:
     *
     *   ENTRY → "1: if (flag)" --TRUE→  "2: x = 1;" --BLANK→ "4: return x;" → EXIT
     *                           --FALSE→ "3: x = 2;" --BLANK→ "4: return x;"
     *
     * Expected:
     *   - CONTROL: node1 → node2   (true branch)
     *   - CONTROL: node1 → node3   (false branch)
     *   - NO CONTROL: node1 → node4 (join)
     *   - DATA(x): node2 → node4
     *   - DATA(x): node3 → node4
     */
    @Test
    void testBuild_ifElse_controlAndDataEdges() {
        ControlFlowGraph cfg = new ControlFlowGraph("ifElse");
        Node node1 = cfg.buildNode("1: if (flag)");
        Node node2 = cfg.buildNode("2: x = 1;");
        Node node3 = cfg.buildNode("3: x = 2;");
        Node node4 = cfg.buildNode("4: return x;");

        cfg.buildEdge(cfg.entry, node1,          EdgeLabel.BLANK);
        cfg.buildEdge(node1,     node2,          EdgeLabel.TRUE);
        cfg.buildEdge(node1,     node3,          EdgeLabel.FALSE);
        cfg.buildEdge(node2,     node4,          EdgeLabel.BLANK);
        cfg.buildEdge(node3,     node4,          EdgeLabel.BLANK);
        cfg.buildEdge(node4,     cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();

        assertTrue(hasControlEdge(pdg, "1: if (flag)", "2: x = 1;"),
                "CONTROL: node1 → node2 (true branch)");
        assertTrue(hasControlEdge(pdg, "1: if (flag)", "3: x = 2;"),
                "CONTROL: node1 → node3 (false branch)");
        assertFalse(hasControlEdge(pdg, "1: if (flag)", "4: return x;"),
                "Join node4 must NOT be control-dependent on node1");

        assertTrue(hasDataEdge(pdg, "2: x = 1;", "4: return x;", "x"),
                "DATA(x): node2 → node4");
        assertTrue(hasDataEdge(pdg, "3: x = 2;", "4: return x;", "x"),
                "DATA(x): node3 → node4");
    }

    // -------------------------------------------------------------------------
    // 5d. While loop — back-edge data and loop-body control dependences
    // -------------------------------------------------------------------------

    /**
     * While-loop:
     *
     *   ENTRY → "1: int i = 0;" → "2: while (i < 10)" --TRUE→  "3: i++;" → "2: ..."
     *                                                   --FALSE→ EXIT
     */
    @Test
    void testBuild_whileLoop_controlAndDataEdges() {
        ControlFlowGraph cfg = new ControlFlowGraph("whileLoop");
        Node node1 = cfg.buildNode("1: int i = 0;");
        Node node2 = cfg.buildNode("2: while (i < 10)");
        Node node3 = cfg.buildNode("3: i++;");

        cfg.buildEdge(cfg.entry, node1,          EdgeLabel.BLANK);
        cfg.buildEdge(node1,     node2,          EdgeLabel.BLANK);
        cfg.buildEdge(node2,     node3,          EdgeLabel.TRUE);
        cfg.buildEdge(node2,     cfg.normalExit, EdgeLabel.FALSE);
        cfg.buildEdge(node3,     node2,          EdgeLabel.BLANK); // back edge

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();

        assertTrue(hasControlEdge(pdg, "2: while (i < 10)", "3: i++;"),
                "CONTROL: while-node → loop body");
        assertTrue(hasDataEdge(pdg, "1: int i = 0;", "2: while (i < 10)", "i"),
                "DATA(i): node1 → node2 (initial def)");
        assertTrue(hasDataEdge(pdg, "3: i++;", "2: while (i < 10)", "i"),
                "DATA(i): node3 → node2 (loop back-edge)");
        // Loop-carried: incremented i can also flow back to itself (body reads i)
        assertTrue(hasDataEdge(pdg, "1: int i = 0;", "3: i++;", "i"),
                "DATA(i): node1 → node3 (first-iteration def reaches body)");
        assertTrue(hasDataEdge(pdg, "3: i++;", "3: i++;", "i"),
                "DATA(i): node3 → node3 (loop-carried self-dependence)");
    }

    // -------------------------------------------------------------------------
    // 5e. Nested control dependence (if inside while)
    // -------------------------------------------------------------------------

    /**
     * While with if inside:
     *
     *   ENTRY → "1: int i = 0;" → "2: while (i < 10)"
     *     --TRUE→ "3: if (i > 5)" --TRUE→  "4: x = i;" → "2: ..."
     *                              --FALSE→ "2: ..."
     *     --FALSE→ EXIT
     *
     * Expected:
     *   - CONTROL: node2 → node3   (if body depends on while)
     *   - CONTROL: node2 → node4   (inner body transitively depends on while)
     *   - CONTROL: node3 → node4   (inner body depends on inner if)
     */
    @Test
    void testBuild_nestedControl_ifInsideWhile() {
        ControlFlowGraph cfg = new ControlFlowGraph("nestedCtrl");
        Node node1 = cfg.buildNode("1: int i = 0;");
        Node node2 = cfg.buildNode("2: while (i < 10)");
        Node node3 = cfg.buildNode("3: if (i > 5)");
        Node node4 = cfg.buildNode("4: x = i;");

        cfg.buildEdge(cfg.entry, node1,          EdgeLabel.BLANK);
        cfg.buildEdge(node1,     node2,          EdgeLabel.BLANK);
        cfg.buildEdge(node2,     node3,          EdgeLabel.TRUE);
        cfg.buildEdge(node2,     cfg.normalExit, EdgeLabel.FALSE);
        cfg.buildEdge(node3,     node4,          EdgeLabel.TRUE);
        cfg.buildEdge(node3,     node2,          EdgeLabel.FALSE); // back edge
        cfg.buildEdge(node4,     node2,          EdgeLabel.BLANK); // back edge

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();

        assertTrue(hasControlEdge(pdg, "2: while (i < 10)", "3: if (i > 5)"),
                "CONTROL: while → inner if");
        assertTrue(hasControlEdge(pdg, "3: if (i > 5)", "4: x = i;"),
                "CONTROL: inner if → body");
        assertTrue(hasControlEdge(pdg, "2: while (i < 10)", "4: x = i;"),
                "CONTROL: while → innermost body (transitive)");
    }

    // -------------------------------------------------------------------------
    // 5f. For loop
    // -------------------------------------------------------------------------

    /**
     * For-loop equivalent CFG (the StatementNodeBuilder collapses init/cond/update
     * into one node, so we mirror that here).
     *
     *   ENTRY → "1: int sum = 0;"
     *          → "2: for ( int i = 0 ; i < 10 ; i++)"
     *              --TRUE→ "3: sum += i;" → "2: ..."
     *              --FALSE→ EXIT
     */
    @Test
    void testBuild_forLoop_controlAndDataEdges() {
        ControlFlowGraph cfg = new ControlFlowGraph("forLoop");
        Node node1 = cfg.buildNode("1: int sum = 0;");
        Node node2 = cfg.buildNode("2: for ( int i = 0 ; i < 10 ; i++)");
        Node node3 = cfg.buildNode("3: sum += i;");

        cfg.buildEdge(cfg.entry, node1,          EdgeLabel.BLANK);
        cfg.buildEdge(node1,     node2,          EdgeLabel.BLANK);
        cfg.buildEdge(node2,     node3,          EdgeLabel.TRUE);
        cfg.buildEdge(node2,     cfg.normalExit, EdgeLabel.FALSE);
        cfg.buildEdge(node3,     node2,          EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();

        assertTrue(hasControlEdge(pdg, "2: for ( int i = 0 ; i < 10 ; i++)", "3: sum += i;"),
                "CONTROL: for-node → body");
        assertTrue(hasDataEdge(pdg,
                "2: for ( int i = 0 ; i < 10 ; i++)",
                "3: sum += i;", "i"),
                "DATA(i): for-node → body (i is defined and used in for)");
        assertTrue(hasDataEdge(pdg, "1: int sum = 0;", "3: sum += i;", "sum"),
                "DATA(sum): node1 → node3");
        // Loop-carried: node3 redefines sum, which flows back to node3 itself
        assertTrue(hasDataEdge(pdg, "3: sum += i;", "3: sum += i;", "sum"),
                "DATA(sum): node3 → node3 (loop-carried)");
    }

    // -------------------------------------------------------------------------
    // 5g. Do-while loop
    // -------------------------------------------------------------------------

    /**
     * Do-while: body executes at least once before the condition is checked.
     *
     *   ENTRY → "1: do" → "2: x++;" → "3: while (x < 5)" --TRUE→  "2: x++;"
     *                                                       --FALSE→ EXIT
     */
    @Test
    void testBuild_doWhileLoop_controlAndDataEdges() {
        ControlFlowGraph cfg = new ControlFlowGraph("doWhile");
        Node node1 = cfg.buildNode("1: int x = 0;");
        Node node2 = cfg.buildNode("2: x++;");
        Node node3 = cfg.buildNode("3: while (x < 5)");

        cfg.buildEdge(cfg.entry, node1,          EdgeLabel.BLANK);
        cfg.buildEdge(node1,     node2,          EdgeLabel.BLANK);
        cfg.buildEdge(node2,     node3,          EdgeLabel.BLANK);
        cfg.buildEdge(node3,     node2,          EdgeLabel.TRUE);  // back edge
        cfg.buildEdge(node3,     cfg.normalExit, EdgeLabel.FALSE);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();

        // Body (node2) is control-dependent on while condition (node3)
        assertTrue(hasControlEdge(pdg, "3: while (x < 5)", "2: x++;"),
                "CONTROL: while-condition → body");

        // Data: initial x flows into body and condition
        assertTrue(hasDataEdge(pdg, "1: int x = 0;", "2: x++;", "x"),
                "DATA(x): init → body (first iteration)");
        assertTrue(hasDataEdge(pdg, "2: x++;", "3: while (x < 5)", "x"),
                "DATA(x): body → condition (updated x flows to check)");
    }

    // -------------------------------------------------------------------------
    // 5h. Try-catch (exception edges)
    // -------------------------------------------------------------------------

    /**
     * Try-catch: a THROWN edge from the try-body reaches the catch block.
     *
     *   ENTRY → "1: try" → "2: int x = riskyOp();" --BLANK→ "4: return x;" → EXIT
     *                                                --THROWN→ "3: catch" → EXIT
     */
    @Test
    void testBuild_tryCatch_thrownEdge() {
        ControlFlowGraph cfg = new ControlFlowGraph("tryCatch");
        Node node1 = cfg.buildNode("1: try");
        Node node2 = cfg.buildNode("2: int x = riskyOp();");
        Node node3 = cfg.buildNode("3: catch ( Exception e )");
        Node node4 = cfg.buildNode("4: return x;");

        cfg.buildEdge(cfg.entry, node1,          EdgeLabel.BLANK);
        cfg.buildEdge(node1,     node2,          EdgeLabel.BLANK);
        cfg.buildEdge(node2,     node4,          EdgeLabel.BLANK);
        cfg.buildEdge(node2,     node3,          EdgeLabel.THROWN);
        cfg.buildEdge(node3,     cfg.normalExit, EdgeLabel.BLANK);
        cfg.buildEdge(node4,     cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();

        // All real nodes should appear in the PDG
        Set<String> labels = pdg.getNodes().stream()
                .map(PDGNode::getLabel).collect(Collectors.toSet());
        assertTrue(labels.contains("1: try"),                       "try node must appear");
        assertTrue(labels.contains("2: int x = riskyOp();"),        "body node must appear");
        assertTrue(labels.contains("3: catch ( Exception e )"),     "catch node must appear");
        assertTrue(labels.contains("4: return x;"),                  "return node must appear");

        // The catch parameter 'e' should be defined at the catch node
        PDGNode catchNode = pdg.getNodeByLabel("3: catch ( Exception e )");
        assertNotNull(catchNode);
        assertTrue(catchNode.getDefs().contains("e"),
                "Catch node must define the exception parameter 'e'");
    }

    // -------------------------------------------------------------------------
    // 5i. Throw statement (abrupt exit edge)
    // -------------------------------------------------------------------------

    /**
     * Throw: the throw node has a THROWN edge directly to abruptExit.
     *
     *   ENTRY → "1: if (x < 0)" --TRUE→  "2: throw new Exception();" → THROWN
     *                             --FALSE→ "3: return x;"              → EXIT
     */
    @Test
    void testBuild_throwStatement_abruptExit() {
        ControlFlowGraph cfg = new ControlFlowGraph("throwStmt");
        Node node1 = cfg.buildNode("1: if (x < 0)");
        Node node2 = cfg.buildNode("2: throw new Exception();");
        Node node3 = cfg.buildNode("3: return x;");

        cfg.buildEdge(cfg.entry, node1,           EdgeLabel.BLANK);
        cfg.buildEdge(node1,     node2,           EdgeLabel.TRUE);
        cfg.buildEdge(node1,     node3,           EdgeLabel.FALSE);
        cfg.buildEdge(node2,     cfg.abruptExit,  EdgeLabel.THROWN);
        cfg.buildEdge(node3,     cfg.normalExit,  EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();

        // Both branches of the if are control-dependent on node1
        assertTrue(hasControlEdge(pdg, "1: if (x < 0)", "2: throw new Exception();"),
                "CONTROL: if → throw branch");
        assertTrue(hasControlEdge(pdg, "1: if (x < 0)", "3: return x;"),
                "CONTROL: if → normal branch");
    }

    // -------------------------------------------------------------------------
    // 5j. Switch statement
    // -------------------------------------------------------------------------

    /**
     * Switch with two cases and a break:
     *
     *   ENTRY → "1: switch (k)"
     *     --CASE case 0:→ "2: x = 0;" → "4: return x;" → EXIT
     *     --CASE case 1:→ "3: x = 1;" → "4: return x;"
     */
    @Test
    void testBuild_switchStatement_controlEdges() {
        ControlFlowGraph cfg = new ControlFlowGraph("switchStmt");
        Node node1 = cfg.buildNode("1: switch (k)");
        Node node2 = cfg.buildNode("2: x = 0;");
        Node node3 = cfg.buildNode("3: x = 1;");
        Node node4 = cfg.buildNode("4: return x;");

        cfg.buildEdge(cfg.entry, node1,          EdgeLabel.BLANK);
        cfg.buildEdge(node1,     node2,          EdgeLabel.CASE, "case 0:");
        cfg.buildEdge(node1,     node3,          EdgeLabel.CASE, "case 1:");
        cfg.buildEdge(node2,     node4,          EdgeLabel.BLANK);
        cfg.buildEdge(node3,     node4,          EdgeLabel.BLANK);
        cfg.buildEdge(node4,     cfg.normalExit, EdgeLabel.BLANK);

        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();

        // Both case bodies are control-dependent on the switch node
        assertTrue(hasControlEdge(pdg, "1: switch (k)", "2: x = 0;"),
                "CONTROL: switch → case 0 body");
        assertTrue(hasControlEdge(pdg, "1: switch (k)", "3: x = 1;"),
                "CONTROL: switch → case 1 body");

        // Join point is NOT control-dependent on the switch
        assertFalse(hasControlEdge(pdg, "1: switch (k)", "4: return x;"),
                "Join node must NOT be control-dependent on switch");

        // Data: both case defs of x reach the return
        assertTrue(hasDataEdge(pdg, "2: x = 0;", "4: return x;", "x"),
                "DATA(x): case0 → return");
        assertTrue(hasDataEdge(pdg, "3: x = 1;", "4: return x;", "x"),
                "DATA(x): case1 → return");
    }

    // -------------------------------------------------------------------------
    // 5k. Dead / unreachable code
    // -------------------------------------------------------------------------

    /**
     * A node with no path to any exit is unreachable ("dead code").
     * The PDG builder must not crash and must still include the dead node
     * (it is a real statement node even if unreachable).
     *
     *   ENTRY → "1: return 0;" → EXIT
     *   "2: int dead = 1;"      (no edges to or from it)
     */
    @Test
    void testBuild_deadCode_doesNotCrash() {
        ControlFlowGraph cfg = new ControlFlowGraph("deadCode");
        Node live = cfg.buildNode("1: return 0;");
        Node dead = cfg.buildNode("2: int dead = 1;");

        cfg.buildEdge(cfg.entry, live,           EdgeLabel.BLANK);
        cfg.buildEdge(live,      cfg.normalExit, EdgeLabel.BLANK);
        // dead node has no edges at all

        // Must not throw
        ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
        assertNotNull(pdg);

        Set<String> labels = pdg.getNodes().stream()
                .map(PDGNode::getLabel).collect(Collectors.toSet());
        assertTrue(labels.contains("1: return 0;"), "Live node must be in PDG");
        // dead node still appears in cfg.nodes() and should be in the PDG
        assertTrue(labels.contains("2: int dead = 1;"), "Dead node must still be in PDG");
    }

    // =========================================================================
    // Helper predicates
    // =========================================================================

    /** Returns true iff the PDG has a CONTROL edge from srcLabel to tgtLabel. */
    private static boolean hasControlEdge(ProgramDependenceGraph pdg,
                                           String srcLabel, String tgtLabel) {
        return pdg.getEdgesOfType(PDGEdge.PDGEdgeType.CONTROL).stream().anyMatch(e ->
                e.getSource().getLabel().equals(srcLabel) &&
                e.getTarget().getLabel().equals(tgtLabel));
    }

    /** Returns true iff the PDG has a DATA edge from srcLabel to tgtLabel for variable. */
    private static boolean hasDataEdge(ProgramDependenceGraph pdg,
                                        String srcLabel, String tgtLabel, String variable) {
        return pdg.getEdgesOfType(PDGEdge.PDGEdgeType.DATA).stream().anyMatch(e ->
                variable.equals(e.getVariable()) &&
                e.getSource().getLabel().equals(srcLabel) &&
                e.getTarget().getLabel().equals(tgtLabel));
    }
}