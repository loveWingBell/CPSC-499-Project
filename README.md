# CPSC 499: Final Project
# Elda Britu - 30158734


## Requirements

- **JDK 11+** (Java Development Kit) — `javac` and `java` must be on your PATH
- **Maven 3.6+** — `mvn` must be on your PATH


## Directory Layout

```
Project/
├── pom.xml                         Root Maven POM (multi-module build)
│
├── CFG-W26/                        CFG module (prerequisite)
│   └── CPSC 499.02/
│       └── src/
│           ├── ca/ucalgary/cpsc499_02/w26/
│           │   ├── Java1_2ANTLRLexer.g4
│           │   └── Java1_2ANTLRParser.g4
│           └── org/lsmr/cfg/
│               ├── ControlFlowGraph.java
│               ├── Edge.java
│               ├── Node.java
│               ├── StatementNodeBuilder.java
│               ├── TreePrinter.java
│               ├── WorkingGraph.java
│               └── pom.xml
│
├── Part 2/                         PDG module
│   ├── pom.xml
│   └── src/
│       ├── main/java/org/lsmr/pdg/
│       │   ├── ControlFlowGraph.java     (re-exported from CFG module)
│       │   ├── DominatorTree.java        Post-dominator tree builder
│       │   ├── PDGBuilder.java           Main entry point — builds a PDG from a CFG
│       │   ├── PDGEdge.java              Control- and data-dependence edge model
│       │   ├── PDGNode.java              PDG node wrapping a CFG node
│       │   └── ProgramDependenceGraph.java  PDG container with query API
│       └── test/java/org/lsmr/pdg/
│           └── PDGBuilderTest.java       JUnit 5 test suite (79 tests)
│
├── Part 3/                         CIA module
│   ├── pom.xml
│   ├── subjects/                   Plain-text Java programs used as evaluation subjects
│   │   ├── Subject1.java           Sequential data chain
│   │   ├── Subject2.java           If-statement with control dependence
│   │   ├── Subject3.java           While loop with loop-carried dependence
│   │   └── Subject4.java           If-statement with two independent variables
│   └── src/
│       ├── main/java/org/lsmr/cia/
│       │   ├── CIAResult.java            Output of one CIA query (changePoint + impactedLines)
│       │   ├── ChangeImpactAnalyzer.java  Core CIA engine — forward BFS over PDG edges
│       │   └── CIATool.java              CLI entry point
│       └── test/
│           ├── java/org/lsmr/cia/
│           │   ├── ChangeImpactAnalyzerTest.java  JUnit 5 test suite
│           │   └── eval/
│           │       ├── CIAEvaluator.java   Precision/recall evaluation runner
│           │       └── EvaluationOracle.java  Ground-truth oracle entry model
│           └── resources/eval/
│               └── oracle.json             Manual ground-truth for evaluation subjects
```

---

## Maven Modules

| Module | Artifact ID | Description |
|---|---|---|
| `CFG-W26/CPSC 499.02/src/org/lsmr/cfg` | `cfg` | Control Flow Graph library (Part 1 - Provided) |
| `Part 2` | `pdg` | Program Dependence Graph builder (Part 2) |
| `Part 3` | `cia` | Change Impact Analysis tool and evaluator (Part 3) |

---
# Part 2: PDF Builder
## Build & Test

Run all commands from inside the root `Project/` directory.

**Build and test everything at once:**
```bash
mvn install
```

**Build without running tests:**
```bash
mvn install -DskipTests
```

**Run only the PDG tests:**
```bash
mvn test -pl "Part 2"
```

**Clean all compiled output:**
```bash
mvn clean
```

---

## Running in Eclipse

**Requirements:** Eclipse IDE for Java Developers with the **m2e** (Maven Integration) plugin installed. m2e is bundled by default in modern Eclipse releases.

**Import the project:**
1. Open Eclipse and go to **File → Import…**
2. Select **Maven → Existing Maven Projects** and click **Next**
3. Set the **Root Directory** to the `Project/` folder (the one containing the root `pom.xml`)
4. Eclipse will detect both modules (`cfg` and `pdg`) — ensure both are checked, then click **Finish**
5. Wait for Eclipse to resolve dependencies (progress shown in the bottom status bar)

**Run all tests:**
1. In the **Package Explorer**, right-click the `pdg` project (`Part 2/`)
2. Select **Run As → JUnit Test**
3. The JUnit view will open and show all 79 tests passing

**Run a single test class:**
1. Expand `Part 2/src/test/java/org/lsmr/pdg/`
2. Right-click `PDGBuilderTest.java`
3. Select **Run As → JUnit Test**

**Run Maven goals from Eclipse:**
1. Right-click the root `pdg-builder` project (or any sub-module)
2. Select **Run As → Maven build…**
3. Enter a goal such as `install`, `test`, or `clean` and click **Run**

> **Note:** If Eclipse shows red errors after import, try right-clicking the project and selecting **Maven → Update Project…** (`Alt+F5`) to force dependency resolution.

---

## Usage

`PDGBuilder` accepts a `ControlFlowGraph` and returns a `ProgramDependenceGraph`:

```java
ControlFlowGraph cfg = /* ... build or obtain a CFG ... */;

PDGBuilder builder = new PDGBuilder(cfg);
ProgramDependenceGraph pdg = builder.build();

// Query control-dependence edges
pdg.getEdges().stream()
   .filter(e -> e.getType() == PDGEdge.PDGEdgeType.CONTROL)
   .forEach(System.out::println);

// Query data-dependence edges for a specific variable
pdg.getDataEdgesFor("x").forEach(System.out::println);
```

The build process runs in three steps:

1. **Node creation** — one `PDGNode` per real CFG node (entry is included; virtual exit/throw nodes are excluded).
2. **Control dependencies** — computed via the post-dominator tree of the CFG.
3. **Data dependencies** — computed via a reaching-definitions (gen/kill) analysis; a `DATA` edge is added from every definition of a variable to each of its subsequent uses.

---

## Test Output

The test suite (`PDGBuilderTest`) is organised into five sections and covers 79 test cases with 0 failures:

```
Tests run: 79, Failures: 0, Errors: 0, Skipped: 0
```

| Section | Coverage |
|---|---|
| 1. Static helper methods | `stripPrefix`, `isVirtualExitNode`, `extractDefs/Uses`, `collectIdents` |
| 2. Constructor / validation | `PDGNode`, `PDGEdge`, `ProgramDependenceGraph`, `PDGBuilder` null-argument checks |
| 3. DominatorTree API | Post-dominator tree construction and querying |
| 4. PDG query API | `getNodes`, `getEdges`, `getControlEdges`, `getDataEdges`, `getDataEdgesFor` |
| 5. Integration tests | Small hand-crafted CFGs run through `PDGBuilder`; expected control- and data-dependence edges asserted |

Surefire reports are written to `Part 2/target/surefire-reports/` after `mvn test`.

# Part 3: Change Impact Analyzer
## Build & Test

Run all commands from inside the root `Project/` directory.

**Build and test everything at once:**
```bash
mvn install
```

**Build without running tests:**
```bash
mvn install -DskipTests
```

**Run only the CIA tests:**
```bash
mvn test -pl "Part 3"
```

**Run the evaluation against the four subject programs:**
```bash
mvn exec:java -pl "Part 3"
```

**Clean all compiled output:**
```bash
mvn clean
```

---

## Running in Eclipse

**Run the CIA test suite:**
1. In the **Package Explorer**, right-click the `cia` project (`Part 3/`)
2. Select **Run As → JUnit Test**
3. The JUnit view will open and show all tests passing

**Run a single test class:**
1. Expand `Part 3/src/test/java/org/lsmr/cia/`
2. Right-click `ChangeImpactAnalyzerTest.java`
3. Select **Run As → JUnit Test**

**Run the evaluator from Eclipse:**
1. Right-click `Part 3/src/test/java/org/lsmr/cia/eval/CIAEvaluator.java`
2. Select **Run As → Java Application**
3. The evaluation report will appear in the Console view

> **Note:** The evaluator reads `oracle.json` from the test classpath and subject `.java` files from `Part 3/subjects/` relative to the working directory. If the subjects are not found, set the working directory to the `Project/` root in the run configuration (**Run Configurations → Arguments → Working directory**).

---

## Usage

### CIATool (CLI)

`CIATool` takes a Java source file and a PDG node number and prints the impact set as JSON to stdout.

**List all node numbers in a subject file:**
```bash
mvn exec:java -pl "Part 3" \
  -Dexec.mainClass=org.lsmr.cia.CIATool \
  -Dexec.classpathScope=runtime \
  -Dexec.args="Part 3/subjects/Subject1.java --list"
```

**Run CIA from a specific node number:**
```bash
mvn exec:java -pl "Part 3" \
  -Dexec.mainClass=org.lsmr.cia.CIATool \
  -Dexec.classpathScope=runtime \
  -Dexec.args="Part 3/subjects/Subject1.java 1"
```

Output format (one JSON object per method, printed to stdout):
```json
{"method":"Subject1.run(int)","changePoint":1,"impactedLines":[2,3,4]}
```

> **Important:** The second argument is the PDG **node number**, not the source line number. Node numbers are assigned sequentially by `StatementNodeBuilder` starting from 0, beginning with a `block` node for the method body. Use `--list` to see all node numbers before querying.

### ChangeImpactAnalyzer (API)

`ChangeImpactAnalyzer` can be used programmatically once a PDG has been built:

```java
ControlFlowGraph cfg = /* ... parse source via StatementNodeBuilder ... */;

ProgramDependenceGraph pdg = new PDGBuilder(cfg).build();
ChangeImpactAnalyzer analyzer = new ChangeImpactAnalyzer(pdg);

CIAResult result = analyzer.analyze(1);   // change at node 1
System.out.println(result.toJson());
// → {"changePoint":1,"impactedLines":[2,3,4]}

System.out.println(result);
// → CIA result: change at line 1 impacts [2, 3, 4]
```

### CIAEvaluator

`CIAEvaluator` loads `oracle.json` from the classpath, runs CIA on each subject, and prints a precision/recall/F1 report. Run it via Maven from the project root:

```bash
mvn exec:java -pl "Part 3"
```

To pass a custom subjects directory:
```bash
mvn exec:java -pl "Part 3" -Dexec.args="path/to/subjects"
```

---

## Test Output

The test suite (`ChangeImpactAnalyzerTest`) is organised into six sections:

| Section | Coverage |
|---|---|
| 1. `extractLineNumber` | Numeric label parsing; virtual node labels return -1 |
| 2. `findNodeByLine` | Node lookup by number; prefix disambiguation (e.g. 1 vs 10) |
| 3. Constructor validation | Null PDG rejected with `IllegalArgumentException` |
| 4. `analyze()` edge cases | Invalid line numbers, unknown change points, empty methods, leaf nodes, self-exclusion |
| 5. Integration tests | Sequential chains, if/else, while loops, independent variables — hand-crafted CFGs run through `PDGBuilder` then `ChangeImpactAnalyzer` |
| 6. Output format | Sorted result list, `toJson()` format, change point stored correctly |

Surefire reports are written to `Part 3/target/surefire-reports/` after `mvn test -pl "Part 3"`.