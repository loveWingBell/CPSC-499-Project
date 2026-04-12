// CPSC 499 - Part 3 - Elda Britu - 30158734

package org.lsmr.cia;

import java.util.List;

/**
 * Holds the output of one CIA query.
 * Contains the change-point line number and the sorted list of impacted line numbers.
 */
public class CIAResult {

    private final int changePoint;
    private final List<Integer> impactedLines;

    public CIAResult(int changePoint, List<Integer> impactedLines) {
        // TODO: implement
        this.changePoint = changePoint;
        this.impactedLines = impactedLines;
    }

    public int getChangePoint() { return changePoint; }

    public List<Integer> getImpactedLines() { return impactedLines; }

    /**
     * Returns a JSON representation for easy scripting/evaluation.
     * Example: {"changePoint":3,"impactedLines":[5,7,9]}
     */
    public String toJson() {
        // TODO: implement
        return "";
    }

    @Override
    public String toString() {
        // TODO: implement
        return "";
    }
}
