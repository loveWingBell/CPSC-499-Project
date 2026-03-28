// CPSC 499 Assignment 4 - Elda Britu - 30158734 - March 28, 2025

package org.lsmr.pdg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lsmr.cfg.ControlFlowGraph;
import org.lsmr.cfg.Edge;
import org.lsmr.cfg.Node;

// Computes the post-dominator tree of a CFG, which is needed to derive control-dependence edges for the PDG.

public class DominatorTree {
    
    // Maps each CFG node to its immediate post-dominator.
    // A value of {@code null} means the node's immediate post-dominator is the virtual super-exit root (i.e. the node is directly controlled by "the program's exit", so it is in the outermost control region).
    private final Map<Node, Node> ipostdom;

    // Reverse-post-order numbers used by the intersect function.
    // The virtual super-exit (null) is assigned RPO number 0.
    // Real nodes are assigned RPO number = (their position + 1) so 0 is reserved.
    private final Map<Node, Integer> rpo;

    public DominatorTree(ControlFlowGraph cfg) {
        if (cfg == null) throw new IllegalArgumentException("cfg must not be null");

        List<Node> allNodes = new ArrayList<>(cfg.nodes());

        // Build predecessor map for the reversed CFG.
        Map<Node, List<Node>> reversePreds = buildReversePredecessors(cfg, allNodes);

        // Compute RPO numbers on the reversed CFG (DFS from virtual super-exit).
        rpo = computeRPO(cfg, allNodes);

        // Sort nodes by ascending RPO number for the Cooper et al. sweep.
        List<Node> rpoOrder = new ArrayList<>(allNodes);
        rpoOrder.sort(Comparator.comparingInt(n -> rpo.getOrDefault(n, Integer.MAX_VALUE)));

        // Run the iterative dominator algorithm.
        ipostdom = computeIpostdom(rpoOrder, reversePreds);
    }

    // Public API ------------------------------

    // Returns the immediate post-dominator of {@code node}.
    public Node getImmediatePostDominator(Node node) {
        return ipostdom.get(node);
    }

    /**
     * Returns an unmodifiable view of the full ipostdom map.
     * A {@code null} value means the virtual super-exit is the ipostdom.
     */
    public Map<Node, Node> getIpostdomMap() {
        return Collections.unmodifiableMap(ipostdom);
    }

    /**
     * Tests whether {@code ancestor} is an ancestor-or-equal of {@code node}
     * in the post-dominator tree.
     * {@code null} (virtual super-exit root) is an ancestor of every node.
     *
     * @param ancestor candidate ancestor; {@code null} = virtual root
     * @param node     the node to test; must not be null
     * @return {@code true} if {@code ancestor} is an ancestor-or-equal of {@code node}
     */
    public boolean isAncestorOrEqual(Node ancestor, Node node) {
        Node current = node;
        while (current != null) {
            if (current.equals(ancestor)) return true;
            current = ipostdom.get(current);
        }
        // Walked off the top — reached virtual root (null)
        return ancestor == null;
    }

    // -------------------------------------------------------------------------
    // Step 1: build reversed-CFG predecessor map
    // -------------------------------------------------------------------------

    /**
     * In the reversed CFG, node N's predecessors = CFG successors of N
     * (targets of N's out-edges).  Additionally, the virtual super-exit (null)
     * is a predecessor of {@code normalExit} and {@code abruptExit}.
     */
    private static Map<Node, List<Node>> buildReversePredecessors(
            ControlFlowGraph cfg, List<Node> allNodes) {

        Map<Node, List<Node>> preds = new HashMap<>();
        for (Node n : allNodes) preds.put(n, new ArrayList<>());

        for (Node n : allNodes) {
            for (Edge e : n.outEdges()) {
                Node target = e.target();
                if (target != null) {
                    preds.computeIfAbsent(n, k -> new ArrayList<>()).add(target);
                }
            }
        }

        // Virtual super-exit (null) → normalExit and abruptExit (in reversed CFG,
        // normalExit/abruptExit have the super-exit as a predecessor).
        preds.computeIfAbsent(cfg.normalExit, k -> new ArrayList<>()).add(null);
        if (!cfg.normalExit.equals(cfg.abruptExit)) {
            preds.computeIfAbsent(cfg.abruptExit, k -> new ArrayList<>()).add(null);
        }

        return preds;
    }

    // -------------------------------------------------------------------------
    // Step 2: compute RPO numbers via iterative DFS on reversed CFG
    // -------------------------------------------------------------------------

    /**
     * Assigns reverse-post-order numbers by DFS on the reversed CFG from the
     * virtual super-exit.  In the reversed CFG, the successors of a real node N
     * are the CFG in-edge sources of N (i.e. CFG predecessors of N).  The
     * super-exit's successors are {@code normalExit} and {@code abruptExit}.
     *
     * <p>RPO number 0 is reserved for the virtual super-exit.  Real nodes
     * receive numbers 1 … n in RPO order (lower = closer to root = processed
     * earlier by the Cooper et al. sweep).
     */
    private static Map<Node, Integer> computeRPO(ControlFlowGraph cfg, List<Node> allNodes) {
        List<Node> postOrder = new ArrayList<>();
        Set<Node> visited   = new HashSet<>();

        // Iterative DFS. Each stack frame: [node (null=super-exit), childIndex, childList]
        Deque<Object[]> stack = new ArrayDeque<>();

        List<Node> superSuccessors = new ArrayList<>();
        superSuccessors.add(cfg.normalExit);
        if (!cfg.normalExit.equals(cfg.abruptExit))
            superSuccessors.add(cfg.abruptExit);

        // Mark super-exit visited immediately (it's the root)
        stack.push(new Object[]{null, 0, superSuccessors});

        while (!stack.isEmpty()) {
            Object[] frame   = stack.peek();
            @SuppressWarnings("unchecked")
            List<Node> succs = (List<Node>) frame[2];
            int idx          = (int) frame[1];

            if (idx >= succs.size()) {
                // All children done → record post-order for real nodes
                stack.pop();
                Node n = (Node) frame[0];
                if (n != null) postOrder.add(n);
                // super-exit (null) is the root — not added to postOrder list
            } else {
                frame[1] = idx + 1;
                Node child = succs.get(idx);
                if (child == null || visited.contains(child)) continue;
                visited.add(child);

                // Successors of child in reversed CFG = CFG predecessors of child
                List<Node> childSuccs = new ArrayList<>();
                for (Edge e : child.inEdges())
                    childSuccs.add(e.source());

                stack.push(new Object[]{child, 0, childSuccs});
            }
        }

        // Any node unreachable from exits (dead code) goes last
        for (Node n : allNodes) {
            if (!visited.contains(n))
                postOrder.add(n);
        }

        // Convert post-order to RPO:
        // Last entry in postOrder = first in RPO = RPO number 1 (root=0 reserved).
        Map<Node, Integer> rpoMap = new HashMap<>();
        int size = postOrder.size();
        for (int i = 0; i < size; i++) {
            // post-order index i → RPO number (size - i), starting at 1
            rpoMap.put(postOrder.get(i), size - i);
        }
        return rpoMap;
    }

    // -------------------------------------------------------------------------
    // Step 3: Cooper et al. iterative dominator computation
    // -------------------------------------------------------------------------

    /**
     * Runs the Cooper et al. (2001) iterative dominator algorithm on the
     * reversed CFG.  The "dominator" of a node in the reversed CFG is its
     * post-dominator in the original CFG.
     *
     * <p>The virtual super-exit (null) is always considered processed and
     * its own ipostdom is null (root maps to itself).
     *
     * @param rpoOrder    real nodes sorted by ascending RPO number
     * @param reversePreds predecessor map in the reversed CFG
     * @return map from each real node to its immediate post-dominator
     */
    private Map<Node, Node> computeIpostdom(
            List<Node> rpoOrder, Map<Node, List<Node>> reversePreds) {

        // Use a sentinel map: a node is "processed" when it appears in dom.
        // The virtual super-exit (null) is always processed (ipostdom[root]=root).
        Map<Node, Node> dom = new HashMap<>();
        // We use a special sentinel to mean "null maps to null" without
        // conflating with absent keys: we store null→null by tracking separately.
        // Since HashMap allows null keys, dom.put(null, null) is legal.
        dom.put(null, null); // virtual super-exit: ipostdom = itself

        boolean changed = true;
        while (changed) {
            changed = false;

            for (Node b : rpoOrder) {
                List<Node> preds = reversePreds.getOrDefault(b, Collections.emptyList());

                // Find the first processed predecessor
                Node newIdom = null;
                boolean found = false;
                for (Node p : preds) {
                    // null == virtual super-exit, always in dom
                    if (p == null || dom.containsKey(p)) {
                        newIdom = p;
                        found   = true;
                        break;
                    }
                }
                if (!found) continue; // no processed predecessor yet

                // Intersect with every other processed predecessor
                for (Node p : preds) {
                    boolean processed = (p == null || dom.containsKey(p));
                    if (!processed) continue;
                    if (nodesEqual(p, newIdom)) continue;
                    newIdom = intersect(p, newIdom, dom);
                }

                // Did ipostdom[b] change?
                boolean alreadySet = dom.containsKey(b);
                Node oldIdom       = alreadySet ? dom.get(b) : null;

                if (!alreadySet || !nodesEqual(oldIdom, newIdom)) {
                    dom.put(b, newIdom);
                    if (!alreadySet || !nodesEqual(oldIdom, newIdom))
                        changed = true;
                }
            }
        }

        // Remove the virtual super-exit entry; callers never query it
        dom.remove(null);
        return dom;
    }

    /**
     * The intersect function from Cooper et al.
     * Walks both fingers up the partially built dominator tree until they meet.
     * Uses RPO numbers: lower RPO = closer to root → move the finger with the
     * higher RPO number upward.
     */
    private Node intersect(Node b1, Node b2, Map<Node, Node> dom) {
        Node finger1 = b1;
        Node finger2 = b2;

        while (!nodesEqual(finger1, finger2)) {
            while (rpoNum(finger1) > rpoNum(finger2)) {
                finger1 = dom.get(finger1); // move up
            }
            while (rpoNum(finger2) > rpoNum(finger1)) {
                finger2 = dom.get(finger2); // move up
            }
        }
        return finger1;
    }

    /**
     * RPO number of a node.  The virtual super-exit (null) gets 0, which is
     * lower than every real node, so it wins the intersect race as the root.
     */
    private int rpoNum(Node n) {
        if (n == null) return 0;
        return rpo.getOrDefault(n, Integer.MAX_VALUE);
    }

    /** Null-safe node equality. */
    private static boolean nodesEqual(Node a, Node b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
}