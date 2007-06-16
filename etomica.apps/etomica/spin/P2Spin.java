package etomica.spin;

import etomica.atom.AtomSet;
import etomica.atom.IAtomPositioned;
import etomica.phase.Phase;
import etomica.potential.Potential2;
import etomica.space.Space;

/**
 * Magnetic spin potential, with an energy defined by
 * 
 * U = -J r1 dot r2
 * 
 * where J is a coupling parameter, and r1 and r2 are the vectors given by
 * atom.coord.position. It is expected (but not verified here) that these
 * vectors are normalized to unity, and that the simulation integrator's
 * algorithm enforces this constraint.
 * 
 * @author David Kofke
 *  
 */
public class P2Spin extends Potential2 {

    public P2Spin(Space space) {
        this(space, 1.0);
    }

    public P2Spin(Space space, double coupling) {
        super(space);
        setCoupling(coupling);
    }

    /**
     * Returns the energy for the given pair of atoms.
     * 
     * @throws ClassCastException
     *             if atoms is not an instance of AtomPair
     */
    public double energy(AtomSet atoms) {
        return -coupling
                * ((IAtomPositioned)atoms.getAtom(0)).getPosition().dot(((IAtomPositioned)atoms.getAtom(1)).getPosition());
    }

    /**
     * Returns 0, becuase potential operates on a lattice and range
     * should not be needed.  The PotentialMasterSite expects all Potentials
     * to have a range and uses the return value to determine whether or not
     * to use site iteration.
     */
    public double getRange() {
        return 0;
    }

    /**
     * @return Returns the coupling.
     */
    public double getCoupling() {
        return coupling;
    }

    /**
     * @param coupling
     *            The coupling to set.
     */
    public void setCoupling(double coupling) {
        this.coupling = coupling;
    }

    public void setPhase(Phase phase) {
        //does nothing
    }

    private static final long serialVersionUID = 1L;
    private double coupling;
}
