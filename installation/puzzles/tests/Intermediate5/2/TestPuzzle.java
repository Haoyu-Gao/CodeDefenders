import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test(timeout = 4000)
    public void test() {
        Puzzle b = new Puzzle();
        assertEquals(1, b.run(8, 14));
    }

}
