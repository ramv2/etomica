/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.virial.simulations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import etomica.api.IBox;
import etomica.api.IMoleculeList;
import etomica.api.IPotential;
import etomica.chem.elements.ElementSimple;
import etomica.listener.IntegratorListenerAction;
import etomica.math.SpecialFunctions;
import etomica.space.ISpace;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;
import etomica.util.ParameterBase;
import etomica.util.ParseArgs;
import etomica.util.RandomMersenneTwister;
import etomica.virial.ClusterAbstract;
import etomica.virial.ClusterChainHS;
import etomica.virial.ClusterSinglyConnected;
import etomica.virial.ClusterWeightAbs;
import etomica.virial.ClusterWeightUmbrella;
import etomica.virial.ClusterWheatleyExtendSW;
import etomica.virial.IntSet;
import etomica.virial.IntSet.PropertyBin;
import etomica.virial.MCMoveClusterAtomHSChain;
import etomica.virial.MCMoveClusterAtomHSRing;
import etomica.virial.MCMoveClusterAtomHSTree;
import etomica.virial.MayerEHardSphere;
import etomica.virial.MayerFunction;
import etomica.virial.MeterVirialEBinMultiThreaded.MyData;
import etomica.virial.MeterVirialEBinMultiThreaded;
import etomica.virial.cluster.Standard;

/**
 * Calculation for virial coefficients of hard spheres
 */
public class VirialSQWBinMultiThreaded {


    public static void main(String[] args) {

        VirialHSBinParam params = new VirialHSBinParam();
        if (args.length > 0) {
            ParseArgs.doParseArgs(params, args);
        }
        else {
            params.nPoints = 4;
            params.numSteps = 10000000L;
            params.chainFrac = 0.1;
            params.ringFrac = 0.8;
            params.lambda = 2.0;
            params.nThreads = 1;
        }
        

        final int nPoints = params.nPoints;
        long steps = params.numSteps;
        final double chainFrac = params.chainFrac;
        final double ringFrac = params.ringFrac;
        final double lambda = params.lambda;
        final double sigmaHS = lambda;
        final int nThreads = params.nThreads;
        final double w = params.w;
        final int[] allRandomSeeds = params.randomSeeds;

        final double vhs = (4.0/3.0)*Math.PI*sigmaHS*sigmaHS*sigmaHS;

        System.out.println("SQW sampling B"+nPoints);
		
        final Space space = Space3D.getInstance();
        
        MayerEHardSphere fTargete2 = new MayerEHardSphere(1.0);
        MayerFunction fTargetf1 = new MayerFunction() {
            final double sigma2 = 1.0;
            final double well2 = lambda*lambda;
            
            public void setBox(IBox box) {}
            
            public IPotential getPotential() {return null;}
            
            public double f(IMoleculeList pair, double r2, double beta) {
                if (r2 < sigma2 || r2 > well2) return 0;
                return 1;
            }
        };
        MayerFunction fRefPos = new MayerFunction() {
            
            public void setBox(IBox box) {}
            
            public IPotential getPotential() {return null;}
            
            public double f(IMoleculeList pair, double r2, double beta) {
                return r2 < sigmaHS*sigmaHS ? 1 : 0;
            }
        };

        Map<IntSet,MeterVirialEBinMultiThreaded.MyData> allMyData = new HashMap<IntSet,MeterVirialEBinMultiThreaded.MyData>();
        boolean doReweight = true;
        if (w < 0) {
            // meter0 exists only to allow us to read in the weights file (if it exists) and to populate allMyData
            // this needs to be done (finished) before any thread gets started.
        	MeterVirialEBinMultiThreaded meter0 = new MeterVirialEBinMultiThreaded(null, null, null, new long[1], allMyData, 0, true);
            meter0.readWeights(params.runName+nPoints+"_weights.dat");
            doReweight = allMyData.size() == 0;
        }
        if (!doReweight && !params.shareData) {
            // reweighting not needed, we don't want to share data
            allMyData = null;
        }
        else if (doReweight && !params.shareData) {
            // reweighting is needed, we have to share data
            params.shareData = true;
        }
        System.out.println("using a ring/chain/tree reference ("+ringFrac+" rings, "+chainFrac+" chains)");

        // (nPoints-1)! is simply not included by ClusterWheatley, so do that here.
        double refIntegral = 1;

        double tRatio = 0;
        double t0Tree = -3.18286 + nPoints*(1.66621 + nPoints*(-0.199946 + nPoints*0.0113384));
        double t0Chain = -1.10143 + nPoints*(0.707278 + nPoints*(-0.0830664 + nPoints*0.00582492));
        double t0Ring = -3.10337 + nPoints*(1.81457 + nPoints*(-0.243239 + nPoints*0.0191835));
        t0Tree = Math.exp(-8.727299006700907e-01 + 2.437041849455276e-01*nPoints);
        t0Chain = Math.exp(-1.076280422298466e+00 + 2.319522918620784e-01*nPoints);
        t0Ring = Math.exp(-6.048590247634202e-01 + 2.898441951777069e-01*nPoints);
        double tc = Math.exp(8.644763200709733e-01 + 3.227819802929632e-03*nPoints +  5.355934080103668e-02*nPoints*nPoints);
        double fUnscreenedTree = 0.00446301 + 224.919*Math.pow(nPoints, -5.29);
        double fUnscreenedChain = 0.00521601 + 90.2381*Math.pow(nPoints, -4.73159);
        double fUnscreenedRing = 0.0409468 + 1530.97*Math.pow(nPoints, -5.5381);
        double tRatioTree = 5.858 + 0.0190644*Math.pow(2.65435, nPoints);
        double tRatioChain = 7.22624 + 0.0431276*Math.pow(2.53604, nPoints);
        double tRatioRing = 3.95055 + 0.0249246*Math.pow(2.39758, nPoints);
        double t0 = 0;
        double t1Tree = t0Tree*(1-chainFrac-ringFrac);  // total time sampling
        double t2Tree = tRatioTree*fUnscreenedTree*t1Tree; // total time computing
        double t1Chain = t0Chain*chainFrac;
        double t2Chain = tRatioChain*fUnscreenedChain*t1Chain;
        double t1Ring = t0Ring*ringFrac;
        double t2Ring = tRatioRing*fUnscreenedRing*t1Ring;
        t0 = t1Tree + t1Chain + t1Ring;
        double fUnscreened = fUnscreenedTree*(1-chainFrac-ringFrac) + fUnscreenedChain*chainFrac + fUnscreenedRing*ringFrac; // overall fraction unscreened
//            System.out.println(t0Tree*tRatioTree+" "+t0Chain*tRatioChain+" "+t0Ring*tRatioRing);
//            System.out.println((1-chainFrac-ringFrac)*t0Tree*tRatioTree + chainFrac*t0Chain*tRatioChain + ringFrac*t0Ring*tRatioRing);
        tRatio = (t2Tree + t2Chain + t2Ring)/(fUnscreened*(t1Tree + t1Chain + t1Ring));
        tc = tRatio*t0;
//            System.out.println("old tRatio: "+tRatio);
//            System.out.println((t2Tree+t2Chain+t2Ring)/((1-chainFrac-ringFrac)*t0Tree*tRatioTree + chainFrac*t0Chain*tRatioChain + ringFrac*t0Ring*tRatioRing)+" "+fUnscreened);
//            System.out.println(((1-chainFrac-ringFrac)*t0Tree*tRatioTree + chainFrac*t0Chain*tRatioChain + ringFrac*t0Ring*tRatioRing)/(t1Tree + t1Chain + t1Ring));
//            System.out.println("new tRatio: "+tc/t0);
        tRatio = tc/t0;
        System.out.println("tRatio: "+tRatio+"   tc: "+tc+"  ts: "+t0);

//        MeterVirialBDBinMultiThreadedOld.setTRatio(tRatio);


        long t1 = System.currentTimeMillis();
        SimulationWorker[] sw = new SimulationWorker[nThreads];
        long[] totalCount = new long[nThreads];
        for (int it=0; it<nThreads; it++) {
            int[] mySeeds = allRandomSeeds.length > 0 ? new int[allRandomSeeds.length/nThreads] : null;
            if (mySeeds != null) {
                System.arraycopy(allRandomSeeds, allRandomSeeds.length/nThreads*it, mySeeds, 0, mySeeds.length);
            }
            sw[it] = new SimulationWorker(it, nPoints, fTargetf1, fTargete2, fRefPos, lambda, vhs, chainFrac, ringFrac, steps, space, params.runName, tRatio, allMyData, w, totalCount, doReweight, mySeeds);
        }
        for (int it=0; it<nThreads; it++) {
            sw[it].start();
        }
        for (int it=0; it<nThreads; it++) {
            try {
                sw[it].join();
            }
            catch (InterruptedException ex) {
                System.out.println("interrupted "+it+"?  really?");
            }
        }

        long t2 = System.currentTimeMillis();
        System.out.println("total steps: "+(nThreads*steps));

        File out = null;
        String filename = null;
        int rn = 1;
        while (true) {
            filename = params.runName+nPoints+"_run"+rn+"_raw.dat";
            out = new File(filename);
            try {
                if (out.createNewFile()) break;
            }
            catch (IOException e) {
                throw new RuntimeException("couldn't create raw output");
            }
            rn++;
        }
        if (allMyData == null) {
            // data is not shared.  merge it now.
            for (int i=1; i<nThreads; i++) {
                sw[0].meter.mergeData(sw[i].meter.getAllMyData());
            }
            allMyData = sw[0].meter.getAllMyData();
        }
        MeterVirialEBinMultiThreaded.writeData(filename, allMyData, nThreads*steps);
        
        if (doReweight) {
            System.out.println();
            
            MeterVirialEBinMultiThreaded.setQuiet(false);
            MeterVirialEBinMultiThreaded.recomputeWeights(allMyData, nThreads*steps, nPoints);

            List<IntSet> pvs = new ArrayList<IntSet>();
            pvs.addAll(allMyData.keySet());
            Collections.sort(pvs);
            
            long totalSampleCount = 0;
            long totalNotScreenedCount = 0;
            for (int i=0; i<1+nPoints*(nPoints-1)/2; i++) {
	            double sum = 0;
	            double sumErrNum = 0;
	            double sumErrStdev = 0;
	            int nSets = 0;
	            for (IntSet pv : pvs) {
	                MyData amd = allMyData.get(pv);
	                long c = amd.unscreenedCount;
	    
	                nSets++;
	                if (i==0) totalNotScreenedCount += c;
	                long sc = amd.sampleCount;
	                if (sc == 0) {
	                    continue;
	                }
	    
	                if (i==0) totalSampleCount += sc;
	    
	                double avg = amd.getAvg(i);
	                double var = amd.getVar(i);
	                sum += c*avg;
	                sumErrStdev += var/sc*c*c;
	                sumErrNum += c*((double)(nThreads*steps - c))/(nThreads*steps)*avg*avg;
	            }
	            if (i==0) System.out.println(nSets+" sets");
	            sum *= refIntegral/(nThreads*steps);
	            double finalErr = Math.sqrt(sumErrStdev + sumErrNum)*Math.abs(refIntegral)/(nThreads*steps);
	    
	            System.out.println();
	    
	            System.out.println(i+" average: "+sum+"  error: "+finalErr);
	            if (sumErrNum > 0) System.out.println("  number variance fraction: "+sumErrNum/(sumErrStdev + sumErrNum));
	    
//	            System.out.println("Difficulty: "+(finalErr*Math.sqrt(t2-t1)));
            }
            System.out.println("number time fraction: "+(nThreads*steps)/(nThreads*steps + totalSampleCount*tRatio));
            System.out.println("fraction not screened: "+((double)totalNotScreenedCount)/(nThreads*steps));
            System.out.println("fraction measured: "+((double)totalSampleCount)/totalNotScreenedCount);
        }
        System.out.println("time: "+(t2-t1)/1000.0);
//        }
    }

    public static class SimulationWorker extends Thread {
        
        protected final int nPoints;
        protected final MayerFunction fTargetf1, fTargete2;
        protected final MayerFunction fRefPos;
        protected final double lambda;
        protected final double vhs;
        protected final double chainFrac, ringFrac;
        protected final long steps;
        protected final ISpace space;
        protected final String runName;
        protected final double tRatio;
        protected final Map<IntSet,MeterVirialEBinMultiThreaded.MyData> allMyData;
        protected final int iThread;
        protected final double w;
        protected final long[] totalCount;
        protected final boolean doReweight;
        protected final int[] mySeeds;
        public MeterVirialEBinMultiThreaded meter;
        
        public SimulationWorker(int iThread, int nPoints, MayerFunction fTargetf1, MayerFunction fTargete2,
                MayerFunction fRefPos, double lambda, double vhs, double chainFrac, double ringFrac,
                long steps, ISpace space, String runName, double tRatio,
                Map<IntSet,MeterVirialEBinMultiThreaded.MyData> allMyData, double w, long[] totalCount,
                boolean doReweight, int[] mySeeds) {
            this.iThread = iThread;
            this.nPoints = nPoints;
            this.fTargetf1 = fTargetf1;
            this.fTargete2 = fTargete2;
            this.fRefPos = fRefPos;
            this.lambda = lambda;
            this.vhs = vhs;
            this.chainFrac = chainFrac;
            this.ringFrac = ringFrac;
            this.steps = steps;
            this.space = space;
            this.runName = runName;
            this.tRatio = tRatio;
            this.allMyData = allMyData;
            this.w = w;
            this.totalCount = totalCount;
            this.doReweight = doReweight;
            this.mySeeds = mySeeds;
        }
        
        public void run() {
            long t1 = System.currentTimeMillis();
            final ClusterWheatleyExtendSW targetCluster = new ClusterWheatleyExtendSW(nPoints, fTargetf1, fTargete2);
            targetCluster.setTemperature(1.0);
            
            ClusterAbstract refCluster = null;
            ClusterChainHS cr = new ClusterChainHS(nPoints, fRefPos, true);
            long numRingDiagrams = cr.numDiagrams();
            double ringIntegral = numRingDiagrams*Standard.ringHS(nPoints)*Math.pow(lambda, 3*(nPoints-1));
            double chainIntegral = (SpecialFunctions.factorial(nPoints)/2)*Math.pow(vhs, nPoints-1);
            ClusterChainHS crc = new ClusterChainHS(nPoints, fRefPos, chainFrac/chainIntegral, ringFrac/ringIntegral);
            ClusterSinglyConnected ct = new ClusterSinglyConnected(nPoints, fRefPos);
            refCluster = new ClusterWeightUmbrella(new ClusterAbstract[]{crc, ct});
            long numTreeDiagrams = 1;
            for (int i=0; i<nPoints-2; i++) {
                numTreeDiagrams *= nPoints;
            }

            double treeIntegral = numTreeDiagrams*Math.pow(vhs, nPoints-1);

            // weighting for chain and ring are handled internally
            ((ClusterWeightUmbrella)refCluster).setWeightCoefficients(new double[]{1,(1-ringFrac-chainFrac)/treeIntegral});
            refCluster.setTemperature(1.0);
            
            ClusterAbstract[] targetDiagrams = new ClusterAbstract[]{targetCluster};


            System.out.println("thread "+iThread+", "+steps+" steps");
            
            final SimulationVirial sim = new SimulationVirial(space,new SpeciesSpheresMono(space, new ElementSimple("A")), 1.0,ClusterWeightAbs.makeWeightCluster(refCluster),refCluster, targetDiagrams, false, mySeeds);
            int[] randSeed = ((RandomMersenneTwister)sim.getRandom()).getSeedArray();
            if (randSeed == null) {
                System.out.println(iThread+" Random seed: "+((RandomMersenneTwister)sim.getRandom()).getSeed());
            }
            else {
                System.out.println(iThread+" Random seeds: "+Arrays.toString(randSeed));
            }
            sim.setMeter(null);
            PropertyBin pod0 = new PropertyBin() {
                final IntSet pv = new IntSet(new int[1]);
                public IntSet value() {
                    return pv;
                }
            };
            PropertyBin pod = new PropertyBin() {
                final IntSet pv = new IntSet(new int[2]);
                public IntSet value() {
                    pv.v[0] = targetCluster.getCoreEdgeCount();
                    pv.v[1] = targetCluster.getWellEdgeCount();
                    return pv;
                }
            };
            meter = new MeterVirialEBinMultiThreaded(targetCluster, sim.getRandom(), pod, totalCount, allMyData, iThread, doReweight);
            meter.setBox(sim.box);
            if (w>=0) {
                meter.setWeight(w);
            }
            else if (allMyData == null) {
                meter.readWeights(runName+nPoints+"_weights.dat");
            }
//            double tRatio = nPoints == 4 ? 30.0 : 0.154*Math.exp(1.0683*nPoints);

            // based on fit of data using weight=1, weight=0, each for 10^9 steps.
//            double tRatio = 0.44869 * Math.exp(0.64714 * nPoints);
            //double tRatio = 0.292077*nPoints*nPoints + 0.00378375*Math.pow(3, nPoints);
            sim.integrator.getEventManager().addListener(new IntegratorListenerAction(meter));
            
            sim.integrator.getMoveManager().removeMCMove(sim.mcMoveTranslate);

            MCMoveClusterAtomHSRing mcMoveHSR = new MCMoveClusterAtomHSRing(sim.getRandom(), space, lambda);
            sim.integrator.getMoveManager().addMCMove(mcMoveHSR);
            sim.integrator.getMoveManager().setFrequency(mcMoveHSR, ringFrac);
            MCMoveClusterAtomHSChain mcMoveHSC = new MCMoveClusterAtomHSChain(sim.getRandom(), space, lambda);
            sim.integrator.getMoveManager().addMCMove(mcMoveHSC);
            sim.integrator.getMoveManager().setFrequency(mcMoveHSC, chainFrac);
            MCMoveClusterAtomHSTree mcMoveHST = new MCMoveClusterAtomHSTree(sim.getRandom(), space, lambda);
            sim.integrator.getMoveManager().addMCMove(mcMoveHST);
            sim.integrator.getMoveManager().setFrequency(mcMoveHST, 1-ringFrac-chainFrac);
            MeterVirialEBinMultiThreaded.setTRatio(tRatio);

            sim.ai.setMaxSteps(steps);
            sim.getController().actionPerformed();
            long t2 = System.currentTimeMillis();
            System.out.println("thread "+iThread+" time: "+(t2-t1)*0.001);
        }
    }
    
    public static class DooDad {
        boolean[] excludedCliques;
        public DooDad(int nPoints) {
            excludedCliques = new boolean[1<<nPoints];
        }
        public int value(int cliqueCount, int[] cliqueList) {
            int ncbonds = 0;
            for (int i=0; i<cliqueCount; i++) {
                excludedCliques[i] = false;
            }
            
            for (int i=cliqueCount-1; i>=2; i--) {
                if (excludedCliques[i]) continue;
                int ci = cliqueList[i];
                for (int j=i-1; j>=0; j--) {
                    if (excludedCliques[j]) continue;
                    if ((ci|cliqueList[j]) == ci) {
                        // j is a subset of i; ignore j
                        excludedCliques[j] = true;
                        continue;
                    }
                }
            }
            for (int i=cliqueCount-1; i>=1; i--) {
                if (excludedCliques[i]) continue;
                int ci = cliqueList[i];
                for (int j=i-1; j>=0; j--) {
                    if (excludedCliques[j]) continue;
                    int cj = cliqueList[j];
                    if ((ci|cj) != ci+cj) {
                        // i and j share some points in common
                        ncbonds++;
                    }
                }
            }
            return ncbonds;
        }
    }
    
    /**
     * Inner class for parameters
     */
    public static class VirialHSBinParam extends ParameterBase {
        public int nPoints = 6;
        public double lambda = 1.5;
        public long numSteps = 100000000;
        public double chainFrac = 0.4;
        public double ringFrac = 0.4;
        public String runName = "sqw";
        public int nThreads = 1;
        public double w = -1;
        public int[] randomSeeds = new int[0];
        public boolean shareData = true;
    }
    
}
