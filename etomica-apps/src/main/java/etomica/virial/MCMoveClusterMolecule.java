/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.virial;

import etomica.api.IRandom;
import etomica.api.ISimulation;
import etomica.integrator.mcmove.MCMoveMolecule;
import etomica.space.ISpace;

/**
 * Standard Monte Carlo molecule-displacement trial move for cluster integrals.
 */
public class MCMoveClusterMolecule extends MCMoveMolecule {
    
    private static final long serialVersionUID = 1L;

    public MCMoveClusterMolecule(ISimulation sim, ISpace _space) {
    	this(sim.getRandom(), _space, 1.0);
    }
    
    public MCMoveClusterMolecule(IRandom random, ISpace _space, double stepSize) {
        super(null, random,_space, stepSize, Double.POSITIVE_INFINITY);
    }
    
    public boolean doTrial() {
        if(box.getMoleculeList().getMoleculeCount()==1) return false;
        
        molecule = moleculeSource.getMolecule();
        while (molecule.getIndex() == 0) {
            molecule = moleculeSource.getMolecule();
        }
        
        uOld = ((BoxCluster)box).getSampleCluster().value((BoxCluster)box);
        groupTranslationVector.setRandomCube(random);
        groupTranslationVector.TE(stepSize);
        moveMoleculeAction.actionPerformed(molecule);
        ((BoxCluster)box).trialNotify();
        uNew = ((BoxCluster)box).getSampleCluster().value((BoxCluster)box);
        return true;
    }
    
    public double getB() {return 0.0;}
    
    public double getA() {
        return (uOld==0.0) ? Double.POSITIVE_INFINITY : uNew/uOld;
    }
    
    public void acceptNotify() {
        super.acceptNotify();
        ((BoxCluster)box).acceptNotify();
        System.out.println("acceptNotify");
//        System.out.println(atom+" accepted => "+atom.type.getPositionDefinition().position(atom));
    }
    
    public void rejectNotify() {
        super.rejectNotify();
        ((BoxCluster)box).rejectNotify();
        System.out.println("rejectNotify");
        //        System.out.println(atom+" rejected => "+atom.type.getPositionDefinition().position(atom));
    }
        
}