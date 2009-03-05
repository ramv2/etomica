package etomica.virial;

import etomica.api.IBox;
import etomica.api.IMoleculeList;
import etomica.api.IPotential;

/**
 * Mayer function that wraps another MayerFunction and ignores 
 * the temperature passed to f(), asking the wrapped MayerFunction
 * for its value at the MayerFunction Temperature's temperature.
 */
public class MayerFunctionTemperature implements MayerFunction {

    public MayerFunctionTemperature(MayerFunction fWrapped, double temperature) {
        mayerFunction = fWrapped;
        beta = 1/temperature;
    }

    public double f(IMoleculeList pair, double r2, double b) {
        return mayerFunction.f(pair,r2,beta);
    }

    public IPotential getPotential() {
        return mayerFunction.getPotential();
    }
    
    public void setBox(IBox newBox) {
        mayerFunction.setBox(newBox);
    }

    private final MayerFunction mayerFunction;
    private final double beta;
}
