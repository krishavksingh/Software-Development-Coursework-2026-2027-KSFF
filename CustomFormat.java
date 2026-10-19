
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
/* custom log formatting used by every player and deck  */
public class CustomFormat extends Formatter{
    @Override 
    public String format(LogRecord record){
        /* return just the message followed by a line break */
        return record.getMessage() + System.lineSeparator();
    }
}
