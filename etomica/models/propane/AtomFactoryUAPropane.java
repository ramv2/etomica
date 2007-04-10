package etomica.models.propane;

import etomica.atom.Atom;
import etomica.atom.AtomFactory;
import etomica.atom.AtomFactoryMono;
import etomica.atom.AtomFactoryMonoDynamic;
import etomica.atom.AtomLeaf;
import etomica.atom.AtomPositionGeometricCenter;
import etomica.atom.AtomTypeGroup;
import etomica.atom.AtomTypeSphere;
import etomica.chem.elements.ElementSimple;
import etomica.simulation.Simulation;

/**
 * Factory that constructs a 3-point water molecule, with three child atoms of 
 * two Hydrogen and one Oxygen.
 * @author kofke
 *
 */
public class AtomFactoryUAPropane extends AtomFactory {

	/**
	 * Constructor for AtomFactoryWater.
	 * @param sim
	 * @param sequencerFactory
	 */
    public AtomFactoryUAPropane(Simulation sim) {
		super(new AtomTypeGroup(new AtomPositionGeometricCenter(sim.getSpace())));
        AtomTypeSphere UAType = new AtomTypeSphere(new ElementSimple("UA", 15), 3.75);
        UAType.setParentType((AtomTypeGroup)atomType);
        UAFactory = sim.isDynamic() ? new AtomFactoryMonoDynamic(sim.getSpace(), UAType) : 
                                      new AtomFactoryMono(sim.getSpace(), UAType);

		conformation = new ConformationUAPropane(sim.getSpace()); 
	}

	/**
	 * @see etomica.atom.AtomFactory#build(etomica.Atom)
	 */
	public Atom makeAtom() {
        AtomUAPropane propane = new AtomUAPropane(atomType);
        propane.UA1 = (AtomLeaf)UAFactory.makeAtom();
        propane.UA2 = (AtomLeaf)UAFactory.makeAtom();
        propane.UA1.setParent(propane);
        propane.UA2.setParent(propane);
		conformation.initializePositions(propane.getChildList());
		return propane;
	}
    
	public final AtomFactoryMono UAFactory;

	/* (non-Javadoc)
	 * @see etomica.atom.AtomFactory#getNumTreeAtoms()
	 */
	public int getNumTreeAtoms() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see etomica.atom.AtomFactory#getNumChildAtoms()
	 */
	public int getNumChildAtoms() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see etomica.atom.AtomFactory#getNumLeafAtoms()
	 */
	public int getNumLeafAtoms() {
		// TODO Auto-generated method stub
		return 0;
	}

    private static final long serialVersionUID = 1L;
}
