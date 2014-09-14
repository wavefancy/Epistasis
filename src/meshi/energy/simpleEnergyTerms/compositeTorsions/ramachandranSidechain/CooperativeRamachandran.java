package meshi.energy.simpleEnergyTerms.compositeTorsions.ramachandranSidechain;
import meshi.energy.simpleEnergyTerms.compositeTorsions.*;

import meshi.energy.*;
import meshi.geometry.*;
import meshi.molecularElements.atoms.*;
import meshi.molecularElements.*;
import meshi.util.*;
import meshi.parameters.*;

public class CooperativeRamachandran extends AbstractEnergy implements CompositeTorsionsDefinitions {
    private        final double[] targetSigma          = new double[ResidueType.values().length];
    private        final double[] diffSigma            = new double[ResidueType.values().length];
    private              double[] sumPerResidueType;
    public  static final int      numberOfResidueTypes = ResidueType.values().length;

    private static Residue residue;
    private static ResidueType type;
    private static double   diff, factor;
    private static int n;
    private RamachandranSidechainEnergy ramachandranSidechainEnergy;
    private ResidueTorsionsList residueTorsionsList;

    public CooperativeRamachandran(RamachandranSidechainEnergy ramachandranSidechainEnergy, double weight, CooperativeRamachandranParameters parameters) {
	super(toArray(), weight);
    this.ramachandranSidechainEnergy = ramachandranSidechainEnergy;
    residueTorsionsList = ramachandranSidechainEnergy.residueTorsionsList;
	comment = "CooperativeRamachandran";
	for (ResidueType type:ResidueType.values()) {
	    int n = ramachandranSidechainEnergy.numberOfResiduesPerType()[type.ordinal()];
	    targetSigma[type.ordinal()] = parameters.minAvgSigma[type.ordinal()] * n;
	}
	this.sumPerResidueType = ramachandranSidechainEnergy.sumPerResidueType();
    }

   public double evaluate() {
	return evaluate(false);
   }
    public void evaluateAtoms() {
	evaluate(true);
    }
    
    public double evaluate(boolean evaluateAtoms) {
	if (! on) return 0.0;
	double energy = 0;
	for (ResidueType type:ResidueType.values()) {
	    int index = type.ordinal();
	    diffSigma[index] = sumPerResidueType[index]-targetSigma[index];
	    if (diffSigma[index] > 0 ) diffSigma[index] = 0;
	    energy += weight * diffSigma[index] * diffSigma[index];
	    if (evaluateAtoms) {
		type = ResidueType.values()[index];
		n    = RamachandranSidechainEnergy.numberOfResiduesPerType()[index];
		for (ResidueTorsions residueTorsions:residueTorsionsList) {
		    residue = residueTorsions.getResidue();
		    if (residue.type() == type) {
			AtomList atoms = residue.atoms();
			int     natoms = atoms.size();
			for (Atom atom:atoms) 
			    atom.addEnergy((weight * diffSigma[index] * diffSigma[index])/n/natoms);
		    }
		}
	    }	    
	}
	for (ResidueTorsions residueTorsions:residueTorsionsList) {
        ResidueTorsionsAttribute rta = (ResidueTorsionsAttribute) residueTorsions.getAttribute(MeshiAttribute.RESIDUE_TORSIONS_ATTRIBUTE);
        if (rta == null) continue; //if all atoms of residue are frozen
        residue = residueTorsions.getResidue();
	    type   = residue.type();
	    diff   = diffSigma[type.ordinal()];
	    factor = 2*diff*weight;
	    if (factor != 0) {
		residueTorsions.applyForce( PHI, -rta.phi_deriv*factor );
		residueTorsions.applyForce( PSI, -rta.psi_deriv*factor);
		if (rta.chi_1_deriv != 0) residueTorsions.applyForce( CHI_1, -rta.chi_1_deriv*factor );
		if (rta.chi_2_deriv != 0) residueTorsions.applyForce( CHI_2, -rta.chi_2_deriv*factor );
		if (rta.chi_3_deriv != 0) residueTorsions.applyForce( CHI_3, -rta.chi_3_deriv*factor );
		if (rta.chi_4_deriv != 0) residueTorsions.applyForce( CHI_4, -rta.chi_4_deriv*factor );
	    }
	}
	return energy;
    }

    public void test(TotalEnergy totalEnergy, Atom atom) {
        if (! on) {System.out.println(""+this +" is off"); return;}
	System.out.println("Testing "+this+" using "+atom);
        if (atom == null) 
	    throw new RuntimeException("Cannot test "+this);

        double[][] coordinates = new double[3][];
        coordinates[0] = atom.X();
        coordinates[1] = atom.Y();
        coordinates[2] = atom.Z();
        for(int i = 0; i< 3; i++) {
            try{totalEnergy.update();}catch(UpdateableException ue){}
	    ramachandranSidechainEnergy.evaluate();
            double x = coordinates[i][0];
            coordinates[i][1] = 0;
            double e1 = evaluate();
            double analiticalForce = coordinates[i][1];
            coordinates[i][0] += EnergyElement.DX;
            // Whatever should be updated ( such as distance matrix torsion list etc. )
            try{totalEnergy.update();}catch(UpdateableException ue){}
	    ramachandranSidechainEnergy.evaluate();
            double e2 = evaluate();
            double de = e2-e1;
            double numericalForce = - de/ EnergyElement.DX;
            coordinates[i][0] -=  EnergyElement.DX;
            try{totalEnergy.update();}catch(UpdateableException ue){}
            
            double diff = Math.abs(analiticalForce - numericalForce);
            
            if ((2*diff/(Math.abs(analiticalForce)+Math.abs(numericalForce)+EnergyElement.VERY_SMALL)) > EnergyElement.relativeDiffTolerance){
                System.out.println("Testing "+this);
                System.out.println("Atom["+atom.number()+"]."+EnergyElement.XYZ.charAt(i)+" = "+x);
                System.out.println("Analytical force = "+analiticalForce);
                System.out.println("Numerical force  = "+numericalForce);
                
                System.out.println("diff = "+diff+"\n"+
                                   "tolerance = 2*diff/(|analiticalForce| + |numericalForce|+EnergyElement.VERY_SMALL) = "+
                                   2*diff/(Math.abs(analiticalForce) + Math.abs(numericalForce)+EnergyElement.VERY_SMALL));
                System.out.println();
            }
            if ((e1 == AbstractEnergy.INFINITY) | (e1 == AbstractEnergy.NaN))
                System.out.println("Testing "+this+"\ne1 = "+e1);
            if ((e2 == AbstractEnergy.INFINITY) | (e2 == AbstractEnergy.NaN))
                System.out.println("Testing "+this+"\ne2 = "+e2);
            if ((analiticalForce == AbstractEnergy.INFINITY) | (analiticalForce == AbstractEnergy.NaN))
                System.out.println("Testing "+this+"\nanaliticalForce = "+analiticalForce);

        }
	
    }
}


		
	