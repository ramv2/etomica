package etomica;

/**
 * Parallel-tempering integrator.  Oversees other integrators that are defined to perform
 * MC trials (or perhaps molecular dynamics) in different phases.  These integrators
 * are identified (added) to this integrator, and the simulation runs on this
 * integrator's thread.  When this integrator does a step, it passes on the
 * instruction to each added integrator, causing all to do a step on each's phase.
 * Occasionally, this integrator will instead attempt to swap the configurations
 * of two of the phases.  Acceptance of this move depends on parameters (usually 
 * temperature) of the integrators for the phases, as well as the configurations
 * in each phase.  The swap trial is performed by a MCMove class designed for
 * this purpose.  Such a class is made by a MCMoveSwapConfigurationFactory class
 * that is identified to this integrator (a default is selected if not specified).
 * Every time an integrator is added to this one, a MCMoveSwap class is made (by this
 * integrator using the factory) to manage swap trials between the new integrator's
 * phase and that of the one most recently added.
 * 
 * @author David Kofke
 */
 
 /* History of changes
  * 7/16/02 (DAK) new
  */

public class IntegratorPT extends IntegratorMC implements EtomicaElement {
    
    public String version() {return "IntegratorPT:01.11.20"+super.version();}

    public IntegratorPT() {
        this(Simulation.instance);
    }
    public IntegratorPT(Simulation sim) {
        super(sim);
        setSwapInterval(100);
    }
    
    /**
     * Adds the given integrator to those managed by this integrator, and
     * includes integrator's phase to the set among which configurations are
     * swapped.  Phase of new integrator will be subject to swapping with
     * the most recently added integrator/phase (and the next one, if another
     * is added subsequently).
     */
	public void addIntegrator(Integrator integrator){
		int n = integrators.length;
		Integrator[] newintegrators = new Integrator[n+1];
		for(int i=0; i<n; i++) { newintegrators[i] = integrators[i];}
		newintegrators[n] = integrator;
		integrators = newintegrators;
		nIntegrators++;
	}
	
	/**
     * Performs a Monte Carlo trial that attempts to swap the configurations
     * between two "adjacent" phases, or instructs all integrators to perform
     * a single doStep.
     */
    public void doStep() {
        if(Simulation.random.nextDouble() < swapProbability) {
            super.doStep();
        } else {
            for(int i=0; i<nIntegrators; i++) integrators[i].doStep();
        }
    }
    
    /**
     * Resets this integrator and passes on the reset to all managed integrators.
     */
    public void doReset() {
        super.doReset();
	    for(int i=0; i<nIntegrators; i++) {
	        integrators[i].doReset();
	    }
	}
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Parallel-tempering Monte Carlo simulation");
        return info;
    }
    
    /**
     * Sets the average interval between phase-swap trials.  With each 
     * call to doStep of this integrator, there will be a probability of
     * 1/nSwap that a swap trial will be attempted.  An swap is attempted
     * for only one pair of phases.  Default is 100.
     */
    public void setSwapInterval(int nSwap) {
        if(nSwap < 1) nSwap = 1;
        this.nSwap = nSwap;
        swapProbability = 1.0/(double)nSwap;
    }
    
    /**
     * Accessor method for the average interval between phase-swap trials.
     */
    public int getSwapInterval() {return nSwap;}
    
    private int nSwap;
    private double swapProbability;
	private Integrator[] integrators = new Integrator[0];	
	private int nIntegrators = 0;
	private MCMoveSwapFactory mcMoveSwapFactory = new MCMoveSwapFactoryDefault();

	
	//----------- inner interface -----------
	
	/**
	 * Interface for a class that can make a MCMove that will swap
	 * the configurations of two phases.  Different MCMove classes
	 * would do this differently, depending on ensemble of simulation
	 * and other factors.
	 */
public interface MCMoveSwapFactory {
    
    /**
     * @param integratorMC the parent integrator using this move
     * @param integrator1 integrator for one of the phases being swapped
     * @param integrator2 integrator for the other phase
     */
    public MCMove makeMCMoveSwap(IntegratorMC integratorMC, Integrator integrator1, Integrator integrator2);
}//end of MCMoveSwapFactory



    // -----------inner class----------------
    
public class MCMoveSwapFactoryDefault implements MCMoveSwapFactory {
    public MCMove makeMCMoveSwap(IntegratorMC integratorMC, 
                                    Integrator integrator1, Integrator integrator2) {
        return new MCMoveSwapConfiguration(integratorMC, integrator1, integrator2);
    }
}//end of MCMoveSwapFactoryDefault 


	
	// -----------inner class----------------
	
	/**
	 * Basic MCMove for swapping coordinates of atoms in two phases.
	 * Requires same number of atoms in each phase.
	 */
public class MCMoveSwapConfiguration extends MCMove {

	private Integrator integrator1, integrator2;	
    private final IteratorDirective iteratorDirective = new IteratorDirective();
	private AtomIteratorMolecule iterator1 = new AtomIteratorMolecule();
	private AtomIteratorMolecule iterator2 = new AtomIteratorMolecule();
	private AtomIteratorMolecule affectedAtomIterator = new AtomIteratorMolecule();
	private Space.Vector r;
	private double u1, u2, temp1, temp2, deltaU1;
	private Phase phase1, phase2;

	public MCMoveSwapConfiguration(IntegratorMC integratorMC, 
	                                Integrator integrator1, Integrator integrator2) {
  		super(integratorMC);
		r = integratorMC.parentSimulation().space.makeVector();
		setTunable(false);
		this.integrator1 = integrator1;
		this.integrator2 = integrator2;
	}

	public boolean doTrial() {
 		phase1 = integrator1.getPhase(0);
		phase2 = integrator2.getPhase(0);

		temp1 = integrator1.getTemperature();
		temp2 = integrator2.getTemperature();

        u1 = potential.set(phase1).calculate(iteratorDirective, energy.reset()).sum();
        u2 = potential.set(phase2).calculate(iteratorDirective, energy.reset()).sum();
        deltaU1 = Double.NaN;
        return true;
    }
    
    public double lnTrialRatio() {return 0.0;}
    
    public double lnProbabilityRatio() {
        deltaU1 = u2 - u1;  //if accepted, energy of phase1 will be u2, and its old energy is u1
		return  -deltaU1*((1/temp1) - (1/temp2));
	}
	
	/**
	 * Swaps positions of molecules in two phases.
	 */
	public void acceptNotify() {
		iterator1.setBasis(phase1);
		iterator2.setBasis(phase2);
			
		iterator1.reset();
		iterator2.reset();

		while(iterator1.hasNext()) {
			Atom a1 = iterator1.next();
			Atom a2 = iterator2.next();

			r.E(a1.coord.position());
				
			a1.coord.translateTo(a2.coord.position());
			a2.coord.translateTo(r);
		}
	}
	
	/**
     * Performs no action; nothing required when move rejected.
     */
	public void rejectNotify() {}
	
	public double energyChange(Phase phase) {
	    if(phase == phase1)      return +deltaU1;
	    else if(phase == phase2) return -deltaU1;
	    else                     return 0.0;
	}

	public AtomIterator affectedAtoms(Phase p) {
	    if(p == phase1 || p == phase2) {
	        affectedAtomIterator.setBasis(p);
	        affectedAtomIterator.reset();
	        return affectedAtomIterator;
	    }
	    else return AtomIterator.NULL;
	}
}//end of MCMoveSwapConfiguration

    
}//end of IntegratorPT

