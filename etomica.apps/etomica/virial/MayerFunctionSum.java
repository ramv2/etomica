package etomica.virial;

import etomica.api.IBox;
import etomica.api.IMoleculeList;
import etomica.api.IPotential;

public class MayerFunctionSum implements MayerFunction {

	public MayerFunctionSum(MayerFunction[] functions, double []coefficients) {
		this.functions = functions;
		this.coefficients = coefficients;
	}

	public IPotential getPotential() {
		return null;
	}

	public void setBox(IBox box) {
		for (int i=0;i<functions.length;i++){
			functions[i].setBox(box);
		}
	}

	public double f(IMoleculeList pair, double r2, double beta) {
		double sum = 0;
		for (int i=0;i<functions.length;i++){
			sum += coefficients[i]*functions[i].f(pair, r2, beta);
		}
		return sum;
	}

	protected final MayerFunction[] functions;
	protected final double []coefficients;

}
