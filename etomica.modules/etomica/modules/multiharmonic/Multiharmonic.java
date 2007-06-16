package etomica.modules.multiharmonic;

import etomica.action.SimulationDataAction;
import etomica.action.activity.ActivityIntegrate;
import etomica.action.activity.Controller;
import etomica.atom.AtomTypeSphere;
import etomica.atom.IAtomPositioned;
import etomica.atom.iterator.AtomIteratorLeafAtoms;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorHistory;
import etomica.data.DataPump;
import etomica.data.DataSourceCountTime;
import etomica.data.meter.MeterEnergy;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.phase.Phase;
import etomica.potential.P1Harmonic;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space1d.Space1D;
import etomica.space1d.Vector1D;
import etomica.species.Species;
import etomica.species.SpeciesSpheresMono;
import etomica.util.HistoryCollapsing;


/**
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 *
 * @author David Kofke
 *
 */
public class Multiharmonic extends Simulation {

    public Multiharmonic() {
        super(Space1D.getInstance());
        PotentialMaster potentialMaster = new PotentialMaster(space);
        double x0 = 1;
        species = new SpeciesSpheresMono(this);
        getSpeciesManager().addSpecies(species);
        ((AtomTypeSphere)species.getMoleculeType()).setDiameter(0.02);
        phase = new Phase(this);
        addPhase(phase);
        phase.getBoundary().setDimensions(new Vector1D(3.0));
        controller = getController();
        integrator = new IntegratorVelocityVerlet(this, potentialMaster);
        integrator.setPhase(phase);
        integrator.setTimeStep(0.02);
        integrator.setIsothermal(true);
        integrator.setTemperature(1.0);
        species.getAgent(phase).setNMolecules(20);
        potentialA = new P1Harmonic(space);
        potentialA.setX0(new Vector1D(x0));
        potentialA.setSpringConstant(1.0);
        potentialMaster.addPotential(potentialA, new Species[] {species});
        
        phase.getAgent(species).setNMolecules(20);
        
        AtomIteratorLeafAtoms iterator = new AtomIteratorLeafAtoms();
        iterator.setPhase(phase);
        iterator.reset();
        for (IAtomPositioned a = (IAtomPositioned)iterator.nextAtom(); a != null;
             a = (IAtomPositioned)iterator.nextAtom()) {
            a.getPosition().setX(0,x0);
        }
        activityIntegrate = new ActivityIntegrate(integrator);
        activityIntegrate.setDoSleep(true);
        activityIntegrate.setSleepPeriod(1);
        getController().addAction(activityIntegrate);

        potentialB = new P1Harmonic(space);
        potentialB.setX0(new Vector1D(x0+1));
        potentialB.setSpringConstant(10);
        meter = new MeterFreeEnergy(potentialA, potentialB);
        meter.setPhase(phase);
        accumulator = new AccumulatorAverage();
        accumulator.setBlockSize(100);
        dataPump = new DataPump(meter, accumulator);
        integrator.addIntervalAction(dataPump);
        
        meterEnergy = new MeterEnergy(potentialMaster);
        meterEnergy.setPhase(phase);
        accumulatorEnergy = new AccumulatorAverage();
        accumulatorEnergy.setBlockSize(100);
        DataPump dataPumpEnergy = new DataPump(meterEnergy, accumulatorEnergy);
        integrator.addIntervalAction(dataPumpEnergy);
        
        register(meter,dataPump);
        register(meterEnergy,dataPumpEnergy);
        
        historyEnergy = new AccumulatorHistory(new HistoryCollapsing());
        accumulatorEnergy.addDataSink(historyEnergy, new AccumulatorAverage.StatType[] {AccumulatorAverage.StatType.AVERAGE});

        timeCounter = new DataSourceCountTime(integrator);
        
        historyEnergy.setTimeDataSource(timeCounter);
    }

    private static final long serialVersionUID = 1L;
    MeterEnergy meterEnergy;
    AccumulatorAverage accumulatorEnergy;
    AccumulatorHistory historyEnergy;
    SpeciesSpheresMono species;
    Phase phase;
    Controller controller;
    P1Harmonic potentialA, potentialB;
    IntegratorVelocityVerlet integrator;
    ActivityIntegrate activityIntegrate;
    MeterFreeEnergy meter;
    AccumulatorAverage accumulator;
    DataPump dataPump;
    SimulationDataAction resetAccumulators;
    DataSourceCountTime timeCounter;
}
