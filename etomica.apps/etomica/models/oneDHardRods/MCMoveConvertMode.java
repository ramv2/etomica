package etomica.models.oneDHardRods;

import etomica.api.IBox;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.IVector;
import etomica.atom.iterator.AtomIterator;
import etomica.atom.iterator.AtomIteratorAllMolecules;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.integrator.mcmove.MCMoveBoxStep;
import etomica.normalmode.CoordinateDefinition;
import etomica.normalmode.CoordinateDefinition.BasisCell;

/**
 * A Monte Carlo move which selects a wave vector, and an eigenvector allowed 
 * by that wave vector.
 * 
 * @author cribbin
 *
 */
public class MCMoveConvertMode extends MCMoveBoxStep{

    private static final long serialVersionUID = 1L;
    protected CoordinateDefinition coordinateDefinition;
    private final AtomIteratorAllMolecules iterator;
    protected double[][] uOld;
    protected double[] deltaU;
    protected final IRandom random;
    protected double energyOld, energyNew /*, latticeEnergy*/;
    protected final MeterPotentialEnergy energyMeter;
    private double[][][] eigenVectors;
    private IVector[] waveVectors;
    int changedWV;
    private double gaussian;
    boolean waveVectorRandomFlag;     //used to indicate whether the wavevector of 
                                //of interest is random or not.
    
    public MCMoveConvertMode(IPotentialMaster potentialMaster, IRandom random) {
        super(potentialMaster);
        
        this.random = random;
        iterator = new AtomIteratorAllMolecules();
        energyMeter = new MeterPotentialEnergy(potentialMaster);
        waveVectorRandomFlag = false;
        
    }

    public void setCoordinateDefinition(CoordinateDefinition newCoordinateDefinition) {
        coordinateDefinition = newCoordinateDefinition;
        deltaU = new double[coordinateDefinition.getCoordinateDim()];
        uOld = null;
    }
    
    public CoordinateDefinition getCoordinateDefinition() {
        return coordinateDefinition;
    }

    /**
     * Set the wave vectors used by the move.
     * 
     * @param wv
     */
    public void setWaveVectors(IVector[] wv){
        waveVectors = new IVector[wv.length];
        waveVectors = wv;
    }
    /**
     * Informs the move of the eigenvectors for the selected wave vector.  The
     * actual eigenvectors used will be those specified via setModes
     */
    public void setEigenVectors(double[][][] newEigenVectors) {
        eigenVectors = newEigenVectors;
    }
    
    public void setBox(IBox newBox) {
        super.setBox(newBox);
        iterator.setBox(newBox);
        energyMeter.setBox(newBox);
    }

    public AtomIterator affectedAtoms() {
        return iterator;
    }

    public boolean doTrial() {
        energyOld = energyMeter.getDataAsScalar();
        int coordinateDim = coordinateDefinition.getCoordinateDim();
        BasisCell[] cells = coordinateDefinition.getBasisCells();
        
        //nan These lines make it a single atom-per-molecule class, and
        // assumes that the first cell is the same as every other cell.
//        BasisCell cell = cells[0];
        double sqrtCells = Math.sqrt(cells.length);
//        double[] calcedU = coordinateDefinition.calcU(cell.molecules);
        uOld = new double[cells.length][coordinateDim];
        
        // Select the wave vector whose eigenvectors will be changed.
        //The zero wavevector is center of mass motion, and is rejected as a 
        //possibility.
        if(waveVectorRandomFlag == true){
            changedWV = random.nextInt(waveVectors.length-1);
            changedWV +=1;
        }
        
        //calculate the new positions of the atoms.
        //loop over cells
        double delta1 = (2*random.nextDouble()-1) * stepSize;
        double delta2 = (2*random.nextDouble()-1) * stepSize;
        for(int iCell = 0; iCell < cells.length; iCell++){
            //store old positions.
            double[] uNow = coordinateDefinition.calcU(cells[iCell].molecules);
            System.arraycopy(uNow, 0, uOld[iCell], 0, coordinateDim);
            BasisCell cell = cells[iCell];
            for(int i = 0; i< coordinateDim; i++){
                  deltaU[i] = 0;
            }
            
            //loop over the wavevectors, and sum contribution of each to the
            //generalized coordinates.  Change the selected wavevector's eigen-
            //vectors at the same time!
            double kR = waveVectors[changedWV].dot(cell.cellPosition);
                double coskR = Math.cos(kR);
                double sinkR = Math.sin(kR);
                for(int i = 0; i < coordinateDim; i++){
                    for(int j = 0; j < coordinateDim; j++){
                        deltaU[j] += eigenVectors[changedWV][i][j]*2.0*(delta1*coskR - delta2*sinkR);
                    }
                }
            double normalization = 1/Math.sqrt(cells.length);
            for(int i = 0; i < coordinateDim; i++){
                deltaU[i] *= normalization;
            }
            
            for(int i = 0; i < coordinateDim; i++) {
                uNow[i] += deltaU[i];
            }
            coordinateDefinition.setToU(cells[iCell].molecules, uNow);
            
        }
        
        energyNew = energyMeter.getDataAsScalar();
        return true;
    }
    
    public double getA() {
        return 1;
    }

    public double getB() {
        return -(energyNew - energyOld);
    }
    
    public void acceptNotify() {
//        System.out.println("accept");
    }

    public double energyChange() {
        return energyNew - energyOld;
    }

    public void rejectNotify() {
//        System.out.println("reject");
        // Set all the atoms back to the old values of u
        BasisCell[] cells = coordinateDefinition.getBasisCells();
        for (int iCell = 0; iCell<cells.length; iCell++) {
            BasisCell cell = cells[iCell];
            coordinateDefinition.setToU(cell.molecules, uOld[iCell]);
        }
    }
    
    public double getGaussian(){
        return gaussian;
    }
    
    public void setWaveVector(int wv){
        changedWV = wv;
        waveVectorRandomFlag = false;
    }
    public int getWaveVector(){
        return changedWV;
    }

}