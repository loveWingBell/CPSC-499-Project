// CPSC 499 Project Part 3 - Elda Britu - 30158734 - April 13, 2026

package org.lsmr.cia.eval;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.lsmr.cfg.ControlFlowGraph;
import org.lsmr.cia.CIAResult;
import org.lsmr.cia.CIATool;
import org.lsmr.cia.ChangeImpactAnalyzer;
import org.lsmr.pdg.PDGBuilder;
import org.lsmr.pdg.ProgramDependenceGraph;

// Evaluation runner for the CIA tool.

public class CIAEvaluator {

    // Path to the directory containing subject {@code .java} files. 
    private final String subjectsDir;

    /**
     * @param subjectsDir path to the directory containing subject {@code .java} files;
     *                    must not be null
     */
    public CIAEvaluator(String subjectsDir) {
        if (subjectsDir == null)
            throw new IllegalArgumentException("subjectsDir must not be null");
        this.subjectsDir = subjectsDir;
    }

    // Metrics ------------------------------------------------------------------------
    
    // Precision: {@code |reported ∩ expected| / |reported|}. Returns 1.0 when reported is empty (vacuously no false positives)
    static double precision(Set<Integer> reported, Set<Integer> expected) {
        if (reported.isEmpty()) return 1.0;
        long truePositives = reported.stream().filter(expected::contains).count();
        return (double) truePositives / reported.size();
    }

    // Recall: {@code |reported ∩ expected| / |expected|}. Returns 1.0 when expected is empty (nothing to miss)
    static double recall(Set<Integer> reported, Set<Integer> expected) {
        if (expected.isEmpty()) return 1.0;
        long truePositives = reported.stream().filter(expected::contains).count();
        return (double) truePositives / expected.size();
    }

    // F1: harmonic mean of precision and recall. Returns 1.0 when both are 1.0, 0.0 when both are 0.0
    static double f1(double precision, double recall) {
        if (precision + recall == 0.0) return 0.0;
        return 2.0 * precision * recall / (precision + recall);
    }

    // Evaluation runner -----------------------------------------------------------------------------

    /**
     * Runs evaluation over all oracle entries and prints a formatted report to stdout.
     * For each entry the evaluator:
     * - Locates {@code <subjectsDir>/<subjectName>.java} and parses it.</li>
     * - Builds a PDG for every method in the file via {@link PDGBuilder}.</li>
     *  - Runs {@link ChangeImpactAnalyzer} on the PDG that contains the change point.</li>
     * - Computes precision, recall, and F1 against the oracle's expected set.</li>
     *  - Prints a per-entry row to the table.</li>
     * 
     * After all entries a summary row prints mean precision, recall, and F1.
     */
    public void evaluate(List<EvaluationOracle> oracles) {
        if (oracles == null) throw new IllegalArgumentException("oracles must not be null");

        // Table header
        String headerFmt = "%-12s  %6s  %-22s  %-22s  %9s  %7s  %6s%n";
        String rowFmt    = "%-12s  %6d  %-22s  %-22s  %9.3f  %7.3f  %6.3f%n";
        String separator = "-".repeat(100);

        System.out.printf("%n=== CIA Evaluation Report ===%n%n");
        System.out.printf(headerFmt,
                "Subject", "Change", "Reported", "Expected",
                "Precision", "Recall", "F1");
        System.out.println(separator);

        // Per-entry rows
        double totalPrecision = 0.0;
        double totalRecall    = 0.0;
        double totalF1        = 0.0;
        int    rowCount       = 0;

        for (EvaluationOracle oracle : oracles) {
            String subjectPath = subjectsDir + "/" + oracle.getSubjectName() + ".java";

            // Parse and build PDG(s)
            List<ControlFlowGraph> cfgs;
            try {
                cfgs = CIATool.parseSource(subjectPath);
            } catch (IOException e) {
                System.err.printf("  [SKIP] Cannot read %s: %s%n", subjectPath, e.getMessage());
                continue;
            }

            // Find the PDG that contains the change-point node
            CIAResult result = null;
            for (ControlFlowGraph cfg : cfgs) {
                ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
                ChangeImpactAnalyzer analyzer = new ChangeImpactAnalyzer(pdg);
                if (analyzer.findNodeByLine(oracle.getChangePoint()) != null) {
                    result = analyzer.analyze(oracle.getChangePoint());
                    break;
                }
            }

            if (result == null) {
                System.err.printf("  [SKIP] Node %d not found in %s%n",
                        oracle.getChangePoint(), oracle.getSubjectName());
                continue;
            }

            Set<Integer> reported = new HashSet<>(result.getImpactedLines());
            Set<Integer> expected = oracle.getExpectedImpactedLines();

            double p  = precision(reported, expected);
            double r  = recall(reported, expected);
            double f  = f1(p, r);

            totalPrecision += p;
            totalRecall    += r;
            totalF1        += f;
            rowCount++;

            System.out.printf(rowFmt,
                    oracle.getSubjectName(),
                    oracle.getChangePoint(),
                    formatSet(reported),
                    formatSet(expected),
                    p, r, f);
        }

        // Aggregate summary
        System.out.println(separator);
        if (rowCount > 0) {
            System.out.printf("%n=== Aggregate (n=%d) ===%n", rowCount);
            System.out.printf("  Mean Precision : %.3f%n", totalPrecision / rowCount);
            System.out.printf("  Mean Recall    : %.3f%n", totalRecall    / rowCount);
            System.out.printf("  Mean F1        : %.3f%n", totalF1        / rowCount);
            System.out.printf("%n");
            interpretResults(totalPrecision / rowCount, totalRecall / rowCount);
        } else {
            System.out.println("No entries were evaluated.");
        }
    }

    // Oracle loading ----------------------------------------------------------------------

    // Loads oracle entries from {@code oracle.json} on the test classpath ({@code src/test/resources/eval/oracle.json})
    // Uses a hand-rolled parser to avoid introducing a JSON library dependency
    public static List<EvaluationOracle> loadOracle() throws IOException {
        InputStream is = CIAEvaluator.class.getResourceAsStream("/eval/oracle.json");
        if (is == null)
            throw new IOException("oracle.json not found on classpath at /eval/oracle.json");

        List<EvaluationOracle> entries = new ArrayList<>();
        String subject = null;
        int changePoint = -1;
        Set<Integer> expected = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("\"subject\"")) {
                    subject = extractStringValue(line);

                } else if (line.startsWith("\"changePoint\"")) {
                    changePoint = extractIntValue(line);

                } else if (line.startsWith("\"expectedImpactedNodes\"")) {
                    expected = extractIntArray(line);

                } else if (line.startsWith("}")) {
                    // End of one entry — commit if we have all three fields
                    if (subject != null && changePoint >= 0 && expected != null) {
                        entries.add(new EvaluationOracle(subject, changePoint, expected));
                    }
                    subject = null;
                    changePoint = -1;
                    expected = null;
                }
            }
        }
        return entries;
    }

    // Entry point ----------------------------------------------------------------

    /**
     * Runs the full evaluation from the command line.
     *
     * <pre>
     *   java -cp &lt;classpath&gt; org.lsmr.cia.eval.CIAEvaluator [subjects-dir]
     * </pre>
     */
    public static void main(String[] args) {
        String subjectsDir = (args.length > 0) ? args[0] : "Part 3/subjects";

        List<EvaluationOracle> oracles;
        try {
            oracles = loadOracle();
        } catch (IOException e) {
            System.err.println("Error loading oracle: " + e.getMessage());
            System.exit(1);
            return;
        }

        new CIAEvaluator(subjectsDir).evaluate(oracles);
    }

    // Interpretation helper -------------------------------------------------------

    // Prints a plain-English interpretation of the aggregate precision and recall, explaining what the numbers mean in terms of PDG model quality
    private static void interpretResults(double meanPrecision, double meanRecall) {
        System.out.println("=== Interpretation ===");

        if (meanPrecision < 1.0) {
            System.out.printf(
                "  Precision < 1.0 (%.3f): the PDG over-approximates impact — some reported%n" +
                "  nodes are false positives that would not actually be affected by the change.%n" +
                "  Cause: PDGs track all possible data/control dependences conservatively;%n" +
                "  they cannot reason about values (e.g. a branch that is always taken).%n",
                meanPrecision);
        } else {
            System.out.println("  Precision = 1.000: no false positives — every reported node is truly impacted.");
        }

        if (meanRecall < 1.0) {
            System.out.printf(
                "  Recall < 1.0 (%.3f): the PDG under-approximates impact — some truly%n" +
                "  impacted nodes were not reported. Check the oracle and node numbering.%n",
                meanRecall);
        } else {
            System.out.println("  Recall    = 1.000: no false negatives — all truly impacted nodes were reported.");
        }
        System.out.println();
    }

    // Private parsing helpers ----------------------------------------------------------------

    // Extracts the string value from a JSON line of the form {@code "key": "value",}
    private static String extractStringValue(String line) {
        int first = line.indexOf('"', line.indexOf(':'));
        int last  = line.lastIndexOf('"');
        if (first < 0 || first == last) return null;
        return line.substring(first + 1, last);
    }

    // Extracts the integer value from a JSON line of the form {@code "key": 42,}.
    private static int extractIntValue(String line) {
        String after = line.substring(line.indexOf(':') + 1).trim().replaceAll("[^0-9]", "");
        return after.isEmpty() ? -1 : Integer.parseInt(after);
    }

    // Extracts a JSON integer array from a line of the form {@code "key": [1, 2, 3],} or {@code "key": [],}
    private static Set<Integer> extractIntArray(String line) {
        int open  = line.indexOf('[');
        int close = line.indexOf(']');
        if (open < 0 || close < 0) return new HashSet<>();
        String inner = line.substring(open + 1, close).trim();
        Set<Integer> result = new HashSet<>();
        if (inner.isEmpty()) return result;
        for (String token : inner.split(",")) {
            token = token.trim();
            if (!token.isEmpty()) result.add(Integer.parseInt(token));
        }
        return result;
    }

    // Formats an integer set as a sorted, bracketed string for the report table
    private static String formatSet(Set<Integer> set) {
        if (set.isEmpty()) return "[]";
        String contents = set.stream()
                .sorted()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        return "[" + contents + "]";
    }
}