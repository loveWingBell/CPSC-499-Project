// CPSC 499 Project Part 3 - Elda Britu - 30158734 - April 13, 2026

// Evaluation Subject 3: while loop with loop-carried data dependence.
// Tests whether CIA correctly crosses the loop back-edge.

// Expected CIA results:
//   change at 0 -> impacts [3, 5]      (sum flows to body and return; i chain separate)
//   change at 1 -> impacts [2, 3, 4, 5] (i flows everywhere)
//   change at 4 -> impacts [2, 3, 5]   (i++ feeds condition, body, and return;
//                                        node 4 itself excluded as change point)

public class Subject3 {
    public int run(int n) {
        int sum = 0;         // node 0
        int i = 0;           // node 1
        while (i < n) {      // node 2
            sum = sum + i;   // node 3
            i++;             // node 4
        }
        return sum;          // node 5
    }
}
