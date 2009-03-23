package etomica.junit.data;

import junit.framework.TestCase;
import etomica.api.IData;
import etomica.data.AccumulatorAverage;
import etomica.data.types.DataDouble;
import etomica.units.Null;
import etomica.util.RandomNumberGenerator;

public abstract class AccumulatorAverageTestBase extends TestCase {

    public AccumulatorAverageTestBase(AccumulatorAverage accumulator) {
        this.accumulator = accumulator;
    }

    public void testSimple() {
        accumulator.putDataInfo(new DataDouble.DataInfoDouble("test", Null.DIMENSION));

        assertTrue(accumulator.getData().isNaN());
        assertEquals(accumulator.getBlockCount(), 0);

        simpleTest();

        accumulator.reset();
        assertTrue(accumulator.getData().isNaN());
        assertEquals(accumulator.getBlockCount(), 0);

        simpleTest();

        accumulator.putDataInfo(new DataDouble.DataInfoDouble("test", Null.DIMENSION));
        assertTrue(accumulator.getData().isNaN());
        assertEquals(accumulator.getBlockCount(), 0);

        simpleTest();
    }

    // test accumulator on simple uncorrelated data
    public void simpleTest() {
        DataDouble rawData = new DataDouble();
        RandomNumberGenerator rng = new RandomNumberGenerator();

        for (int i=0; i<1000000; i++) {
            rawData.x = rng.nextDouble();
            accumulator.putData(rawData);
        }

        IData accData = accumulator.getData();
        double avg = accData.getValue(AccumulatorAverage.StatType.AVERAGE.index);
        assertTrue(Math.abs(avg-0.5) < 0.01);

        double blockCorrelation = accData.getValue(AccumulatorAverage.StatType.BLOCK_CORRELATION.index);
        // block correlation should be ~0, but actual value will depend on # of blocks 
        assertTrue(Math.abs(blockCorrelation) < 3.0/Math.sqrt(accumulator.getBlockCount()));

        double stdev = accData.getValue(AccumulatorAverage.StatType.STANDARD_DEVIATION.index);
        assertTrue(Math.abs(stdev-Math.sqrt(1.0/12.0)) < 5.e-4);

        double error = accData.getValue(AccumulatorAverage.StatType.ERROR.index);
        assertTrue(error/2.9e-4 + 2.9e-4/error - 2 < 0.02);
    }

    public void testSingleValue() {
        accumulator.putDataInfo(new DataDouble.DataInfoDouble("test", Null.DIMENSION));

        DataDouble rawData = new DataDouble();
        rawData.x = 5.6;
        for (int i=0; i<12345; i++) {
            accumulator.putData(rawData);
        }
        
        IData accData = accumulator.getData();
        double avg = accData.getValue(AccumulatorAverage.StatType.AVERAGE.index);
        assertTrue(Math.abs(avg-rawData.x) < 1.e-10);
        
        accData.getValue(AccumulatorAverage.StatType.BLOCK_CORRELATION.index);
        // block correlation should be 0/0, actual value might be 0, some number, Infinity or NaN 
        
        double stdev = accData.getValue(AccumulatorAverage.StatType.STANDARD_DEVIATION.index);
        assertTrue(stdev < 1.e-7);

        double error = accData.getValue(AccumulatorAverage.StatType.ERROR.index);
        assertTrue(error < 1.e-6);
    }

    protected final AccumulatorAverage accumulator;
}
