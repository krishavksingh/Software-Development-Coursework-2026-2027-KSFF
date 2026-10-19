import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
/* entry point of game, validates user's inputs, deals cards, starts one thread per player, waits for game to finish and writes final deck logs */
public class CardGame {
    public static Integer numplayers;
    public static CardDeck[] decks;
    public static CountDownLatch latch;
    public static CountDownLatch startLatch;
    
    public static void main(String[] args) throws IOException {
        List<String> cards; /* raw cards taken from pack file*/
        LinkedList<Card> pack;/* pack as Card objects */
        List<Player> players;
        
        
        
        System.out.print("Krishav Singh, Fin Feakes Software Development Coursework 2026/27\n");

        Scanner input = new Scanner(System.in);
        /* ask user for number of players until a valid value is given*/
        numplayers = 0;
        while(numplayers <= 1){
            System.out.print("Please enter the number of players: ");
            try{
                numplayers = Integer.parseInt(input.nextLine().trim());
                if (numplayers <= 1){
                    System.out.println("Please make sure the value you enter is a positive integer greater than one.");
                }
            }
            catch(NumberFormatException e){
                numplayers = 0;
                System.out.println("Please make sure the value you enter is a positive integer greater than one.");
            }
        }
        /* latch started to wait for number of players specified by user to start game*/
        startLatch = new CountDownLatch(numplayers);
        /* ask for pack file until a valid one is given: checks if pack is valid size given number of players, if there are any black lines, if some lines aren't non-negative integers*/
        cards = null;
        boolean validPack = false;
        while(!validPack){
            System.out.print("Please enter the location of the pack to load:");
            String filename = input.nextLine();
            try {
                cards = Files.readAllLines(Paths.get(filename));
            } catch (IOException | InvalidPathException e) {
                System.out.println("Please enter a valid pack name");
                continue;
            }
            if (cards.size() != 8*numplayers){
                System.out.println("Please enter a pack of a valid size");
                continue;
            }
            validPack = true;
            for (String line : cards){
                try {
                    if(Integer.parseInt(line) <0){
                        validPack = false;
                    }
                } catch (NumberFormatException e) {
                    validPack = false;
                }
            }
            if(!validPack){
                System.out.println("Every row must contain single positive integer");
            }
        }
        input.close();
        
        latch = new CountDownLatch(numplayers);
        /* turn lines that passed validation into card objects*/
        pack = new LinkedList<Card>();
        for (String card: cards){
            pack.add(new Card(Integer.valueOf(card)));
        }
        /* create players a*/
        players = new ArrayList<Player>();
        for (int i = 1; i <= numplayers; i++) {
            Player player = new Player(i, players);
            players.add(player);
            
        }
        /* deal the hands to players in a round-robin fashion*/
        for (int i = 0; i < 4; i++) {
            for (Player player : players) {
                Card card = pack.remove();
                player.addCard(card);
                
            }
        }

        
        /* create and deal cards to decks*/
        decks = new CardDeck[numplayers];
        for (int i = 0; i < numplayers; i++){
            decks[i] = new CardDeck(i+1);
        }
        for (int i = 0; i < 4; i++) {
            for (CardDeck deck: decks) {
                deck.getLock().lock();
                try{
                    Card card = pack.remove();
                    deck.addCard(card);
                }
                finally{
                    deck.getLock().unlock();
                }

                
            }
        }

        /* start game, one thread per player*/
        for (Player player : players) {
            Thread t = new Thread(player);
            t.start();
            
                
        }
        /* wait for game to end then write deck logs*/
        try{
            /* waits until every player is finished*/
            latch.await();
            /* each deck is locked while logging so no player can change it at the same time*/
            for (CardDeck deck: CardGame.decks) {
            deck.getLock().lock();
            try {
                deck.doFinalLog();
                deck.close();
            }
            finally{
                deck.getLock().unlock();
            }
        }
        }
        catch(InterruptedException e){
            Thread.currentThread().interrupt();
        } 

        
    }
}
