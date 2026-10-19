import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;



public class Player implements Runnable{
    private List<Card> cards;
    private int number = -1;
    private final Logger logger;
    private volatile boolean running = true;
    private List<Player> players;
    private int winnerNumber = -1;
    private FileHandler fileHandler;

    /* constructor that creates an output file for each player*/
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
    /* constructor for testing that creates an output file for each player*/
    public Player(int playerNumber, List<Player> players, boolean log) throws IOException{
        number = playerNumber;
        this.players = players;
        cards = new ArrayList<Card>();
        logger = Logger.getLogger("Player " + number);
        if(log){
            
            

            fileHandler = new FileHandler("player"+number+"_output.txt");
            fileHandler.setFormatter(new CustomFormat());

            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false);
        }
        else{
            logger.setLevel(Level.OFF);
        }


    }
    /* the player's whole game run on its own thread*/
    public void run(){
        try {
            /* barrier that makes sure every player is ready before game starting*/
            CardGame.startLatch.countDown();
            try {
                CardGame.startLatch.await();
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
            
            int gameState = 0; /* 0 = still player and 1 = this player has won*/
            CardDeck left;
            CardDeck right;
            /* check for immediate win*/
            if(this.hasWon()){
                gameState = 1;
            }
            /* work at players decks, player i draws from deck i and discards to deck i+1*/
            if (number != CardGame.numplayers){
                left = CardGame.decks[number-1];
                right = CardGame.decks[number];
            }
            /* last player case, right deck is deck 1*/
            else{
                left = CardGame.decks[number-1];
                right = CardGame.decks[0];
    
            }
            /* player carries on taking turns until it has won, or is told to stop from another thread winning*/
            while (gameState == 0 && running) {  
                gameState = this.play(left, right, CardGame.numplayers);
                
                
            }
            /* closing lines of logging (wins / informed by other player they have won, exiting)*/
            if(this.isLogging())this.gameEndLog();
            /* if this play has won notify every other player*/
            if (gameState == 1) {
                for (Player player: players) {
                    if (player!=this){
                        player.stop(number);
                    }
                
                
                }
            }
            
        } 
        finally{
            if(this.isLogging()) fileHandler.close();
            CardGame.latch.countDown();
        }
        
    }
    /* called by winning player's thread to tell this player game is over whilst recording who won*/
    public void stop(int winnerNumber){
        this.winnerNumber = winnerNumber;
        running = false;
        
        
    }
    /* playing logic for one turn, returing 1 if player has winning hand and 0 if not*/
    public int play (CardDeck leftDeck, CardDeck rightDeck, int numbers){
        /* immediate win case*/
        if (this.hasWon()) return 1;
        List<Card> unpreferredCards = new ArrayList<Card>();
        for (Card card: cards){
            if (card.getDenom() != (number)){
                unpreferredCards.add(card);
            }  
        }
        
        /* lock both decks for whole turn*/
        if (numbers != number){
            leftDeck.getLock().lock();
            rightDeck.getLock().lock();

        }
        else {
            rightDeck.getLock().lock();
            leftDeck.getLock().lock();
        }
        try {
            /* if left deck is empty player simply skips turn*/
            if(!leftDeck.isEmpty()){
                /* card drawn from front end of left deck*/
                Card drawn = leftDeck.removeFromDeck();
                
                logger.info(() -> "player " + number + " draws a " + drawn.getDenom() + " from deck " + number);
                cards.add(drawn);
                /*keep every card that is not this players number */
                unpreferredCards = new ArrayList<Card>();
                for (Card card: cards){
                    if (card.getDenom() != (number)){
                        unpreferredCards.add(card);
                    }  
                }
                /* discards a random card that is not preferred (not player's number)*/
                if (!unpreferredCards.isEmpty() ){
                    Random r = new Random();
                    int index = r.nextInt(unpreferredCards.size());
                    Card discarded = unpreferredCards.get(index);
                    cards.remove(discarded);
                    rightDeck.addCard(discarded);
                    /* logging for last player so it outputs correct deck*/
                    if (number == CardGame.numplayers){
                        logger.info(() -> "player " + number + " discards a " + discarded.getDenom() + " to deck 1");
                    }
                    else {
                        logger.info(() -> "player " + number + " discards a " + discarded.getDenom() + " to deck " + (number+1));
                    }
                    
                    
                }
                /* logging the player's hand after the turn*/
                String msg = "player "+ number + " current hand is ";
                for (Card card : cards) {
                    msg += card.getDenom() + " ";

                    
                }
                logger.info(msg);
            }
            
        
            
        }
        /* always unlock decks no matter what happens to prevent neighbouring players getting blocked*/
        finally {
            leftDeck.getLock().unlock();
            rightDeck.getLock().unlock();
        }
        /* check for win after both unlocking and turn complete*/
        if (this.hasWon()) return 1;
        
        return 0;
    }
    /* used when dealing to add a card to hand*/
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
    /* returns players hand to be used for tests*/
    public List<Card> getCards(){
        return cards;
    }
    /* removes a card from hand*/
    public void removeCard(Card card){
        cards.remove(card);
    }
    /* returns a players number*/
    public int getNumber(){
        return number;
    }
    public boolean isLogging(){
        return (logger!=null);
    }
    public void closeLogger(){
        if(this.isLogging()) fileHandler.close();
    }

    /* player has won when they hold exactly 4 cards of the same number*/
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
    /* closing line of logs, winner logs wins, exits and final hand; other players log who informed them, exits and hand*/
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