import java.util.LinkedList;
import java.util.Queue;

public class CardDeck {
    private Queue<Card> cards;

    public CardDeck(){
        cards = new LinkedList<Card>();
    }
    public synchronized Card removeFromPack(){ // Draw
        return cards.remove();
    }
    public synchronized void addToPack(Card card){ // Discard
        cards.add(card);
        
    }
}
