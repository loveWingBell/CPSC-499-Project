// CPSC 499 Project Part 3 - Elda Britu - 30158734 - April 13, 2026

// Evaluation Subject 2: if-statement with control dependence.
// Tests whether CIA correctly propagates through CONTROL edges.

// Expected CIA results:
//   change at 0 -> impacts [1, 2]  (predicate controls both outcomes)
//   change at 1 -> impacts []      (leaf: no outgoing edges)
//   change at 2 -> impacts []      (leaf)

public class Subject2 {
    public int run(int a, int b) {
        if (a > b) {      // node 0
            return a - b; // node 1
        }
        return b - a;     // node 2
    }
}
