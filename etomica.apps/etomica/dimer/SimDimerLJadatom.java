package etomica.dimer;

import java.io.FileWriter;
import java.io.IOException;

import etomica.action.BoxImposePbc;
import etomica.action.BoxInflate;
import etomica.action.WriteConfiguration;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAction;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IAtomTypeSphere;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.atom.AtomArrayList;
import etomica.box.Box;
import etomica.chem.elements.Tin;
import etomica.config.Configuration;
import etomica.config.ConfigurationFile;
import etomica.config.ConfigurationLattice;
import etomica.data.AccumulatorAverageCollapsing;
import etomica.data.AccumulatorHistory;
import etomica.data.DataPump;
import etomica.data.AccumulatorAverage.StatType;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DisplayBox;
import etomica.graphics.DisplayPlot;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.lattice.LatticeCubicFcc;
import etomica.potential.P2LennardJones;
import etomica.potential.PotentialMaster;
import etomica.potential.PotentialMasterMonatomic;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularSlit;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Kelvin;
import etomica.util.HistoryCollapsingAverage;
import etomica.util.RandomNumberGenerator;

/**
 * Simulation using Henkelman's Dimer method to find a saddle point for
 * an adatom on a surface, modeled with LJ.
 * 
 * @author msellers
 *
 */

public class SimDimerLJadatom extends Simulation{

    private static final long serialVersionUID = 1L;
    private static final String APP_NAME = "DimerLJadatom";
    public final PotentialMaster potentialMaster;
    public IntegratorVelocityVerlet integratorMD;
    public IntegratorDimerRT integratorDimer;
    public IntegratorDimerMin integratorDimerMin;
    public IBox box;
    public IVector [] saddle, normal;
    public SpeciesSpheresMono fixed, movable;
//    public P2LennardJones potential;
    public ActivityIntegrate activityIntegrateMD, activityIntegrateDimer, activityIntegrateMin;
    public CalcGradientDifferentiable calcGradientDifferentiable;
    public CalcVibrationalModes calcVibrationalModes;
    public double [][] dForces;
    public int [] d, modeSigns;
    public double [] positions;
    public double [] lambdas, frequencies;
    public IAtomSet movableSet;
    public IVector adAtomPos;
    public Boolean saddleFine, calcModes, minSearch, normalDir;
    

    public SimDimerLJadatom() {
    	super(Space3D.getInstance(), true);
    	potentialMaster = new PotentialMasterMonatomic(this, space);
    	
    //SIMULATION BOX
        box = new Box(new BoundaryRectangularSlit(random, 0, 5, space), space);
        addBox(box);
        
    //SPECIES
    	double sigma = 1.0;
    	Tin tinFixed = new Tin("SnFixed", Double.POSITIVE_INFINITY);
    	fixed = new SpeciesSpheresMono(this, space, tinFixed);
        movable = new SpeciesSpheresMono(this, space);      
        getSpeciesManager().addSpecies(fixed);
        getSpeciesManager().addSpecies(movable);
        ((IAtomTypeSphere)fixed.getLeafType()).setDiameter(sigma);
        ((IAtomTypeSphere)movable.getLeafType()).setDiameter(sigma);
    	
        // Must be in same order as the respective species is added to SpeciesManager
        box.setNMolecules(fixed, 256);    	
    	
        BoxInflate inflater = new BoxInflate(box, space);
        inflater.setTargetDensity(1);
        inflater.actionPerformed();
    	
//    	potential = new P2LennardJones(space, sigma, 1.0);
//		potentialMaster.addPotential(, new IAtomTypeLeaf[]{fixed.getLeafType(), fixed.getLeafType()});
		potentialMaster.addPotential(new P2LennardJones(space, sigma, 1.0), new IAtomTypeLeaf[]{movable.getLeafType(), fixed.getLeafType()});
		potentialMaster.addPotential(new P2LennardJones(space, sigma, 1.0), new IAtomTypeLeaf[]{movable.getLeafType(), movable.getLeafType()});
        
    //CRYSTAL
        Configuration config = new ConfigurationLattice(new LatticeCubicFcc(space), space);
        config.initializeCoordinates(box); 
       
        //ADATOM CREATION AND PLACEMENT
        
        IMolecule iMolecule = movable.makeMolecule();
        box.addMolecule(iMolecule);
        adAtomPos = ((IAtomPositioned)iMolecule.getChildList().getAtom(0)).getPosition();
        //adAtomPos = getSpace().makeVector();
        adAtomPos.setX(0, 3.5);
        adAtomPos.setX(1, -0.30);
        adAtomPos.setX(2, -0.30);
        IVector newBoxLength = space.makeVector();
        newBoxLength.E(box.getBoundary().getDimensions());
        newBoxLength.setX(0, 2.0*adAtomPos.x(0)+2.0);
        box.getBoundary().setDimensions(newBoxLength);

    }
    
    public void setMovableAtoms(double distance, IVector center){
        //distance = distance*distance;
        IVector rij = space.makeVector();
        AtomArrayList movableList = new AtomArrayList();
        IAtomSet loopSet = box.getMoleculeList();
        for (int i=0; i<loopSet.getAtomCount(); i++){
            rij.Ev1Mv2(center,((IAtomPositioned)((IMolecule)loopSet.getAtom(i)).getChildList().getAtom(0)).getPosition());
            if(rij.x(0) > (box.getBoundary().getDimensions().x(0) - 3.0)){continue;}
            //box.getBoundary().nearestImage(rij);
            if(rij.squared() < distance){
               movableList.add(loopSet.getAtom(i));
            } 
        }
        for (int i=0; i<movableList.getAtomCount(); i++){
            IMolecule newMolecule = movable.makeMolecule();
            box.addMolecule(newMolecule);
            ((IAtomPositioned)newMolecule.getChildList().getAtom(0)).getPosition().E(((IAtomPositioned)((IMolecule)movableList.getAtom(i)).getChildList().getAtom(0)).getPosition());
            box.removeMolecule((IMolecule)movableList.getAtom(i));
        }
        movableSet = box.getMoleculeList(movable);
    }

    
    //Must be run after setMovableAtoms
    public void removeAtoms(double distance, IVector center){
        distance = distance*distance;
        IVector rij = space.makeVector();
        
        IAtomSet loopSet = box.getMoleculeList(movable);
        for (int i=0; i<loopSet.getAtomCount(); i++){
            rij.Ev1Mv2(center,((IAtomPositioned)((IMolecule)loopSet.getAtom(i)).getChildList().getAtom(0)).getPosition());
            box.getBoundary().nearestImage(rij);
            if(rij.squared() < distance){
               box.removeMolecule((IMolecule)loopSet.getAtom(i));
            } 
        }   
    }
    
    public void initializeConfiguration(String fileName){
        ConfigurationFile config = new ConfigurationFile(fileName);
        config.initializeCoordinates(box);
    }
    
    public void generateConfigs(String fileName, double percentd){       
        
        RandomNumberGenerator random = new RandomNumberGenerator();
        IVector workVector = space.makeVector();
        IVector [] currentPos = new IVector [movableSet.getAtomCount()];
        for(int i=0; i<currentPos.length; i++){
            currentPos[i] = space.makeVector();
            currentPos[i].E(((IAtomPositioned)((IMolecule)movableSet.getAtom(i)).getChildList().getAtom(0)).getPosition());
        }
        
        //Create multiple configurations
        for(int m=0; m<50; m++){
            WriteConfiguration genConfig = new WriteConfiguration(space);
            genConfig.setBox(box);
            genConfig.setConfName(fileName+"_config_"+m);
            //Displaces atom's by at most +/-0.03 in each coordinate
            for(int i=0; i<movableSet.getAtomCount(); i++){
                IVector atomPosition = ((IAtomPositioned)((IMolecule)movableSet.getAtom(i)).getChildList().getAtom(0)).getPosition();
                for(int j=0; j<3; j++){
                    workVector.setX(j,percentd*random.nextGaussian());
                }
                atomPosition.Ev1Pv2(currentPos[i],workVector);
            }
            genConfig.actionPerformed();            
        }
    }
    
    public IAction calculateVibrationalModes(){
        /*
        String file = fileName;
        ConfigurationFile configFile = new ConfigurationFile(file);
        configFile.initializeCoordinates(box); 
        */
        System.out.println(" ***Vibrational Normal Mode Analysis***");
        System.out.println("  -Reading in system coordinates...");
        calcGradientDifferentiable = new CalcGradientDifferentiable(box, potentialMaster, movableSet, space);
        d = new int[movableSet.getAtomCount()*3];
        positions = new double[d.length];
        dForces = new double[positions.length][positions.length];
        
        // setup position array
        for(int i=0; i<movableSet.getAtomCount(); i++){
            for(int j=0; j<3; j++){
                positions[(3*i)+j] = ((IAtomPositioned)((IMolecule)movableSet.getAtom(i)).getChildList().getAtom(0)).getPosition().x(j);
            }
        }
        // fill dForces array
        for(int l=0; l<d.length; l++){
            d[l] = 1;
            System.arraycopy(calcGradientDifferentiable.df2(d, positions), 0, dForces[l], 0, d.length);
            System.out.println("  -Calculating force constant row "+l+"...");
            d[l] = 0;
        }
        calcVibrationalModes = new CalcVibrationalModes(dForces,movable.getLeafType().getElement().getMass());
        modeSigns = new int[3];
    
        // calculate vibrational modes and frequencies
        System.out.println("  -Calculating lambdas...");
        lambdas = calcVibrationalModes.getLambdas();
        System.out.println("  -Calculating frequencies...");
        frequencies = calcVibrationalModes.getFrequencies();
        modeSigns = calcVibrationalModes.getModeSigns();
        System.out.println("  -Writing data...");
        // output data
        FileWriter writer;
        //LAMBDAS
        try { 
            writer = new FileWriter("_lambdas");
            for(int i=0; i<lambdas.length; i++){
                writer.write(lambdas[i]+"\n");
            }
            writer.close();
        }catch(IOException e) {
            System.err.println("Cannot open file, caught IOException: " + e.getMessage());
        }
        //FREQUENCIES
        try { 
            writer = new FileWriter("_frequencies");
            for(int i=0; i<frequencies.length; i++){
                writer.write(frequencies[i]+"\n");
            }
            writer.close();
        }catch(IOException e) {
            System.err.println("Cannot open file, caught IOException: " + e.getMessage());
        }
        //MODE INFO
        try { 
            writer = new FileWriter("_modeSigns");
            writer.write(modeSigns[0]+" positive modes"+"\n");
            writer.write(modeSigns[1]+" negative modes"+"\n");
            writer.write(modeSigns[2]+" total modes"+"\n");
            writer.close();
        }catch(IOException e) {
            System.err.println("Cannot open file, caught IOException: " + e.getMessage());
        }
        System.out.println("Done.");
        return null;
    }
    
    public void enableMolecularDynamics(long maxSteps){
        integratorMD = new IntegratorVelocityVerlet(this, potentialMaster, space);
        integratorMD.setTimeStep(0.01);
        integratorMD.setTemperature(0.1);
        integratorMD.setThermostatInterval(100);
        integratorMD.setIsothermal(true);
        integratorMD.setBox(box);
        activityIntegrateMD = new ActivityIntegrate(integratorMD);
        BoxImposePbc imposePbc = new BoxImposePbc(box, space);
        integratorMD.addIntervalAction(imposePbc);
        getController().addAction(activityIntegrateMD);
        activityIntegrateMD.setMaxSteps(maxSteps);
    }
    
    public void enableDimerSearch(String fileName, long maxSteps, Boolean orthoSearch, Boolean fine){
        
        integratorDimer = new IntegratorDimerRT(this, potentialMaster, new ISpecies[]{movable}, space);
        integratorDimer.setBox(box);
        integratorDimer.setOrtho(orthoSearch, false);
        if(fine){
            ConfigurationFile configFile = new ConfigurationFile(fileName+"_saddle");
            configFile.initializeCoordinates(box);
            
            integratorDimer.setFileName(fileName+"_fine");
            integratorDimer.deltaR = 0.0005;
            integratorDimer.dXl = 10E-5;       
            integratorDimer.deltaXmax = 0.005;
            integratorDimer.dFsq = 0.0001*0.0001;
            integratorDimer.dFrot = 0.01;
        }
        integratorDimer.setFileName(fileName);
       activityIntegrateDimer = new ActivityIntegrate(integratorDimer);
        integratorDimer.setActivityIntegrate(activityIntegrateDimer);
        getController().addAction(activityIntegrateDimer);
        activityIntegrateDimer.setMaxSteps(maxSteps);
    }
        
    public void enableMinimumSearch(String fileName, Boolean normalDir){
        
        integratorDimerMin = new IntegratorDimerMin(this, potentialMaster, new ISpecies[]{movable}, fileName, normalDir, space);
        integratorDimerMin.setBox(box);
        activityIntegrateMin = new ActivityIntegrate(integratorDimerMin);
        integratorDimerMin.setActivityIntegrate(activityIntegrateMin);
        getController().addAction(activityIntegrateMin);
    }
    
    public static void main(String[] args){
       
        final SimDimerLJadatom sim = new SimDimerLJadatom();
        IVector vect = sim.getSpace().makeVector();
        vect.setX(0, 3.5);
        vect.setX(1, 0.0);
        vect.setX(2, 0.0);
        
        //sim.initializeConfiguration("sns-00_B_minimum");
        
        sim.setMovableAtoms(12.0, vect);
        //sim.removeAtoms(2.9, vect);
        
        sim.enableDimerSearch("sns-test1", 500, true, false);
        sim.integratorDimer.setRotNum(20);
        sim.enableMinimumSearch("sns-test1", false);
        
        MeterPotentialEnergy energyMeter = new MeterPotentialEnergy(sim.potentialMaster);
        energyMeter.setBox(sim.box);
        AccumulatorHistory energyAccumulator = new AccumulatorHistory(new HistoryCollapsingAverage());
        AccumulatorAverageCollapsing accumulatorAveragePE = new AccumulatorAverageCollapsing();
        DataPump energyPump = new DataPump(energyMeter,accumulatorAveragePE);       
        accumulatorAveragePE.addDataSink(energyAccumulator, new StatType[]{StatType.MOST_RECENT});
        DisplayPlot plotPE = new DisplayPlot();
        plotPE.setLabel("PE Plot");
        energyAccumulator.setDataSink(plotPE.getDataSet().makeDataSink());
        accumulatorAveragePE.setPushInterval(1);      
        sim.integratorDimer.addIntervalAction(energyPump);
        sim.integratorDimer.setActionInterval(energyPump,1);
        
        SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, APP_NAME,1, sim.getSpace(), sim.getController());
        simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(sim.box));
        simGraphic.add(plotPE);
        
        sim.integratorDimerMin.addIntervalAction(simGraphic.getPaintAction(sim.box));
        sim.integratorDimer.addIntervalAction(simGraphic.getPaintAction(sim.box));

        ColorSchemeByType colorScheme = ((ColorSchemeByType)((DisplayBox)simGraphic.displayList().getFirst()).getColorScheme());
        
        colorScheme.setColor(sim.fixed.getLeafType(),java.awt.Color.gray);
        colorScheme.setColor(sim.movable.getLeafType(),java.awt.Color.red);

        simGraphic.makeAndDisplayFrame(APP_NAME);
    }

}
