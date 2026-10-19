import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

public class CardGameTest {
    private int nextDeckId = 2000;
    private final List<CardDeck> decks = new ArrayList<>();
    // Helper methods
    private CardDeck newDeck(int... denominations) throws IOException {
        CardDeck deck = new CardDeck(nextDeckId);
        nextDeckId += 1;
        decks.add(deck);
        deck.getLock().lock();
        try {
            for (int denomination : denominations) {
                deck.addCard(new Card(denomination));
            }
        } finally {
            deck.getLock().unlock();
        }
        return deck;
    }


    private List<Integer> drain(CardDeck deck) {
        List<Integer> result = new ArrayList<>();
        deck.getLock().lock();
        try {
            while (!deck.isEmpty()) {
                result.add(deck.removeFromDeck().getDenom());
            }
        } finally {
            deck.getLock().unlock();
        }
        return result;
    }

    private boolean isEmpty(CardDeck deck) {
        deck.getLock().lock();
        try {
            return deck.isEmpty();
        } finally {
            deck.getLock().unlock();
        }
    }

    @Test
    void cardStoresDenom() {
        assertEquals(5, new Card(5).getDenom());
    }

    @Test
    void cardAllowsforZero() {
        assertEquals(0, new Card(0).getDenom());
    }

    @Test
    void newDeckStartsWithEmpty() throws IOException {
        assertTrue(isEmpty(newDeck()));
    }

    @Test
    void deckReturnsCardsasFIFO() throws IOException {
        assertEquals(List.of(1, 2, 3), drain(newDeck(1, 2, 3)));
    }

    @Test
    void tryingDeckOperationsWithoutLockFails() throws IOException {
        CardDeck deck = newDeck();

        assertThrows(IllegalStateException.class, () -> deck.addCard(new Card(5)));
        assertThrows(IllegalStateException.class, () -> deck.removeFromDeck());
        assertThrows(IllegalStateException.class, () -> deck.isEmpty());
        assertThrows(IllegalStateException.class, () -> deck.doFinalLog());
        
    }

    @Test
    void tryingDeckOperationsWithDifferentThreadHoldingLockFails() throws Exception {
        CardDeck deck = newDeck();
        deck.getLock().lock();
        AtomicBoolean numFails = new AtomicBoolean(false);
        Thread t = new Thread(() -> {
            try{
                assertThrows(IllegalStateException.class, () -> deck.addCard(new Card(5)));
                assertThrows(IllegalStateException.class, () -> deck.removeFromDeck());       
                assertThrows(IllegalStateException.class, () -> deck.isEmpty());
                assertThrows(IllegalStateException.class, () -> deck.doFinalLog());
                
            }
            catch (AssertionError e){
                numFails.set(true);

            }
        });
        t.start();
        t.join(500);
        deck.getLock().unlock();
        assertEquals(false, numFails.get());
        

        
        
    }
    
}
