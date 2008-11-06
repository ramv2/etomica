package etomica.kmc;

import etomica.action.BoxImposePbc;
import etomica.action.WriteConfiguration;
import etomica.action.XYZWriter;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IMolecule;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.config.ConfigurationFile;
import etomica.dimer.IntegratorDimerMin;
import etomica.dimer.IntegratorDimerRT;
import etomica.exception.ConfigurationOverlapException;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorBox;
import etomica.space.ISpace;
import etomica.units.Joule;

public class IntegratorKMC extends IntegratorBox{

    private static final long serialVersionUID = 1L;
    IntegratorDimerRT integratorDimer;
    IntegratorDimerMin integratorMin1, integratorMin2;
    IPotentialMaster potentialMaster;
    double temperature;
    private final ISpace space;
    IRandom random;
    ISimulation sim;
    ISpecies [] species;
    IVector [] minPosition, currentSaddle, previousSaddle;
    double[] saddleVib;
    double massSec;
    double[] saddleEnergies;
    double[] rates;
    double tau;
    double msd;
    double beta;
    double minEnergy;
    double minVib;
    boolean search;
    int goodSearch;
    int stepCounter;
    int searchlimit;
    SimulationGraphic graphic;
    XYZWriter xyzfile;
    BoxImposePbc imposePbc;
    
    public IntegratorKMC(ISimulation _sim, IPotentialMaster _potentialMaster, double _temperature, IRandom _random, ISpecies [] _species, ISpace _space){
        super(_potentialMaster, _temperature);
        this.potentialMaster = _potentialMaster;
        this.temperature = _temperature;
        this.space = _space;
        this.random = _random;
        this.sim = _sim;
        this.species = _species;
        
        searchlimit = 5;
        tau = 0;
        
                
        // TODO Auto-generated constructor stub
    }
 
    @Override
    protected void doStepInternal(){
        
        //Dimer Searches from minimum
        goodSearch = 0;
        while(search){
            loadConfiguration(stepCounter+"");
            randomizePositions();
            try {
                System.out.println("Initializing dimer.");
                integratorDimer.initialize();
            } catch (ConfigurationOverlapException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            integratorDimer.setFileName("s_"+goodSearch);
            System.out.println("Searching...");
            for(int j=0;j<500;j++){
                imposePbc.actionPerformed();
                integratorDimer.doStep();
                if(integratorDimer.saddleFound){
                    if(checkUniqueSaddle()){
                        System.out.println("Good search "+goodSearch+", adding saddle data.");
                        saddleEnergies[goodSearch] = integratorDimer.saddleEnergy;
                        saddleVib[goodSearch] = integratorDimer.vib.getProductOfFrequencies();
                    }
                    goodSearch++;
                    break;
                }
                
            }
            if(goodSearch>searchlimit-1){search = false;}
        }
        search = true;
                
        calcRates();
        int rateNum = chooseRate();
        System.out.println("Rate "+rateNum+" is chosen.");
        System.out.println("Tau is "+tau);
        //Minimum Search with random transition
        integratorMin1.setFileName("s_"+rateNum);
        try {
            integratorMin1.initialize();
        } catch (ConfigurationOverlapException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        writeConfiguration(stepCounter+"_saddle");
        xyzfile.actionPerformed();
        
        stepCounter++;

        for(int j=0;j<1000;j++){
            System.out.println("Minimum search step...");
            integratorMin1.doStep();
            if(integratorMin1.minFound){
                break;
            }
        }
        if(checkMin()){
            minEnergy = integratorMin1.e0;
            minVib = integratorMin1.vib.getProductOfFrequencies();
            writeConfiguration(stepCounter+"");
            setInitialStateConditions(minEnergy, minVib);
            System.out.println("Good minimum found.");
        }else{
            integratorMin2.setFileName("s_"+rateNum);
            try {
                integratorMin2.initialize();
            } catch (ConfigurationOverlapException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            for(int j=0;j<1000;j++){
                integratorMin2.doStep();
                if(integratorMin2.minFound){
                    break;
                }
            }
            minEnergy = integratorMin2.e0;
            minVib = integratorMin2.vib.getProductOfFrequencies();
            writeConfiguration(stepCounter+"");
            setInitialStateConditions(minEnergy, minVib);
            System.out.println("Good minimum found on second attempt.");
        }
        xyzfile.actionPerformed();
        
    }
    
    public void setup(){
        search = true;
        saddleVib = new double[searchlimit];
        saddleEnergies = new double[searchlimit];
        massSec = Math.sqrt(species[0].getChildType(0).getMass()) * 0.000000000001;
        rates = new double[searchlimit];
        beta = 1.0/(temperature*1.3806503E-023);
        stepCounter = 0;     
        imposePbc = new BoxImposePbc(box, space);
        currentSaddle = new IVector[box.getMoleculeList().getAtomCount()];
        previousSaddle = new IVector[box.getMoleculeList().getAtomCount()];
        for(int i=0; i<currentSaddle.length; i++){
            currentSaddle[i] = space.makeVector();
            previousSaddle[i] = space.makeVector();
        }
        
        try {
            integratorDimer.initialize();
        } catch (ConfigurationOverlapException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setInitialStateConditions(double energy, double vibFreq){
        minEnergy = energy;
        minVib = vibFreq;
        
        IAtomSet loopSet2 = box.getMoleculeList();
        minPosition = new IVector[loopSet2.getAtomCount()];
        for(int i=0; i<minPosition.length; i++){
            minPosition[i] = space.makeVector();
        }
        
        for(int i=0; i<loopSet2.getAtomCount(); i++){
            minPosition[i].E(((IAtomPositioned)((IMolecule)loopSet2.getAtom(i)).getChildList().getAtom(0)).getPosition());
        }  
    }
    
    public void setSearchLimit(int limit){
        searchlimit = limit;
    }
    
    public void randomizePositions(){
        IVector workVector = space.makeVector();
        IAtomSet loopSet3 = box.getMoleculeList(species[0]);
        IVector [] currentPos = new IVector [loopSet3.getAtomCount()];
        double offset = 0;
        for(int i=0; i<currentPos.length; i++){
            currentPos[i] = space.makeVector();
            currentPos[i] = (((IAtomPositioned)((IMolecule)loopSet3.getAtom(i)).getChildList().getAtom(0)).getPosition());
            for(int j=0; j<3; j++){
                offset = random.nextGaussian()/10.0;
                if(Math.abs(offset)>0.1){offset=0.1;}
                workVector.setX(j,offset);
            }
            currentPos[i].PE(workVector);
        }
    }
        
    public void calcRates(){
        //convert energies to Joules and use hTST
        double rateSum = 0;
        minEnergy = Joule.UNIT.fromSim(minEnergy);
        for(int i=0; i<rates.length; i++){
            if(saddleEnergies[i]==0){continue;}
            saddleEnergies[i] = Joule.UNIT.fromSim(saddleEnergies[i]);
            rates[i] = (minVib / saddleVib[i] / massSec)* Math.exp( -(saddleEnergies[i] - minEnergy)*beta);
            rateSum += rates[i];
        }
        //compute residence time
        tau += -Math.log(random.nextDouble())/rateSum;

    }
    
    public int chooseRate(){
        int rt = 0;
        double sum = 0;
        double rand = random.nextDouble();
        for(int q=0; q<rates.length; q++){
            sum += rates[q];
        }
        double sumgrt = 0;
        for(int i=0; i<rates.length; i++){
            sumgrt += rates[i];
            if(rand*sum<=sumgrt){
                rt = i;
                System.out.println("-----Choosing a rate-----");
                for(int l=0; l<rates.length; l++){ 
                    System.out.println("Rate "+l+": "+rates[l]+", v: "+minVib / saddleVib[i] / massSec);
                }
                System.out.println("Sum:    "+sum);
                System.out.println("-------------------------");
                System.out.println(rand*sum+" <= "+sumgrt);
                break;
            }
        }
        return rt;
    }
    
    private boolean checkUniqueSaddle(){    
        for(int p=0; p<box.getMoleculeList().getAtomCount(); p++){
            currentSaddle[p].E(((IAtomPositioned)((IMolecule)box.getMoleculeList().getAtom(p)).getChildList().getAtom(0)).getPosition());
        }
        for(int i=0; i<goodSearch; i++){
            double positionDiff = 0;
            loadConfiguration("s_"+i+"_saddle");
            for(int j=0; j<box.getMoleculeList().getAtomCount(); j++){
                previousSaddle[j].E(((IAtomPositioned)((IMolecule)box.getMoleculeList().getAtom(j)).getChildList().getAtom(0)).getPosition());
                previousSaddle[j].ME(currentSaddle[j]);
                positionDiff += previousSaddle[j].squared();
            }
            if(positionDiff < 0.5){
                System.out.println("Duplicate saddle found.");
                return false;
            }
        }
        System.out.println("Unique saddle found.");
        return true;  
    }
    
    private double truncate(double numA, int digits){
        digits = (int)Math.pow(10,digits);
        numA = (long)(digits*numA);
        numA = (double)(numA/digits);
        return numA;
    }
    public boolean checkMin(){
        IVector workVector = space.makeVector();
        double positionDiff=0;
        for(int i=0; i<box.getMoleculeList().getAtomCount(); i++){
            workVector.Ev1Mv2(minPosition[i],((IAtomPositioned)((IMolecule)box.getMoleculeList().getAtom(i)).getChildList().getAtom(0)).getPosition());
            positionDiff += workVector.squared();
        }
        if(positionDiff > 0.5){return true;}
        return false;
    }
        
    public void writeConfiguration(String file){
        WriteConfiguration writer = new WriteConfiguration(space);
        writer.setBox(box);
        writer.setConfName(file);
        writer.actionPerformed();
    }
    
    public void loadConfiguration(String file){
        ConfigurationFile config = new ConfigurationFile(file);
        config.initializeCoordinates(box);
    }

    public void createIntegrators(){
        integratorMin1 = new IntegratorDimerMin(sim, potentialMaster, species, true, space);
        integratorMin2= new IntegratorDimerMin(sim, potentialMaster, species, false, space);
        integratorDimer = new IntegratorDimerRT(sim, potentialMaster, species, space);
        
        integratorMin1.setBox(box);
        integratorMin2.setBox(box);
        integratorDimer.setBox(box);
        integratorDimer.setRotNum(0);
        integratorDimer.setOrtho(false, false);
                
        xyzfile = new XYZWriter(box);
        xyzfile.setIsAppend(true);
        xyzfile.setFileName("kmc-lj-3.xyz");
    }
    

}