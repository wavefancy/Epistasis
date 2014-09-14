/**
 *Excluded Volume potential.
 *
 *A potential that creates a strong repulsion between atoms when they are getting
 *near their VDW radius. There is no attraction part like in the VDW.
 *The functional form of the term is:  
 *
 *dis =                EV
 *
 *[0,sigma]  C*(dis-sigma)^4
 *[sigma,Inf]          0
 *
 *ALPHA is set in ExcludedVolParameters.java. Currently it is 0.2 Ang.
 *ALPHA is the transition zone (in Ang) where the energy change in the forth power 0.0 to 1.0.
 *
 **/
package meshi.energy.pairwiseNonBondedTerms.excludedVolOLD;

import meshi.energy.pairwiseNonBondedTerms.NonBondedEnergyTerm;
import meshi.geometry.Distance;
import meshi.geometry.DistanceList;
import meshi.geometry.DistanceMatrix;
import meshi.util.Utils;
import meshi.util.filters.Filter;

public class ExcludedVolOLD extends NonBondedEnergyTerm {
    protected double Rfac;
    protected Filter filter; 

    public ExcludedVolOLD(){super();}

    public ExcludedVolOLD(DistanceMatrix distanceMatrix,
                       double Rfac ,
                       double weight,
                       Filter filter,
                       double[][] parameters){
	super(toArray(distanceMatrix), weight,distanceMatrix);
	comment = "ExcludedVolOLD";
	energyElement = new ExcludedVolEnergyElementOLD(distanceMatrix, Rfac, weight, parameters);
	this.filter = filter;
    }

    public double evaluate() {
	if (! on) return 0.0;
	double energy = 0;
	DistanceList nonBondedList;
	if (filter == null) 
		nonBondedList = distanceMatrix.nonBondedList();
	else {
	    nonBondedList = new DistanceList(100);
	    Utils.filter(distanceMatrix.nonBondedList(),filter,nonBondedList);
	}
	for (Distance distance:nonBondedList) {
            if (!distance.mode().frozen) {
	            energyElement.set(distance);
	            energy += energyElement.evaluate();
            }
        }
	return energy;
    }//evaluate

} //class

	
 
