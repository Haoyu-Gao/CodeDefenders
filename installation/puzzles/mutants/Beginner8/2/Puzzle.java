public class Puzzle {

    public int testMe(int x, int y) {

        int z = -10;

        if(y > x) {
            z = 0;
        }
        if(x > y) {
            z = 1;
        }

        return z;
    }
}
