package meshi.sequences;
import meshi.molecularElements.*;
import meshi.molecularElements.atoms.*;

public class ResidueAlignmentCell extends AlignmentCell {
    public ResidueAlignmentCell(Residue residue) {
	super(residue, residue.number());
    }
    
    public Residue residue() {return (Residue) obj;}
    public boolean gap() {
	return (((Residue) obj).dummy());
    }
}
	
