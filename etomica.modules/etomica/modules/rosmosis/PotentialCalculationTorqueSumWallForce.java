package etomica.modules.rosmosis;

import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IMolecule;
import etomica.api.IPotential;
import etomica.api.IVector;
import etomica.atom.AtomAgentManager;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.MoleculeAgentManager;
import etomica.integrator.IntegratorBox;
import etomica.potential.IPotentialTorque;
import etomica.potential.PotentialCalculationTorqueSum;
import etomica.potential.PotentialSoft;

/**
 * Sums the force and torque on each iterated atom or molecule and adds it to
 * the agent associated with the atom.
 */
public class PotentialCalculationTorqueSumWallForce extends PotentialCalculationTorqueSum {
        
    private static final long serialVersionUID = 1L;
    protected AtomAgentManager integratorAgentManager;
    protected AtomAgentManager.AgentIterator agentIterator;
    
    public PotentialCalculationTorqueSumWallForce(IPotential potentialTether) {
        this.potentialTether = potentialTether;
    }
    
    public void setAgentManager(AtomLeafAgentManager agentManager) {
        leafAgentManager = agentManager;
        leafAgentIterator = leafAgentManager.makeIterator();
    }
    
    public void setMoleculeAgentManager(MoleculeAgentManager newMoleculeAgentManager) {
        moleculeAgentManager = newMoleculeAgentManager;
        moleculeAgentIterator = moleculeAgentManager.makeIterator();
    }
    
    /**
     * Re-zeros the force vectors.
     *
     */
    public void reset(){
        wallForce = 0;
        
        leafAgentIterator.reset();
        while(leafAgentIterator.hasNext()){
            Object agent = leafAgentIterator.next();
            if (agent instanceof IntegratorBox.Torquable) {
                ((IntegratorBox.Torquable)agent).torque().E(0);
                ((IntegratorBox.Forcible)agent).force().E(0);
            }
            else if (agent instanceof IntegratorBox.Forcible) {
                ((IntegratorBox.Forcible)agent).force().E(0);
            }
        }
        moleculeAgentIterator.reset();
        while(moleculeAgentIterator.hasNext()){
            Object agent = moleculeAgentIterator.next();
            if (agent instanceof IntegratorBox.Torquable) {
                ((IntegratorBox.Torquable)agent).torque().E(0);
                ((IntegratorBox.Forcible)agent).force().E(0);
            }
            else if (agent instanceof IntegratorBox.Forcible) {
                ((IntegratorBox.Forcible)agent).force().E(0);
            }
        }

    }
    
    public double getWallForce() {
        return 0.5*wallForce;
    }
    
    /**
     * Adds forces and torques due to given potential acting on the atoms produced by the iterator.
     * Implemented for 1-, 2- and N-body potentials.
     */
    public void doCalculation(IAtomSet atoms, IPotential potential) {
        int nBody = potential.nBody();
        if (atoms.getAtom(0) instanceof IMolecule) {
            if (potential instanceof IPotentialTorque) {
                // IPotentialTorque will give us gradient and torque in one call
                IPotentialTorque potentialSoft = (IPotentialTorque)potential;
                IVector[][] gt = potentialSoft.gradientAndTorque(atoms);
                IVector[] g = gt[0];
                IVector[] t = gt[1];
                switch(nBody) {
                    case 1:
                        ((IntegratorBox.Torquable)moleculeAgentManager.getAgent((IMolecule)atoms.getAtom(0))).torque().PE(t[0]);
                        ((IntegratorBox.Forcible)moleculeAgentManager.getAgent((IMolecule)atoms.getAtom(0))).force().ME(g[0]);
                        break;
                    case 2:
                        ((IntegratorBox.Torquable)moleculeAgentManager.getAgent((IMolecule)atoms.getAtom(0))).torque().PE(t[0]);
                        ((IntegratorBox.Torquable)moleculeAgentManager.getAgent((IMolecule)atoms.getAtom(1))).torque().PE(t[1]);
                        ((IntegratorBox.Forcible)moleculeAgentManager.getAgent((IMolecule)atoms.getAtom(0))).force().ME(g[0]);
                        ((IntegratorBox.Forcible)moleculeAgentManager.getAgent((IMolecule)atoms.getAtom(1))).force().ME(g[1]);
                        break;
                    default:
                        //XXX atoms.count might not equal f.length.  The potential might size its 
                        //array of vectors to be large enough for one AtomSet and then not resize it
                        //back down for another AtomSet with fewer atoms.
                        for (int i=0; i<atoms.getAtomCount(); i++) {
                            ((IntegratorBox.Torquable)moleculeAgentManager.getAgent((IMolecule)atoms.getAtom(i))).torque().PE(t[i]);
                            ((IntegratorBox.Forcible)moleculeAgentManager.getAgent((IMolecule)atoms.getAtom(i))).force().ME(g[i]);
                        }
                }
            }
            else if (potential instanceof PotentialSoft) {
                // we can only get the gradient, but we're probably just dealing with a set of (leaf) Atoms.
                PotentialSoft potentialSoft = (PotentialSoft)potential;
                IVector[] gradient = potentialSoft.gradient(atoms);
                switch(nBody) {
                    case 1:
                        ((IntegratorBox.Forcible)leafAgentManager.getAgent(atoms.getAtom(0))).force().ME(gradient[0]);
                        break;
                    case 2:
                        ((IntegratorBox.Forcible)leafAgentManager.getAgent(atoms.getAtom(0))).force().ME(gradient[0]);
                        ((IntegratorBox.Forcible)leafAgentManager.getAgent(atoms.getAtom(1))).force().ME(gradient[1]);
                        break;
                    default:
                        //XXX atoms.count might not equal f.length.  The potential might size its 
                        //array of vectors to be large enough for one AtomSet and then not resize it
                        //back down for another AtomSet with fewer atoms.
                        for (int i=0; i<atoms.getAtomCount(); i++) {
                            ((IntegratorBox.Forcible)leafAgentManager.getAgent(atoms.getAtom(i))).force().ME(gradient[i]);
                        }
                }
            }
        }
        else {
            if (potential instanceof IPotentialTorque) {
                // IPotentialTorque will give us gradient and torque in one call
                IPotentialTorque potentialSoft = (IPotentialTorque)potential;
                IVector[][] gt = potentialSoft.gradientAndTorque(atoms);
                IVector[] g = gt[0];
                IVector[] t = gt[1];
                switch(nBody) {
                    case 1:
                        ((IntegratorBox.Torquable)leafAgentManager.getAgent(atoms.getAtom(0))).torque().PE(t[0]);
                        ((IntegratorBox.Forcible)leafAgentManager.getAgent(atoms.getAtom(0))).force().ME(g[0]);
                        break;
                    case 2:
                        ((IntegratorBox.Torquable)leafAgentManager.getAgent(atoms.getAtom(0))).torque().PE(t[0]);
                        ((IntegratorBox.Torquable)leafAgentManager.getAgent(atoms.getAtom(1))).torque().PE(t[1]);
                        ((IntegratorBox.Forcible)leafAgentManager.getAgent(atoms.getAtom(0))).force().ME(g[0]);
                        ((IntegratorBox.Forcible)leafAgentManager.getAgent(atoms.getAtom(1))).force().ME(g[1]);
                        break;
                    default:
                        //XXX atoms.count might not equal f.length.  The potential might size its 
                        //array of vectors to be large enough for one AtomSet and then not resize it
                        //back down for another AtomSet with fewer atoms.
                        for (int i=0; i<atoms.getAtomCount(); i++) {
                            ((IntegratorBox.Torquable)leafAgentManager.getAgent(atoms.getAtom(i))).torque().PE(t[i]);
                            ((IntegratorBox.Forcible)leafAgentManager.getAgent(atoms.getAtom(i))).force().ME(g[i]);
                        }
                }
            }
            else if (potential instanceof PotentialSoft) {
                // we can only get the gradient, but we're probably just dealing with a set of (leaf) Atoms.
                PotentialSoft potentialSoft = (PotentialSoft)potential;
                IVector[] gradient = potentialSoft.gradient(atoms);
                switch(nBody) {
                    case 1:
                        if (potential == potentialTether) {
                            if (((IAtomPositioned)atoms.getAtom(0)).getPosition().x(0) > 0) {
                                wallForce += gradient[0].x(0);
                            }
                            else {
                                wallForce -= gradient[0].x(0);
                            }
                        }
                        ((IntegratorBox.Forcible)leafAgentManager.getAgent(atoms.getAtom(0))).force().ME(gradient[0]);
                        break;
                    case 2:
                        ((IntegratorBox.Forcible)leafAgentManager.getAgent(atoms.getAtom(0))).force().ME(gradient[0]);
                        ((IntegratorBox.Forcible)leafAgentManager.getAgent(atoms.getAtom(1))).force().ME(gradient[1]);
                        break;
                    default:
                        //XXX atoms.count might not equal f.length.  The potential might size its 
                        //array of vectors to be large enough for one AtomSet and then not resize it
                        //back down for another AtomSet with fewer atoms.
                        for (int i=0; i<atoms.getAtomCount(); i++) {
                            ((IntegratorBox.Forcible)leafAgentManager.getAgent(atoms.getAtom(i))).force().ME(gradient[i]);
                        }
                }
            }
        }
    }

	protected final IPotential potentialTether;
	protected double wallForce;
}