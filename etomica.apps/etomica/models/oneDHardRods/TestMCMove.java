package etomica.models.oneDHardRods;

import etomica.action.activity.ActivityIntegrate;
import etomica.action.activity.Controller;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.box.Box;
import etomica.integrator.IntegratorMC;
import etomica.lattice.crystal.Basis;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.nbr.list.PotentialMasterList;
import etomica.normalmode.CoordinateDefinition;
import etomica.normalmode.CoordinateDefinitionLeaf;
import etomica.normalmode.NormalModes1DHR;
import etomica.normalmode.P2XOrder;
import etomica.normalmode.WaveVectorFactory;
import etomica.potential.P2HardSphere;
import etomica.potential.Potential2;
import etomica.potential.Potential2HardSpherical;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.species.SpeciesSpheresMono;
import etomica.util.ParameterBase;
import etomica.util.ReadParameters;

public class TestMCMove extends Simulation {
    private static final long serialVersionUID = 1L;
    public Boundary boundary;
    IntegratorMC integrator;
    ActivityIntegrate activityIntegrate;
    IBox box;
    CoordinateDefinition coordinateDefinition;
    Primitive primitive;
    Basis basis;
    int[] nCells;
    SpeciesSpheresMono species;
    NormalModes1DHR nm;

    private static final String APP_NAME = "TestMCMove";
    

    public TestMCMove(Space _space, int numAtoms, double density, double temperature, String filename, double harmonicFudge){
        super(_space, true);
        
        SpeciesSpheresMono species = new SpeciesSpheresMono(this, space);
        getSpeciesManager().addSpecies(species);
        
        PotentialMasterList potentialMaster = new PotentialMasterList(this, space);
        box = new Box(this, space);
        box.setNMolecules(species, numAtoms);
        
        Potential2 p2 = new P2HardSphere(space, 1.0, true);
        p2 = new P2XOrder(space, (Potential2HardSpherical)p2);
        potentialMaster.addPotential(p2, new IAtomTypeLeaf[]
                {species.getLeafType(), species.getLeafType()});
        
        primitive = new PrimitiveCubic(space, 1.0/density);
        boundary = new BoundaryRectangularPeriodic(space, getRandom(),
                numAtoms/density);
        nCells = new int[]{numAtoms};
        box.setBoundary(boundary);
        
        coordinateDefinition = new 
                CoordinateDefinitionLeaf(this, box, primitive, space);
        coordinateDefinition.initializeCoordinates(nCells);
        
        double neighborRange = 1.01/density;
        potentialMaster.setRange(neighborRange);
        //find neighbors now.  Don't hook up NeighborListManager since the
        //  neighbors won't change
        potentialMaster.getNeighborManager(box).reset();
        
        integrator = new IntegratorMC(null, random, temperature);
        integrator.setBox(box);
        
        nm = new NormalModes1DHR(space.D());
        nm.setHarmonicFudge(harmonicFudge);
        nm.setTemperature(temperature);
        
        WaveVectorFactory waveVectorFactory = nm.getWaveVectorFactory();
        waveVectorFactory.makeWaveVectors(box);
        
        //nan WAVE VECTOR FACTORY STUFF GOES HERE
        
        
        MCMoveChangeMode convert = new MCMoveChangeMode(potentialMaster, random);
        integrator.getMoveManager().addMCMove(convert);
        convert.setWaveVectors(waveVectorFactory.getWaveVectors());
        convert.setEigenVectors(nm.getEigenvectors(box));
        convert.setCoordinateDefinition(coordinateDefinition);
//        convert.setTemperature(temperature);
        convert.setBox((IBox)box);
        
        integrator.setBox(box);
        potentialMaster.getNeighborManager(box).reset();
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
    	/*
		 * This whole setup defines a set of default parameters
		 * in the inner class Sim1DHRParams.  These parameters can be changed
		 * individually in an appropriately named file, without affecting
		 * the values of the other parameters.  The order of definition in the
		 * file is irrelevant.
		 * 
		 */
		Sim1DHRParams params = new Sim1DHRParams();
		String inputFilename = null;
		if(args.length > 0){
			inputFilename = args[0];
		}
        if(inputFilename != null){
        	ReadParameters readParameters = new ReadParameters(inputFilename, params);
        	readParameters.readParameters();
        }
        double density = params.density;
        double numSteps = params.numSteps;
        int numAtoms = params.numAtoms;
        double harmonicFudge = params.harmonicFudge;
        double temperature = params.temperature;
        int D = params.D;
        String filename = params.filename;
        if(filename.length() ==0){
        	filename = "normal_modes_1DHR _" + numAtoms;
        }
        String refFileName = args.length>0 ? filename+"_ref" : null;
        
        System.out.println("Running 1D hard rod simulation");
        System.out.println(numAtoms+" atoms at density "+density);
        System.out.println("harmonic fudge: "+harmonicFudge);
        System.out.println((numSteps/1000)+ " total steps of 1000");
        System.out.println("output data to "+filename);
        
        //instantiate simulation
        TestMCMove sim = new TestMCMove(Space.getInstance(D), numAtoms, density, temperature, filename, harmonicFudge);
        ((Controller)sim.getController()).actionPerformed();
        
        
        
        
        
        
        System.out.println("Fini.");
    }

    
    /**
     * Inner class for parameters understood by the class's constructor
     */
    public static class Sim1DHRParams extends ParameterBase {
        public int numAtoms = 32;
        public double density = 0.5;
        public int D = 1;
        public long numSteps = 1000;
        public double harmonicFudge = 1.0;
        public String filename = "HR1D_";
        public double temperature = 1.0;
    }
}