package etomica.modules.entropylottery;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Iterator;

import etomica.atom.AtomLeaf;
import etomica.atom.iterator.AtomIteratorLeafAtoms;
import etomica.graphics.ColorSchemeCollective;
import etomica.graphics.DisplayCanvas;
import etomica.graphics.DisplayPhase;
import etomica.graphics.Drawable;
import etomica.phase.Phase;
import etomica.space.Vector;
import etomica.species.Species;

    /* History of changes
     * 7/16/02 (DAK) Modified for AtomType.Sphere diameter and radius method to take atom as argument.
     * 09/07/02 (DAK) added atomFilter
     */

//Class used to define canvas onto which configuration is drawn
public class DisplayPhaseCanvas1DBins extends DisplayCanvas {
    //  private int annotationHeight = font.getFontMetrics().getHeight();
    private double yScale = 0.4;
    private final AtomIteratorLeafAtoms atomIterator = new AtomIteratorLeafAtoms();
    private int[] atomCount;
    
    public DisplayPhaseCanvas1DBins(DisplayPhase _phase) {
        displayPhase = _phase;
        atomCount = new int[0];
    }
    
    public void setYScale(double newYScale) {
        yScale = newYScale;
    }
    
    public double getYScale() {
        return yScale;
    }
    
    /**
    * Sets the size of the display to a new value and scales the image so that
    * the phase fits in the canvas in the same proportion as before.
    */
    public void scaleSetSize(int width, int height) {
        if(getBounds().width * getBounds().height != 0) {  //reset scale based on larger size change
            double ratio1 = (double)width/(double)getBounds().width;
            double ratio2 = (double)height/(double)getBounds().height;
            double factor = Math.min(ratio1, ratio2);
    //        double factor = (Math.abs(Math.log(ratio1)) > Math.abs(Math.log(ratio2))) ? ratio1 : ratio2;
            displayPhase.setScale(displayPhase.getScale()*factor);
            setSize(width, height);
        }
    }
            
          
    //Override superclass methods for changing size so that scale is reset with any size change  
    // this setBounds is ultimately called by all other setSize, setBounds methods
    public void setBounds(int x, int y, int width, int height) {
        if(width == 0 || height == 0) return;
        super.setBounds(x,y,width,height);
        createOffScreen(width,height);
    }
       
    /**
    * doPaint is the method that handles the drawing of the phase to the screen.
    * Several variables and conditions affect how the image is drawn.  First,
    * the Unit.Length.Sim class variable <code>TO_PIXELS</code> performs the conversion between simulation
    * length units (Angstroms) and pixels.  The default value is 10 pixels/Angstrom
    * reflecting the default size of the phase (300 pixels by 300 pixels) and the
    * default phase size (30 by 30 A).  
    * The field <code>scale</code> is a multiplicative factor that directly
    * scales up or down the size of the image; this value is adjusted automatically
    * whenever shells of periodic images are drawn, to permit the central image and all 
    * of the specified periodic images to fit in the drawing of the phase.  
    *
    * @param g The graphic object to which the image of the phase is drawn
    * @see Species
    */
    public void doPaint(Graphics g) {
        if(!isVisible() || displayPhase.getPhase() == null) {return;}
        int w = getSize().width;
        int h = getSize().height;
            
        g.setColor(getBackground());
        g.fillRect(0,0,w,h);
        displayPhase.computeImageParameters2(w, h);

        //Draw other features if indicated
//        if(drawBoundary>DRAW_BOUNDARY_NONE) {displayPhase.getPhase().boundary().draw(g, displayPhase.getOrigin(), displayPhase.getScale());}
//        if(displayPhase.getDrawBoundary()) {displayPhase.getPhase().boundary().draw(g, displayPhase.getOrigin(), displayPhase.getScale());}

        //do drawing of all drawing objects that have been added to the display
        for(Iterator iter=displayPhase.getDrawables().iterator(); iter.hasNext(); ) {
            Drawable obj = (Drawable)iter.next();
            obj.draw(g, displayPhase.getOrigin(), displayPhase.getToPixels());
        }
            
        //Draw all atoms
        if(displayPhase.getColorScheme() instanceof ColorSchemeCollective) {
            ((ColorSchemeCollective)displayPhase.getColorScheme()).colorAllAtoms();
        }
        
        Phase phase = displayPhase.getPhase();
        Vector dimensions = phase.getBoundary().getDimensions();
        if (atomCount.length != (int)Math.round(dimensions.x(0))) {
            atomCount = new int[(int)Math.round(dimensions.x(0))];
        }
        for (int i=0; i<atomCount.length; i++) {
            atomCount[i] = 0;
        }
        atomIterator.setPhase(phase);
        atomIterator.reset();
        while(atomIterator.hasNext()) {
            AtomLeaf a = (AtomLeaf)atomIterator.nextAtom();
            int x = (int)Math.round(a.coord.position().x(0)+dimensions.x(0)*0.5);
            atomCount[x]++;
        }
            
        int[] origin = displayPhase.getOrigin();
        g.setColor(Color.RED);
        int drawingHeight = displayPhase.getDrawingHeight();
        for (int i=0; i<atomCount.length; i++) {
            int baseXP = origin[0] + (int)(displayPhase.getToPixels()*i);
            int height = (int)(displayPhase.getToPixels()*atomCount[i]*yScale);
            if (height > drawingHeight) {
                height = drawingHeight;
            }
            int baseYP = origin[1] + drawingHeight - height;
            g.fillRect(baseXP, baseYP, (int)displayPhase.getToPixels(), height);
        }
    }
}
