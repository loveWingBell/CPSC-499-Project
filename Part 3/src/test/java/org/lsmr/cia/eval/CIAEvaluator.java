// CPSC 499 - Part 3 - Elda Britu - 30158734

package org.lsmr.cia.eval;

import org.lsmr.cia.CIAResult;

import java.util.List;
import java.util.Set;

/**
 * Evaluation runner for the CIA tool.
 *
 * For each {@link EvaluationOracle} entry:
 *   1. Loads the corresponding subject Java file and builds its PDG.
 *   2. Runs ChangeImpactAnalyzer.
 *   3. Compares the result to the oracle using precision and recall.
 *   4. Prints a per-query row and an aggregate summary.
 *
 * NOTE: Evaluation differs from testing.
 *   - Testing checks that the tool conforms to the PDG model (no bugs).
 *   - Evaluation checks whether the PDG model itself is a good proxy for real CIA.
 *     Even a perfectly correct PDG implementation may have poor precision/recall
 *     because PDGs over-approximate or under-approximate true impact.
 */
public class CIAEvaluator {

    /**
     * Computes precision: fraction of reported lines that are truly impacted.
     * precision = |reported ∩ expected| / |reported|
     */
    static double precision(Set<Integer> reported, Set<Integer> expected) {
        // TODO: implement
        return 0.0;
    }

    /**
     * Computes recall: fraction of truly impacted lines that were reported.
     * recall = |reported ∩ expected| / |expected|
     */
    static double recall(Set<Integer> reported, Set<Integer> expected) {
        // TODO: implement
        return 0.0;
    }

    /**
     * Runs evaluation over a list of oracle entries and prints a report to stdout.
     */
    public void evaluate(List<EvaluationOracle> oracles) {
        // TODO: implement
        //  For each oracle:
        //    - Load subject file from src/test/resources/eval/<subjectName>.java
        //    - Build CFG -> PDG
        //    - Run ChangeImpactAnalyzer
        //    - Compute precision + recall
        //    - Print row
        //  After all entries, print aggregate (mean precision, mean recall)
    }

    public static void main(String[] args) {
        // TODO: define oracle entries and call evaluate()
    }
}
