package etomica.util;

import etomica.api.IAtom;
import etomica.api.IAtomLeaf;
import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.atom.AtomPair;

/**
 * Class holding static fields that determine whether debugging is on, how
 * much, what types and what (if anything) should be looked at specifically.
 * @author andrew
 */

public final class Debug {

    protected static AtomPair debugPair = null;
    
	/**
	 * true if any debugging should be done
	 */
	public static final boolean ON = false;
        
	/**
	 * what step of the integrator debugging should start from.
	 * to get debugging to start before Integrator.run, explicitly
	 * set DEBUG_NOW to true. 
	 */
	public static int START = 0;
	
	/**
	 * what step of the integrator should the simulation bail
	 * set this to prevent Etomica from running for a long time while 
	 * outputting debugging information (potentially filling up the disk)  
	 */
	public static int STOP = -1;
	
	/**
	 * debugging level.  A higher level means more debugging
	 * performance should suffer as the level goes up, and
	 * finding useful information might get difficult.
	 */
	public static int LEVEL = 1;
	
    /**
     * true if debugging is currently enabled (when the integrator reaches step START) 
     */
    public static boolean DEBUG_NOW = false;

	/**
	 * Global index of first atom of interest.  More debugging information will be
	 * printed out about this particular atom.  -1 indicates no particular atom.
	 */
	public static final int ATOM1_INDEX = -1;

	/**
	 * Global index of second atom of interest.  This is often used in conjunction with 
	 * ATOM1_INDEX to collect information about a pair of atoms.  -1 indicates no
	 * particular atom.  
	 */
	public static final int ATOM2_INDEX = -1;

    /**
     * Global index of a molecule of interest.  More debugging information will be
     * printed out about atoms within this molecule.  -1 indicates no
     * particular atom.  
     */
    public static final int MOLECULE1_INDEX = -1;

    /**
     * Global index of a molecule of interest.  This is often used in conjunction with 
     * MOLECULE2_INDEX to collect information about a pair of molecules.  -1 indicates no
     * particular atom.  
     */
    public static final int MOLECULE2_INDEX = -1;

    public static final int SPECIES1_INDEX = 1;

    public static final int SPECIES2_INDEX = 1;

    /**
     * index of box of interest.  -1 indicates no particular box.
     */
    public static final int BOX_INDEX = 0;

    /**
     * The minimum allowable distance between Atoms.
     */
    public static final double ATOM_SIZE = 1.0;
    
	/**
	 * determines whether any of the atoms in the given array are set to be debugged
	 * (via ATOMx_NUM)
	 * @param atoms array of atoms to be checked for debugging status
	 * @return true if any of the atoms in the atoms array should be debugged
	 */
	public static boolean anyAtom(IAtomSet atoms) {
		for (int i=0; i<atoms.getAtomCount(); i++) {
		    if (atoms.getAtom(i) instanceof IAtomLeaf) {
		        IAtomLeaf atom = (IAtomLeaf)atoms.getAtom(i);
		        if ((atom.getIndex() == ATOM1_INDEX || ATOM1_INDEX == -1) &&
		            atom.getParentGroup().getIndex() == MOLECULE1_INDEX &&
		            atom.getParentGroup().getType().getIndex() == SPECIES1_INDEX) {
		            return true;
		        }
                if ((atom.getIndex() == ATOM2_INDEX || ATOM1_INDEX == -1) &&
                    atom.getParentGroup().getIndex() == MOLECULE2_INDEX &&
                    atom.getParentGroup().getType().getIndex() == SPECIES2_INDEX) {
                    return true;
                }
		    }
		    else {
		        IMolecule molecule = (IMolecule)atoms.getAtom(i);
                if (molecule.getIndex() == MOLECULE1_INDEX &&
                    molecule.getType().getIndex() == SPECIES1_INDEX) {
                    return true;
                }
                if (molecule.getIndex() == MOLECULE2_INDEX &&
                    molecule.getType().getIndex() == SPECIES2_INDEX) {
                    return true;
                }
		    }
		}
		return false;
	}

	/**
	 * determines if all of the atoms in the given array are set to be debugged
	 * (via ATOMx_NUM and setAtoms(box)).
	 * @param atoms array of atoms to be checked for debugging status
	 * @return true if all of the atoms in the atoms array should be debugged
	 */
	public static boolean allAtoms(IAtomSet atoms) {
        for (int i=0; i<atoms.getAtomCount(); i++) {
            boolean success = false;
            if (atoms.getAtom(i) instanceof IAtomLeaf) {
                IAtomLeaf atom = (IAtomLeaf)atoms.getAtom(i);
                if ((atom.getIndex() == ATOM1_INDEX || ATOM1_INDEX == -1) &&
                    atom.getParentGroup().getIndex() == MOLECULE1_INDEX &&
                    atom.getParentGroup().getType().getIndex() == SPECIES1_INDEX) {
                    success = true;
                }
                if ((atom.getIndex() == ATOM2_INDEX || ATOM1_INDEX == -1) &&
                    atom.getParentGroup().getIndex() == MOLECULE2_INDEX &&
                    atom.getParentGroup().getType().getIndex() == SPECIES2_INDEX) {
                    success = true;
                }
            }
            else {
                IMolecule molecule = (IMolecule)atoms.getAtom(i);
                if (molecule.getIndex() == MOLECULE1_INDEX &&
                    molecule.getType().getIndex() == SPECIES1_INDEX) {
                    success = true;
                }
                if (molecule.getIndex() == MOLECULE2_INDEX &&
                    molecule.getType().getIndex() == SPECIES2_INDEX) {
                    success = true;
                }
            }
            if (!success) {
                return false;
            }
        }
		return true;
	}

    /**
     * Checks whether the given box is of debugging interest
     * @param box to be checked
     * @return true if the box is of interest
     */
    public static boolean thisBox(IBox box) {
         return box.getIndex() == BOX_INDEX;
    }
    
    /**
     * Returns an AtomPair containing the two atoms with global indices
     * ATOM1_INDEX and ATOM2_INDEX within the given box.  The atom in
     * the AtomPair will be null if the box does not contain an Atom 
     * with the proper global index.
     */
    public static AtomPair getAtoms(IBox box) {
        if (debugPair == null) {
            debugPair = new AtomPair();
        }
        if (ATOM1_INDEX > -1 || ATOM2_INDEX > -1 && MOLECULE1_INDEX > -1 && MOLECULE2_INDEX > -1) {
            IAtomSet moleculeList = box.getMoleculeList();
            for (int i=0; i<moleculeList.getAtomCount(); i++) {
                IMolecule molecule = (IMolecule)moleculeList.getAtom(i);
                if (molecule.getIndex() == MOLECULE1_INDEX && molecule.getType().getIndex() == SPECIES1_INDEX) {
                    debugPair.atom0 = molecule.getChildList().getAtom(ATOM1_INDEX);
                }
                else if (molecule.getIndex() == MOLECULE2_INDEX && molecule.getType().getIndex() == SPECIES2_INDEX) {
                    debugPair.atom1 = molecule.getChildList().getAtom(ATOM2_INDEX);
                }
            }
        }
        if (debugPair.atom0 == null || debugPair.atom1 == null) return null;
        return debugPair;
    }
    
    /**
     * Returns an AtomPair containing the two atoms with global indices
     * ATOM1_INDEX and ATOM2_INDEX within the given box.  The atom in
     * the AtomPair will be null if the box does not contain an Atom 
     * with the proper global index.
     */
    public static IAtom getAtomLeaf1(IBox box) {
        if (ATOM1_INDEX > -1 && ATOM1_INDEX < box.getLeafList().getAtomCount()) {
            return box.getLeafList().getAtom(ATOM1_INDEX);
        }
        return null;
    }

}
