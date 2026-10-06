import java.util.ArrayList;
import java.util.List;

public class Player {
    private List<Card> cards;
    private int number = -1;

    public Player(int playerNumber){
        number = playerNumber;
        cards = new ArrayList<Card>();

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