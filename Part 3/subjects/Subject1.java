// CPSC 499 Project Part 3 - Elda Britu - 30158734 - April 13, 2026

// Evaluation Subject 1: pure sequential data chain.
// No branching; all dependences are data dependences.

// Expected CIA results:
//   change at 0 -> impacts [1, 2, 3]
//   change at 1 -> impacts [2, 3]
//   change at 2 -> impacts [3]
//   change at 3 -> impacts []

public class Subject1 {
    public int run(int a) {
        int x = a;        // node 0
        int y = x + 1;    // node 1
        int z = y * 2;    // node 2
        return z;         // node 3
    }
}
