package etomica.modules.materialfracture;

import etomica.api.IAction;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.atom.IAtomKinetic;
import etomica.data.AccumulatorAverageCollapsing;
import etomica.data.AccumulatorHistory;
import etomica.data.Data;
import etomica.data.DataFork;
import etomica.data.DataPipe;
import etomica.data.DataProcessor;
import etomica.data.DataPump;
import etomica.data.DataTag;
import etomica.data.IDataInfo;
import etomica.data.meter.MeterPressureTensorFromIntegrator;
import etomica.data.types.DataDouble;
import etomica.graphics.DeviceSlider;
import etomica.graphics.DeviceThermoSlider;
import etomica.graphics.DisplayPlot;
import etomica.graphics.DisplayTextBoxesCAE;
import etomica.graphics.SimulationGraphic;
import etomica.modifier.ModifierGeneral;
import etomica.units.Pressure2D;
import etomica.util.HistoryScrolling;

/**
 * Graphical components for Material Fracture module
 */
public class MaterialFractureGraphic extends SimulationGraphic {

    public MaterialFractureGraphic(final MaterialFracture sim) {
        super(sim, SimulationGraphic.TABBED_PANE, "Material Fracture", 1, sim.getSpace());
             
        final StrainColorScheme strainColor = new StrainColorScheme(sim);    
        getDisplayBox(sim.box).setColorScheme(strainColor);
        // 37 is the index of the first atom (on the left) to be colored red
        strainColor.setNumber(37);

        getController().getSimRestart().setConfiguration(sim.config);

        DeviceThermoSlider thermoSlider = new DeviceThermoSlider(sim.getController());
        thermoSlider.setIntegrator(sim.integrator);
        thermoSlider.setMaximum(600);
        add(thermoSlider);
        
        final MeterStrain meterStrain = new MeterStrain();
        meterStrain.setBox(sim.box);
        meterStrain.setAtomNumber(37);
        DataFork strainFork = new DataFork();
        DataPump strainPump = new DataPump(meterStrain, strainFork);
        getController().getDataStreamPumps().add(strainPump);
        sim.integrator.addIntervalAction(strainPump);
        AccumulatorAverageCollapsing strainAverage = new AccumulatorAverageCollapsing();
        strainFork.addDataSink(strainAverage);
    
        final MeterStress meterStress = new MeterStress(sim.pc);
        meterStress.setBox(sim.box);
        DataFork stressFork = new DataFork();
        DataPump stressPump = new DataPump(meterStress, stressFork);
        getController().getDataStreamPumps().add(stressPump);
        sim.integrator.addIntervalAction(stressPump);
        AccumulatorHistory stressHistory = new AccumulatorHistory(new HistoryScrolling(1));
        stressHistory.setTimeDataSource(meterStrain);
        stressFork.addDataSink(stressHistory);
        AccumulatorAverageCollapsing stressAverage = new AccumulatorAverageCollapsing();
        stressAverage.setPushInterval(10);
        stressFork.addDataSink(stressAverage);
    
        final DisplayPlot stressHistoryPlot = new DisplayPlot();
        stressHistory.setDataSink(stressHistoryPlot.getDataSet().makeDataSink());
        stressHistoryPlot.setLabel("Stress");
        stressHistoryPlot.setDoClear(false);
        stressHistoryPlot.setDoDrawLines(new DataTag[]{stressHistory.getTag()}, false);

        add(stressHistoryPlot);

        DisplayTextBoxesCAE stressDisplay = new DisplayTextBoxesCAE();
        stressDisplay.setAccumulator(stressAverage);
        add(stressDisplay);

        DisplayTextBoxesCAE strainDisplay = new DisplayTextBoxesCAE();
        strainDisplay.setAccumulator(strainAverage);
        add(strainDisplay);
        
        final MeterPressureTensorFromIntegrator meterPressure = new MeterPressureTensorFromIntegrator(space);
        meterPressure.setIntegrator(sim.integrator);
        DataProcessor pressureToStress = new DataProcessor(){
        
            public DataPipe getDataCaster(IDataInfo incomingDataInfo) {
                return null;
            }
        
            protected IDataInfo processDataInfo(IDataInfo inputDataInfo) {
                return dataInfo;
            }
        
            protected Data processData(Data inputData) {
                // xx component is the first one
                data.x = -inputData.getValue(0);
                return data;
            }
            
            protected final IDataInfo dataInfo = new DataDouble.DataInfoDouble("Stress", Pressure2D.DIMENSION);
            protected final DataDouble data = new DataDouble();
        };

        DataPump internalStressPump = new DataPump(meterPressure, pressureToStress);
        getController().getDataStreamPumps().add(internalStressPump);
        sim.integrator.addIntervalAction(internalStressPump);

        DataFork internalStressFork = new DataFork();
        pressureToStress.setDataSink(internalStressFork);
        AccumulatorHistory internalStressHistory = new AccumulatorHistory(new HistoryScrolling(1));
        internalStressHistory.setTimeDataSource(meterStrain);
        internalStressFork.addDataSink(internalStressHistory);
        AccumulatorAverageCollapsing internalStressAverage = new AccumulatorAverageCollapsing();
        internalStressAverage.setPushInterval(10);
        internalStressFork.addDataSink(internalStressAverage);
    
        internalStressHistory.setDataSink(stressHistoryPlot.getDataSet().makeDataSink());
        stressHistoryPlot.setDoDrawLines(new DataTag[]{internalStressHistory.getTag()}, false);
        stressHistoryPlot.setLegend(new DataTag[]{internalStressHistory.getTag()}, "Internal Stress");

        DisplayTextBoxesCAE internalStressDisplay = new DisplayTextBoxesCAE();
        internalStressDisplay.setLabel("Internal Stress");
        internalStressDisplay.setAccumulator(internalStressAverage);
        add(internalStressDisplay);

        ModifierGeneral springConstantModifier = new ModifierGeneral(sim.p1Tension, "springConstant");
        DeviceSlider springConstantSlider = new DeviceSlider(sim.getController(), springConstantModifier);
        springConstantSlider.setLabel("Spring Constant");
        springConstantSlider.setShowBorder(true);
        springConstantSlider.setMaximum(30);
        add(springConstantSlider);
        
        getController().getResetAveragesButton().setPostAction(new IAction() {
            public void actionPerformed() {
                stressHistoryPlot.getPlot().clear(false);
            }
        });
    }
  
    /**
    /* main method
     */
    public static void main(String[] args) {
        MaterialFracture sim = new MaterialFracture();
        MaterialFractureGraphic simGraphic = new MaterialFractureGraphic(sim);

        simGraphic.makeAndDisplayFrame();
    }

    public static class Applet extends javax.swing.JApplet{
        public void init(){ 
            getContentPane().add(new MaterialFractureGraphic(new MaterialFracture()).getPanel());
        }
    }
}