// Modified from web.engr.oregonstate.edu.zheng.gef.task.msexp.stackingmv.osu

package MikeExperiments;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import web.engr.oregonstate.edu.zheng.gef.def.Array;
import web.engr.oregonstate.edu.zheng.gef.def.Table;
import web.engr.oregonstate.edu.zheng.gef.def.TaskDef;
import web.engr.oregonstate.edu.zheng.gef.def.Var;
import web.engr.oregonstate.edu.zheng.gef.executor.ExecutorBuilder;
import web.engr.oregonstate.edu.zheng.gef.executor.ExecutorBuilder.VerificationType;
import web.engr.oregonstate.edu.zheng.gef.task.freeliving.Formula;
import web.engr.oregonstate.edu.zheng.gef.utils.RUtils;

public class GlmnetTrainValidateTest extends TaskDef {

	private static final Logger log = Logger
			.getLogger(GlmnetTrainValidateTest.class.getName());

	private void singleScaleModel(String expRootPath, String datasetStr,
			List<String> trialGroupIdList, String tvtDataPath,
			String labVisitFileFolder, String labVisitFileExt,
			List<String> windowSizeList, String scaleExp,
			List<String> formulaList, List<String> formulaNameList,
			String clusterWorkspace, String jobId) throws Exception {
		Var dataset = var(datasetStr);
		Array windowSizes = array(windowSizeList);
		Array trialGroupIds = array(trialGroupIdList);

		Array splitId = array("[0:1:30]");
		Var iterationId = var("split").cat(splitId);
		Var dataPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat("glmnet.stacking").fileSep().cat(iterationId);
		Var expPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat("glmnet.stacking.mv").fileSep().cat(iterationId);

		Array formulaName = array(formulaNameList);
		Array alpha = array(Arrays.asList("1"));
		Array scale = array(scaleExp);

		logStep("Generate stacking features");
		Var modelId = var(formulaName).cat("_alpha").cat(alpha);
		Array subsets = array(Arrays.asList("train", "validate", "test"));
		Var stackingFeaturizesFunction = var("/nfs/guille/wong/users/andermic//scratch/workspace/ObesityExperimentRScript/ms.osu/stacking.featurize.R");
		Var stackingFeaturizesScript = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).dot().cat(subsets).cat(".stacking.featurize.R");
		Var stackingFeaturizedPath = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).dot().cat(subsets)
				.cat(".stacking.featurized.csv");
		ExecutorBuilder stackingFeatures = rScript(stackingFeaturizesFunction,
				stackingFeaturizesScript, var("featurizeMajorityVote"),
				execConfig().setParallelizable(true).setOnCluster(true)
						.setNumJobs(100).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		stackingFeatures.addParam("predictionDataFolder", String.class,
				dataPath);
		stackingFeatures.addParam("algorithmId", String.class, "glmnet");
		stackingFeatures.addParam("modelId", String.class, var(formulaName)
				.cat("_alpha").cat(alpha));
		stackingFeatures.addParam("algorithmId", String.class, "glmnet");
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
		Var summarizeFunction = var("/nfs/guille/wong/users/andermic//scratch/workspace/ObesityExperimentRScript/ms.osu/stacking.featurize.R");
		Var summarizeScript = expPath.fileSep().cat("glmnet").dot()
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
				var("glmnet.single.scale.model.").cat(formulaName)
						.cat("_alpha").cat(alpha).cat("_s"));
		summarizeSingleSize.addParam("dataset", String.class, var(subsets));
		summarizeSingleSize.after(expPath.fileSep()
				.cat("glmnet.single.scale.model.").cat(formulaName)
				.cat("_alpha").cat(alpha).cat("_s1").dot().cat(subsets)
				.cat(".summary.csv"));
		summarizeSingleSize.after(expPath.fileSep()
				.cat("glmnet.single.scale.model.").cat(formulaName)
				.cat("_alpha").cat(alpha).cat("_s1").dot().cat(subsets)
				.cat(".summary.csv"));
		summarizeSingleSize.after(expPath.fileSep()
				.cat("glmnet.single.scale.model.").cat(formulaName)
				.cat("_alpha").cat(alpha).cat("_s1").dot().cat(subsets)
				.cat(".summary.csv"));
		summarizeSingleSize.prodMode();
		summarizeSingleSize.execute();

		logStep("List all resources in a table");
		subsets = array(Arrays.asList("test"));
		modelId = var(formulaName).cat("_alpha").cat(alpha).cat("_s")
				.cat(scale);
		Var testResultPath = expPath.fileSep()
				.cat("glmnet.single.scale.model.").cat(modelId).dot()
				.cat(subsets).cat(".csv");
		Var summaryPath = expPath.fileSep().cat("glmnet.single.scale.model.")
				.cat(modelId).dot().cat(subsets).cat(".summary.csv");
		Var confusionMatrixPath = expPath.fileSep()
				.cat("glmnet.single.scale.model.").cat(modelId).dot()
				.cat(subsets).cat(".cm.csv");
		Array trialGroupId = array(trialGroupIdList);
		Table bestSingleScaleModelResTable = createTable(
				newColumn("WindowSize", windowSizes),
				newColumn("TrialGroup", trialGroupId),
				newColumn("Formula", formulaName), newColumn("Alpha", alpha),
				newColumn("Split", splitId), newColumn("TestDataSet", subsets),
				newColumn("Scale", scale),
				newColumn("TestReportFile", testResultPath),
				newColumn("TestAccuracyFile", summaryPath),
				newColumn("TestConfusionMatrixFile", confusionMatrixPath));
		expPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat("glmnet.stacking.mv");
		Var bestSingleScaleModelResSavePath = expPath.fileSep().cat(
				"glmnet.single.scale.model.res.csv");
		save(bestSingleScaleModelResTable, bestSingleScaleModelResSavePath);

		logStep("Merge splits results");
		Var mergeFunction = var("/nfs/guille/wong/users/andermic//scratch/workspace/ObesityExperimentRScript/function/merge.split.shuffle.R");
		Var mergeScript = expPath.fileSep().cat("merge.split.").cat(modelId)
				.dot().cat(subsets).cat(".R");
		Var accuracySavePath = expPath.fileSep()
				.cat("glmnet.single.scale.model.").cat(modelId).dot()
				.cat(subsets).cat(".accuracy.csv");
		Var meanAccuracySavePath = expPath.fileSep()
				.cat("glmnet.single.scale.model.").cat(modelId).dot()
				.cat(subsets).cat(".summary.csv");
		Var confusionMatrixSavePath = expPath.fileSep()
				.cat("glmnet.single.scale.model.").cat(modelId).dot()
				.cat(subsets).cat(".cm.csv");
		Var pctConfusionMatrixSavePath = expPath.fileSep()
				.cat("glmnet.single.scale.model.").cat(modelId).dot()
				.cat(subsets).cat(".pctcm.csv");
		ExecutorBuilder merge = rScript(mergeFunction, mergeScript,
				var("mergeSplitShuffle"));
		merge.addParam("bestModelResFilePath", String.class,
				bestSingleScaleModelResSavePath, VerificationType.Before);
		merge.addParam("windowSize", Integer.class, var(windowSizes));
		merge.addParam("scale", Integer.class, var(scale));
		merge.addParam("formula", String.class, var(formulaName));
		merge.addParam("alpha", Integer.class, var(alpha));
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
			List<String> trialGroupIdList, List<String> windowSizeList,
			List<String> formulaList, List<String> formulaNameList,
			String clusterWorkspace, String jobId) throws Exception {
		Var dataset = var(datasetStr);
		Array windowSizes = array(windowSizeList);
		Array trialGroupIds = array(trialGroupIdList);

		Array splitId = array("[0:1:30]");
		Var iterationId = var("split").cat(splitId);
		Var expPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat("glmnet.stacking.mv").fileSep().cat(iterationId);

		Array formula = array(formulaList);
		Array formulaName = array(formulaNameList);
		bind(formula, formulaName);
		Array alpha = array(Arrays.asList("1"));

		Var modelId = var(formulaName).cat("_alpha").cat(alpha);

		Var trainDataPath = expPath.fileSep().cat("glmnet").dot().cat(modelId)
				.cat(".train.stacking.featurized.csv");
		Var validateDataPath = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).cat(".validate.stacking.featurized.csv");
		Var testDataPath = expPath.fileSep().cat("glmnet").dot().cat(modelId)
				.cat(".test.stacking.featurized.csv");

		Var trainPredictionReportPath = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).cat(".stacking.train.csv");
		Var trainConfusionMatrixPath = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).cat(".stacking.train.cm.csv");
		Var trainPctConfusionMatrixPath = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).cat(".stacking.train.pctcm.csv");
		Var trainSummaryPath = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).cat(".stacking.train.summary.csv");

		Var validatePredictionReportPath = expPath.fileSep().cat("glmnet")
				.dot().cat(modelId).cat(".stacking.validate.csv");
		Var validateConfusionMatrixPath = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).cat(".stacking.validate.cm.csv");
		Var validatePctConfusionMatrixPath = expPath.fileSep().cat("glmnet")
				.dot().cat(modelId).cat(".stacking.validate.pctcm.csv");
		Var validateSummaryPath = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).cat(".stacking.validate.summary.csv");

		Var testPredictionReportPath = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).cat(".stacking.test.csv");
		Var testConfusionMatrixPath = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).cat(".stacking.test.cm.csv");
		Var testPctConfusionMatrixPath = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).cat(".stacking.test.pctcm.csv");
		Var testSummaryPath = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).cat(".stacking.test.summary.csv");

		logStep("Train, validate and test the stacking model");
		List<String> weightingFeatureList = Arrays
				.asList(Formula.getWeightingModelFormula("ActivityClass",
						"7cls", "scale1to10").toString());
		Array weightingFeatures = array(weightingFeatureList);
		Array trialGroupId = array(trialGroupIdList);
		bind(weightingFeatures, trialGroupId);
		Var stackingModelFunction = var("/nfs/guille/wong/users/andermic//scratch/workspace/ObesityExperimentRScript/function/glmnet.exp.R");
		Var stackingModelScript = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).dot().cat("stacking.R");
		Var modelSavePath = expPath.fileSep().cat("glmnet").dot().cat(modelId)
				.cat(".stacking.model.save");
		Var modelCoefSavePath = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).cat(".stacking.model.coef.csv");
		ExecutorBuilder stackingModel = rScript(stackingModelFunction,
				stackingModelScript, var("trainValidateTest"), execConfig()
						.setParallelizable(false).setOnCluster(true)
						.setNumJobs(50).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		// parameters for model training
		stackingModel.addParam("formula", String.class, var(weightingFeatures));
		stackingModel.addParam("trialGroupId", String.class, var(trialGroupId));
		stackingModel.addParam("trainDataPath", String.class, trainDataPath,
				VerificationType.Before);
		stackingModel.addParam("validateDataPath", String.class,
				validateDataPath, VerificationType.Before);
		stackingModel.addParam("testDataPath", String.class, testDataPath,
				VerificationType.Before);
		stackingModel.addParam("alpha", Integer.class, var(alpha));
		stackingModel.addParam("modelSavePath", String.class, modelSavePath,
				VerificationType.After);
		stackingModel.addParam("coefSavePath", String.class, modelCoefSavePath,
				VerificationType.After);
		stackingModel.addParam(
				"validateLambdaTablePath",
				String.class,
				expPath.fileSep().cat("glmnet").dot().cat(modelId)
						.cat(".stacking.validate.lambda.csv"),
				VerificationType.After);
		// parameters for testing the trained model on training data
		stackingModel.addParam("trainReportPath", String.class,
				trainPredictionReportPath, VerificationType.After);
		stackingModel.addParam("trainConfusionMatrixPath", String.class,
				trainConfusionMatrixPath, VerificationType.After);
		stackingModel.addParam("trainPctConfusionMatrixPath", String.class,
				trainPctConfusionMatrixPath, VerificationType.After);
		stackingModel.addParam("trainSummaryPath", String.class,
				trainSummaryPath, VerificationType.After);
		// parameters for testing the trained model on validation data
		stackingModel.addParam("validateReportPath", String.class,
				validatePredictionReportPath, VerificationType.After);
		stackingModel.addParam("validateConfusionMatrixPath", String.class,
				validateConfusionMatrixPath, VerificationType.After);
		stackingModel.addParam("validatePctConfusionMatrixPath", String.class,
				validatePctConfusionMatrixPath, VerificationType.After);
		stackingModel.addParam("validateSummaryPath", String.class,
				validateSummaryPath, VerificationType.After);
		// parameters for testing the trained model on validating data
		stackingModel.addParam("testReportPath", String.class,
				testPredictionReportPath, VerificationType.After);
		stackingModel.addParam("testConfusionMatrixPath", String.class,
				testConfusionMatrixPath, VerificationType.After);
		stackingModel.addParam("testPctConfusionMatrixPath", String.class,
				testPctConfusionMatrixPath, VerificationType.After);
		stackingModel.addParam("testSummaryPath", String.class,
				testSummaryPath, VerificationType.After);
		stackingModel.prodMode();
		stackingModel.execute();

		logStep("List all resources in a table");
		Array testDataSets = array(Arrays.asList("train", "validate", "test"));
		Var testAccuracyFile = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).dot().cat("stacking").dot().cat(testDataSets)
				.cat(".summary.csv");
		Var testConfusionMatrix = expPath.fileSep().cat("glmnet").dot()
				.cat(modelId).dot().cat("stacking").dot().cat(testDataSets)
				.cat(".cm.csv");
		Table bestModelResTable = createTable(
				newColumn("WindowSize", windowSizes),
				newColumn("TrialGroup", trialGroupId),
				newColumn("Formula", formulaName),
				newColumn("Alpha", alpha),
				newColumn("Split", splitId),
				newColumn("TestDataSet", testDataSets),
				newColumn("TestAccuracyFile", testAccuracyFile),
				newColumn("TestConfusionMatrixFile", testConfusionMatrix),
				newColumn("ValidateLambdaFile", expPath.fileSep().cat("glmnet")
						.dot().cat(modelId)
						.cat(".stacking.validate.lambda.csv")));
		expPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat("glmnet.stacking.mv");
		Var bestModelResSavePath = expPath.fileSep().cat(
				"glmnet.best.stacking.model.res.csv");
		save(bestModelResTable, bestModelResSavePath);

		logStep("Merge splits results");
		Var mergeFunction = var("/nfs/guille/wong/users/andermic//scratch/workspace/ObesityExperimentRScript/function/merge.split.shuffle.R");
		Var mergeScript = expPath.fileSep().cat("glmnet.merge.split.")
				.cat(modelId).dot().cat(testDataSets).cat(".R");
		// Var trialWindowPath = var(
		// "/nfs/guille/tgd/wonglab/data/obesity/2012/mstm/glmnet.stacking.mv/")
		// .cat(dataPath).fileSep().cat(trialTypeWindowSizeId);
		ExecutorBuilder merge = rScript(mergeFunction, mergeScript,
				var("mergeSplitShuffle"));
		merge.addParam("bestModelResFilePath", String.class,
				bestModelResSavePath, VerificationType.Before);
		merge.addParam("windowSize", Integer.class, var(windowSizes));
		merge.addParam("trialGroup", String.class, var(trialGroupId));
		merge.addParam("formula", String.class, var(formulaName));
		merge.addParam("alpha", Integer.class, var(alpha));
		merge.addParam("testDataSet", String.class, var(testDataSets));
		String expectedNumEntries = String.valueOf(splitId.getValues().size());
		merge.addParam("expectedNumEntries", Integer.class,
				var(expectedNumEntries));
		Var accuracySavePath = expPath.fileSep()
				.cat("glmnet.best.stacking.model.").cat(modelId).dot()
				.cat(testDataSets).cat(".accuracy.csv");
		merge.addParam("accuracySavePath", String.class, accuracySavePath,
				VerificationType.After);
		Var meanAccuracySavePath = expPath.fileSep()
				.cat("glmnet.best.stacking.model.").cat(modelId).dot()
				.cat(testDataSets).cat(".summary.csv");
		merge.addParam("meanAccuracySavePath", String.class,
				meanAccuracySavePath, VerificationType.After);
		Var confusionMatrixSavePath = expPath.fileSep()
				.cat("glmnet.best.stacking.model.").cat(modelId).dot()
				.cat(testDataSets).cat(".cm.csv");
		merge.addParam("confusionMatrixSavePath", String.class,
				confusionMatrixSavePath, VerificationType.After);
		Var pctConfusionMatrixSavePath = expPath.fileSep()
				.cat("glmnet.best.stacking.model.").cat(modelId).dot()
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
				.cat("DataSet: ").cat(dataset).cat(" Alpha: ").cat(alpha)
				.cat(" WindowSize: ").cat(windowSizes).cat(")");
		texConsuionMatrix.addParam("caption", String.class, caption);
		Var label = var("tbl:glmnet.").cat(dataset).cat(".ws").cat(windowSizes)
				.dot().cat(modelId).dot().cat(testDataSets);
		texConsuionMatrix.addParam("label", String.class, label);
		texConsuionMatrix.prodMode();
		texConsuionMatrix.execute();
	}

	private void OSU_YR4_Hip_30Hz() throws Exception {
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

		String scaleExp = "[1:1:10]";

		String clusterWorkspace = "/nfs/guille/wong/wonglab3/obesity/2012/cpd/OSU_YR4_Hip_30Hz.ws10.7cls/svm.linear.stacking.mv/cluster";
		singleScaleModel(expRootPath, datasetStr, trialGroupIdList,
				tvtDataPath, labVisitFileFolder, labVisitFileExt,
				windowSizeList, scaleExp, formulaList, formulaNameList,
				clusterWorkspace, "single");
		allScaleStackingModel(expRootPath, datasetStr, trialGroupIdList,
				windowSizeList, formulaList, formulaNameList, clusterWorkspace,
				"stacking");
	}

	private void OSU_YR4_30Hz_Wrist() throws Exception {
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

		String scaleExp = "[1:1:10]";

		String clusterWorkspace = "/nfs/guille/wong/wonglab2/obesity/2012/msexp/"
				+ datasetStr + ".ws10.7cls/glmnet.stacking.mv/cluster";
		singleScaleModel(expRootPath, datasetStr, trialGroupIdList,
				tvtDataPath, labVisitFileFolder, labVisitFileExt,
				windowSizeList, scaleExp, formulaList, formulaNameList,
				clusterWorkspace, "single");
		allScaleStackingModel(expRootPath, datasetStr, trialGroupIdList,
				windowSizeList, formulaList, formulaNameList, clusterWorkspace,
				"stacking");
	}

	public static void main(String[] args) {
		try {
			new GlmnetTrainValidateTest().OSU_YR4_Hip_30Hz();
			//new GlmnetTrainValidateTest().OSU_YR4_30Hz_Wrist();
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
