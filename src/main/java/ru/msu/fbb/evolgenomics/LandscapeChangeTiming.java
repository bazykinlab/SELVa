/**
 * Enum class for the LANDSCAPE_CHANGE_TIMING options
 */

public enum LandscapeChangeTiming{
    STOCHASTIC, FIXED_INTERVAL_LENGTH, FIXED_NUM_CHANGES, SPECIFIED_BRANCH_AND_TIME;

    /**
     * Return the enum corresponding to the given string (case-insensitive)
     * @param value the string to convert to enum
     * @throws UnrecognizedValueException if the string is not recognized
     */
    static LandscapeChangeTiming stringToEnum(String value){
	try{
	    return valueOf(value.toUpperCase());
	}
	catch(IllegalArgumentException e){
	    throw new UnrecognizedValueException("LANDSCAPE_CHANGE_TIMING", value);
	}
    }
}
