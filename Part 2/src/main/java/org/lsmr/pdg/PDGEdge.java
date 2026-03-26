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

    // TODO: implement
}
