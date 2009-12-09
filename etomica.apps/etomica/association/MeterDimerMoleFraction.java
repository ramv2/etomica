package etomica.association;

import etomica.EtomicaInfo;
import etomica.api.IBox;
import etomica.api.ISpecies;
import etomica.data.DataSourceScalar;
import etomica.space.Boundary;
import etomica.space.ISpace;
import etomica.units.Fraction;

/**
 * Meter for measurement of species mole fraction within a specified subvolume
 */
public class MeterDimerMoleFraction extends DataSourceScalar {
	
    private AssociationManager associationManager;

    public MeterDimerMoleFraction(ISpace space, IBox _box) {
        super("Dimer Mole Fraction",Fraction.DIMENSION);
        if(!(_box.getBoundary() instanceof Boundary)) {
        	throw new RuntimeException("The box boundary must be a subclass of etomica.Space.Boundary");
        }
        box = _box;
    }

    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Local number density in a subregion of a box");
        return info;
    }
    
    public void setAssociationManager(AssociationManager associationManager){
    	this.associationManager = associationManager;
    }
    
    /**
     * @return the current value of the local density or local mole fraction
     */
    public double getDataAsScalar() {
        if (box == null) throw new IllegalStateException("must call setBox before using meter");        
        double ni = associationManager.getAssociatedAtoms().getAtomCount();
        return ni/box.getMoleculeList().getMoleculeCount();
    }
    
    /**
     * @return Returns the box.
     */
    public IBox getBox() {
        return box;
    }
    /**
     * @param box The box to set.
     */

    public void setBox(IBox box) {
        this.box = box;
    }


    private static final long serialVersionUID = 1L;
    private IBox box;
}