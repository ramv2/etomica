package etomica.data;

import java.util.Arrays;

import etomica.Data;
import etomica.DataInfo;
import etomica.utility.Function;

/**
 * Data object that wraps an array of doubles.
 * 
 * @author David Kofke and Andrew Schultz
 *  
 */

/*
 * History Created on Jun 15, 2005 by kofke
 */
public class DataDoubleArray extends Data implements DataArithmetic {

    public DataDoubleArray(DataInfo dataInfo) {
        super(dataInfo);
    }

    public void E(Data y) {
        System.arraycopy(((DataDoubleArray) y).x, 0, x, 0, x.length);
    }

    public void PE(DataArithmetic y) {
        double[] yx = ((DataDoubleArray) y).x;
        for (int i = 0; i < x.length; i++) {
            x[i] += yx[i];
        }

    }

    public void ME(DataArithmetic y) {
        double[] yx = ((DataDoubleArray) y).x;
        for (int i = 0; i < x.length; i++) {
            x[i] -= yx[i];
        }
    }

    public void TE(DataArithmetic y) {
        double[] yx = ((DataDoubleArray) y).x;
        for (int i = 0; i < x.length; i++) {
            x[i] *= yx[i];
        }

    }

    public void DE(DataArithmetic y) {
        double[] yx = ((DataDoubleArray) y).x;
        for (int i = 0; i < x.length; i++) {
            x[i] /= yx[i];
        }

    }

    public void E(double y) {
        Arrays.fill(x, y);
    }

    public void PE(double y) {
        for (int i = 0; i < x.length; i++) {
            x[i] += y;
        }
    }

    public void TE(double y) {
        for (int i = 0; i < x.length; i++) {
            x[i] *= y;
        }
    }

    public void map(Function function) {
        for (int i = 0; i < x.length; i++) {
            x[i] = function.f(x[i]);
        }
    }

    public void setLength(int n) {
        x = new double[n];
    }

    public double[] getData() {
        return x;
    }

    public boolean isNaN() {
        for (int i = 0; i < x.length; i++) {
            if (Double.isNaN(x[i]))
                return true;
        }
        return false;
    }
    
    public DataArithmetic toArithmetic(DataArithmetic data) {
        if (data == null) {
            data = this;
        }
        else if (data != this) {
            data.E(this);
        }
        return data;
    }
    
    public String toString() {
        return x.toString();
    }

    private double[] x;
}