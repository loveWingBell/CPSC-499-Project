// Evaluation Subject 3: while loop with loop-carried data dependence
// Used to test CIA across back-edges (loop variable updated in body feeds back to condition).

public class Subject3 {
    public int run(int n) {
        int sum = 0;            // line 6
        int i = 0;              // line 7
        while (i < n) {         // line 8
            sum = sum + i;      // line 9
            i++;                // line 10
        }
        return sum;             // line 12
    }
}
