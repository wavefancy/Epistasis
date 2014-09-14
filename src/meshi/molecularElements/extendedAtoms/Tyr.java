package meshi.molecularElements.extendedAtoms;
import meshi.molecularElements.*; 
import meshi.molecularElements.atoms.*;
import meshi.geometry.*;
import meshi.parameters.*;
/**
 *<pre>
 *                O
 *                |
 *       N - CA - C...n
 *       |   |
 *       H   CB
 *           |
 *           CG - CD1 - CE1
 *           |          |
 *           CD2- CE2 - CZ-OH
 **/
public class Tyr extends ResidueExtendedAtoms {
    public final Atom CG, CD1, CE1, CZ, CD2, CE2, OH;
    public static final String COMMENT = "From Levitt, JMB 168:592 (1983) table 2.\n"+
	"                O\n"+
	"                |\n"+
	"       N - CA - C...n\n"+
	"       |   |\n"+
	"       H   CB\n"+
	"           |\n"+
	"           CG - CD1 - CE1\n"+
	"           |          |\n"+
	"           CD2- CE2 - CZ-OH\n";

 
   public Tyr(AtomList atomList, ResidueIdentifier id, ResidueMode mode) {
	super(ResidueType.TYR, atomList, id, mode);
	atoms.add(CG = getAtom("CG",AtomType.YCG, atomList, this));
	atoms.add(CD1 = getAtom("CD1",AtomType.YCD, atomList, this));
	atoms.add(CE1 = getAtom("CE1",AtomType.YCE, atomList, this));
	atoms.add(CZ = getAtom("CZ",AtomType.YCZ, atomList, this));
	atoms.add(CD2 = getAtom("CD2",AtomType.YCD, atomList, this));
	atoms.add(CE2 = getAtom("CE2",AtomType.YCE, atomList, this));
	atoms.add(OH = getAtom("OH",AtomType.YOH, atomList, this));
	bonds.add(CB.bond(CG));
	bonds.add(CG.bond(CD1));
	bonds.add(CG.bond(CD2));
	bonds.add(CD1.bond(CE1));
	bonds.add(CD2.bond(CE2));
	bonds.add(CE1.bond(CZ));
	bonds.add(CE2.bond(CZ));
	bonds.add(CZ.bond(OH));
    }
    public String comment() {
	return COMMENT;
    }
}

