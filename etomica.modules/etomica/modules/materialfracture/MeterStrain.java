package etomica.modules.materialfracture;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.data.DataSourceScalar;
import etomica.units.Null;


public class MeterStrain extends DataSourceScalar {
    
    public MeterStrain() {
        super("Strain", Null.DIMENSION);
        setHexagonal(true);
    }


    public void setBox(IBox newBox) {
        box = newBox;
        originalGageLength = calcGageLength();
    }

    public IBox getBox() {
        return box;
    }

    public void setAtomNumber(int n){
        aNumber = n;
        if (box != null) {
            originalGageLength = calcGageLength();
        }
    }

    public void setHexagonal(boolean b){
        hex = b;
        if (box != null) {
            originalGageLength = calcGageLength();
        }
    }
    
    protected double calcGageLength() {
        IAtomSet leafList = box.getLeafList();
        if(hex){
            double sum = 0;
            for (int i=aNumber; i<aNumber+18; i+=2) {
                sum += ((IAtomPositioned)leafList.getAtom(i)).getPosition().x(0);
            }
            for (int i=197-aNumber; i>197-aNumber-18; i-=2) {
                sum -= ((IAtomPositioned)leafList.getAtom(i)).getPosition().x(0);
            }
            return sum/9.0;
        }
        double firstLine = 0, secondLine = 0;
        for(int i=0; i<10; i++){
//                firstLine =firstLine+phase.speciesMaster.atomList.get((int)(190-aNumber+i)).coord.position().x(0);
//                secondLine=secondLine+phase.speciesMaster.atomList.get(aNumber+i).coord.position().x(0);
        }
        return (firstLine-secondLine)/10.0;
    }
    
    public double getDataAsScalar(){
        return (calcGageLength()-originalGageLength)/originalGageLength; 
    }

    protected int aNumber;
    protected double originalGageLength;
    protected boolean hex;
    protected IBox box;
}