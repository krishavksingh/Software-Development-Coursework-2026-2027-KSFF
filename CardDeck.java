import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class CardDeck {
    private Queue<Card> cards;
    private final int id;
    private final ReentrantLock lock = new ReentrantLock();

    public CardDeck(int id){
        cards = new LinkedList<Card>();
        this.id = id;
    }
    public int getId(){
        return id;
    }

    public ReentrantLock getLock(){
        return lock;
    }

    public synchronized Card removeFromDeck(){ // Draw
        return cards.remove();
    }
    
    public synchronized void addCard(Card card){ // Discard
        cards.add(card);
        
    }
}
