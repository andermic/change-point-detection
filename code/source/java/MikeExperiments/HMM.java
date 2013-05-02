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
			Var labVisitFileFolder, Var trainingLabVisitFileExt,
			List<String> tuningParams, List<Array> tuningParamVals,
			String baseClassifierStr, Array windowSizes) throws Exception {
		logStep("Train/validate base classifier");
		Var trainValidateScriptPath = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var TrainCallingPath = expPath.fileSep().cat(baseClassifierStr)
				.cat(".ws").cat(windowSizes).dot().cat(modelId).cat(".R");
		Var quickValidateSummaryPath = expPath.fileSep().cat(baseClassifierStr)
				.cat(".ws").cat(windowSizes).dot().cat(modelId)
				.cat(".validate.summary.csv");
		//Var modelSavePath = expPath.fileSep().cat(baseClassifierStr).dot().cat(modelId)
		// .cat(".model.save");
		
		ExecutorBuilder singleScale = rScript(trainValidateScriptPath,
				TrainCallingPath, var("quickTrainValidateCPD"),
				execConfig().setParallelizable(useCluster).setOnCluster(true)
						.setNumJobs(clusterJobNum).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		// parameters for model training
		singleScale.addParam("algorithm", String.class, baseClassifierStr);
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
			singleScale.addParam(tuningParams.get(i), Double.class, var(tuningParamVals.get(i)));
		}
		
		singleScale.prodMode();
		singleScale.execute();
	}

	private void summarizeValidate(Var expPath, Array splitId,
			Array formulaName, List<String> tuningParams,
			List<Array> tuningParamVals, String tvtDataPath,
			String baseClassifierStr, Array windowSizes) throws Exception {
		logStep("Summarize model validation results");
		
		for(String windowSize : windowSizes.getValues()) {
			ArrayList<TableColumn> columns = new ArrayList<TableColumn>(1);
			columns.add(new TableColumn("ExpPath", expPath));
			columns.add(new TableColumn("Split", var(splitId)));
			columns.add(new TableColumn("Formula", var(formulaName)));
			columns.add(new TableColumn("Scale", var(windowSize)));
			columns.add(new TableColumn("ValidateAccuracy", var("NA")));
			for (int i = 0; i < tuningParams.size(); ++i)
				columns.add(new TableColumn(tuningParams.get(i), var(tuningParamVals.get(i))));
			
			Table singleScaleValidationTbl = createTable(columns.toArray(new TableColumn[columns.size()]));
			DataSet data = singleScaleValidationTbl.getData();
			for (int i = 0; i < data.getRowCount(); ++i) {
				String filePath = data.getData("ExpPath", i) + "/"
						+ baseClassifierStr + ".ws" + windowSize + "."
						+ data.getData("Formula", i);
						 
				for (int j = 0; j < tuningParams.size(); ++j)
					filePath += "_" + tuningParams.get(j) +
					data.getData(tuningParams.get(j), i);	
				filePath += ".validate.summary.csv";
				//System.out.print(filePath);
				//System.out.print('\n');
				if (new File(filePath).exists()) {
					DataSet accTbl = new DataSet(filePath, true);
					double accuracy = accTbl.getDouble("Accuracy", 0);
					data.insertData("ValidateAccuracy", i, accuracy);
				}
			}
		singleScaleValidationTbl.save(tvtDataPath + "/" + baseClassifierStr + 
				"/" + baseClassifierStr + ".ws" + windowSize +
				".validate.summary.csv");
		}
	}
	
	private void testBestModel(Integer clusterJobNum, Boolean useCluster,
			Array formulaName, Var expPath, String clusterWorkspace, 
			Var validateSummaryFile, Array formula, String jobId, 
			Var trainDataInfoPath, Var validateDataInfoPath, 
			Var trainHMMDataInfoPath, Array splitId, Var labVisitFileFolder,
			Var trainingLabVisitFileExt, Var bestModelInfoPath,
			Var bestModelSavePath, Var trainResultPath, Var validateResultPath,
			Var trainHMMResultPath, List<String> tuningParams,
			List<Array> tuningParamVals, String baseClassifierStr,
			Array windowSizes, Array labels) throws Exception {
		logStep("Test the best base classifier model");
		Var testBestSingleScaleModelFunction = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var testBestSingleScaleModelScript = expPath.fileSep()
				.cat(baseClassifierStr).dot().cat("ws").cat(windowSizes).dot()
				.cat(formulaName).cat(".best.R");
		ExecutorBuilder bestSingleScale = rScript(
				testBestSingleScaleModelFunction,
				testBestSingleScaleModelScript, var("testBestModelCPD"),
				execConfig().setParallelizable(useCluster).setOnCluster(true)
						.setNumJobs(clusterJobNum).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		bestSingleScale.addParam("algorithm", String.class, baseClassifierStr);
		bestSingleScale.addParam("formula", Formula.class, var(formula));
		bestSingleScale.addParam("formulaName", String.class, var(formulaName));
		bestSingleScale.addParam("labels", List.class, RUtils.varToRList(var(labels), true));
		bestSingleScale.addParam("split", Integer.class, var(splitId));
		bestSingleScale.addParam("trainDataInfoPath", String.class,
				trainDataInfoPath, VerificationType.Before);
		bestSingleScale.addParam("validateDataInfoPath", String.class,
				validateDataInfoPath);
		bestSingleScale.addParam("testDataInfoPath", String.class,
				trainHMMDataInfoPath, VerificationType.Before);
		bestSingleScale.addParam("labVisitFileFolder", String.class,
				labVisitFileFolder);
        bestSingleScale.addParam("trainingLabVisitFileExt", String.class,
                trainingLabVisitFileExt);
		bestSingleScale.addParam("kernal", String.class, "linear");
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
		bestSingleScale.addParam("windowSize", Integer.class, var(windowSizes));

		if (tuningParams.size() > 0) {
			bestSingleScale.addParam("bestModelInfoSavePath", String.class,
					bestModelInfoPath, VerificationType.After);
			bestSingleScale.addParam("validateSummaryFile", String.class,
					validateSummaryFile, VerificationType.Before);
		}

		bestSingleScale.prodMode();
		bestSingleScale.execute();
	}
	
	private void summarizeTest(Integer clusterJobNum, Boolean useCluster,
			Array formula, Array windowSizes, String clusterWorkspace,
			String jobId, Var expPath, Var bestModelId, Var trainHMMResultPath,
			Var bestModelSavePath, Var testHMMDataInfoPath, Var labVisitFileFolder,
			Var trainingLabVisitFileExt, Var confusionMatrixPath,
			Var pctConsufionMatrixPath, Var summaryPath,
			String baseClassifierStr, Array labels) throws Exception {
		logStep("Train on HMM, predict on base classifier, predict on HMM");
		Var summarizeFunctionPath = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/hmm/hmm.R");
		Var summarizeScriptPath = expPath.fileSep().cat(baseClassifierStr).dot()
				.cat(bestModelId).cat(".predictHMM.R");
		Var predictBasePath = expPath.fileSep().cat(baseClassifierStr)
				.dot().cat(bestModelId).cat(".predict.base.csv");
		Var predictHMMPath = expPath.fileSep().cat(baseClassifierStr)
				.dot().cat(bestModelId).cat(".predict.hmm.csv");
				
		ExecutorBuilder predictHMM = rScript(summarizeFunctionPath,
				summarizeScriptPath, var("predictHMM"), 
				execConfig().setParallelizable(useCluster).setOnCluster(true)
						.setNumJobs(clusterJobNum).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		predictHMM.addParam("labels", List.class, RUtils.varToRList(var(labels), true));
		predictHMM.addParam("formula", String.class, var(formula));
		predictHMM.addParam("windowSize", Integer.class, var(windowSizes));
		predictHMM.addParam("trainHMMResultPath", String.class,
				trainHMMResultPath, VerificationType.Before);
		predictHMM.addParam("bestModelSavePath", String.class,
				bestModelSavePath, VerificationType.Before);
		predictHMM.addParam("testHMMDataInfoPath", String.class,
				testHMMDataInfoPath, VerificationType.Before);
		predictHMM.addParam("labVisitFileFolder", String.class,
				labVisitFileFolder, VerificationType.Before);
		predictHMM.addParam("labVisitFileExt", String.class,
				trainingLabVisitFileExt);
		predictHMM.addParam("predictBasePath", String.class,
				predictBasePath, VerificationType.After);
		predictHMM.addParam("predictHMMPath", String.class,
				predictHMMPath, VerificationType.After);
		predictHMM.addParam("confusionMatrixPath", String.class,
				confusionMatrixPath, VerificationType.After);
		predictHMM.addParam("pctConsufionMatrixPath", String.class,
				pctConsufionMatrixPath, VerificationType.After);
		predictHMM.addParam("summaryPath", String.class, summaryPath,
				VerificationType.After);
		predictHMM.prodMode();
		predictHMM.execute();
	}
	
	private void makeTable(Array formulaName, String tvtDataPath,
			Array splitId, Array windowSizes, Array trialGroupId,
			Array testDataSets, Var modelPath, String baseClassifierStr)
			throws Exception {
		logStep("List all resources in a table");
		for(String form: formulaName.getValues()) {
			for(String ws : windowSizes.getValues()) {
				Var testResultPathSingle = var(tvtDataPath).fileSep().cat(baseClassifierStr).fileSep().cat("split").cat(splitId).fileSep().cat(baseClassifierStr).cat(".ws.").cat(ws).dot().cat(form).cat(".best.model.test.csv");
				String bestModelIdSingle = "ws" + ws + ".best." + form;
				Var summaryPathSingle = var(tvtDataPath).fileSep().cat(baseClassifierStr).fileSep().cat("split").cat(splitId).fileSep().cat(baseClassifierStr).dot().cat(bestModelIdSingle).cat(".test.summary.csv");
				Var confusionMatrixPathSingle = var(tvtDataPath).fileSep().cat(baseClassifierStr).fileSep().cat("split").cat(splitId).fileSep().cat(baseClassifierStr).dot().cat(bestModelIdSingle).cat(".test.cm.csv");
				Table bestSingleScaleModelResTable = createTable(
						newColumn("WindowSize", var(ws)),
						newColumn("TrialGroup", trialGroupId),
						newColumn("Formula", formulaName),
						newColumn("Split", splitId),
						newColumn("TestDataSet", testDataSets),
						newColumn("TestReportFile", testResultPathSingle),
						newColumn("TestAccuracyFile", summaryPathSingle),
						newColumn("TestConfusionMatrixFile", confusionMatrixPathSingle));
				Var bestSingleScaleModelResSavePath = modelPath.fileSep()
						.cat(baseClassifierStr).cat(".model.").cat(ws)
						.cat(".res.csv");
				save(bestSingleScaleModelResTable, bestSingleScaleModelResSavePath);
			}
		}
	}

	private void mergeSplits(Var modelPath, Var bestModelId,
			Array testDataSets, Array trialGroupId, Array formulaName,
			Array splitId, Array windowSizes, String baseClassifierStr)
			throws Exception {
		logStep("Merge splits results");
		Var mergeFunction = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var mergeScript = modelPath.fileSep().cat(baseClassifierStr).cat(".merge.split.")
				.cat(bestModelId).dot().cat(testDataSets).cat(".R");
		Var accuracySavePath = modelPath.fileSep()
				.cat(baseClassifierStr).cat(".model.").cat(bestModelId).dot()
				.cat(testDataSets).cat(".accuracy.csv");
		Var meanAccuracySavePath = modelPath.fileSep()
				.cat(baseClassifierStr).cat(".model.").cat(bestModelId).dot()
				.cat(testDataSets).cat(".summary.csv");
		Var confusionMatrixSavePath = modelPath.fileSep()
				.cat(baseClassifierStr).cat(".model.").cat(bestModelId).dot()
				.cat(testDataSets).cat(".cm.csv");
		Var pctConfusionMatrixSavePath = modelPath.fileSep()
				.cat(baseClassifierStr).cat(".model.").cat(bestModelId).dot()
				.cat(testDataSets).cat(".pctcm.csv");
		// Var reportSavePath = modelPath.fileSep()
		// .cat(baseClassifierStr).cat(".single.scale.model.").cat(modelId).dot()
		// .cat(testDataSets).cat(".csv");
		ExecutorBuilder merge = rScript(mergeFunction, mergeScript,
				var("mergeSplitShuffleCPD"));
		Var bestSingleScaleModelResSavePathVar = modelPath.fileSep()
				.cat(baseClassifierStr).cat(".model.").cat(windowSizes)
				.cat(".res.csv");
		merge.addParam("bestModelResFilePath", String.class,
				bestSingleScaleModelResSavePathVar, VerificationType.Before);
		merge.addParam("windowSize", Integer.class, var(windowSizes));
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
		Var mergeAlgorithmScript = modelPath.fileSep().cat(baseClassifierStr).cat(".merge.algorithms.")
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
			String tvtDataPath, Var labVisitFileFolder,
			Var trainingLabVisitFileExt,
			List<String> trialGroupIdList,
			Array windowSizes,
			List<String> formulaList, List<String> formulaNameList,
			String clusterWorkspace, String jobId,
			Integer clusterJobNum, Boolean useCluster,
			List<String> tuningParams, List<Array> tuningParamVals,
			String baseClassifierStr, Array labels) throws Exception {

		Var dataset = var(datasetStr);
		Array trialGroupIds = array(trialGroupIdList);

		Array splitId = array("[0:1:10]");
		//Array splitId = array("0");
		Var iterationId = var("split").cat(splitId);
		Var expPath = var(expRootPath).fileSep().cat(dataset).dot().cat("HMM")
				.cat(trialGroupIds).fileSep().cat(baseClassifierStr).fileSep()
				.cat(iterationId);
		
		Var trainDataInfoPath = var(tvtDataPath).fileSep().cat(iterationId)
				.fileSep().cat("trainBase.ws").cat(windowSizes) 
				.cat(".data.csv");
		Var validateDataInfoPath = var(tvtDataPath).fileSep().cat(iterationId)
				.fileSep().cat("validateBase.ws").cat(windowSizes)
				.cat(".data.csv");
		Var trainHMMDataInfoPath = var(tvtDataPath).fileSep().cat(iterationId)
				.fileSep().cat("trainHMM.ws").cat(windowSizes)
				.cat(".data.csv");
		Var testHMMDataInfoPath = var(tvtDataPath).fileSep().cat(iterationId)
				.fileSep().cat("testHMM.ws").cat(windowSizes).cat(".data.csv");
		
		Array formula = array(formulaList);
		Array formulaName = array(formulaNameList);
		bind(formula, formulaName);
		Var modelId = var(formulaName);
		for (int i = 0; i < tuningParams.size(); ++i)
			modelId = modelId.cat("_").cat(tuningParams.get(i)).cat(tuningParamVals.get(i));
		
		Var validateSummaryFile = var(tvtDataPath).fileSep()
				.cat(baseClassifierStr).fileSep().cat(baseClassifierStr)
				.cat(".ws").cat(windowSizes).cat(".validate.summary.csv");
		Var bestModelPrefix = expPath.fileSep().cat(baseClassifierStr)
				.cat(".ws").cat(windowSizes).dot().cat(formulaName)
				.cat(".best.model");
		Var bestModelInfoPath = bestModelPrefix.cat(".info.csv");
		Var bestModelSavePath = bestModelPrefix.cat(".save");
		Var trainResultPath = bestModelPrefix.cat(".trainBase.csv");
		Var validateResultPath = bestModelPrefix.cat(".validateBase.csv");
		Var trainHMMResultPath = bestModelPrefix.cat(".trainHMM.csv");
		Var testHMMResultPath = bestModelPrefix.cat(".testHMM.csv");
		
		Var bestModelId = var("ws").cat(windowSizes).cat(".best").dot().cat(formulaName);
		Var confusionMatrixPath = expPath.fileSep().cat(baseClassifierStr)
				.dot().cat(bestModelId).cat(".test.cm.csv");
		Var pctConsufionMatrixPath = expPath.fileSep().cat(baseClassifierStr)
				.dot().cat(bestModelId).cat(".test.pctcm.csv");
		Var summaryPath = expPath.fileSep().cat(baseClassifierStr).dot()
				.cat(bestModelId).cat(".test.summary.csv");

		Array trialGroupId = array(trialGroupIdList);
		Array testDataSets = array(Arrays.asList("testHMM"));
		Var modelPath = var(tvtDataPath).fileSep().cat(baseClassifierStr);
		
		// Validate with tuning parameters if there are any
		if (!tuningParams.isEmpty()) {
			trainValidate(clusterJobNum, useCluster, expPath, modelId, clusterWorkspace, jobId, formula, trainDataInfoPath, validateDataInfoPath, labVisitFileFolder, trainingLabVisitFileExt, tuningParams, tuningParamVals, baseClassifierStr, windowSizes);
			summarizeValidate(expPath, splitId, formulaName, tuningParams, tuningParamVals, tvtDataPath, baseClassifierStr, windowSizes);
		}
		
		testBestModel(clusterJobNum, useCluster, formulaName, expPath, clusterWorkspace, validateSummaryFile, formula, jobId, trainDataInfoPath, validateDataInfoPath, trainHMMDataInfoPath, splitId, labVisitFileFolder, trainingLabVisitFileExt, bestModelInfoPath, bestModelSavePath, trainResultPath, validateResultPath, trainHMMResultPath, tuningParams, tuningParamVals, baseClassifierStr, windowSizes, labels);
		summarizeTest(clusterJobNum, useCluster, formula, windowSizes, clusterWorkspace, jobId, expPath, bestModelId, trainHMMResultPath, bestModelSavePath, testHMMDataInfoPath, labVisitFileFolder, trainingLabVisitFileExt, confusionMatrixPath, pctConsufionMatrixPath, summaryPath, baseClassifierStr, labels);
		makeTable(formulaName, tvtDataPath, splitId, windowSizes, trialGroupId, testDataSets, modelPath, baseClassifierStr);
		mergeSplits(modelPath, bestModelId, testDataSets, trialGroupId, formulaName, splitId, windowSizes, baseClassifierStr);
	}
	
	private void OSU_YR4_30Hz_Hip() throws Exception {
		final int SVM = 1;
		final int NNET = 2;
		final int DT = 3;
		
		String expRootPath = "/nfs/guille/wong/users/andermic/Desktop/hmm";
		String expName = "OSU_YR4_Hip_30Hz.HMM.7cls";
		String datasetStr = "OSU_YR4_Hip_30Hz";
		String tvtDataPath = expRootPath + "/" + expName;
		List<String> formulaList = Arrays.asList(Formula.FORMULA_ALL_WO_FFT
				.toString());
		List<String> formulaNameList = Arrays.asList("AllWoFFT");
		List<String> trialGroupIdList = Arrays.asList(".7cls");
		Array labels = array(Arrays.asList("lying_down", "sitting",
				"standing_household", "walking", "running",
				"basketball", "dance"));
		
		// This block, along with the tuning parameters, will be customized to each experiment
		Array windowSizes = array("[1:1:20]");
		Var labVisitFileFolder = var(tvtDataPath).cat("/features/").cat("ws").cat(windowSizes);
		Integer clusterJobNum = 100;
		int baseClassifier = SVM;
		Boolean useCluster = true;

		List<String> tuningParams = Collections.emptyList();
		List<Array> tuningParamVals = Collections.emptyList();
		String baseClassifierStr;
		switch (baseClassifier) {
			case SVM:
				baseClassifierStr = "svm";
				tuningParams = Arrays.asList("Cost");
				tuningParamVals = Arrays.asList(array("[0.01,0.1,1,10,100,1000]"));
				break;
			case NNET:
				baseClassifierStr = "nnet";
	            tuningParams = Arrays.asList("NumHiddenUnits","WeightDecay");
				tuningParamVals = Arrays.asList(array("[1:1:30]"), array("[0.0,0.5,1]"));
				break;
			case DT:
				baseClassifierStr = "dt";
				break;
			default:
				throw new Exception();
		}

		Var trainingLabVisitFileExt = var("PureTrial.featurized.csv");
		String clusterWorkspace = "/nfs/guille/wong/users/andermic/Desktop/hmm/" + expName + "/" + baseClassifierStr + "/cluster";
		
		singleScaleModel(expRootPath, datasetStr, tvtDataPath,
				labVisitFileFolder, trainingLabVisitFileExt, trialGroupIdList,
				windowSizes, formulaList, formulaNameList, clusterWorkspace,
				"single", clusterJobNum, useCluster, tuningParams,
				tuningParamVals, baseClassifierStr, labels);
	}

	private void UQ_30Hz() throws Exception {
		final int SVM = 1;
		final int NNET = 2;
		final int DT = 3;
		
		String expRootPath = "/nfs/guille/wong/wonglab3/obesity/2012/hmm";
		String day = "2";
		String expName = "uq_30Hz_day" + day + ".HMM";
		String datasetStr = "uq_30Hz_day" + day;
		String tvtDataPath = expRootPath + "/" + expName;
		List<String> formulaList = Arrays.asList(Formula.FORMULA_ALL_WO_FFT
				.toString());
		List<String> formulaNameList = Arrays.asList("AllWoFFT");
		List<String> trialGroupIdList = Arrays.asList("");
		Array labels = array(Arrays.asList("0", "1", "2"));
		
		// This block, along with the tuning parameters, will be customized to each experiment
		Array windowSizes = array("[1:1:20]");
		windowSizes = array(Arrays.asList("16","18","20"));
		Var labVisitFileFolder = var(tvtDataPath).cat("/features/").cat("ws").cat(windowSizes);
		Integer clusterJobNum = 100;
		int baseClassifier = NNET;
		Boolean useCluster = true;

		List<String> tuningParams = Collections.emptyList();
		List<Array> tuningParamVals = Collections.emptyList();
		String baseClassifierStr;
		switch (baseClassifier) {
			case SVM:
				baseClassifierStr = "svm";
				tuningParams = Arrays.asList("Cost");
				tuningParamVals = Arrays.asList(array("[0.01,0.1,1,10,100,1000]"));
				break;
			case NNET:
				baseClassifierStr = "nnet";
	            tuningParams = Arrays.asList("NumHiddenUnits","WeightDecay");
	            tuningParamVals = Arrays.asList(array("[1:1:30]"), array("[0.0,0.5,1]"));
				break;
			case DT:
				baseClassifierStr = "dt";
				break;
			default:
				throw new Exception();
		}

		Var trainingLabVisitFileExt = var(".featurized.csv");
		String clusterWorkspace = expRootPath + "/" + expName + "/" + baseClassifierStr + "/cluster";
		
		singleScaleModel(expRootPath, datasetStr, tvtDataPath,
				labVisitFileFolder, trainingLabVisitFileExt, trialGroupIdList,
				windowSizes, formulaList, formulaNameList, clusterWorkspace,
				"single", clusterJobNum, useCluster, tuningParams,
				tuningParamVals, baseClassifierStr, labels);
	}	

	public static void main(String[] args) {
		try {
			//new HMM().OSU_YR4_30Hz_Hip();
			new HMM().UQ_30Hz();
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
