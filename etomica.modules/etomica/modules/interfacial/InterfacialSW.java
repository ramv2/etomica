package etomica.modules.interfacial;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtom;
import etomica.api.IAtomPositionDefinition;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.atom.iterator.ApiBuilder;
import etomica.box.Box;
import etomica.config.ConfigurationLattice;
import etomica.config.ConformationLinear;
import etomica.integrator.IntegratorHard;
import etomica.integrator.IntegratorMD.ThermostatType;
import etomica.lattice.LatticeCubicFcc;
import etomica.lattice.LatticeOrthorhombicHexagonal;
import etomica.nbr.list.PotentialMasterList;
import etomica.potential.P2HardBond;
import etomica.potential.P2HardSphere;
import etomica.potential.P2SquareWell;
import etomica.potential.PotentialGroup;
import etomica.simulation.Simulation;
import etomica.space.Space;
import etomica.space2d.Space2D;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresHetero;
import etomica.species.SpeciesSpheresMono;

/**
 * Simulation for interfacial tension module.  Simulation itself is just a
 * simple LJ system.
 *
 * @author Andrew Schultz
 */
public class InterfacialSW extends Simulation {

    private static final long serialVersionUID = 1L;
    public final SpeciesSpheresMono species;
    public final SpeciesSpheresHetero surfactant;
    public final IBox box;
    public final IntegratorHard integrator;
    public final ActivityIntegrate activityIntegrate;
    public final IAtomTypeLeaf leafType, headType, tailType;
    public final P2SquareWell p2Head, p2HeadHead;
    public final P2HardSphere p2TailTail, p2Tail, p2HeadTail;
    public final P2HardBond p2Bond;

    public InterfacialSW(Space _space) {
        super(_space);
        double pRange = 2.0;
        PotentialMasterList potentialMaster = new PotentialMasterList(this, pRange, space);

        int N = 643;  //number of atoms

        //controller and integrator
	    integrator = new IntegratorHard(this, potentialMaster, space);
	    if (space.D() == 2) {
	        integrator.setTemperature(0.4);
	        N = 300;
	    }
	    integrator.setIsothermal(true);
        integrator.setThermostat(ThermostatType.ANDERSEN_SINGLE);
        integrator.setThermostatInterval(1);
        activityIntegrate = new ActivityIntegrate(integrator);
        getController().addAction(activityIntegrate);
        integrator.setTimeStep(0.01);

	    //species and potentials
	    species = new SpeciesSpheresMono(this, space);
        getSpeciesManager().addSpecies(species);
        surfactant = new SpeciesSpheresHetero(this, space, 2);
        surfactant.setChildCount(new int[]{1,1});
        surfactant.setTotalChildren(2);
        surfactant.setPositionDefinition(new IAtomPositionDefinition() {
            public IVector position(IAtom atom) {
                IAtomSet children = ((IMolecule)atom).getChildList();
                IVector pos0 = ((IAtomPositioned)children.getAtom(0)).getPosition();
                IVector pos1 = ((IAtomPositioned)children.getAtom(1)).getPosition();
                dr.Ev1Mv2(pos1, pos0);
                box.getBoundary().nearestImage(dr);
                dr.TE(0.5);
                dr.PE(pos0);
                dr.ME(box.getBoundary().centralImage(dr));
                return dr;
            }
            final IVector dr = space.makeVector();
        });
        ((ConformationLinear)surfactant.getConformation()).setBondLength(0.9);
        getSpeciesManager().addSpecies(surfactant);
        leafType = species.getLeafType();
        headType = surfactant.getChildType(0); // head likes the monatomic species
        tailType = surfactant.getChildType(1);
        // these will (unfortunately) not be in the system when the neighborCellManager
        // tries to decide if they're interacting or not.  so it to true here
        headType.setInteracting(true);
        tailType.setInteracting(true);

        //instantiate several potentials for selection in combo-box
        P2SquareWell p2SW = new P2SquareWell(space, 1.0, 1.5, 1.0, true);
	    potentialMaster.addPotential(p2SW, new IAtomTypeLeaf[]{leafType, leafType});
        p2Head = new P2SquareWell(space, 1.0, 1.5, 1.0, true);
        potentialMaster.addPotential(p2Head, new IAtomTypeLeaf[]{leafType, headType});
        p2HeadHead = new P2SquareWell(space, 1.0, 1.5, 1.0, true);
        potentialMaster.addPotential(p2HeadHead, new IAtomTypeLeaf[]{headType, headType});

        p2TailTail = new P2HardSphere(space, 1.0, true);
        potentialMaster.addPotential(p2TailTail, new IAtomTypeLeaf[]{tailType, tailType});
        p2Tail = new P2HardSphere(space, 1.0, true);
        potentialMaster.addPotential(p2Tail, new IAtomTypeLeaf[]{leafType, tailType});
        p2HeadTail = new P2HardSphere(space, 1.0, true);
        potentialMaster.addPotential(p2HeadTail, new IAtomTypeLeaf[]{headType, tailType});
        
        p2Bond = new P2HardBond(space, 0.8, 0.2, true);
        PotentialGroup p1Surfactant = potentialMaster.makePotentialGroup(1);
        p1Surfactant.addPotential(p2Bond, ApiBuilder.makeAdjacentPairIterator());
        potentialMaster.addPotential(p1Surfactant, new ISpecies[]{surfactant});

        //construct box
	    box = new Box(this, space);
        addBox(box);
        IVector dim = space.makeVector();
        if (space.D() == 2) {
            dim.E(new double[]{30,15});
        }
        else {
            dim.E(new double[]{12,10,10});
        }
        box.setDimensions(dim);
        box.setNMolecules(species, N);
        if (space.D() == 2) {
            new ConfigurationLattice(new LatticeOrthorhombicHexagonal(), space).initializeCoordinates(box);
        }
        else {
            new ConfigurationLattice(new LatticeCubicFcc(), space).initializeCoordinates(box);
        }
        integrator.setBox(box);

        integrator.addIntervalAction(potentialMaster.getNeighborManager(box));
        integrator.addNonintervalListener(potentialMaster.getNeighborManager(box));
    }
    
    public static void main(String[] args) {
        Space space = Space2D.getInstance();
        if(args.length != 0) {
            try {
                int D = Integer.parseInt(args[0]);
                if (D == 3) {
                    space = Space3D.getInstance();
                }
            } catch(NumberFormatException e) {}
        }
            
        InterfacialSW sim = new InterfacialSW(space);
        sim.getController().actionPerformed();
    }//end of main
}