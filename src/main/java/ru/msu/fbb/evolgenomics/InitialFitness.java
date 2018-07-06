/**
 * Enum class for the INITIAL_FITNESS_DEFINITION options
 */

public enum InitialFitness{
    FILE, FLAT, LOGNORM, GAMMA;

    /**
     * Return the enum corresponding to the given string (case-insensitive)
     * @param value the string to convert to enum
     * @throws UnrecognizedValueException if the string is not recognized
     */
    static InitialFitness stringToEnum(String value){
	try{
	    return valueOf(value.toUpperCase());
	}
	catch(IllegalArgumentException e){
	    throw new UnrecognizedValueException("INITIAL_FITNESS", value);
	}
    }

}
