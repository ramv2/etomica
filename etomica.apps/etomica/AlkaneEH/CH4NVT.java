package etomica.AlkaneEH;

import java.awt.Color;

import etomica.action.BoxImposePbc;
import etomica.action.activity.ActivityIntegrate;
import etomica.action.activity.Controller;
import etomica.api.IAtomType;
import etomica.api.IBox;
import etomica.api.IPotentialMaster;
import etomica.api.ISpecies;
import etomica.atom.DiameterHashByType;
import etomica.atom.iterator.ApiBuilder;
import etomica.box.Box;
import etomica.config.ConfigurationLattice;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorAverageFixed;
import etomica.data.DataPump;
import etomica.data.meter.MeterPotentialEnergyFromIntegrator;
import etomica.data.meter.MeterPressure;
import etomica.data.types.DataDouble;
import etomica.data.types.DataGroup;
import etomica.graphics.DisplayBox;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorMC;
import etomica.integrator.mcmove.MCMoveMolecule;
import etomica.integrator.mcmove.MCMoveRotateMolecule3D;
import etomica.lattice.LatticeCubicBcc;
import etomica.listener.IntegratorListenerAction;
import etomica.potential.P2LennardJones;
import etomica.potential.P2SoftSphericalTruncated;
import etomica.potential.PotentialGroup;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.units.Kelvin;
import etomica.units.Pixel;
import etomica.util.Constants;
import etomica.util.ParameterBase;
import etomica.util.ParseArgs;

/**
 * CH4(TraPPE-EH)
 * Canonical ensemble Monte Carlo simulation (NVT)
 * @author shu
 * Aug 7 2013
 */

public class CH4NVT extends Simulation {

	private static final long serialVersionUID = 1L;
	protected final SpeciesMethane speciesCH4; 
	protected final IPotentialMaster potentialMaster;
	protected final IntegratorMC integrator;
	protected final MCMoveMolecule moveMolecule;//translation mc move
	protected final MCMoveRotateMolecule3D rotateMolecule;//rotation mc move
	protected final IBox box;
	private final static String APP_NAME = "methane, TraPPE-EH model";
	private static final int PIXEL_SIZE = 15;
	public final ActivityIntegrate activityIntegrate;
    public Controller controller; //control the simulation process

	//************************************* constructor ********************************************//
	public CH4NVT(Space space, int numberMolecules, double boxSize, double temperature){
		
		super(space);		
		//setRandom(new RandomNumberGenerator(1));
		speciesCH4 = new SpeciesMethane(space);
		addSpecies(speciesCH4);
		box = new Box(space);
		addBox(box);
		box.setNMolecules(speciesCH4, numberMolecules);
		box.getBoundary().setBoxSize(space.makeVector(new double[]{boxSize,boxSize,boxSize}));
		potentialMaster = new PotentialMaster();
		integrator = new IntegratorMC(this, potentialMaster);
		
		
		//CH4 potential
        double sigmaH = 3.31;// "middle point of CH bond"
        double sigmaC = 3.31;
        double sigmaCH = (sigmaH+sigmaC)/2;
        double epsilonH = Kelvin.UNIT.toSim(15.3);
        double epsilonC = Kelvin.UNIT.toSim(0.01);
        double epsilonCH = Math.sqrt((epsilonH * epsilonC ));
        P2LennardJones p2H = new P2LennardJones(space, sigmaH, epsilonH);// H-H
        P2LennardJones p2C = new P2LennardJones(space, sigmaC, epsilonC);//C-C
        P2LennardJones p2CH = new P2LennardJones(space, sigmaCH, epsilonCH);//C-H
        double truncationRadius = 20;
        P2SoftSphericalTruncated potentialTruncated_2H = new P2SoftSphericalTruncated(space, p2H, truncationRadius);
        P2SoftSphericalTruncated potentialTruncated_2C = new P2SoftSphericalTruncated(space, p2C, truncationRadius);
        P2SoftSphericalTruncated potentialTruncated_2CH = new P2SoftSphericalTruncated(space, p2CH, truncationRadius);

        PotentialGroup pCH4 = new PotentialGroup(2);
	    
        IAtomType typeC = speciesCH4.getAtomType(0);
        IAtomType typeH = speciesCH4.getAtomType(1);

        // build methane potential
        pCH4.addPotential(potentialTruncated_2C, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{typeC, typeC}));//C-C
        pCH4.addPotential(potentialTruncated_2CH, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{typeC, typeH}));//C-H
        pCH4.addPotential(potentialTruncated_2CH, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{typeH, typeC }));//H-C
        pCH4.addPotential(potentialTruncated_2H, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{typeH, typeH}));//H-H
        
        //add potential to potential master 
        potentialMaster.addPotential(pCH4, new ISpecies[]{speciesCH4,speciesCH4});
        moveMolecule = new MCMoveMolecule(this, potentialMaster, space);//stepSize:1.0, stepSizeMax:15.0
        rotateMolecule = new MCMoveRotateMolecule3D(potentialMaster,random,space);

		activityIntegrate = new ActivityIntegrate(integrator);
        activityIntegrate.setMaxSteps(10000000);
		getController().addAction(activityIntegrate);

		//******************************** periodic boundary condition ******************************** //
		BoxImposePbc imposePbc = new BoxImposePbc(box, space);
		imposePbc.setApplyToMolecules(true);
		//**************************** integrator ****************************** //
		integrator.setTemperature(temperature);
	    integrator.setBox(box);
        integrator.getMoveManager().addMCMove(moveMolecule);
        integrator.getMoveManager().addMCMove(rotateMolecule);
		integrator.getEventManager().addListener(new IntegratorListenerAction(imposePbc));
		
		//******************************** initial configuration ******************************** //
		LatticeCubicBcc lattice = new LatticeCubicBcc(space);
		ConfigurationLattice configuration = new ConfigurationLattice(lattice, space);
		configuration.initializeCoordinates(box);
		
	}
		
	// **************************** simulation part **************************** //
	public static void main (String[] args){
		
		Param params = new Param();
		if (args.length > 0) {
			ParseArgs.doParseArgs(params, args);
		} else {
			
		}
		Space space = Space3D.getInstance();
		int steps = params.steps;
		boolean isGraphic = params.isGraphic;
		double temperatureK = params.temperatureK;
		double temperature = Kelvin.UNIT.toSim(temperatureK);// convert Kelvin temperature to T(sim), essentially kT
		int numberMolecules = params.numberMolecules;
		double density = params.density;// in mol/L
		double densitySim = density * Constants.AVOGADRO * 1e-27;  // convert density to sim unit; in 1/(A)^3
		System.out.println("density="+density+"mol/L,denisty(sim)="+densitySim+",temperature="+temperatureK+" Kelvin,temperature(sim)="+temperature);
		double boxSize = Math.pow(numberMolecules / densitySim, (1.0/3.0)); 
		System.out.println("box size:"+boxSize+",number of molecules="+numberMolecules);
		System.out.println("******************* methane,TraPPE-EH model, NVT********************");
		final CH4NVT sim = new CH4NVT(space,numberMolecules,boxSize,temperature);
		
    	if (isGraphic){
			SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, space, sim.getController());
		    simGraphic.getDisplayBox(sim.box).setPixelUnit(new Pixel(PIXEL_SIZE));
	        simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(sim.box));
	        //***********************************  set diameters  ******************************************************
	        ((DiameterHashByType)((DisplayBox)simGraphic.displayList().getFirst()).getDiameterHash()).setDiameter(sim.speciesCH4.getCType(),1);
			simGraphic.makeAndDisplayFrame(APP_NAME);
			simGraphic.getDisplayBox(sim.box).repaint();
	    	return ;
	    	
		}
    	
    	System.out.println("no graphic simulation involved");
   		sim.activityIntegrate.setMaxSteps(steps/10);// equilibration period
   		sim.getController().actionPerformed();
   		sim.getController().reset();
   		sim.integrator.getMoveManager().setEquilibrating(false);
   		System.out.println("equilibration finished");
    		
    	// pressure
        MeterPressure pMeter = new MeterPressure(sim.space);
        pMeter.setIntegrator(sim.integrator);
        AccumulatorAverage pAccumulator = new AccumulatorAverageFixed(10);
        DataPump pPump = new DataPump(pMeter,pAccumulator);
        IntegratorListenerAction pumpListener = new IntegratorListenerAction(pPump);
        pumpListener.setInterval(2*numberMolecules);//block nbr=steps/(10*2*N)
        sim.integrator.getEventManager().addListener(pumpListener);
        
        MeterPotentialEnergyFromIntegrator energyMeter = new MeterPotentialEnergyFromIntegrator(sim.integrator);
        AccumulatorAverage energyAccumulator = new AccumulatorAverageFixed(10);
        DataPump energyManager = new DataPump(energyMeter, energyAccumulator);
        energyAccumulator.setBlockSize(50);
        sim.integrator.getEventManager().addListener(new IntegratorListenerAction(energyManager));
        sim.activityIntegrate.setMaxSteps(steps);// equilibration period
        sim.getController().actionPerformed();
        
        // compressibility factor Z=P/rho/T(all in sim units)
        double Z = ((DataDouble)((DataGroup)pAccumulator.getData()).getData(pAccumulator.AVERAGE.index)).x*sim.box.getBoundary().volume()/(sim.box.getMoleculeList().getMoleculeCount()*sim.integrator.getTemperature());
        double Zerr = ((DataDouble)((DataGroup)pAccumulator.getData()).getData(pAccumulator.ERROR.index)).x*sim.box.getBoundary().volume()/(sim.box.getMoleculeList().getMoleculeCount()*sim.integrator.getTemperature());
        double Zblock_correlation = ((DataDouble)((DataGroup)pAccumulator.getData()).getData(pAccumulator.BLOCK_CORRELATION.index)).x;
        double avgPE = ((DataDouble)((DataGroup)energyAccumulator.getData()).getData(energyAccumulator.AVERAGE.index)).x;
        avgPE /= numberMolecules;
        System.out.println("Z="+Z+" Zerr="+Zerr +"Z block correlation="+Zblock_correlation);
        System.out.println("PE/epsilon="+avgPE);
    	
	}
	
	// ******************* parameters **********************// 
	public static class Param extends ParameterBase {
		public boolean isGraphic = false;
		public double temperatureK = 300.0;
		public int numberMolecules = 500;
		public double density = 0.5; // in mol/L
		public int steps = 100000;
	    	
	}
}
