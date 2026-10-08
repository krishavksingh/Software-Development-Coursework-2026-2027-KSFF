import java.util.ArrayList;
import java.util.List;

public class Player {
    private List<Card> cards;
    private int number = -1;

    public Player(int playerNumber){
        number = playerNumber;
        cards = new ArrayList<Card>();

    }

    public void play (CardDeck leftDeck, CardDeck rightDeck, int numbers){
        if (numbers != number){
            leftDeck.getLock().lock();
            rightDeck.getLock().lock();

        }
        else {
            rightDeck.getLock().lock();
            leftDeck.getLock().lock();
        }
        cards.add(leftDeck.removeFromDeck());

        List<Card> unpreferredCards = new ArrayList<Card>();
        for (Card card: cards){
            if (card.getDenom() != (number)){
                unpreferredCards.add(card);
            }  
        }
        if (!unpreferredCards.isEmpty()){
            cards.remove(unpreferredCards.getFirst());
        }
        else {
            //
        }
        
    }

    public void addCard(Card card){
        cards.add(card);
    }

    public void removeCard(Card card){
        cards.remove(card);
    }

    public void getPreferredCards(){}

    public void getNonPreferredCards(){}

    public int getNumber(){
        return number;
    }

}