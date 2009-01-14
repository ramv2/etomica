package etomica.modifier;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import etomica.units.Dimension;
import etomica.units.Null;

/**
 * Implements the Modifier functionality using introspection to obtain the accessor methods for a property.
 * Capable of modifying the same property for several objects at once (e.g., temperature for two different
 * integrator objects can be set simultaneously).  In the case of multi-object modification, calls to readValue
 * are taken from only the first object in the list, which assumes all would return the same value
 *
 * @author David Kofke
 * @author Jhumpa Adhikari
 */

public class ModifierGeneral implements Modifier, java.io.Serializable {

    private static final long serialVersionUID = 1L;
    protected Object[] object;
    protected String property;
    protected transient Method[] readMethod;
    protected transient Method[] writeMethod;
    private int nObjects;
    private Dimension dimension;
    private String label;
    
    /**
     * Constructor connecting the modifier's getValue and setValue methods to the set/get accessor
     * methods of the object being modifier.  If the accessor methods of the object are, for
     * example getTime and setTime, then the string "time" should be passed in the constructor.
     * Note the adherance to the JavaBeans case conventions for property and accessor names.
     *
     * @param obj the object with the property to be modified
     * @param property the name of the property being modified
     */
    public ModifierGeneral(Object[] obj, String prop) {
        dimension = Null.DIMENSION;
        nObjects = obj.length;
        object    = new Object[nObjects];
        property = prop;
        setLabel(prop);
        for(int j=0; j<nObjects; j++) {object[j] = obj[j];}
        initialize();
    }
    /**
     * Modifier for property of a single object (most common use)
     */
    public ModifierGeneral(Object obj, String prop) {
        this(new Object[] {obj}, prop);
    }
    
    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, java.lang.ClassNotFoundException {
        s.defaultReadObject();
        initialize();
    }
    
    private void initialize() {
        if(nObjects == 0) return;
        readMethod = new Method[nObjects];  //not sure if these need to be arrays, but doing this way to be safe
        writeMethod = new Method[nObjects]; //(might be able to use same readMethod and writeMethod objects for all objects
        for(int j=0; j<nObjects; j++) {
            Class c = object[j].getClass();
            //bits of this code are taken from Thinking in Java (1st edition), pages 708-713
            BeanInfo bi = null;
            try {
                bi = Introspector.getBeanInfo(c, java.lang.Object.class);
            }
            catch(IntrospectionException ex) {
                System.out.println("Couldn't introspect " + c.getName());
                System.exit(1);
            }
            PropertyDescriptor[] properties = bi.getPropertyDescriptors();
            for(int i=0; i<properties.length; i++) {
                String propertyName = properties[i].getName();
                if(propertyName.equals(property)) {
                    readMethod[j] = properties[i].getReadMethod();
                    writeMethod[j] = properties[i].getWriteMethod();
                    break;
                }
            }

            if(j == 0) {//discover dimension of modified property by looking at getDimension method of first object
                dimension = Dimension.introspect(object[0],property,bi);
            }
        }
        
    }
    
    public void setValue(double d) {
        for(int j=0; j<nObjects; j++) {
            try {
                writeMethod[j].invoke(object[j], new Object[] {new Double(d)});
            }
            catch(InvocationTargetException ex) {
                throw new RuntimeException(ex.getTargetException());
            }
            catch (NullPointerException ex) {
            	throw(ex);
            }
            catch(IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }//end of j-loop
    }
        
    
    public double getValue() {
        double value = Double.NaN;
        try {
            value = ((Double)readMethod[0].invoke(object[0], (Object[])null)).doubleValue();
        }
        catch(InvocationTargetException ex) {
            System.err.println("InvocationTargetException in getValue");
            ex.getTargetException().printStackTrace();
        }
        catch (NullPointerException ex) {
        	throw(ex);
        }
        catch(IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
        return value;
    }
    
    /**
     * @return Returns the label.
     */
    public String getLabel() {
        return label;
    }
    /**
     * @param label The label to set.  Default is the property name.
     */
    public void setLabel(String label) {
        this.label = label;
    }
    
    public Dimension getDimension() {
        return dimension;
    }
    
    public Object[] getObject() {return object;}
    public String getProperty() {return property;}
    
    /**
     * Method to demonstrate how to implement and use a modifier
     */
/*    public static void main(String[] args) {
        
      //A typical class to demonstrate use of the Modifier
      //Modifier will be used to set and get the pressure field of this class.
        IntegratorMC integrator = new IntegratorMC();
        MCMoveVolume target = new MCMoveVolume(integrator);
      
      //Pass the instance of the target object and a string indicating the name of the field
      // to be accessed with the modifier
        Modifier modifier = new Modifier(target, "pressure");
        
        System.out.println("Setting field to 10.5 using modifier");
        modifier.setValue(10.5);
        System.out.println("Value of field as obtained directly from object: "+target.getPressure());
        System.out.println("Value of field as obtained using modifier: "+modifier.getValue());
        System.out.println("Dimensions of field as obtained by modifier: "+modifier.getDimension().toString());
        boolean isMass = modifier.getDimension() instanceof Dimension.Mass;
        System.out.println("Does the field have dimensions of MASS? "+isMass);
        boolean isPressure = modifier.getDimension() instanceof Dimension.Pressure;
        System.out.println("Does the field have dimensions of PRESSURE? "+isPressure);
    }
    */
}
    
    
