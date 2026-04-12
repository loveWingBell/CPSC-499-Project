// CPSC 499 - Part 3 - Elda Britu - 30158734

package org.lsmr.cia.eval;

import java.util.Set;

/**
 * Represents one ground-truth CIA oracle entry.
 *
 * Each entry specifies:
 *   - the name of the subject program file (without path/extension)
 *   - the change-point line number
 *   - the manually-determined set of lines that should be reported as impacted
 */
public class EvaluationOracle {

    private final String subjectName;
    private final int changePoint;
    private final Set<Integer> expectedImpactedLines;

    public EvaluationOracle(String subjectName, int changePoint, Set<Integer> expectedImpactedLines) {
        // TODO: validate and store
        this.subjectName = subjectName;
        this.changePoint = changePoint;
        this.expectedImpactedLines = expectedImpactedLines;
    }

    public String getSubjectName() { return subjectName; }
    public int getChangePoint() { return changePoint; }
    public Set<Integer> getExpectedImpactedLines() { return expectedImpactedLines; }
}
