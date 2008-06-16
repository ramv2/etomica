package etomica.action;

import etomica.api.IAction;
import etomica.api.IAtom;
import etomica.api.IAtomSet;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.api.IVector;
import etomica.atom.IAtomKinetic;
import etomica.space.ISpace;
import etomica.util.Debug;

/**
 * Scales the momenta of all the leaf atoms in an IBox such that the kinetic
 * temperature matches some value.  The net momentum is also subtracted off so
 * that there is no net momentum.
 * 
 * @author Andrew Schultz
 */
public class BoxScaleMomenta implements IAction {

    public BoxScaleMomenta(IBox box, ISpace space) {
        this.box = box;
        momentum = space.makeVector();
    }

    public void setTemperature(double newTemperature) {
        temperature = newTemperature;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public IBox getBox() {
        return box;
    }
    
    public void actionPerformed() {
        momentum.E(0);
        IAtomSet leafList = box.getLeafList();
        int nLeaf = leafList.getAtomCount();
        if (nLeaf == 0) return;
        if (nLeaf > 1) {
            for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
                IAtom a = leafList.getAtom(iLeaf);
                double mass = ((IAtomTypeLeaf)a.getType()).getMass();
                if (mass != Double.POSITIVE_INFINITY) {
                    momentum.PEa1Tv1(mass,((IAtomKinetic)a).getVelocity());
                }
            }
            momentum.TE(1.0/nLeaf);
            //set net momentum to 0
            for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
                IAtomKinetic a = (IAtomKinetic)leafList.getAtom(iLeaf);
                double rm = ((IAtomTypeLeaf)a.getType()).rm();
                if (rm != 0) {
                    a.getVelocity().PEa1Tv1(-rm,momentum);
                }
            }
            if (Debug.ON) {
                momentum.E(0);
                for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
                    IAtomKinetic a = (IAtomKinetic)leafList.getAtom(iLeaf);
                    double mass = ((IAtomTypeLeaf)a.getType()).getMass();
                    if (mass != Double.POSITIVE_INFINITY) {
                        momentum.PEa1Tv1(mass,a.getVelocity());
                    }
                }
                momentum.TE(1.0/nLeaf);
                if (Math.sqrt(momentum.squared()) > 1.e-10) {
                    System.out.println("Net momentum per leaf atom is "+momentum+" but I expected it to be 0");
                }
            }
            momentum.E(0);
        }
        
        // calculate current kinetic temperature.
        for (int i = 0; i < momentum.getD(); i++) {
            // scale independently in each dimension
            double sum = 0.0;
            for (int iAtom = 0; iAtom<nLeaf; iAtom++) {
                IAtomKinetic atom = (IAtomKinetic)leafList.getAtom(iAtom);
                double mass = ((IAtomTypeLeaf)atom.getType()).getMass();
                if(mass == Double.POSITIVE_INFINITY) continue;
                double v = atom.getVelocity().x(i);
                sum += mass*v*v;
            }
            if (sum == 0 && temperature != 0) {
                // wonky.  possible if you try to scale up velocities after T=0.
                // but then, you called scaleMomenta, so you're probably a bad
                // person and deserve this.
                throw new RuntimeException("atoms have no velocity component in "+i+" dimension");
            }
            double s = Math.sqrt(temperature / (sum / nLeaf));
            if (s == 1) continue;
            for (int iAtom = 0; iAtom<nLeaf; iAtom++) {
                IAtomKinetic atom = (IAtomKinetic)leafList.getAtom(iAtom);
                IVector vel = atom.getVelocity(); 
                vel.setX(i, vel.x(i)*s); //scale momentum
            }
        }
    }

    protected final IBox box;
    protected final IVector momentum;
    protected double temperature;
}