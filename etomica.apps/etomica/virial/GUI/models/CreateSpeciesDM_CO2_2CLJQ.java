package etomica.virial.GUI.models;


import etomica.api.ISpecies;
import etomica.config.ConformationLinear;
import etomica.potential.P22CLJQ;

import etomica.space.ISpace;
import etomica.space.Space;
import etomica.space3d.Space3D;

import etomica.units.Kelvin;
import etomica.virial.SpeciesFactory;

import etomica.virial.SpeciesFactoryTangentSpheres;

public class CreateSpeciesDM_CO2_2CLJQ implements CreateSpeciesDM_IFactory,Cloneable{
	private static String MoleculeDisplayName = "CO2 - 2CCLJQ";
	private Space space;
	private double[] sigma;
	private double[] epsilon;
	private double[] moment;
	

	private double sigmaHSRef;
	
	
	private double bondLength;
	
	
	private ConformationLinear conformation;
	
	private int SpeciesID;
	
	private String[] PotentialSites = {"CO2"};
	
	
	
	public String getPotentialSiteAtIndex(int index) {
		return PotentialSites[index];
	}

	private int id;
	private static int numberOfInstances = 0;
	
	private String[] ComponentParameters  =  {"SIGMA","EPSILON", "MOMENT"};
			
	private String[] SharedComponentParameters ={"BONDL"};
	
	private String[][] ParamAndValues; 

	
	private String[][] ComponentValues = {{"3.0354",Double.toString(Kelvin.UNIT.toSim(125.317)),Double.toString(3.0255*Kelvin.UNIT.toSim(125.317)*Math.pow(3.0354,5))}};
	
	private String[] SharedComponentValues = {"2.1347"};
	
	
	private String[] SimEnvParameters = {"SIGMAHSREF"};
	
	private String[] SimEnvValues = {Double.toString(1.5*3.0354)};
	
	
	
	
	//Constructors for different Instantiations
	
	
	public CreateSpeciesDM_CO2_2CLJQ(){
		space = Space3D.getInstance();
		sigma = new double[PotentialSites.length];
		epsilon = new double[PotentialSites.length];
		moment = new double[PotentialSites.length];
		ParamAndValues=setParameterValues();
		//id=++numberOfInstances;
	}
	
	private String[][] setParameterValues() {
		
		int NoOfParam = ComponentParameters.length;
		int NoOfCommonParam = SharedComponentParameters.length;
		int NoOfSites = PotentialSites.length;
		int totalNoOfParam = NoOfParam*NoOfSites;
		String[][] ReturnArray = new String[totalNoOfParam][2];
		int index = 0;
		for(int i=0;i<NoOfSites;i++){
			for(int j=0;j<NoOfParam;j++){
				
				if(ComponentParameters[j]=="SIGMA"){
					setSigma(Double.parseDouble(ComponentValues[i][j]),i);
					
				}
				if(ComponentParameters[j]=="EPSILON"){
					setEpsilon(Double.parseDouble(ComponentValues[i][j]),i);
					
				}
				if(ComponentParameters[j]=="MOMENT"){
					setMoment(Double.parseDouble(ComponentValues[i][j]),i);
					
				}
				
				ReturnArray[index][0] = ComponentParameters[j]+PotentialSites[i];
				ReturnArray[index][1] = ComponentValues[i][j];
				index++;
			}
		}
		for(int k = 0;k<NoOfCommonParam;k++){
			if(SharedComponentParameters[k]=="BONDL"){
				setBondLength(Double.parseDouble(SharedComponentValues[k]));
			}
		}
		
		int NoOfSimEnvParam = 1;
		for(int l = 0;l<NoOfSimEnvParam;l++){
			
			
			if(SimEnvParameters[l]=="SIGMAHSREF"){
				setSigmaHSRef(Double.parseDouble(SimEnvValues[l]));
			}
		}
		return ReturnArray;
		
		
	}
	
	

	public double getSigmaHSRef() {
		return sigmaHSRef;
	}

	public void setSigmaHSRef(double sigmaHSRef) {
		this.sigmaHSRef = sigmaHSRef;
	}

	public int getId() {
		return id;
	}
	
	 public Object clone(){
		 try{
			 CreateSpeciesDM_CO2_2CLJQ cloned = (CreateSpeciesDM_CO2_2CLJQ)super.clone();
			 return cloned;
		  }
		  catch(CloneNotSupportedException e){
		     System.out.println(e);
		     return null;
		   }
	 }

	
	public int getSpeciesID() {
		return SpeciesID;
	}

	public void setSpeciesID(int speciesID) {
		SpeciesID = speciesID;
	}
	//Getter for ConformationLinear class
	public ConformationLinear getConformation() {
		return this.conformation;
	}

	public void setConformation() {
		this.conformation = new ConformationLinear(this.space, this.bondLength);
	}
	
	//Creates the LJ Molecule Species
	public ISpecies createSpecies(){
		
		setConformation();
		SpeciesFactory speciesFactory = new SpeciesFactoryTangentSpheres(2,this.getConformation());
		return speciesFactory.makeSpecies(this.space);
	}
	
	//Creates the LJ Molecule Species
	public SpeciesFactory createSpeciesFactory(){
		
		setConformation();
		SpeciesFactory speciesFactory = new SpeciesFactoryTangentSpheres(2,this.getConformation());
		return speciesFactory;
	}
	
	public double getSigma(int index) {
		return sigma[index];
	}

	public void setSigma(double sigma, int index) {
		this.sigma[index] = sigma;
	}

	public double getEpsilon(int index) {
		return epsilon[index];
	}

	public void setEpsilon(double epsilon, int index) {
		this.epsilon[index] = epsilon;
	}

	public double getMoment(int index) {
		return moment[index];
	}

	public void setMoment(double moment, int index) {
		this.moment[index] = moment;
	}

	public double getBondLength() {
		return bondLength;
	}
	
	public void setBondLength(double bondLength) {
		this.bondLength = bondLength;
	}

	@Override
	public int getParameterCount() {
		return 4;
	}

	@Override
	public void setParameter(String Parameter, String ParameterValue) {
		// TODO Auto-generated method stub
		for(int i=0;i<PotentialSites.length;i++){
			if(Parameter.toUpperCase().equals(PotentialParamDM_Description.SIGMA.toString()+PotentialSites[i])){
				setSigma(Double.parseDouble(ParameterValue),i); 
			}
			if(Parameter.toUpperCase().equals(PotentialParamDM_Description.EPSILON.toString()+PotentialSites[i])){
				setEpsilon(Double.parseDouble(ParameterValue),i); 
			}
			if(Parameter.toUpperCase().equals(PotentialParamDM_Description.MOMENT.toString()+PotentialSites[i])){
				setMoment(Double.parseDouble(ParameterValue),i); 
			}
			
		}
		if(Parameter.toUpperCase().equals(PotentialParamDM_Description.BONDL.toString())){
			setBondLength(Double.parseDouble(ParameterValue)); 
		}
		
		if(Parameter.toUpperCase().equals(PotentialParamDM_Description.SIGMAHSREF.toString())){
			setSigmaHSRef(Double.parseDouble(ParameterValue)); 
		}
		
	}

	@Override
	public String getDescription(String Parameter) {
		String Description = null;
		for(int i = 0;i <PotentialSites.length;i++){
			if(Parameter.toUpperCase().equals(PotentialParamDM_Description.SIGMA.toString()+PotentialSites[i])){
				Description = PotentialParamDM_Description.SIGMA.Description();
			}
			if(Parameter.toUpperCase().equals(PotentialParamDM_Description.EPSILON.toString()+PotentialSites[i])){
				Description = PotentialParamDM_Description.EPSILON.Description();
			}
		
			if(Parameter.toUpperCase().equals(PotentialParamDM_Description.MOMENT.toString()+PotentialSites[i])){
				Description = PotentialParamDM_Description.MOMENT.Description();
			}
		}
		if(Parameter.toUpperCase().equals(PotentialParamDM_Description.BONDL.toString())){
			Description = PotentialParamDM_Description.BONDL.Description();
		}
		
		if(Parameter.toUpperCase().equals(PotentialParamDM_Description.TEMPERATURE.toString())){
			Description = PotentialParamDM_Description.TEMPERATURE.Description();
		}
		if(Parameter.toUpperCase().equals(PotentialParamDM_Description.STEPS.toString())){
			Description = PotentialParamDM_Description.STEPS.Description();
		}
		if(Parameter.toUpperCase().equals(PotentialParamDM_Description.SIGMAHSREF.toString())){
			Description = PotentialParamDM_Description.SIGMAHSREF.Description();
		}
		return Description;
	}

	//Testing Class
	public static void main(String[] args){
		CreateSpeciesDM_CO2_2CLJQ lj = new CreateSpeciesDM_CO2_2CLJQ();
		
	}

	@Override
	public Double getDoubleDefaultParameters(String Parameter) {
		// TODO Auto-generated method stub
		Double parameterValue = null;
		for(int i=0;i<PotentialSites.length;i++){
			if(Parameter.toUpperCase().equals(PotentialParamDM_Description.SIGMA.toString()+PotentialSites[i])){
				parameterValue = getSigma(i);
			}
			if(Parameter.toUpperCase().equals(PotentialParamDM_Description.EPSILON.toString()+PotentialSites[i])){
				parameterValue = getEpsilon(i);
			}
		
			if(Parameter.toUpperCase().equals(PotentialParamDM_Description.MOMENT.toString()+PotentialSites[i])){
				parameterValue = getMoment(i);
			}
		}
		if(Parameter.toUpperCase().equals(PotentialParamDM_Description.BONDL.toString())){
			parameterValue = getBondLength();
		}
		
		if(Parameter.toUpperCase().equals(PotentialParamDM_Description.SIGMAHSREF.toString())){
			parameterValue = getSigmaHSRef();
		}
		
		
		return parameterValue;
	}



	public String[] getParametersArray() {
		return ComponentParameters;
	}

	@Override
	public String getCustomName() {
		// TODO Auto-generated method stub
		return "2CLJQ";
	}


	public String getPotentialSites(int index) {
		return PotentialSites[index];
	}

	@Override
	public String[][] getParamAndValues() {
		// TODO Auto-generated method stub
		return ParamAndValues;
	}

	@Override
	public String[] getPotentialSites() {
		// TODO Auto-generated method stub
		return PotentialSites;
	}

	
	@Override
	public String getMoleculeDisplayName() {
		// TODO Auto-generated method stub
		return MoleculeDisplayName;
	}

	@SuppressWarnings("rawtypes")
	
	public Class getPotential() {
		// TODO Auto-generated method stub
		return P22CLJQ.class;
	}
	
	@Override
	public Space getSpace() {
		// TODO Auto-generated method stub
		return this.space;
	}

	@Override
	public boolean hasElectrostaticInteraction() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getNonBondedInteractionModel() {
		// TODO Auto-generated method stub
		return "LennardJonesWithQuadrapole";
	}


}