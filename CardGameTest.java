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
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.AfterEach;
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
    private Player newPlayer(boolean logging, int number, int... denominations) throws IOException {
        Player player = new Player(number, new ArrayList<>(), logging);
        players.add(player);
        for (int denomination : denominations) {
            player.addCard(new Card(denomination));
        }
        return player;
    }
    /* Reset logs to prevent issues between tests */
    @AfterEach 
    void cleanup(){
        LogManager.getLogManager().reset();
    }
    /* test that the card stores the denomination correctly */
    @Test
    void cardStoresDenom() {
        assertEquals(5, new Card(5).getDenom());
    }
    /* test that card allows denomination of zero */
    @Test
    void cardAllowsforZero() {
        assertEquals(0, new Card(0).getDenom());
    }
    /* test that card throws exception when attempting to assign negative value */
    @Test 
    void cardThrowsOnNegative(){
        assertThrows(IllegalStateException.class,() -> new Card(-5));
    }
    /* test that a new deck starts emptz */
    @Test
    void newDeckStartsWithEmpty() throws IOException {
        assertTrue(isEmpty(newDeck(false)));
    }
    /* test for enforcing FIFO behaviour */
    @Test
    void deckReturnsCardsasFIFO() throws IOException {
        assertEquals(List.of(1, 2, 3), drain(newDeck(false, 1, 2, 3)));
    }
    /* test that deck operations without having the lock should fail */
    @Test
    void tryingDeckOperationsWithoutLockFails() throws IOException {
        CardDeck deck = newDeck(false);

        assertThrows(IllegalStateException.class, () -> deck.addCard(new Card(5)));
        assertThrows(IllegalStateException.class, () -> deck.removeFromDeck());
        assertThrows(IllegalStateException.class, () -> deck.isEmpty());
        assertThrows(IllegalStateException.class, () -> deck.doFinalLog());
        
    }
    /* test that it MUST be THE thread trying the operation that should have the lock */
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
    /* test that player wins with 4 preferred cards */
    @Test
    void playerWinsWithFourPreferredDenomCards() throws IOException {
        assertTrue(newPlayer(1, 1, 1, 1, 1).hasWon());
    }

    /* test that player wins with 4 non preferred cards */
    @Test
    void playerCanWinWithNonPrefferredDenomCards() throws IOException {
        assertTrue(newPlayer(1, 9, 9, 9, 9).hasWon());
    }
    /* test that player does not win with differing cards */
    @Test
    void playerDoesNotWinWithDiffering() throws IOException {
        assertFalse(newPlayer(2, 5, 5, 3, 6).hasWon());
    }
    /* test that winning player will not attempt another go and frees the lock */
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
    /* test that player discards the non preferred card if thats the only option */
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
    /* test that if left deck is empty, turn must be skipped */
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
    /* test that the player has four cards before/after the action, confirming it is atomic */
    @Test 
    void playerHasFourCardsBefore_AfterPlayCalled() throws IOException{
        CardGame.numplayers = 2;
        CardDeck left = newDeck(false, 2);
        CardDeck right = newDeck(false);
        Player player = newPlayer(1, 1, 1, 1, 3);
        assertEquals(4, player.getCards().size());
        player.play(left, right, 2);
        assertEquals(4, player.getCards().size());


    }
    /* testing the custom format to remove timestamps */
    @Test
    void customFormatAddsANewLineToTheMessage() {
        CustomFormat format = new CustomFormat();
        LogRecord record = new LogRecord(Level.INFO, "test message");

        assertEquals("test message" + System.lineSeparator(), format.format(record));
    }

    /* test that the final deck log contains the correct cards */
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
    /* test that the final player hand is logged correctly after a go */
    @Test
    void finalPlayerHandLogCheck() throws IOException {
        Player player = newPlayer(true, 10, 10, 10, 10, 3);
        CardDeck leftDeck = newDeck(false, 10);
        CardDeck rightDeck = newDeck(false);
        CardGame.numplayers = 2;
        player.play(leftDeck, rightDeck, 2);

        player.gameEndLog();
        List<String> lines = Files.readAllLines(new File("player10_output.txt").toPath());
        
        assertEquals(List.of("player 10 wins", "player 10 exits", "player 10 final hand: 10 10 10 10 "), (lines.subList(lines.size()-3, lines.size())));
        new File("player10_output.txt").delete();
    }

    
    /* test that if the player count given is invalid, it should trigger validation */
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
    /* test that if the pack name given is invalid, it should trigger validation */
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

    /* test if the size of the pack file is invalid it should trigger validation */
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
    /* if the pack file contains negative elements then it should trigger validation */
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
    /* if the pack file contains lines elements then it should trigger validation */
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
    /* test that the game runs normally when everything is valid for two players */
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
    /* test that the game runs normally when everything is valid for four players */
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
    /* test that the game runs normally when everything is valid for five players */
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
    /* test that when a player is given a winning hand, they do not attempt a go as they have already won */
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
    /* test that the cards before a game are the same as the cards after a game */
    @Test 
    void finalGameStateConservationTest() throws Exception{
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        int n = 2;
        String packPath = "testpacks/2_packNotImmediateWin.txt";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream((n + "\n" + packPath + "\n").getBytes()));
            System.setOut(new PrintStream(out));
            CardGame.main(new String[0]);
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }

        List<Integer> expected = new ArrayList<>();
        for (String line : Files.readAllLines(new File(packPath).toPath())){
            expected.add(Integer.parseInt(line));
        }
        Collections.sort(expected);

        List<Integer> actual = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            List<String> playerLines = Files.readAllLines(new File("player"+i+"_output.txt").toPath());
            String playerHand = playerLines.get(playerLines.size()-1);
            String[] parts = playerHand.split(":");
            String[] cardStrings = parts[1].trim().split(" ");
            List<Integer> hand = new ArrayList<>();
            for (String s : cardStrings){
                hand.add(Integer.parseInt(s));
            }
            actual.addAll(hand);

            List<String> deckLine = Files.readAllLines(new File("deck"+i+"_output.txt").toPath());
            String[] deckParts = deckLine.get(0).split(":");
            if(deckParts.length > 1 && !deckParts[1].trim().isEmpty()){
                for (String s: deckParts[1].trim().split(" ")){
                    actual.add(Integer.parseInt(s));
                }
            }

        }
        Collections.sort(actual);
        assertEquals(expected, actual);
    }
}
