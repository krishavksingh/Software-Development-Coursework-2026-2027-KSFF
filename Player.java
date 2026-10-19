import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;



public class Player implements Runnable{
    private List<Card> cards;
    private int number = -1;
    private final Logger logger;
    private volatile boolean running = true;
    private List<Player> players;
    private int winnerNumber = -1;
    private FileHandler fileHandler;

    public Player(int playerNumber, List<Player> players) throws IOException{
        number = playerNumber;
        this.players = players;
        cards = new ArrayList<Card>();
        logger = Logger.getLogger("Player " + number);
        

        fileHandler = new FileHandler("player"+number+"_output.txt");
        fileHandler.setFormatter(new CustomFormat());

        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);


    }
    public void run(){
        try {
            CardGame.startLatch.countDown();
            try {
                CardGame.startLatch.await();
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
            
            int gameState = 0;
            CardDeck left;
            CardDeck right;
    
            if(this.hasWon()){
                gameState = 1;
            }
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
            this.gameEndLog();
    
            if (gameState == 1) {
                for (Player player: players) {
                    if (player!=this){
                        player.stop(number);
                    }
                
                
                }
            }
            
        } 
        finally{
            fileHandler.close();
            CardGame.latch.countDown();
        }
        
    }
    public void stop(int winnerNumber){
        this.winnerNumber = winnerNumber;
        running = false;
        
    }
    public int play (CardDeck leftDeck, CardDeck rightDeck, int numbers){
        
        if (this.hasWon()) return 1;
        List<Card> unpreferredCards = new ArrayList<Card>();
        for (Card card: cards){
            if (card.getDenom() != (number)){
                unpreferredCards.add(card);
            }  
        }
        

        if (numbers != number){
            leftDeck.getLock().lock();
            rightDeck.getLock().lock();

        }
        else {
            rightDeck.getLock().lock();
            leftDeck.getLock().lock();
        }
        try {
            if(!leftDeck.isEmpty()){
                Card drawn = leftDeck.removeFromDeck();
                
                logger.info(() -> "player " + number + " draws a " + drawn.getDenom() + " from deck " + number);
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
                        logger.info(() -> "player " + number + " discards a " + discarded.getDenom() + " to deck 1");
                    }
                    else {
                        logger.info(() -> "player " + number + " discards a " + discarded.getDenom() + " to deck " + (number+1));
                    }
                    
                    
                }
                String msg = "player "+ number + " current hand is ";
                for (Card card : cards) {
                    msg += card.getDenom() + " ";

                    
                }
                logger.info(msg);
            }
            
        
            
        }
        finally {
            leftDeck.getLock().unlock();
            rightDeck.getLock().unlock();
        }
        
        if (this.hasWon()) return 1;
        
        return 0;
    }

    public void addCard(Card card){
        cards.add(card);
        if (cards.size() == 4){
            String msg = "player "+ number + " initial hand ";
            for (Card card1 : cards) {
                msg += card1.getDenom() + " ";

            }
            logger.info(msg);
        }
    }

    public void removeCard(Card card){
        cards.remove(card);
    }

    public int getNumber(){
        return number;
    }


    public boolean hasWon(){
        if(cards.size()!=4) return false;
        int cardDenom = cards.get(0).getDenom();
        for (int i=1; i < cards.size(); i++){

            if (cards.get(i).getDenom() != cardDenom){
                return false;
            }
        }
        return true;
    }
    public void gameEndLog(){
        if ((!running || this.hasWon())){
            
            if(running == false && !this.hasWon()){
                logger.info(() -> "player " + winnerNumber + " has informed player " + number + " that player " + winnerNumber + " has won");
            }
            if(this.hasWon()){
                logger.info(() -> "player " + number + " wins");
                System.out.println("player " + number + " wins");
            }
            String finalmsg;
            if(running == false || this.hasWon()){
                logger.info(() -> "player " + number + " exits");

                if(this.hasWon()){
                    finalmsg = "player "+ number + " final hand: ";
                }
                else{
                    finalmsg = "player "+ number + " hand: ";
                }
                
            for (Card card : cards) {
                finalmsg += card.getDenom() + " ";

                
            }
            logger.info(finalmsg);
            }
        }
    }

}