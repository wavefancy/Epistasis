package meshi.energy.simpleEnergyTerms.compositeTorsions.compositePropensity;
import meshi.util.*;

public class ResidueTorsionsPropensityAttribute implements MeshiAttribute{
    double phi_deriv   = 0;
    double psi_deriv   = 0;
    public int key() {return RESIDUE_TORSIONS_ATTRIBUTE;}
}