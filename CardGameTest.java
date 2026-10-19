import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

public class CardGameTest {
    private int nextDeckId = 2000;
    private final List<CardDeck> decks = new ArrayList<>();
    private final List<Player> players = new ArrayList<>();
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
    private Player newPlayer(int number, int... denominations) throws IOException {
        Player player = new Player(number, new ArrayList<>());
        players.add(player);
        for (int denomination : denominations) {
            player.addCard(new Card(denomination));
        }
        return player;
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

    @Test
    void playerWinsWithFourPreferredDenomCards() throws IOException {
        assertTrue(newPlayer(1, 1, 1, 1, 1).hasWon());
    }

    
    @Test
    void playerCanWinWithNonPrefferredDenomCards() throws IOException {
        assertTrue(newPlayer(1, 9, 9, 9, 9).hasWon());
    }
    @Test
    void playerDoesNotWinWithDiffering() throws IOException {
        assertFalse(newPlayer(2, 5, 5, 3, 6).hasWon());
    }
    @Test
    void winningPlayerDoesNotDrawAnotherCard() throws IOException {
        CardGame.numplayers = 2;
        CardDeck left = newDeck(2);
        CardDeck right = newDeck();
        Player player = newPlayer(1, 1, 1, 1, 1);

        assertEquals(1, player.play(left, right, 2));
        assertEquals(List.of(2), drain(left));
        assertTrue(isEmpty(right));
        assertFalse(left.getLock().isLocked());
        assertFalse(right.getLock().isLocked());
    }

    @Test
    void playDiscardsNonPreferredCardLogic() throws IOException {
        CardGame.numplayers = 2;
        CardDeck left = newDeck(1);
        CardDeck right = newDeck();
        Player player = newPlayer(1, 1, 1, 1, 2);

        assertEquals(1, player.play(left, right, 2));
        assertTrue(player.hasWon());
        assertTrue(isEmpty(left));
        assertEquals(List.of(2), drain(right));
        assertFalse(left.getLock().isLocked());
        assertFalse(right.getLock().isLocked());
    }

    @Test
    void playWithEmptyLeftDeckSkipsTurn() throws IOException {
        CardGame.numplayers = 2;
        CardDeck left = newDeck();
        CardDeck right = newDeck(8, 9);
        Player player = newPlayer(1, 1, 1, 2, 3);

        assertEquals(0, player.play(left, right, 2));
        assertTrue(isEmpty(left));
        assertEquals(List.of(8, 9), drain(right));
        assertFalse(left.getLock().isLocked());
        assertFalse(right.getLock().isLocked());
    }
    
}
