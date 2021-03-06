package etomica.meam;

import etomica.api.IAtomList;
import etomica.api.IBoundary;
import etomica.api.IBox;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.normalmode.CoordinateDefinition;
import etomica.potential.PotentialN;
import etomica.potential.PotentialSoft;
import etomica.space.ISpace;
import etomica.space.Tensor;

/**
 * EFS (Extended Finnis-Sinclair) potential
 * 
 * @author Sabry Moustafa
 */
public class PotentialFeEAM_LS extends PotentialN implements PotentialSoft{

    protected double nFe , mFe , epsFe , aFe , cFe , rC1, rC2;
    protected IBoundary boundary; 
    protected final IVectorMutable dr, dR;
    protected IVectorMutable[] gradient; 
    protected IVectorMutable[] rhograd;
    protected double [][] secondder;
    protected final IVectorMutable Lxyz, drtmp, dRtmp, R1,R2,R3;
    protected final int[] nShells;
    protected final IVector[] a0;
    protected final CoordinateDefinition coordinateDefinition;


    
    public PotentialFeEAM_LS(CoordinateDefinition coordinateDefinition,ISpace space, double nFe ,double  mFe ,double  epsFe ,double  aFe ,double  cFe, double rC, IVector[] a0) {
        super(space);
        
        this.nFe=nFe;
        this.mFe=mFe;
        this.epsFe = epsFe;
        this.aFe = aFe;
        this.cFe = cFe;
        this.coordinateDefinition = coordinateDefinition;
        rC1 = rC;
        rC2 = rC1;
        dr=space.makeVector();
        dR=space.makeVector();
        gradient=new IVectorMutable[0];
        rhograd=new IVectorMutable[0];
		dRtmp = space.makeVector();
		drtmp = space.makeVector();
        R1 = space.makeVector();
        R2 = space.makeVector();
        R3 = space.makeVector();

    	Lxyz = space.makeVector();
        this.a0 = a0;
		nShells = new int[] {(int) Math.ceil(rC/a0[0].getX(0) - 0.49999), (int) Math.ceil(rC/a0[1].getX(1) - 0.49999), (int) Math.ceil(rC/a0[2].getX(2) - 0.49999)};
		System.out.println("nShells = " + nShells[0] + " "+ nShells[1] + " "+ nShells[2] );
    }
    
    public double getRange() {
        return rC1;
    }
    public void setRange(double rC) {
        rC1 = rC;
        rC2 = rC1;
    }
    //coordina
    public double energy(IAtomList atoms) {
      double sumV=0;
      double rhoi=0;
      double rij, Rij, Lij;
      IVector ipos=atoms.getAtom(0).getPosition();
      IVector Ri = coordinateDefinition.getLatticePosition(atoms.getAtom(0));
      IVectorMutable shiftR = space.makeVector();

      
      for(int j=1;j<atoms.getAtomCount();j++){
        IVector jpos=atoms.getAtom(j).getPosition();
        IVector Rj = coordinateDefinition.getLatticePosition(atoms.getAtom(j));
        dr.Ev1Mv2(ipos, jpos);
        dR.Ev1Mv2(Ri, Rj);
        shiftR.E(dR);
        boundary.nearestImage(dR);
        shiftR.ME(dR);
        dr.ME(shiftR);
        for(int nx = -nShells[0]; nx <= nShells[0]; nx++) {
          R1.setX(0, nx*a0[0].getX(0)); R1.setX(1, nx*a0[0].getX(1)); R1.setX(2, nx*a0[0].getX(2));
          Lxyz.E(R1);
	      for(int ny = -nShells[1]; ny <= nShells[1]; ny++) {
	        R2.setX(0, ny*a0[1].getX(0)); R2.setX(1, ny*a0[1].getX(1)); R2.setX(2, ny*a0[1].getX(2));
	        Lxyz.Ev1Pv2(R1, R2);
	        for(int nz = -nShells[2]; nz <= nShells[2]; nz++) {
	        R3.setX(0, nz*a0[2].getX(0)); R3.setX(1, nz*a0[2].getX(1)); R3.setX(2, nz*a0[2].getX(2));
	        Lxyz.Ev1Pv2(R1, R2); Lxyz.PE(R3);

  			  drtmp.Ev1Pv2(dr, Lxyz);
  			  dRtmp.Ev1Pv2(dR, Lxyz);
			  Lij = Math.sqrt(Lxyz.squared());
			  rij = Math.sqrt(drtmp.squared());
			  Rij = Math.sqrt(dRtmp.squared());
			  //pair pot.
		      if(j==1 && Lij<=rC1 && Lij > 0){ //self with 1/2 (Not needed for n-body!)
			    sumV += 0.5*epsFe*Math.pow(aFe/Lij , nFe);          		
		      }
		      if(Rij<=rC1 && atoms.getAtom(0).getLeafIndex() < atoms.getAtom(j).getLeafIndex()){
			    sumV += epsFe*Math.pow(aFe/rij , nFe);            		
		      }
		      //n-body pot.
		      if(j==1 && Lij<=rC2  && Lij > 0){ //self
		        rhoi += Math.pow(aFe/Lij , mFe);
		      }
		      if(Rij<=rC2){
		        rhoi += Math.pow(aFe/rij , mFe);
		      }
	        }
	      }
	    }
      }
      double frho = -epsFe*cFe*Math.sqrt(rhoi);
      return sumV + frho;
    }
    
    public void setBox(IBox box) {
        boundary=box.getBoundary();
    }

    public double virial(IAtomList atoms) {
      IVector ipos=atoms.getAtom(0).getPosition();
      IVector Ri = coordinateDefinition.getLatticePosition(atoms.getAtom(0));
      IVectorMutable gij2b = space.makeVector();
      IVectorMutable gijnb = space.makeVector();
      IVectorMutable shiftR = space.makeVector();

      double rhoi = 0;
      double vir2b = 0;
      double virnb = 0;
      double dvdr, Lij;
      double rij, Rij;
      double drhodr;

      for(int j=1;j<atoms.getAtomCount();j++){
        IVector jpos =atoms.getAtom(j).getPosition();
        IVector Rj = coordinateDefinition.getLatticePosition(atoms.getAtom(j));
        dr.Ev1Mv2(ipos, jpos);
        dR.Ev1Mv2(Ri, Rj);
        shiftR.E(dR);
        boundary.nearestImage(dR);
        shiftR.ME(dR);
        dr.ME(shiftR);

        for(int nx = -nShells[0]; nx <= nShells[0]; nx++) {
            R1.setX(0, nx*a0[0].getX(0)); R1.setX(1, nx*a0[0].getX(1)); R1.setX(2, nx*a0[0].getX(2));
            Lxyz.E(R1);
      	  for(int ny = -nShells[1]; ny <= nShells[1]; ny++) {
  	        R2.setX(0, ny*a0[1].getX(0)); R2.setX(1, ny*a0[1].getX(1)); R2.setX(2, ny*a0[1].getX(2));
  	        Lxyz.Ev1Pv2(R1, R2);
      	    for(int nz = -nShells[2]; nz <= nShells[2]; nz++) {
    	        R3.setX(0, nz*a0[2].getX(0)); R3.setX(1, nz*a0[2].getX(1)); R3.setX(2, nz*a0[2].getX(2));
    	        Lxyz.Ev1Pv2(R1, R2); Lxyz.PE(R3);
        	  drtmp.Ev1Pv2(dr, Lxyz);
        	  dRtmp.Ev1Pv2(dR, Lxyz);
			  Lij = Math.sqrt(Lxyz.squared());
        	  rij = Math.sqrt(drtmp.squared());
        	  Rij = Math.sqrt(dRtmp.squared());
        	  if(j==1 && Lij<=rC1 && Lij > 0){
          	    dvdr =  -epsFe*nFe/aFe*Math.pow(aFe/Lij , nFe+1.0) ;
    			gij2b.Ea1Tv1(dvdr/Lij, Lxyz);
    			vir2b += 0.5*gij2b.dot(Lxyz); // Note the 1/2 here
          	  }
        	  if(Rij<=rC1 && atoms.getAtom(0).getLeafIndex() < atoms.getAtom(j).getLeafIndex()){
        	    dvdr =  -epsFe*nFe/aFe*Math.pow(aFe/rij , nFe+1.0) ;
  			    gij2b.Ea1Tv1(dvdr/rij, drtmp);
  			    vir2b += gij2b.dot(drtmp);
        	  }

		      if(j==1 && Lij<=rC2  && Lij > 0){ //self
	            rhoi += Math.pow(aFe/Lij , mFe);
          	    drhodr= -mFe/aFe*Math.pow(aFe/Lij, mFe+1);
	        	gijnb.Ea1Tv1(drhodr/Lij, Lxyz);
	  			virnb += gijnb.dot(Lxyz);//WHY no 1/2? Bcs Fij== fij-fji = 2fij and 1/2*Fij=fij (which we compute)
			  }
			  if(Rij<=rC2){
			    rhoi += Math.pow(aFe/rij , mFe);
          	    drhodr= -mFe/aFe*Math.pow(aFe/rij, mFe+1);
	        	gijnb.Ea1Tv1(drhodr/rij, drtmp);
	  			virnb += gijnb.dot(drtmp);
			  }
  		    }
      	  }
   	    }
      }                  	  
      double f=Math.sqrt(rhoi);
      virnb *= -epsFe*cFe/2.0/f;
      double virial = vir2b + virnb;
      return virial;
    }

    
    
    public IVector[] gradient(IAtomList atoms) {
      if(gradient.length<atoms.getAtomCount()){
        rhograd=new IVectorMutable[atoms.getAtomCount()];
        gradient=new IVectorMutable[atoms.getAtomCount()];
        for(int j=0;j<atoms.getAtomCount();j++){
          gradient[j]=space.makeVector();
          rhograd[j]=space.makeVector();
        }
      }
      gradient[0].E(0);
      IVector ipos=atoms.getAtom(0).getPosition();
      IVector Ri = coordinateDefinition.getLatticePosition(atoms.getAtom(0));
        IVectorMutable shiftR = space.makeVector();
        double rhoi=0;
    	double dvdr;
        double rij, Rij, Lij;
        double drhodr;
        //S: Do NOT start from 0 as it will be -SUM(j>0); see below
        for(int j=1;j<atoms.getAtomCount();j++){
          gradient[j].E(0);
          rhograd[j].E(0);
          IVector jpos=atoms.getAtom(j).getPosition();
          IVector Rj = coordinateDefinition.getLatticePosition(atoms.getAtom(j));
          dr.Ev1Mv2(ipos, jpos);
          dR.Ev1Mv2(Ri, Rj);
          shiftR.E(dR);
          boundary.nearestImage(dR);
          shiftR.ME(dR);
          dr.ME(shiftR);
    	  for(int nx = -nShells[0]; nx <= nShells[0]; nx++) {
              R1.setX(0, nx*a0[0].getX(0)); R1.setX(1, nx*a0[0].getX(1)); R1.setX(2, nx*a0[0].getX(2));
              Lxyz.E(R1);
    	    for(int ny = -nShells[1]; ny <= nShells[1]; ny++) {
    	        R2.setX(0, ny*a0[1].getX(0)); R2.setX(1, ny*a0[1].getX(1)); R2.setX(2, ny*a0[1].getX(2));
    	        Lxyz.Ev1Pv2(R1, R2);
      	   	  for(int nz = -nShells[2]; nz <= nShells[2]; nz++) {
      	        R3.setX(0, nz*a0[2].getX(0)); R3.setX(1, nz*a0[2].getX(1)); R3.setX(2, nz*a0[2].getX(2));
    	        Lxyz.Ev1Pv2(R1, R2); Lxyz.PE(R3);
      			drtmp.Ev1Pv2(dr, Lxyz);
      			dRtmp.Ev1Pv2(dR, Lxyz);
  			    Lij = Math.sqrt(Lxyz.squared());
    			rij = Math.sqrt(drtmp.squared());
    			Rij = Math.sqrt(dRtmp.squared());
    			
    			//2-body
	            if(Rij<=rC1 && atoms.getAtom(0).getLeafIndex() < atoms.getAtom(j).getLeafIndex()){
			      dvdr =  -epsFe*nFe/aFe*Math.pow(aFe/rij , nFe+1.0) ;
			      gradient[j].PEa1Tv1(-dvdr/rij, drtmp);
		        }
	            //n-body
		        if(j==1 && Lij<=rC2  && Lij > 0){ //self
			      rhoi += Math.pow(aFe/Lij , mFe);
			    }
		        if(Rij<=rC2){
		          rhoi += Math.pow(aFe/rij , mFe);
		          drhodr= -mFe/aFe*Math.pow(aFe/rij, mFe+1);
		          rhograd[j].PEa1Tv1(-drhodr/rij, drtmp);
		        }
    	      }
    	    }
    	  }          
        }//End j
        double f=Math.sqrt(rhoi);
        for (int j=1;j<atoms.getAtomCount();j++){
            gradient[j].PEa1Tv1(-epsFe*cFe/2.0/f,rhograd[j]);//Adds the n-body to the 2-body for j
            gradient[0].ME(gradient[j]);
        }
        return gradient;
    }

    public IVector[] gradient(IAtomList atoms, Tensor pressureTensor) {
        return gradient(atoms);
    }
 }
