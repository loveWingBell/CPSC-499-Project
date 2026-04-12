// CPSC 499 - Part 3 - Elda Britu - 30158734

package org.lsmr.cia;

/**
 * CLI entry point for the Change Impact Analysis tool.
 *
 * Usage: java -cp <classpath> org.lsmr.cia.CIATool <source-file> <line-number>
 *
 * The tool:
 *   1. Reads and parses the given Java source file using the ANTLR-based StatementNodeBuilder.
 *   2. Builds a CFG, then hands it to PDGBuilder to produce a ProgramDependenceGraph.
 *   3. Runs ChangeImpactAnalyzer on the specified line number.
 *   4. Prints the CIAResult as JSON to stdout.
 */
public class CIATool {

    public static void main(String[] args) {
        // TODO: implement
        //  - Validate args (need exactly 2: file path + line number)
        //  - Parse the Java source file into a CFG via StatementNodeBuilder
        //  - Build the PDG via PDGBuilder
        //  - Run ChangeImpactAnalyzer.analyze(lineNumber)
        //  - Print result.toJson() to stdout
    }
}
