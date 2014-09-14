package meshi.energy.pairwiseNonBondedTerms.atomicPairwisePMFSumma;

import meshi.util.KeyWords;
import meshi.util.CommandList;
import meshi.util.string.StringList;
import meshi.util.file.MeshiLineReader;
import meshi.parameters.MeshiPotential;
import meshi.parameters.AtomType;

import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: tetyanam
 * Date: 04/06/2009
 * Time: 11:41:31
 * To change this template use File | Settings | File Templates.
 */
public class CooperativeZParameters  implements KeyWords, MeshiPotential {
    protected final double[] mean = new double[AtomType.values().length];
    protected final double[] std = new double[AtomType.values().length];

    public CooperativeZParameters(CommandList commands ) {
    String          parametersFileName = commands.firstWord(PARAMETERS_DIRECTORY ).secondWord() + "/meshiPotential/" + commands.firstWord(COOPERATIVE_Z_SUMMA_FILENAME).secondWord();

    System.out.println("CooperativeZSummaParametersFile: "+parametersFileName);
    MeshiLineReader mlrParameters = new MeshiLineReader( parametersFileName );

    StringList lines = new StringList(mlrParameters);
    AtomType type;
    for (String str :lines) {
        StringTokenizer line = new StringTokenizer(str);
        //line.nextToken(); // ignore the first word in the line.
        String name = line.nextToken();
        type = AtomType.type(name);
        mean[type.ordinal()] = (new Double(line.nextToken())).doubleValue();
        std[type.ordinal()] = (new Double(line.nextToken())).doubleValue();
    }
    }

}

