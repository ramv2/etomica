//Source file generated by Etomica

package etomica.simulations;

import etomica.*;

public class HSMD3D extends Simulation {

  public HSMD3D() {
    super(new etomica.Space3D());
    Simulation.instance = this;
    Default.ATOM_SIZE = 6.6;
//    Default.DISPLAY_USE_OPENGL = false;
    etomica.Phase phase0  = new etomica.Phase();
//    phase0.setConfiguration(new ConfigurationFcc());
    etomica.P2HardSphere potentialHardSphere0  = new etomica.P2HardSphere();
    etomica.Controller controller0  = new etomica.Controller();
//    etomica.SpeciesSpheresMono speciesSpheres0  = new etomica.SpeciesSpheresMono();
    etomica.SpeciesSpheres speciesSpheres0  = new etomica.SpeciesSpheres();
      speciesSpheres0.setNMolecules(32);
      speciesSpheres0.setColor(new java.awt.Color(0,255,0));
    etomica.DisplayPhase displayPhase0  = new etomica.DisplayPhase();
    etomica.IntegratorHard integratorHard0  = new etomica.IntegratorHard();
      integratorHard0.setIsothermal(true);
      integratorHard0.setTemperature(1500.);
    
  } //end of constructor

  public static void main(String[] args) {
    Simulation sim = new HSMD3D();
    sim.mediator().go(); 
    Simulation.makeAndDisplayFrame(sim);
 //   sim.controller(0).start();
  }//end of main
}//end of class
