package etomica.virial.simulations;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import etomica.action.IAction;
import etomica.api.IAtom;
import etomica.api.IBox;
import etomica.api.IFunction;
import etomica.api.IMoleculeList;
import etomica.api.IPotential;
import etomica.chem.elements.ElementSimple;
import etomica.data.AccumulatorAverageFixed;
import etomica.data.IData;
import etomica.data.IEtomicaDataInfo;
import etomica.data.types.DataDouble;
import etomica.data.types.DataGroup;
import etomica.graphics.ColorScheme;
import etomica.graphics.DisplayBox;
import etomica.graphics.DisplayBoxCanvasG3DSys;
import etomica.graphics.DisplayTextBox;
import etomica.graphics.SimulationGraphic;
import etomica.graphics.SimulationPanel;
import etomica.listener.IntegratorListenerAction;
import etomica.math.SpecialFunctions;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Null;
import etomica.util.ParameterBase;
import etomica.util.ParseArgs;
import etomica.virial.CalcFFT;
import etomica.virial.ClusterAbstract;
import etomica.virial.ClusterChainHS;
import etomica.virial.ClusterChainSoft;
import etomica.virial.ClusterSinglyConnected;
import etomica.virial.ClusterWeightAbs;
import etomica.virial.ClusterWeightUmbrella;
import etomica.virial.ClusterWheatleyHS;
import etomica.virial.ClusterWheatleyPartitionScreening;
import etomica.virial.MCMoveClusterAtomChainHSTail;
import etomica.virial.MCMoveClusterAtomHSChain;
import etomica.virial.MCMoveClusterAtomHSRing;
import etomica.virial.MCMoveClusterAtomHSTree;
import etomica.virial.MayerFunction;
import etomica.virial.MayerHardSphere;
import etomica.virial.MeterVirialBD;
import etomica.virial.cluster.Standard;

/**
 * Calculation for virial coefficients of hard spheres
 */
public class VirialHS {

    public static void main(String[] args) {

        VirialHSParam params = new VirialHSParam();
        if (args.length > 0) {
            ParseArgs.doParseArgs(params, args);
        }
        else {
            params.nPoints = 7;
            params.numSteps = 1000000L;
            params.ref = VirialHSParam.TREE;
            params.chainFrac = 0.2;
            params.ringFrac = 0.7;
            params.nPtsTabulated = 0;
        }

        final int nPoints = params.nPoints;
        long steps = params.numSteps;
        final int ref = params.ref;
        final double sigmaHS = 1.0;
        final double chainFrac = params.chainFrac;
        final double ringFrac = params.ringFrac;
        final int nPtsTabulated = params.nPtsTabulated;

        double litHSB = Double.NaN;
        try {
            litHSB = Standard.BHS(nPoints, sigmaHS);
        }
        catch (RuntimeException e) {}
        double vhs = (4.0/3.0)*Math.PI*sigmaHS*sigmaHS*sigmaHS;

        System.out.println("HS singly-connected sampling B"+nPoints);

        Space space = Space3D.getInstance();

        final double pow = 12;
        MayerHardSphere fRef = new MayerHardSphere(sigmaHS);
        MayerFunction fRefPos = new MayerFunction() {

            public void setBox(IBox box) {}
            public IPotential getPotential() {return null;}

            public double f(IMoleculeList pair, double r2, double beta) {
                return r2 < sigmaHS*sigmaHS ? 1 : 0;
            }
        };
        MayerFunction fHSTail = new MayerFunction() {
            
            public void setBox(IBox box) {
            }
            
            public IPotential getPotential() {
                return null;
            }
            
            public double f(IMoleculeList pair, double r2, double beta) {
                return r2 < sigmaHS*sigmaHS ? 1 : Math.pow(r2, -pow/2);
            }
        };
        ClusterAbstract targetCluster = nPtsTabulated > 0 ? new ClusterWheatleyPartitionScreening(nPoints, fRef, nPtsTabulated) : new ClusterWheatleyHS(nPoints, fRef);

        targetCluster.setTemperature(1.0);

        ClusterAbstract refCluster = null;
        long numDiagrams = 0;

        double ri = 0;
        if (ref == VirialHSParam.TREE) {
            System.out.println("using a tree reference");
            refCluster = new ClusterSinglyConnected(nPoints, fRefPos);
            numDiagrams = ((ClusterSinglyConnected)refCluster).numDiagrams();
            ri = numDiagrams*Math.pow(vhs, nPoints-1);
        }
        else if (ref == VirialHSParam.CHAINS) {
            System.out.println("using a chain reference");
            refCluster = new ClusterChainHS(nPoints, fRefPos);
            numDiagrams = ((ClusterChainHS)refCluster).numDiagrams();
            ri = numDiagrams*Math.pow(vhs, nPoints-1);
        }
        else if (ref == VirialHSParam.CRINGS) {
            System.out.println("using a chain->ring reference");
            refCluster = new ClusterChainHS(nPoints, fRefPos, true);
            numDiagrams = ((ClusterChainHS)refCluster).numDiagrams();
            final double dr = 0.00001;
            CalcFFT myFFT = new CalcFFT(new IFunction() {
                public double f(double x) {
                    if (Math.abs(x-1) < 0.1*dr) {
                        return 0.5;
                    }
                    return x<1 ? 1 : 0;
                }
            }, dr, 20);
            List<Object> strands = new ArrayList<Object>();
            strands.add(2);
            List<Integer> list1 = new ArrayList<Integer>();
            for (int i=1; i<nPoints; i++) {
                list1.add(2);
            }
            strands.add(list1);
            List<Object> oneMore = new ArrayList<Object>();
            oneMore.add(strands);
            ri = numDiagrams*myFFT.value(oneMore, true)[0][0];
        }
        else if (ref == VirialHSParam.RINGS) {
            System.out.println("using a ring reference");
            refCluster = new ClusterChainHS(nPoints, fRefPos, true);
            numDiagrams = ((ClusterChainHS)refCluster).numDiagrams();
            final double dr = 0.00001;
            CalcFFT myFFT = new CalcFFT(new IFunction() {
                public double f(double x) {
                    if (Math.abs(x-1) < 0.1*dr) {
                        return 0.5;
                    }
                    return x<1 ? 1 : 0;
                }
            }, dr, 20);
            List<Object> strands = new ArrayList<Object>();
            strands.add(2);
            List<Integer> list1 = new ArrayList<Integer>();
            for (int i=1; i<nPoints; i++) {
                list1.add(2);
            }
            strands.add(list1);
            List<Object> oneMore = new ArrayList<Object>();
            oneMore.add(strands);
            ri = numDiagrams*myFFT.value(oneMore, true)[0][0];
        }
        else if (ref == VirialHSParam.CHAIN_TREE) {
            System.out.println("using a chain/tree reference ("+chainFrac+" chains)");
            ClusterChainHS cc = new ClusterChainHS(nPoints, fRefPos);
            ClusterSinglyConnected ct = new ClusterSinglyConnected(nPoints, fRefPos);
            refCluster = new ClusterWeightUmbrella(new ClusterAbstract[]{cc, ct});
            long numTreeDiagrams = 1;
            for (int i=0; i<nPoints-2; i++) {
                numTreeDiagrams *= nPoints;
            }
            ((ClusterWeightUmbrella)refCluster).setWeightCoefficients(new double[]{chainFrac/(SpecialFunctions.factorial(nPoints)/2),(1-chainFrac)/numTreeDiagrams});
            ri = Math.pow(vhs, nPoints-1);
        }
        else if (ref == VirialHSParam.RING_TREE) {
            System.out.println("using a ring/tree reference ("+ringFrac+" rings)");
            ClusterChainHS cr = new ClusterChainHS(nPoints, fRefPos, true);
            long numRingDiagrams = cr.numDiagrams();
            ClusterSinglyConnected ct = new ClusterSinglyConnected(nPoints, fRefPos);
            refCluster = new ClusterWeightUmbrella(new ClusterAbstract[]{cr, ct});
            long numTreeDiagrams = 1;
            for (int i=0; i<nPoints-2; i++) {
                numTreeDiagrams *= nPoints;
            }

            final double dr = 0.00001;
            CalcFFT myFFT = new CalcFFT(new IFunction() {
                public double f(double x) {
                    if (Math.abs(x-1) < 0.1*dr) {
                        return 0.5;
                    }
                    return x<1 ? 1 : 0;
                }
            }, dr, 20);
            List<Object> strands = new ArrayList<Object>();
            strands.add(2);
            List<Integer> list1 = new ArrayList<Integer>();
            for (int i=1; i<nPoints; i++) {
                list1.add(2);
            }
            strands.add(list1);
            List<Object> oneMore = new ArrayList<Object>();
            oneMore.add(strands);
            double ringIntegral = numRingDiagrams*myFFT.value(oneMore, true)[0][0];
            System.out.println("ring integral: "+ringIntegral);
            double treeIntegral = numTreeDiagrams*Math.pow(vhs, nPoints-1);

            ((ClusterWeightUmbrella)refCluster).setWeightCoefficients(new double[]{ringFrac/ringIntegral,(1-ringFrac)/treeIntegral});
            ri = 1;
        }
        else if (ref == VirialHSParam.RING_CHAIN_TREES) {
            System.out.println("using a ring/chain/tree reference ("+ringFrac+" rings, "+chainFrac+" chains)");
            ClusterChainHS cr = new ClusterChainHS(nPoints, fRefPos, true);
            long numRingDiagrams = cr.numDiagrams();
            ClusterChainHS cc = new ClusterChainHS(nPoints, fRefPos);
            ClusterSinglyConnected ct = new ClusterSinglyConnected(nPoints, fRefPos);
            refCluster = new ClusterWeightUmbrella(new ClusterAbstract[]{cr, cc, ct});
            long numTreeDiagrams = 1;
            for (int i=0; i<nPoints-2; i++) {
                numTreeDiagrams *= nPoints;
            }

            final double dr = 0.00001;
            CalcFFT myFFT = new CalcFFT(new IFunction() {
                public double f(double x) {
                    if (Math.abs(x-1) < 0.1*dr) {
                        return 0.5;
                    }
                    return x<1 ? 1 : 0;
                }
            }, dr, 20);
            List<Object> strands = new ArrayList<Object>();
            strands.add(2);
            List<Integer> list1 = new ArrayList<Integer>();
            for (int i=1; i<nPoints; i++) {
                list1.add(2);
            }
            strands.add(list1);
            List<Object> oneMore = new ArrayList<Object>();
            oneMore.add(strands);
            double ringIntegral = numRingDiagrams*myFFT.value(oneMore, true)[0][0];
            System.out.println("ring integral: "+ringIntegral);
            double chainIntegral = (SpecialFunctions.factorial(nPoints)/2)*Math.pow(vhs, nPoints-1);
            double treeIntegral = numTreeDiagrams*Math.pow(vhs, nPoints-1);

            ((ClusterWeightUmbrella)refCluster).setWeightCoefficients(new double[]{ringFrac/ringIntegral,chainFrac/chainIntegral,(1-ringFrac-chainFrac)/treeIntegral});
            ri = 1;
        }
        else if (ref == VirialHSParam.CHAIN_TAIL) {
            System.out.println("using a chain+tail reference");
            refCluster = new ClusterChainSoft(nPoints, fHSTail);
            vhs *= pow/(pow-3);
            numDiagrams = SpecialFunctions.factorial(nPoints)/2;
            System.out.println("# of chain diagrams "+numDiagrams);
            ri = numDiagrams*Math.pow(vhs, nPoints-1);
        }

        // (nPoints-1)! is simply not included by ClusterWheatley, so do that here.
        final double refIntegral = ri/SpecialFunctions.factorial(nPoints);
        System.out.println("reference integral: "+refIntegral);
        refCluster.setTemperature(1.0);

        ClusterAbstract[] targetDiagrams = new ClusterAbstract[]{targetCluster};

        System.out.println(steps+" steps");

        final SimulationVirial sim = new SimulationVirial(space,new SpeciesSpheresMono(space, new ElementSimple("A")), 1.0,ClusterWeightAbs.makeWeightCluster(refCluster),refCluster, targetDiagrams);
        MeterVirialBD meter = new MeterVirialBD(sim.allValueClusters);
        meter.setBox(sim.box);
        sim.setMeter(meter);
        AccumulatorAverageFixed accumulator = new AccumulatorAverageFixed(1000);
        sim.setAccumulator(accumulator);
        accumulator.setPushInterval(100000000);

        sim.integrator.getMoveManager().removeMCMove(sim.mcMoveTranslate);
        if (ref == VirialHSParam.TREE) {
            MCMoveClusterAtomHSTree mcMoveHS = new MCMoveClusterAtomHSTree(sim.getRandom(), space, sigmaHS);
            sim.integrator.getMoveManager().addMCMove(mcMoveHS);
        }
        else if (ref == VirialHSParam.CHAINS) {
            MCMoveClusterAtomHSChain mcMoveHS = new MCMoveClusterAtomHSChain(sim.getRandom(), space, sigmaHS);
            sim.integrator.getMoveManager().addMCMove(mcMoveHS);
        }
        else if (ref == VirialHSParam.RINGS) {
            MCMoveClusterAtomHSRing mcMoveHS = new MCMoveClusterAtomHSRing(sim.getRandom(), space, sigmaHS);
            sim.integrator.getMoveManager().addMCMove(mcMoveHS);
        }
        else if (ref == VirialHSParam.CHAIN_TAIL) {
            MCMoveClusterAtomChainHSTail mcMoveHS = new MCMoveClusterAtomChainHSTail(sim.getRandom(), space, sigmaHS, pow);
            sim.integrator.getMoveManager().addMCMove(mcMoveHS);
        }
        else if (ref == VirialHSParam.CHAIN_TREE) {
            MCMoveClusterAtomHSTree mcMoveHST = new MCMoveClusterAtomHSTree(sim.getRandom(), space, sigmaHS);
            sim.integrator.getMoveManager().addMCMove(mcMoveHST);
            sim.integrator.getMoveManager().setFrequency(mcMoveHST, 1-chainFrac);
            MCMoveClusterAtomHSChain mcMoveHSC = new MCMoveClusterAtomHSChain(sim.getRandom(), space, sigmaHS);
            sim.integrator.getMoveManager().addMCMove(mcMoveHSC);
            sim.integrator.getMoveManager().setFrequency(mcMoveHSC, chainFrac);
        }
        else if (ref == VirialHSParam.RING_TREE) {
            MCMoveClusterAtomHSTree mcMoveHST = new MCMoveClusterAtomHSTree(sim.getRandom(), space, sigmaHS);
            sim.integrator.getMoveManager().addMCMove(mcMoveHST);
            sim.integrator.getMoveManager().setFrequency(mcMoveHST, 1-ringFrac);
            MCMoveClusterAtomHSRing mcMoveHSCR = new MCMoveClusterAtomHSRing(sim.getRandom(), space, sigmaHS);
            sim.integrator.getMoveManager().addMCMove(mcMoveHSCR);
            sim.integrator.getMoveManager().setFrequency(mcMoveHSCR, ringFrac);
        }
        else if (ref == VirialHSParam.RING_CHAIN_TREES) {
            MCMoveClusterAtomHSRing mcMoveHSR = new MCMoveClusterAtomHSRing(sim.getRandom(), space, sigmaHS);
            sim.integrator.getMoveManager().addMCMove(mcMoveHSR);
            sim.integrator.getMoveManager().setFrequency(mcMoveHSR, ringFrac);
            MCMoveClusterAtomHSChain mcMoveHSC = new MCMoveClusterAtomHSChain(sim.getRandom(), space, sigmaHS);
            sim.integrator.getMoveManager().addMCMove(mcMoveHSC);
            sim.integrator.getMoveManager().setFrequency(mcMoveHSC, chainFrac);
            MCMoveClusterAtomHSTree mcMoveHST = new MCMoveClusterAtomHSTree(sim.getRandom(), space, sigmaHS);
            sim.integrator.getMoveManager().addMCMove(mcMoveHST);
            sim.integrator.getMoveManager().setFrequency(mcMoveHST, 1-ringFrac-chainFrac);
        }

        if (false) {
            sim.box.getBoundary().setBoxSize(space.makeVector(new double[]{10,10,10}));
            SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, space, sim.getController());
            DisplayBox displayBox0 = simGraphic.getDisplayBox(sim.box); 
//            displayBox0.setPixelUnit(new Pixel(300.0/size));
            displayBox0.setShowBoundary(false);
            ((DisplayBoxCanvasG3DSys)displayBox0.canvas).setBackgroundColor(Color.WHITE);
            
            
            ColorScheme colorScheme = new ColorScheme() {
                
                public Color getAtomColor(IAtom a) {
                    float b=a.getLeafIndex()/((float)nPoints);
                    float r=1.0f-b;
                    return new Color(r, 0f, b);
                }
            };
            displayBox0.setColorScheme(colorScheme);
            simGraphic.makeAndDisplayFrame();

            sim.setAccumulatorBlockSize(1000);
            
            final JPanel panelParentGroup = new JPanel(new java.awt.GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            final DisplayTextBox stepsBox = new DisplayTextBox();
            stepsBox.setLabel("steps");
            panelParentGroup.add(stepsBox.graphic(), gbc);

            final DisplayTextBox averageBox = new DisplayTextBox();
            averageBox.setLabel("Average");
            final DisplayTextBox errorBox = new DisplayTextBox();
            errorBox.setLabel("Error");
            JLabel jLabelPanelParentGroup = new JLabel("B"+nPoints);
            gbc.gridy = 1;
            panelParentGroup.add(jLabelPanelParentGroup, gbc);
            gbc.gridy = 2;
            gbc.gridwidth = 1;
            panelParentGroup.add(averageBox.graphic(), gbc);
            gbc.gridx = 1;
            panelParentGroup.add(errorBox.graphic(), gbc);
            simGraphic.getPanel().controlPanel.add(panelParentGroup, SimulationPanel.getVertGBC());
            
            IAction pushAnswer = new IAction() {
                public void actionPerformed() {
                    IData avgData = sim.accumulator.getData();
                    double ratio = ((DataGroup)avgData).getData(sim.accumulator.RATIO.index).getValue(1);
                    double error = ((DataGroup)avgData).getData(sim.accumulator.RATIO_ERROR.index).getValue(1);
                    data.x = ratio*refIntegral;
                    averageBox.putData(data);
                    data.x = error*Math.abs(refIntegral);
                    errorBox.putData(data);
                    
                    data.x = sim.integrator.getStepCount();
                    stepsBox.putData(data);
                }
                
                DataDouble data = new DataDouble();
            };
            IEtomicaDataInfo dataInfoSteps = new DataDouble.DataInfoDouble("B"+nPoints, Null.DIMENSION);
            stepsBox.putDataInfo(dataInfoSteps);
            IEtomicaDataInfo dataInfo = new DataDouble.DataInfoDouble("B"+nPoints, Null.DIMENSION);
            averageBox.putDataInfo(dataInfo);
            averageBox.setLabel("average");
            errorBox.putDataInfo(dataInfo);
            errorBox.setLabel("error");
            errorBox.setPrecision(2);
            sim.integrator.getEventManager().addListener(new IntegratorListenerAction(pushAnswer, 1000));

            return;
        }

        long t1 = System.currentTimeMillis();

        sim.ai.setMaxSteps(steps);
        sim.getController().actionPerformed();
        long t2 = System.currentTimeMillis();

        if (!Double.isNaN(litHSB)) System.out.println("lit value "+litHSB);

        DataGroup allYourBase = (DataGroup)accumulator.getData();
        IData averageData = allYourBase.getData(accumulator.AVERAGE.index);
        IData errorData = allYourBase.getData(accumulator.ERROR.index);
        
        System.out.println();

        double avg = averageData.getValue(0);
        double err = errorData.getValue(0);
        System.out.print(String.format("target average: %20.15e error: %9.4e\n", avg, err));

        System.out.println();

        System.out.print(String.format("abs average: %20.15e  error: %9.4e\n", avg*refIntegral, err*Math.abs(refIntegral)));

        System.out.println("time: "+(t2-t1)/1000.0);
        
        if (targetCluster instanceof ClusterWheatleyPartitionScreening) {
            ClusterWheatleyPartitionScreening tc = (ClusterWheatleyPartitionScreening)targetCluster;
            if(tc.sigCounter != null) {
                for (int i=3; i<6; i++) {
                    System.out.println("size "+i);
                    tc.sigCounter[i-1].print();
                }
                tc.sigCounter[nPoints-1].print();
                System.out.println("Fraction tabulated for each n");
                for(int i=0; i<nPoints; i++) {
                    System.out.println(tc.sigCounter[i].getn()+"\t"+tc.sigCounter[i].fractionTabulated()+"\t"+tc.sigCounter[i].getEntries());
                }
            }
        }
    }

    /**
     * Inner class for parameters
     */
    public static class VirialHSParam extends ParameterBase {
        public int nPoints = 10;
        public long numSteps = 100000000;
        public static final int TREE = 0, CHAINS = 1, CHAIN_TAIL = 4, CHAIN_TREE = 5, CRINGS = 6, RING_TREE = 7, RINGS = 8, RING_CHAIN_TREES = 9;
        public int ref = TREE;
        public double chainFrac = 1;
        public double ringFrac = 0.5;
        public int nPtsTabulated = 0;
    }
    
}
