package etomica.modules.chainequilibrium;

import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.api.IConformation;
import etomica.api.IMolecule;
import etomica.api.IRandom;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.config.ConfigurationLattice;
import etomica.lattice.BravaisLatticeCrystal;
import etomica.lattice.SpaceLattice;
import etomica.space.ISpace;

/**
 * Configuration that puts atoms randomly on a lattice and groups pairs of
 * initiator atoms together as though they are a single molecule.
 * 
 * @author Andrew Schultz
 */
public class ConfigurationLatticeFreeRadical extends ConfigurationLattice {

    public ConfigurationLatticeFreeRadical(SpaceLattice lattice,
            ISpace space, IRandom random) {
        super(lattice, space);
        this.random = random;
    }

    public void setSpecies(ISpecies speciesInitiator, ISpecies speciesMonomer) {
        this.speciesInitiator = speciesInitiator;
        this.speciesMonomer = speciesMonomer;
    }

    /**
     * Places the molecules in the given box on the positions of the
     * lattice.  
     */
    public void initializeCoordinates(IBox box) {
        IAtomSet moleculeList = box.getMoleculeList();
        IAtomSet initiatorList = box.getMoleculeList(speciesInitiator);
        IAtomSet monomerList = box.getMoleculeList(speciesMonomer);
        int sumOfMolecules = initiatorList.getAtomCount() / 2 + monomerList.getAtomCount();
        int basisSize = 1;
        if (lattice instanceof BravaisLatticeCrystal) {
            basisSize = ((BravaisLatticeCrystal)lattice).getBasis().getScaledCoordinates().length;
        }
        int nCells = (int) Math.ceil((double) sumOfMolecules
                / (double) basisSize);

        // determine scaled shape of simulation volume
        IVector shape = space.makeVector();
        shape.E(box.getBoundary().getDimensions());
        shape.PE(-boundaryPadding);
        IVector latticeConstantV = space.makeVector(lattice.getLatticeConstants());
        shape.DE(latticeConstantV);

        // determine number of cells in each direction
        int[] latticeDimensions = calculateLatticeDimensions(nCells, shape);
        int nSites = basisSize;
        for (int i=0; i<latticeDimensions.length; i++) {
            nSites *= latticeDimensions[i];
        }
        if (indexIterator.getD() > latticeDimensions.length) {
            int[] iteratorDimensions = new int[latticeDimensions.length+1];
            System.arraycopy(latticeDimensions, 0, iteratorDimensions, 0,
                    latticeDimensions.length);
            iteratorDimensions[latticeDimensions.length] = basisSize;
            indexIterator.setSize(iteratorDimensions);
        }
        else {
            indexIterator.setSize(latticeDimensions);
        }

        // determine lattice constant
        IVector latticeScaling = space.makeVector();
        if (rescalingToFitVolume) {
            // in favorable situations, this should be approximately equal
            // to 1.0
            latticeScaling.E(box.getBoundary().getDimensions());
            latticeScaling.PE(-boundaryPadding);
            latticeScaling.DE(latticeConstantV);
            latticeScaling.DE(space.makeVector(latticeDimensions));
        } else {
            latticeScaling.E(1.0);
        }

        // determine amount to shift lattice so it is centered in volume
        IVector offset = space.makeVector();
        offset.E(box.getBoundary().getDimensions());
        IVector vectorOfMax = space.makeVector();
        IVector vectorOfMin = space.makeVector();
        IVector site = space.makeVector();
        vectorOfMax.E(Double.NEGATIVE_INFINITY);
        vectorOfMin.E(Double.POSITIVE_INFINITY);

        indexIterator.reset();

        while (indexIterator.hasNext()) {
            site.E((IVector) lattice.site(indexIterator.next()));
            site.TE(latticeScaling);
            for (int i=0; i<site.getD(); i++) {
                vectorOfMax.setX(i, Math.max(site.x(i),vectorOfMax.x(i)));
                vectorOfMin.setX(i, Math.min(site.x(i),vectorOfMin.x(i)));
            }
        }
        offset.Ev1Mv2(vectorOfMax, vectorOfMin);
        offset.TE(-0.5);
        offset.ME(vectorOfMin);

        myLat = new MyLattice(lattice, latticeScaling, offset);

        // Place molecules
        indexIterator.reset();
        double voidFrac = (nSites - sumOfMolecules)/((double)nSites);
        double voidSum = 0;
        int siteCount = 0;
        boolean[] done = new boolean[sumOfMolecules];
        for (int j=0; j<sumOfMolecules; j++) {
            int i;
            do {
                i = random.nextInt(sumOfMolecules);
            }
            while (done[i]);
            done[i] = true;
            if (i < initiatorList.getAtomCount()/2) {
                i *= 2;
            }
            else {
                i += initiatorList.getAtomCount()/2;
            }
            int[] ii = indexIterator.next();
            siteCount++;
            // add voidFrac for each /site/ (not molecule)
            voidSum += voidFrac;
            while (voidSum > 1.0) {
                // we've gone through enough sites that we should insert a void
                // now.  Subtract one, but still add voidFrac since we're still
                // advancing one site.
                voidSum += voidFrac - 1;
                ii = indexIterator.next();
                siteCount++;
            }
            // initialize coordinates of child atoms
            IMolecule a = (IMolecule)moleculeList.getAtom(i);
        	atomActionTranslateTo.setAtomPositionDefinition(a.getType().getPositionDefinition());
            IConformation config = ((ISpecies)a.getType()).getConformation();
            config.initializePositions(a.getChildList());

            atomActionTranslateTo.setDestination((IVector)myLat.site(ii));
            if (a.getType() == speciesInitiator) {
                IVector dest = atomActionTranslateTo.getDestination();
                dest.setX(0, dest.x(0)-0.4);
            }
            atomActionTranslateTo.actionPerformed(a);
            if (a.getType() == speciesInitiator) {
                i++;
                a = (IMolecule)moleculeList.getAtom(i);
                IVector dest = atomActionTranslateTo.getDestination();
                dest.setX(0, dest.x(0)+0.8);
                atomActionTranslateTo.actionPerformed(a);
            }
        }
        if (nSites - siteCount > Math.ceil(1.0/(1.0-voidFrac))) {
            // nSites - siteCount = 0 is ideal.
            // indexIterator.next() would throw if nSites < siteCount
            // nSites - siteCount = 1 will be typical for cases where the void distribution can't be perfect
            // so we just need to check for nSites - siteCount > 1
            // for very low occupancy lattices, we'll do worse.
            throw new RuntimeException("Failed to properly iterate through the lattice sites "+nSites+" "+siteCount);
        }
    }

    protected ISpecies speciesInitiator, speciesMonomer;
    protected final IRandom random;
    private static final long serialVersionUID = 3L;
}