package meshi.geometry;

import  meshi.molecularElements.atoms.*;
import java.util.*;

public  class MatrixRowCaOnly extends MatrixRow {
    private static MolecularSystem molecularSystem;
    private int atomNumber;
     public MatrixRowCaOnly(AtomCore atom, MatrixRow[] matrix, MolecularSystem molecularSystem) {
	super(atom, matrix);
	this.molecularSystem = molecularSystem;
	atomNumber = atom.number;	
    }
    public void update(DistanceList nonBondedList, ArrayList<DistanceList> energyTermsDistanceLists, int counter) {
	double x1,y1,z1,dx,dy,dz,d2;
	boolean found;          //already existing distances - bond and NB, so !found - only new NB distances
	AtomCore iAtom;
	x1 = atom.x();
	y1 = atom.y();
	z1 = atom.z();
	// looking for new NBL distances
	for (int i = 0; i < atomNumber; i++) {
	    iAtom = molecularSystem.get(i);
	    dx = x1 - iAtom.x();
	    dy = y1 - iAtom.y();
	    dz = z1 - iAtom.z();
	    d2 = dx*dx + dy*dy + dz*dz;
	    if ((counter%20 != 0) && (counter > 100)) {
		if (d2 > 25*rMax2) {
		    //System.out.println(d2+" jump 5");
		    i += 5;
		    continue;
		}
		else if (d2 > 16*rMax2) {
		    //System.out.println(d2+" jump 3");
		    i += 3;
		    continue;
		}
		else if (d2 > 9*rMax2) {
		    //System.out.println(d2+" jump 2");
		    i += 2;
		    continue;
		}
		else  if (d2 > 6*rMax2) {
		    //System.out.println(d2+" jump 1");
		    i++;
		    continue;
		}
	    }
		    if (d2 <= rMax2) {
			found = false;          //distance has existed already, it can be bonded or nonBonded, so !found - only new NB distances

			for (int j = 0; j < size; j++) {
			    distance = internalArray[j];
			    if (distance == null){
			        lastEmpty = j; //i
			    }
			    else {
			        atom2number = distance.atom2Number;
				if (iAtom.number == atom2number) {// Distance <atom, iAtom> is in the Matrix already, it can be Bonded or NB
    				    found = true;
				    break;
			        }
			    }
			}

			if (!found) {// (!found) is a subcase of someNonBonded

			    if (atom.status().frozen() & iAtom.status().frozen())
				distance = new FrozenDistance(atom,iAtom, dx, dy, dz, Math.sqrt(d2));
			    else {
				distance = new Distance(atom,iAtom, dx, dy, dz, Math.sqrt(d2));
				distance.mode = DistanceMode.NEW;
			    }

			    nonBondedList.add(distance); 
			    for (DistanceList dl:energyTermsDistanceLists)//TODO is it right place?
				dl.add(distance);

			    //if ((distance.distance*distance.distance >= rMax2) || (distance.mode.bonded))
			    //	throw new RuntimeException("Weird new distance is here!"+distance);
			}      
		    }
	}     // end of new NBL distance
    }
			    
    public String toString() {
	String out =  "MatrixRowCaOnly atom = "+atom+" number = "+number+"\n";
	for (Distance dis:this)
	    out+=" "+dis.toString()+" ; ";
	return out;
    }
}

