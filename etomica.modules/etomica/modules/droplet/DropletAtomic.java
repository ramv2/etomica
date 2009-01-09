package etomica.modules.droplet;
import etomica.action.BoxInflate;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomList;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.api.IVectorMutable;
import etomica.atom.AtomTypeSphere;
import etomica.atom.MoleculeArrayList;
import etomica.box.Box;
import etomica.chem.elements.Argon;
import etomica.config.ConfigurationLattice;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.lattice.LatticeCubicFcc;
import etomica.nbr.list.PotentialMasterList;
import etomica.potential.P2LennardJones;
import etomica.potential.P2SoftSphericalTruncatedForceShifted;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.space2d.Space2D;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Kelvin;

/**
 * Atomic simulation for Droplet module.
 *
 * @author Andrew Schultz
 */
public class DropletAtomic extends Simulation {

    private static final long serialVersionUID = 1L;
    public final SpeciesSpheresMono species;
    public final IBox box;
    public final IntegratorVelocityVerlet integrator;
    public final ActivityIntegrate activityIntegrate;
    public final P2LennardJones p2LJ;
    public final P2SoftSphericalTruncatedForceShifted p2LJt;
    public final P1Smash p1Smash;
    protected int nNominalAtoms;
    protected double dropRadius;
    protected double xDropAxis;
    protected double density;
    protected double sigma;

    public DropletAtomic(Space _space) {
        super(_space);
        double pRange = 2.5;
        sigma = 3.35;
        nNominalAtoms = 171500;
        dropRadius = 44;
        xDropAxis = 1;
        density = 0.6;
        
        PotentialMasterList potentialMaster = new PotentialMasterList(this, sigma*pRange*1.5, space);

        //controller and integrator
	    integrator = new IntegratorVelocityVerlet(this, potentialMaster, space);
	    integrator.setTimeStep(0.005);
	    integrator.setIsothermal(true);
        activityIntegrate = new ActivityIntegrate(integrator);
        getController().addAction(activityIntegrate);
        integrator.setTemperature(Kelvin.UNIT.toSim(118));

	    //species and potentials
	    species = new SpeciesSpheresMono(this, space, Argon.INSTANCE);
        getSpeciesManager().addSpecies(species);
        IAtomTypeLeaf leafType = species.getLeafType();
        ((AtomTypeSphere)leafType).setDiameter(sigma);
        
        p2LJ = new P2LennardJones(space);
        p2LJ.setEpsilon(Kelvin.UNIT.toSim(118));
        p2LJ.setSigma(3.35);
        p2LJt = new P2SoftSphericalTruncatedForceShifted(space, p2LJ, pRange);
        potentialMaster.addPotential(p2LJt, new IAtomTypeLeaf[]{leafType,leafType});

        p1Smash = new P1Smash(space);
        p1Smash.setG(0.004);
        potentialMaster.addPotential(p1Smash, new IAtomTypeLeaf[]{leafType});

        //construct box
	    box = new Box(new BoundaryRectangularPeriodic(space), space);
	    IVectorMutable newDim = space.makeVector();
	    newDim.E(100);
	    box.getBoundary().setDimensions(newDim);
        addBox(box);
        integrator.setBox(box);

        makeDropShape();
        
        integrator.addIntervalAction(potentialMaster.getNeighborManager(box));
        integrator.addNonintervalListener(potentialMaster.getNeighborManager(box));
    }
    
    public void makeDropShape() {
        box.setNMolecules(species, nNominalAtoms);

        BoxInflate inflater = new BoxInflate(box, space);
        inflater.setTargetDensity(density/(sigma*sigma*sigma));
        inflater.actionPerformed();

        ConfigurationLattice config = new ConfigurationLattice(new LatticeCubicFcc(space), space);
        config.initializeCoordinates(box);
        
        IAtomList leafList = box.getLeafList();
        IVectorMutable v = space.makeVector();
        double dropRadiusSq = dropRadius*dropRadius;
        int ambientCount = 0;
        MoleculeArrayList outerMolecules = new MoleculeArrayList();
        for (int i=0; i<leafList.getAtomCount(); i++) {
            v.E(((IAtomPositioned)leafList.getAtom(i)).getPosition());
            v.setX(0, v.x(0)/xDropAxis);
            if (v.squared() > dropRadiusSq) {
                ambientCount++;
                if (ambientCount == 20) {
                    ambientCount = 0;
                }
                else {
                    outerMolecules.add(leafList.getAtom(i).getParentGroup());
                }
            }
        }
        for (int i=0; i<outerMolecules.getMoleculeCount(); i++) {
            box.removeMolecule(outerMolecules.getMolecule(i));
        }
        System.out.println(outerMolecules.getMoleculeCount());
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
            
        DropletAtomic sim = new DropletAtomic(space);
        sim.getController().actionPerformed();
    }//end of main
}
