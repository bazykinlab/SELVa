import java.util.concurrent.*;
import org.apache.commons.math3.distribution.*;
import org.apache.commons.math3.random.*;

/**
 * The collection of all "utility" functions for working with arrays and with probability.
 */
public class Utils{
    /** methods for working with arrays **/

    /**
     * Sum the values of a doublearray
     * @param arr the array to sum
     * @return sum of array elements
     */
    public static double arraySum(double[] arr){
	double sum = 0;
	for (int i = 0; i < arr.length; i++)
	    sum+=arr[i];
	return sum;
    }
    /**
     * Normalize the array to sum to one (in place, so its values change)
     * @param arr the array to normalize
     */
    
    public static void normalizeArray(double[] arr){	
double sum = arraySum(arr);;
	for (int i = 0; i < arr.length; i++)
	    arr[i]/=sum;
    }
    /**
     * Zero out an array
     * @param arr the array to zero out
     */
    public static void zeroOutArray(double[] arr){
	for (int i = 0; i < arr.length; i++)
	    arr[i] = 0.0f;
    }
    /**
     * Create a copy of a matrix (2D array)
     * @param source matrix to copy
     * @return a copy of the matrix
     */
    public static double [][] copyMat(double[][] source){
	double [][]target = new double[source.length][source[0].length];
	for (int i = 0; i < source.length; i++)
	    for (int j = 0; j < source[i].length; j++)
		target[i][j] = source[i][j];
	return target;
    }

    /**
     * Are all elements of the array the same
     * @param arr the array
     * @return true if all elements are equal, false otherwise
     */
    public static boolean allElementsEqual(double[] arr){
	int N = arr.length;
	if (N == 0)
	    return true;
	double x = arr[0];
	for (int i = 1; i < N; i++)
	    if (x != arr[i])
		return false;
	return true;
    }

}
