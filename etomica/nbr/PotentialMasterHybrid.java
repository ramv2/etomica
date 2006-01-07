/*
 * History
 * Created on Sep 20, 2004 by kofke
 */
package etomica.nbr;

import etomica.atom.AtomPositionDefinition;
import etomica.atom.iterator.IteratorDirective;
import etomica.nbr.cell.NeighborCellManager;
import etomica.nbr.cell.PotentialMasterCell;
import etomica.nbr.list.NeighborListManager;
import etomica.nbr.list.PotentialMasterList;
import etomica.phase.Phase;
import etomica.potential.Potential;
import etomica.potential.PotentialCalculation;
import etomica.space.Space;
import etomica.species.Species;

/**
 * PotentialMaster that uses both neighbor-cell iteration and cell-list 
 * iteration.  This is needed by simulations that employ both Monte Carlo
 * and molecular dynamics integration steps, alternately as the simulation
 * proceeds.  See DCVGCMD simulation module for an example.
 * <br>
 */
public class PotentialMasterHybrid extends PotentialMasterNbr {

	/**
	 * Invokes superclass constructor, specifying IteratorFactoryCell
     * for generating molecule iterators.  Sets default nCells of 10,
     * and position definition to null, causing cell assignment to be
     * based on atom type's position definition. 
	 */
	public PotentialMasterHybrid(Space space, double range) {
        this(space, null, range);
    }
    
    /**
     * Constructs class using given position definition for all atom cell assignments.
     * @param positionDefinition if null, specifies use of atom type's position definition
     */
    public PotentialMasterHybrid(Space space, AtomPositionDefinition positionDefinition, double range) {
        super(space);
        potentialMasterNbr = new PotentialMasterList(space, range, positionDefinition);
        potentialMasterCell = new PotentialMasterCell(space, range, positionDefinition);
	}

    /**
     * Forward updateTypeList to the PotentialMasterCell.  PotentialMasterNbr 
     * needs this too, but gets it on its own from NeighborManager.
     */
    public void updateTypeList(Phase phase) {
        potentialMasterCell.updateTypeList(phase);
    }
    
    /**
     * Overrides superclass method to enable direct neighbor-list iteration
     * instead of iteration via species/potential hierarchy. If no target atoms are
     * specified in directive, neighborlist iteration is begun with
     * speciesMaster of phase, and repeated recursively down species hierarchy;
     * if one atom is specified, neighborlist iteration is performed on it and
     * down species hierarchy from it; if two or more atoms are specified,
     * superclass method is invoked.
     */
    public void calculate(Phase phase, IteratorDirective id, PotentialCalculation pc) {
		if(!enabled) return;
        if (useNbrLists) potentialMasterNbr.calculate(phase,id,pc);
        else potentialMasterCell.calculate(phase,id,pc);
    }
    
    public double getCellRange() {
        return potentialMasterCell.getRange();
    }
    
    public void setCellRange(int newRange) {
        potentialMasterNbr.setCellRange(newRange);
        potentialMasterCell.setCellRange(newRange);
    }
    
    public double getRange() {
        return potentialMasterCell.getRange();
    }

    public void setRange(double newRange) {
        potentialMasterNbr.setRange(newRange);
        potentialMasterCell.setRange(newRange);
    }

    public NeighborCellManager getNbrCellManager(Phase phase) {
        return potentialMasterNbr.getNbrCellManager(phase);
    }
    
    public AtomPositionDefinition getAtomPositionDefinition() {
        return potentialMasterNbr.getAtomPositionDefinition();
    }

    public void setUseNbrLists(boolean flag) {
        useNbrLists = flag;
    }
    
    /* (non-Javadoc)
     * @see etomica.PotentialMaster#setSpecies(etomica.Potential, etomica.Species[])
     */
    public void addPotential(Potential potential, Species[] species) {
        potentialMasterNbr.addPotential(potential, species);
        potentialMasterCell.addPotential(potential, species);
    }
    
    public NeighborListManager getNeighborManager() {
        return potentialMasterNbr.getNeighborManager();
    }

    private boolean useNbrLists;
    private final PotentialMasterList potentialMasterNbr;
    private final PotentialMasterCell potentialMasterCell;
}
