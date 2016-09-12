package etomica.interfacial;

import etomica.api.IBox;
import etomica.api.ISpecies;
import etomica.data.DataSourceScalar;
import etomica.units.Length;

public class MeterWallPosition extends DataSourceScalar {

    protected final IBox box;
    protected final ISpecies wallSpecies;
    
    public MeterWallPosition(IBox box, ISpecies wallSpecies) {
        super("Wall position", Length.DIMENSION);
        this.box = box;
        this.wallSpecies = wallSpecies;
    }

    public double getDataAsScalar() {
        return box.getMoleculeList(wallSpecies).getMolecule(0).getChildList().getAtom(0).getPosition().getX(2) + 0.5*box.getBoundary().getBoxSize().getX(2);
    }

}
