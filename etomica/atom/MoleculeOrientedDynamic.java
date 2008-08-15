package etomica.atom;

import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.space.ISpace;

/**
 * Molecule class appropriate for a rigid molecule in a dynamic context.  The
 * molecule object holds a position, velocity orientation and angular momentum
 * as fields.
 *
 * @author Andrew Schultz
 */
public class MoleculeOrientedDynamic extends MoleculeOriented implements IAtomOrientedKinetic {

    public MoleculeOrientedDynamic(ISpace space, ISpecies species) {
        super(space, species);
        angularMomentum = space.makeVector();
        velocity = space.makeVector();
    }

    public IVector getAngularVelocity() {
        return angularMomentum;
    }

    public IVector getVelocity() {
        return velocity;
    }

    private static final long serialVersionUID = 1L;
    protected final IVector angularMomentum;
    protected final IVector velocity;
}
