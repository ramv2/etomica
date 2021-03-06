/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.modules.materialfracture;

import etomica.api.IAtomList;
import etomica.api.IPotentialAtomic;
import etomica.api.IVector;
import etomica.integrator.IntegratorBox;
import etomica.potential.PotentialCalculationForcePressureSum;
import etomica.potential.PotentialSoft;
import etomica.space.ISpace;

public class PotentialCalculationForceStress extends
        PotentialCalculationForcePressureSum {

    public PotentialCalculationForceStress(ISpace space) {
        super(space);
    }
    
    public void reset() {
        super.reset();
        load = 0;
    }

    public double getLoad() {
        return load;
    }

    /**
     * Adds forces due to given potential acting on the atoms produced by the iterator.
     * Implemented for only 1- and 2-body potentials.
     */
    public void doCalculation(IAtomList atoms, IPotentialAtomic potential) {
        PotentialSoft potentialSoft = (PotentialSoft)potential;
        int nBody = potential.nBody();
        IVector[] f = potentialSoft.gradient(atoms, pressureTensor);
        switch(nBody) {
            case 1:
                ((IntegratorBox.Forcible)integratorAgentManager.getAgent(atoms.getAtom(0))).force().ME(f[0]);
                if (potential instanceof P1Tension) {
                    load += Math.abs(f[0].getX(0));
                }
                break;
            case 2:
                ((IntegratorBox.Forcible)integratorAgentManager.getAgent(atoms.getAtom(0))).force().ME(f[0]);
                ((IntegratorBox.Forcible)integratorAgentManager.getAgent(atoms.getAtom(1))).force().ME(f[1]);
                break;
            default:
                //XXX atoms.count might not equal f.length.  The potential might size its 
                //array of vectors to be large enough for one IAtomSet and then not resize it
                //back down for another IAtomSet with fewer atoms.
                for (int i=0; i<atoms.getAtomCount(); i++) {
                    ((IntegratorBox.Forcible)integratorAgentManager.getAgent(atoms.getAtom(i))).force().ME(f[i]);
                }
        }
    }

    protected double load;
}
