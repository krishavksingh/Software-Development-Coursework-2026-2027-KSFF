public class Card {
    private int denom = -1;

    public void setDenom(int number){
        if(denom==-1){
        denom = number;
        }
        else{
            throw new UnsupportedOperationException("Cannot change value of the denomination of a card after assignment.");
        }
    }

}