/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.modules.droplet;

import etomica.api.IAtomList;
import etomica.api.IPotentialAtomic;
import etomica.api.IVector;
import etomica.atom.AtomFilter;
import etomica.potential.Potential2SoftSpherical;
import etomica.space.ISpace;
import etomica.space.Tensor;

/**
 * Cohesive potential for mesoscale droplet simulation
 * @author Andrew Schultz
 */
public class P2Cohesion extends Potential2SoftSpherical implements
        IPotentialAtomic {

    public P2Cohesion(ISpace space) {
        super(space);
    }

    public double energy(IAtomList atoms) {
        if (useSurfaceOnly && (liquidFilter.accept(atoms.getAtom(0)) || liquidFilter.accept(atoms.getAtom(1)))) {
            return 0;
        }
        return super.energy(atoms);
    }

    public IVector[] gradient(IAtomList atoms, Tensor pressureTensor) {
        if (useSurfaceOnly && (liquidFilter.accept(atoms.getAtom(0)) || liquidFilter.accept(atoms.getAtom(1)))) {
            gradient[0].E(0);
            gradient[1].E(0);
            pressureTensor.E(0);
            return gradient;
        }
        return super.gradient(atoms, pressureTensor);
    }

    public IVector[] gradient(IAtomList atoms) {
        if (useSurfaceOnly && (liquidFilter.accept(atoms.getAtom(0)) || liquidFilter.accept(atoms.getAtom(1)))) {
            gradient[0].E(0);
            gradient[1].E(0);
            return gradient;
        }
        return super.gradient(atoms);
    }

    public double hyperVirial(IAtomList atoms) {
        if (useSurfaceOnly && (liquidFilter.accept(atoms.getAtom(0)) || liquidFilter.accept(atoms.getAtom(1)))) {
            return 0;
        }
        return super.hyperVirial(atoms);
    }

    public double virial(IAtomList atoms) {
        if (useSurfaceOnly && (liquidFilter.accept(atoms.getAtom(0)) || liquidFilter.accept(atoms.getAtom(1)))) {
            return 0;
        }
        return super.virial(atoms);
    }

    public double d2u(double r2) {
        if (r2 > epsilonSq) {
            return 0;
        }
        return r2*fac*(1-3*r2/epsilonSq)*dv;
    }

    public double du(double r2) {
        if (r2 > epsilonSq) {
            return 0;
        }
        return r2*fac*(1-r2/epsilonSq)*dv;
    }

    public double uInt(double rc) {
        return 0;
    }

    public double u(double r2) {
        if (r2 > epsilonSq) {
            return 0;
        }
        return 0.5*r2*fac*(1-0.5*r2/epsilonSq)*dv;
    }
    
    public double getRange() {
        return epsilon;
    }

    public void setEpsilon(double newEpsilon) {
        if (newEpsilon < 0) {
            throw new RuntimeException("Ooops");
        }
        epsilon = newEpsilon;
        epsilonSq = epsilon*epsilon;
        fac = 192/Math.PI/(epsilonSq*epsilonSq*epsilonSq);
    }
    
    public double getEpsilon() {
        return epsilon;
    }
    
    public void setDv(double newDv) {
        dv = newDv;
    }
    
    public double getDv() {
        return dv;
    }

    public void setLiquidFilter(AtomFilter newLiquidFilter) {
        liquidFilter = newLiquidFilter;
    }

    public AtomFilter getLiquidFilter() {
        return liquidFilter;
    }

    public void setUseSurfaceOnly(boolean newUseSurfaceOnly) {
        useSurfaceOnly = newUseSurfaceOnly;
    }
    
    public boolean getUseSurfaceOnly() {
        return useSurfaceOnly;
    }
    
    protected double epsilon, epsilonSq;
    protected double fac, dv;
    protected boolean useSurfaceOnly;
    protected AtomFilter liquidFilter;
}
