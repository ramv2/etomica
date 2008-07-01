package etomica.virial;

import etomica.api.IAtom;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.ISimulation;
import etomica.api.IVector;
import etomica.api.IVector3D;
import etomica.atom.iterator.AtomIteratorAllMolecules;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.integrator.mcmove.MCMoveMolecule;
import etomica.integrator.mcmove.MCMoveStepTracker;
import etomica.potential.P4BondTorsion;
import etomica.space.ISpace;

/**
 * An MC Move for cluster simulations that performs torsion moves on a chain
 * molecule (of length N>=4).  The move is performed on all molecules in the
 * Box.  The move needs the torsion potential in order to choose appropriate
 * torsion angles.  The torsion angle is chosen from a Boltzmann distribution,
 * determined numerically from the potential.  The angles are divided into bins
 * of equal probability.  For a move, a bin is chosen at random and accepted
 * based on the ratio of the 
 * 
 * @author Andrew Schultz
 */
public class MCMoveClusterTorsionMulti extends MCMoveMolecule {

    public MCMoveClusterTorsionMulti(ISimulation sim, IPotentialMaster potentialMaster, ISpace space,
                                     int nAtoms, P4BondTorsion torsionPotential) {
    	this(potentialMaster, space, sim.getRandom(), 1.0, nAtoms, torsionPotential, 20);
        setBondLength(1.0);
    }
    
    /**
     * Constructor for MCMoveAtomMulti.
     * @param parentIntegrator
     * @param nAtoms number of atoms to move in a trial.  Number of atoms in
     * box should be at least one greater than this value (greater
     * because first atom is never moved)
     */
    public MCMoveClusterTorsionMulti(IPotentialMaster potentialMaster, ISpace space,
            IRandom random, double stepSize, int nAtoms, P4BondTorsion torsionPotential, int nBins) {
        super(potentialMaster,random,space,stepSize,Double.POSITIVE_INFINITY,false);
        ((MCMoveStepTracker)getTracker()).setTunable(false);
        this.nAtoms = nAtoms;
        probabilityBins = new double[nBins+1];
        binSize = new double[nBins];
        probabilityReverseMap = new int[nBins+1];
        this.torsionPotential = torsionPotential;
        setStepSizeMax(Math.PI);
        selectedMolecules = new IMolecule[nAtoms];
        oldPositions = new IVector[nAtoms][0];
        weightMeter = new MeterClusterWeight(potential);
        energyMeter = new MeterPotentialEnergy(potential);
        work1 = (IVector3D)space.makeVector();
        work2 = (IVector3D)space.makeVector();
        work3 = (IVector3D)space.makeVector();
        dr21 = (IVector3D)space.makeVector();
        dr23 = (IVector3D)space.makeVector();
        dr34 = (IVector3D)space.makeVector();
        oldCenter = space.makeVector();
    }

    public void setBox(IBox p) {
        super.setBox(p);
        weightMeter.setBox(p);
        energyMeter.setBox(p);
    }
    
    public void setTemperature(double temperature) {
        int nBins = probabilityBins.length - 1;
        int nSubBins = 100;
        beta = 1.0/temperature;
        // numerically integrate P = exp(-beta U) from cosphi=1 to cosphi=-1 (0 to pi radians)
        double totP = 0.5 * Math.exp(-beta*torsionPotential.energyAtAngle(1));
        for (int i=1; i<nSubBins * nBins; i++) {
            double cosphi = Math.cos((Math.PI*i)/(nSubBins*nBins));
            double u = torsionPotential.energyAtAngle(cosphi);
            totP +=  Math.exp(-beta*u);
        }
        totP += 0.5 * Math.exp(-beta * torsionPotential.energyAtAngle(-1));
        // pPerBin is the amount of probability allocated to each bin.
        double pPerBin = totP / nBins;
        
        double thisBinP = 0;
        double previousP = 1;
        double newTot = 0;
        int iBin = 1;
        // last bin always ends at phi=PI
        int lastMapBin = 0;
        // probabilityBins is an array of the boundaries of the bins (in terms of cos phi)
        // bin i goes from probabilityBins[i] to probabilityBins[i+1]
        // the first bin is bin 0 and the last bin is bin nBins-1
        probabilityBins[0] = 0;
        probabilityBins[nBins] = Math.PI;
        
        // probabilityReverseMap maps an angle to a bin
        // probabilityReverseMap[phi / PI * nBins] will be less than or
        //   equal to the appropriate bin.
        // probabilityReverseMap[(phi / PI * nBins) + 1] will be greater than or
        //   equal to the appropriate bin.
        probabilityReverseMap[0] = 0;
        // sum up probability (=exp(-beta U)) until we exceed pPerBin.  Then
        // interpolate to find approximate when the sum is equal to pPerBin.
        // Call that the bin boundary and then begin summing for the next bin.
        for (int i=1; i<nSubBins * nBins + 1; i++) {
            double cosphi = Math.cos((Math.PI*i)/(nSubBins*nBins));
            double u = torsionPotential.energyAtAngle(cosphi);
            double thisP = Math.exp(-beta*u);
            double newP = 0.5 * (previousP + thisP);
            newTot += newP;
//            System.out.println(i+" "+cosphi+" "+u+" "+newTot+" "+thisBinP+" "+pPerBin);
            // if i is too much probability, then ix would be the value
            // of i (based on interpolation) where we hit the limit
            double ix = (i-1) + (pPerBin-thisBinP)/newP;
            while (ix < i) {
                // we have too much probability for this bin.  back up (interpolate)
                // to find where we hit the limit for this bin.  dump the rest into the
                // next bin
                probabilityBins[iBin] = (ix*Math.PI)/(nSubBins*nBins);
                // we'll use this to correct acceptance (which is not correct due
                // to energy inhomogeniety within the bin)
                binSize[iBin-1] = (probabilityBins[iBin]-probabilityBins[iBin-1])/(Math.PI/nBins);

                // we found the lower bound of iBin.  Now mark 
                int nextMapBin = (int)((probabilityBins[iBin])/Math.PI * nBins);
                for (int j=lastMapBin+1; j<nextMapBin+1; j++) {
                    probabilityReverseMap[j] = iBin-1;
//                    System.out.println(j+" "+Math.acos(-1.0+(2.0*j)/nBins)+" "+(iBin-1));
                }
                lastMapBin = nextMapBin;
//                System.out.println(iBin+" "+probabilityBins[iBin]+" "+Math.acos(probabilityBins[iBin]) +" "+ torsionPotential.energyAtAngle(probabilityBins[iBin]));
//                double phi = Math.acos((probabilityBins[iBin-1]+probabilityBins[iBin])*0.5);
//                System.out.println(phi+" "+ torsionPotential.energyAtAngle(Math.cos(phi)) + " "+ (probabilityBins[iBin-1]-probabilityBins[iBin]));
                iBin++;
                if (iBin == nBins) {
                    // we just found the end of the next-to-the-last bin
                    // we know the last bin ends at PI (and already marked it as such)
//                    phi = Math.acos((probabilityBins[iBin-1]+probabilityBins[iBin])*0.5);
//                    System.out.println(phi+" "+ torsionPotential.energyAtAngle(Math.cos(phi))+" "+(probabilityBins[iBin-1]-probabilityBins[iBin]));
                    // we'll use this to correct acceptance (which is not correct due
                    // to energy inhomogeniety within the bin)
                    binSize[iBin-1] = (probabilityBins[iBin]-probabilityBins[iBin-1])/(Math.PI/nBins);;
                    for (int j=lastMapBin+1; j<nBins+1; j++) {
                        probabilityReverseMap[j] = iBin-1;
//                        System.out.println(j+" "+Math.acos(-1.0+(2.0*j)/nBins)+" "+(iBin-1));
                    }
                    return;
                }
                thisBinP -= pPerBin;
                // if we still have too much probability (for the next bin)
                // ix will be the the new value where we hit the limit
                ix += pPerBin/newP;
            }
            thisBinP += newP;
            previousP = thisP;
        }
        throw new RuntimeException("oops");
    }
    
    //note that total energy is calculated
    public boolean doTrial() {
        if (selectedMolecules[0] == null) selectMolecules();
        uOld = energyMeter.getDataAsScalar();
        wOld = weightMeter.getDataAsScalar();
        bias = 1;

        for(int i=0; i<selectedMolecules.length; i++) {
            oldCenter.E(selectedMolecules[i].getType().getPositionDefinition().position(selectedMolecules[i]));
            IAtomSet childList = selectedMolecules[i].getChildList();
            int numChildren = childList.getAtomCount();

            int j = random.nextInt(numChildren-3);  // j=0 ==> first torsion bond (atoms 0,1,2,3)
//            System.out.println("rotating about "+(j+1)+" and "+(j+2));
            // atoms j+1 and j+2 stay fixed.  atoms 0 to j and j+3 to N-1 move
            IAtomPositioned atom0 = (IAtomPositioned)childList.getAtom(j+0);
            IAtomPositioned atom1 = (IAtomPositioned)childList.getAtom(j+1);
            IAtomPositioned atom2 = (IAtomPositioned)childList.getAtom(j+2);
            IAtomPositioned atom3 = (IAtomPositioned)childList.getAtom(j+3);
            dr21.Ev1Mv2(atom0.getPosition(), atom1.getPosition());
            dr23.Ev1Mv2(atom2.getPosition(), atom1.getPosition());
            dr34.Ev1Mv2(atom3.getPosition(), atom2.getPosition());

//            System.out.println("|dr21| "+Math.sqrt(dr21.squared()));
            if (Math.abs(Math.sqrt(dr21.squared())-1.54) > 0.0001) {
                throw new RuntimeException("oops dr21 "+Math.sqrt(dr21.squared())+" "+i);
            }
//            System.out.println("|dr23| "+Math.sqrt(dr23.squared()));
            if (Math.abs(Math.sqrt(dr23.squared())-1.54) > 0.0001) {
                throw new RuntimeException("oops dr23 "+Math.sqrt(dr23.squared()));
            }
//            System.out.println("|dr34| "+Math.sqrt(dr34.squared()));
            if (Math.abs(Math.sqrt(dr34.squared())-1.54) > 0.0001) {
                throw new RuntimeException("oops dr34 "+Math.sqrt(dr34.squared()));
            }
            
            double dr23Sq = dr23.squared();
            dr23.TE(1.0/Math.sqrt(dr23Sq));
            dr21.PEa1Tv1(-dr21.dot(dr23), dr23);
            dr34.PEa1Tv1(-dr34.dot(dr23), dr23);
            
            // current angle
            double oldcosphi = dr21.dot(dr34)/Math.sqrt(dr21.squared()*dr34.squared());
            if (oldcosphi > 0.99999 || oldcosphi < -0.99999) {
                // torsion angle is almost exactly 0 or PI.  skip this molecule
                for (int k=0; k<numChildren; k++) {
                    // save positions anyway since rejectNotify won't know to skip this
                    IAtomPositioned atomk = (IAtomPositioned)childList.getAtom(k);
//                    System.out.println("skipping "+i+" "+k+" "+atomk+" "+atomk.getPosition());
                    oldPositions[i][k].E(atomk.getPosition());
                }
                continue;
            }
            double oldphi = Math.acos(oldcosphi);
            int oldTorsionBin = probabilityReverseMap[(int)((oldphi/Math.PI)*(probabilityBins.length-1))];
//            System.out.println("oldTorsionBin0 = "+oldTorsionBin);
            if (probabilityBins[oldTorsionBin] > oldphi) {
                throw new RuntimeException("reversemap overshot "+oldphi+" "+oldTorsionBin+" "+probabilityBins[oldTorsionBin]);
            }
            while (oldTorsionBin < probabilityReverseMap.length && probabilityBins[oldTorsionBin+1] < oldphi) {
                oldTorsionBin++;
            }
            
            work1.E(dr21);
            work1.XE(dr34);
            // do we need to rotate the first part of the molecule clockwise
            // or counter-clockwise?
            boolean flippit = work1.dot(dr23) > 0;

            // pick a new bin
            int torsionBin = random.nextInt(probabilityBins.length-1);
            // pick some angle within that bin
            double newphi = probabilityBins[torsionBin] + random.nextDouble()*
                        (probabilityBins[torsionBin+1]-probabilityBins[torsionBin]);
            double deltaphi = 0.5*(newphi - oldphi);

            // if we move from a high-U (wide) bin to a low-U (narrow) bin, the nominal
            // acceptance probability will be too high.  correct for that here. 
            bias *= binSize[torsionBin]/binSize[oldTorsionBin];

            if (flippit) deltaphi = -deltaphi;

            for (int k=0; k<numChildren; k++) {
                IAtomPositioned atomk = (IAtomPositioned)childList.getAtom(k);
                oldPositions[i][k].E(atomk.getPosition());
                if (k == j+3) {
                    deltaphi = -deltaphi;
                }
                else if (k > j && k < j+3) {
                    continue;
                }
                
//                System.out.println("rotating "+atomk);

                // we can use atom1 as a reference atom to rotate the first and
                // last part of the molecule.  anything on the atom2-atom3 axis
                // would work
                work1.Ev1Mv2(atomk.getPosition(), atom1.getPosition());
//                System.out.println("|work1| "+Math.sqrt(work1.squared()));
                work2.E(dr23);   // dr23 is the axis of rotation
                // v1 = v1overAxis * axis
                double v1overAxis = work2.dot(work1);

                if (Math.abs(Math.abs(v1overAxis)-Math.sqrt(work1.squared())) < 1E-10) {
                    // axis is almost exactly parallel or anti-parallel to direction,
                    // so just don't rotate.
                    continue;
                }

                work2.TE(-v1overAxis);
                work2.PE(work1);
                dr21.E(work2);
                dr21.PEa1Tv1(v1overAxis, dr23);
//                System.out.println("|v1+v2| "+Math.sqrt(dr21.squared()));

                // now temp = v2 
                double v2Sq = work2.squared();
                work3.E(dr23);
                work3.XE(work1);
                work3.TE(Math.sqrt(v2Sq/work3.squared()));
                //System.out.println(Math.sqrt(work2.squared())+" "+Math.sqrt(work3.squared()));

                dr21.E(work3);
                dr21.PEa1Tv1(v1overAxis, dr23);
//                System.out.println("|v1+v3| "+Math.sqrt(dr21.squared()));

                // now temp = v3
//                System.out.println("dot "+dr23.dot(work2)+" "+work2.dot(work3)+" "+dr23.dot(work3));

                work1.Ea1Tv1(Math.cos(deltaphi), work2);
                work1.PEa1Tv1(Math.sin(deltaphi), work3);
                work1.PEa1Tv1(v1overAxis, dr23);
//                System.out.println("|work1| "+Math.sqrt(work1.squared()));
                work1.PE(atom1.getPosition());

                atomk.getPosition().E(work1);

                if (k==j || k == j+2) {
                    dr21.Ev1Mv2(atom0.getPosition(), atom1.getPosition());
//                    System.out.println("|dr21| "+Math.sqrt(dr21.squared()));
                    if (Math.abs(Math.sqrt(dr21.squared())-1.54) > 0.0001) {
                        throw new RuntimeException("oops dr21 "+Math.sqrt(dr21.squared()));
                    }
                    dr23.Ev1Mv2(atom2.getPosition(), atom1.getPosition());
//                    System.out.println("|dr23| "+Math.sqrt(dr23.squared()));
                    if (Math.abs(Math.sqrt(dr23.squared())-1.54) > 0.0001) {
                        throw new RuntimeException("oops dr23 "+Math.sqrt(dr23.squared()));
                    }
                    dr34.Ev1Mv2(atom3.getPosition(), atom2.getPosition());
//                    System.out.println("|dr34| "+Math.sqrt(dr34.squared()));
                    if (Math.abs(Math.sqrt(dr34.squared())-1.54) > 0.0001) {
                        throw new RuntimeException("oops dr34 "+Math.sqrt(dr34.squared()));
                    }
                    
                    dr23Sq = dr23.squared();
                    dr23.TE(1.0/Math.sqrt(dr23Sq));
                    dr21.PEa1Tv1(-dr21.dot(dr23), dr23);
                    dr34.PEa1Tv1(-dr34.dot(dr23), dr23);
                    
                    // current angle
                    double newcosphicheck = dr21.dot(dr34)/Math.sqrt(dr21.squared()*dr34.squared());
//                    System.out.println(Math.acos(oldcosphi)+" "+Math.acos(newcosphi)+" "+Math.acos(newcosphicheck));
//                    System.out.println(oldcosphi+" "+newcosphi+" "+newcosphicheck);
                    if (k<j+1) {
//                        System.out.println("error: "+((Math.acos(oldcosphi) + Math.acos(newcosphi))*0.5-Math.acos(newcosphicheck)));
                        if (Math.abs((oldphi + newphi)*0.5-Math.acos(newcosphicheck)) > 1.e-8) {
                            throw new RuntimeException("shouldn't need to flip");
                        }
                    }
                    else {
//                        System.out.println("error: "+(Math.acos(newcosphi)-Math.acos(newcosphicheck)));
                        if (Math.abs(newphi-Math.acos(newcosphicheck)) > 1.e-8) {
                            throw new RuntimeException("shouldn't need to flip");
                        }
                    }
                }
            }
            oldCenter.ME(selectedMolecules[i].getType().getPositionDefinition().position(selectedMolecules[i]));
            for (int k=0; k<numChildren; k++) {
                // shift the whole molecule so that the center of mass (or whatever
                // the position definition uses) doesn't change
                IAtomPositioned atomk = (IAtomPositioned)childList.getAtom(k);
                atomk.getPosition().PE(oldCenter);
            }
        }
        ((BoxCluster)box).trialNotify();
        wNew = weightMeter.getDataAsScalar();
        uNew = energyMeter.getDataAsScalar();
//        System.out.println(uOld+" => "+uNew+"   "+wOld+" => "+wNew+" "+bias+" "+stepSize);
        return true;
    }
    
    public void setBondLength(double b) {
        bondLength = b;
    }
	
    protected IAtom[] selectMolecules() {
        AtomIteratorAllMolecules iterator = new AtomIteratorAllMolecules(box);
        if (iterator.size() != nAtoms) throw new IllegalStateException("move should work on number of molecules in box");
        iterator.reset();
        int i=0;
        for (IMolecule a = (IMolecule)iterator.nextAtom(); a != null;
             a = (IMolecule)iterator.nextAtom()) {
            int numChildren = a.getChildList().getAtomCount();
            oldPositions[i] = new IVector[numChildren];
            for (int j=0; j<numChildren; j++) {
                oldPositions[i][j] = space.makeVector();
            }
            selectedMolecules[i++] = a;
        }
        return selectedMolecules;
    }

    public void rejectNotify() {
        for(int i=0; i<selectedMolecules.length; i++) {
            IAtomSet childList = selectedMolecules[i].getChildList();
            for (int j=0; j<childList.getAtomCount(); j++) {
                IAtomPositioned atomj = (IAtomPositioned)childList.getAtom(j);
                atomj.getPosition().E(oldPositions[i][j]);
            }
//            System.out.println(selectedAtoms[i]+" rejected => "+selectedAtoms[i].coord.position());
        }
        ((BoxCluster)box).rejectNotify();
    }

    public void acceptNotify() {
        ((BoxCluster)box).acceptNotify();
    }
    
    public double getB() {
        return -(uNew - uOld);
    }
    
    public double getA() {
        return bias*wNew/wOld;
    }
	
    private static final long serialVersionUID = 1L;
    protected final MeterClusterWeight weightMeter;
    protected final MeterPotentialEnergy energyMeter;
    protected final P4BondTorsion torsionPotential;
    protected final double[] probabilityBins;
    protected final double[] binSize;
    protected final int[] probabilityReverseMap;
    protected final int nAtoms;
    protected final IMolecule[] selectedMolecules;
    protected double bondLength;
    protected final IVector3D work1, work2, work3;
    protected final IVector3D dr21, dr23, dr34;
    protected final IVector[][] oldPositions;
    protected final IVector oldCenter;
    protected double wOld, wNew, bias;
    protected double beta;
}