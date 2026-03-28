// CPSC 499 Assignment 4 - Elda Britu - 30158734 - March 28, 2025

package org.lsmr.pdg;

import org.lsmr.cfg.ControlFlowGraph;
import org.lsmr.cfg.Edge;
import org.lsmr.cfg.Node;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Builds a PDG from a CFG in three steps:
// - Create nodes for each real CFG node (exclude exit/throw, include entry).
// - Compute control dependencies using the post-dominator tree.
// - Compute data dependencies via reaching-definitions (link defs to uses).

public class PDGBuilder {

    private final ControlFlowGraph cfg;
    
    // Java keywords + common type names that should never be treated as variables.
    static final Set<String> KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "abstract","assert","boolean","break","byte","case","catch","char","class",
        "const","continue","default","do","double","else","extends","final","finally",
        "float","for","goto","if","implements","import","instanceof","int","interface",
        "long","native","new","null","package","private","protected","public","return",
        "short","static","strictfp","super","switch","synchronized","this","throw",
        "throws","transient","try","true","false","void","volatile","while",
        // common non-variable identifiers from the label format
        "block","try","do"
    )));

    // All Java-style identifiers.
    private static final Pattern IDENT_PAT = Pattern.compile(
        "\\b([a-zA-Z_$][a-zA-Z0-9_$]*)\\b"
    );

    private static final Pattern ASSIGN_LHS_PAT = Pattern.compile(
        "\\b([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*(?:[+\\-*/%&|^]|<<|>>|>>>)?=(?!=)"
    );

    static final Pattern POST_INC_DEC_PAT = Pattern.compile(
        "\\b([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*(?:\\+\\+|--)"
    );

    private static final Pattern PRE_INC_DEC_PAT = Pattern.compile(
        "(?:\\+\\+|--)\\s*([a-zA-Z_$][a-zA-Z0-9_$]*)\\b"
    );

    private static final Pattern TYPE_DECL_PAT = Pattern.compile(
        "\\b(?:int|long|short|byte|char|float|double|boolean" +
        "|[A-Z][a-zA-Z0-9_$]*)(?:\\s*\\[\\s*\\])*\\s+" +
        "([a-zA-Z_$][a-zA-Z0-9_$]*)\\b"
    );

    public PDGBuilder(ControlFlowGraph cfg) {
        if (cfg == null) throw new IllegalArgumentException("cfg must not be null");
        this.cfg = cfg;
    }

    // Builds and returns the PDG for the supplied CFG.
    public ProgramDependenceGraph build() {
        ProgramDependenceGraph pdg = new ProgramDependenceGraph(cfg.name());

        // Phase 1: create PDG nodes
        Map<Node, PDGNode> nodeMap = buildNodeMap(pdg);

        // Phase 2: control dependence
        DominatorTree postDomTree = new DominatorTree(cfg);
        computeControlDependence(pdg, nodeMap, postDomTree);

        // Phase 3: data dependence
        computeDataDependence(pdg, nodeMap);

        return pdg;
    }

    // Phase 1: Creates one {@link PDGNode} per real CFG node (skips {@code *EXIT*} and {@code *THROWN*})
    // - Registers them all in {@code pdg}, and returns the CFG-node -> PDG-node mapping.
    private Map<Node, PDGNode> buildNodeMap(ProgramDependenceGraph pdg) {
        Map<Node, PDGNode> nodeMap = new LinkedHashMap<>();
        for (Node n : cfg.nodes()) {
            if (isVirtualExitNode(n)) continue;
            Set<String> defs = extractDefs(n.label());
            Set<String> uses = extractUses(n.label());
            PDGNode pdgNode = new PDGNode(n, defs, uses);
            nodeMap.put(n, pdgNode);
            pdg.addNode(pdgNode);
        }
        return nodeMap;
    }

    // Phase 2: A CONTROL edge {@code A->node} added to the PDG.
    private void computeControlDependence(ProgramDependenceGraph pdg,
                                          Map<Node, PDGNode> nodeMap,
                                          DominatorTree postDomTree) {

        Set<PDGEdge> seen = new HashSet<>();

        for (Node a : cfg.nodes()) {
            PDGNode pdgA = nodeMap.get(a);
            if (pdgA == null) continue; // skip virtual exit nodes

            Node ipostdomA = postDomTree.getImmediatePostDominator(a);
            // null == virtual super-exit root; walk stops when current == null

            for (Edge e : a.outEdges()) {
                Node b = e.target();
                if (b == null) continue;

                // Walk from b up to (exclusive) ipostdomA
                Node current = b;
                while (current != null && !current.equals(ipostdomA)) {
                    PDGNode pdgCurrent = nodeMap.get(current);
                    if (pdgCurrent != null && !pdgA.equals(pdgCurrent)) {
                        PDGEdge controlEdge = new PDGEdge(pdgA, pdgCurrent);
                        if (seen.add(controlEdge)) {
                            pdg.addEdge(controlEdge);
                        }
                    }
                    current = postDomTree.getImmediatePostDominator(current);
                }
            }
        }
    }

    // Phase 3: Runs a forward iterative reaching-definitions analysis and emits a DATA edge for every (definition, use) pair where the definition reaches the use.
    private void computeDataDependence(ProgramDependenceGraph pdg,
                                       Map<Node, PDGNode> nodeMap) {

        List<Node> cfgNodes = new ArrayList<>(cfg.nodes());

        // IN and OUT sets per CFG node
        Map<Node, Map<String, Set<Node>>> inMap  = new HashMap<>();
        Map<Node, Map<String, Set<Node>>> outMap  = new HashMap<>();

        for (Node n : cfgNodes) {
            inMap.put(n,  new HashMap<>());
            outMap.put(n, new HashMap<>());
        }

        // Seed OUT with GEN sets (definitions produced at each node)
        for (Node n : cfgNodes) {
            PDGNode pn = nodeMap.get(n);
            if (pn == null) continue;
            Map<String, Set<Node>> out = outMap.get(n);
            for (String def : pn.getDefs()) {
                Set<Node> s = new HashSet<>();
                s.add(n);
                out.put(def, s);
            }
        }

        // Worklist: process every node, re-add successors when OUT changes
        Queue<Node> worklist = new LinkedList<>(cfgNodes);

        while (!worklist.isEmpty()) {
            Node n = worklist.poll();

            // IN[n] = ∪ OUT[pred]  for all predecessors of n
            Map<String, Set<Node>> newIn = new HashMap<>();
            for (Edge e : n.inEdges()) {
                Node pred = e.source();
                if (pred != null) mergeInto(newIn, outMap.get(pred));
            }
            inMap.put(n, newIn);

            // OUT[n] = GEN[n] ∪ (IN[n] − KILL[n])
            // KILL[n] removes all other definitions of variables that n defines.
            Map<String, Set<Node>> newOut = new HashMap<>(newIn);
            PDGNode pn = nodeMap.get(n);
            if (pn != null) {
                for (String def : pn.getDefs()) {
                    // Kill previous definitions of def, then gen this one
                    Set<Node> fresh = new HashSet<>();
                    fresh.add(n);
                    newOut.put(def, fresh);
                }
            }

            if (!newOut.equals(outMap.get(n))) {
                outMap.put(n, newOut);
                for (Edge e : n.outEdges()) {
                    if (e.target() != null) worklist.add(e.target());
                }
            }
        }

        // Emit DATA edges: for each node u, for each variable v ∈ uses(u),
        // for each definition node d of v that reaches u -> add DATA edge d->u.
        Set<PDGEdge> seen = new HashSet<>();

        for (Node u : cfgNodes) {
            PDGNode pdgU = nodeMap.get(u);
            if (pdgU == null) continue;

            Map<String, Set<Node>> in = inMap.get(u);
            if (in == null) continue;

            for (String var : pdgU.getUses()) {
                Set<Node> defNodes = in.get(var);
                if (defNodes == null) continue;

                for (Node defNode : defNodes) {
                    PDGNode pdgDef = nodeMap.get(defNode);
                    if (pdgDef == null) continue;
                    PDGEdge dataEdge = new PDGEdge(pdgDef, pdgU, var);
                    if (seen.add(dataEdge)) {
                        pdg.addEdge(dataEdge);
                    }
                }
            }
        }
    }

    // Union of {@code src} into {@code dst} (per-variable set union).
    private static void mergeInto(Map<String, Set<Node>> dst,
                                   Map<String, Set<Node>> src) {
        if (src == null) return;
        for (Map.Entry<String, Set<Node>> entry : src.entrySet()) {
            dst.computeIfAbsent(entry.getKey(), k -> new HashSet<>())
               .addAll(entry.getValue());
        }
    }

    // Def / use extraction helpers ----------------------------------------------------------

    // Strips the {@code "N: "} numeric counter prefix from a node label and returns the statement text.
    static String stripPrefix(String label) {
        int colon = label.indexOf(':');
        if (colon >= 0 && colon + 1 < label.length())
            return label.substring(colon + 1).trim();
        return label;
    }

    // Returns {@code true} for virtual CFG exit nodes that should not appear in the PDG ({@code *EXIT*} and {@code *THROWN*}).
    static boolean isVirtualExitNode(Node n) {
        String lbl = n.label();
        return "*EXIT*".equals(lbl) || "*THROWN*".equals(lbl);
    }

    // Extracts the set of variable names <em>defined</em> (written) by the statement whose label is {@code label}.
    static Set<String> extractDefs(String label) {
        String text = stripPrefix(label);
        Set<String> defs = new LinkedHashSet<>();

        // Type declarations (highest priority catches 'int x = 0' before the assignment pattern can incorrectly attribute the '= 0' to 'int')
        Matcher m = TYPE_DECL_PAT.matcher(text);
        while (m.find()) {
            String name = m.group(1);
            if (!KEYWORDS.contains(name)) defs.add(name);
        }

        // Assignment LHS
        m = ASSIGN_LHS_PAT.matcher(text);
        while (m.find()) {
            String name = m.group(1);
            if (!KEYWORDS.contains(name)) defs.add(name);
        }

        // Post-increment / post-decrement
        m = POST_INC_DEC_PAT.matcher(text);
        while (m.find()) {
            String name = m.group(1);
            if (!KEYWORDS.contains(name)) defs.add(name);
        }

        // Pre-increment / pre-decrement
        m = PRE_INC_DEC_PAT.matcher(text);
        while (m.find()) {
            String name = m.group(1);
            if (!KEYWORDS.contains(name)) defs.add(name);
        }

        return defs;
    }

    // Extracts the set of variable names <em>used</em> (read) by the statement whose label is {@code label}.
    static Set<String> extractUses(String label) {
        String text = stripPrefix(label);
        Set<String> uses = new LinkedHashSet<>();

        Matcher m = IDENT_PAT.matcher(text);
        while (m.find()) {
            String name = m.group(1);
            if (!KEYWORDS.contains(name)) uses.add(name);
        }

        return uses;
    }
}