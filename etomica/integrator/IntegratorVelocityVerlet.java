package etomica.integrator;

import etomica.Atom;
import etomica.EtomicaElement;
import etomica.EtomicaInfo;
import etomica.Integrator;
import etomica.IteratorDirective;
import etomica.PotentialMaster;
import etomica.Space;
import etomica.potential.PotentialCalculationForceSum;
import etomica.space.ICoordinateKinetic;
import etomica.space.Vector;

/* History of changes
 * 08/29/02 (DAK) changed Andersen thermostat to velocity-scaling thermostat
 * 01/10/03 (DAK) reintroduced Andersen thermostat with flag to allow
 * selection of it or velocity-rescaling as the thermostat mechanism
 * */

public final class IntegratorVelocityVerlet extends IntegratorMD implements EtomicaElement {

    public final PotentialCalculationForceSum forceSum;
    private final Space space;
    private final IteratorDirective allAtoms = new IteratorDirective();
    
    public IntegratorVelocityVerlet(PotentialMaster potentialMaster, Space space) {
        super(potentialMaster);
        this.space = space;
        forceSum = new PotentialCalculationForceSum(space);
        setTimeStep(etomica.units.systems.LJ.SYSTEM.time().toSim(2.0));
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Molecular dynamics using velocity Verlet integration algorithm");
        return info;
    }
    
    private double t2;
    public final void setTimeStep(double t) {
        super.setTimeStep(t);
        t2 = timeStep*timeStep;
    }
  

          
//--------------------------------------------------------------
// steps all particles across time interval tStep

    // assumes one phase
    public void doStep() {
        atomIterator.setPhase(phase[0]);
        atomIterator.reset();              //reset iterator of atoms
        while(atomIterator.hasNext()) {    //loop over all atoms
            Atom a = atomIterator.nextAtom();  //  advancing positions full step
            MyAgent agent = (MyAgent)a.ia;     //  and momenta half step
            Vector r = a.coord.position();
            Vector v = ((ICoordinateKinetic)a.coord).velocity();
            v.PEa1Tv1(0.5*timeStep*a.type.rm(),agent.force);  // p += f(old)*dt/2
            r.PEa1Tv1(timeStep,v);         // r += p*dt/m
            agent.force.E(0.0);
        }
                
        //Compute forces on each atom
        potential.calculate(firstPhase, allAtoms, forceSum);
        
        //Finish integration step
        atomIterator.reset();
        while(atomIterator.hasNext()) {     //loop over atoms again
            Atom a = atomIterator.nextAtom();   //  finishing the momentum step
            ((ICoordinateKinetic)a.coord).velocity().PEa1Tv1(0.5*timeStep*a.type.rm(),((MyAgent)a.ia).force);  //p += f(new)*dt/2
        }
        if(isothermal) {
            doThermostat();
        }
        return;
    }
    
    
//--------------------------------------------------------------

    public void reset() {
        atomIterator.setPhase(phase[0]);
        atomIterator.reset();
        while(atomIterator.hasNext()) {
            Atom a = atomIterator.nextAtom();
            MyAgent agent = (MyAgent)a.ia;
            agent.force.E(0.0);
        }
        potential.calculate(firstPhase, allAtoms, forceSum);//assumes only one phase
        super.reset();
    }
              
//--------------------------------------------------------------

    public final Object makeAgent(Atom a) {
        return new MyAgent(space,a);
    }
            
    public final static class MyAgent implements Integrator.Forcible {  //need public so to use with instanceof
        public Atom atom;
        public Vector force;

        public MyAgent(Space space, Atom a) {
            atom = a;
            force = space.makeVector();
        }
        
        public Vector force() {return force;}
    }//end of MyAgent
    
}//end of IntegratorVelocityVerlet

