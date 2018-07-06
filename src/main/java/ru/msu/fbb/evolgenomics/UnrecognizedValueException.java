/**
 * The exception for an invalid parameter value in the config file.
 * Printes out the message: "Value V of parameter P not supported.\nPerhaps you need to check its spelling?  Aborting", with the calling method providing V and P.
 */
public class UnrecognizedValueException extends RuntimeException{
    /**
     * @param parameter the parameter whose value is not recognized
     * @param value the value for that parameter that is not reconized
     */
    
    public UnrecognizedValueException(String parameter, String value){
	super("\nValue " + value + " of parameter " + parameter + " not supported.\nPerhaps you need to check its spelling?  Aborting");
    }

}
