import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;



public class Player implements Runnable{
    private List<Card> cards;
    private int number = -1;
    private final Logger logger;
    private volatile boolean running = true;
    private List<Player> players;


    public Player(int playerNumber, List<Player> players) throws IOException{
        number = playerNumber;
        this.players = players;
        cards = new ArrayList<Card>();
        logger = Logger.getLogger("Player " + number);

        FileHandler fileHandler = new FileHandler("player_"+number+"_output.txt");
        fileHandler.setFormatter(new CustomFormat());

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
            if (!left.isEmpty()) {
            gameState = this.play(left, right, CardGame.numplayers);
            }
            
            
        }
        if (gameState == 1) {
            logger.info("player" + number + "wins");
            for (Player player: players) {
                if (player!=this){
                    player.stop();
                }
                
            }
            for (CardDeck deck: CardGame.decks) {
                deck.getLock().lock();
                deck.doFinalLog();
                deck.getLock().unlock();
            }
        }
    }
    public void stop(){

        running = false;
        
    }
    public int play (CardDeck leftDeck, CardDeck rightDeck, int numbers){
        List<Card> unpreferredCards = new ArrayList<Card>();
        for (Card card: cards){
            if (card.getDenom() != (number)){
                unpreferredCards.add(card);
            }  
        }
        if (unpreferredCards.isEmpty()) return 1;

        if (numbers != number){
            leftDeck.getLock().lock();
            rightDeck.getLock().lock();

        }
        else {
            rightDeck.getLock().lock();
            leftDeck.getLock().lock();
        }
        Card drawn = leftDeck.removeFromDeck();
        
        logger.info("player " + number + " draws a " + drawn.getDenom() + " from deck " + number);
        cards.add(drawn);

        unpreferredCards = new ArrayList<Card>();
        for (Card card: cards){
            if (card.getDenom() != (number)){
                unpreferredCards.add(card);
            }  
        }
        if (!unpreferredCards.isEmpty() ){
            Random r = new Random();
            int index = r.nextInt(unpreferredCards.size());
            Card discarded = unpreferredCards.get(index);
            cards.remove(discarded);
            rightDeck.addCard(discarded);
            if (number == CardGame.numplayers){
                logger.info("player " + number + " discards a " + discarded.getDenom() + " to deck 1" );
            }
            else {
                logger.info("player " + number + " discards a " + discarded.getDenom() + " to deck " + (number+1));
            }
            
            
        }
        
            
        
        leftDeck.getLock().unlock();
        rightDeck.getLock().unlock();
        if (this.hasWon()){
            return 1;
        }
        
        String msg = "player "+ number + " current hand is ";
        for (Card card : cards) {
            msg += card.getDenom() + " ";

            
        }
        logger.info(msg);
        if(running == false){
        logger.info("player " + number + " exits");
        }
        return 0;
    }

    public void addCard(Card card){
        cards.add(card);
        if (cards.size() == 4){
            String msg = "player "+ number + " initial hand is ";
            for (Card card1 : cards) {
                msg += card1.getDenom() + " ";

                
            }
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


    public boolean hasWon(){
        int cardDenom = cards.get(0).getDenom();
        for (int i=1; i < cards.size(); i++){

            if (cards.get(i).getDenom() != cardDenom){
                return false;
            }
        }
        return true;
    }

}