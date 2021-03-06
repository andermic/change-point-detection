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
			String trainingLabVisitFileExt, Var valiLabVisitFileExt,
			Var testLabVisitFileExt,
			Var bestModelInfoPath, Var bestModelSavePath, 
			Var trainResultPath, Var validateResultPath,
			Var testResultPath, Array labels)
			throws Exception {
		logStep("Test decision tree model");
		Var testBestSingleScaleModelFunction = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var testBestSingleScaleModelScript = expPath.fileSep().cat("dt").dot()
				.cat(formulaName).dot().cat(cpdAlgorithm).dot().cat(cpdFPR)
				.cat(".best.R");
		ExecutorBuilder bestSingleScale = rScript(
				testBestSingleScaleModelFunction,
				testBestSingleScaleModelScript, var("testBestModelCPD"),
				execConfig().setParallelizable(useCluster).setOnCluster(true)
						.setNumJobs(clusterJobNumber).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		bestSingleScale.addParam("algorithm", String.class, "dt");
		bestSingleScale.addParam("formula", Formula.class, var(formula));
		bestSingleScale.addParam("formulaName", String.class, var(formulaName));
		bestSingleScale.addParam("labels", List.class, RUtils.varToRList(var(labels), true));
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
		bestSingleScale.addParam("valiLabVisitFileExt", String.class,
				valiLabVisitFileExt);
		bestSingleScale.addParam("testLabVisitFileExt", String.class,
				testLabVisitFileExt);
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
			Var pctConsufionMatrixPath, Var summaryPath, Array labels) throws Exception {
		logStep("Summarize model test results");
		Var summarizeFunctionPath = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var summarizeScriptPath = expPath.fileSep().cat("dt").dot()
				.cat(bestModelId).cat(".summarize.R");
		ExecutorBuilder summarizeSingle = rScript(summarizeFunctionPath,
				summarizeScriptPath, var("summarizeCPD"),
				execConfig().setParallelizable(useCluster).setOnCluster(true)
						.setNumJobs(clusterJobNum).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		summarizeSingle.addParam("labels", List.class, RUtils.varToRList(
				var(labels), true));
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
	
	private void makeTable(Array formulaName, Array cpdAlgorithm,
			Array cpdFPR, String tvtDataPath, Array splitId,
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
		Var mergeFunction = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
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
		//merge.addParam("trialGroup", String.class, var(trialGroupId));
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
		Var mergeAlgorithmFunction = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
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
			String trainingLabVisitFileExt, Var valiLabVisitFileExt,
			Var testLabVisitFileExt, List<String> trialGroupIdList,
			List<String> formulaList, List<String> formulaNameList,
			String clusterWorkspace, String jobId, Array cpdAlgorithm,
			Array cpdFPR, Integer clusterJobNum, Boolean useCluster,
			Array labels, String ground_str) throws Exception {

		Var dataset = var(datasetStr);
		Array trialGroupIds = array(trialGroupIdList);

		Array splitId = array("[0:1:30]");
		Var iterationId = var("split").cat(splitId);
		Var expPath = var(expRootPath).fileSep().cat(dataset)
				.cat(trialGroupIds).fileSep()
				.cat("dt").fileSep().cat(iterationId);

		Var trainDataInfoPath = var(tvtDataPath).fileSep().cat(iterationId)
				.fileSep().cat("train.").cat(ground_str).cat(".data.csv");
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
		Var modelPath = var(expRootPath).fileSep().cat(dataset)
				.cat(trialGroupIds).fileSep().cat("dt");
		
		testBestModel(clusterJobNum, useCluster, formulaName, expPath, cpdAlgorithm, cpdFPR, clusterWorkspace, validateSummaryFile, formula, jobId, trainDataInfoPath, validateDataInfoPath, testDataInfoPath, splitId, labVisitFileFolder, trainingLabVisitFileExt, valiLabVisitFileExt, testLabVisitFileExt, bestModelInfoPath, bestModelSavePath, trainResultPath, validateResultPath, testResultPath, labels);
		summarizeTest(clusterJobNum, useCluster, clusterWorkspace, jobId, expPath, bestModelId, testResultPath, confusionMatrixPath, pctConsufionMatrixPath, summaryPath, labels);
		makeTable(formulaName, cpdAlgorithm, cpdFPR, tvtDataPath, splitId, trialGroupId, testDataSets, modelPath);
		mergeSplits(modelPath, bestModelId, testDataSets, cpdAlgorithm, cpdFPR, trialGroupId, formulaName, splitId);
	}
	
	private void OSU_YR4_30Hz_Hip() throws Exception {
		String expRootPath = "/nfs/guille/wong/wonglab3/obesity/2012/cpd";
		String datasetStr = "OSU_YR4_Hip_30Hz.ws120";

		List<String> trialGroupIdList = Arrays.asList(".7cls");

		String tvtDataPath = expRootPath + "/OSU_YR4_Hip_30Hz.ws120.7cls";
		String labVisitFileFolder = tvtDataPath + "/features";
		Array labels = array(Arrays.asList("lying_down", "sitting",
				"standing_household", "walking", "running",
				"basketball", "dance"));
		
		List<String> formulaList = Arrays.asList(Formula.FORMULA_ALL_WO_FFT
				.toString());
		List<String> formulaNameList = Arrays.asList("AllWoFFT");

		String clusterWorkspace = "/nfs/guille/wong/wonglab3/obesity/2012/cpd/OSU_YR4_Hip_30Hz.ws120.7cls/dt/cluster";
		Integer clusterJobNum = 25;
		Boolean useCluster = true;
		
		Array cpdAlgorithm = array(Arrays.asList("cc"));
		Array cpdFPR = array(Arrays.asList("0.0017", "0.0019", "0.0021", "0.0024", "0.0028", "0.0033"));
		String trainingLabVisitFileExt = "PureTrial.featurized.120.csv";
		Var valiLabVisitFileExt = var("PureTrial.featurized.120.csv");
		Var testLabVisitFileExt = var("PureTrial.featurized.").cat(cpdAlgorithm).dot().cat(cpdFPR).cat(".csv");
		String ground_str = "120";
		
		singleScaleModel(expRootPath, datasetStr, tvtDataPath,
				labVisitFileFolder, trainingLabVisitFileExt, valiLabVisitFileExt,
				testLabVisitFileExt, trialGroupIdList,
				formulaList, formulaNameList, clusterWorkspace, "single",
				cpdAlgorithm, cpdFPR, clusterJobNum, useCluster, labels, ground_str);
	}

	private void UQ_30Hz() throws Exception {
		String expRootPath = "/nfs/guille/wong/wonglab3/obesity/2012/cpd";
		String day = "2";
		String datasetStr = "uq_30Hz_day" + day;

		List<String> trialGroupIdList = Arrays.asList("");

		String tvtDataPath = expRootPath + "/" + datasetStr;
		String labVisitFileFolder = tvtDataPath + "/features";
		Array labels = array(Arrays.asList("0","1","2"));
		
		List<String> formulaList = Arrays.asList(Formula.FORMULA_ALL_WO_FFT
				.toString());
		List<String> formulaNameList = Arrays.asList("AllWoFFT");

		String clusterWorkspace = expRootPath + "/" + datasetStr + "/dt/cluster";
		Integer clusterJobNum = 500;
		Boolean useCluster = false;
		
		Array cpdAlgorithm = array(Arrays.asList("cc", "kliep"));
		Array cpdFPR = array(Arrays.asList("0.0005", "0.001", "0.005", "0.01"));
		String trainingLabVisitFileExt = ".featurized.ground.csv";
		Var valiLabVisitFileExt = var(".featurized.ground.csv");
		Var testLabVisitFileExt = var(".featurized.").cat(cpdAlgorithm).dot().cat(cpdFPR).cat(".csv");
		String ground_str = "ground";
		
		singleScaleModel(expRootPath, datasetStr, tvtDataPath,
				labVisitFileFolder, trainingLabVisitFileExt, valiLabVisitFileExt,
				testLabVisitFileExt,
				trialGroupIdList, formulaList, formulaNameList,
				clusterWorkspace, "single", cpdAlgorithm, cpdFPR, clusterJobNum,
				useCluster, labels, ground_str);
	}

	public static void main(String[] args) {
		try {
			new DecisionTree().OSU_YR4_30Hz_Hip();
			//new DecisionTree().UQ_30Hz();
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
