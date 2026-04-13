// CPSC 499 Project Part 3 - Elda Britu - 30158734 - April 13, 2026

package org.lsmr.cia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.lsmr.cfg.ControlFlowGraph;
import org.lsmr.cfg.StatementNodeBuilder;
import org.lsmr.pdg.PDGBuilder;
import org.lsmr.pdg.ProgramDependenceGraph;

import ca.ucalgary.cpsc499_02.w26.Java1_2ANTLRLexer;
import ca.ucalgary.cpsc499_02.w26.Java1_2ANTLRParser;

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

    // Exit codes
    static final int EXIT_OK    = 0;
    static final int EXIT_ERROR = 1;

    public static void main(String[] args) {
        if (args.length != 2) {
            printUsage();
            System.exit(EXIT_ERROR);
        }

        String sourcePath = args[0];
        String secondArg  = args[1];

        // Parse source file ------------------------------------------------
        List<ControlFlowGraph> cfgs;
        try {
            cfgs = parseSource(sourcePath);
        } catch (IOException e) {
            System.err.println("Error: cannot read file: " + sourcePath);
            System.err.println("       " + e.getMessage());
            System.exit(EXIT_ERROR);
            return; // unreachable — keeps the compiler happy about cfgs initialisation
        }

        if (cfgs.isEmpty()) {
            System.err.println("Warning: no methods found in " + sourcePath);
            System.exit(EXIT_OK);
            return;
        }

        // --list mode: print all node labels and exit ----------------------
        if ("--list".equals(secondArg)) {
            listNodes(cfgs);
            System.exit(EXIT_OK);
            return;
        }

        // Parse change-point node number -----------------------------------
        int nodeNumber;
        try {
            nodeNumber = Integer.parseInt(secondArg);
        } catch (NumberFormatException e) {
            System.err.println("Error: second argument must be a node number (integer) or --list");
            System.err.println("       Got: \"" + secondArg + "\"");
            printUsage();
            System.exit(EXIT_ERROR);
            return;
        }

        if (nodeNumber < 0) {
            System.err.println("Error: node number must be >= 0, got: " + nodeNumber);
            System.exit(EXIT_ERROR);
            return;
        }

        // Build PDGs and search for the change-point node 
        boolean found = false;

        for (ControlFlowGraph cfg : cfgs) {
            ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
            ChangeImpactAnalyzer analyzer = new ChangeImpactAnalyzer(pdg);

            // Node labels use "N: statement" where N starts at 0.
            // findNodeByLine searches for a label beginning with "<nodeNumber>:".
            // We skip this PDG if it does not contain the requested node.
            if (analyzer.findNodeByLine(nodeNumber) == null)
                continue;

            found = true;
            CIAResult result = analyzer.analyze(nodeNumber);

            // Print one JSON line per matching method to stdout.
            System.out.println(toMethodJson(cfg.name(), result));
        }

        if (!found) {
            System.err.println("Error: no node with number " + nodeNumber
                    + " found in any method of " + sourcePath);
            System.err.println("       Run with --list to see available node numbers.");
            System.exit(EXIT_ERROR);
        }
    }

    // Parsing, parses the Java source at {@code path} and returns one {@link ControlFlowGraph} ------------------------
    public static List<ControlFlowGraph> parseSource(String path) throws IOException {
        CharStream chars = CharStreams.fromFileName(path);

        Java1_2ANTLRLexer lexer   = new Java1_2ANTLRLexer(chars);
        CommonTokenStream tokens  = new CommonTokenStream(lexer);
        Java1_2ANTLRParser parser = new Java1_2ANTLRParser(tokens);

        // Suppress ANTLR's default console error listener to keep stderr clean
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        parser.addErrorListener(new AntlrStderrErrorListener(path));

        StatementNodeBuilder builder = new StatementNodeBuilder();
        // Call builder.visit() rather than root.accept(builder) directly
        builder.visit(parser.compilationUnit());

        return new ArrayList<>(builder.getCFGs());
    }

    // --list mode ------------------------------------------------------------------------
    /**
     * Prints every node label for every CFG to stdout, one per line, in the format:
     *   [ClassName.methodName]  0: *ENTRY*
     *   [ClassName.methodName]  1: int x = 0;
     */
    static void listNodes(List<ControlFlowGraph> cfgs) {
        for (ControlFlowGraph cfg : cfgs) {
            System.out.println("[" + cfg.name() + "]");
            cfg.nodes().stream()
                .map(n -> "  " + n.label())
                .sorted()
                .forEach(System.out::println);
            System.out.println();
        }
    }

    // Output formatting, returns a JSON object that wraps a {@link CIAResult} with the method name --------
    static String toMethodJson(String methodName, CIAResult result) {
        // Embed the method name, then inline the fields from CIAResult.toJson().
        // We do this by hand to avoid a JSON library dependency.
        String inner = result.toJson(); // {"changePoint":N,"impactedLines":[...]}
        // Strip the leading '{' and prepend the method field.
        return "{\"method\":\"" + methodName + "\"," + inner.substring(1);
    }

    // Usage ------------------------------------------------------------------------
    private static void printUsage() {
        System.err.println("Usage: CIATool <source-file> <node-number>");
        System.err.println("       CIATool <source-file> --list");
        System.err.println();
        System.err.println("  source-file   Path to a Java source file");
        System.err.println("  node-number   PDG node label number N (from \"N: statement\")");
        System.err.println("  --list        Print all node labels and their numbers, then exit");
    }

    // Inner helper: ANTLR error listener that writes to stderr with context -----------------------------------------------------------------

    // Minimal ANTLR error listener that prefixes parse errors with the filename so they are distinguishable from other stderr output.
    private static class AntlrStderrErrorListener
            extends org.antlr.v4.runtime.BaseErrorListener {

        private final String filename;

        AntlrStderrErrorListener(String filename) {
            this.filename = filename;
        }

        @Override
        public void syntaxError(org.antlr.v4.runtime.Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line, int charPositionInLine,
                                String msg,
                                org.antlr.v4.runtime.RecognitionException e) {
            System.err.println("Parse error in " + filename
                    + " at line " + line + ":" + charPositionInLine
                    + " — " + msg);
        }
    }
}