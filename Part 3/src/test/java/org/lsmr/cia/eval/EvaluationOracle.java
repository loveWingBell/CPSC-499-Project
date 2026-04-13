// CPSC 499 Project Part 3 - Elda Britu - 30158734 - April 13, 2026

package org.lsmr.cia.eval;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// The oracle that evaluates the CIA tool. The oracle that evaluates th

public class EvaluationOracle {

    private final String subjectName;
    private final int changePoint;
    private final Set<Integer> expectedImpactedLines;

    /**
     * @param subjectName            name of the subject file (no path, no extension); must not be null or blank
     * @param changePoint            PDG node number of the changed statement; must be >= 0
     * @param expectedImpactedLines  manually determined set of impacted node numbers;
     *                               must not be null, may be empty; defensively copied
     * @throws IllegalArgumentException if any argument fails validation
     */
    public EvaluationOracle(String subjectName, int changePoint, Set<Integer> expectedImpactedLines) {
        if (subjectName == null || subjectName.isBlank())
            throw new IllegalArgumentException("subjectName must not be null or blank");
        if (changePoint < 0)
            throw new IllegalArgumentException("changePoint must be >= 0, got: " + changePoint);
        if (expectedImpactedLines == null)
            throw new IllegalArgumentException("expectedImpactedLines must not be null");

        this.subjectName             = subjectName;
        this.changePoint             = changePoint;
        this.expectedImpactedLines   = Collections.unmodifiableSet(new HashSet<>(expectedImpactedLines));
    }

    // Accessors --------------------------------------------------------------------

    //Name of the subject file, without path or {@code .java} extension
    public String getSubjectName() { return subjectName; }

    // PDG node number of the changed statement
    public int getChangePoint() { return changePoint; }

    // Unmodifiable set of node numbers that the CIA tool should report as impacted
    // Empty when the change point is a leaf with no dependents
    public Set<Integer> getExpectedImpactedLines() { return expectedImpactedLines; }


    // Object overrides -------------------------------------------------------------

    @Override
    public String toString() {
        return "EvaluationOracle["
                + subjectName + ", changePoint=" + changePoint
                + ", expected=" + expectedImpactedLines + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EvaluationOracle)) return false;
        EvaluationOracle other = (EvaluationOracle) obj;
        return changePoint == other.changePoint
                && subjectName.equals(other.subjectName)
                && expectedImpactedLines.equals(other.expectedImpactedLines);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + subjectName.hashCode();
        result = 31 * result + changePoint;
        result = 31 * result + expectedImpactedLines.hashCode();
        return result;
    }
}