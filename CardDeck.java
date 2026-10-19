import java.io.IOException;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CardDeck {
    private Queue<Card> cards;
    private final int id;
    private final ReentrantLock lock = new ReentrantLock();
    private final Logger logger;
    private FileHandler fileHandler;
    /* constructor uses custom formatter to create deck files*/
    public CardDeck(int id) throws IOException{
        cards = new LinkedList<Card>();
        this.id = id;
        logger = Logger.getLogger("Deck "+ id);

        fileHandler = new FileHandler("deck"+id+"_output.txt");
        fileHandler.setFormatter(new CustomFormat());

        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);

    }
    /* constructer with logging switch for testing purposes*/
    public CardDeck(int id, boolean log) throws IOException{
        cards = new LinkedList<Card>();
        this.id = id;
        logger = Logger.getLogger("Deck "+ id);
        
        if(log){
            fileHandler = new FileHandler("deck"+id+"_output.txt");
            fileHandler.setFormatter(new CustomFormat());

            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false);
        }
        else{
            logger.setLevel(Level.OFF);
        }
        

    }
    /* returns deck number*/
    public int getId(){
        return id;
    }
    /* allows for holding of lock over different operations*/
    public ReentrantLock getLock(){
        return lock;
    }
    /* draw a card from top of deck*/
    public Card removeFromDeck(){ // Draw
        checkLock();
        if (cards.isEmpty()) throw new NoSuchElementException("Deck is empty");
        else return cards.remove();
    }
    /* add card to back of deck for discarding*/
    public void addCard(Card card){ // Discard
        checkLock();
        cards.add(card);
        
    }
    /* returns true if deck is empty*/
    public boolean isEmpty(){
        checkLock();
        return cards.isEmpty();
    }
    /* reports if deck is writing to a log file*/
    public boolean isLogging(){
        return (logger!=null);
    }
    /* writes decks final contents as a single line*/
    public void doFinalLog(){
        if(this.isLogging()){
            checkLock();
            String msg = "deck"+ id + " contents: ";
            for (Card card : cards) {
                msg += card.getDenom() + " ";

                
            }
            logger.info(msg);
        }
        
       
    
    
    }
    /* closes output files once final log has been written*/
    public void close(){
        checkLock();
        if(this.isLogging()){
            fileHandler.close();
        }
    }
    /* throws if calling thread does not have deck lock */
    public void checkLock(){
        if(!lock.isHeldByCurrentThread()){
            throw new IllegalStateException("Current thread doesn't hold deck lock");

        }
    }



}
