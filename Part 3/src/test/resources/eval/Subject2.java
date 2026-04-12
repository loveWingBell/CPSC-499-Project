// Evaluation Subject 2: if/else with control + data dependence
// Used to test CIA where a predicate controls two branches.

public class Subject2 {
    public int run(int a, int b) {
        int result;                     // line 6
        if (a > b) {                    // line 7
            result = a - b;             // line 8
        } else {
            result = b - a;             // line 10
        }
        return result;                  // line 12
    }
}
