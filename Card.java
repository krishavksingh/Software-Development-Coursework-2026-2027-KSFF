public class Card {
    private final int denom;
    public Card(int denomination){
        if(denomination>=0){
            denom = denomination;
        }
        else {
            throw new IllegalStateException("denomination is below 0");
        }
    }
    public int getDenom(){
        return denom;
    }

}