/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

//This class includes a main method to demonstrate its use
package etomica.graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import etomica.action.IAction;
import etomica.action.activity.IController;
import etomica.simulation.prototypes.HSMD2D;

/**
 * Button that causes an action to be performed.
 * 
 * @author David Kofke
 */
 
public class DeviceButton extends Device {
    
    /**
     * Constructs button that is not connected to any action.  Subsequent
     * call to setAction is needed to make the button do something.
     * @param controller
     */
    public DeviceButton(IController controller) {
        super(controller);
        button = new JButton();
    }
    
    /**
     * Constructs a button connected to the given action.  Controller
     * and action may be changed independently after construction.
     */
    public DeviceButton(IController controller, IAction action) {
        this(controller);
        setAction(action);
    }
    
    public JButton getButton() {
        return button;
    }

    /**
     * Performs the button's action. Same effect as if the button were pressed
     * in the user interface.
     */
    public void press() {
        button.doClick();
    }
    
    /**
     * Returns the currently defined action associated with the button.
     */
    public IAction getAction() {return targetAction;}

    /**
     * Defines the action to be performed when the button is pressed.
     */
    public void setAction(final IAction newAction) {
        if(buttonAction != null) button.removeActionListener(buttonAction);
        targetAction = newAction;
        if(newAction == null) return;
        buttonAction = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                try {
                    doAction(targetAction);
                }
                catch (RuntimeException e) {
                    System.err.println(e+" "+e.getMessage());
                }
            }
        };
        button.addActionListener(buttonAction);
    }
    
    /**
     * Returns the GUI element for display in the simulation.
     */
    public java.awt.Component graphic(Object obj) {
        return button;
    }
    
    /**
     * Sets the value of a descriptive label using the given string.
     */
    public void setLabel(String text) {button.setText(text);}
    /**
     * @return the current instance of the descriptive label.
     */
    public String getLabel() {return button.getText();}
    
    private ActionListener buttonAction;
    protected JButton button;
    protected IAction targetAction;
    
    /**
     * Method to demonstrate and test the use of this class.  
     * Slider is used to control the temperature of a hard-sphere MD simulation
     */
    public static void main(String[] args) {
        
    	final String APP_NAME = "Device Button";

    	etomica.space.Space sp = etomica.space2d.Space2D.getInstance();
        HSMD2D sim = new HSMD2D();
        final SimulationGraphic graphic = new SimulationGraphic(sim, APP_NAME, sp, sim.getController());
        
        //here's the part unique to this class
        etomica.action.SimulationRestart action = new etomica.action.SimulationRestart(sim, sp, sim.getController());
        DeviceButton button = new DeviceButton(sim.getController(),action);
        button.setLabel("Device Button");

        //end of unique part
        graphic.add(button);
        graphic.makeAndDisplayFrame(APP_NAME);
    }
    
}