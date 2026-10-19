/*a single playing card that is immutable */
public class Card {
    /* cards face value, class is thread safe because it is final*/
    private final int denom;
    
    public Card(int denomination){
        /* reject anything below zero when card is created, invalid card can't exist anywhere */
        if(denomination>=0){
            denom = denomination;
        }
        else {
            throw new IllegalStateException("denomination is below 0");
        }
    }
    /* returns face value of card */
    public int getDenom(){
        return denom;
    }

}