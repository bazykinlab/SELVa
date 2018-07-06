/**
 * RuntimeException to be thrown if the config file contains incompatible parameters
 */
public class InvalidParameterCombinationException extends RuntimeException{
    /**
     * @param msg The error message to be displayed followed by the word "Aborting"
     */
    public InvalidParameterCombinationException(String msg){
	super(msg + ". Aborting");
    }
}
