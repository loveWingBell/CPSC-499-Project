// Evaluation Subject 4: nested if + independent variable
// Used to test precision: a change to one variable should NOT impact
// lines that only use a completely independent variable.

public class Subject4 {
    public int run(int a, int b) {
        int x = a * 2;              // line 6
        int y = b + 1;              // line 7  -- independent of x
        if (x > 10) {               // line 8
            x = x - 1;             // line 9
        }
        int result = x + y;        // line 11
        return result;             // line 12
    }
}
