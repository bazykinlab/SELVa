/**
 * Enum class for the NEW_FITNESS_RULE options
 */

public enum NewFitnessRule{
    SHUFFLE, IID, CURRENT_ALLELE_DEPENDENT;

    /**
     * Return the enum corresponding to the given string (case-insensitive)
     * @param value the string to convert to enum
     * @throws UnrecognizedValueException if the string is not recognized
     */
    static NewFitnessRule stringToEnum(String value){
	try{
	    return valueOf(value.toUpperCase());
	}
	catch(IllegalArgumentException e){
	    throw new UnrecognizedValueException("NEW_FITNESS_RULE", value);
	}
    }
}
