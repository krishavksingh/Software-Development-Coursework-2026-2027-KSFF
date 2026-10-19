import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CardGameTest {
    @Test
    void cardStoresDenom() {
        assertEquals(5, new Card(5).getDenom());
    }

    @Test
    void cardAllowsforZero() {
        assertEquals(0, new Card(0).getDenom());
    }
    
}
