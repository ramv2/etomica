package etomica.atom;

import etomica.config.Conformation;
import etomica.space.CoordinateFactory;
import etomica.species.Species;

/**
 * Class responsible for building new instances of the atoms (or atom groups)
 * that are collected in a given AtomGroup.
 *
 * @author David Kofke and Andrew Schultz
 */
 
public abstract class AtomFactory implements java.io.Serializable {
    
    protected Conformation conformation;
    protected AtomTreeNodeFactory nodeFactory;
    protected final AtomType atomType;
    protected final CoordinateFactory coordFactory;
    
    /**
     * Makes an atom factory with atoms having AtomTreeNodeGroup for node.
     */
    public AtomFactory(CoordinateFactory coordFactory, AtomType atomType) {
        this(coordFactory, atomType, AtomTreeNodeGroup.FACTORY);
    }
    
    public AtomFactory(CoordinateFactory coordFactory, AtomType atomType, AtomTreeNodeFactory nodeFactory) {
        this.coordFactory = coordFactory;
        this.nodeFactory = nodeFactory;
        this.atomType = atomType;
        atomType.creator = this;
    }
    
    /**
     * Builds and returns the atom/atomgroup made by this factory.
     * Implementation of this method in the subclass defines the 
     * product of this factory.
     */
    public abstract Atom makeAtom();

    /**
     * Identifies the species for which this factory makes its atoms.
     * Should be invoked only in the species constructor, and by any
     * an atom factory on its child factories.
     */
    public abstract void setSpecies(Species species);
    
    /**
     * Method used by subclasses to make the root atom of the group it is building.
     */
    protected Atom newParentAtom() {
        Atom atom = new Atom(coordFactory.makeCoordinate(), atomType, nodeFactory);
        return atom;
    }

    /**
     * Returns the atomType instance given to all atoms made by this factory.
     */
    public AtomType getType() {
        return atomType;
    }
    
    /**
     * Returns the CoordinateFactory that gives coordinates to the
     * atom (or the root atom, if this makes an atom group) made by this
     * AtomFactory
     */
    public CoordinateFactory getCoordinateFactory() {
        return coordFactory;
    }

    /**
     * Sets the conformation used to set the standard arrangement of
     * the atoms/atom-groups produced by this factory.
     */
    public void setConformation(Conformation config) {
        conformation = config;
    }
    
    /**
     * Returns the conformation used to set the standard arrangement of
     * the atoms/atom-groups produced by this factory.
     */
    public Conformation getConformation() {return conformation;}
    
}//end of AtomFactory
