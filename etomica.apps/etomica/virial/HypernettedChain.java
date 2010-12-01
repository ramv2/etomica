package etomica.virial;

import etomica.util.SineTransform;


/**
 * 
 * Calculates the Percus-Yevick (PY) virial coefficients of second to Mth order for any spherically-symmetric Mayer function, fr.
 * 
 * This class has only been tested for the hard sphere and Lennard-Jones potentials.
 * 
 * @author kate
 *
 */

public class HypernettedChain {

	public HypernettedChain() {
	}
	
	public double[] computeB(double[] fr, int M, int N, double del_r, boolean DCF) {
		
		/*******************************************
		/*******************************************
		 * 
		 * Computes Percus-Yevick approximations of the virial coefficients up to Mth order.  
		 * The second and third coefficients are fully accurate (the PY approximation is exact).
		 * 
		/*******************************************
		/********************************************/
	
		double[] B = new double[M];
		
		SineTransform dst = new SineTransform();
		double del_k = Math.PI/(del_r*N);
		
		double[] dummy = new double[N];
		dummy = fr;
		double[] fk = new double[N];
		fk = dst.forward(dummy, del_r);
		
		// Arrays to store the density expansion coefficients of c(r) and h(r)
		double[][] cnr = new double[M][N];
		double[][] hnr = new double[M][N];
		double[][] tnr = new double[M][N];
		
		// Fill zeroth-order density expansion coefficients of c(r) and h(r)
		for (int i=0;i<N;i++) {
			cnr[0][i] = fr[i];
			hnr[0][i] = fr[i];
			tnr[0][i] = 0;
		}
		
		double B2 = -1.0/(2.0)*(fk[0]);
		
		B[0] = B2;
		
		// System.out.println("B2 = " + (B2));
		double[] cmr = new double[N];
		
		//Compute B3 (from c1) up to BM (from c(M-2))
		for (int m = 1; m <= M-2; m++) {
			
			/**************************************************************************************
			/**************************************************************************************
			 * Apply the Ornstein-Zernike relation to compute mth-order density expansion of t.
			/**************************************************************************************
			/***************************************************************************************/
			
			OrnsteinZernike oz = new OrnsteinZernike();
			
			double[] tmr = oz.tCompute(cnr, hnr, N, m, del_r);
			
			/**************************************************************************************
			/**************************************************************************************
			 * Apply the Percus-Yevick approximation to compute mth-order density expansion of c.
			/**************************************************************************************
			/***************************************************************************************/
			
			for (int i = 0; i < N; i++) {
				
				tnr[m][i] = tmr[i];
				
				if (m == 1) {
					hnr[m][i] = (fr[i]+1) * tnr[1][i];
				} else if (m == 2) {
					hnr[m][i] = (fr[i]+1) * (tnr[2][i] + 0.5*tnr[1][i]*tnr[1][i]);
				} else if (m == 3) {
					hnr[m][i] = (fr[i]+1) * (tnr[3][i]+tnr[1][i]*tnr[2][i]+(1.0/6.0)*tnr[1][i]*tnr[1][i]*tnr[1][i]);
				} 
			
				cnr[m][i] = hnr[m][i] - tnr[m][i];
				
				cmr[i]=cnr[m][i];
				
 			}
			
			/*******************************************
			/*******************************************
			 * Calculate (m+2)th virial coefficient
			/*******************************************
			/********************************************/
			
			dummy = cmr;
			double[] cmk = new double[N];
			cmk = dst.forward(dummy, del_r);
			
			double Bm = -1.0/((double)m+2.0)*(cmk[0]); // B3 for m = 1
			
			B[m] = Bm;

            //System.out.println("B"+(m+2) + " = "+ (Bm) );
		
		
		}
		
		if (DCF) {
			return cmr;
		} else {
			return B;
		}
	}
	
}