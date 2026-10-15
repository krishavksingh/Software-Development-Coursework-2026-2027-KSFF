import java.io.IOException;
import java.nio.file.Files;
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
    
    public static void main(String[] args) throws IOException {
        List<String> cards;
        int count;
        LinkedList<Card> pack;
        List<Player> players;
        
        
        
        System.out.print("Krishav Singh, Fin Feakes Software Development Coursework 2026/27\nPlease enter the number of players:");
        Scanner input = new Scanner(System.in);
        numplayers = input.nextInt();

        System.out.print("Please enter the location of the pack to load:");
        String filename = input.next();

        input.close();
        latch = new CountDownLatch(numplayers);
        cards = Files.readAllLines(Paths.get(filename));
        count = cards.size();
        if(count != 8*numplayers){
            throw new IllegalArgumentException("Pack file is invalid. Please enter a valid pack file.");
        }

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
                
                Card card = pack.remove();
                deck.addCard(card);
                
            }
        }

        
        for (Player player : players) {
            Thread t = new Thread(player);
            t.start();
            
                
        }
        try{
            latch.await();
        }
        catch(InterruptedException e){
            Thread.currentThread().interrupt();
        } 

        for (CardDeck deck: CardGame.decks) {
            deck.getLock().lock();
            try {
                deck.doFinalLog();
            }
            finally{
                deck.getLock().unlock();
            }
        }
    }
}
