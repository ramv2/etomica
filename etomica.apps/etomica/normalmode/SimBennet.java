package etomica.normalmode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAction;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.box.Box;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorAverageFixed;
import etomica.data.AccumulatorHistogram;
import etomica.data.DataFork;
import etomica.data.DataLogger;
import etomica.data.DataPump;
import etomica.data.DataTableWriter;
import etomica.data.IEtomicaDataSource;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.integrator.IntegratorMC;
import etomica.lattice.crystal.Basis;
import etomica.lattice.crystal.BasisCubicFcc;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.potential.P2SoftSphere;
import etomica.potential.P2SoftSphericalTruncatedShifted;
import etomica.potential.Potential2SoftSpherical;
import etomica.potential.PotentialMasterMonatomic;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.species.SpeciesSpheresMono;
import etomica.util.DoubleRange;
import etomica.util.HistogramSimple;
import etomica.util.ParameterBase;
import etomica.util.ReadParameters;

/**
 * Simulation to sample Bennet's Overlap Region
 */
public class SimBennet extends Simulation {

	private static final String APP_NAME = "Sim Bennet's";

    public SimBennet(Space _space, int numAtoms, double density, double temperature, String filename, int exponent) {
        super(_space, true);

        String refFileName = filename +"_ref";
        FileReader refFileReader;
        try {
        	refFileReader = new FileReader(refFileName);
        } catch (IOException e){
        	throw new RuntimeException ("Cannot find refPref file!! "+e.getMessage() );
        }
        try {
        	BufferedReader bufReader = new BufferedReader(refFileReader);
        	String line = bufReader.readLine();
        	
        	refPref = Double.parseDouble(line);
        	setRefPref(refPref);
        	
        } catch (IOException e){
        	throw new RuntimeException(" Cannot read from file "+ refFileName);
        }
        //System.out.println("refPref is: "+ refPref);
        
        
        int D = space.D();
        
        potentialMasterMonatomic = new PotentialMasterMonatomic(this);
        integrator = new IntegratorMC(potentialMasterMonatomic, getRandom(), temperature);
       
        species = new SpeciesSpheresMono(this, space);
        getSpeciesManager().addSpecies(species);

        //Target        
        box = new Box(space);
        addBox(box);
        box.setNMolecules(species, numAtoms);

        activityIntegrate = new ActivityIntegrate(integrator);
        getController().addAction(activityIntegrate);
      
       	double L = Math.pow(4.0/density, 1.0/3.0);
        primitive = new PrimitiveCubic(space, L);
        int n = (int)Math.round(Math.pow(numAtoms/4, 1.0/3.0));
        nCells = new int[]{n,n,n};
        boundary = new BoundaryRectangularPeriodic(space, n*L);
        basis = new BasisCubicFcc();
        
        box.setBoundary(boundary);
        
        coordinateDefinition = new CoordinateDefinitionLeaf(this, box, primitive, basis, space);
        normalModes = new NormalModesFromFile(filename, D);
        /*
         * nuke this line when it is derivative-based
         */
        normalModes.setTemperature(temperature);
        coordinateDefinition.initializeCoordinates(nCells);
        
        Potential2SoftSpherical potential = new P2SoftSphere(space, 1.0, 1.0, exponent);
        double truncationRadius = boundary.getDimensions().x(0) * 0.495;
        P2SoftSphericalTruncatedShifted pTruncated = new P2SoftSphericalTruncatedShifted(space, potential, truncationRadius);
        IAtomTypeLeaf sphereType = species.getLeafType();
        potentialMasterMonatomic.addPotential(pTruncated, new IAtomTypeLeaf[] { sphereType, sphereType });
        
        
        integrator.setBox(box);
        
        potentialMasterMonatomic.lrcMaster().setEnabled(false);
        MeterPotentialEnergy meterPE = new MeterPotentialEnergy(potentialMasterMonatomic);
        meterPE.setBox(box);
        latticeEnergy = meterPE.getDataAsScalar();
        
        meterHarmonicEnergy = new MeterHarmonicEnergy(coordinateDefinition, normalModes);
        meterHarmonicEnergy.setBox(box);
        
        MCMoveAtomCoupledBennet move = new MCMoveAtomCoupledBennet(potentialMasterMonatomic, getRandom(), 
        		coordinateDefinition, normalModes, getRefPref(), space);
        move.setTemperature(temperature);
        move.setLatticeEnergy(latticeEnergy);
        integrator.getMoveManager().addMCMove(move);

        
    }


	public double getRefPref() {
		return refPref;
	}

	public void setRefPref(double refPref) {
		this.refPref = refPref;
	}
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        SimBennetParam params = new SimBennetParam();
        String inputFilename = null;
        if (args.length > 0) {
            inputFilename = args[0];
        }
        if (inputFilename != null) {
            ReadParameters readParameters = new ReadParameters(inputFilename, params);
            readParameters.readParameters();
        }
        double density = params.density/1000;
        int exponentN = params.exponentN;
        long numSteps = params.numSteps;
        int numAtoms = params.numMolecules;
        double temperature = params.temperature;
        double harmonicFudge = params.harmonicFudge;
        int D = params.D;
        String filename = params.filename;
        if (filename.length() == 0) {
        	System.err.println("Need input files!!!");
            filename = "CB_FCC_n"+exponentN+"_T"+ (int)Math.round(temperature*10);
        }
       
    	
        System.out.println("Running "+(D==1 ? "1D" : (D==3 ? "FCC" : "2D hexagonal")) +" soft sphere Bennet's overlap perturbation simulation");
        System.out.println(numAtoms+" atoms at density "+density+" and temperature "+temperature);
        System.out.println("exponent N: "+ exponentN );
        System.out.println("total steps: "+ numSteps);
        System.out.println("output data to "+filename);
        
        //construct simulation
        final SimBennet sim = new SimBennet(Space.getInstance(D), numAtoms, density, temperature, filename, exponentN);
        
        
        
        IEtomicaDataSource[] workMeters = new IEtomicaDataSource[2];
        
      // Bennet's Overlap ---> Harmonic
        MeterWorkBennetHarmonic meterWorkBennetHarmonic = new MeterWorkBennetHarmonic(sim.integrator, sim.meterHarmonicEnergy);
        meterWorkBennetHarmonic.setRefPref(sim.refPref);
        meterWorkBennetHarmonic.setLatticeEnergy(sim.latticeEnergy);
        workMeters[0] = meterWorkBennetHarmonic;
        
        DataFork dataForkHarmonic = new DataFork();
        DataPump pumpHarmonic = new DataPump(workMeters[0], dataForkHarmonic);
        
        final AccumulatorAverageFixed dataAverageHarmonic = new AccumulatorAverageFixed();
        
        dataForkHarmonic.addDataSink(dataAverageHarmonic);
        sim.integrator.addIntervalAction(pumpHarmonic);
        sim.integrator.setActionInterval(pumpHarmonic, 1);
        
        //Histogram Harmonic
        final AccumulatorHistogram histogramHarmonic = new AccumulatorHistogram(new HistogramSimple(1500, new DoubleRange(-50,100)));
        dataForkHarmonic.addDataSink(histogramHarmonic);
        
        
      //Bennet's Overlap ---> Target
        MeterWorkBennetTarget meterWorkBennetTarget = new MeterWorkBennetTarget(sim.integrator, sim.meterHarmonicEnergy);
        meterWorkBennetTarget.setRefPref(sim.refPref);
        meterWorkBennetTarget.setLatticeEnergy(sim.latticeEnergy);
        workMeters[1] = meterWorkBennetTarget;
        
        DataFork dataForkTarget = new DataFork();
        DataPump pumpTarget = new DataPump(workMeters[1], dataForkTarget);
        
        final AccumulatorAverageFixed dataAverageTarget = new AccumulatorAverageFixed();

        dataForkTarget.addDataSink(dataAverageTarget);
        sim.integrator.addIntervalAction(pumpTarget);
        sim.integrator.setActionInterval(pumpTarget, numAtoms*2);

        //Histogram Target
        final AccumulatorHistogram histogramTarget = new AccumulatorHistogram(new HistogramSimple(1500, new DoubleRange(-50,100)));
        dataForkTarget.addDataSink(histogramTarget);
        

        
        /*
         * 
         */
        FileWriter fileWriter;
        
        try{
        	fileWriter = new FileWriter(filename + "_SimBen");
        } catch(IOException e){
        	fileWriter = null;
        }
        
        final String outFileName = filename;
        final FileWriter fileWriterSimBen = fileWriter;
        
        IAction outputAction = new IAction(){
        	public void actionPerformed(){
        		long idStep = sim.integrator.getStepCount();
        		 //Target
        		DataLogger dataLogger1 = new DataLogger();
        		DataTableWriter dataTableWriter1 = new DataTableWriter();
        		dataLogger1.setFileName(outFileName + "_hist_BennTarg");
        		dataLogger1.setDataSink(dataTableWriter1);
        		dataTableWriter1.setIncludeHeader(false);
        		dataLogger1.putDataInfo(histogramTarget.getDataInfo());
        		
        		dataLogger1.setWriteInterval(1);
        		dataLogger1.setAppending(false);
        		dataLogger1.putData(histogramTarget.getData());
        		dataLogger1.closeFile();
                
        		
        		//Harmonic
        		DataLogger dataLogger2 = new DataLogger();
        		DataTableWriter dataTableWriter2 = new DataTableWriter();
        		dataLogger2.setFileName(outFileName + "_hist_BennHarm");
        		dataLogger2.setDataSink(dataTableWriter2);
        		dataTableWriter2.setIncludeHeader(false);
        		dataLogger2.putDataInfo(histogramHarmonic.getDataInfo());
        		
        		dataLogger2.setWriteInterval(1);
        		dataLogger2.setAppending(false);
        		dataLogger2.putData(histogramHarmonic.getData());
        		dataLogger2.closeFile();
                /*
                 * 
                 */
                
                double wHarmonic = dataAverageHarmonic.getData().getValue(AccumulatorAverage.StatType.AVERAGE.index);
                double wTarget   = dataAverageTarget.getData().getValue(AccumulatorAverage.StatType.AVERAGE.index);
                
                double eHarmonic = dataAverageHarmonic.getData().getValue(AccumulatorAverage.StatType.ERROR.index);
                double eTarget   = dataAverageTarget.getData().getValue(AccumulatorAverage.StatType.ERROR.index);
                
		        System.out.println("\n*****************************************************************");
		        System.out.println("**********                "+ idStep + "               ***************");
		        System.out.println("*****************************************************************");
                
                System.out.println("\nwBennetHarmonic: "  + wHarmonic  + " ,error: "+ eHarmonic);
                System.out.println("wBennetTarget: " + wTarget + " ,error: "+ eTarget);
				
                try {
                	
	                fileWriterSimBen.write(idStep + " " + wHarmonic + " "+ eHarmonic + " " + wTarget  + " "+ eTarget +"\n");
	                
                	} catch(IOException e){
                	
                }
                /*
                 * To get the entropy (overlap --> target or harmonic system),
                 * all you need to do is to take:
                 * 
                 *  sHarmonic = wHarmonic + ln(ratioHarmonicAverage)
                 *   sTarget  =  wTarget  + ln( ratioTargetAverage )
                 *   
                 *  both ratioHarmonicAverage and ratioTargetAverage are from SimPhaseOverlapSoftSphere class
                 *  
                 */
        	}
        };
        
       sim.integrator.addIntervalAction(outputAction);
       sim.integrator.setActionInterval(outputAction, 200000);
       
       sim.activityIntegrate.setMaxSteps(numSteps);
       sim.getController().actionPerformed();
        
       try {
    	   fileWriterSimBen.close();
       } catch(IOException e){
    	   
       }
    }

    private static final long serialVersionUID = 1L;
    public IntegratorMC integrator;
    public ActivityIntegrate activityIntegrate;
    public IBox box;
    public Boundary boundary;
    public Basis basis;
    public SpeciesSpheresMono species;
    public NormalModes normalModes;
    public int[] nCells;
    public CoordinateDefinition coordinateDefinition;
    public Primitive primitive;
    public PotentialMasterMonatomic potentialMasterMonatomic;
    public double latticeEnergy;
    public MeterHarmonicEnergy meterHarmonicEnergy;
    public double refPref;
    
    public static class SimBennetParam extends ParameterBase {
    	public int numMolecules = 32;
    	public double density = 1256;
    	public int exponentN = 12;
    	public int D = 3;
    	public long numSteps = 10000000;
    	public double harmonicFudge =1;
    	public String filename = "CB_FCC_n12_T10";
    	public double temperature = 1.0;
    }

}