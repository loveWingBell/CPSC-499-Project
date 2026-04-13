// CPSC 499 Project Part 3 - Elda Britu - 30158734 - April 13, 2026

package org.lsmr.cia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the output of one CIA query. Immutable. Contains:
 *   - the change-point line number (the line that was changed)
 *   - a sorted, unmodifiable list of all impacted line numbers
 */
public class CIAResult {

    private final int changePoint;
    private final List<Integer> impactedLines;

    /**
     * Constructs a CIAResult.
     *
     * @param changePoint   the 1-based source line number of the changed statement; must be >= 1
     * @param impactedLines the set of impacted line numbers; will be defensively copied and sorted;
     *                      must not be null, may be empty
     * @throws IllegalArgumentException if changePoint < 1 or impactedLines is null
     */
    public CIAResult(int changePoint, List<Integer> impactedLines) {
        if (changePoint < 1)
            throw new IllegalArgumentException("changePoint must be >= 1, got: " + changePoint);
        if (impactedLines == null)
            throw new IllegalArgumentException("impactedLines must not be null");

        this.changePoint = changePoint;

        List<Integer> copy = new ArrayList<>(impactedLines);
        Collections.sort(copy);
        this.impactedLines = Collections.unmodifiableList(copy);
    }

    // Accessors ------------------------------------------------------

    // Returns the 1-based source line number of the changed statement
    public int getChangePoint() { return changePoint; }

    // Returns an unmodifiable, sorted list of all line numbers transitively impacted by the change at {@link #getChangePoint()}
    public List<Integer> getImpactedLines() { return impactedLines; }

    // Returns true if no lines are impacted (isolated change)
    public boolean isEmpty() { return impactedLines.isEmpty(); }

    // Output formats ---------------------------------------------------------------------

    // Returns a compact JSON representation suitable for automated evaluation.
    // - Format: {"changePoint":<n>,"impactedLines":[<a>,<b>,...]}
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"changePoint\":");
        sb.append(changePoint);
        sb.append(",\"impactedLines\":[");
        for (int i = 0; i < impactedLines.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(impactedLines.get(i));
        }
        sb.append("]}");
        return sb.toString();
    }

    // Returns a human-readable string representation
    @Override
    public String toString() {
        if (impactedLines.isEmpty())
            return "CIA result: change at line " + changePoint + " impacts no other lines";
        return "CIA result: change at line " + changePoint + " impacts " + impactedLines;
    }

    // Equality / hashing (probably will come in useful for testing, might remove later) ------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CIAResult)) return false;
        CIAResult other = (CIAResult) obj;
        return changePoint == other.changePoint && impactedLines.equals(other.impactedLines);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + changePoint;
        result = 31 * result + impactedLines.hashCode();
        return result;
    }
}