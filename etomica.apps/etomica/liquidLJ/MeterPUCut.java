/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.liquidLJ;
import etomica.api.IBox;
import etomica.api.IPotentialMaster;
import etomica.atom.iterator.IteratorDirective;
import etomica.data.DataTag;
import etomica.data.IData;
import etomica.data.IEtomicaDataInfo;
import etomica.data.IEtomicaDataSource;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataDoubleArray.DataInfoDoubleArray;
import etomica.space.ISpace;
import etomica.units.Null;

/**
 * Meter for evaluation of the soft-potential pressure in a box.
 * Requires that temperature be set in order to calculation ideal-gas
 * contribution to pressure; default is to use zero temperature, which
 * causes this contribution to be omitted.
 *
 * @author David Kofke
 */
 
public class MeterPUCut implements IEtomicaDataSource {
    
    protected final DataDoubleArray data;
    protected final DataInfoDoubleArray dataInfo;
    protected final DataTag tag;
    protected IteratorDirective iteratorDirective;
    protected final PotentialCalculationSumCutoff pc;
    protected IPotentialMaster potentialMaster;
    protected double temperature;
    protected IBox box;
    private final int dim;
    
    public MeterPUCut(ISpace space, double[] cutoffs) {
        data = new DataDoubleArray(new int[]{cutoffs.length,4});
        dataInfo = new DataInfoDoubleArray("PU", Null.DIMENSION, new int[]{cutoffs.length,4});
        tag = new DataTag();
        dataInfo.addTag(tag);
    	dim = space.D();
        iteratorDirective = new IteratorDirective();
        iteratorDirective.includeLrc = false;
        pc = new PotentialCalculationSumCutoff(space, cutoffs);
    }

    public void setPotentialMaster(IPotentialMaster newPotentialMaster) {
        potentialMaster = newPotentialMaster;
    }
    
    public void setTemperature(double newTemperature) {
        temperature = newTemperature;
    }

    public void setBox(IBox newBox) {
        box = newBox;
        pc.setBox(box);
    }

    /**
     * Computes total pressure in box by summing virial over all pairs, and adding
     * ideal-gas contribution.
     */
    public IData getData() {
        if (potentialMaster == null || box == null) {
            throw new IllegalStateException("You must call setIntegrator before using this class");
        }
        pc.zeroSums();
        potentialMaster.calculate(box, iteratorDirective, pc);
        double[] uSum = pc.getUSums();
        double[] vSum = pc.getVSums();
        double[] x = data.getData();
        int j = 0;
        for (int i=0; i<uSum.length; i++) {
            double vol = box.getBoundary().volume();
            int N = box.getMoleculeList().getMoleculeCount();
            double density = N / vol;
            
            double P = density*temperature - vSum[i]/(vol*dim);
            double U = uSum[i]/N;
            x[j+0] = U;
            x[j+1] = P;
            x[j+2] = U/(4*Math.pow(density,4));
            // dbA/drho at constant Y
            // (Z - 4u/T)/density  --  for SS, Z-1 = 4u/T
            // dbA/dv2 at constant Y
            // dbA/dv2 = dbA/rho * (-rho^3/2)
            x[j+3] = -(P/(temperature*density) - 1 - 4 * U / (temperature))*density*density/2;
            
            j+=4;
        }
        return data;
    }

    public DataTag getTag() {
        return tag;
    }

    public IEtomicaDataInfo getDataInfo() {
        return dataInfo;
    }

}
