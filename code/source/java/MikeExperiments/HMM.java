// Modified from web.engr.oregonstate.edu.zheng.gef.task.msexp.stacking.osu

package MikeExperiments;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

public class HMM extends TaskDef {

	private static final Logger log = Logger
			.getLogger(HMM.class.getName());

	private void trainValidate(Integer clusterJobNum, Boolean useCluster,
			Var expPath, Var modelId, String clusterWorkspace, String jobId,
			Array formula, Var trainDataInfoPath, Var validateDataInfoPath,
			String labVisitFileFolder, Var trainingLabVisitFileExt,
			List<String> tuningParams, List<String> tuningParamVals,
			String baseClassifierStr) throws Exception {
		logStep("Train/validate base classifier");
		Var trainValidateScriptPath = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var svmTrainCallingPath = expPath.fileSep().cat(baseClassifierStr).dot()
				.cat(modelId).cat(".R");
		//Var modelSavePath = expPath.fileSep().cat(baseClassifierStr).dot().cat(modelId)
		// .cat(".model.save");
		Var quickValidateSummaryPath = expPath.fileSep().cat(baseClassifierStr).dot()
				.cat(modelId).cat(".validate.summary.csv");
		ExecutorBuilder singleScale = rScript(trainValidateScriptPath,
				svmTrainCallingPath, var("quickTrainValidateCPD"),
				execConfig().setParallelizable(useCluster).setOnCluster(true)
						.setNumJobs(clusterJobNum).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		// parameters for model training
		singleScale.addParam("algorithm", String.class, "svm");
		singleScale.addParam("formula", String.class, var(formula));
		singleScale.addParam("trainDataInfoPath", String.class,
				trainDataInfoPath, VerificationType.Before);
		singleScale.addParam("validateDataInfoPath", String.class,
				validateDataInfoPath, VerificationType.Before);
		singleScale.addParam("labVisitFileFolder", String.class,
				labVisitFileFolder, VerificationType.Before);
		singleScale.addParam("trainingLabVisitFileExt", String.class,
				trainingLabVisitFileExt);
		singleScale.addParam("kernal", String.class, "linear");
		singleScale.addParam("validateSummaryPath", String.class,
				quickValidateSummaryPath, VerificationType.After);

		for (int i = 0; i < tuningParams.size(); ++i) {
			singleScale.addParam(tuningParams.get(i), Double.class, var(array(tuningParamVals.get(i))));
		}
		
		singleScale.prodMode();
		singleScale.execute();
	}

	private void summarizeValidate(Var expPath, Array splitId,
			Array formulaName, List<String> tuningParams,
			List<String> tuningParamVals, String tvtDataPath,
			String baseClassifierStr) throws Exception {
		logStep("Summarize model validation results");
		Table singleScaleValidationTbl = createTable(
				new TableColumn("ExpPath", expPath),
				new TableColumn("Split", var(splitId)),
				new TableColumn("Formula", var(formulaName)),
				new TableColumn("Scale", var("120")),
				new TableColumn("ValidateAccuracy", var("NA")));
		DataSet data = singleScaleValidationTbl.getData();
		for (int i = 0; i < data.getRowCount(); ++i) {
			String filePath = data.getData("ExpPath", i)
					+ "/" + baseClassifierStr + "."
					+ data.getData("Formula", i); 
			for (int j = 0; j < tuningParams.size(); ++j)
				filePath += "_" + data.getData(tuningParams.get(j), i);	
			filePath += ".validate.summary.csv";
			if (new File(filePath).exists()) {
				DataSet accTbl = new DataSet(filePath, true);
				double accuracy = accTbl.getDouble("Accuracy", 0);
				data.insertData("ValidateAccuracy", i, accuracy);
			}
		}
	singleScaleValidationTbl.save(tvtDataPath + "/" + baseClassifierStr + "/" 
			+ baseClassifierStr + ".validate.summary.csv");
	}
	
	private void testBestModel(Integer clusterJobNum, Boolean useCluster,
			Array formulaName, Var expPath, 
			String clusterWorkspace, 
			Var validateSummaryFile, Array formula, String jobId, 
			Var trainDataInfoPath, Var validateDataInfoPath, 
			Var trainHMMDataInfoPath, Array splitId, String labVisitFileFolder,
			Var trainingLabVisitFileExt, Var bestModelInfoPath,
			Var bestModelSavePath, Var trainResultPath, Var validateResultPath,
			Var trainHMMResultPath, String baseClassifierStr) throws Exception {
		logStep("Test the best base classifier model");
		Var testBestSingleScaleModelFunction = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var testBestSingleScaleModelScript = expPath.fileSep()
				.cat(baseClassifierStr).dot().cat(formulaName).cat(".best.R");
		ExecutorBuilder bestSingleScale = rScript(
				testBestSingleScaleModelFunction,
				testBestSingleScaleModelScript, var("testBestModelCPD"),
				execConfig().setParallelizable(useCluster).setOnCluster(true)
						.setNumJobs(clusterJobNum).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		bestSingleScale.addParam("algorithm", String.class, baseClassifierStr);
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
				trainHMMDataInfoPath, VerificationType.Before);
		bestSingleScale.addParam("labVisitFileFolder", String.class,
				labVisitFileFolder);
        bestSingleScale.addParam("trainingLabVisitFileExt", String.class,
                trainingLabVisitFileExt);
		bestSingleScale.addParam("kernal", String.class, "linear");
		bestSingleScale.addParam("bestModelInfoSavePath", String.class,
				bestModelInfoPath, VerificationType.After);
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
				trainHMMResultPath, VerificationType.After);
		bestSingleScale.addParam("validateSummaryFile", String.class,
				validateSummaryFile, VerificationType.Before);
		bestSingleScale.prodMode();
		bestSingleScale.execute();
	}
	
	/*private void summarizeTest(Integer clusterJobNum, Boolean useCluster,
			String clusterWorkspace, String jobId, Var expPath,
			Var bestModelId, Var testResultPath, Var confusionMatrixPath,
			Var pctConsufionMatrixPath, Var summaryPath) throws Exception {
		logStep("Summarize model test results");
		Var summarizeFunctionPath = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var summarizeScriptPath = expPath.fileSep().cat(baseClassifierStr).dot()
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
	
	private void makeTable(Array formulaName, String tvtDataPath,
			Array splitId, Array windowSizes, Array trialGroupId,
			Array testDataSets, Var modelPath) throws Exception {
		logStep("List all resources in a table");
		for(String form: formulaName.getValues()) {
			for(String alg : cpdAlgorithm.getValues()) {
				for (String fpr: cpdFPR.getValues()) {
					Var testResultPathSingle = var(tvtDataPath).cat("/svm/").cat("split").cat(splitId).cat("/svm.").cat(form).dot().cat(alg).dot().cat(fpr).cat(".best.model.test.csv");
					String bestModelIdSingle = "best." + form + "." + alg + "." + fpr;
					Var summaryPathSingle = var(tvtDataPath).cat("/svm/").cat("split").cat(splitId).cat("/svm.").cat(bestModelIdSingle).cat(".test.summary.csv");
					Var confusionMatrixPathSingle = var(tvtDataPath).cat("/svm/").cat("split").cat(splitId).cat("/svm.").cat(bestModelIdSingle).cat(".test.cm.csv");
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
							.cat("svm.model.").cat(alg).dot().cat(fpr)
							.cat(".res.csv");
					save(bestSingleScaleModelResTable, bestSingleScaleModelResSavePath);
				}
			}
		}
	}

	private void mergeSplits(Var modelPath, Var bestModelId,
			Array testDataSets, Array trialGroupId, Array formulaName,
			Array splitId) throws Exception {
		logStep("Merge splits results");
		Var mergeFunction = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var mergeScript = modelPath.fileSep().cat("svm.merge.split.")
				.cat(bestModelId).dot().cat(testDataSets).cat(".R");
		Var accuracySavePath = modelPath.fileSep()
				.cat("svm.model.").cat(bestModelId).dot()
				.cat(testDataSets).cat(".accuracy.csv");
		Var meanAccuracySavePath = modelPath.fileSep()
				.cat("svm.model.").cat(bestModelId).dot()
				.cat(testDataSets).cat(".summary.csv");
		Var confusionMatrixSavePath = modelPath.fileSep()
				.cat("svm.model.").cat(bestModelId).dot()
				.cat(testDataSets).cat(".cm.csv");
		Var pctConfusionMatrixSavePath = modelPath.fileSep()
				.cat("svm.model.").cat(bestModelId).dot()
				.cat(testDataSets).cat(".pctcm.csv");
		// Var reportSavePath = modelPath.fileSep()
		// .cat("svm.single.scale.model.").cat(modelId).dot()
		// .cat(testDataSets).cat(".csv");
		ExecutorBuilder merge = rScript(mergeFunction, mergeScript,
				var("mergeSplitShuffleCPD"));
		Var bestSingleScaleModelResSavePathVar = modelPath.fileSep()
				.cat("svm.model.").cat(cpdAlgorithm).dot().cat(cpdFPR)
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
	}*/

	/*
	private void mergeAlgorithmResults(Var modelPath, Array cpdAlgorithm, Array cpdFPR) throws Exception {
		logStep("Merge results across algorithms");
		Var mergeAlgorithmFunction = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var mergeAlgorithmScript = modelPath.fileSep().cat("svm.merge.algorithms.")
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
			Var trainingLabVisitFileExt,
			List<String> trialGroupIdList,
			Array windowSizes,
			List<String> formulaList, List<String> formulaNameList,
			String clusterWorkspace, String jobId,
			Integer clusterJobNum, Boolean useCluster,
			List<String> tuningParams, List<String> tuningParamVals,
			String baseClassifierStr) throws Exception {

		Var dataset = var(datasetStr);
		Array trialGroupIds = array(trialGroupIdList);

		//Array splitId = array("[0:1:30]");
		Array splitId = array("[0:1:2]");
		Var iterationId = var("split").cat(splitId);
		Var expPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat(baseClassifierStr).fileSep().cat(iterationId);

		Var trainDataInfoPath = var(tvtDataPath).fileSep().cat(iterationId)
				.fileSep().cat("trainBase.").cat("ws").cat(windowSizes)
				.cat(".data.csv");
		Var validateDataInfoPath = var(tvtDataPath).fileSep().cat(iterationId)
				.fileSep().cat("validateBase.").cat("ws").cat(windowSizes)
				.cat(".data.csv");
		Var trainHMMDataInfoPath = var(tvtDataPath).fileSep().cat(iterationId)
				.fileSep().cat("trainHMM.").cat("ws").cat(windowSizes)
				.cat(".data.csv");

		Array formula = array(formulaList);
		Array formulaName = array(formulaNameList);
		bind(formula, formulaName);
		Var modelId = var(formulaName);
		for (int i = 0; i < tuningParams.size(); ++i)
			modelId = modelId.cat("_").cat(tuningParams.get(i)).cat(array(tuningParamVals.get(i)));
		
		Var validateSummaryFile = var(tvtDataPath).fileSep()
				.cat(baseClassifierStr).fileSep().cat(baseClassifierStr)
				.cat(".validate.summary.csv");

		Var bestModelPrefix = expPath.fileSep().cat(baseClassifierStr).dot()
				.cat(formulaName).cat(".best.model");
		
		Var bestModelInfoPath = bestModelPrefix.cat(".info.csv");
		Var bestModelSavePath = bestModelPrefix.cat(".best.model").cat(".save");
		Var trainResultPath = bestModelPrefix.cat(".trainBase.csv");
		Var validateResultPath = bestModelPrefix.cat(".validateBase.csv");
		Var trainHMMResultPath = bestModelPrefix.cat(".trainHMM.csv");
		
		Var bestModelId = var("best").dot().cat(formulaName);
		Var confusionMatrixPath = expPath.fileSep().cat(baseClassifierStr)
				.dot().cat(bestModelId).cat(".test.cm.csv");
		Var pctConsufionMatrixPath = expPath.fileSep().cat(baseClassifierStr)
				.dot().cat(bestModelId).cat(".test.pctcm.csv");
		Var summaryPath = expPath.fileSep().cat(baseClassifierStr).dot()
				.cat(bestModelId).cat(".test.summary.csv");

		Array trialGroupId = array(trialGroupIdList);
		Array testDataSets = array(Arrays.asList("testHMM"));
		Var modelPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat(baseClassifierStr);
		
		// Validate tuning parameters if there are any
		if (!tuningParams.isEmpty()) {
			trainValidate(clusterJobNum, useCluster, expPath, modelId, clusterWorkspace, jobId, formula, trainDataInfoPath, validateDataInfoPath, labVisitFileFolder, trainingLabVisitFileExt, tuningParams, tuningParamVals, baseClassifierStr);
			summarizeValidate(expPath, splitId, formulaName, tuningParams, tuningParamVals, tvtDataPath, baseClassifierStr);
		}
		
		testBestModel(clusterJobNum, useCluster, formulaName, expPath, clusterWorkspace, validateSummaryFile, formula, jobId, trainDataInfoPath, validateDataInfoPath, trainHMMDataInfoPath, splitId, labVisitFileFolder, trainingLabVisitFileExt, bestModelInfoPath, bestModelSavePath, trainResultPath, validateResultPath, trainHMMResultPath, baseClassifierStr);
		//summarizeTest(clusterJobNum, useCluster, clusterWorkspace, jobId, expPath, bestModelId, testResultPath, confusionMatrixPath, pctConsufionMatrixPath, summaryPath);
		//makeTable(formulaName, tvtDataPath, splitId, windowSizes, trialGroupId, testDataSets, modelPath);
		//mergeSplits(modelPath, bestModelId, testDataSets, trialGroupId, formulaName, splitId);
	}
	
	private void OSU_YR4_30Hz_Hip() throws Exception {
		final int SVM = 1;
		final int NNET = 2;
		final int DT = 3;
		
		String expRootPath = "/nfs/guille/wong/wonglab3/obesity/2012/hmm";
		String datasetStr = "OSU_YR4_Hip_30Hz";
		String tvtDataPath = expRootPath + "/OSU_YR4_Hip_30Hz.HMM.7cls";
		String labVisitFileFolder = tvtDataPath + "/features";
		String clusterWorkspace = "/nfs/guille/wong/wonglab3/obesity/2012/cpd/OSU_YR4_Hip_30Hz.ws120.7cls/svm/cluster";
		List<String> formulaList = Arrays.asList(Formula.FORMULA_ALL_WO_FFT
				.toString());
		List<String> formulaNameList = Arrays.asList("AllWoFFT");
		
		// This block, along with the tuning parameters, will be customized to each experiment
		Array windowSizes = array(Arrays.asList("1","2","3","4","5","6","7","8","9","10"));
		List<String> trialGroupIdList = Arrays.asList("7cls");
		Integer clusterJobNum = 100;
		Boolean useCluster = false;
		int baseClassifier = SVM;

		List<String> tuningParams = Collections.emptyList();
		List<String> tuningParamVals = Collections.emptyList();
		String baseClassifierStr;
		switch (baseClassifier) {
			case SVM:
				baseClassifierStr = "svm";
				tuningParams = Arrays.asList("cost");
				tuningParamVals = Arrays.asList("[0.01,0.1,1,10,100,1000]");
				break;
			case NNET:
				baseClassifierStr = "nnet";
	            tuningParams = Arrays.asList("","");
				tuningParamVals = Arrays.asList("[5,10,15]", "[0.0,0.5,1]");
				break;
			case DT:
				baseClassifierStr = "dt";
				break;
			default:
				throw new Exception();
		}

		Var trainingLabVisitFileExt = var("PureTrial.featurized.").cat(windowSizes).cat(".csv");
		
		singleScaleModel(expRootPath, datasetStr, tvtDataPath,
				labVisitFileFolder, trainingLabVisitFileExt, trialGroupIdList,
				windowSizes, formulaList, formulaNameList, clusterWorkspace,
				"single", clusterJobNum, useCluster, tuningParams,
				tuningParamVals, baseClassifierStr);
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
				+ datasetStr + ".ws10.7cls/svm.linear.stacking/cluster";
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
			new HMM().OSU_YR4_30Hz_Hip();
			//new SVMLinearTrainValidateTest1().OSU_YR4_30Hz_Wrist();
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
