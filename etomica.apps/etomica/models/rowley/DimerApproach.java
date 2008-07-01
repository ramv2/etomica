package etomica.models.rowley;

import java.awt.Color;
import etomica.action.IntegratorDimerApproach;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomTypeLeaf;
import etomica.api.ISpecies;
import etomica.box.Box;
import etomica.data.AccumulatorHistory;
import etomica.data.DataFork;
import etomica.data.DataLogger;
import etomica.data.DataPump;
import etomica.data.DataSourceAtomDistance;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DisplayPlot;
import etomica.graphics.SimulationGraphic;
import etomica.potential.PotentialGroup;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularNonperiodic;
import etomica.space3d.Space3D;
import etomica.units.Calorie;
import etomica.units.Mole;
import etomica.units.Prefix;
import etomica.units.PrefixedUnit;
import etomica.units.Unit;
import etomica.units.UnitRatio;

/*
 * Uses the site-site models of Rowley et al (2006) to reproduce potential-energy plots for dimers 
 * of ethanol or methanol molecules along particular approach routes (please see the paper for details).
 * 
 * Set the last three fields at the end of the file to specify the species, the model (with or without
 * point charges), and the approach route.  
 * 
 * Please note that about half of the approach routes do not generate the correct initial configuration, and 
 * the implementation of the potentials with point charges has yet to be validated.  In other words, this
 * class is still junk.
 * 
 * K.R. Schadel 2008 
 */


public class DimerApproach extends Simulation {
	
	public DimerApproach() {
		
	
		super(Space3D.getInstance());
		
		box = new Box(new BoundaryRectangularNonperiodic(space, random), space);
		addBox(box);
		
		// *************************
		// The Species & Potential
		// *************************
		
		PotentialGroup U_a_b = new PotentialGroup(2, space);
		if (ethanol) {
			species = new SpeciesEthanol(space, pointCharges);
			speciesEthanol = (SpeciesEthanol) species;
			EthanolPotentialHelper.initPotential(space, speciesEthanol, U_a_b, pointCharges);
			
		} else {
			species = new SpeciesMethanol(space, pointCharges);
			speciesMethanol = (SpeciesMethanol) species;
			MethanolPotentialHelper.initPotential(space, speciesMethanol, U_a_b, pointCharges);
		}
		getSpeciesManager().addSpecies(species);
		box.setNMolecules(species, 2); // 2 molecules in box...
		U_a_b.setBox(box);
		potentialMaster = new PotentialMaster(space);
		potentialMaster.addPotential(U_a_b, new ISpecies[] {species,species} );
		
		// *********************
		// The Integrator
		// *********************
		
		dimerApproach = new IntegratorDimerApproach(potentialMaster, space);
		dimerApproach.setBox(box);
		
		// Methods in dimerApproach that must be called
		dimerApproach.setMolecules();
        dimerApproach.setImportantAtoms();
        dimerApproach.setRoute(route);
        
        double [][] params;
        if (ethanol) { 
        	params = EthanolRouteParams.setEthanolParams(route); 
        } else {         
        	params = MethanolRouteParams.setMethanolParams(route); 
        }
        
        dimerApproach.setRouteParams(params);
        
        // The following is required for dataDistances:
        atom_O_A  =  dimerApproach.getAtom_O_A(); 
		atom_aC_A =  dimerApproach.getAtom_aC_A();
		atom_aH_A =  dimerApproach.getAtom_aH_A();
		atom_H1_A =  dimerApproach.getAtom_H1_A();
		
		atom_O_B  =  dimerApproach.getAtom_O_B();
		atom_aC_B =  dimerApproach.getAtom_aC_B();
		atom_aH_B =  dimerApproach.getAtom_aH_B();
		
       
        // This may be called here or in the main method
		// Must be called after above set methods
		dimerApproach.initializeCoordinates();
		
		ActivityIntegrate activityIntegrate = new ActivityIntegrate(dimerApproach);	
		activityIntegrate.setMaxSteps(50);
		activityIntegrate.setSleepPeriod(100);
	
		getController().addAction(activityIntegrate);	
		
	}

	public static void main(String[] string)  {
		
		DimerApproach sim = new DimerApproach();
		
		/*
	     ****************************************************************************
	     ****************************************************************************
         Directives for calculating and storing potential energy
         ****************************************************************************
	     ****************************************************************************
	     */

		meterPE = new MeterPotentialEnergy(sim.potentialMaster);

		meterPE.setBox(sim.box);
		
		DataLogger dataLoggerPE = new DataLogger();
		
		DataFork dataForkPE = new DataFork();

		DataPump dataPumpPE = new DataPump(meterPE, dataForkPE);
		
		dataForkPE.addDataSink(dataLoggerPE);
		
		sim.dimerApproach.addIntervalAction(dataPumpPE); // measure data at each step
		
		dataLoggerPE.setFileName("Potential energy");

		/*
	     ****************************************************************************
	     ****************************************************************************
         Directives for calculating and storing distances  between sites
         ****************************************************************************
	     ****************************************************************************
	     */
	   
        DataSourceAtomDistance  dataDistance1 = new DataSourceAtomDistance(sim.space);
        DataSourceAtomDistance  dataDistance2 = new DataSourceAtomDistance(sim.space);
        DataSourceAtomDistance  dataDistance3 = new DataSourceAtomDistance(sim.space);
        DataSourceAtomDistance  dataDistance4 = new DataSourceAtomDistance(sim.space);
        DataSourceAtomDistance  dataDistanceGrr = new DataSourceAtomDistance(sim.space);
        
        
		dataDistance1.setAtoms(atom_aC_A, atom_aC_B);
		dataDistance2.setAtoms(atom_O_B,  atom_aH_A);
		dataDistance3.setAtoms(atom_O_A,  atom_aH_B);
		dataDistance4.setAtoms(atom_O_A,  atom_O_B);
		dataDistanceGrr.setAtoms(atom_O_B,  atom_H1_A);
	
		DataLogger dataLoggerDistance1 = new DataLogger();
		DataLogger dataLoggerDistance2 = new DataLogger();
		DataLogger dataLoggerDistance3 = new DataLogger();

		DataPump dataPumpDistance1 = new DataPump(dataDistance1, dataLoggerDistance1);
		DataPump dataPumpDistance2 = new DataPump(dataDistance2, dataLoggerDistance2);
		DataPump dataPumpDistance3 = new DataPump(dataDistance3, dataLoggerDistance3);
		
		sim.dimerApproach.addIntervalAction(dataPumpDistance1); // measure data at each step
		sim.dimerApproach.addIntervalAction(dataPumpDistance2); 
		sim.dimerApproach.addIntervalAction(dataPumpDistance3); 

	
		dataLoggerDistance1.setFileName("Distance between alpha carbons");
		dataLoggerDistance2.setFileName("Distance between oxygen (monomer B) and alpha hydrogen (monomer A)");
		dataLoggerDistance3.setFileName("Distance between oxygen (monomer A) and alpha hydrogen (monomer B)");
		
		
		/*
	     ****************************************************************************
	     ****************************************************************************
         Directives for graphics
	        true to run graphics 
            false to not run graphics 
         ****************************************************************************
	     ****************************************************************************
	     */

        if (true) { 
            
            sim.box.getBoundary().setDimensions(sim.space.makeVector(new double[]{40,40,40}));
            
            // *********************
            // The Title
            // *********************
            
            String string1;
            String string2;
            
            if (ethanol) {
            	string1 = "Ethanol ";
            } else {
            	string1 = "Methanol ";
            }
            
            if (pointCharges) {
            	string2 = "with point charges";
            } else {
            	string2 = "without point charges";
            }
            
            String appName = string1 + string2 +  ": Route " + route;
            
            // ****************************************
            // Things that matter more than the title
            // ****************************************
            
            SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, appName, sim.space);
            // The default Paint Interval is too infrequent
            simGraphic.setPaintInterval(sim.box, 1);
            //simGraphic.getDisplayBox(sim.box).setShowBoundary(false);
  
            
            // Create instances of ColorSchemeByType for reference and target simulations
            ColorSchemeByType colorScheme = (ColorSchemeByType) simGraphic.getDisplayBox(sim.box).getColorScheme();
	            
        	
            if (ethanol) {
            	
            	// Create instances of the types of molecular sites
            	
            	IAtomTypeLeaf type_O  = speciesEthanol.getOxygenType();
                IAtomTypeLeaf type_aC = speciesEthanol.getAlphaCarbonType(); 
                IAtomTypeLeaf type_C = speciesEthanol.getCarbonType();
                IAtomTypeLeaf type_aH = speciesEthanol.getAlphaHydrogenType();
                IAtomTypeLeaf type_H  = speciesEthanol.getHydrogenType();
                IAtomTypeLeaf type_X  = speciesEthanol.getXType();
                
                // Set color of each site type for each simulation
                
                colorScheme.setColor(type_O, Color.RED);
                colorScheme.setColor(type_aC, Color.GRAY);
                colorScheme.setColor(type_C, Color.GRAY);
                colorScheme.setColor(type_aH, Color.WHITE);
                colorScheme.setColor(type_H, Color.WHITE);
                colorScheme.setColor(type_X, Color.BLUE);
	        	
            } else {
            	
            	// Create instances of the types of molecular sites
            	
            	IAtomTypeLeaf type_O  = speciesMethanol.getOxygenType();
                IAtomTypeLeaf type_aC = speciesMethanol.getAlphaCarbonType(); 
                IAtomTypeLeaf type_aH = speciesMethanol.getAlphaHydrogenType();
                IAtomTypeLeaf type_H  = speciesMethanol.getHydrogenType();
                IAtomTypeLeaf type_X  = speciesMethanol.getXType();
                
                // Set color of each site type for each simulation
                
                colorScheme.setColor(type_O, Color.RED);
                colorScheme.setColor(type_aC, Color.GRAY);
                colorScheme.setColor(type_aH, Color.WHITE);
                colorScheme.setColor(type_H, Color.WHITE);
                colorScheme.setColor(type_X, Color.BLUE);
            	
            }
           
            /*
    	     ****************************************************************************
    	     ****************************************************************************
             Plotting potential energy vs. distance between alpha carbons
             ****************************************************************************
    	     ****************************************************************************
    	     */
            
        
            AccumulatorHistory energyHistory = new AccumulatorHistory();
            
            // To use distance between alpha carbons as independent variable for plot, rather than the step number
            energyHistory.setTimeDataSource(dataDistance1);
           
            dataForkPE.addDataSink(energyHistory);
            
            DisplayPlot ePlot = new DisplayPlot();
            
            ePlot.setLabel("Potential Energy");
            
            // Create conversion factor to change epsilon values from simulation units to kcal/mol
    		// NOTE: This only works for the graphics, not the dataLogger
    		Unit eUnit = new UnitRatio(new PrefixedUnit(Prefix.KILO, Calorie.UNIT), Mole.UNIT);
            ePlot.setUnit(eUnit);
            
            energyHistory.setDataSink(ePlot.getDataSet().makeDataSink());
            
    		ePlot.setDoLegend(true);
    		
    		ePlot.getPlot().setXRange(0.0, 14.0);
    		
    		ePlot.getPlot().setYRange(-6, 2);
    		
    		simGraphic.add(ePlot);
    		
    		/*
    	     ****************************************************************************
    	     ****************************************************************************
             Plotting potential energy vs. distance between 
             oxygen(monomer B) and hydrogen(2) (monomer A)
             ****************************************************************************
    	     ****************************************************************************
    	     */
            
        
            AccumulatorHistory energyHistoryGrr = new AccumulatorHistory();
            
            // To use distance between alpha carbons as independent variable for plot, rather than the step number
            energyHistoryGrr.setTimeDataSource(dataDistanceGrr);
           
            dataForkPE.addDataSink(energyHistoryGrr);
            
            DisplayPlot ePlotGrr = new DisplayPlot();
            
            ePlotGrr.setLabel("Potential Energy vs. rOH");
            
            energyHistoryGrr.setDataSink(ePlotGrr.getDataSet().makeDataSink());
            
    		ePlotGrr.setDoLegend(true);
    		
    		simGraphic.add(ePlotGrr);
            
            simGraphic.makeAndDisplayFrame(appName);
            
            return;
            
        } else {
        	// this method is called when graphics are used and the start button is pressed 
        	// must be included here:
    		sim.getController().actionPerformed();
        }
	}
	
	
	public final static long serialVersionUID = 1L;
	public ISpecies species;
	public static SpeciesEthanol speciesEthanol;
	public static SpeciesMethanol speciesMethanol;
	public Box box;
	public PotentialMaster potentialMaster;
	public IntegratorDimerApproach dimerApproach;
	public static MeterPotentialEnergy meterPE;
	
	
	static IAtomPositioned atom_O_A;
    static IAtomPositioned atom_aC_A;
    static IAtomPositioned atom_aH_A;
    static IAtomPositioned atom_H1_A; 
    
    static IAtomPositioned atom_O_B;
    static IAtomPositioned atom_aC_B;
    static IAtomPositioned atom_aH_B;
    
    // True to consider ethanol, false to consider methanol
    static boolean ethanol = false;
    
    // True to use model with point charges, false to use model without point charges
    public final static boolean pointCharges = false;
    
    // ID of approach route (see Rowley et al (2006) for table)
    static int route = 15;

}