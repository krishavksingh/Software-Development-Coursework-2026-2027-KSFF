import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

public class CardGame {
    static Integer numplayers;
    static CardDeck[] decks;

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

        cards = Files.readAllLines(Paths.get(filename));
        count = cards.size();
        if(count != 8*numplayers){
            throw new IllegalArgumentException("Pack file is invalid. Please enter a valid pack file.");
        }

        pack = new LinkedList<Card>();
        for (String card: cards){
            pack.add(new Card(Integer.valueOf(card)));
        }

        
        System.out.println(pack.toString());
        decks = new CardDeck[numplayers];
        for (int i = 0; i < numplayers; i++){
            decks[i] = new CardDeck(i+1);
        }
        for (int i = 0; i < numplayers; i++) {
            for (CardDeck deck: decks) {
                System.out.println(i);
                Card card = pack.remove();
                deck.addCard(card);
                
            }
        }
        players = new ArrayList<Player>();
        for (int i = 1; i <= numplayers; i++) {
            Player player = new Player(i);
            players.add(player);
            
        }
        
        for (int i = 0; i < 4; i++) {
            for (Player player : players) {
                Card card = pack.remove();
                player.addCard(card);
                
            }
        }
        for (Player player : players) {
                Thread t = new Thread(player);
                t.start();
                
            }
        

                




         
        
    }
}
