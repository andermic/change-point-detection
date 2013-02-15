// Modified from web.engr.oregonstate.edu.zheng.gef.task.msexp.stacking.osu

package MikeExperiments;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import web.engr.oregonstate.edu.zheng.gef.def.Array;
import web.engr.oregonstate.edu.zheng.gef.def.Table;
import web.engr.oregonstate.edu.zheng.gef.def.TableColumn;
import web.engr.oregonstate.edu.zheng.gef.def.TaskDef;
import web.engr.oregonstate.edu.zheng.gef.def.Var;
import web.engr.oregonstate.edu.zheng.gef.executor.ExecutorBuilder;
import web.engr.oregonstate.edu.zheng.gef.executor.ExecutorBuilder.VerificationType;
import web.engr.oregonstate.edu.zheng.gef.task.freeliving.Formula;
import web.engr.oregonstate.edu.zheng.gef.utils.RUtils;
import web.engr.oregonstate.edu.zheng.obesity.commons.utils.DataSet;

public class DecisionTree extends TaskDef {

	private static final Logger log = Logger
			.getLogger(DecisionTree.class.getName());
	
	private void testBestModel(Integer clusterJobNumber, Boolean useCluster,
			Array formulaName, Var expPath, Array cpdAlgorithm, Array cpdFPR,
			String clusterWorkspace, Var validateSummaryFile, Array formula,
			String jobId, Var trainDataInfoPath, Var validateDataInfoPath, 
			Var testDataInfoPath, Array splitId, String labVisitFileFolder,
			String trainingLabVisitFileExt, Var valiTestLabVisitFileExt, 
			Var bestModelInfoPath, Var bestModelSavePath, 
			Var trainResultPath, Var validateResultPath,
			Var testResultPath)
			throws Exception {
		logStep("Test the best decision tree model");
		Var testBestSingleScaleModelFunction = var("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var testBestSingleScaleModelScript = expPath.fileSep().cat("dt").dot()
				.cat(formulaName).dot().cat(cpdAlgorithm).dot().cat(cpdFPR)
				.cat(".best.R");
		ExecutorBuilder bestSingleScale = rScript(
				testBestSingleScaleModelFunction,
				testBestSingleScaleModelScript, var("testBestModelDtCPD"),
				execConfig().setParallelizable(useCluster).setOnCluster(true)
						.setNumJobs(clusterJobNumber).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		bestSingleScale.addParam("validateSummaryFile", String.class,
				validateSummaryFile);
		bestSingleScale.addParam("formula", Formula.class, var(formula));
		bestSingleScale.addParam("formulaName", String.class, var(formulaName));
		bestSingleScale.addParam("labels", List.class, RUtils.varToRList(
				var(array(Arrays.asList("lying_down", "sitting",
						"standing_household", "walking", "running",
						"basketball", "dance"))), true));
		bestSingleScale.addParam("split", Integer.class, var(splitId));
		bestSingleScale.addParam("trainDataInfoPath", String.class,
				trainDataInfoPath, VerificationType.Before);
		bestSingleScale.addParam("validateDataInfoPath", String.class,
				validateDataInfoPath, VerificationType.Before);
		bestSingleScale.addParam("testDataInfoPath", String.class,
				testDataInfoPath, VerificationType.Before);
		bestSingleScale.addParam("labVisitFileFolder", String.class,
				labVisitFileFolder);
        bestSingleScale.addParam("trainingLabVisitFileExt", String.class,
                trainingLabVisitFileExt);
		bestSingleScale.addParam("valiTestLabVisitFileExt", String.class,
				valiTestLabVisitFileExt);
		bestSingleScale.addParam("kernal", String.class, "linear");
		bestSingleScale.addParam("bestModelInfoSavePath", String.class,
				bestModelInfoPath);
		bestSingleScale.addParam("bestModelSavePath", String.class,
				bestModelSavePath, VerificationType.After);
		// parameters for testing the trained model on training data
		bestSingleScale.addParam("trainReportPath", String.class,
				trainResultPath, VerificationType.After);
		// parameters for testing the trained model on validation data
		bestSingleScale.addParam("validateReportPath", String.class,
				validateResultPath, VerificationType.After);
		// parameters for testing the trained model on testing data
		bestSingleScale.addParam("testReportPath", String.class,
				testResultPath, VerificationType.After);
		bestSingleScale.prodMode();
		bestSingleScale.execute();
	}
	
	private void summarizeTest(Integer clusterJobNum, Boolean useCluster,
			String clusterWorkspace, String jobId, Var expPath,
			Var bestModelId, Var testResultPath, Var confusionMatrixPath,
			Var pctConsufionMatrixPath, Var summaryPath) throws Exception {
		logStep("Summarize model test results");
		Var summarizeFunctionPath = var("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var summarizeScriptPath = expPath.fileSep().cat("dt").dot()
				.cat(bestModelId).cat(".summarize.R");
		ExecutorBuilder summarizeSingle = rScript(summarizeFunctionPath,
				summarizeScriptPath, var("summarizeCPD"),
				execConfig().setParallelizable(useCluster).setOnCluster(true)
						.setNumJobs(clusterJobNum).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		summarizeSingle.addParam("labels", List.class, RUtils.varToRList(
				var(array(Arrays.asList("lying_down", "sitting",
						"standing_household", "walking", "running",
						"basketball", "dance"))), true));
		summarizeSingle.addParam("predictionReportPath", String.class,
				testResultPath, VerificationType.Before);
		summarizeSingle.addParam("confusionMatrixPath", String.class,
				confusionMatrixPath, VerificationType.After);
		summarizeSingle.addParam("pctConsufionMatrixPath", String.class,
				pctConsufionMatrixPath, VerificationType.After);
		summarizeSingle.addParam("summaryPath", String.class, summaryPath,
				VerificationType.After);
		summarizeSingle.prodMode();
		summarizeSingle.execute();
	}
	
	private void makeTable(Array formulaName, Array cpdAlgorithm, Array cpdFPR,
			String tvtDataPath, Array splitId, Array windowSizes,
			Array trialGroupId, Array testDataSets, Var modelPath)
			throws Exception {
		logStep("List all resources in a table");
		for(String form: formulaName.getValues()) {
			for(String alg : cpdAlgorithm.getValues()) {
				for (String fpr: cpdFPR.getValues()) {
					Var testResultPathSingle = var(tvtDataPath).cat("/dt/").cat("split").cat(splitId).cat("/dt.").cat(form).dot().cat(alg).dot().cat(fpr).cat(".best.model.test.csv");
					String bestModelIdSingle = "best." + form + "." + alg + "." + fpr;
					Var summaryPathSingle = var(tvtDataPath).cat("/dt/").cat("split").cat(splitId).cat("/dt.").cat(bestModelIdSingle).cat(".test.summary.csv");
					Var confusionMatrixPathSingle = var(tvtDataPath).cat("/dt/").cat("split").cat(splitId).cat("/dt.").cat(bestModelIdSingle).cat(".test.cm.csv");
					Table bestSingleScaleModelResTable = createTable(
							newColumn("WindowSize", windowSizes),
							newColumn("TrialGroup", trialGroupId),
							newColumn("Formula", formulaName),
							newColumn("Split", splitId),
							newColumn("CpdAlgorithm", array(Arrays.asList(alg))),
							newColumn("CpdFPR", array(Arrays.asList(fpr))),
							newColumn("TestDataSet", testDataSets),
							newColumn("TestReportFile", testResultPathSingle),
							newColumn("TestAccuracyFile", summaryPathSingle),
							newColumn("TestConfusionMatrixFile", confusionMatrixPathSingle));
					Var bestSingleScaleModelResSavePath = modelPath.fileSep()
							.cat("dt.model.").cat(alg).dot().cat(fpr)
							.cat(".res.csv");
					save(bestSingleScaleModelResTable, bestSingleScaleModelResSavePath);
				}
			}
		}
	}

	private void mergeSplits(Var modelPath, Var bestModelId, 
			Array testDataSets, Array cpdAlgorithm, Array cpdFPR, 
			Array trialGroupId, Array formulaName, Array splitId)
			throws Exception {
		logStep("Merge splits results");
		Var mergeFunction = var("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var mergeScript = modelPath.fileSep().cat("dt.merge.split.")
				.cat(bestModelId).dot().cat(testDataSets).cat(".R");
		Var accuracySavePath = modelPath.fileSep()
				.cat("dt.model.").cat(bestModelId).dot()
				.cat(testDataSets).cat(".accuracy.csv");
		Var meanAccuracySavePath = modelPath.fileSep()
				.cat("dt.model.").cat(bestModelId).dot()
				.cat(testDataSets).cat(".summary.csv");
		Var confusionMatrixSavePath = modelPath.fileSep()
				.cat("dt.model.").cat(bestModelId).dot()
				.cat(testDataSets).cat(".cm.csv");
		Var pctConfusionMatrixSavePath = modelPath.fileSep()
				.cat("dt.model.").cat(bestModelId).dot()
				.cat(testDataSets).cat(".pctcm.csv");
		// Var reportSavePath = modelPath.fileSep()
		// .cat("dt.single.scale.model.").cat(modelId).dot()
		// .cat(testDataSets).cat(".csv");
		ExecutorBuilder merge = rScript(mergeFunction, mergeScript,
				var("mergeSplitShuffleCPD"));
		Var bestSingleScaleModelResSavePathVar = modelPath.fileSep()
				.cat("dt.model.").cat(cpdAlgorithm).dot().cat(cpdFPR)
				.cat(".res.csv");
		merge.addParam("bestModelResFilePath", String.class,
				bestSingleScaleModelResSavePathVar, VerificationType.Before);
		//merge.addParam("windowSize", Integer.class, var(windowSizes));
		merge.addParam("trialGroup", String.class, var(trialGroupId));
		merge.addParam("formula", String.class, var(formulaName));
		merge.addParam("testDataSet", String.class, var(testDataSets));
		String expectedNumEntries = String.valueOf(splitId.getValues().size());
		merge.addParam("expectedNumEntries", Integer.class,
				var(expectedNumEntries));
		merge.addParam("accuracySavePath", String.class, accuracySavePath,
				VerificationType.After);
		merge.addParam("meanAccuracySavePath", String.class,
				meanAccuracySavePath, VerificationType.After);
		merge.addParam("confusionMatrixSavePath", String.class,
				confusionMatrixSavePath, VerificationType.After);
		merge.addParam("pctConfusionMatrixSavePath", String.class,
				pctConfusionMatrixSavePath, VerificationType.After);
		// merge.addParam("reportSavePath", String.class, reportSavePath,
		// VerificationType.After);
		merge.prodMode();
		merge.execute();
	}

	/*
	private void mergeAlgorithmResults(Var modelPath, Array cpdAlgorithm, Array cpdFPR) throws Exception {
		logStep("Merge results across algorithms");
		Var mergeAlgorithmFunction = var("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var mergeAlgorithmScript = modelPath.fileSep().cat("dt.merge.algorithms.")
				.cat(bestModelId).dot().cat(testDataSets).cat(".R");
		Var summarySavePath = modelPath.dot().cat(cpdAlgorithm).dot().cat("results.csv")
		
	    Iterator<String> iter = cpdFPR.getValues().iterator();
	    StringBuilder builder = new StringBuilder(iter.next());
	    while( iter.hasNext() ) {
	    	builder.append(" ").append(iter.next());
	    }
	    String cpdFPRStr = builder.toString();
	    
		ExecutorBuilder mergeAlgorithm = rScript(mergeAlgorithmFunction, 
				mergeAlgorithmScript, var("mergeAlgorithmResults"));
		mergeAlgorithm.addParam();
		mergeAlgorithm.addParam("fprsStr", String.class, cpdFPRStr);
		mergeAlgorithm.addParam("summarySavePath", String.class, summarySavePath);
	}*/
	
	private void singleScaleModel(String expRootPath, String datasetStr,
			String tvtDataPath, String labVisitFileFolder,
			String trainingLabVisitFileExt, Var valiTestLabVisitFileExt,
			List<String> trialGroupIdList,
			List<String> windowSizeList,
			List<String> formulaList, List<String> formulaNameList,
			String clusterWorkspace, String jobId, Array cpdAlgorithm,
			Array cpdFPR, Integer clusterJobNum, Boolean useCluster)
			throws Exception {

		Var dataset = var(datasetStr);
		Array windowSizes = array(windowSizeList);
		Array trialGroupIds = array(trialGroupIdList);

		Array splitId = array("[0:1:30]");
		Var iterationId = var("split").cat(splitId);
		Var expPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat("dt").fileSep().cat(iterationId);

		Var trainDataInfoPath = var(tvtDataPath).fileSep().cat(iterationId)
				.fileSep().cat("train.120.data.csv");
		Var validateDataInfoPath = var(tvtDataPath).fileSep().cat(iterationId)
				.fileSep().cat("validate.").cat(cpdAlgorithm).dot().cat(cpdFPR)
				.cat(".data.csv");
		Var testDataInfoPath = var(tvtDataPath).fileSep().cat(iterationId)
				.fileSep().cat("test.").cat(cpdAlgorithm).dot().cat(cpdFPR)
				.cat(".data.csv");

		Array formula = array(formulaList);
		Array formulaName = array(formulaNameList);
		bind(formula, formulaName);
		
		Var validateSummaryFile = var(tvtDataPath).cat("/dt/dt.validate.")
				.cat(cpdAlgorithm).dot().cat(cpdFPR).cat(".summary.csv");

		Var bestModelPrefix = expPath.fileSep().cat("dt").dot()
				.cat(formulaName).dot().cat(cpdAlgorithm).dot()
				.cat(cpdFPR).cat(".best.model");
		
		Var bestModelInfoPath = bestModelPrefix.cat(".info.csv");
		Var bestModelSavePath = bestModelPrefix.cat(".best.model").cat(".save");
		Var trainResultPath = bestModelPrefix.cat(".train.csv");
		Var validateResultPath = bestModelPrefix.cat(".validate.csv");
		Var testResultPath = bestModelPrefix.cat(".test.csv");
		
		Var bestModelId = var("best").dot().cat(formulaName).dot()
				.cat(cpdAlgorithm).dot().cat(cpdFPR);
		Var confusionMatrixPath = expPath.fileSep().cat("dt").dot()
				.cat(bestModelId).cat(".test.cm.csv");
		Var pctConsufionMatrixPath = expPath.fileSep().cat("dt").dot()
				.cat(bestModelId).cat(".test.pctcm.csv");
		Var summaryPath = expPath.fileSep().cat("dt").dot().cat(bestModelId)
				.cat(".test.summary.csv");

		Array trialGroupId = array(trialGroupIdList);
		Array testDataSets = array(Arrays.asList("test"));
		Var modelPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat("dt");
		
		testBestModel(clusterJobNum, useCluster, formulaName, expPath, cpdAlgorithm, cpdFPR, clusterWorkspace, validateSummaryFile, formula, jobId, trainDataInfoPath, validateDataInfoPath, testDataInfoPath, splitId, labVisitFileFolder, trainingLabVisitFileExt, valiTestLabVisitFileExt, bestModelInfoPath, bestModelSavePath, trainResultPath, validateResultPath, testResultPath);
		summarizeTest(clusterJobNum, useCluster, clusterWorkspace, jobId, expPath, bestModelId, testResultPath, confusionMatrixPath, pctConsufionMatrixPath, summaryPath);
		makeTable(formulaName, cpdAlgorithm, cpdFPR, tvtDataPath, splitId, windowSizes, trialGroupId, testDataSets, modelPath);
		mergeSplits(modelPath, bestModelId, testDataSets, cpdAlgorithm, cpdFPR, trialGroupId, formulaName, splitId);
	}
	
	private void OSU_YR4_30Hz_Hip() throws Exception {
		String expRootPath = "/nfs/guille/wong/wonglab3/obesity/2012/cpd";
		String datasetStr = "OSU_YR4_Hip_30Hz";

		List<String> windowSizeList = Arrays.asList("120");
		List<String> trialGroupIdList = Arrays.asList("7cls");

		String tvtDataPath = expRootPath + "/OSU_YR4_Hip_30Hz.ws120.7cls";
		String labVisitFileFolder = tvtDataPath + "/features";

		List<String> formulaList = Arrays.asList(Formula.FORMULA_ALL_WO_FFT
				.toString());
		List<String> formulaNameList = Arrays.asList("AllWoFFT");

		String clusterWorkspace = "/nfs/guille/wong/wonglab3/obesity/2012/cpd/OSU_YR4_Hip_30Hz.ws120.7cls/dt/cluster";
		Integer clusterJobNum = 100;
		Boolean useCluster = false;
		
		Array cpdAlgorithm = array(Arrays.asList("cc", "kliep"));
		//Array cpdFPR = array(Arrays.asList("0.0001", "0.0002", "0.0003", "0.0004", "0.0005", "0.0006", "0.0007", "0.0008", "0.0009", "0.001", "0.0011", "0.0012", "0.0013", "0.0014", "0.0015", "0.0016", "0.0017", "0.0018", "0.0019", "0.002", "0.0021", "0.0022", "0.0023", "0.0024", "0.0025", "0.0026", "0.0027", "0.0028", "0.0029", "0.003", "0.0031", "0.0032", "0.0033", "0.0034", "0.0035", "0.0036", "0.0037", "0.0038", "0.0039", "0.004", "0.0041", "0.0042", "0.0043", "0.0044", "0.0045", "0.0046", "0.0047", "0.0048", "0.0049", "0.005", "0.0051", "0.0052", "0.0053", "0.0054", "0.0055", "0.0056", "0.0057", "0.0058", "0.0059", "0.006", "0.0061", "0.0062", "0.0063", "0.0064", "0.0065", "0.0066", "0.0067", "0.0068", "0.0069", "0.007", "0.0071", "0.0072", "0.0073", "0.0074", "0.0075", "0.0076", "0.0077", "0.0078", "0.0079", "0.008", "0.0081", "0.0082", "0.0083", "0.0084", "0.0085", "0.0086", "0.0087", "0.0088", "0.0089", "0.009", "0.0091", "0.0092", "0.0093", "0.0094", "0.0095", "0.0096", "0.0097", "0.0098", "0.0099", "0.01"));
		Array cpdFPR = array(Arrays.asList("0.015", "0.02", "0.025", "0.03", "0.035", "0.04", "0.045", "0.05", "0.055", "0.06", "0.065", "0.07", "0.075", "0.08", "0.085", "0.09", "0.095"));
		String trainingLabVisitFileExt = ("PureTrial.featurized.120.csv");
		Var valiTestLabVisitFileExt = var("PureTrial.featurized.").cat(cpdAlgorithm).dot().cat(cpdFPR).cat(".csv");
		
		singleScaleModel(expRootPath, datasetStr, tvtDataPath,
				labVisitFileFolder, trainingLabVisitFileExt, valiTestLabVisitFileExt, trialGroupIdList,
				windowSizeList, formulaList,
				formulaNameList, clusterWorkspace, "single",
				cpdAlgorithm, cpdFPR, clusterJobNum, useCluster);
	}

	/*private void OSU_YR4_30Hz_Wrist() throws Exception {
		String expRootPath = "/nfs/guille/wong/wonglab2/obesity/2012/msexp";
		String datasetStr = "OSU_YR4_Wrist_30Hz";

		List<String> windowSizeList = Arrays.asList("10");
		List<String> trialGroupIdList = Arrays.asList("7cls");

		String tvtDataPath = expRootPath + "/" + datasetStr + ".ws10.7cls";
		String labVisitFileFolder = tvtDataPath + "/features";
		String labVisitFileExt = "PureTrial.featurized.csv";

		List<String> formulaList = Arrays.asList(Formula.FORMULA_ALL_WO_FFT
				.toString());
		List<String> formulaNameList = Arrays.asList("AllWoFFT");

		String costExp = "[0.01,0.1,1,10,100,1000]";
		String scaleExp = "[1:1:10]";

		String clusterWorkspace = "/nfs/guille/wong/wonglab2/obesity/2012/msexp/"
				+ datasetStr + ".ws10.7cls/dt/cluster";
		singleScaleModel(expRootPath, datasetStr, tvtDataPath,
				labVisitFileFolder, labVisitFileExt, trialGroupIdList,
				windowSizeList, costExp, scaleExp, formulaList,
				formulaNameList, clusterWorkspace, "single");
		allScaleStackingModel(expRootPath, datasetStr, tvtDataPath,
				trialGroupIdList, windowSizeList, formulaList, formulaNameList,
				costExp, clusterWorkspace, "stacking");
		List<String> singleScaleList = Arrays.asList("1", "2", "3", "4", "5",
				"6", "7", "8", "9");
		for (String singleScale : singleScaleList) {
			singleScaleStackingModel(expRootPath, datasetStr, tvtDataPath,
					trialGroupIdList, windowSizeList, formulaNameList,
					singleScale, costExp, clusterWorkspace, "mss");
		}
	}*/

	public static void main(String[] args) {
		try {
			new DecisionTree().OSU_YR4_30Hz_Hip();
			//new SVMLinearTrainValidateTest1().OSU_YR4_30Hz_Wrist();
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
