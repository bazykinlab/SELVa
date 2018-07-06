import org.apache.commons.math3.linear.*;

/**
 * Class for generating the matrix Q and the stationary distribution pi 
 * from the fitness vector, following the Genreal Time Reversible Model
 *
 */
public class QfromFitness{

    /** 
     * Generates Q from fitness vector F 
     * The computation follows Eq 3, Yang and Nielsen 2008, setting all the
     * non-selection terms (such as mutation bias) to 1
     * 
     * @param F the fitness vector
     *
     * @return matrix Q
     */
    
    public static double[][] generateQ(double[] F){
	int vectSize = F.length;
	double rowsums[] = new double[vectSize];
	double [][] Q = new double[vectSize][vectSize];
	for (int i = 0; i < vectSize; i++){

	    for (int j = 0;  j< vectSize; j++){
		if (i != j){
		    double diff = F[j] - F[i];
		    //get the fixation probability ratio
		    if (Math.abs(diff) > 1e-9)
			Q[i][j] = diff/(double)(1-Math.exp(-diff));
		    else
			Q[i][j] = 1;
		    rowsums[i]+=Q[i][j];
		}
	    }
	}
	for (int i = 0; i < vectSize; i++){
	    Q[i][i] = -rowsums[i];
	}
	return Q;
    }
    
    /** 
     * Generates the stationary probability vector pi from the fitness vector
     * using the fact that pi[i] is proportional to exp(F[i]) - eq. 4 in Yang and Nielsen 2008
     * 
     * @param fitness The fitness vector
     *
     * @return stationary distribution pi
     */
    public static double[]  PiFromFitness (double[] fitness) {
	double[] expVect = new double[fitness.length];
	for (int i = 0; i < fitness.length; i++){
	    expVect[i] = Math.exp(fitness[i]);
	}
	Utils.normalizeArray(expVect);
	return expVect;
    }

    /** 
     * Generates the stationary probability vector pi from Q
     * 
     * @param Q the matrix Q
     *
     * @return stationary distribution pi
     */
     public static double []  PiFromQ (double[][] Q) {
    	/* we want to solve the following system of equations:
          \forall j \Sum_i{pij*q_ij} = 0, or pi * Q = [0,0,..,0] //Yang formula 1.56
          \Sum pi = 1;
    	  In other words, 
    	create matrix A that looks like 
    	    Q 1
              1
              1
              ... 
              1
    	   we want to solve the system pi * A  = [0, 0, 0, ..., 0, 1] = b
    	   we do it by doing this:
    	   pi * A = b
    	   pi * A * A' = b * A'
    	   pi = pi * (A * A') * (A * A')^-1 = b * A' * (A * A')^-1
    	*/
    	double[][] coeff = new double[Q.length][Q[0].length+1];
    	for (int i = 0; i < Q.length; i++)
    	    for (int j = 0; j < Q[i].length; j++)
    		coeff[i][j] = Q[i][j];
    	//add new column of all 1's for the equation pi_0 + pi_1+...pi_n = 1
    	for (int i = 0; i < coeff.length; i++)
    	    coeff[i][coeff[i].length-1] = 1;
    	RealMatrix A = new Array2DRowRealMatrix(coeff);

    	//AAT = A * A'
    	RealMatrix AAT = A.multiply(A.transpose());
    	double[] b = new double[A.getRowDimension()+1];
    	for (int i = 0; i < b.length-1; i++)
    	    b[i] = 0;
    	b[b.length-1] = 1;
    	double [] intermed = A.transpose().preMultiply(b);
    	double[] pidouble = MatrixUtils.inverse(AAT).preMultiply(intermed);
    	double[] pi = new double[pidouble.length];
    	for (int i = 0; i < pidouble.length; i++)
    	    pi[i] = (double) pidouble[i];
    	return pi;
    }
    public static void main (String[] args){
	int ALPHABET_SIZE = 2;
	double [] F = new double[ALPHABET_SIZE];
	F[0] = 1;
	for (int i = 1; i < F.length; i++)
	    F[i] = 1;
	double[][]Q = generateQ(F);
	double [] pi = PiFromQ(Q);
    }
}
