package etomica;       
/**
 * Simple truncation of the potential at an adjustable cutoff separation.
 * The energy is unaffected for separations less than the truncation distance,
 * and it is set to zero beyond this distance.
 *
 * @author David Kofke
 */
 
 /* History of changes
  * 7/13/02 (DAK) Restructured instantiation of LRC potential; Space argument for constructor
  */
public final class PotentialTruncationSimple extends PotentialTruncation {
        
    double rCutoff, r2Cutoff, rCD;
    private /*final*/ double A; //inner class doesn't permit finals
    private /*final*/ int D;
    
    public PotentialTruncationSimple(Space space) {
        this(space, Default.POTENTIAL_CUTOFF_FACTOR * Default.ATOM_SIZE);}
        
    public PotentialTruncationSimple(Space space, double rCutoff) {
        super();
        A = space.sphereArea(1.0);  //multiplier for differential surface element
        D = space.D();              //spatial dimension
        setTruncationRadius(rCutoff);
    }
    public boolean isZero(double r2) {return r2 > r2Cutoff;}
        
    public double uTransform(double r2, double untruncatedValue) {
        return (r2 > r2Cutoff) ? 0.0 : untruncatedValue;
    }
    public double duTransform(double r2, double untruncatedValue) {
        return (r2 > r2Cutoff) ? 0.0 : untruncatedValue;
    }
    public double d2uTransform(double r2, double untruncatedValue) {
        return (r2 > r2Cutoff) ? 0.0 : untruncatedValue;
    }
    
    /**
     * Mutator method for the radial cutoff distance.
     */
    public final void setTruncationRadius(double rCut) {
        rCutoff = rCut;
        r2Cutoff = rCut*rCut;
        rCD = 1.0;
        for(int i=D; i>0; i--) {rCD *= rCD;}  //rC^D
    }
    /**
     * Accessor method for the radial cutoff distance.
     */
    public double getTruncationRadius() {return rCutoff;}
    /**
     * Returns the dimension (length) of the radial cutoff distance.
     */
    public etomica.units.Dimension getTruncationRadiusDimension() {return etomica.units.Dimension.LENGTH;}
    
    /**
     * Returns the zero-body potential that evaluates the contribution to the
     * energy and its derivatives from pairs that are separated by a distance
     * exceeding the truncation radius.
     */
    public Potential0Lrc makeLrcPotential(PotentialGroup parent, Potential2 potential) {
        return new P0Lrc(parent, potential);
    }
    
    /**
     * Inner class that implements the long-range correction for this truncation scheme.
     */
    private class P0Lrc extends Potential0Lrc {
        
        private Phase phase;
        private Potential2SoftSpherical potential;
        
        public P0Lrc(PotentialGroup parent, Potential2 potential) {
            super(parent);
            this.potential = (Potential2SoftSpherical)potential;
        }
        public Potential set(Atom a) {return this;}
        public Potential set(Atom a1, Atom a2) {return this;}
        public Potential set(SpeciesMaster s) {return this;}
        
        public double energy() {
            return uCorrection(potential.iterator().size()/phase.volume());
        }
        
        public Potential set(Phase p) {
            phase = p;
            potential.set(p.speciesMaster);
            return this;
        }
        
        /**
         * Long-range correction to the energy.
         * @param pairDensity average pairs-per-volume affected by the potential.
         */
        public double uCorrection(double pairDensity) {
            double integral = ((Potential2SoftSpherical)potential).integral(rCutoff);
            return pairDensity*integral;
        }
        
        /**
         * Uses result from integration-by-parts to evaluate integral of
         * r du/dr using integral of u.
         * @param pairDensity average pairs-per-volume affected by the potential.
         */
            //not checked carefully
        public double duCorrection(double pairDensity) {
            Potential2SoftSpherical potentialSpherical = (Potential2SoftSpherical)potential;
            double integral = potentialSpherical.integral(rCutoff);
            integral = A*rCD*potentialSpherical.u(rCutoff) - D*integral;//need potential to be spherical to apply here
            return pairDensity*integral;
        }

        /**
         * Uses result from integration-by-parts to evaluate integral of
         * r^2 d2u/dr2 using integral of u.
         * @param pairDensity average pairs-per-volume affected by the potential.
         *
         * Not implemented: throws RuntimeException.
         */
        public double d2uCorrection(double pairDensity) {
            throw new RuntimeException("method d2uCorrection not implemented in PotentialTruncationSimple.P0Lrc");
        }
    }//end of P0lrc
    
    /**
     * main method to demonstrate and test this class.
     */
/*   public static void main(String[] args) {
        
        Default.TRUNCATE_POTENTIALS = true;
        Simulation.instance = new etomica.graphics.SimulationGraphic();
	    IntegratorMC integrator = new IntegratorMC();
	    MCMoveAtom mcMove = new MCMoveAtom(integrator);//comment this line to examine LRC by itself
	    SpeciesSpheresMono species = new SpeciesSpheresMono();
	    species.setNMolecules(72);
	    final Phase phase = new Phase();
	    P2LennardJones potential = new P2LennardJones(3.0, 2000.);
	    Controller controller = new Controller();
	    etomica.graphics.DisplayPhase display = new etomica.graphics.DisplayPhase();

		MeterEnergy energy = new MeterEnergy();
		energy.setPhase(phase);
		energy.setHistorying(true);
		energy.setActive(true);		
		energy.getHistory().setNValues(500);		
		etomica.graphics.DisplayPlot plot = new etomica.graphics.DisplayPlot();
		plot.setLabel("Energy");
		plot.setDataSources(energy.getHistory());
		
		integrator.setSleepPeriod(2);
		
		etomica.graphics.DeviceToggleButton lrcToggle = new etomica.graphics.DeviceToggleButton(Simulation.instance,
		    new ModulatorBoolean() {
		        public void setBoolean(boolean b) {phase.setLrcEnabled(b);}
		        public boolean getBoolean() {return phase.isLrcEnabled();}
		    },"LRC enabled", "LRC disabled" );
		
		Simulation.instance.elementCoordinator.go();
	    
//        potential.setIterator(new AtomPairIteratorGeneral(phase));
//        potential.set(species.getAgent(phase));
        
        etomica.graphics.SimulationGraphic.makeAndDisplayFrame(Simulation.instance);
    }//end of main
 // */   
        
}//end of PotentialTruncationSimple
