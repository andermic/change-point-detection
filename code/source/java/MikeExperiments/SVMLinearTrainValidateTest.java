// Modified from web.engr.oregonstate.edu.zheng.gef.task.msexp.stackingmv.osu

package MikeExperiments;

import java.io.File;
import java.util.Arrays;
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

public class SVMLinearTrainValidateTest extends TaskDef {

	private static final Logger log = Logger
			.getLogger(SVMLinearTrainValidateTest.class.getName());

	private void singleScaleModel(String expRootPath, String datasetStr,
			String tvtDataPath, String labVisitFileFolder,
			String labVisitFileExt, List<String> trialGroupIdList,
			List<String> windowSizeList, String costExp, String scaleExp,
			List<String> formulaList, List<String> formulaNameList,
			String clusterWorkspace, String jobId) throws Exception {
		logStep("Train single sacle svm on training data");
		Var dataset = var(datasetStr);
		Array windowSizes = array(windowSizeList);
		Array trialGroupIds = array(trialGroupIdList);

		Array splitId = array("[0:1:30]");
		Var iterationId = var("split").cat(splitId);
		Var dataPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat("svm.linear.stacking").fileSep().cat(iterationId);
		Var expPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat("svm.linear.stacking.mv").fileSep().cat(iterationId);

		Array formulaName = array(formulaNameList);
		Array scale = array(scaleExp);

		// convert single scale model prediction results to features
		logStep("Convert single scale model prediction results to stacking features");
		Array subsets = array(Arrays.asList("train", "validate", "test"));
		Var stackingFeaturizesFunction = var("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/ms.osu/stacking.featurize.R");
		Var stackingFeaturizesScript = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).dot().cat(subsets)
				.cat(".stacking.featurize.R");
		Var stackingFeaturizedPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).dot().cat(subsets)
				.cat(".stacking.features.csv");
		ExecutorBuilder stackingFeatures = rScript(stackingFeaturizesFunction,
				stackingFeaturizesScript, var("featurizeMajorityVote"),
				execConfig().setParallelizable(false).setOnCluster(true)
						.setNumJobs(300).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		stackingFeatures.addParam("predictionDataFolder", String.class,
				dataPath);
		stackingFeatures.addParam("algorithmId", String.class, "svm");
		stackingFeatures.addParam("modelId", String.class, var(formulaName)
				.cat(".best.model"));
		stackingFeatures.addParam("scalePrefix", String.class, "scale");
		stackingFeatures.addParam("scaleList", List.class,
				RUtils.varToRList(var(scale), false));
		stackingFeatures.addParam("fileExt", String.class,
				var(subsets).cat(".csv"));
		stackingFeatures.addParam("trialGroupId", String.class,
				var(trialGroupIds));
		stackingFeatures.addParam("savePath", String.class,
				stackingFeaturizedPath, VerificationType.After);
		stackingFeatures.prodMode();
		stackingFeatures.execute();

		logStep("Summarize single size subwindow models");
		Var modelId = var(formulaName);
		Var summarizeFunction = var("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/ms.osu/stacking.featurize.R");
		Var summarizeScript = expPath.fileSep().cat("svm.linear").dot()
				.cat(modelId).dot().cat(subsets)
				.cat(".summarize.single.size.R");
		ExecutorBuilder summarizeSingleSize = rScript(summarizeFunction,
				summarizeScript, var("summarizeMajorityVoteResults"),
				execConfig().setParallelizable(false).setOnCluster(true)
						.setNumJobs(10).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		summarizeSingleSize.addParam("mvFile", String.class,
				stackingFeaturizedPath, VerificationType.Before);
		summarizeSingleSize.addParam("labels", List.class, RUtils.varToRList(
				var(array(Arrays.asList("lying_down", "sitting",
						"standing_household", "walking", "running",
						"basketball", "dance"))), true));
		summarizeSingleSize.addParam("labelColname", String.class,
				"ActivityClass");
		summarizeSingleSize.addParam("scales", List.class,
				RUtils.varToRList(var(scale), false));
		summarizeSingleSize.addParam("savePath", String.class, expPath);
		summarizeSingleSize.addParam("saveName", String.class,
				var("svm.linear.single.scale.model.").cat(modelId).cat("_s"));
		summarizeSingleSize.addParam("dataset", String.class, var(subsets));
		summarizeSingleSize.after(expPath.fileSep()
				.cat("svm.linear.single.scale.model.").cat(modelId).cat("_s1")
				.dot().cat(subsets).cat(".summary.csv"));
		summarizeSingleSize.after(expPath.fileSep()
				.cat("svm.linear.single.scale.model.").cat(modelId).cat("_s1")
				.dot().cat(subsets).cat(".summary.csv"));
		summarizeSingleSize.after(expPath.fileSep()
				.cat("svm.linear.single.scale.model.").cat(modelId).cat("_s1")
				.dot().cat(subsets).cat(".summary.csv"));
		summarizeSingleSize.prodMode();
		summarizeSingleSize.execute();

		logStep("List all resources in a table");
		subsets = array(Arrays.asList("test"));
		modelId = var(formulaName).cat("_s").cat(scale);
		Var testResultPath = expPath.fileSep()
				.cat("svm.linear.single.scale.model.").cat(modelId).dot()
				.cat(subsets).cat(".csv");
		Var summaryPath = expPath.fileSep()
				.cat("svm.linear.single.scale.model.").cat(modelId).dot()
				.cat(subsets).cat(".summary.csv");
		Var confusionMatrixPath = expPath.fileSep()
				.cat("svm.linear.single.scale.model.").cat(modelId).dot()
				.cat(subsets).cat(".cm.csv");
		Array trialGroupId = array(trialGroupIdList);
		Table bestSingleScaleModelResTable = createTable(
				newColumn("WindowSize", windowSizes),
				newColumn("TrialGroup", trialGroupId),
				newColumn("Formula", formulaName), newColumn("Split", splitId),
				newColumn("TestDataSet", subsets), newColumn("Scale", scale),
				newColumn("TestReportFile", testResultPath),
				newColumn("TestAccuracyFile", summaryPath),
				newColumn("TestConfusionMatrixFile", confusionMatrixPath));
		expPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat("svm.linear.stacking.mv");
		Var bestSingleScaleModelResSavePath = expPath.fileSep().cat(
				"svm.linear.single.scale.model.res.csv");
		save(bestSingleScaleModelResTable, bestSingleScaleModelResSavePath);

		logStep("Merge splits results");
		Var mergeFunction = var("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/function/merge.split.shuffle.R");
		Var mergeScript = expPath.fileSep().cat("merge.split.").cat(modelId)
				.dot().cat(subsets).cat(".R");
		Var accuracySavePath = expPath.fileSep()
				.cat("svm.linear.single.scale.model.").cat(modelId).dot()
				.cat(subsets).cat(".accuracy.csv");
		Var meanAccuracySavePath = expPath.fileSep()
				.cat("svm.linear.single.scale.model.").cat(modelId).dot()
				.cat(subsets).cat(".summary.csv");
		Var confusionMatrixSavePath = expPath.fileSep()
				.cat("svm.linear.single.scale.model.").cat(modelId).dot()
				.cat(subsets).cat(".cm.csv");
		Var pctConfusionMatrixSavePath = expPath.fileSep()
				.cat("svm.linear.single.scale.model.").cat(modelId).dot()
				.cat(subsets).cat(".pctcm.csv");
		ExecutorBuilder merge = rScript(mergeFunction, mergeScript,
				var("mergeSplitShuffle"));
		merge.addParam("bestModelResFilePath", String.class,
				bestSingleScaleModelResSavePath, VerificationType.Before);
		merge.addParam("windowSize", Integer.class, var(windowSizes));
		merge.addParam("scale", Integer.class, var(scale));
		merge.addParam("formula", String.class, var(formulaName));
		merge.addParam("testDataSet", String.class, var(subsets));
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
		merge.prodMode();
		merge.execute();
	}

	private void allScaleStackingModel(String expRootPath, String datasetStr,
			String tvtDataPath, List<String> trialGroupIdList,
			List<String> windowSizeList, List<String> formulaList,
			List<String> formulaNameList, String costExp,
			String clusterWorkspace, String jobId) throws Exception {
		Var dataset = var(datasetStr);
		Array windowSizes = array(windowSizeList);
		Array trialGroupIds = array(trialGroupIdList);

		Array splitId = array("[0:1:30]");
		Var iterationId = var("split").cat(splitId);
		Var expPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat("svm.linear.stacking.mv").fileSep().cat(iterationId);

		Array formula = array(formulaList);
		Array formulaName = array(formulaNameList);
		bind(formula, formulaName);
		Array cost = array(costExp);
		Var modelId = var(formulaName).cat("_cost").cat(cost).cat(".stacking");

		Var trainDataPath = expPath.fileSep().cat("svm").dot().cat(formulaName)
				.cat(".train.stacking.features.csv");
		Var validateDataPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".validate.stacking.features.csv");
		Var testDataPath = expPath.fileSep().cat("svm").dot().cat(formulaName)
				.cat(".test.stacking.features.csv");

		logStep("Quickly train and validate the models");
		List<String> stackingFeatureList = Arrays
				.asList(Formula.getWeightingModelFormula("ActivityClass",
						"7cls", "scale1to10").toString());
		Array weightingFeatures = array(stackingFeatureList);
		Array trialGroupId = array(trialGroupIdList);
		bind(weightingFeatures, trialGroupId);
		Var stackingModelFunction = var("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/ms.osu/svm.exp.R");
		Var stackingModelScript = expPath.fileSep().cat("svm").dot()
				.cat(modelId).dot().cat("quick.train.validate.R");
		// Var modelSavePath = expPath.fileSep().cat("svm").dot().cat(modelId)
		// .cat(".stacking.model.save");
		ExecutorBuilder trainValidate = rScript(stackingModelFunction,
				stackingModelScript, var("quickTrainValidate2"),
				execConfig().setParallelizable(true).setOnCluster(true)
						.setNumJobs(50).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		Var quickValidateSummaryPath = expPath.fileSep().cat("svm").dot()
				.cat(modelId).cat(".validate.summary.csv");
		// parameters for model training
		trainValidate.addParam("formula", String.class, var(weightingFeatures));
		trainValidate.addParam("trainDataPath", String.class, trainDataPath,
				VerificationType.Before);
		trainValidate.addParam("validateDataPath", String.class,
				validateDataPath, VerificationType.Before);
		trainValidate.addParam("kernal", String.class, "linear");
		trainValidate.addParam("cost", Double.class, var(cost));
		trainValidate.addParam("validateSummaryPath", String.class,
				quickValidateSummaryPath, VerificationType.After);
		trainValidate.prodMode();
		trainValidate.execute();

		logStep("Summarize single sacle model validation results");
		Table stackingValidationTbl = createTable(new TableColumn("ExpPath",
				expPath), new TableColumn("Split", var(splitId)),
				new TableColumn("Formula", var(formulaName)), new TableColumn(
						"Cost", var(cost)), new TableColumn("ValidateAccuracy",
						var("NA")));
		DataSet data = stackingValidationTbl.getData();
		for (int i = 0; i < data.getRowCount(); ++i) {
			String filePath = data.getData("ExpPath", i) + "/svm."
					+ data.getData("Formula", i) + "_cost"
					+ data.getData("Cost", i)
					+ ".stacking.validate.summary.csv";
			if (new File(filePath).exists()) {
				DataSet accTbl = new DataSet(filePath, true);
				double accuracy = accTbl.getDouble("Accuracy", 0);
				data.insertData("ValidateAccuracy", i, accuracy);
			}
		}
		String validateSummaryFile = tvtDataPath
				+ "/svm.linear.stacking.mv/svm.linear.stacking.mv.validate.summary.csv";
		stackingValidationTbl.save(validateSummaryFile);

		logStep("Test the best stacking models");
		Var testBestSingleScaleModelFunction = var("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/ms.osu/svm.exp.R");
		Var testBestSingleScaleModelScript = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model.R");

		Var bestModelInfoPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model.info.csv");
		Var bestModelSavePath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model.save");

		Var trainResultPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model.train.csv");
		Var trainConfusionMatrixPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model.train.cm.csv");
		Var trainPctConfusionMatrixPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model.train.pctcm.csv");
		Var trainSummaryPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model.train.summary.csv");

		Var validateResultPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model.validate.csv");
		Var validateConfusionMatrixPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model.validate.cm.csv");
		Var validatePctConfusionMatrixPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName)
				.cat(".best.stacking.model.validate.pctcm.csv");
		Var validateSummaryPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName)
				.cat(".best.stacking.model.validate.summary.csv");

		Var testResultPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model.test.csv");
		Var testConfusionMatrixPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model.test.cm.csv");
		Var testPctConfusionMatrixPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model.test.pctcm.csv");
		Var testSummaryPath = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model.test.summary.csv");

		ExecutorBuilder bestStackingModel = rScript(
				testBestSingleScaleModelFunction,
				testBestSingleScaleModelScript, var("testBestStackingModel"),
				execConfig().setParallelizable(true).setOnCluster(true)
						.setNumJobs(10).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		bestStackingModel.addParam("validateSummaryFile", String.class,
				validateSummaryFile, VerificationType.Before);
		bestStackingModel.addParam("features", String.class,
				var(weightingFeatures));
		bestStackingModel.addParam("formulaName", String.class,
				var(formulaName));
		bestStackingModel.addParam("labels", List.class, RUtils.varToRList(
				var(array(Arrays.asList("lying_down", "sitting",
						"standing_household", "walking", "running",
						"basketball", "dance"))), true));
		bestStackingModel.addParam("split", Integer.class, var(splitId));
		bestStackingModel.addParam("trainDataPath", String.class,
				trainDataPath, VerificationType.Before);
		bestStackingModel.addParam("validateDataPath", String.class,
				validateDataPath, VerificationType.Before);
		bestStackingModel.addParam("testDataPath", String.class, testDataPath,
				VerificationType.Before);
		bestStackingModel.addParam("kernal", String.class, "linear");
		bestStackingModel.addParam("bestModelInfoSavePath", String.class,
				bestModelInfoPath, VerificationType.After);
		bestStackingModel.addParam("bestModelSavePath", String.class,
				bestModelSavePath, VerificationType.After);
		// parameters for testing the trained model on training data
		bestStackingModel.addParam("trainReportPath", String.class,
				trainResultPath, VerificationType.After);
		bestStackingModel.addParam("trainConfusionMatrixPath", String.class,
				trainConfusionMatrixPath, VerificationType.After);
		bestStackingModel.addParam("trainPctConfusionMatrixPath", String.class,
				trainPctConfusionMatrixPath, VerificationType.After);
		bestStackingModel.addParam("trainSummaryPath", String.class,
				trainSummaryPath, VerificationType.After);
		// parameters for testing the trained model on validation data
		bestStackingModel.addParam("validateReportPath", String.class,
				validateResultPath, VerificationType.After);
		bestStackingModel.addParam("validateConfusionMatrixPath", String.class,
				validateConfusionMatrixPath, VerificationType.After);
		bestStackingModel.addParam("validatePctConfusionMatrixPath",
				String.class, validatePctConfusionMatrixPath,
				VerificationType.After);
		bestStackingModel.addParam("validateSummaryPath", String.class,
				validateSummaryPath, VerificationType.After);
		// parameters for testing the trained model on testing data
		bestStackingModel.addParam("testReportPath", String.class,
				testResultPath, VerificationType.After);
		bestStackingModel.addParam("testConfusionMatrixPath", String.class,
				testConfusionMatrixPath, VerificationType.After);
		bestStackingModel.addParam("testPctConfusionMatrixPath", String.class,
				testPctConfusionMatrixPath, VerificationType.After);
		bestStackingModel.addParam("testSummaryPath", String.class,
				testSummaryPath, VerificationType.After);
		bestStackingModel.prodMode();
		bestStackingModel.execute();

		logStep("List all resources in a table");
		Array testDataSets = array(Arrays.asList("train", "validate", "test"));
		Var testAccuracyFile = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model").dot()
				.cat(testDataSets).cat(".summary.csv");
		Var testConfusionMatrix = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).cat(".best.stacking.model").dot()
				.cat(testDataSets).cat(".cm.csv");
		Table bestModelResTable = createTable(
				newColumn("WindowSize", windowSizes),
				newColumn("TrialGroup", trialGroupId),
				newColumn("Formula", formulaName), newColumn("Split", splitId),
				newColumn("TestDataSet", testDataSets),
				newColumn("TestAccuracyFile", testAccuracyFile),
				newColumn("TestConfusionMatrixFile", testConfusionMatrix));
		expPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat("svm.linear.stacking.mv");
		Var bestModelResSavePath = expPath.fileSep().cat(
				"svm.best.stacking.model.res.csv");
		save(bestModelResTable, bestModelResSavePath);

		logStep("Merge splits results");
		modelId = var(formulaName);
		Var mergeFunction = var("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/function/merge.split.shuffle.R");
		Var mergeScript = expPath.fileSep().cat("svm.merge.split.")
				.cat(modelId).dot().cat(testDataSets).cat(".R");
		ExecutorBuilder merge = rScript(mergeFunction, mergeScript,
				var("mergeSplitShuffle"));
		merge.addParam("bestModelResFilePath", String.class,
				bestModelResSavePath, VerificationType.Before);
		merge.addParam("windowSize", Integer.class, var(windowSizes));
		merge.addParam("trialGroup", String.class, var(trialGroupId));
		merge.addParam("formula", String.class, var(formulaName));
		merge.addParam("testDataSet", String.class, var(testDataSets));
		String expectedNumEntries = String.valueOf(splitId.getValues().size());
		merge.addParam("expectedNumEntries", Integer.class,
				var(expectedNumEntries));
		Var accuracySavePath = expPath.fileSep()
				.cat("svm.best.stacking.model.").cat(modelId).dot()
				.cat(testDataSets).cat(".accuracy.csv");
		merge.addParam("accuracySavePath", String.class, accuracySavePath,
				VerificationType.After);
		Var meanAccuracySavePath = expPath.fileSep()
				.cat("svm.best.stacking.model.").cat(modelId).dot()
				.cat(testDataSets).cat(".summary.csv");
		merge.addParam("meanAccuracySavePath", String.class,
				meanAccuracySavePath, VerificationType.After);
		Var confusionMatrixSavePath = expPath.fileSep()
				.cat("svm.best.stacking.model.").cat(modelId).dot()
				.cat(testDataSets).cat(".cm.csv");
		merge.addParam("confusionMatrixSavePath", String.class,
				confusionMatrixSavePath, VerificationType.After);
		Var pctConfusionMatrixSavePath = expPath.fileSep()
				.cat("svm.best.stacking.model.").cat(modelId).dot()
				.cat(testDataSets).cat(".pctcm.csv");
		merge.addParam("pctConfusionMatrixSavePath", String.class,
				pctConfusionMatrixSavePath, VerificationType.After);
		merge.prodMode();
		merge.execute();

		logStep("Create latex confusion matrix");
		Var texCmClass = var("web.engr.oregonstate.edu.zheng.gef.def.utils.LatexConfusionMatrixCreator");
		ExecutorBuilder texConsuionMatrix = java(texCmClass,
				var("createConfusionMatrixTex"));
		texConsuionMatrix.addParam("confusionMatrixPath", String.class,
				pctConfusionMatrixSavePath);
		Var caption = var("Confusion matrix of Glmnet with ").cat(formulaName)
				.cat(" feature set on ").cat(testDataSets).cat(" data (")
				.cat("DataSet: ").cat(dataset).cat(" WindowSize: ")
				.cat(windowSizes).cat(")");
		texConsuionMatrix.addParam("caption", String.class, caption);
		Var label = var("tbl:svm.").cat(dataset).cat(".ws").cat(windowSizes)
				.dot().cat(modelId).dot().cat(testDataSets);
		texConsuionMatrix.addParam("label", String.class, label);
		texConsuionMatrix.prodMode();
		texConsuionMatrix.execute();
	}

	private void OSU_YR4_30Hz_Hip() throws Exception {
		String expRootPath = "/nfs/guille/wong/wonglab3/obesity/2012/cpd";
		String datasetStr = "OSU_YR4_Hip_30Hz";

		List<String> windowSizeList = Arrays.asList("10");
		List<String> trialGroupIdList = Arrays.asList("7cls");

		String tvtDataPath = expRootPath + "/OSU_YR4_Hip_30Hz.ws10.7cls";
		String labVisitFileFolder = "/nfs/guille/wong/wonglab2/obesity/2012/msexp/OSU_YR4_Hip_30Hz.ws10.7cls/features";
		String labVisitFileExt = "PureTrial.featurized.csv";

		List<String> formulaList = Arrays.asList(Formula.FORMULA_ALL_WO_FFT
				.toString());
		List<String> formulaNameList = Arrays.asList("AllWoFFT");

		String costExp = "[0.01,0.1,1,10,100,1000]";
		String scaleExp = "[1:1:10]";

		String clusterWorkspace = "/nfs/guille/wong/wonglab3/obesity/2012/cpd/OSU_YR4_Hip_30Hz.ws10.7cls/svm.linear.stacking.mv/cluster";
		singleScaleModel(expRootPath, datasetStr, tvtDataPath,
				labVisitFileFolder, labVisitFileExt, trialGroupIdList,
				windowSizeList, costExp, scaleExp, formulaList,
				formulaNameList, clusterWorkspace, "single");
		allScaleStackingModel(expRootPath, datasetStr, tvtDataPath,
				trialGroupIdList, windowSizeList, formulaList, formulaNameList,
				costExp, clusterWorkspace, "stacking");
	}

	private void OSU_YR4_30Hz_Wrist() throws Exception {
		String expRootPath = "/nfs/guille/wong/wonglab2/obesity/2012/msexp/MikeTest";
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

		String clusterWorkspace = "/nfs/guille/wong/wonglab2/obesity/2012/msexp/MikeTest/"
				+ datasetStr + ".ws10.7cls/svm.linear.stacking.mv/cluster";
		singleScaleModel(expRootPath, datasetStr, tvtDataPath,
				labVisitFileFolder, labVisitFileExt, trialGroupIdList,
				windowSizeList, costExp, scaleExp, formulaList,
				formulaNameList, clusterWorkspace, "single");
		allScaleStackingModel(expRootPath, datasetStr, tvtDataPath,
				trialGroupIdList, windowSizeList, formulaList, formulaNameList,
				costExp, clusterWorkspace, "stacking");
	}

	public static void main(String[] args) {
		try {
			new SVMLinearTrainValidateTest().OSU_YR4_30Hz_Hip();
			//new SVMLinearTrainValidateTest().OSU_YR4_30Hz_Wrist();
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
