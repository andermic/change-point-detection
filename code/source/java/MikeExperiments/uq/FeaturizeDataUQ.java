// Modified from web.engr.oregonstate.edu.zheng.gef.task.msexp.stacking.osu

package MikeExperiments.uq;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import web.engr.oregonstate.edu.zheng.gef.def.Array;
import web.engr.oregonstate.edu.zheng.gef.def.Table;
import web.engr.oregonstate.edu.zheng.gef.def.TaskDef;
import web.engr.oregonstate.edu.zheng.gef.def.Var;
import web.engr.oregonstate.edu.zheng.gef.executor.ExecutorBuilder;
import web.engr.oregonstate.edu.zheng.gef.executor.ExecutorBuilder.VerificationType;

public class FeaturizeDataUQ extends TaskDef {
	private static final Logger log = Logger.getLogger(FeaturizeDataUQ.class
			.getName());

	protected FeaturizeDataUQ() {
	}

	private void featurizeGroundTruth(Var expPath, Integer clusterJobNum,
			Boolean useCluster, Var featurizeFunctionScriptPath,
			String clusterWorkspace, Var truncatedFileNames,
			Var duplicatesFileNames, Var eventsFileNames, Var frequency,
			Var featurePath, Array subjectIDs, String day, Var featurizeDataPath,
			Var callingScriptPath) throws Exception {
		logStep("Featurize data using ground truth windows");
		ExecutorBuilder featurizeGroundTruth = rScript(
				featurizeFunctionScriptPath,
				callingScriptPath,
				var("featurizeUQCPD"),
				execConfig().setParallelizable(useCluster).setNumJobs(clusterJobNum)
						.setOnCluster(true)
						.setClusterWorkspace(clusterWorkspace));
		featurizeGroundTruth.addParam("day", String.class, day);
		featurizeGroundTruth.addParam("truncatedDataFilePath", String.class,
				truncatedFileNames);
		featurizeGroundTruth.addParam("duplicatesDataFilePath", String.class,
				duplicatesFileNames);
		featurizeGroundTruth.addParam("frequency", Integer.class, frequency);
		featurizeGroundTruth.addParam("savePath", String.class, featurizeDataPath);
		featurizeGroundTruth.addParam("eventsPath", String.class,
				eventsFileNames);
		// add verification
		featurizeGroundTruth.before(truncatedFileNames);
		featurizeGroundTruth.before(duplicatesFileNames);
		featurizeGroundTruth.before(eventsFileNames);
		featurizeGroundTruth.after(featurizeDataPath);
		// set mode and start
		featurizeGroundTruth.prodMode();
		featurizeGroundTruth.execute();
	}
	
	private void featurizeValidateTest(Integer clusterJobNum, 
			Boolean useCluster, Array cpdAlgorithm, Array cpdFPR,
			Var featurizeFunctionScriptPath, Var callingScriptPathCPD,
			Var featurizedDataPathCPD, String clusterWorkspace,
			Var truncatedFileNames, Var duplicatesFileNames, Var frequency,
			Var eventsFileNames, Var predictedCpFileNames, Array subjectIDs,
			String day) throws Exception {
		logStep("Featurize data using predicted change points from the given algorithm");
		ExecutorBuilder featurizeChangePoints = rScript(
				featurizeFunctionScriptPath,
				callingScriptPathCPD,
				var("featurizeUQCPD"),
				execConfig().setParallelizable(useCluster)
						.setNumJobs(clusterJobNum)
						.setOnCluster(true)
						.setClusterWorkspace(clusterWorkspace));
		featurizeChangePoints.addParam("day", String.class, day);
		featurizeChangePoints.addParam("truncatedDataFilePath", String.class,
				truncatedFileNames);
		featurizeChangePoints.addParam("duplicatesDataFilePath", String.class,
				duplicatesFileNames);
		featurizeChangePoints.addParam("frequency", Integer.class, frequency);
		featurizeChangePoints.addParam("savePath", String.class,
				featurizedDataPathCPD);
		featurizeChangePoints.addParam("eventsPath", String.class,
				eventsFileNames);
		featurizeChangePoints.addParam("predictedCpPath", String.class,
				predictedCpFileNames);
		// add verification
		featurizeChangePoints.before(truncatedFileNames);
		featurizeChangePoints.before(duplicatesFileNames);
		featurizeChangePoints.before(eventsFileNames);
		featurizeChangePoints.before(predictedCpFileNames);
		featurizeChangePoints.after(featurizedDataPathCPD);
		// set mode and start
		featurizeChangePoints.prodMode();
		featurizeChangePoints.execute();
	}


	private void splitData(String tvtDataAssignmentPath, Array splitId,
			int numSplits, Array fileNames) throws Exception {
		logStep("Split dataset into three subsets");
		Var dataSetAssignmentPath = var(tvtDataAssignmentPath);
		// Randomly assign training and validation data
		Array subsetId = array("[0:1:3]");
		Var assignmentTablePath = dataSetAssignmentPath.fileSep().cat("split")
				.cat(splitId).cat(".part").cat(subsetId).cat(".csv");
		if (!all(fileExists(assignmentTablePath))) {
			logStep("Randomly assign training, validating and testing data");
			for (int i = 0; i < numSplits; ++i) {
				Var part1AssignmentTablePath = dataSetAssignmentPath.fileSep()
						.cat("split").cat(String.valueOf(i)).cat(".part0.csv");
				Var part2AssignmentTablePath = dataSetAssignmentPath.fileSep()
						.cat("split").cat(String.valueOf(i)).cat(".part1.csv");
				Var part3AssignmentTablePath = dataSetAssignmentPath.fileSep()
						.cat("split").cat(String.valueOf(i)).cat(".part2.csv");
				Var part4AssignmentTablePath = dataSetAssignmentPath.fileSep()
						.cat("split").cat(String.valueOf(i)).cat(".part2.csv");
				splitDataSetInto4Parts(fileNames, part1AssignmentTablePath,
						part2AssignmentTablePath, part3AssignmentTablePath,
						part4AssignmentTablePath);
			}
		}
	}
	
	private void mergeGroundTruth(Integer clusterJobNum, 
			Boolean useCluster, Array dataSets, Array splitId, 
			String tvtDataPath, String tvtDataAssignmentPath, 
			String clusterWorkspace, Var featurePath,
			String featurizedFileExtStr) throws Exception {
		logStep("Merge featurized ground truth data to train, validate and test data set files");
		Array dataSetFileExtension = array(Arrays.asList(featurizedFileExtStr,
				featurizedFileExtStr, featurizedFileExtStr));
		Array assignment = array(Arrays.asList("0", "1", "2"));
		bind(dataSets, dataSetFileExtension, assignment);
	
		Var iterationId = var("split").cat(splitId);
		Var mergeDataFunctionScriptPath = var("/nfs/guille/wong/users/andermic//scratch/workspace/ObesityExperimentRScript/free.living/data/merge.data.R");
		Var trainValidateTestDataPath = var(tvtDataPath);
		Var mergeTrainDataCallingPath = trainValidateTestDataPath.fileSep()
				.cat(iterationId).fileSep().cat("merge.").cat(dataSets)
				.cat(".120.data.R");
		Var saveDataPath = trainValidateTestDataPath.fileSep().cat(iterationId)
				.fileSep().cat(dataSets).cat(".120.data.csv");
		Var dataAssignmentTablePath = var(tvtDataAssignmentPath).fileSep()
				.cat("split").cat(splitId).cat(".part").cat(assignment)
				.cat(".csv");
		ExecutorBuilder createTrainData = rScript(
				mergeDataFunctionScriptPath,
				mergeTrainDataCallingPath,
				var("mergeData"),
				execConfig().setParallelizable(useCluster)
						.setNumJobs(clusterJobNum)
						.setOnCluster(true)
						.setClusterWorkspace(clusterWorkspace));
		createTrainData.addParam("dataFileFolder", String.class, featurePath);
		createTrainData.addParam("dataNameFilePath", String.class,
				dataAssignmentTablePath, VerificationType.Before);
		createTrainData.addParam("dataFileExtension", String.class,
				var(dataSetFileExtension));
		createTrainData.addParam("savePath", String.class, saveDataPath,
				VerificationType.After);
		createTrainData.addParam("columns", List.class,
				var("c(\"WindowId\",\"File\")"));
		createTrainData.prodMode();
		createTrainData.execute();
	}

	private void mergeValidateTest(Integer clusterJobNum, Boolean useCluster,
			Array cpdAlgorithm, Array cpdFPR, 
			String tvtDataPath, String tvtDataAssignmentPath, 
			String featurizedFileExtStr, Array dataSets, Array splitId, 
			String clusterWorkspace, Var featurePath) throws Exception {
		logStep("Merge featurized cpd algorithm data");
		//Var featurizedFileExt = var("PureTrial.featurized.").
		//		cat(cpdAlgorithm).dot().cat(cpdFPR).dot()
		//		.cat("csv");
		Array dataSetFileExtension = array(Arrays.asList(featurizedFileExtStr,
				featurizedFileExtStr, featurizedFileExtStr));
		Array assignment = array(Arrays.asList("0", "1", "2"));
		bind(dataSets, dataSetFileExtension, assignment);
	
		Var iterationId = var("split").cat(splitId);
		Var mergeDataFunctionScriptPath = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/free.living/data/merge.data.R");
		Var trainValidateTestDataPath = var(tvtDataPath);
		Var mergeTrainDataCallingPath = trainValidateTestDataPath.fileSep()
				.cat(iterationId).fileSep().cat("merge.").cat(dataSets)
				.dot().cat(cpdAlgorithm).dot().cat(cpdFPR).cat(".data.R");
		Var saveDataPath = trainValidateTestDataPath.fileSep().cat(iterationId)
				.fileSep().cat(dataSets).dot().cat(cpdAlgorithm).dot()
				.cat(cpdFPR).cat(".data.csv");
		Var dataAssignmentTablePath = var(tvtDataAssignmentPath).fileSep()
				.cat("split").cat(splitId).cat(".part").cat(assignment)
				.cat(".csv");
		ExecutorBuilder createValidateTestData = rScript(
				mergeDataFunctionScriptPath,
				mergeTrainDataCallingPath,
				var("mergeData"),
				execConfig().setParallelizable(useCluster)
						.setNumJobs(clusterJobNum)
						.setOnCluster(true)
						.setClusterWorkspace(clusterWorkspace));
		createValidateTestData.addParam("dataFileFolder", String.class, featurePath);
		createValidateTestData.addParam("dataNameFilePath", String.class,
				dataAssignmentTablePath, VerificationType.Before);
		createValidateTestData.addParam("dataFileExtension", String.class,
				var(dataSetFileExtension));
		createValidateTestData.addParam("savePath", String.class, saveDataPath,
				VerificationType.After);
		createValidateTestData.addParam("columns", List.class,
				var("c(\"WindowId\",\"File\")"));
		createValidateTestData.prodMode();
		createValidateTestData.execute();
	}

	private void splitDataSetInto4Parts(Array fileNames,
			Var part1AssignmentTablePath, Var part2AssignmentTablePath,
			Var part3AssignmentTablePath, Var part4AssignmentTablePath)
			throws Exception {
		Array labVisit1Files = which(fileNames, contains(fileNames, "V1"));
		log.info("Number of Visit1: " + labVisit1Files.getValues().size());
		List<Array> labVisit1Split = split(labVisit1Files, 4);

		Array labVisit2Files = subtract(fileNames, labVisit1Files);
		log.info("Number of Visit2: " + labVisit2Files.getValues().size());
		List<Array> labVisit2Split = split(labVisit2Files, 4);

		Array part1 = union(labVisit1Split.get(0), labVisit2Split.get(0));
		save(createTable(newColumn("SubjectID", part1)),
				part1AssignmentTablePath);

		Array part2 = union(labVisit1Split.get(1), labVisit2Split.get(1));
		save(createTable(newColumn("SubjectID", part2)),
				part2AssignmentTablePath);

		Array part3 = union(labVisit1Split.get(2), labVisit2Split.get(2));
		save(createTable(newColumn("SubjectID", part3)),
				part3AssignmentTablePath);

		Array part4 = union(labVisit1Split.get(3), labVisit2Split.get(3));
		save(createTable(newColumn("SubjectID", part4)),
				part4AssignmentTablePath);
	}
	
	private void featurizeData(String expRootPath, String datasetStr,
			String frequencyStr, String rawDataPathStr,
			String tvtDataAssignmentPath, String tvtDataPath,
			String clusterWorkspace, Array cpdAlgorithms, Array cpdFPR,
			Integer clusterJobNum, Boolean useCluster, Array subjectIDs,
			String day, Array kpres) throws Exception {
		Var dataset = var(datasetStr);
		Var frequency = var(frequencyStr);
		Var rawDataPath = var(rawDataPathStr);

		logStep("Convert the timestamped data to feature vectors");
		Var expPath = var(expRootPath).fileSep().cat(dataset);
		Var featurePath = expPath.fileSep().cat("features");
		Var truncatedFileNames = rawDataPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_").cat(frequency).cat("hz_truncated_day").cat(day).cat(".csv");
		Var duplicatesFileNames = rawDataPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_").cat(frequency).cat("hz_duplicates_day").cat(day).cat(".csv");
		Var eventsFileNames = rawDataPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_events_day").cat(day).cat(".csv");
		//Var startEndFileNames = rawDataPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_start_and_end.csv");
		Var callingScriptPath = featurePath.fileSep().cat(subjectIDs).cat(".featurize.ground.R");
		Var featurizeDataPath = featurePath.fileSep().cat(subjectIDs).cat(".featurized.ground.csv");

		Var predictedCpFileNames = var("/nfs/guille/wong/users/andermic/uq/changepoints").fileSep().cat("predicted_changes_").cat(cpdAlgorithms).cat("_kpre").cat(kpres).fileSep().cat(cpdFPR).fileSep().cat(subjectIDs).cat(".csv");
		Var callingScriptPathCPD = featurePath.fileSep().cat(subjectIDs).cat(".featurize.").cat(cpdAlgorithms).dot().cat(cpdFPR).cat(".R");
		Var featurizeDataPathCPD = featurePath.fileSep().cat(subjectIDs).cat(".featurized.").cat(cpdAlgorithms).dot().cat(cpdFPR).cat(".csv");
		
		Var featurizeFunctionScriptPath = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		mkdir(featurePath);
		
		int numSplits = 30;
		Array splitId = array("[0:1:" + numSplits + "]");
		Array dataSets = array(Arrays.asList("trainBase", "validateBase", "trainHMM", "testHMM"));

		// This is where the action happens
		featurizeGroundTruth(expPath, clusterJobNum, useCluster, featurizeFunctionScriptPath, clusterWorkspace, truncatedFileNames, duplicatesFileNames, eventsFileNames, frequency, featurePath, subjectIDs, day, featurizeDataPath, callingScriptPath);
		featurizeValidateTest(clusterJobNum, useCluster, cpdAlgorithms, cpdFPR, featurizeFunctionScriptPath, callingScriptPathCPD, featurizeDataPathCPD, clusterWorkspace, truncatedFileNames, duplicatesFileNames, frequency, eventsFileNames, predictedCpFileNames, subjectIDs, day);
		//splitData(tvtDataAssignmentPath, splitId, numSplits, fileNames);
		//mergeGroundTruth(clusterJobNum, useCluster, dataSets, splitId, tvtDataPath, tvtDataAssignmentPath, clusterWorkspace, featurePath, featurizedFileExtStr);
		//mergeValidateTest(clusterJobNum, useCluster, cpdAlgorithm, cpdFPR, tvtDataPath, tvtDataAssignmentPath, featurizedFileExtStr, dataSets, splitId, clusterWorkspace, featurePath);
	}
	
	private void UQ_30Hz() throws Exception {
		String expRootPath = "/nfs/guille/wong/users/andermic/Desktop/cpd";
		String day = "2";
		String frequencyStr = "30";
		String datasetStr = "uq_" + frequencyStr + "Hz_day" + day;
		
		String rawDataPathStr = "/nfs/guille/wong/wonglab3/obesity/freeliving/UQ/processed";
		String tvtDataPath = "/nfs/guille/wong/users/andermic/Desktop/cpd/" + datasetStr;
		String tvtDataAssignmentPath = tvtDataPath + "/splits";
		
		String clusterWorkspace = "/nfs/guille/wong/users/andermic/Desktop/cpd/" + datasetStr + "/cluster";
		Integer clusterJobNum = 100;
		Boolean useCluster = false;
		
		Array subjectIDs = array(Arrays.asList("1", "2", "3", "4", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "18", "19", "20", "21", "22", "23", "24", "25"));
subjectIDs = array(Arrays.asList("1"));
		Array cpdAlgorithms = array(Arrays.asList("cc"));
		Array cpdFPR = array(Arrays.asList("0.01", "0.05", "0.1"));
cpdFPR = array(Arrays.asList("0.01"));
		Array kpres = array(Arrays.asList("30"));
		
		featurizeData(expRootPath, datasetStr, frequencyStr,
				rawDataPathStr, tvtDataAssignmentPath, tvtDataPath,
				clusterWorkspace, cpdAlgorithms, cpdFPR, clusterJobNum,
				useCluster, subjectIDs, day, kpres);
	}

	public static void main(String[] args) {
		try {
			new FeaturizeDataUQ().UQ_30Hz();
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}