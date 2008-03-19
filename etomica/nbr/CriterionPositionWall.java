package etomica.nbr;

import etomica.api.IAtom;
import etomica.api.IAtomLeaf;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.api.ISimulation;
import etomica.atom.AtomAgentManager;
import etomica.atom.AtomSetSinglet;
import etomica.atom.AtomAgentManager.AgentSource;
import etomica.box.BoxAgentManager;
import etomica.box.BoxAgentSourceAtomManager;
import etomica.units.Dimension;
import etomica.units.Length;
import etomica.util.Debug;

/**
 * Simple neighbor criterion based on distance moved by a leaf atom since
 * the last update.  The potential is assumed to be from a wall that exists
 * at the box boundaries or at some fixed position within the box.
 * @author andrew
 */
public class CriterionPositionWall implements NeighborCriterion, AgentSource, java.io.Serializable {

	public CriterionPositionWall(ISimulation sim) {
		super();
		this.interactionRange = Double.NaN;
        this.neighborRange = Double.NaN;
        setBoundaryWall(true);
        setSafetyFactor(0.8);
        boxAgentManager = new BoxAgentManager(new BoxAgentSourceAtomManager(this),sim,true);
	}

    /**
     * Sets the safety factor (between 0 and 1.0) that determines when the 
     * criterion thinks it needs an update.  Safety factors near 1.0 allow
     * atoms to travel farther before needing an update, but are more risky.
     */
	public void setSafetyFactor(double f) {
		if (f <= 0.0 || f >= 1.0) throw new IllegalArgumentException("safety factor must be positive and less than 1.0");
		safetyFactor = f;
        rMaxSafe = (neighborRange - interactionRange);
		displacementLimit = rMaxSafe * safetyFactor;
	}

    /**
     * returns the safety factor
     */
    public double getSafetyFactor() {
        return safetyFactor;
    }

    /**
     * Sets the orientation of the wall to be perpendicular to the given dimension
     */
    public void setWallDim(int d) {
        neighborDim = d;
    }
    
    /**
     * Returns the interaction range of the wall potential.
     */
    public double getInteractionRange() {
        return interactionRange;
    }
	
    /**
     * Informs the criterion of the interaction range of the wall potential.
     */
    public void setInteractionRange(double r) {
        interactionRange = r;
        if (neighborRange == Double.NaN) {
            return;
        }
        rMaxSafe = (neighborRange - interactionRange);
        displacementLimit = rMaxSafe * safetyFactor;
    }        

    public Dimension getInteractionRangeDimension() {
        return Length.DIMENSION;
    }
    
    /**
     * Sets the neighbor range of the criterion.  Atoms within the given 
     * distance of the wall are "accepted".
     */
	public void setNeighborRange(double r) {
		if (interactionRange != Double.NaN && r < interactionRange) throw new IllegalArgumentException("Neighbor radius must be larger than interaction range");
		neighborRange = r;
        if (interactionRange == Double.NaN) {
            return;
        }
        rMaxSafe = (neighborRange - interactionRange);
        displacementLimit = rMaxSafe * safetyFactor;
	}
    
    public double getNeighborRange() {
        return neighborRange;
    }
    
    public Dimension getNeighborRangeDimension() {
        return Length.DIMENSION;
    }
    
	/**
     * Returns true if the walls are at the box boundaries.
     */
    public boolean isBoundaryWall() {
        return isBoundaryWall;
    }

    /**
     * Sets whether the walls are at the box boundaries or not.
     */
    public void setBoundaryWall(boolean isBoundaryWall) {
        this.isBoundaryWall = isBoundaryWall;
    }

    /**
     * Sets the position of the wall.  This parameter is ignored if 
     * isBoundaryWall is true.
     */
    public void setWallPosition(double p) {
        wallPosition = p;
    }
    
    /**
     * Returns the position of the wall.  This parameter is ignored if
     * isBoundaryWall is true.
     */
    public double getWallPosition() {
        return wallPosition;
    }
    
    public Dimension getWallPositionDimension() {
        return Length.DIMENSION;
    }

	public boolean needUpdate(IAtom atom) {
        dr = Math.abs(((IAtomPositioned)atom).getPosition().x(neighborDim) - ((DoubleWrapper)agentManager.getAgent(atom)).x);
        if (Debug.ON && Debug.DEBUG_NOW && Debug.LEVEL > 1 && Debug.allAtoms(new AtomSetSinglet(atom))) {
            System.out.println("atom "+atom+" displacement "+dr+" "+((IAtomPositioned)atom).getPosition());
        }
		if (Debug.ON && Debug.DEBUG_NOW && dr > rMaxSafe) {
			System.out.println("atom "+atom+" exceeded safe limit ("+dr+" > "+rMaxSafe+")");
			System.out.println("old position "+((DoubleWrapper)agentManager.getAgent(atom)).x);
			System.out.println("new position "+((IAtomPositioned)atom).getPosition().x(neighborDim));
            System.err.println("stop that");
		}
		return dr > displacementLimit;
	}

	public void setBox(IBox box) {
        boxSize = box.getBoundary().getDimensions().x(neighborDim);
        agentManager = (AtomAgentManager)boxAgentManager.getAgent(box);
	}
    
	public boolean unsafe() {
		if (Debug.ON && Debug.DEBUG_NOW && dr > rMaxSafe) {
			System.out.println("some atom exceeded safe limit ("+dr+" > "+rMaxSafe+")");
		}
		return dr > rMaxSafe;
	}

	public boolean accept(IAtomSet atom) {
		dr = ((IAtomPositioned)atom.getAtom(0)).getPosition().x(neighborDim);
        if (!isBoundaryWall) {
            dr = Math.abs(dr - wallPosition);
        }
        else {
            if (dr > 0.0) {
                dr = 0.5*boxSize - dr;
            }
            else {
                dr = dr + 0.5*boxSize;
            }
        }
		if (Debug.ON && Debug.DEBUG_NOW && (Debug.LEVEL > 0 && Debug.allAtoms(atom))) {
			if (dr < neighborRange || Debug.LEVEL > 1) {
				System.out.println("Atom "+atom+" is "+(dr < neighborRange ? "" : "not ")+"interacting, dr="+dr);
            }
		}
		return dr < neighborRange;
	}
	
	public void reset(IAtom atom) {
		((DoubleWrapper)agentManager.getAgent(atom)).x = ((IAtomPositioned)atom).getPosition().x(neighborDim);
	}

    public Class getAgentClass() {
        return DoubleWrapper.class;
    }
    
    public Object makeAgent(IAtom atom) {
        return atom instanceof IAtomPositioned ? new DoubleWrapper() : null;
    }
    
    public void releaseAgent(Object agent, IAtom atom) {}

    protected static class DoubleWrapper implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public double x;
    }
    
    private static final long serialVersionUID = 1L;
    private double interactionRange, displacementLimit, neighborRange;
    private int neighborDim;
    private boolean isBoundaryWall;
    private double wallPosition;
    private double boxSize;
	protected double safetyFactor;
	protected double dr, rMaxSafe;
    protected AtomAgentManager agentManager;
    private final BoxAgentManager boxAgentManager;
}
