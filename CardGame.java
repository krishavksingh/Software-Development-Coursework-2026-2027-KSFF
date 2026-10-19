import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class CardGame {
    public static Integer numplayers;
    public static CardDeck[] decks;
    public static CountDownLatch latch;
    public static CountDownLatch startLatch;
    
    public static void main(String[] args) throws IOException {
        List<String> cards;
        LinkedList<Card> pack;
        List<Player> players;
        
        
        
        System.out.print("Krishav Singh, Fin Feakes Software Development Coursework 2026/27\n");

        Scanner input = new Scanner(System.in);

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
        startLatch = new CountDownLatch(numplayers);

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
        
        pack = new LinkedList<Card>();
        for (String card: cards){
            pack.add(new Card(Integer.valueOf(card)));
        }

        players = new ArrayList<Player>();
        for (int i = 1; i <= numplayers; i++) {
            Player player = new Player(i, players);
            players.add(player);
            
        }
        
        for (int i = 0; i < 4; i++) {
            for (Player player : players) {
                Card card = pack.remove();
                player.addCard(card);
                
            }
        }

        
        
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

        
        for (Player player : players) {
            Thread t = new Thread(player);
            t.start();
            
                
        }

        try{
            latch.await();
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
