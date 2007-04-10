package etomica.models.water;
import etomica.atom.AtomArrayList;
import etomica.atom.AtomLeaf;
import etomica.atom.iterator.AtomIteratorArrayListSimple;
import etomica.config.Conformation;
import etomica.space.Space;

/**
 * Conformation for 4-point water molecule.
 */
public class ConformationWaterTIP4P extends Conformation {

    private double bondLengthOH = 0.9572;
    private double angleHOH = 104.52*Math.PI/180.;
    private double rOM=0.15;
    private final AtomIteratorArrayListSimple iterator;

    public ConformationWaterTIP4P(Space space) {
        super(space);
        iterator = new AtomIteratorArrayListSimple();
    }
    
    public void initializePositions(AtomArrayList list){
        
        iterator.setList(list);
        double x = 0.0;
        double y = 0.0;
        
        iterator.reset();
        
        AtomLeaf o = (AtomLeaf)iterator.nextAtom();
        o.getPosition().E(new double[] {x, y, 0.0});
               
        AtomLeaf h1 = (AtomLeaf)iterator.nextAtom();
        h1.getPosition().E(new double[] {x+bondLengthOH, y, 0.0});
                
        AtomLeaf h2 = (AtomLeaf)iterator.nextAtom();
        h2.getPosition().E(new double[] {x+bondLengthOH*Math.cos(angleHOH), y+bondLengthOH*Math.sin(angleHOH), 0.0});
        
        AtomLeaf m = (AtomLeaf)iterator.nextAtom();
        m.getPosition().E(new double[] {x+rOM*Math.cos(angleHOH/2.0), y+rOM*Math.sin(angleHOH/2.0), 0.0});

    }
    
    private static final long serialVersionUID = 1L;
}
