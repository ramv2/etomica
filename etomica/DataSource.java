package etomica;

public interface DataSource {
    
    public double[] values(ValueType type);
    
    public static abstract class ValueType extends Constants.TypedConstant {
        public ValueType(String label) {super(label);}
    } 
    
    public interface User {
        public void setDataSource(DataSource source);
        public DataSource getDataSource();
    }
}