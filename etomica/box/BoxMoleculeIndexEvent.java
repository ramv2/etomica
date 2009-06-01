package etomica.box;

import etomica.api.IBox;
import etomica.api.IBoxMoleculeIndexEvent;
import etomica.api.IMolecule;


/**
 * Event that conveys that an Atom's global index in a box has changed.
 */
public class BoxMoleculeIndexEvent extends BoxMoleculeEvent implements IBoxMoleculeIndexEvent {

    public BoxMoleculeIndexEvent(IBox box, IMolecule mole, int _index) {
        super(box, mole);
        this.index = _index;
    }
    
    public int getIndex() {
        return index;
    }
    
    protected int index = -1;
    private static final long serialVersionUID = 1L;
}
