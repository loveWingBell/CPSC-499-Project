// Evaluation Subject 1: pure sequential data chain
// Used to test CIA on a straight-line program with no branching.

public class Subject1 {
    public int run(int a) {
        int x = a;          // line 6
        int y = x + 1;      // line 7
        int z = y * 2;      // line 8
        return z;           // line 9
    }
}
