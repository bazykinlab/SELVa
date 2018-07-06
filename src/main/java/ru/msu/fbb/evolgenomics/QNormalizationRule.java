/**
 * Enum class for the Q_NORMALIZATION options.
 */
public enum QNormalizationRule{
    CONSTANT_RATE, CONSTANT_FOR_FLAT;
    /**
     * Return the enum corresponding to the given string (case-insensitive)
     * @param value the string to convert to enum
     * @throws UnrecognizedValueException
     */
    static QNormalizationRule stringToEnum (String value) throws UnrecognizedValueException{
	try{
	    return valueOf(value.toUpperCase());
	}
	catch(IllegalArgumentException e){
	    throw new UnrecognizedValueException("Q_NORMALIZATION", value);
	}
    }
}
