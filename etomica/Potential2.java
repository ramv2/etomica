package etomica; 

/**
 * Potential acting on or within an atom, or between a pair of atoms or atom
 * groups. Contains other Potential instances that describe the specific
 * interactions between the atoms of the group(s).
 *
 * @author David Kofke
 */

 /* History of changes
  * 07/13/02 (DAK) Restructured instantiation of LRC potential
  * 07/15/02 (DAK) Constructor makes P0LRC only if instance of Potential2SoftSpherical
  * 12/06/02 (DAK) Added setIterators1A method
  * 01/27/03 (DAK) Numerous changes with redesign of Potential.
  * 07/17/03 (DAK) Made calculate method not final (so etomica.virial.P2Cluster
  * could override it)
  */

public abstract class Potential2 extends Potential {
  
    protected final Space.CoordinatePair cPair, cPairNbr;
    
    public Potential2(Space space) {
        this(space, Default.TRUNCATE_POTENTIALS ? 
                        new PotentialTruncationSimple()
                      : PotentialTruncation.NULL);
      /*                  
        super(parent);
        iterator1 = new ApiIntergroup1A(parentSimulation());
        iteratorA = new ApiIntergroupAA(parentSimulation());
        if(Default.TRUNCATE_POTENTIALS) {//can't use other constructor because of "this" in constructor of PotentialTruncationSimple
            potentialTruncation = new PotentialTruncationSimple(parentSimulation().space, Default.POTENTIAL_CUTOFF_FACTOR * Default.ATOM_SIZE);
            Potential0GroupLrc lrcMaster = parentSimulation().hamiltonian.potential.lrcMaster();
            potentialTruncation.makeLrcPotential(lrcMaster, this); //adds this to lrcMaster
        } else {
            potentialTruncation = PotentialTruncation.NULL;
        }*/
    }
    public Potential2(Space space, PotentialTruncation potentialTruncation) {
        super(2, space, potentialTruncation);
        if( (potentialTruncation != PotentialTruncation.NULL) && (potentialTruncation != null
            && (this instanceof Potential2SoftSpherical)) ) {
        	//TODO fix by doing this when adding potential to parent group
//            PotentialMaster potentialMaster = simulation().potentialMaster;
//            potentialTruncation.makeLrcPotential(potentialMaster, this); //constructor of lrcPotential adds it to lrcMaster of potentialMaster
        }
        cPair = space.makeCoordinatePair();
        cPairNbr = space.makeCoordinatePair();
    }

    public void setPhase(Phase phase) {
        cPair.setNearestImageTransformer(phase.boundary());
        cPairNbr.setNearestImageTransformer(phase.boundary());
    }
    
    public void setNearestImageTransformer(NearestImageTransformer nearestImageTransformer) {
        cPairNbr.setNearestImageTransformer(nearestImageTransformer);
    }
    
	/**
	 * Methods for properties obtained for a soft, differentiable pair potential.
	 *
	 * @author David Kofke
	 */
    public interface Soft extends PotentialSoft {
		public double hyperVirial(Atom[] pair);

		public double virial(Atom[] pair);
	    
		/**
		 * Integral used to evaluate correction to truncation of potential.
		 */
		public abstract double integral(double rC);
    }
    
}//end of Potential2



