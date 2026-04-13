// CPSC 499 Project Part 3 - Elda Britu - 30158734 - April 13, 2026

// Evaluation Subject 4: nested if + independent variable
// Used to test precision: a change to one variable should NOT impact
// lines that only use a completely independent variable.

// Expected CIA results:
//   change at 0 -> impacts [2, 3, 4]  (x flows to condition, body, return)
//   change at 1 -> impacts [4]        (y flows only to return; NOT [2, 3])
//   change at 2 -> impacts [3, 4]     (predicate controls body; body flows to return)
//   change at 3 -> impacts [4]        (redefined x flows to return)\

public class Subject4 {
    public int run(int a, int b) {
        int x = a * 2;     // node 0
        int y = b + 1;     // node 1
        if (x > 10) {      // node 2
            x = x - 1;     // node 3
        }
        return x + y;      // node 4
    }
}
