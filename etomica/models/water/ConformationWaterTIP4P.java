package etomica.models.water;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IConformation;
import etomica.space.ISpace;
import etomica.units.Electron;

/**
 * Conformation for 4-point water molecule.
 */
public class ConformationWaterTIP4P implements IConformation, java.io.Serializable {

    public ConformationWaterTIP4P(ISpace space) {
        this.space = space;
    }
    
    public void initializePositions(IAtomSet list){
        
        IAtomPositioned o = (IAtomPositioned)list.getAtom(SpeciesWater4P.indexO);
        o.getPosition().E(new double[] {0, 0, 0.0});

        double x = bondLengthOH*Math.sin(0.5*angleHOH);
        double y = bondLengthOH*Math.cos(0.5*angleHOH);
        
        IAtomPositioned h1 = (IAtomPositioned)list.getAtom(SpeciesWater4P.indexH1);
        h1.getPosition().E(new double[] {-x, y, 0.0});
                
        IAtomPositioned h2 = (IAtomPositioned)list.getAtom(SpeciesWater4P.indexH2);
        h2.getPosition().E(new double[] {+x, y, 0.0});
        
        IAtomPositioned m = (IAtomPositioned)list.getAtom(SpeciesWater4P.indexM);
        m.getPosition().E(new double[] {0, rOM, 0.0});
    }
    
    public final static double [] Echarge = new double [4];
    static {
        ConformationWaterTIP4P.Echarge[SpeciesWater4P.indexH1] = Electron.UNIT.toSim( 0.52);
        ConformationWaterTIP4P.Echarge[SpeciesWater4P.indexH2] = Electron.UNIT.toSim( 0.52);
        ConformationWaterTIP4P.Echarge[SpeciesWater4P.indexO] = Electron.UNIT.toSim( 0.00);
        ConformationWaterTIP4P.Echarge[SpeciesWater4P.indexM] = Electron.UNIT.toSim(-1.04);
    }
    
    private static final long serialVersionUID = 1L;
    protected final ISpace space;
    public static final double bondLengthOH = 0.9572;
    public static final double angleHOH = 104.52*Math.PI/180.;
    public static final double rOM=0.15;
}
