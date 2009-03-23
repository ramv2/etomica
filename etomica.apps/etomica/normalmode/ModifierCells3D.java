package etomica.normalmode;

import etomica.api.IBox;
import etomica.api.ISpecies;
import etomica.modifier.Modifier;
import etomica.units.Dimension;
import etomica.units.Null;

/**
 * Modifier class that enables change of the number of cells in 2D
 * 
 */
public class ModifierCells3D implements Modifier, java.io.Serializable {

    /**
     * 
     */
    public ModifierCells3D(IBox box, ISpecies species) {
        this.box = box;
        this.species = species;
    }

    public void setValue(double d) {
        if (d < 0) d = 0;
        previousValue = mostRecentValue;
        mostRecentValue = (int)d;
        
        int nCell = (int)d;
        box.setNMolecules(species, nCell*nCell*nCell*4);
    }

    public double getValue() {
        return Math.pow(box.getNMolecules(species)/4, 1/3);
    }

    public Dimension getDimension() {
        return Null.DIMENSION;
    }
    
    public String getLabel() {
        return "n-Cell Number";
    }
    
 
    private static final long serialVersionUID = 1L;
    protected final IBox box;
    protected final ISpecies species;
    protected int mostRecentValue, previousValue;
}
