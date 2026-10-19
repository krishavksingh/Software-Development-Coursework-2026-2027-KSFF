import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

public class CardGameTest {
    private static int nextDeckId = 2000;
    private final List<CardDeck> decks = new ArrayList<>();
    private final List<Player> players = new ArrayList<>();
    // Helper methods
    private CardDeck newDeck(boolean logging, int... denominations) throws IOException {
        CardDeck deck = new CardDeck(nextDeckId, logging);
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
        Player player = new Player(number, new ArrayList<>(), false);
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
        assertTrue(isEmpty(newDeck(false)));
    }

    @Test
    void deckReturnsCardsasFIFO() throws IOException {
        assertEquals(List.of(1, 2, 3), drain(newDeck(false, 1, 2, 3)));
    }

    @Test
    void tryingDeckOperationsWithoutLockFails() throws IOException {
        CardDeck deck = newDeck(false);

        assertThrows(IllegalStateException.class, () -> deck.addCard(new Card(5)));
        assertThrows(IllegalStateException.class, () -> deck.removeFromDeck());
        assertThrows(IllegalStateException.class, () -> deck.isEmpty());
        assertThrows(IllegalStateException.class, () -> deck.doFinalLog());
        
    }

    @Test
    void tryingDeckOperationsWithDifferentThreadHoldingLockFails() throws Exception {
        CardDeck deck = newDeck(false);
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
        CardDeck left = newDeck(false,2);
        CardDeck right = newDeck(false);
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
        CardDeck left = newDeck(false,1);
        CardDeck right = newDeck(false);
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
        CardDeck left = newDeck(false);
        CardDeck right = newDeck(false,8, 9);
        Player player = newPlayer(1, 1, 1, 2, 3);

        assertEquals(0, player.play(left, right, 2));
        assertTrue(isEmpty(left));
        assertEquals(List.of(8, 9), drain(right));
        assertFalse(left.getLock().isLocked());
        assertFalse(right.getLock().isLocked());
    }
    
    @Test
    void customFormatAddsANewLineToTheMessage() {
        CustomFormat format = new CustomFormat();
        LogRecord record = new LogRecord(Level.INFO, "test message");

        assertEquals("test message" + System.lineSeparator(), format.format(record));
    }

    @Test
    void finalDeckLogContainsTheCards() throws IOException {
        CardDeck deck = newDeck(true, 4, 5, 6);

        deck.getLock().lock();
        try {
            deck.doFinalLog();
            deck.close();
        } finally {
            deck.getLock().unlock();
        }

        

        String output = Files.readString(Path.of("deck" + deck.getId() + "_output.txt"));
        assertTrue(output.contains("deck" + deck.getId()+ " contents: 4 5 6 "));
        File file = new File(Path.of("deck" + deck.getId() + "_output.txt").toString());
        file.delete();

    }

    @Test
    void invalidPlayerCountRejected() throws Exception {
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(("abc").getBytes()));
            System.setOut(new PrintStream(out));
            assertThrows(NoSuchElementException.class, () -> CardGame.main(new String[0]));
        } 
        finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }

        String text = out.toString();
        assertTrue(text.contains("positive integer greater than one"));
    }

    @Test
    void invalidPackNameRejected() throws Exception {
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(("4\nNotAFile.txt").getBytes()));
            System.setOut(new PrintStream(out));
            assertThrows(NoSuchElementException.class, () -> CardGame.main(new String[0])); // Happens because Scanner runs out, is normal
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }

        String text = out.toString();
        assertTrue(text.contains("Please enter a valid pack name"));
    }


    @Test
    void invalidPackSizeRejected() throws Exception {
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(("4\ntestpacks/2_pack.txt").getBytes()));
            System.setOut(new PrintStream(out));
            assertThrows(NoSuchElementException.class, () -> CardGame.main(new String[0])); // Happens because Scanner runs out, is normal
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }

        String text = out.toString();
        assertTrue(text.contains("Please enter a pack of a valid size"));
    }
    
    @Test
    void invalidPackFileRejected_Negative() throws Exception {
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(("2\ntestpacks/invalidpack_negative.txt").getBytes()));
            System.setOut(new PrintStream(out));
            assertThrows(NoSuchElementException.class, () -> CardGame.main(new String[0])); // Happens because Scanner runs out, is normal
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }

        String text = out.toString();
        assertTrue(text.contains("contain single positive integer"));
    }

    @Test
    void invalidPackFileRejected_Lines() throws Exception {
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(("2\ntestpacks/invalidpack_lines.txt").getBytes()));
            System.setOut(new PrintStream(out));
            assertThrows(NoSuchElementException.class, () -> CardGame.main(new String[0])); // Happens because Scanner runs out, is normal
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }

        String text = out.toString();
        assertTrue(text.contains("Please enter a pack of a valid size"));
    }

    @Test
    void validNumberAndPackFileAllowed() throws Exception {
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(("2\ntestpacks/2_pack.txt").getBytes()));
            System.setOut(new PrintStream(out));
            CardGame.main(new String[0]);
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }

        String text = out.toString();
        assertTrue(text.contains("wins"));

        for (int i = 1; i <= 2; i++) {
            assertTrue(new File("player" + i +"_output.txt").exists());
            assertTrue(new File("deck"+ i + "_output.txt").exists());
            new File("player" + i +"_output.txt").delete();
            new File("deck"+ i + "_output.txt").delete();
        }
    }

    @Test
    void validNumberAndPackFileAllowed_Four() throws Exception {
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(("4\ntestpacks/4_pack.txt").getBytes()));
            System.setOut(new PrintStream(out));
            CardGame.main(new String[0]);
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }

        String text = out.toString();
        assertTrue(text.contains("wins"));

        for (int i = 1; i <= 4; i++) {
            assertTrue(new File("player" + i +"_output.txt").exists());
            assertTrue(new File("deck"+ i + "_output.txt").exists());
            new File("player" + i +"_output.txt").delete();
            new File("deck"+ i + "_output.txt").delete();
        }
    }

    @Test
    void validNumberAndPackFileAllowed_Five() throws Exception {
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(("5\ntestpacks/5_pack.txt").getBytes()));
            System.setOut(new PrintStream(out));
            CardGame.main(new String[0]);
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }

        String text = out.toString();
        assertTrue(text.contains("wins"));

        for (int i = 1; i <= 5; i++) {
            assertTrue(new File("player" + i +"_output.txt").exists());
            assertTrue(new File("deck"+ i + "_output.txt").exists());
            new File("player" + i +"_output.txt").delete();
            new File("deck"+ i + "_output.txt").delete();
        }
    
    }

    @Test
    void immediateWinCheck() throws Exception {
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(("2\ntestpacks/2_pack.txt").getBytes()));
            System.setOut(new PrintStream(out));
            CardGame.main(new String[0]);
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }

        for (int i = 1; i <= 2; i++) {
            assertTrue(new File("player" + i +"_output.txt").exists());
            assertTrue(new File("deck"+ i + "_output.txt").exists());
            new File("deck"+ i + "_output.txt").delete();
        }
        
        List<String> lines = Files.readAllLines(new File("player1_output.txt").toPath());
        List<String> correctlines = new ArrayList<>();
        correctlines.add("player 1 initial hand 1 1 1 1 ");
        correctlines.add("player 1 wins");
        correctlines.add("player 1 exits");
        correctlines.add("player 1 final hand: 1 1 1 1 ");
        assertEquals(correctlines, lines);

        for (int i = 1; i <= 2; i++) {
            new File("player"+ i + "_output.txt").delete();
        }

    }

    
}
