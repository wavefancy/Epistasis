package ca.mcgill.pcingola.epistasis.likelihood;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.StreamSupport;

import ca.mcgill.mcb.pcingola.fileIterator.VcfFileIterator;
import ca.mcgill.mcb.pcingola.probablility.FisherExactTest;
import ca.mcgill.mcb.pcingola.util.Gpr;
import ca.mcgill.mcb.pcingola.util.Timer;
import ca.mcgill.mcb.pcingola.vcf.VcfEntry;
import ca.mcgill.pcingola.epistasis.Genotype;
import ca.mcgill.pcingola.epistasis.gwas.GwasResult;
import ca.mcgill.pcingola.regression.LogisticRegression;
import ca.mcgill.pcingola.regression.LogisticRegressionIrwls;

/**
 * Logistic regression fitting and likelihood analysis of VCF + phenotype data
 *
 * @author pcingola
 */
public class LogisticRegressionGt {

	public static String VCF_INFO_LOG_LIKELIHOOD = "LL";

	String phenoCovariatesFileName = Gpr.HOME + "/snpeff/epistasis/pheno.txt";
	String vcfFileName = Gpr.HOME + "/snpeff/epistasis/gwas.vcf";

	boolean debug = false;
	boolean verbose = false;
	boolean writeToFile = false;
	int numSamples;
	int numCovariates;
	int count = 0;
	int covariatesToNormalize[] = { 10, 11 };
	int numGtAlt = 1, numGtNull = 0;
	int deltaDf = 1; // Difference in degrees of freedom between Alt and Null model
	double covariates[][];
	double pheno[];
	double logLik = 0;
	double logLikMax = Double.NEGATIVE_INFINITY;
	String logLikInfoField; // If not null, an INFO field is added
	String sampleIds[];
	LogisticRegression lr;
	LogisticRegression lrAlt, lrNull;
	HashMap<String, Double> llNullCache = new HashMap<String, Double>();

	public static void main(String[] args) {
		Timer.showStdErr("Start");

		boolean debug = false;

		LogisticRegressionGt zzz = new LogisticRegressionGt(args);

		if (debug) {
			zzz.setDebug(debug);
			zzz.setLogLikInfoField(VCF_INFO_LOG_LIKELIHOOD);
		}

		zzz.run();

		Timer.showStdErr("End");
	}

	public LogisticRegressionGt(String args[]) {
		if (args.length == 2) {
			phenoCovariatesFileName = args[0];
			vcfFileName = args[1];
		} else {
			System.err.println("Usage: " + this.getClass().getSimpleName() + " phenoCovariatesFileName.txt file.vcf");
		}
	}

	public LogisticRegressionGt(String phenoCovariatesFileName, String vcfFileName) {
		this.phenoCovariatesFileName = phenoCovariatesFileName;
		this.vcfFileName = vcfFileName;
	}

	/**
	 * Calculate logistic regression's null model (or retrieve it form a cache)
	 */
	protected double calcNullModel(GwasResult gwasResult, double phenoNonSkip[]) {
		// Is logLikelihood cached?
		String skipKey = gwasResult.getSkipKey();
		Double llNull = llNullCache.get(skipKey);
		if (llNull != null) return llNull;

		// Not found in cache? Create model and calculate it
		LogisticRegression lrNull = createNullModel(gwasResult.getSkip(), gwasResult.getCountSkip(), phenoNonSkip);
		this.lrNull = lrNull;
		lrNull.learn();

		llNull = lrNull.logLikelihood();
		llNullCache.put(skipKey, llNull); // Add to cache

		return llNull;
	}

	/**
	 * Check that sample names and sample order matches
	 */
	void checkSamplesVcf(VcfFileIterator vcf) {
		//---
		// Check that sample names and sample order matches
		//---
		vcf.readHeader();
		List<String> sampleNames = vcf.getVcfHeader().getSampleNames();
		int snum = 0;
		for (String s : sampleNames) {
			if (!s.equals(sampleIds[snum])) { throw new RuntimeException("Sample names do not match:" //
					+ "\n\tSample [" + snum + "] in VCF file        :  '" + s + "'" //
					+ "\n\tSample [" + snum + "] in phenotypes file :  '" + sampleIds[snum] + "'" //
			); }
			snum++;
		}
	}

	/**
	 * Create alternative model
	 */
	protected LogisticRegression createAltModel(GwasResult gwasResult, double phenoNonSkip[]) {
		LogisticRegression lrAlt = new LogisticRegressionIrwls(numCovariates + 1); // Add genotype

		// Copy all covariates (except one that are skipped)
		int totalSamples = numSamples - gwasResult.getCountSkip();
		double xAlt[][] = new double[totalSamples][numCovariates + 1];

		boolean skip[] = gwasResult.getSkip();
		byte gt[] = gwasResult.genoi.getGt();
		int idx = 0;
		double gtmax = Double.NEGATIVE_INFINITY, gtmin = Double.POSITIVE_INFINITY;
		for (int i = 0; i < numSamples; i++) {
			if (skip[i]) continue;

			// First row from phenotypes file is phenotype. But we want to predict using 'genotype', so we replace it
			xAlt[idx][0] = gt[i];

			gtmax = Math.max(gtmax, gt[i]);
			gtmin = Math.min(gtmin, gt[i]);

			// Copy all other covariates
			for (int j = 0; j < numCovariates; j++)
				xAlt[idx][j + 1] = covariates[i][j];

			idx++;
		}

		// Set samples
		lrAlt.setSamplesAddIntercept(xAlt, phenoNonSkip);
		lrAlt.setDebug(debug);

		this.lrAlt = lrAlt;
		return lrAlt;
	}

	/**
	 * Create null model
	 */
	protected LogisticRegression createNullModel(boolean skip[], int countSkip, double phenoNonSkip[]) {
		LogisticRegression lrNull = new LogisticRegressionIrwls(numCovariates); // Null model: No genotypes

		// Copy all covariates (except one that are skipped)
		int totalSamples = numSamples - countSkip;
		double xNull[][] = new double[totalSamples][numCovariates];

		int idx = 0;
		for (int i = 0; i < numSamples; i++) {
			if (skip[i]) continue;

			for (int j = 0; j < numCovariates; j++)
				xNull[idx][j] = covariates[i][j];

			idx++;
		}

		// Set samples
		lrNull.setSamplesAddIntercept(xNull, phenoNonSkip);
		lrNull.setDebug(debug);

		this.lrNull = lrNull;
		return lrNull;
	}

	public double getLogLik() {
		return logLik;
	}

	public double getLogLikMax() {
		return logLikMax;
	}

	public LogisticRegression getLrAlt() {
		return lrAlt;
	}

	public LogisticRegression getLrNull() {
		return lrNull;
	}

	/**
	 * Does this 'theta' value have an error?
	 */
	protected boolean hasError(double theta[]) {
		for (int i = 0; i < theta.length; i++) {
			if (Double.isNaN(theta[i]) // Is it NaN?
					|| Double.isInfinite(theta[i]) // Id it infinite?
			) return true;
		}
		return false;
	}

	public void init() {
		//---
		// Load phenotype and covariates
		//---
		loadPhenoAndCovariates(phenoCovariatesFileName);

		//---
		// Read VCF file and run analysis
		//---
		Timer.showStdErr("Checking VCF file '" + vcfFileName + "' against phenotypes file '" + phenoCovariatesFileName + "'");
		VcfFileIterator vcf = new VcfFileIterator(vcfFileName);
		checkSamplesVcf(vcf); // Check that sample names and sample order matches
		vcf.close();
	}

	/**
	 * Load phenotypes and covariates
	 */
	void loadPhenoAndCovariates(String phenoCovariates) {
		Timer.showStdErr("Reading data form '" + phenoCovariates + "'");

		// Read "phenotypes + covariates" file
		String lines[] = Gpr.readFile(phenoCovariates).split("\n");

		// Allocate PCA matrix
		numCovariates = lines.length - 2; // Row 1 is the title, row 2 is phenotype, other rows are covariates
		numSamples = lines[0].split("\t").length - 1; // All, but first column are samples
		covariates = new double[numSamples][numCovariates];

		// Parse
		int lineNum = 0, covNum = 0;
		for (String line : lines) {
			line = line.trim();
			String fields[] = line.split("\t");

			// Parse lines
			if (lineNum == 0) {
				// First line: Title. Load sample names
				sampleIds = new String[fields.length - 1];
				for (int i = 1; i < fields.length; i++)
					sampleIds[i - 1] = fields[i];
			} else if (lineNum == 1) {
				// Second line: Phenotypes.
				pheno = new double[numSamples];
				for (int i = 1; i < fields.length; i++)
					pheno[i - 1] = Gpr.parseDoubleSafe(fields[i]) - 1.0; // Convert phenotype values from {1,2} to {0,1}
			} else {
				// Other lines: Covariantes
				for (int i = 1; i < fields.length; i++)
					covariates[i - 1][covNum] = Gpr.parseDoubleSafe(fields[i]);
				covNum++;
			}

			lineNum++;
		}
		Timer.showStdErr("Done. Phenotype and " + covNum + "/" + numCovariates + " covariates for " + numSamples + " samples.");

		//---
		// Normalize covariates: sex, age
		//---
		for (int cov : covariatesToNormalize)
			normalizeCovariates(cov);
	}

	/**
	 * Fit models and calculate log likelihood ratio using 'genotypes' (gt) for the Alt model
	 */
	protected double logLikelihood(Genotype geno) {
		GwasResult gwasResult = new GwasResult(geno, pheno);
		gwasResult.calcSkip();

		//---
		// Create and fit logistic models, calculate log likelihood
		//---
		// Phenotypes without 'skipped' entries
		double phenoNonSkip[] = gwasResult.phenoNoSkip();

		// Calculate 'Null' model (or retrieve from cache)
		double llNull = calcNullModel(gwasResult, phenoNonSkip);

		// Create and calculate 'Alt' model
		LogisticRegression lrAlt = createAltModel(gwasResult, phenoNonSkip);
		lrAlt.learn();

		double llAlt = lrAlt.logLikelihood();

		// Calculate likelihood ratio
		double ll = 2.0 * (llAlt - llNull);
		logLik = ll; // Store latest value (used for test cases and debugging)

		//---
		// Stats
		//---
		if (Double.isFinite(ll)) {
			boolean show = (logLikMax < ll);
			logLikMax = Math.max(logLikMax, ll);

			if (show || debug) {
				// Calculate p-value
				double pval = FisherExactTest.get().chiSquareCDFComplementary(ll, deltaDf);

				Timer.show(count //
						+ "\t" + geno.getId() //
						+ "\tLL_ratio: " + ll //
						+ "\tp-value: " + pval //
						+ "\tLL_alt: " + llAlt //
						+ "\tLL_null: " + llNull //
						+ "\tLL_ratio_max: " + logLikMax //
						+ "\tModel Alt  : " + lrAlt //
				);
			} else if (verbose) Timer.show(count + "\tLL_ratio: " + ll + "\tCache size: " + llNullCache.size() + "\t" + geno.getId());
		} else throw new RuntimeException("Likelihood ratio is infinite! ID: " + geno.getId() + ", LL.null: " + llNull + ", LL.alt: " + llAlt);

		//		countModel(lrAlt);
		count++;

		return ll;
	}

	/**
	 * Calculate log likelihood on a VCF entry
	 */
	protected void logLikelihood(VcfEntry ve) {
		Genotype geno = new Genotype(ve);
		double ll = logLikelihood(geno);

		if (logLikInfoField != null) ve.addInfo(logLikInfoField, "" + ll);

		//---
		// Save as TXT table (only used for debugging)
		//---
		if (debug && writeToFile) {
			// ALT data
			String fileName = Gpr.HOME + "/lr_test." + ve.getChromosomeName() + "_" + (ve.getStart() + 1) + ".alt.txt";
			Gpr.debug("Writing 'alt data' table to :" + fileName);
			Gpr.toFile(fileName, lrAlt.toStringSamples());

			// NULL data
			fileName = Gpr.HOME + "/lr_test." + ve.getChromosomeName() + "_" + (ve.getStart() + 1) + ".null.txt";
			Gpr.debug("Writing 'null data' table to :" + fileName);
			Gpr.toFile(fileName, lrNull.toStringSamples());

			// ALT model
			fileName = Gpr.HOME + "/lr_test." + ve.getChromosomeName() + "_" + (ve.getStart() + 1) + ".alt.model.txt";
			Gpr.debug("Writing 'alt model' to :" + fileName);
			Gpr.toFile(fileName, lrAlt.toStringModel());
		}
	}

	/**
	 * Normalize (mean 0, variance 1) a covariates' row
	 */
	void normalizeCovariates(int covNum) {
		// Calculate mean
		double sum = 0;
		for (int i = 0; i < covariates.length; i++)
			sum += covariates[i][covNum];
		double avg = sum / covariates.length;

		// Calculate stddev
		sum = 0;
		for (int i = 0; i < covariates.length; i++) {
			double d = (covariates[i][covNum] - avg);
			sum += d * d;
		}
		double stddev = Math.sqrt(sum / (covariates.length - 1));

		Timer.showStdErr("Covariate " + covNum + ", mean: " + avg + ", stddev: " + stddev);

		// Normalize
		for (int i = 0; i < covariates.length; i++)
			covariates[i][covNum] = (covariates[i][covNum] - avg) / stddev;

	}

	public void run() {
		run(false);
	}

	/**
	 * Run analysis and collect some results (for test cases)
	 */
	public List<VcfEntry> run(boolean createList) {
		init();
		VcfFileIterator vcf = new VcfFileIterator(vcfFileName);
		return run(vcf, createList);
	}

	protected List<VcfEntry> run(VcfFileIterator vcf, boolean createList) {
		ArrayList<VcfEntry> list = new ArrayList<VcfEntry>();
		if (createList) {
			// Process and populate list of VCF entries (single thread, used for debugging and test cases)
			for (VcfEntry ve : vcf) {
				logLikelihood(ve);
				list.add(ve);
			}
		} else StreamSupport.stream(vcf.spliterator(), true).forEach(ve -> logLikelihood(ve)); // Process (do not populate list)

		return list;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public void setLogLikInfoField(String logLikInfoField) {
		this.logLikInfoField = logLikInfoField;
	}

	public void setWriteToFile(boolean writeToFile) {
		this.writeToFile = writeToFile;
	}
}
