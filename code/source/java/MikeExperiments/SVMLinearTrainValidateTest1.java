// Modified from web.engr.oregonstate.edu.zheng.gef.task.msexp.stacking.osu

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

public class SVMLinearTrainValidateTest1 extends TaskDef {

	private static final Logger log = Logger
			.getLogger(SVMLinearTrainValidateTest1.class.getName());

	private void singleScaleModel(String expRootPath, String datasetStr,
			String tvtDataPath, String labVisitFileFolder,
			String trainingLabVisitFileExt, Var valiTestLabVisitFileExt,
			List<String> trialGroupIdList,
			List<String> windowSizeList, String costExp,
			List<String> formulaList, List<String> formulaNameList,
			String clusterWorkspace, String jobId, Array cpdAlgorithm,
			Array cpdFPR) throws Exception {

		Var dataset = var(datasetStr);
		Array windowSizes = array(windowSizeList);
		Array trialGroupIds = array(trialGroupIdList);

		Array splitId = array("[0:1:30]");
		Var iterationId = var("split").cat(splitId);
		Var expPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat("svm.linear").fileSep().cat(iterationId);

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
		Array cost = array(costExp);
		Var modelId = var(formulaName).dot().cat(cpdAlgorithm).dot()
				.cat(cpdFPR).cat("_cost").cat(cost);
		
		Var validateSummaryFile = var(tvtDataPath).cat("/svm.linear/svm.validate.")
				.cat(cpdAlgorithm).dot().cat(cpdFPR).cat(".summary.csv");

		Var bestModelPrefix = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).dot().cat(cpdAlgorithm).dot()
				.cat(cpdFPR).cat(".best.model");
		
		Var bestModelInfoPath = bestModelPrefix.cat(".info.csv");
		Var bestModelSavePath = bestModelPrefix.cat(".best.model").cat(".save");
		Var trainResultPath = bestModelPrefix.cat(".train.csv");
		Var validateResultPath = bestModelPrefix.cat(".validate.csv");
		Var testResultPath = bestModelPrefix.cat(".test.csv");
		
		Var bestModelId = var("best").dot().cat(formulaName).dot()
				.cat(cpdAlgorithm).dot().cat(cpdFPR);
		Var confusionMatrixPath = expPath.fileSep().cat("svm").dot()
				.cat(bestModelId).cat(".test.cm.csv");
		Var pctConsufionMatrixPath = expPath.fileSep().cat("svm").dot()
				.cat(bestModelId).cat(".test.pctcm.csv");
		Var summaryPath = expPath.fileSep().cat("svm").dot().cat(bestModelId)
				.cat(".test.summary.csv");

		Array trialGroupId = array(trialGroupIdList);
		Array testDataSets = array(Arrays.asList("test"));
		Var modelPath = var(expRootPath).fileSep().cat(dataset).dot().cat("ws")
				.cat(windowSizes).dot().cat(trialGroupIds).fileSep()
				.cat("svm.linear");
		
		logStep("Train/validate svm model");
		Var trainValidateScriptPath = var("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var svmTrainCallingPath = expPath.fileSep().cat("svm").dot()
				.cat(modelId).cat(".R");
		//Var modelSavePath = expPath.fileSep().cat("svm").dot().cat(modelId)
		// .cat(".model.save");
		Var quickValidateSummaryPath = expPath.fileSep().cat("svm").dot()
				.cat(modelId).cat(".validate.summary.csv");
		ExecutorBuilder singleScale = rScript(trainValidateScriptPath,
				svmTrainCallingPath, var("quickTrainValidateCPD"),
				execConfig().setParallelizable(false).setOnCluster(true)
						.setNumJobs(500).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		// parameters for model training
		singleScale.addParam("formula", String.class, var(formula));
		singleScale.addParam("trainDataInfoPath", String.class,
				trainDataInfoPath, VerificationType.Before);
		singleScale.addParam("validateDataInfoPath", String.class,
				validateDataInfoPath, VerificationType.Before);
		singleScale.addParam("labVisitFileFolder", String.class,
				labVisitFileFolder, VerificationType.Before);
		singleScale.addParam("trainingLabVisitFileExt", String.class,
				trainingLabVisitFileExt);
		singleScale.addParam("valiTestLabVisitFileExt", String.class, 
				valiTestLabVisitFileExt);
		singleScale.addParam("kernal", String.class, "linear");
		singleScale.addParam("cost", Double.class, var(cost));
		singleScale.addParam("validateSummaryPath", String.class,
				quickValidateSummaryPath, VerificationType.After);
		singleScale.prodMode();
		singleScale.execute();

		logStep("Summarize model validation results");
		for(String alg : cpdAlgorithm.getValues()) {
			for (String fpr: cpdFPR.getValues()) {
				Table singleScaleValidationTbl = createTable(
						new TableColumn("ExpPath", expPath),
						new TableColumn("Split", var(splitId)),
						new TableColumn("Formula", var(formulaName)),
						new TableColumn("Scale", var("120")),
						new TableColumn("CpdAlgorithm", var(alg)),
						new TableColumn("CpdFpr", var(fpr)),
						new TableColumn("Cost", var(cost)), new TableColumn(
								"ValidateAccuracy", var("NA")));
				DataSet data = singleScaleValidationTbl.getData();
				for (int i = 0; i < data.getRowCount(); ++i) {
					String filePath = data.getData("ExpPath", i) + "/svm."
							+ data.getData("Formula", i) + "."
							+ data.getData("CpdAlgorithm", i) + "."
							+ data.getData("CpdFpr", i) + "_cost"
							+ data.getData("Cost", i) 
							+ ".validate.summary.csv";
					if (new File(filePath).exists()) {
						DataSet accTbl = new DataSet(filePath, true);
						double accuracy = accTbl.getDouble("Accuracy", 0);
						data.insertData("ValidateAccuracy", i, accuracy);
					}
				}
			singleScaleValidationTbl.save(tvtDataPath 
					+ "/svm.linear/svm.validate." + alg + '.' + fpr + ".summary.csv");
			}
		}

		logStep("Test the best svm model");
		Var testBestSingleScaleModelFunction = var("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var testBestSingleScaleModelScript = expPath.fileSep().cat("svm").dot()
				.cat(formulaName).dot().cat(cpdAlgorithm).dot().cat(cpdFPR)
				.cat(".best.R");
		ExecutorBuilder bestSingleScale = rScript(
				testBestSingleScaleModelFunction,
				testBestSingleScaleModelScript, var("testBestModelCPD"),
				execConfig().setParallelizable(false).setOnCluster(true)
						.setNumJobs(500).setClusterWorkspace(clusterWorkspace)
						.setJobId(jobId));
		bestSingleScale.addParam("validateSummaryFile", String.class,
				validateSummaryFile, VerificationType.Before);
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
				testResultPath, VerificationType.After);
		bestSingleScale.prodMode();
		bestSingleScale.execute();

		logStep("Summarize model test results");
		Var summarizeFunctionPath = var("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var summarizeScriptPath = expPath.fileSep().cat("svm").dot()
				.cat(bestModelId).cat(".summarize.R");
		ExecutorBuilder summarizeSingle = rScript(summarizeFunctionPath,
				summarizeScriptPath, var("summarizeCPD"),
				execConfig().setParallelizable(false).setOnCluster(true)
						.setNumJobs(500).setClusterWorkspace(clusterWorkspace)
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

		logStep("List all resources in a table");
		for(String form: formulaName.getValues()) {
			for(String alg : cpdAlgorithm.getValues()) {
				for (String fpr: cpdFPR.getValues()) {
					Var testResultPathSingle = var(tvtDataPath).cat("/svm.linear/").cat("split").cat(splitId).cat("/svm.").cat(form).dot().cat(alg).dot().cat(fpr).cat(".best.model.test.csv");
					String bestModelIdSingle = "best." + form + "." + alg + "." + fpr;
					Var summaryPathSingle = var(tvtDataPath).cat("/svm.linear/").cat("split").cat(splitId).cat("/svm.").cat(bestModelIdSingle).cat(".test.summary.csv");
					Var confusionMatrixPathSingle = var(tvtDataPath).cat("/svm.linear/").cat("split").cat(splitId).cat("/svm.").cat(bestModelIdSingle).cat(".test.cm.csv");
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
		
		logStep("Merge splits results");
		Var mergeFunction = var("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/function/merge.split.shuffle.R");
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
				var("mergeSplitShuffle"));
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

		String costExp = "[0.01,0.1,1,10,100,1000]";

		String clusterWorkspace = "/nfs/guille/wong/wonglab3/obesity/2012/cpd/OSU_YR4_Hip_30Hz.ws120.7cls/svm.linear/cluster";

		Array cpdAlgorithm = array(Arrays.asList("cc", "kliep"));
		Array cpdFPR = array(Arrays.asList("0.0005", "0.001", "0.002", "0.003", "0.004"));
		String trainingLabVisitFileExt = ("PureTrial.featurized.120.csv");
		Var valiTestLabVisitFileExt = var("PureTrial.featurized.").cat(cpdAlgorithm).dot().cat(cpdFPR).cat(".csv");
		
		singleScaleModel(expRootPath, datasetStr, tvtDataPath,
				labVisitFileFolder, trainingLabVisitFileExt, valiTestLabVisitFileExt, trialGroupIdList,
				windowSizeList, costExp, formulaList,
				formulaNameList, clusterWorkspace, "single",
				cpdAlgorithm, cpdFPR);
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
			new SVMLinearTrainValidateTest1().OSU_YR4_30Hz_Hip();
			//new SVMLinearTrainValidateTest1().OSU_YR4_30Hz_Wrist();
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
