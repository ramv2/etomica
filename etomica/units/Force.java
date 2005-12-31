package etomica.units;

import java.io.ObjectStreamException;

import etomica.units.systems.UnitSystem;

/**
 * Base for all force units. Simulation unit of force is D-A/ps^2
 */
public final class Force extends Dimension {

    public static final Dimension DIMENSION = new Force();
    public static final Unit SIM_UNIT = new SimpleUnit(DIMENSION, 1.0, "sim force units", "D-A/ps^2", Prefix.NOT_ALLOWED);

    private Force() {
        super("Force", 1, 1, -2);
    }
    
    public Unit getUnit(UnitSystem unitSystem) {
        return unitSystem.force();
    }

   /**
     * Required to guarantee singleton when deserializing.
     * 
     * @return the singleton DIMENSION
     */
    private Object readResolve() throws ObjectStreamException {
        return DIMENSION;
    }

    private static final long serialVersionUID = 1;

}