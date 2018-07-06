/**
 * RuntimeException to be thrown if a required parameter is missing from the config file.
 * Prints out the message "Parameter  P not set. Aborting", with P provided by the caller.
 */

public class MissingParameterException extends RuntimeException{
    /**
     * @param parameter The missing parameter
     */
    public MissingParameterException(String parameter){
	super("Parameter " + parameter + " not set. Aborting");

    }
}
