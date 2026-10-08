import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Player implements Runnable{
    private List<Card> cards;
    private int number = -1;
    private final Logger logger;
    private volatile boolean running = true;



    public Player(int playerNumber) throws IOException{
        number = playerNumber;
        
        cards = new ArrayList<Card>();
        logger = Logger.getLogger("Player " + number);

        FileHandler fileHandler = new FileHandler("player_"+number+"_output.txt");
        fileHandler.setFormatter(new SimpleFormatter());

        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);


    }
    public void run(){
        int gameState = 0;
        CardDeck left;
        CardDeck right;
        if (number != CardGame.numplayers){
            left = CardGame.decks[number-1];
            right = CardGame.decks[number];
        }
        else{
            left = CardGame.decks[number-1];
            right = CardGame.decks[0];

        }
        while (gameState == 0 && running) { 
            gameState = this.play(left, right, CardGame.numplayers);
            
        }
        if (gameState == 1) logger.info("player" + number + "wins");
    }
    public void stop(){

        running = false;
        logger.info("player" + number + "exits");
    }
    public int play (CardDeck leftDeck, CardDeck rightDeck, int numbers){
        if (numbers != number){
            leftDeck.getLock().lock();
            rightDeck.getLock().lock();

        }
        else {
            rightDeck.getLock().lock();
            leftDeck.getLock().lock();
        }
        Card drawn = leftDeck.removeFromDeck();
        logger.info("player " + number + " draws a " + drawn.getDenom() + "from deck" + number);
        cards.add(drawn);

        List<Card> unpreferredCards = new ArrayList<Card>();
        for (Card card: cards){
            if (card.getDenom() != (number)){
                unpreferredCards.add(card);
            }  
        }
        if (!unpreferredCards.isEmpty()){
            Card discarded = unpreferredCards.getFirst();
            cards.remove(discarded);
            logger.info("player " + number + " discards a " + discarded.getDenom() + "from deck" + (number+1));
            
        }
        else {
            return 1;
            
        }
        leftDeck.getLock().unlock();
        rightDeck.getLock().unlock();
        logger.info("player " + number + " current hand is " + cards.get(0).getDenom()  +" "+ cards.get(1).getDenom()  +" "+ cards.get(2).getDenom()  +" "+ cards.get(3).getDenom());
        return 0;
    }

    public void addCard(Card card){
        cards.add(card);
        if (cards.size() == 4){
            logger.info("player " + number + " initial hand is " + cards.get(0).getDenom() +" "+ cards.get(1).getDenom()+" " + cards.get(2).getDenom()+" " + cards.get(3).getDenom());
        }
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