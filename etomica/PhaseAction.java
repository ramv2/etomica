package etomica;

//import java.awt.event.ActionEvent;

 /**
  * Superclass of classes that apply some elementary action (transformation) to a phase.
  * 
  */
public abstract class PhaseAction extends etomica.Action implements PhaseListener {

    public static String getVersion() {return "PhaseAction:01.03.28/"+Action.VERSION;}
    public static AtomIteratorMolecule iterator = new AtomIteratorMolecule();

    protected Phase phase;
    public PhaseAction() {this(null);}
    public PhaseAction(Phase p) {
        super();
        phase = p;
    }
        
    public void setPhase(Phase p) {phase = p;}
    public Phase getPhase() {return phase;}
    
    public void actionPerformed(SimulationEvent evt) {
        actionPerformed((PhaseEvent)evt);
    }
    public void actionPerformed(PhaseEvent pe) {
        actionPerformed(pe.phase());
    }
    
    public void actionPerformed() {actionPerformed(phase);}
    
    public abstract void actionPerformed(Phase p);

        
    //************* End of fields and methods for PhaseAction ************//
    
    //************* The remainder defines some subclasses of PhaseAction **********//
    
    public static final class Translate extends PhaseAction implements Action.Undoable {
        
        private Space.Vector translationVector;
        
        public Translate(Phase p) {
            super(p);
        }
        
        public void setTranslationVector(Space.Vector v) {translationVector.E(v);}
        public Space.Vector getTranslationVector() {return translationVector;}
        
        public void actionPerformed(Phase p) {doAction(p, translationVector);}
        
        public void actionPerformed(Space.Vector v) {doAction(phase, v);}
        
        public static void doAction(Phase p, Space.Vector v) {
            if(v == null || p == null) return;
            p.speciesMaster().coord.translateBy(v);
        }
        public void attempt() {doAction(phase, translationVector);}
        public void undo() {
            translationVector.TE(-1);
            doAction(phase, translationVector);
            translationVector.TE(-1);
        }
    }//end of Translate
    
    public static final class ImposePbc extends PhaseAction implements Integrator.IntervalListener.ImposePbc {
        
        public ImposePbc(Phase p) {
            super(p);
        }
        
        public void actionPerformed(Phase p) {doAction(p);}
        
        public static void doAction(Phase p) {
            Space.Boundary boundary = p.boundary();
            iterator.setBasis(p.speciesMaster.atomList);
            iterator.reset();
            while(iterator.hasNext()) {
                Atom a = iterator.next();
                //centralImage returns true if it causes the atom to be moved
                if(boundary.centralImage(a.coord)) a.seq.moveNotify();
            }
        }
        
        public void intervalAction(Integrator.IntervalEvent evt) {
            doAction(phase);
        }
        
        //revise this to make nonstatic
        private static final AtomIteratorList iterator = new AtomIteratorList();
        
        
    }//end of ImposePBC
    
    
    /**
     * Performs actions that cause volume of system to expand, with particle positions
     * scaled to keep them in the same relative positions.  Inflation is done isotropically,
     * equal in all coordinate directions.
     */
    public static final class Inflate extends PhaseAction implements Action.Undoable {
        
        double scale = 1.0;
        
        public Inflate() {
            super();
        }
        public Inflate(Phase p) {
            super(p);
        }
                
        public void setScale(double s) {scale = s;}
        public double getScale() {return scale;}

        public void actionPerformed(double s) {
            setScale(s);
            doAction(phase, scale);
        }
        public void actionPerformed(Phase p) {doAction(p, scale);}

        /**
         * Performs isotropic inflation.
         */
        public static void doAction(Phase phase, double scale) {
            phase.boundary().inflate(scale);
            iterator.setBasis(phase);
            iterator.reset();
            while(iterator.hasNext()) {
                iterator.next().coord.inflate(scale);
            }
        }
        
        public void attempt() {
            doAction(phase, scale);
        }
        public void undo() {
            doAction(phase, 1.0/scale);
        }
    }//end of Inflate 

    /**
     * Performs actions that cause volume of system to expand, with particle positions
     * scaled to keep them in the same relative positions.  Inflation is done anisotropically,
     * so that each dimension can be scaled differently.
     */
    public static class InflateAnisotropic extends PhaseAction implements Action.Undoable {
        
        protected Space.Vector scale;
        protected transient Space.Vector temp;
        private transient Space.Vector oldDimensions;
        
        public InflateAnisotropic(Phase p) {
            super(p);
            if(p != null) {
                scale = p.parentSimulation().space().makeVector();
                temp = p.parentSimulation().space().makeVector();
                oldDimensions = p.parentSimulation().space().makeVector();
            }
        }
                
        public void setScale(Space.Vector s) {scale = s;}
        public Space.Vector getScale() {return scale;}

        public void setPhase(Phase p) {
            super.setPhase(p);
            if(p != null && temp == null) temp = p.parentSimulation().space().makeVector();
        }
 
        public void actionPerformed(Phase p) {doAction(p, scale, temp);}

        /**
         * Performs anisotropic inflation.
         * @param scale Vector describing scaling to be performed in each coordinate
         *              direction.  scale(i) == 1 indicates no change in size in direction i.
         */
        public static void doAction(Phase phase, Space.Vector scale, Space.Vector work) {
            phase.boundary().inflate(scale);
      //      scale.PE(-1.0);
            iterator.setBasis(phase);
            iterator.reset();
            while(iterator.hasNext()) {
                iterator.next().coord.inflate(scale);
              //  Atom m = iterator.next();
              //  work.E(m.coord.position());
              //  work.TE(scale);
              //  m.coord.displaceBy(work);
            }
            //scale.PE(1.0);
        }

        public void attempt() {
    //        oldDimensions.E(phase.boundary().dimensions());
            doAction(phase, scale, temp);
        }
        public void undo() {
            temp.E(1.0);
            temp.DE(scale);
            doAction(phase, temp, null);
     //       phase.boundary().setDimensions(oldDimensions);
       //     iterator.setBasis(phase);
       //     iterator.reset();
       //     while(iterator.hasNext()) {
       //         Atom m = iterator.next();
    //            m.coord.replace();
           // }
        }
    }//end of InflateAnisotropic 
    
    /**
     * Toggles the state of enabling the long-range-corrections to the 
     * potential truncation in the phase.
     */
    public static class ToggleLrc extends PhaseAction {
        
        public void actionPerformed(Phase phase) {
            phase.setLrcEnabled(!phase.isLrcEnabled());
        }
    }

}//end of PhaseAction