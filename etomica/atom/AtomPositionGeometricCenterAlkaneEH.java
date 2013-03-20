package etomica.atom;

import java.io.Serializable;

import etomica.api.IAtomList;
import etomica.api.IMolecule;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.space.ISpace;

/**
 * Calculates the geometric center over a set of atoms. The position of the
 * atom or child atoms are accumulated and used to compute their
 * center (unweighted by mass). Calculated center is obtained via the getPosition
 * method.
 * This class is for normal alkane, TraPPE-Explicit Hydrogen only
 * Only carbons are employed to calculate the geometric center
 *
 * @author shu
 * March 2013
 */
public class AtomPositionGeometricCenterAlkaneEH implements IAtomPositionDefinition, Serializable {

    public AtomPositionGeometricCenterAlkaneEH(ISpace space) {
        center = space.makeVector();
    }

    public IVector position(IMolecule atom) {
        center.E(0.0);
        IAtomList children = atom.getChildList();
        int nAtoms = children.getAtomCount();
        int numCarbons = (nAtoms-2)/3;
        for (int i=0; i<numCarbons; i++) {// loop over all carbons ONLY
            center.PE(children.getAtom(i).getPosition());
        }
        center.TE(1.0 / numCarbons);
        return center;
    }

    private static final long serialVersionUID = 1L;
    private final IVectorMutable center;
}
