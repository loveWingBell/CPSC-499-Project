// CPSC 499 Assignment 4 - Elda Britu - 30158734 - March 28, 2025

package org.lsmr.pdg;

import org.lsmr.cfg.ControlFlowGraph;

// The following multi-line comment was taken from Claude bc i got confused about how to build this

/**
 * Builds a {@link ProgramDependenceGraph} from a {@link ControlFlowGraph}.
 *
 * <p>Usage:
 * <pre>
 *   ControlFlowGraph cfg = ...; // built via StatementNodeBuilder
 *   ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
 * </pre>
 *
 * <p>Construction proceeds in two phases:
 * <ol>
 *   <li><b>Control dependence</b> — computed from the post-dominator tree of
 *       the augmented CFG (entry → every node, every node → exit).</li>
 *   <li><b>Data dependence</b> — computed via reaching-definitions analysis
 *       over the CFG.</li>
 * </ol>
 */
public class PDGBuilder {

    private final ControlFlowGraph cfg;

    public PDGBuilder(ControlFlowGraph cfg) {
        this.cfg = cfg;
    }

    /**
     * Builds and returns the PDG for the supplied CFG.
     *
     * @return the completed {@link ProgramDependenceGraph}
     */
    public ProgramDependenceGraph build() {
        // TODO: implement
        throw new UnsupportedOperationException("PDGBuilder.build() not yet implemented");
    }
}
