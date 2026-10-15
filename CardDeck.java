import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class CardDeck {
    private Queue<Card> cards;
    private final int id;
    private final ReentrantLock lock = new ReentrantLock();
    private final Logger logger;

    public CardDeck(int id) throws IOException{
        cards = new LinkedList<Card>();
        this.id = id;
        logger = Logger.getLogger("Deck "+ id);

        FileHandler fileHandler = new FileHandler("deck"+id+"_output.txt");
        fileHandler.setFormatter(new CustomFormat());

        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);

    }
    public int getId(){
        return id;
    }

    public ReentrantLock getLock(){
        return lock;
    }

    public Card removeFromDeck(){ // Draw
        return cards.remove();
    }
    
    public void addCard(Card card){ // Discard
        cards.add(card);
        
    }
    public boolean isEmpty(){
        return cards.isEmpty();
    }
    public void doFinalLog(){
        String msg = "deck"+ id + " contents: ";
        for (Card card : cards) {
            msg += card.getDenom() + " ";

            
        }
        logger.info(msg);
    }



}
